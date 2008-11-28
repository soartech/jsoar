/*
 * Copyright (c) 2008  Dave Ray <daveray@gmail.com>
 *
 * Created on Sep 12, 2008
 */
package org.jsoar.kernel.learning.rl;

import java.util.Iterator;
import java.util.ListIterator;
import java.util.Map;

import org.jsoar.kernel.Agent;
import org.jsoar.kernel.DefaultProductionManager;
import org.jsoar.kernel.ImpasseType;
import org.jsoar.kernel.Production;
import org.jsoar.kernel.ProductionType;
import org.jsoar.kernel.exploration.Exploration;
import org.jsoar.kernel.lhs.Condition;
import org.jsoar.kernel.lhs.GoalIdTest;
import org.jsoar.kernel.lhs.ImpasseIdTest;
import org.jsoar.kernel.lhs.PositiveCondition;
import org.jsoar.kernel.lhs.Test;
import org.jsoar.kernel.lhs.Tests;
import org.jsoar.kernel.memory.Instantiation;
import org.jsoar.kernel.memory.Preference;
import org.jsoar.kernel.memory.PreferenceType;
import org.jsoar.kernel.memory.Slot;
import org.jsoar.kernel.memory.WmeImpl;
import org.jsoar.kernel.rete.Token;
import org.jsoar.kernel.rhs.Action;
import org.jsoar.kernel.rhs.MakeAction;
import org.jsoar.kernel.rhs.ReordererException;
import org.jsoar.kernel.rhs.RhsSymbolValue;
import org.jsoar.kernel.symbols.IdentifierImpl;
import org.jsoar.kernel.symbols.StringSymbol;
import org.jsoar.kernel.symbols.Symbol;
import org.jsoar.kernel.symbols.SymbolImpl;
import org.jsoar.kernel.tracing.Trace.Category;
import org.jsoar.util.ByRef;
import org.jsoar.util.ListHead;

/**
 * @author ray
 */
public class ReinforcementLearning
{
    public static final double RL_RETURN_LONG = 0.1;
    public static final String RL_RETURN_STRING = "";
    
    public static final int RL_LEARNING_ON = 1;
    public static final int RL_LEARNING_OFF = 2;
    
    public static final int RL_LEARNING_SARSA = 1;
    public static final int RL_LEARNING_Q = 2;
    
    public static final int RL_TE_ON = 1;
    public static final int RL_TE_OFF = 2;
    
    // names of params
    public static final int RL_PARAM_LEARNING                  = 0;
    public static final int RL_PARAM_DISCOUNT_RATE             = 1;
    public static final int RL_PARAM_LEARNING_RATE             = 2;
    public static final int RL_PARAM_LEARNING_POLICY           = 3;
    public static final int RL_PARAM_ET_DECAY_RATE             = 4;
    public static final int RL_PARAM_ET_TOLERANCE              = 5;
    public static final int RL_PARAM_TEMPORAL_EXTENSION        = 6;
    public static final int RL_PARAMS                          = 7; // must be 1+ last rl param
    
    // names of stats
    public static final int RL_STAT_UPDATE_ERROR               = 0;
    public static final int RL_STAT_TOTAL_REWARD               = 1;
    public static final int RL_STAT_GLOBAL_REWARD              = 2;
    public static final int RL_STATS                           = 3; // must be 1+ last rl stat
    
    // more specific forms of no change impasse types
    // made negative to never conflict with impasse constants
    public static final int STATE_NO_CHANGE_IMPASSE_TYPE = -1;
    public static final int OP_NO_CHANGE_IMPASSE_TYPE = -2;

    // reinforcement learning
    rl_parameter rl_params[] = new rl_parameter[ RL_PARAMS ];
    rl_stat rl_stats[] = new rl_stat[ RL_STATS ];
    int rl_template_count;
    boolean rl_first_switch;

    private final Agent my_agent;
    private boolean enabled = false;
    
    /**
     * @param context
     */
    public ReinforcementLearning(Agent context)
    {
        this.my_agent = context;
        
        // rl initialization
        // agent.cpp:311:create_agent
        rl_params[ RL_PARAM_LEARNING ] = rl_add_parameter( "learning", RL_LEARNING_OFF,
                    new StringParameterValFunc() { public boolean execute(int in) { return  rl_validate_learning(in); }}, 
                    new StringParameterToString() { public String execute(int in) { return rl_convert_learning(in); } },
                    new StringParameterFromString() { public int execute(String in) { return rl_convert_learning(in); }});
        rl_params[ RL_PARAM_DISCOUNT_RATE ] = rl_add_parameter( "discount-rate", 0.9,
                    new NumberParameterValFunc() { public boolean execute(double in) { return rl_validate_discount(in); } }  );  
        rl_params[ RL_PARAM_LEARNING_RATE ] = rl_add_parameter( "learning-rate", 0.3, 
                new NumberParameterValFunc() { public boolean execute(double in) { return rl_validate_learning_rate(in); } });
        rl_params[ RL_PARAM_LEARNING_POLICY ] = rl_add_parameter( "learning-policy", RL_LEARNING_SARSA, 
                new StringParameterValFunc() { public boolean execute(int in) { return  rl_validate_learning_policy(in); }}, 
                new StringParameterToString() { public String execute(int in) { return rl_convert_learning_policy(in); } },
                new StringParameterFromString() { public int execute(String in) { return rl_convert_learning_policy(in); }}
        );
        rl_params[ RL_PARAM_ET_DECAY_RATE ] = rl_add_parameter( "eligibility-trace-decay-rate", 0, 
                new NumberParameterValFunc() { public boolean execute(double in) { return rl_validate_decay_rate(in); } });
        rl_params[ RL_PARAM_ET_TOLERANCE ] = rl_add_parameter( "eligibility-trace-tolerance", 0.001, 
                new NumberParameterValFunc() { public boolean execute(double in) { return rl_validate_trace_tolerance(in); } });
        rl_params[ RL_PARAM_TEMPORAL_EXTENSION ] = rl_add_parameter( "temporal-extension", RL_TE_ON, 
                new StringParameterValFunc() { public boolean execute(int in) { return  rl_validate_te_enabled(in); }}, 
                new StringParameterToString() { public String execute(int in) { return rl_convert_te_enabled(in); } },
                new StringParameterFromString() { public int execute(String in) { return rl_convert_te_enabled(in); }});

        rl_stats[ RL_STAT_UPDATE_ERROR ] = rl_add_stat( "update-error" );
        rl_stats[ RL_STAT_TOTAL_REWARD ] = rl_add_stat( "total-reward" );
        rl_stats[ RL_STAT_GLOBAL_REWARD ] = rl_add_stat( "global-reward" );

        rl_initialize_template_tracking( );
        
        rl_first_switch = true;

    }

