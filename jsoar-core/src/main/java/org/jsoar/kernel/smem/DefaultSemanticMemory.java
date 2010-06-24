/*
 * Copyright (c) 2010 Dave Ray <daveray@gmail.com>
 *
 * Created on Jun 23, 2010
 */
package org.jsoar.kernel.smem;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.jsoar.kernel.Agent;
import org.jsoar.kernel.learning.Chunker;
import org.jsoar.kernel.lhs.Condition;
import org.jsoar.kernel.memory.Instantiation;
import org.jsoar.kernel.memory.Preference;
import org.jsoar.kernel.memory.RecognitionMemory;
import org.jsoar.kernel.memory.Slot;
import org.jsoar.kernel.memory.WmeImpl;
import org.jsoar.kernel.modules.SoarModule;
import org.jsoar.kernel.rhs.Action;
import org.jsoar.kernel.symbols.IdentifierImpl;
import org.jsoar.kernel.symbols.Symbol;
import org.jsoar.kernel.symbols.SymbolFactory;
import org.jsoar.kernel.symbols.SymbolImpl;
import org.jsoar.kernel.symbols.Symbols;
import org.jsoar.util.ByRef;
import org.jsoar.util.markers.Marker;

/**
 * @author ray
 */
public class DefaultSemanticMemory implements SemanticMemory
{
    /**
     * semantic_memory.h:232:smem_variable_key
     */
    private static enum smem_variable_key
    {
        var_max_cycle, var_num_nodes, var_num_edges, var_act_thresh
    };
    
    /**
     * semantic_memory.h:260:smem_storage_type
     */
    private static enum smem_storage_type { store_level, store_recursive };
    
    /**
     * semantic_memory.h:367:smem_query_levels
     */
    private static enum smem_query_levels { qry_search, qry_full };

    /**
     * semantic_memory.h:237:SMEM_ACT_MAX
     */
    private static final long SMEM_ACT_MAX = (0-1)/ 2; // TODO???

    private final Agent agent;
    private RecognitionMemory recMem;
    private Chunker chunker;
    
    private SemanticMemoryDatabase db;
    
    /** agent.h:smem_validation */
    private /*uintptr_t*/ long smem_validation;
    /** agent.h:smem_first_switch */
    private boolean smem_first_switch = true;
    /** agent.h:smem_made_changes */
    private boolean smem_made_changes = false;
    /** agent.h:smem_max_cycle */
    private /*intptr_t*/ long smem_max_cycle;
    
    private final SemanticMemorySymbols syms;
    
    private Map<IdentifierImpl, SemanticMemoryStateInfo> stateInfos = new HashMap<IdentifierImpl, SemanticMemoryStateInfo>();
    
    public DefaultSemanticMemory(Agent agent)
    {
        this.agent = agent;
        
        this.syms = new SemanticMemorySymbols(agent.getSymbols());
    }

    private SemanticMemoryStateInfo smem_info(IdentifierImpl state)
    {
        return stateInfos.get(state);
    }
    
    @Override
    public void smem_attach()
    {
        // TODO Auto-generated method stub
        
    }

    @Override
    public void smem_close()
    {
        // TODO Auto-generated method stub
        
    }

    @Override
    public boolean smem_enabled()
    {
        // TODO Auto-generated method stub
        return false;
    }

    @Override
    public void smem_go(boolean storeOnly)
    {
        // TODO Auto-generated method stub
        
    }

    @Override
    public long smem_lti_get_id(char nameLetter, long nameNumber)
    {
        // TODO Auto-generated method stub
        return 0;
    }

