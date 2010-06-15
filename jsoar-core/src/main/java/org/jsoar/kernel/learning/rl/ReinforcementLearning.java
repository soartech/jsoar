/*
 * Copyright (c) 2008  Dave Ray <daveray@gmail.com>
 *
 * Created on Sep 12, 2008
 */
package org.jsoar.kernel.learning.rl;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.ListIterator;
import java.util.Map;

import org.jsoar.kernel.Agent;
import org.jsoar.kernel.Decider;
import org.jsoar.kernel.ImpasseType;
import org.jsoar.kernel.PredefinedSymbols;
import org.jsoar.kernel.Production;
import org.jsoar.kernel.ProductionType;
import org.jsoar.kernel.VariableGenerator;
import org.jsoar.kernel.exploration.Exploration;
import org.jsoar.kernel.learning.Chunker;
import org.jsoar.kernel.lhs.Condition;
import org.jsoar.kernel.lhs.GoalIdTest;
import org.jsoar.kernel.lhs.ImpasseIdTest;
import org.jsoar.kernel.lhs.PositiveCondition;
import org.jsoar.kernel.lhs.Test;
import org.jsoar.kernel.lhs.Tests;
import org.jsoar.kernel.lhs.ThreeFieldCondition;
import org.jsoar.kernel.memory.Instantiation;
import org.jsoar.kernel.memory.Preference;
import org.jsoar.kernel.memory.PreferenceType;
import org.jsoar.kernel.memory.RecognitionMemory;
import org.jsoar.kernel.memory.Slot;
import org.jsoar.kernel.memory.WmeImpl;
import org.jsoar.kernel.rete.ConditionsAndNots;
import org.jsoar.kernel.rete.ProductionAddResult;
import org.jsoar.kernel.rete.Rete;
import org.jsoar.kernel.rete.Token;
import org.jsoar.kernel.rhs.Action;
import org.jsoar.kernel.rhs.MakeAction;
import org.jsoar.kernel.rhs.RhsSymbolValue;
import org.jsoar.kernel.rhs.functions.RhsFunctions;
import org.jsoar.kernel.symbols.IdentifierImpl;
import org.jsoar.kernel.symbols.Symbol;
import org.jsoar.kernel.symbols.SymbolFactoryImpl;
import org.jsoar.kernel.symbols.SymbolImpl;
import org.jsoar.kernel.symbols.Symbols;
import org.jsoar.kernel.tracing.Trace.Category;
import org.jsoar.util.ByRef;
import org.jsoar.util.DefaultSourceLocation;
import org.jsoar.util.SourceLocation;
import org.jsoar.util.adaptables.Adaptables;
import org.jsoar.util.markers.DefaultMarker;
import org.jsoar.util.markers.Marker;

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
        
    // more specific forms of no change impasse types
    // made negative to never conflict with impasse constants
    public static final int STATE_NO_CHANGE_IMPASSE_TYPE = -1;
    public static final int OP_NO_CHANGE_IMPASSE_TYPE = -2;
    private static final SourceLocation NEW_PRODUCTION_SOURCE = new DefaultSourceLocation("*RL*", -1, -1);

    // reinforcement learning
    int rl_template_count;
    boolean rl_first_switch;

    private final Agent my_agent;
    private Decider decider;
    private Exploration exploration;
    private Chunker chunker;
    private RecognitionMemory recMemory;
    private Rete rete; // TODO init
    private PredefinedSymbols preSyms; // TODO init
    
    private boolean enabled = false;
    
    IdentifierImpl rl_sym_reward_link;
    SymbolImpl rl_sym_reward;
    SymbolImpl rl_sym_value;

    /**
     * @param context
     */
    public ReinforcementLearning(Agent context)
    {
        this.my_agent = context;
        
    }
    
    /* 
     * reinforcement_learning.cpp:688:rl_reset_data
     * 
     * Not actually used in reinforcement_learning.cpp.
     */

    
    public void initialize()
    {
        this.decider = Adaptables.adapt(my_agent, Decider.class);
        this.exploration = Adaptables.adapt(my_agent, Exploration.class);
        this.chunker = Adaptables.adapt(my_agent, Chunker.class);
        this.recMemory = Adaptables.adapt(my_agent, RecognitionMemory.class);
        this.rete = Adaptables.adapt(my_agent, Rete.class);
        this.preSyms = Adaptables.adapt(my_agent, PredefinedSymbols.class);
        
        // rl initialization
        // agent.cpp:328:create_soar_agent
        // TODO init params
        // TODO init stats
        rl_initialize_template_tracking();
        
        rl_first_switch = true;

    }
    
    
    /**
     * reinforcement_learning.cpp:131:rl_enabled
     * (9.3.0)
     * 
     * @return true if RL is enabled
     */
    public boolean rl_enabled()
    {
        return enabled;
    }
    
    /**
     * reinforcement_learning.cpp:158:rl_remove_refs_for_prod
     * (9.3.0)
     * @param prod
     */
    private void rl_remove_refs_for_prod(Production prod )
    {
        for ( IdentifierImpl state = decider.top_state; state != null; state = state.lower_goal )
        {
            state.rl_info.eligibility_traces.remove( prod );
            
            final ListIterator<Production> it =  state.rl_info.prev_op_rl_rules.listIterator();
            while(it.hasNext())
            {
                final Production c = it.next();
                if(c == prod)
                {
                    it.set(null);
                }
            }
        }
    }

    /**
     * reinforcement_learning.cpp:180:rl_valid_template
     * (9.3.0)
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
     * reinforcement_learning.cpp:207:rl_valid_rule
     * (9.3.0)
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
    
    /**
     * reinforcement_learning.cpp:230:rl_get_template_id
     * 
     * gets the auto-assigned id of a template instantiation
     * 
     * (9.3.0)
     * 
     * @param prod_name
     * @return
     */
    private static int rl_get_template_id( String prod_name )
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
    
    
    /**
     * initializes the max rl template counter
     * 
     * reinforcement_learning.cpp:263:rl_initialize_template_tracking
     * (9.3.0)
     */
    public void rl_initialize_template_tracking()
    {
        rl_template_count = 1;
    }

    /**
     * updates rl template counter for a rule
     * 
     * reinforcement_learning.cpp:269:rl_update_template_tracking
     * 9.3.0
     * @param rule_name
     */
    private void rl_update_template_tracking(String rule_name )
    {
        int new_id = rl_get_template_id( rule_name );

        if ( ( new_id != -1 ) && ( new_id > rl_template_count ) )
            rl_template_count = ( new_id + 1 );
    }

    /**
     * gets the next template-assigned id
     * 
     * reinforcement_learning.cpp:278:rl_next_template_id
     * 9.3.0
     * @return
     */
    private int rl_next_template_id( )
    {
        return (rl_template_count++);
    }

    /**
     * gives back a template-assigned id (on auto-retract)
     * 
     * reinforcement_learning.cpp:278:rl_revert_template_id
     * 9.3.0
     */
    private void rl_revert_template_id( )
    {
        rl_template_count--;
    }
    
    /**
     * reinforcement_learning.cpp:289:rl_get_symbol_constant
     * 9.3.0
     * 
     * @param p_sym
     * @param i_sym
     * @param constants
     */
    static void rl_get_symbol_constant( SymbolImpl p_sym, SymbolImpl i_sym, Map<SymbolImpl, SymbolImpl> constants )
    {
        if(p_sym.asVariable() != null && (i_sym.asIdentifier() == null /*TODO|| i_sym->id.smem_lti != NIL */))
        {
            constants.put(p_sym, i_sym);
        }
    }

    /**
     * reinforcement_learning.cpp:297:rl_get_test_constant
     * 9.3.0
     * 
     * @param p_test
     * @param i_test
     * @param constants
     */
    static void rl_get_test_constant( Test p_test, Test i_test, Map<SymbolImpl, SymbolImpl> constants )
    {
        if (Tests.isBlank(p_test))
        {
            return;
        }
        
        if (p_test.asEqualityTest() != null)
        {       
            rl_get_symbol_constant(p_test.asEqualityTest().getReferent(), i_test.asEqualityTest().getReferent(), constants);
            
            // rl_get_symbol_constant( *(reinterpret_cast<Symbol**>( p_test )), *(reinterpret_cast<Symbol**>( i_test )), constants );
            return;
        }
        
        
        // complex test stuff
        // NLD: If the code below is uncommented, it accesses bad memory on the first
        //      id test and segfaults.  I'm honestly unsure why (perhaps something
        //      about state test?).  Most of this code was copied/adapted from
        //      the variablize_test code in production.cpp.
        /*
        {
            complex_test* p_ct = complex_test_from_test( *p_test );
            complex_test* i_ct = complex_test_from_test( *i_test ); 

            if ( ( p_ct->type == GOAL_ID_TEST ) || ( p_ct->type == IMPASSE_ID_TEST ) || ( p_ct->type == DISJUNCTION_TEST ) )
            {
                return;
            }
            else if ( p_ct->type == CONJUNCTIVE_TEST )
            {
                cons* p_c=p_ct->data.conjunct_list;
                cons* i_c=i_ct->data.conjunct_list;

                while ( p_c )
                {
                    rl_get_test_constant( reinterpret_cast<test*>( &( p_c->first ) ), reinterpret_cast<test*>( &( i_c->first ) ), constants );
                    
                    p_c = p_c->rest;
                    i_c = i_c->rest;
                }

                return;
            }
            else
            {
                rl_get_symbol_constant( p_ct->data.referent, i_ct->data.referent, constants );

                return;
            }
        }
        */
    }
    
    /**
     * reinforcement_learning.cpp:351:rl_get_template_constants
     * 9.3.0
     * 
     * @param p_conds
     * @param i_conds
     * @param constants
     */
    void rl_get_template_constants( Condition p_conds, Condition i_conds, Map<SymbolImpl, SymbolImpl> constants )
    {
        Condition p_cond = p_conds;
        Condition i_cond = i_conds;

        while ( p_cond != null )
        {
            final ThreeFieldCondition tfc = p_cond.asThreeFieldCondition();
            if ( tfc != null /* positive || negative */ )
            {
                rl_get_test_constant( tfc.id_test, i_cond.asThreeFieldCondition().id_test, constants );
                rl_get_test_constant( tfc.attr_test, i_cond.asThreeFieldCondition().attr_test, constants );
                rl_get_test_constant( tfc.value_test, i_cond.asThreeFieldCondition().value_test, constants );
            }
            else if (p_cond.asConjunctiveNegationCondition() != null)
            {
                rl_get_template_constants(p_cond.asConjunctiveNegationCondition().top, 
                                          i_cond.asConjunctiveNegationCondition().top, constants );
            }
            
            p_cond = p_cond.next;
            i_cond = i_cond.next;
        }
    }    

    /**
     * reinforcement_learning.cpp:375:rl_build_template_instantiation
     * 9.3.0
     * 
     * @param my_template_instance
     * @param tok
     * @param w
     * @return
     */
    public SymbolImpl rl_build_template_instantiation( Instantiation my_template_instance, Token tok, WmeImpl w )
    {
    SymbolImpl return_val = null;
    
    // initialize production conditions
    if ( my_template_instance.prod.rl_template_conds == null )
    {
        ConditionsAndNots cans = 
        rete.p_node_to_conditions_and_nots(my_template_instance.prod.getReteNode(), null, null, false);

        my_template_instance.prod.rl_template_conds = cans.top;
    }

    // initialize production instantiation set
    if ( my_template_instance.prod.rl_template_instantiations == null)
    {
        my_template_instance.prod.rl_template_instantiations = new HashSet<Map<SymbolImpl, SymbolImpl>>();
    }

    // get constants
    Map<SymbolImpl, SymbolImpl> constant_map = new HashMap<SymbolImpl, SymbolImpl>();
    {   
        rl_get_template_constants(my_template_instance.prod.rl_template_conds, 
                                  my_template_instance.top_of_instantiated_conditions, 
                                  constant_map);        
    }

    // try to insert into instantiation set
    //if ( !constant_map.empty() )
    {
        if ( my_template_instance.prod.rl_template_instantiations.add(constant_map))
        {
            Production my_template = my_template_instance.prod;
            Action my_action = my_template.action_list;

            boolean chunk_var = chunker.variablize_this_chunk;
            chunker.variablize_this_chunk = true;

            final SymbolFactoryImpl syms = chunker.variableGenerator.getSyms();
            // make unique production name
            String new_name = "";
            do
            {
                final int new_id = rl_next_template_id();
                new_name = "rl*" + my_template.getName() + "*" + new_id;
            } while (syms.findString(new_name) != null);
            SymbolImpl new_name_symbol = syms.createString(new_name);
            
            // prep conditions
            final ByRef<Condition> cond_top = ByRef.create(null);
            final ByRef<Condition> cond_bottom = ByRef.create(null);
            
            Condition.copy_condition_list(my_template_instance.top_of_instantiated_conditions, cond_top, cond_bottom );
            rl_add_goal_or_impasse_tests_to_conds( cond_top.value );
            chunker.variableGenerator.reset(cond_top.value, null);
            chunker.variablization_tc = DefaultMarker.create();
            chunker.variablize_condition_list( cond_top.value );
            chunker.variablize_nots_and_insert_into_conditions( my_template_instance.nots, cond_top.value );

            // get the preference value
            IdentifierImpl id = recMemory.instantiate_rhs_value( my_action.asMakeAction().id, -1, 's', tok, w ).asIdentifier();
            SymbolImpl attr = recMemory.instantiate_rhs_value( my_action.asMakeAction().attr, id.level, 'a', tok, w );
            char first_letter = attr.getFirstLetter();
            SymbolImpl value = recMemory.instantiate_rhs_value(my_action.asMakeAction().value, id.level, first_letter, tok, w );
            SymbolImpl referent = recMemory.instantiate_rhs_value(my_action.asMakeAction().referent, id.level, first_letter, tok, w );

            // make new action list
            Action new_action = rl_make_simple_action(id, attr, value, referent);
            new_action.preference_type = PreferenceType.NUMERIC_INDIFFERENT;

            // make new production
            Production new_production = new Production( ProductionType.USER, NEW_PRODUCTION_SOURCE, 
                                                        new_name_symbol.toString(), null,
                                                        cond_top.value, 
                                                        cond_bottom.value,
                                                        new_action );
            chunker.variablize_this_chunk = chunk_var; // restored to original value

            // set initial expected reward values
            {
                double init_value = 0.0;
                if (referent.asInteger() != null)
                {
                    init_value = referent.asInteger().getValue();
                }
                else if (referent.asDouble() != null)
                {
                    init_value = referent.asDouble().getValue();
                }

                new_production.rl_ecr = 0.0;
                new_production.rl_efr = init_value;
            }

            // attempt to add to rete, remove if duplicate
            if ( rete.add_production_to_rete( new_production, null, false, true) == ProductionAddResult.DUPLICATE_PRODUCTION )
            {
                my_agent.getProductions().exciseProduction(new_production, false);
                rl_revert_template_id();

                new_name_symbol = null;
            }

            return_val = new_name_symbol;
        }
    }

    return return_val;
    }
    
    /**
     * creates an action for a template instantiation
     * 
     * <p>reinforcement_learning.cpp:495:rl_make_simple_action
     * (9.3.0)
     * 
     * @param id_sym
     * @param attr_sym
     * @param val_sym
     * @param ref_sym
     * @return
     */
    private MakeAction rl_make_simple_action( IdentifierImpl id_sym, SymbolImpl attr_sym, SymbolImpl val_sym, SymbolImpl ref_sym )
    {
        MakeAction rhs = new MakeAction();

        // id
        rhs.id = this.chunker.variablize_symbol( id_sym ).toRhsValue();
        
        // attribute
        rhs.attr = this.chunker.variablize_symbol(attr_sym).toRhsValue();

        // value
        rhs.value = this.chunker.variablize_symbol(val_sym).toRhsValue();

        // referent
        rhs.referent = this.chunker.variablize_symbol(ref_sym).toRhsValue();

        return rhs;
    }
    
    /**
     * reinforcement_learning.cpp:531:rl_add_goal_or_impasse_tests_to_conds
     * 
     * (9.3.0)
     * @param all_conds
     */
    private void rl_add_goal_or_impasse_tests_to_conds( Condition all_conds )
    {
        // mark each id as we add a test for it, so we don't add a test for the same id in two different places
        Marker tc = DefaultMarker.create();

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
     * gathers discounted reward for a state
     * 
     * <p>reinforcement_learning.cpp:562:rl_tabulate_reward_value_for_goal
     * (9.3.0)
     * 
     * @param goal
     */
    public void rl_tabulate_reward_value_for_goal(IdentifierImpl goal )
    {
        final ReinforcementLearningInfo data = goal.rl_info;

    if ( !data.prev_op_rl_rules.isEmpty() )
    {
        final Slot s = Slot.make_slot(goal.reward_header, rl_sym_reward, preSyms.operator_symbol );
        
        double reward = 0.0;
        double discount_rate = 0.0; // TODO rl_params->discount_rate->get_value();

        if ( s != null)
        {           
            for (WmeImpl w=s.getWmes(); w != null; w=w.next )
            {
                if (w.value.asIdentifier() != null)
                {
                    final Slot t = Slot.make_slot( w.value.asIdentifier(), rl_sym_value, preSyms.operator_symbol );
                    if (t != null)
                    {
                        for (WmeImpl x=t.getWmes(); x != null; x=x.next )
                        {
                            if(x.value.asDouble() != null)
                            {
                                reward += x.value.asDouble().getValue();
                            }
                            else if(x.value.asInteger() != null)
                            {
                                reward += x.value.asInteger().getValue();
                            }
                        }
                    }
                }
            }
            
            // if temporal_discount is off, don't discount for gaps
            long /*unsigned int*/ effective_age = data.hrl_age;
            if (false /* TODO my_agent->rl_params->temporal_discount->get_value() == soar_module::on*/) {
                effective_age += data.gap_age;
            }

            data.reward += ( reward * Math.pow( discount_rate, (double) effective_age) );
        }

        // update stats
        final double global_reward = 0.0; // TODO my_agent->rl_stats->global_reward->get_value();
        // TODO my_agent->rl_stats->total_reward->set_value( reward );
        // TODO my_agent->rl_stats->global_reward->set_value( global_reward + reward );

        if ( ( goal != decider.bottom_goal ) && (true /* TODO my_agent->rl_params->hrl_discount->get_value() == soar_module::on */ ) )
        {
            data.hrl_age++;
        }
    }

    }
    
    /**
     * gathers reward for all states
     * 
     * <p>reinforcement_learning:617:rl_tabulate_reward_values
     * (9.3.0)
     */
    public void rl_tabulate_reward_values()
    {
        IdentifierImpl goal = decider.top_goal;

        while( goal != null)
        {
            rl_tabulate_reward_value_for_goal( goal );
            goal = goal.lower_goal;
        }
    }
    
    
    /**
     * stores rl info for a state w.r.t. a selected operator
     * 
     * <p>reinforcement_learning.cpp:629:rl_store_data
     * (9.3.0)
     * 
     * @param goal the goal
     * @param cand the candidate preference
     */
    public void rl_store_data(IdentifierImpl goal, Preference cand)
    {
        ReinforcementLearningInfo data = goal.rl_info;
        Symbol op = cand.value;
        data.previous_q = cand.numeric_value;

        boolean using_gaps = false; // TODO ( my_agent->rl_params->temporal_extension->get_value() == soar_module::on );
        
        // Make list of just-fired prods
        int just_fired = 0;
        for ( Preference pref = goal.operator_slot.getPreferencesByType(PreferenceType.NUMERIC_INDIFFERENT); pref != null; pref = pref.next )
        {
            if ( op == pref.value && pref.inst.prod.rl_rule)
            {
                if ( just_fired == 0 && !data.prev_op_rl_rules.isEmpty())
                {
                    data.prev_op_rl_rules.clear();
                }
                
                data.prev_op_rl_rules.push(pref.inst.prod);
                just_fired++;
            }
        }

        if ( just_fired != 0)
        {
            data.previous_q = cand.numeric_value;
        }
        else
        {
            if (my_agent.getTrace().isEnabled(Category.RL) && 
                using_gaps &&
                data.gap_age == 0 &&
                !data.prev_op_rl_rules.isEmpty())
            {
                my_agent.getTrace().print(Category.RL, "gap started (%s)", goal);
            }
            
            if ( !using_gaps )
            {
                if(!data.prev_op_rl_rules.isEmpty())
                {
                    data.prev_op_rl_rules.clear();
                }
                data.previous_q = cand.numeric_value;
            }
            else
            {
                if(!data.prev_op_rl_rules.isEmpty())
                {
                    data.gap_age++;
                }
            }
        }
    }
    
    public void rl_perform_update(double op_value, boolean op_rl, IdentifierImpl goal)
    {
        rl_perform_update(op_value, op_rl, goal, true);
    }

    /**
     * performs the rl update at a state
     * 
     * <p>reinforcement_learning.cpp:688:rl_perform_update
     * (9.3.0)
     * 
     * @param op_value operator value
     * @param goal the goal
     */
public void rl_perform_update(double op_value, boolean op_rl, IdentifierImpl goal, boolean update_efr )
{
    final boolean using_gaps = false; // TODO ( my_agent->rl_params->temporal_extension->get_value() == soar_module::on );

    if ( !using_gaps || op_rl )
    {    
        final ReinforcementLearningInfo data = goal.rl_info;
        
        if (!data.prev_op_rl_rules.isEmpty())
        {           
            final double alpha = 0.0; // TODO my_agent->rl_params->learning_rate->get_value();
            final double lambda = 0.0; // TODO my_agent->rl_params->et_decay_rate->get_value();
            final double gamma = 0.0; // TODO my_agent->rl_params->discount_rate->get_value();
            final double tolerance = 0.0; // TODO my_agent->rl_params->et_tolerance->get_value();

            // if temporal_discount is off, don't discount for gaps
            long effective_age = data.hrl_age + 1;
            if (false /* TODO my_agent->rl_params->temporal_discount->get_value() == soar_module::on */) {
                effective_age += data.gap_age;
            }
 
            double discount = Math.pow( gamma, (double) effective_age);

            // notify of gap closure
            if ( data.gap_age != 0 && using_gaps && my_agent.getTrace().isEnabled(Category.RL))
            {
                my_agent.getTrace().print("gap ended (%s)", goal);
            }           

            // Iterate through eligibility_traces, decay traces. If less than TOLERANCE, remove from map.
            if ( lambda == 0.0 )
            {
                if ( !data.eligibility_traces.isEmpty() )
                {
                    data.eligibility_traces.clear();
                }
            }
            else
            {
                for(Iterator<Map.Entry<Production, Double>> it = data.eligibility_traces.entrySet().iterator(); it.hasNext();)
                {
                    final Map.Entry<Production, Double> e = it.next();
                    e.setValue(e.getValue() * lambda);
                    e.setValue(e.getValue() * discount);
                    if(e.getValue() < tolerance)
                    {
                        it.remove();
                    }
                }
            }
            
            // Update trace for just fired prods
            double sum_old_ecr = 0.0;
            double sum_old_efr = 0.0;
            if (!data.prev_op_rl_rules.isEmpty())
            {
                final double trace_increment = ( 1.0 / (double)( data.prev_op_rl_rules.size() ) );
                
                for(Production p : data.prev_op_rl_rules)
                {
                    if(p != null)
                    {
                        sum_old_ecr += p.rl_ecr;
                        sum_old_efr += p.rl_efr;
                        
                        final Double old = data.eligibility_traces.get(p);
                        if(old != null)
                        {
                            data.eligibility_traces.put(p, old + trace_increment);
                        }
                        else
                        {
                            data.eligibility_traces.put(p, trace_increment);
                        }
                    }
                }
            }
            
            // For each prod with a trace, perform update
            {
                double old_ecr, old_efr;
                double delta_ecr, delta_efr;
                double new_combined, new_ecr, new_efr;
                
                for(Map.Entry<Production, Double> iter : data.eligibility_traces.entrySet())
                {   
                    Production prod = iter.getKey();

                    // get old vals
                    old_ecr = prod.rl_ecr;
                    old_efr = prod.rl_efr;
                    
                    // calculate updates
                    delta_ecr = ( alpha * iter.getValue() * ( data.reward - sum_old_ecr ) );
                    
                    if ( update_efr )
                    {
                        delta_efr = ( alpha * iter.getValue() * ( ( discount * op_value ) - sum_old_efr ) );
                    }
                    else
                    {
                        delta_efr = 0.0;
                    }                   

                    // calculate new vals
                    new_ecr = ( old_ecr + delta_ecr );
                    new_efr = ( old_efr + delta_efr );
                    new_combined = ( new_ecr + new_efr );
                    
                    // print as necessary
                    if (my_agent.getTrace().isEnabled(Category.RL)) 
                    {
                        my_agent.getTrace().print("RL update " + prod.getName() + " "
                           + old_ecr + " " + old_efr + " " + old_ecr + old_efr + " -> "
                           + new_ecr + " " + new_efr + " " + new_combined + "\n");
                    }

                    // Change value of rule
                    prod.action_list.asMakeAction().referent = chunker.variableGenerator.getSyms().createDouble(new_combined).toRhsValue();
                    prod.rl_update_count += 1;
                    prod.rl_ecr = new_ecr;
                    prod.rl_efr = new_efr;

                    // Change value of preferences generated by current instantiations of this rule
                    for (Instantiation inst = prod.instantiations; inst != null; inst = inst.nextInProdList)
                    {
                        for (Preference pref = inst.preferences_generated; pref != null; pref = pref.inst_next )
                        {
                            pref.referent = chunker.variableGenerator.getSyms().createDouble(new_combined);
                        }
                    }
                }
            }
        }

        data.gap_age = 0;
        data.hrl_age = 0;
        data.reward = 0.0;
    }
}
    
    /**
     * <p>reinforcement_learning.cpp:850:rl_perform_update
     * (9.3.0)
     * 
     * @param goal
     */
    public static void rl_watkins_clear(IdentifierImpl goal )
    {
        goal.rl_info.eligibility_traces.clear();
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
        // From production.cpp:make_production
        p.rl_update_count = 0;
        p.rl_rule = false;
        if (p.getType() != ProductionType.JUSTIFICATION && p.getType() != ProductionType.TEMPLATE) 
        {
            p.rl_rule = rl_valid_rule( p );  
            if(p.rl_rule)
            {
                p.rl_efr = Symbols.asDouble(p.action_list.asMakeAction().referent.asSymbolValue().getSym());
            }
        }
        
        rl_update_template_tracking( p.getName() );
        
        // From parser.cpp:parse_production
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
        if ( prod.rl_rule && prod.getFiringCount() != 0 ) 
            rl_remove_refs_for_prod( prod ); 
    }
    
}