    /**
     * reinforcement_learning.cpp:43:rl_clean_parameters
     */
    void rl_clean_parameters()
    {
        for ( int i=0; i<RL_PARAMS; i++ )
        {
            rl_params[i] = null;
        }
    }
    
    /**
     * reinforcement_learning.cpp:55:rl_clean_stats
     */
    void rl_clean_stats()
    {
        for ( int i=0; i<RL_STATS; i++ )
        {
            rl_stats[ i ] = null;
        }
    }
    
    /**
     * reinforcement_learning.cpp:64:rl_reset_data
     */
    public void rl_reset_data()
    {
        IdentifierImpl goal = my_agent.decider.top_goal;
        while( goal != null)
        {
            ReinforcementLearningInfo data = goal.rl_info;

            data.eligibility_traces.clear(); 
            
            data.prev_op_rl_rules.clear();
            data.num_prev_op_rl_rules = 0;

            data.previous_q = 0;
            data.reward = 0;
            data.step = 0;
            data.reward_age = 0;
            data.impasse_type = ImpasseType.NONE;
            
            goal = goal.lower_goal;
        }
    } 
    
    /**
     * reinforcement_learning.cpp:90:rl_reset_stats
     */
    public void rl_reset_stats()
    {
        for ( int i=0; i<RL_STATS; i++ )
            rl_stats[ i ].value = 0;
    } 
    
    /**
     * reinforcement_learning.cpp:99:rl_remove_refs_for_prod
     * 
     * @param prod
     */
    void rl_remove_refs_for_prod(Production prod )
    {
        for ( IdentifierImpl state = my_agent.decider.top_state; state != null; state = state.lower_goal )
        {
            state.rl_info.eligibility_traces.remove( prod );
            
            ListIterator<Production> it =  state.rl_info.prev_op_rl_rules.listIterator();
            while(it.hasNext())
            {
                Production c = it.next();
                if(c == prod)
                {
                    it.set(null);
                }
            }
        }
    }
    
    /**
     * reinforcement_learning.cpp:114:rl_add_parameter
     * 
     * @param name
     * @param value
     * @param val_func
     * @return
     */
    rl_parameter rl_add_parameter( String name, double value, NumberParameterValFunc val_func)
    {
        // new parameter entry
        rl_parameter newbie = new rl_parameter();
        newbie.number_param = new rl_number_parameter();
        newbie.number_param.value = value;
        newbie.number_param.val_func = val_func;
        newbie.type = rl_param_type.rl_param_number;
        newbie.name = name;
        
        return newbie;
    }
    
    /**
     * reinforcement_learning.cpp:127:rl_add_parameter
     * 
     * @param name
     * @param value
     * @param val_func
     * @param to_str
     * @param from_str
     * @return
     */
    rl_parameter rl_add_parameter( String name, int value, StringParameterValFunc val_func, StringParameterToString to_str,
                                   StringParameterFromString from_str)
    {
        // new parameter entry
        rl_parameter newbie = new rl_parameter();
        newbie.string_param = new rl_string_parameter();
        newbie.string_param.val_func = val_func;
        newbie.string_param.to_str = to_str;
        newbie.string_param.from_str = from_str;
        newbie.string_param.value = value;
        newbie.type = rl_param_type.rl_param_string;
        newbie.name = name;
        
        return newbie;
    }
    
    /**
     * reinforcement_learning.cpp:144:rl_convert_parameter
     * 
     * @param param
     * @return
     */
    String rl_convert_parameter(int param )
    {
        if ( ( param < 0 ) || ( param >= RL_PARAMS ) )
            return null;

        return rl_params[ param ].name;
    }

    /**
     * reinforcement_learning.cpp:153:rl_convert_parameter
     * 
     * @param name
     * @return
     */
    int rl_convert_parameter(String name )
    {
        for ( int i=0; i<RL_PARAMS; i++ )
            if ( name.equals(rl_params[ i ].name ))
                return i;

        return RL_PARAMS;
    }
    
    /**
     * reinforcement_learning.cpp:165:rl_valid_parameter
     * 
     * @param name
     * @return
     */
    boolean rl_valid_parameter(String name )
    {
        return ( rl_convert_parameter(name ) != RL_PARAMS );
    }

    /**
     * reinforcement_learning.cpp:170:rl_valid_parameter
     * 
     * @param param
     * @return
     */
    boolean rl_valid_parameter(int param )
    {
        return ( rl_convert_parameter( param ) != null );
    } 
    
    /**
     * reinforcement_learning.cpp:178:rl_get_parameter_type
     * 
     * @param name
     * @return
     */
    rl_param_type rl_get_parameter_type(String name )
    {
        int param = rl_convert_parameter( name );
        if ( param == RL_PARAMS )
            return rl_param_type.rl_param_invalid;
        
        return rl_params[ param ].type;
    }