    @Override
    public IdentifierImpl smem_lti_soar_make(long lti, char nameLetter,
            long nameNumber, int level)
    {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public void smem_reset(IdentifierImpl state)
    {
        // TODO Auto-generated method stub
        
    }

    @Override
    public void smem_reset_id_counters()
    {
        // TODO Auto-generated method stub
        
    }

    @Override
    public boolean smem_valid_production(Condition lhsTop, Action rhsTop)
    {
        // TODO Auto-generated method stub
        return false;
    }
    
    /**
     * semantic_memory.cpp:481:smem_get_direct_augs_of_id
     * 
     * @param sym
     * @param tc
     * @return
     */
    private List<WmeImpl> smem_get_direct_augs_of_id( SymbolImpl sym, Marker tc /*= NIL*/ )
    {
        final List<WmeImpl> return_val = new ArrayList<WmeImpl>();
        // augs only exist for identifiers
        final IdentifierImpl id = sym.asIdentifier();
        if ( id != null )
        {
            if ( tc != null )
            {
                if ( tc == id.tc_number )
                {
                    return return_val;
                }
                else
                {
                    id.tc_number = tc;
                }
            }

            // impasse wmes
            for (WmeImpl w=id.getImpasseWmes(); w!=null; w=w.next )
            {
                if ( !w.acceptable )
                {
                    return_val.add( w );
                }
            }

            // input wmes
            for (WmeImpl w=id.getInputWmes(); w!=null; w=w.next )
            {
                return_val.add( w );
            }

            // regular wmes
            for (Slot s=id.slots; s!=null; s=s.next )
            {
                for (WmeImpl w=s.getWmes(); w!=null; w=w.next )
                {
                    if ( !w.acceptable )
                    {
                        return_val.add( w );
                    }
                }
            }
        }

        return return_val;
    } 
    
    /**
     * semantic_memory.cpp:481:smem_symbol_is_constant
     * 
     * @param sym
     * @return
     */
    private static boolean smem_symbol_is_constant( Symbol sym )
    {
        return sym.asIdentifier() == null;
//        return ( ( sym->common.symbol_type == SYM_CONSTANT_SYMBOL_TYPE ) ||
//                 ( sym->common.symbol_type == INT_CONSTANT_SYMBOL_TYPE ) ||
//                 ( sym->common.symbol_type == FLOAT_CONSTANT_SYMBOL_TYPE ) );
    }
    
    /**
     * semantic_memory.cpp:542:smem_symbol_to_bind
     * 
     * @param sym
     * @param q
     * @param type_field
     * @param val_field
     * @throws SQLException
     */
    private void smem_symbol_to_bind( Symbol sym, PreparedStatement q, int type_field, int val_field ) throws SQLException
    {
        q.setInt( type_field, Symbols.getSymbolType(sym) );
        if(sym.asString() != null)
        {
            q.setString(val_field, sym.asString().getValue());
        }
        else if(sym.asInteger() != null)
        {
            q.setInt(val_field, sym.asInteger().getValue());
        }
        else if(sym.asDouble() != null)
        {
            q.setDouble(val_field, sym.asDouble().getValue());
        }
    }
    
    /**
     * semantic_memory.cpp:561:smem_statement_to_symbol
     * 
     * @param q
     * @param type_field
     * @param val_field
     * @return
     * @throws SQLException
     */
    private Symbol smem_statement_to_symbol( ResultSet q, int type_field, int val_field ) throws SQLException
    {
        final SymbolFactory syms = agent.getSymbols();
        
        switch (q.getInt( type_field ) )
        {
            case Symbols.SYM_CONSTANT_SYMBOL_TYPE:
                return syms.createString(q.getString(val_field));

            case Symbols.INT_CONSTANT_SYMBOL_TYPE:
                return syms.createInteger(q.getInt(val_field));

            case Symbols.FLOAT_CONSTANT_SYMBOL_TYPE:
                return syms.createDouble(q.getDouble(val_field));

            default:
                return null;
        }
    }

    
    /**
     * semantic_memory.cpp:589:_smem_add_wme
     * 
     * @param state
     * @param id
     * @param attr
     * @param value
     * @param meta
     */
    void _smem_add_wme(IdentifierImpl state, IdentifierImpl id, SymbolImpl attr, SymbolImpl value, boolean meta )
    {
        // this fake preference is just for this state.
        // it serves the purpose of simulating a completely
        // local production firing to provide backtracing
        // information, making the result wmes dependent
        // upon the cue wmes.
        final SemanticMemoryStateInfo smem_info = smem_info(state);
        final Preference pref = SoarModule.make_fake_preference( state, id, attr, value, smem_info.cue_wmes );

        // add the preference to temporary memory
        recMem.add_preference_to_tm( pref );

        // and add it to the list of preferences to be removed
        // when the goal is removed
        state.addGoalPreference(pref);
        pref.on_goal_list = true;


        if ( meta )
        {
            // if this is a meta wme, then it is completely local
            // to the state and thus we will manually remove it
            // (via preference removal) when the time comes
            smem_info.smem_wmes.push( pref );
        }
        else
        {
            // otherwise, we submit the fake instantiation to backtracing
            // such as to potentially produce justifications that can follow
            // it to future adventures (potentially on new states)

            final ByRef<Instantiation> my_justification_list = ByRef.create(null);
            chunker.chunk_instantiation( pref.inst, false, my_justification_list );

            // if any justifications are created, assert their preferences manually
            // (copied mainly from assert_new_preferences with respect to our circumstances)
            if ( my_justification_list.value != null)
            {
                Instantiation next_justification = null;
                
                for ( Instantiation my_justification=my_justification_list.value;
                      my_justification!=null;
                      my_justification=next_justification )
                {
                    next_justification = my_justification.nextInProdList;

                    if ( my_justification.in_ms )
                    {
                        my_justification.prod.instantiations = my_justification.insertAtHeadOfProdList(my_justification.prod.instantiations);
                    }

                    for (Preference just_pref=my_justification.preferences_generated; just_pref!=null; just_pref=just_pref.inst_next ) 
                    {
                        recMem.add_preference_to_tm( just_pref );                        
                        
                        // TODO SMEM WMA
//                        if ( wma_enabled( my_agent ) )
//                        {
//                            wma_activate_wmes_in_pref( my_agent, just_pref );
//                        }
                    }
                }
            }
        }
    }
    
//////////////////////////////////////////////////////////
//////////////////////////////////////////////////////////
// Variable Functions (smem::var)
//
// Variables are key-value pairs stored in the database
// that are necessary to maintain a store between
// multiple runs of Soar.
//
//////////////////////////////////////////////////////////
//////////////////////////////////////////////////////////

    /**
     * Gets an SMem variable from the database
     * 
     * semantic_memory.cpp:682:smem_variable_get
     * 
     * @param variable_id
     * @param variable_value
     * @return
     * @throws SQLException
     */
    private boolean smem_variable_get(smem_variable_key variable_id, ByRef<Long> variable_value ) throws SQLException
    {
        final PreparedStatement var_get = db.var_get;
    
        var_get.setInt( 1, variable_id.ordinal() );
        final ResultSet rs = var_get.executeQuery();
        try
        {
            if(rs.next())
            {
                variable_value.value = rs.getLong(0);
                return true;
            }
            else
            {
                return false;
            }
        }
        finally
        {
            rs.close();
        }
    }

    /**
     * Sets an SMem variable in the database
     * 
     * semantic_memory.cpp:705:smem_variable_set
     * 
     * @param variable_id
     * @param variable_value
     * @throws SQLException
     */
    private void smem_variable_set(smem_variable_key variable_id, long variable_value ) throws SQLException
    {
        final PreparedStatement var_set = db.var_set;
    
        var_set.setInt( 1, variable_id.ordinal() );
        var_set.setLong( 2, variable_value );
    
        var_set.execute();
    }


}