    /**
     * reinforcement_learning.cpp:187:rl_get_parameter_type
     * 
     * @param param
     * @return
     */
    rl_param_type rl_get_parameter_type(int param )
    {
        if ( !rl_valid_parameter( param ) )
            return rl_param_type.rl_param_invalid;

        return rl_params[ param ].type;
    }    

    int rl_get_parameter( String name, double test)
    {
        int param = rl_convert_parameter(name );
        if ( param == RL_PARAMS )
            return 0;
        
        if ( rl_get_parameter_type( param ) != rl_param_type.rl_param_string )
            return 0;
        
        return rl_params[ param ].string_param.value;
    }

    String rl_get_parameter( String name, String  test)
    {
        int param = rl_convert_parameter( name );
        if ( param == RL_PARAMS )
            return null;
        
        if ( rl_get_parameter_type( param ) != rl_param_type.rl_param_string )
            return null;
        
        return rl_params[ param ].string_param.to_str.execute( rl_params[ param ].string_param.value );
    }

    double rl_get_parameter( String name )
    {
        int param = rl_convert_parameter( name );
        if ( param == RL_PARAMS )
            return 0;
        
        if ( rl_get_parameter_type( param ) != rl_param_type.rl_param_number )
            return 0;
        
        return rl_params[ param ].number_param.value;
    }

    //

    public int rl_get_parameter( int param, double test)
    {
        if ( !rl_valid_parameter( param ) )
            return 0;

        if ( rl_get_parameter_type( param ) != rl_param_type.rl_param_string )
            return 0;
        
        return rl_params[ param ].string_param.value;
    }

    String rl_get_parameter( int param, String  test)
    {
        if ( !rl_valid_parameter( param ) )
            return null;
        
        if ( rl_get_parameter_type( param ) != rl_param_type.rl_param_string )
            return null;
        
        return rl_params[ param ].string_param.to_str.execute( rl_params[ param ].string_param.value );
    }

    double rl_get_parameter( int param )
    {
        if ( !rl_valid_parameter( param ) )
            return 0;
        
        if ( rl_get_parameter_type( param ) != rl_param_type.rl_param_number )
            return 0;
        
        return rl_params[ param ].number_param.value;
    }    
    
    /// rl_valid_parameter_value
    boolean rl_valid_parameter_value( String name, double new_val )
    {
        int param = rl_convert_parameter( name );
        if ( param == RL_PARAMS )
            return false;
        
        if ( rl_get_parameter_type( param ) != rl_param_type.rl_param_number )
            return false;
        
        return rl_params[ param ].number_param.val_func.execute( new_val );
    }

    boolean rl_valid_parameter_value( String name, String new_val )
    {
        int param = rl_convert_parameter( name );
        if ( param == RL_PARAMS )
            return false;
        
        if ( rl_get_parameter_type( param ) != rl_param_type.rl_param_string )
            return false;
        
        return rl_params[ param ].string_param.val_func.execute( rl_params[ param ].string_param.from_str.execute( new_val ) );
    }

    boolean rl_valid_parameter_value( String name, int new_val )
    {
        int param = rl_convert_parameter( name );
        if ( param == RL_PARAMS )
            return false;
        
        if ( rl_get_parameter_type( param ) != rl_param_type.rl_param_string )
            return false;
        
        return rl_params[ param ].string_param.val_func.execute( new_val );
    }

    //

    boolean rl_valid_parameter_value( int param, double new_val )
    {
        if ( !rl_valid_parameter( param ) )
            return false;
        
        if ( rl_get_parameter_type( param ) != rl_param_type.rl_param_number )
            return false;
        
        return rl_params[ param ].number_param.val_func.execute( new_val );
    }

    boolean rl_valid_parameter_value( int param, String new_val )
    {
        if ( !rl_valid_parameter( param ) )
            return false;
        
        if ( rl_get_parameter_type( param ) != rl_param_type.rl_param_string )
            return false;
        
        return rl_params[ param ].string_param.val_func.execute( rl_params[ param ].string_param.from_str.execute( new_val ) );
    }

    boolean rl_valid_parameter_value( int param, int new_val )
    {
        if ( !rl_valid_parameter( param ) )
            return false;
        
        if ( rl_get_parameter_type( param ) != rl_param_type.rl_param_string )
            return false;
        
        return rl_params[ param ].string_param.val_func.execute( new_val );
    }
    
    boolean rl_set_parameter(String name, double new_val )
    {
        int param = rl_convert_parameter(name );
        if ( param == RL_PARAMS )
            return false;
        
        if ( !rl_valid_parameter_value(param, new_val ) )
            return false;
        
        rl_params[ param ].number_param.value = new_val;

        return true;
    }

    boolean rl_set_parameter(String name, String new_val )
    {
        int param = rl_convert_parameter(name );
        if ( param == RL_PARAMS )
            return false;
        
        if ( !rl_valid_parameter_value(param, new_val ) )
            return false;

        int converted_val = rl_params[ param ].string_param.from_str.execute( new_val );

        // learning special case
        if ( param == RL_PARAM_LEARNING )
        {
            enabled = converted_val != 0;
            
            if ( ( converted_val == RL_LEARNING_ON ) && rl_first_switch )
            {
                rl_first_switch = false;
                my_agent.exploration.exploration_set_policy(Exploration.Policy.USER_SELECT_E_GREEDY );
                
                String msg = "Exploration Mode changed to epsilon-greedy";
                my_agent.getPrinter().print( msg );
                //xml_generate_message(const_cast<char *>( msg ) );
            }
        }
        
        rl_params[ param ].string_param.value = converted_val;

        return true;
    }

    boolean rl_set_parameter(String name, int new_val )
    {
        int param = rl_convert_parameter(name );
        if ( param == RL_PARAMS )
            return false;
        
        if ( !rl_valid_parameter_value(param, new_val ) )
            return false;

        // learning special case
        if ( param == RL_PARAM_LEARNING )
        {
            enabled = new_val != 0;
            
            if ( ( new_val == RL_LEARNING_ON ) && rl_first_switch )
            {
                rl_first_switch = false;
                my_agent.exploration.exploration_set_policy(Exploration.Policy.USER_SELECT_E_GREEDY );
                
                String msg = "Exploration Mode changed to epsilon-greedy";
                my_agent.getPrinter().print(msg);
                //xml_generate_message(const_cast<char *>( msg ) );
            }
        }
        
        rl_params[ param ].string_param.value = new_val;

        return true;
    }

    //

    boolean rl_set_parameter(int param, double new_val )
    {
        if ( !rl_valid_parameter_value(param, new_val ) )
            return false;
        
        rl_params[ param ].number_param.value = new_val;

        return true;
    }

    boolean rl_set_parameter(int param, String new_val )
    {
        if ( !rl_valid_parameter_value(param, new_val ) )
            return false;

        int converted_val = rl_params[ param ].string_param.from_str.execute( new_val );

        // learning special case
        if ( param == RL_PARAM_LEARNING )
        {
            enabled = converted_val != 0;
            
            if ( ( converted_val == RL_LEARNING_ON ) && rl_first_switch )
            {
                rl_first_switch = false;
                my_agent.exploration.exploration_set_policy(Exploration.Policy.USER_SELECT_E_GREEDY );
                
                String msg = "Exploration Mode changed to epsilon-greedy";
                my_agent.getPrinter().print(msg);
                //xml_generate_message(const_cast<char *>( msg ) );
            }
        }
        
        rl_params[ param ].string_param.value = converted_val;

        return true;
    }

    boolean rl_set_parameter(int param, int new_val )
    {   
        if ( !rl_valid_parameter_value(param, new_val ) )
            return false;

        // learning special case
        if ( param == RL_PARAM_LEARNING )
        {
            enabled = new_val != 0;
            
            if ( ( new_val == RL_LEARNING_ON ) && rl_first_switch )
            {
                rl_first_switch = false;
                my_agent.exploration.exploration_set_policy(Exploration.Policy.USER_SELECT_E_GREEDY );
                
                String msg = "Exploration Mode changed to epsilon-greedy";
                my_agent.getPrinter().print(msg);
                //xml_generate_message(const_cast<char *>( msg ) );
            }
        }
        
        rl_params[ param ].string_param.value = new_val;

        return true;
    }   
    
    /**
     * reinforcement_learning.cpp:495:rl_validate_learning
     * 
     * @param new_val
     * @return
     */
    boolean rl_validate_learning( int new_val )
    {
        return ( ( new_val == RL_LEARNING_ON ) || ( new_val == RL_LEARNING_OFF ) );
    }
    
    /**
     * reinforcement_learning.cpp:503:rl_convert_learning
     * 
     * @param val
     * @return
     */
    String rl_convert_learning( int val )
    {
        String return_val = null;
        
        switch ( val )
        {
            case RL_LEARNING_ON:
                return_val = "on";
                break;
                
            case RL_LEARNING_OFF:
                return_val = "off";
                break;
        }
        
        return return_val;
    }  
    
    /**
     * reinforcement_learning.cpp:521:rl_convert_learning
     * 
     * @param val
     * @return
     */
    int rl_convert_learning( String val )
    {
        int return_val = 0;
        
        if ("on".equals(val))
            return_val = RL_LEARNING_ON;
        else if ("off".equals(val))
            return_val = RL_LEARNING_OFF;
        
        return return_val;
    }    
    
    /**
     * reinforcement_learning.cpp:543:rl_validate_discount
     * 
     * @param new_val
     * @return
     */
    static boolean rl_validate_discount( double new_val )
    {
        return ( ( new_val >= 0 ) && ( new_val <= 1 ) );
    }
    static boolean rl_validate_learning_rate( double new_val )
    {
        return ( ( new_val >= 0 ) && ( new_val <= 1 ) );
    }

    /////////////////////////////////////////////////////////////////////////////////////////////////////////
    /////////////////////////////////////////////////////////////////////////////////////////////////////////

    /////////////////////////////////////////////////////////////////////////////////////////////////////////
    // learning policy
    /////////////////////////////////////////////////////////////////////////////////////////////////////////

    /***************************************************************************
     * Function     : rl_validate_learning_policy
     **************************************************************************/
    static boolean rl_validate_learning_policy( int new_val )
    {
        return ( ( new_val == RL_LEARNING_SARSA ) || ( new_val == RL_LEARNING_Q ) );
    }

    /***************************************************************************
     * Function     : rl_convert_learning_policy
     **************************************************************************/
    static String rl_convert_learning_policy( int val )
    {
        String return_val = null;
        
        switch ( val )
        {
            case RL_LEARNING_SARSA:
                return_val = "sarsa";
                break;
                
            case RL_LEARNING_Q:
                return_val = "q-learning";
                break;
        }
        
        return return_val;
    }

    static int rl_convert_learning_policy( String val )
    {
        int return_val = 0;
        
        if ("sarsa".equals(val))
            return_val = RL_LEARNING_SARSA;
        else if ("q-learning".equals(val))
            return_val = RL_LEARNING_Q;
        
        return return_val;
    }

    /////////////////////////////////////////////////////////////////////////////////////////////////////////
    /////////////////////////////////////////////////////////////////////////////////////////////////////////

    /////////////////////////////////////////////////////////////////////////////////////////////////////////
    // eligibility trace decay rate
    /////////////////////////////////////////////////////////////////////////////////////////////////////////

    /***************************************************************************
     * Function     : rl_validate_decay_rate
     **************************************************************************/
    static boolean rl_validate_decay_rate( double new_val )
    {
        return ( ( new_val >= 0 ) && ( new_val <= 1 ) );
    }

    /////////////////////////////////////////////////////////////////////////////////////////////////////////
    /////////////////////////////////////////////////////////////////////////////////////////////////////////

    /////////////////////////////////////////////////////////////////////////////////////////////////////////
    // eligibility trace tolerance
    /////////////////////////////////////////////////////////////////////////////////////////////////////////

    /***************************************************************************
     * Function     : rl_validate_trace_tolerance
     **************************************************************************/
    static boolean rl_validate_trace_tolerance( double new_val )
    {
        return ( new_val > 0 );
    }

    /////////////////////////////////////////////////////////////////////////////////////////////////////////
    /////////////////////////////////////////////////////////////////////////////////////////////////////////

    /////////////////////////////////////////////////////////////////////////////////////////////////////////
    // temporal-extension
    /////////////////////////////////////////////////////////////////////////////////////////////////////////

    /***************************************************************************
     * Function     : rl_validate_te_enabled
     **************************************************************************/
    static boolean rl_validate_te_enabled( int new_val )
    {
        return ( ( new_val == RL_TE_ON ) || ( new_val == RL_TE_OFF ) );
    }

    /***************************************************************************
     * Function     : rl_convert_te_enabled
     **************************************************************************/
    static String rl_convert_te_enabled( int val )
    {
        String return_val = null;
        
        switch ( val )
        {
            case RL_TE_ON:
                return_val = "on";
                break;
                
            case RL_TE_OFF:
                return_val = "off";
                break;
        }
        
        return return_val;
    }

    static int rl_convert_te_enabled( String val )
    {
        int return_val = 0;
        
        if ("on".equals(val))
            return_val = RL_TE_ON;
        else if ("off".equals(val))
            return_val = RL_TE_OFF;
        
        return return_val;
    }
    
    /**
     * reinforcement_learning.cpp:695:rl_enabled
     * 
     * @return
     */
    public boolean rl_enabled()
    {
        return enabled;
    }

    rl_stat rl_add_stat( String name )
    {
        // new stat entry
        rl_stat newbie = new rl_stat();
        newbie.name = name;
        newbie.value = 0;
        
        return newbie;
    }

    /***************************************************************************
     * Function     : rl_convert_stat
     **************************************************************************/
    int  rl_convert_stat( String name )
    {
        for ( int i=0; i<RL_STATS; i++ )
            if ( name.equals(rl_stats[ i ].name ) )
                return i;

        return RL_STATS;
    }

    String rl_convert_stat( int  stat )
    {
        if ( ( stat < 0 ) || ( stat >= RL_STATS ) )
            return null;

        return rl_stats[ stat ].name;
    }

    /***************************************************************************
     * Function     : rl_valid_stat
     **************************************************************************/
    boolean rl_valid_stat( String name )
    {
        return ( rl_convert_stat(name ) != RL_STATS );
    }

    boolean rl_valid_stat( int  stat )
    {
        return ( rl_convert_stat(stat ) != null );
    }

    /***************************************************************************
     * Function     : rl_get_stat
     **************************************************************************/
    double rl_get_stat( String name )
    {
        int  stat = rl_convert_stat(name );
        if ( stat == RL_STATS )
            return 0;

        return rl_stats[ stat ].value;
    }

    double rl_get_stat( int  stat )
    {
        if ( !rl_valid_stat(stat ) )
            return 0;

        return rl_stats[ stat ].value;
    }

    /***************************************************************************
     * Function     : rl_set_stat
     **************************************************************************/
    boolean rl_set_stat( String name, double new_val )
    {
        int  stat = rl_convert_stat(name );
        if ( stat == RL_STATS )
            return false;
        
        rl_stats[ stat ].value = new_val;
        
        return true;
    }

    boolean rl_set_stat( int  stat, double new_val )
    {
        if ( !rl_valid_stat(stat ) )
            return false;
        
        rl_stats[ stat ].value = new_val;
        
        return true;
    }
    
    /**
     * reinforcement_learning.cpp:793:rl_valid_template
     * 
     * @param prod
     * @return
     */
    static boolean rl_valid_template( Production prod )
    {
        boolean numeric_pref = false;
        boolean var_pref = false;
        int num_actions = 0;

        for ( Action a = prod.action_list; a != null; a = a.next ) 
        {
            num_actions++;
            
            MakeAction ma = a.asMakeAction();
            if (ma != null)
            {
                if ( a.preference_type == PreferenceType.NUMERIC_INDIFFERENT )
                {
                    numeric_pref = true;
                }
                else if ( a.preference_type == PreferenceType.BINARY_INDIFFERENT )
                {   
                    RhsSymbolValue asSym = ma.referent.asSymbolValue();
                    if ( asSym != null && asSym.getSym().asVariable() != null)
                        var_pref = true;
                }
            }
        }

        return ( ( num_actions == 1 ) && ( numeric_pref || var_pref ) );
    }
    
    /**
     * reinforcement_learning.cpp:822:rl_valid_rule
     * 
     * @param prod
     * @return
     */
    static boolean rl_valid_rule( Production prod )
    {
        boolean numeric_pref = false;
        int num_actions = 0;

        for ( Action a = prod.action_list; a != null; a = a.next ) 
        {
            num_actions++;
            
            MakeAction ma = a.asMakeAction();
            if (ma != null)
            {
                if ( a.preference_type == PreferenceType.NUMERIC_INDIFFERENT)
                    numeric_pref = true;
            }
        }

        return ( numeric_pref && ( num_actions == 1 ) );
    }
    
    /**
     * misc.cpp:24;is_natural_number
     * 
     * @param s
     * @return
     */
    private static boolean is_natural_number(String s)
    {
        for(int i = 0; i < s.length(); ++i)
        {
            if(!Character.isDigit(s.charAt(i)))
            {
                return false;
            }
        }
        return true;
    }
    
    static int rl_get_template_id( String prod_name )
    {
        String temp = prod_name;
        
        // has to be at least "rl*a*#" (where a is a single letter/number/etc)
        if ( temp.length() < 6 )
            return -1;
        
        // check first three letters are "rl*"
        if (! temp.startsWith( "rl*" ) )
            return -1;
        
        // find last * to isolate id
        int last_star = temp.lastIndexOf( '*' );
        if ( last_star == -1 )
            return -1;
        
        // make sure there's something left after last_star
        if ( last_star == ( temp.length() - 1 ) )
            return -1;
        
        // make sure id is a valid natural number
        String id_str = temp.substring( last_star + 1 );
        if ( !is_natural_number( id_str ) )
            return -1;
        
        // convert id
        return Integer.parseInt(id_str);
    }
    void rl_initialize_template_tracking()
    {
        rl_template_count = 1;
    }

    /***************************************************************************
     * Function     : rl_update_template_tracking
     **************************************************************************/
    void rl_update_template_tracking(String rule_name )
    {
        int new_id = rl_get_template_id( rule_name );

        if ( ( new_id != -1 ) && ( new_id > rl_template_count ) )
            rl_template_count = ( new_id + 1 );
    }

    /***************************************************************************
     * Function     : rl_next_template_id
     **************************************************************************/
    int rl_next_template_id( )
    {
        return (rl_template_count++);
    }

    /***************************************************************************
     * Function     : rl_revert_template_id
     **************************************************************************/
    void rl_revert_template_id( )
    {
        rl_template_count--;
    }


    public Symbol rl_build_template_instantiation( Instantiation my_template_instance, Token tok, WmeImpl w )
    {
        Production my_template = my_template_instance.prod;
        MakeAction my_action = my_template.action_list.asMakeAction();

        boolean chunk_var = my_agent.chunker.variablize_this_chunk;

        // get the preference value
        IdentifierImpl id = my_agent.recMemory.instantiate_rhs_value( my_action.id, -1, 's', tok, w ).asIdentifier();
        SymbolImpl attr = my_agent.recMemory.instantiate_rhs_value( my_action.attr, id.level, 'a', tok, w );
        char first_letter = attr.getFirstLetter();
        SymbolImpl value = my_agent.recMemory.instantiate_rhs_value( my_action.value, id.level, first_letter, tok, w );
        SymbolImpl referent = my_agent.recMemory.instantiate_rhs_value( my_action.referent, id.level, first_letter, tok, w );

        double init_value = 0;
        if ( referent.asInteger() != null )
            init_value = (double) referent.asInteger().getValue();
        else if (referent.asDouble() != null)
            init_value = referent.asDouble().getValue();

        // make new action list
        // small hack on variablization: the artificial tc gets dealt with later, just needs to be explicit non-zero
        my_agent.chunker.variablize_this_chunk = true;
        my_agent.chunker.variablization_tc = -1; // TODO ??? (0u - 1);
        MakeAction new_action = rl_make_simple_action( id, attr, value, referent );
        new_action.preference_type = PreferenceType.NUMERIC_INDIFFERENT;

        // make unique production name
        StringSymbol new_name_symbol;
        String new_name = "";
        String empty_string = "";
        String temp_id;
        int new_id = 0;
        do
        {
            new_id = rl_next_template_id( );
            new_name = ( "rl*" + empty_string + my_template.getName().getValue() + "*" + new_id );
        } while (my_agent.getSymbols().findString(new_name) != null);
        new_name_symbol = my_agent.getSymbols().createString(new_name);
        
        // prep conditions
        ByRef<Condition> cond_top = ByRef.create(null);
        ByRef<Condition> cond_bottom = ByRef.create(null);
        Condition.copy_condition_list( my_template_instance.top_of_instantiated_conditions, cond_top, cond_bottom );
        rl_add_goal_or_impasse_tests_to_conds( cond_top.value );
        my_agent.variableGenerator.reset( cond_top.value, null );
        my_agent.chunker.variablization_tc = my_agent.syms.get_new_tc_number( );
        my_agent.chunker.variablize_condition_list( cond_top.value );
        my_agent.chunker.variablize_nots_and_insert_into_conditions( my_template_instance.nots, cond_top.value );

        // make new production
        Production new_production = new Production( ProductionType.USER, new_name_symbol, cond_top.value, cond_bottom.value, new_action);
        try
        {
            ((DefaultProductionManager)my_agent.getProductions()).addProduction(new_production, false);
        }
        catch (ReordererException e)
        {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
        my_agent.chunker.variablize_this_chunk = chunk_var;

        /*
        // attempt to add to rete, remove if duplicate
        if ( add_production_to_rete(new_production, cond_top, NULL, FALSE, TRUE ) == DUPLICATE_PRODUCTION )
        {
            excise_production(new_production, false );
            rl_revert_template_id( );

            new_name_symbol = null;
        }
        */

        return new_name_symbol;
    }
    
    /**
     * reinforcement_learning.cpp:992:rl_make_simple_action
     * 
     * @param id_sym
     * @param attr_sym
     * @param val_sym
     * @param ref_sym
     * @return
     */
    MakeAction rl_make_simple_action( IdentifierImpl id_sym, SymbolImpl attr_sym, SymbolImpl val_sym, SymbolImpl ref_sym )
    {
        MakeAction rhs = new MakeAction();

        // id
        rhs.id = new RhsSymbolValue(my_agent.chunker.variablize_symbol( id_sym ));
        
        // attribute
        rhs.attr = new RhsSymbolValue(my_agent.chunker.variablize_symbol(attr_sym));

        // value
        rhs.value = new RhsSymbolValue(my_agent.chunker.variablize_symbol(val_sym));

        // referent
        rhs.referent = new RhsSymbolValue(my_agent.chunker.variablize_symbol(ref_sym));

        return rhs;
    }
    
    void rl_add_goal_or_impasse_tests_to_conds( Condition all_conds )
    {
        // mark each id as we add a test for it, so we don't add a test for the same id in two different places
        int tc = my_agent.syms.get_new_tc_number( );

        for (Condition cond = all_conds; cond != null; cond = cond.next )
        {
            PositiveCondition pc = cond.asPositiveCondition();
            if ( pc == null )
                continue;

            IdentifierImpl id = pc.id_test.asEqualityTest().getReferent().asIdentifier();

            if ( ( id.isa_goal || id.isa_impasse ) && ( id.tc_number != tc ) ) 
            {
                Test ct = id.isa_goal ? GoalIdTest.INSTANCE : ImpasseIdTest.INSTANCE;
                pc.id_test = Tests.add_new_test_to_test(pc.id_test, ct );
                id.tc_number = tc;
            }
        }
    }
    
    /**
     * reinforcement_learning.cpp:1060:rl_tabulate_reward_value_for_goal
     * @param goal
     */
    public void rl_tabulate_reward_value_for_goal(IdentifierImpl goal )
    {
        ReinforcementLearningInfo data = goal.rl_info;

        // Only count rewards at top state... 
        // or for op no-change impasses.
        //if ( ( data.impasse_type != NONE_IMPASSE_TYPE ) && ( data.impasse_type != OP_NO_CHANGE_IMPASSE_TYPE ) )  
        //  return;
        
        final ListHead<Slot> slots = goal.reward_header.asIdentifier().slots;
        double reward = 0.0;
        int reward_count = 0;

        if (!slots.isEmpty())
        {
            for (Slot s : slots)
            {
                for (WmeImpl w = s.getWmes(); w != null; w = w.next)
                {
                    IdentifierImpl id = w.value.asIdentifier();
                    if (id != null)
                    {
                        for (Slot t : id.slots)
                        {
                            for (WmeImpl x = t.getWmes(); x != null; x = x.next )
                            {
                                Symbol value = x.value;
                                if(value.asInteger() != null)
                                {
                                    reward = reward + value.asInteger().getValue();
                                    reward_count++;
                                }
                                else if(value.asDouble() != null)
                                {
                                    reward = reward + value.asDouble().getValue();
                                    reward_count++;
                                }
                            }
                        }
                    }
                }
            }
            
            data.reward += rl_discount_reward( reward, data.step );
        }

        // update stats
        double global_reward = rl_get_stat( RL_STAT_GLOBAL_REWARD );
        rl_set_stat( RL_STAT_TOTAL_REWARD, reward );
        rl_set_stat( RL_STAT_GLOBAL_REWARD, ( global_reward + reward ) );

        data.step++;
    }
    
    /**
     * reinforcement_learning:1101:rl_tabulate_reward_values()
     */
    public void rl_tabulate_reward_values()
    {
        IdentifierImpl goal = my_agent.decider.top_goal;

        while( goal != null)
        {
            rl_tabulate_reward_value_for_goal( goal );
            goal = goal.lower_goal;
        }
    }
    
    /**
     * reinforcement_learning.cpp:1115:rl_discount_reward
     * 
     * @param reward
     * @param step
     * @return
     */
    double rl_discount_reward( double reward, int step )
    {
        double rate = rl_get_parameter( RL_PARAM_DISCOUNT_RATE );

        return ( reward * Math.pow( rate, (double) step ) );
    }
    
    /**
     * reinforcement_learning.cpp:1125:rl_store_data
     * 
     * @param goal
     * @param value
     */
    public void rl_store_data(IdentifierImpl goal, Preference cand)
    {
        ReinforcementLearningInfo data = goal.rl_info;
        Symbol op = cand.value;
        data.previous_q = cand.numeric_value;

        boolean using_gaps = ( rl_get_parameter( RL_PARAM_TEMPORAL_EXTENSION, RL_RETURN_LONG ) == RL_TE_ON );
        
        // Make list of just-fired prods
        int just_fired = 0;
        for ( Preference pref = goal.operator_slot.getPreferencesByType(PreferenceType.NUMERIC_INDIFFERENT); pref != null; pref = pref.next )
            if ( ( op == pref.value ) && pref.inst.prod.rl_rule)
                if ( pref.inst.prod.rl_rule) 
                {
                    if ( just_fired == 0)
                    {
                        data.prev_op_rl_rules.clear();
                        // TODO should num_prev_op_rl_rules be set to 0 here?
                    }
                    
                    data.prev_op_rl_rules.push(pref.inst.prod);
                    just_fired++;
                }

        if ( just_fired != 0)
        {
            if ( data.reward_age != 0 )
            {
                my_agent.trace.print(Category.RL, "WARNING: gap ended (%s)", goal);
                
                //xml_generate_warning(buf );
            }
            
            data.reward_age = 0;
            data.num_prev_op_rl_rules = just_fired;
        }
        else
        {
            if ( data.reward_age == 0 )
            {
                my_agent.trace.print(Category.RL, "WARNING: gap started (%s)", goal);
                
                //xml_generate_warning(buf );
            }
            
            if ( !using_gaps )
            {
                data.prev_op_rl_rules.clear();
                data.num_prev_op_rl_rules = 0;
            }
            
            data.reward_age++;
        }
    }

    /**
     * reinforcement_learning.cpp:1193:rl_perform_update
     * 
     * @param i
     * @param g
     */
    public void rl_perform_update(double op_value, IdentifierImpl goal)
    {
        ReinforcementLearningInfo data = goal.rl_info;

        boolean using_gaps = ( rl_get_parameter( RL_PARAM_TEMPORAL_EXTENSION, RL_RETURN_LONG ) == RL_TE_ON );

        double alpha = rl_get_parameter( RL_PARAM_LEARNING_RATE );
        double lambda = rl_get_parameter(RL_PARAM_ET_DECAY_RATE );
        double gamma = rl_get_parameter(RL_PARAM_DISCOUNT_RATE );
        double tolerance = rl_get_parameter(RL_PARAM_ET_TOLERANCE );

        // compute TD update, set stat
        double update = data.reward;

        if ( using_gaps )
            update *= Math.pow( gamma, (double) data.reward_age );

        update += ( Math.pow( gamma, (double) data.step ) * op_value );
        update -= data.previous_q;
        rl_set_stat( RL_STAT_UPDATE_ERROR, (double) ( -update ) );

        // Iterate through eligibility_traces, decay traces. If less than TOLERANCE, remove from map.
        if ( lambda == 0 )
        {
            if ( !data.eligibility_traces.isEmpty() )
                data.eligibility_traces.clear();
        }
        else
        {
            Iterator<Map.Entry<Production, Double>> it = data.eligibility_traces.entrySet().iterator();
            while(it.hasNext())
            {
                Map.Entry<Production, Double> e = it.next();
                e.setValue(e.getValue() * lambda);
                e.setValue(e.getValue() * Math.pow(gamma, data.step));
                if(e.getValue() < tolerance)
                {
                    it.remove();
                }
            }
        }
        
        // Update trace for just fired prods
        if ( data.num_prev_op_rl_rules != 0)
        {
            double trace_increment = ( 1.0 / data.num_prev_op_rl_rules );
            
            for (Production c : data.prev_op_rl_rules)
            {
                if ( c != null )
                {
                    Double iter = data.eligibility_traces.get(c);
                    if(iter != null)
                    {
                        iter += trace_increment;
                        data.eligibility_traces.put(c, iter);
                    }
                    else
                    {
                        data.eligibility_traces.put(c, trace_increment);
                    }
                }
            }
        }
        
        // For each prod in map, add alpha*delta*trace to value
        for (Map.Entry<Production, Double> iter : data.eligibility_traces.entrySet())
        {   
            Production prod = iter.getKey();
            Symbol referent = prod.action_list.asMakeAction().referent.asSymbolValue().getSym();
            double temp = 0.0;
            if(referent.asInteger() != null)
            {
                temp = referent.asInteger().getValue();
            }
            else if(referent.asDouble() != null)
            {
                temp = referent.asDouble().getValue();
            }

            // update is applied depending upon type of accumulation mode
            // sum: add the update to the existing value
            // avg: average the update with the existing value
            temp += ( update * alpha * iter.getValue() );      

            // Change value of rule
            prod.action_list.asMakeAction().referent = new RhsSymbolValue(my_agent.syms.createDouble(temp));
            prod.rl_update_count += 1;

            // Change value of preferences generated by current instantiations of this rule
            if ( !prod.instantiations.isEmpty() )
            {
                for ( Instantiation inst : prod.instantiations)
                {
                    for ( Preference pref : inst.preferences_generated)
                    {
                        pref.referent = my_agent.syms.createDouble(temp);
                    }
                }
            }   
        }

        data.reward = 0.0;
        data.step = 0;
        data.impasse_type = ImpasseType.NONE;
    }
    
    public static void rl_watkins_clear(IdentifierImpl goal )
    {
        ReinforcementLearningInfo data = goal.rl_info;
        // rl_et_map::iterator iter;
        
        // Iterate through eligibility_traces, remove traces
        // TODO: Why not clear?
//        for ( iter = data->eligibility_traces->begin(); iter != data->eligibility_traces->end(); )
//            data->eligibility_traces->erase( iter++ );
        data.eligibility_traces.clear();
    }
    

    /**
     * Function introduced while trying to tease apart production construction
     * 
     * <p>production.cpp:1507:make_production
     * 
     * @param p
     */
    public void addProduction(Production p)
    {
        // Soar-RL stuff
        p.rl_update_count = 0;
        p.rl_rule = false;
        if ( ( p.getType() != ProductionType.JUSTIFICATION ) && ( p.getType() != ProductionType.TEMPLATE ) ) 
        {
            p.rl_rule = rl_valid_rule( p );  
        }
        rl_update_template_tracking( p.getName().getValue() );
        
        // TODO - parser.cpp
        if ( p.getType() == ProductionType.TEMPLATE )
        {
            if ( !rl_valid_template( p ) )
            {
                my_agent.getPrinter().print("Invalid Soar-RL template (%s)\n\n", p.getName() );
                my_agent.getProductions().exciseProduction( p, false );
                
                // TODO: Throw exception?
                return;
            }
        }
    }
    
    /**
     * Function introduced while teasing apary excise functionality
     * 
     * <p>production.cpp:1595:excise_production
     * 
     * @param prod
     */
    public void exciseProduction(Production prod)
    {
        // Remove RL-related pointers to this production (unnecessary if rule never fired).
        if ( prod.rl_rule && prod.firing_count != 0 ) 
            rl_remove_refs_for_prod( prod ); 
    }
    
}
