/*
 * (c) 2008  Dave Ray
 *
 * Created on Aug 22, 2008
 */
package org.jsoar.kernel.rete;

import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedHashSet;
import java.util.List;

import org.jsoar.kernel.Production;
import org.jsoar.kernel.ProductionType;
import org.jsoar.kernel.epmem.DefaultEpisodicMemory;
import org.jsoar.kernel.epmem.EpisodicMemory;
import org.jsoar.kernel.lhs.Condition;
import org.jsoar.kernel.lhs.ConjunctiveNegationCondition;
import org.jsoar.kernel.lhs.ConjunctiveTest;
import org.jsoar.kernel.lhs.DisjunctionTest;
import org.jsoar.kernel.lhs.EqualityTest;
import org.jsoar.kernel.lhs.GoalIdTest;
import org.jsoar.kernel.lhs.ImpasseIdTest;
import org.jsoar.kernel.lhs.NegativeCondition;
import org.jsoar.kernel.lhs.PositiveCondition;
import org.jsoar.kernel.lhs.RelationalTest;
import org.jsoar.kernel.lhs.Test;
import org.jsoar.kernel.lhs.Tests;
import org.jsoar.kernel.lhs.ThreeFieldCondition;
import org.jsoar.kernel.memory.Instantiation;
import org.jsoar.kernel.memory.Wme;
import org.jsoar.kernel.memory.WmeImpl;
import org.jsoar.kernel.rete.PartialMatches.Entry;
import org.jsoar.kernel.rhs.Action;
import org.jsoar.kernel.rhs.FunctionAction;
import org.jsoar.kernel.rhs.MakeAction;
import org.jsoar.kernel.rhs.RhsValue;
import org.jsoar.kernel.symbols.IdentifierImpl;
import org.jsoar.kernel.symbols.SymbolFactoryImpl;
import org.jsoar.kernel.symbols.SymbolImpl;
import org.jsoar.kernel.symbols.Variable;
import org.jsoar.kernel.tracing.Printer;
import org.jsoar.kernel.tracing.Trace;
import org.jsoar.kernel.tracing.Trace.WmeTraceType;
import org.jsoar.util.Arguments;
import org.jsoar.util.ByRef;
import org.jsoar.util.HashTable;
import org.jsoar.util.ListHead;
import org.jsoar.util.ListItem;
import org.jsoar.util.markers.DefaultMarker;
import org.jsoar.util.markers.Marker;

/**
 * <em>This is an internal interface. Don't use it unless you know what you're doing.</em>
 * 
 * @author ray
 */
public class Rete
{
    private final Trace trace;
    private final SymbolFactoryImpl syms;
    
    /* Set to FALSE to preserve variable names in chunks (takes extra space) */
    private final boolean discard_chunk_varnames = true;
    
    private ReteListener listener;
    
    private final LeftTokenHashTable left_ht = new LeftTokenHashTable();
    private final RightMemoryHashTable right_ht = new RightMemoryHashTable();
    // TODO rete_node_counts should be populated
    int rete_node_counts[] = new int[256];
    /*package*/ RightToken dummy_top_token;
    
    private int alpha_mem_id_counter; 
    private List<HashTable<AlphaMemory>> alpha_hash_tables;
    /**
     * TODO: Although this hashset preserves the insertion order of the WMEs, 
     * the order is the reverse of that in CSoar which inserts at the front.
     * It doesn't appear to affect correctness, but it may cause firing order
     * variation from CSoar. See usage in find_or_make_alpha_mem()
     *  
     * all_wmes_in_rete
     */
    private LinkedHashSet<WmeImpl> all_wmes_in_rete = new LinkedHashSet<WmeImpl>();
    //private ListHead<WmeImpl> all_wmes_in_rete = ListHead.newInstance();
    private int beta_node_id_counter;
    ReteNode dummy_top_node;
    
    // TODO: I think this belongs somewhere else. I think it's actually basically
    // a temp variable used by the firer and sme RHS value stuff.
    private SymbolImpl[] rhs_variable_bindings = {};
    private int highest_rhs_unboundvar_index;

    /**
     * Receives tokens for dummy matches node.
     * 
     * <p>agent.h:159:dummy_matches_node_tokens
     */
    private Token dummy_matches_node_tokens;
    
    private final EpisodicMemory episodicMemory;
    
    public Rete(Trace trace, SymbolFactoryImpl syms, EpisodicMemory episodicMemory)
    {
        Arguments.checkNotNull(trace, "trace");
        Arguments.checkNotNull(syms, "syms");
        Arguments.checkNotNull(episodicMemory, "episodicMemory");
        
        this.trace = trace;
        this.syms = syms;
        this.episodicMemory = episodicMemory;
        
        // rete.cpp:8864
        alpha_hash_tables = new ArrayList<HashTable<AlphaMemory>>(16);
        for(int i = 0; i < 16; ++i)
        {
            alpha_hash_tables.add(new HashTable<AlphaMemory>(0, AlphaMemory.HASH_FUNCTION, AlphaMemory.class));
        }
        
        init_dummy_top_node();
    }
    
    public void setReteListener(ReteListener listener)
    {
        if(this.listener != null)
        {
            throw new IllegalStateException("listener already set");
        }
        this.listener = listener;
    }
    
    public SymbolFactoryImpl getSymbols()
    {
        return syms;
    }
    
    /**
     * Access to rhs_variable_bindings array
     * 
     * @param index Index of binding
     * @return The binding symbol
     */
    public SymbolImpl getRhsVariableBinding(int index)
    {
        return rhs_variable_bindings[index];
    }
    
    /**
     * Access to rhs_variable_bindings array
     * 
     * @param index Index of binding
     * @param sym New symbol value
     */
    public void setRhsVariableBinding(int index, SymbolImpl sym)
    {
        rhs_variable_bindings[index] = sym;
        if(highest_rhs_unboundvar_index < index)
        {
            highest_rhs_unboundvar_index = index;
        }
    }
    
    public int getHighestRhsUnboundVariableIndex()
    {
        return highest_rhs_unboundvar_index;
    }
    
    /**
     * @return List of all Wmes currently in the rete. This is the actual list
     *  so please don't modify it.
     */
    public Collection<WmeImpl> getAllWmes()
    {
        return all_wmes_in_rete;
    }
    
    public boolean containsWme(Wme w)
    {
        return all_wmes_in_rete.contains(w);
    }
    
    /**
     * Simpler method that adds a production to the rete with default options
     * 
     * @param p The production to add
     * @return Production addition result
     */
    public ProductionAddResult add_production_to_rete(Production p)
    {
        return add_production_to_rete(p, null, true, false);
    }
    
    /**
     * Add_production_to_rete() adds a given production, with a given LHS,
     * to the rete.  If "refracted_inst" is non-NIL, it should point to an
     * initial instantiation of the production.  This routine returns 
     * DUPLICATE_PRODUCTION if the production was a duplicate; else
     * NO_REFRACTED_INST if no refracted inst. was given; else either
     * REFRACTED_INST_MATCHED or REFRACTED_INST_DID_NOT_MATCH.

     * The initial refracted instantiation is provided so the initial 
     * instantiation of a newly-build chunk doesn't get fired.  We handle
     * this as follows.  We store the initial instantiation as a "tentative
     * retraction" on the new p-node.  Then we inform the p-node of any
     * matches (tokens from above).  If any of them is the same as the
     * refracted instantiation, then that instantiation will get removed
     * from "tentative_retractions".  When the p-node has been informed of
     * all matches, we just check whether the instantiation is still on
     * tentative_retractions.  If not, there was a match (and the p-node's
     * activation routine filled in the token info on the instantiation for
     * us).  If so, there was no match for the refracted instantiation.
     * 
     * BUGBUG should we check for duplicate justifications?
     * 
     * rete.cpp:3522:add_production_to_rete
     * 
     * @param p The production to add
     * @param refracted_inst Refracted instantiation
     * @param warn_on_duplicates
     * @param ignore_rhs
     * @return the result code of adding the production
     */
    public ProductionAddResult add_production_to_rete(Production p, Instantiation refracted_inst,
            boolean warn_on_duplicates, boolean ignore_rhs)
    {
        final Condition lhs_top = p.getFirstCondition();
        ProductionAddResult production_addition_result;

        final ByRef<ReteNode> bottom_node = ByRef.create(null);
        final ByRef<Integer> bottom_depth = ByRef.create(0);
        final ByRef<ListHead<Variable>> vars_bound = ByRef.create(null);
        // build the network for all the conditions
        ReteBuilder.build_network_for_condition_list(this, lhs_top, 1, dummy_top_node, bottom_node, bottom_depth,
                vars_bound);

        // change variable names in RHS to Rete location references or
        // unbound variable indices

        final List<Variable> rhs_unbound_vars_for_new_prod = new ArrayList<Variable>(3);
        final Marker rhs_unbound_vars_tc = DefaultMarker.create();
        for (Action a = p.getFirstAction(); a != null; a = a.next)
        {
            MakeAction ma = a.asMakeAction();
            if (ma != null)
            {
                ma.value = ReteBuilder.fixup_rhs_value_variable_references(this, ma.value, bottom_depth.value,
                        rhs_unbound_vars_for_new_prod, rhs_unbound_vars_tc);

                ma.id = ReteBuilder.fixup_rhs_value_variable_references(this, ma.id, bottom_depth.value,
                        rhs_unbound_vars_for_new_prod, rhs_unbound_vars_tc);

                ma.attr = ReteBuilder.fixup_rhs_value_variable_references(this, ma.attr, bottom_depth.value,
                        rhs_unbound_vars_for_new_prod, rhs_unbound_vars_tc);

                if (a.preference_type.isBinary())
                {
                    ma.referent = ReteBuilder.fixup_rhs_value_variable_references(this, ma.referent, bottom_depth.value,
                            rhs_unbound_vars_for_new_prod, rhs_unbound_vars_tc);
                }
            }
            else
            {
                FunctionAction fa = a.asFunctionAction();
                RhsValue result = ReteBuilder.fixup_rhs_value_variable_references(this, fa.call, bottom_depth.value,
                        rhs_unbound_vars_for_new_prod, rhs_unbound_vars_tc);
                assert result == fa.call; // sanity check

            }
        }

        // clean up variable bindings created by build_network...

        Variable.pop_bindings_and_deallocate_list_of_variables(vars_bound.value);

        update_max_rhs_unbound_variables(rhs_unbound_vars_for_new_prod.size());

        // look for an existing p node that matches
        for (ReteNode p_node = bottom_node.value.first_child; p_node != null; p_node = p_node.next_sibling)
        {
            if (p_node.node_type != ReteNodeType.P_BNODE)
            {
                continue;
            }
            if (!ignore_rhs && !Action.same_rhs(p_node.b_p().prod.getFirstAction(), p.getFirstAction()))
            {
                continue;
            }
            /* --- duplicate production found --- */
            if (warn_on_duplicates)
            {
                // TODO: Test
                trace.getPrinter().warn("\nIgnoring %s because it is a duplicate of %s ",
                                        p.getName(), p_node.b_p().prod.getName());
                
                // TODO: XML Warn
                // std::stringstream output;
                // output << "\nIgnoring " << symbol_to_string( thisAgent, p->name, TRUE, 0, 0 )
                // << " because it is a duplicate of " << symbol_to_string( thisAgent, p_node->b.p.prod->name, TRUE, 0, 0 )
                // << " ";
                // xml_generate_warning( thisAgent, output.str().c_str() );
            }
            // deallocate_symbol_list_removing_references (thisAgent, rhs_unbound_vars_for_new_prod);
            return ProductionAddResult.DUPLICATE_PRODUCTION;
        }

        /* --- build a new p node --- */
        ReteNode p_node = ReteNode.make_new_production_node(this, bottom_node.value, p);
        // adjust_sharing_factors_from_here_to_top (p_node, 1);

        /*
         * KJC 1/28/98 left these comments in to support REW comments below but
         * commented out the operand_mode code
         */
        /* RCHONG: begin 10.11 */
        /*
         * 
         * in operand, we don't want to refract the instantiation. consider this
         * situation: a PE chunk was created during the IE phases. that
         * instantiation shouldn't be applied and we prevent this from happening
         * (see chunk_instantiation() in chunk.c). we eventually get to the
         * OUTPUT_PHASE, then the QUIESCENCE_PHASE. up to this point, the chunk
         * hasn't done it's thing. we start the PE_PHASE. now, it is at this
         * time that the just-built PE chunk should match and fire. if we were
         * to refract the chunk, it wouldn't fire it at this point and it's
         * actions would never occur. by not refracting it, we allow the chunk
         * to match and fire.
         * 
         * caveat: we must refract justifications, otherwise they would fire and
         * in doing so would produce more chunks/justifications.
         * 
         * if ((thisAgent->operand_mode == TRUE) && 1) if (refracted_inst !=
         * NIL) { if (refracted_inst->prod->type !=
         * JUSTIFICATION_PRODUCTION_TYPE) refracted_inst = NIL; }
         */
        /* RCHONG: end 10.11 */

        /* REW: begin 09.15.96 */
        /*
         * In Operand2, for now, we want both chunks and justifications to be
         * treated as refracted instantiations, at least for now. At some point,
         * this issue needs to be re-visited for chunks that immediately match
         * with a different instantiation and a different type of support than
         * the original, chunk-creating instantion.
         */
        /* REW: end 09.15.96 */

        // handle initial refraction by adding it to tentative_retractions
        if (refracted_inst != null)
        {
            listener.startRefraction(this, p, refracted_inst, p_node);
        }

        // call new node's add_left routine with all the parent's tokens
        update_node_with_matches_from_above(p_node);

        // store result indicator
        if (refracted_inst == null)
        {
            production_addition_result = ProductionAddResult.NO_REFRACTED_INST;
        }
        else
        {
            boolean refactedInstMatched = listener.finishRefraction(this, p, refracted_inst, p_node);
            production_addition_result = refactedInstMatched ? ProductionAddResult.REFRACTED_INST_MATCHED : ProductionAddResult.REFRACTED_INST_DID_NOT_MATCH;
        }

        // If it's a chunk, we don't store RHS variable names and stuff.
        // For all other rules, save the variable names
        if (p.getType() == ProductionType.CHUNK && discard_chunk_varnames)
        {
            p.getReteNode().b_p().parents_nvn = null;
            p.clearRhsUnboundVariables();
        }
        else
        {
            p.getReteNode().b_p().parents_nvn = NodeVarNames.get_nvn_for_condition_list(lhs_top, null);
            p.setRhsUnboundVariables(rhs_unbound_vars_for_new_prod);
        }

        return production_addition_result;
    }


    /**
     * This removes a given production from the Rete net, and enqueues all 
     * its existing instantiations as pending retractions.
     * 
     * rete.cpp:3726:excise_production_from_rete
     * 
     * @param p The production to remove
     */
    public void excise_production_from_rete(Production p)
    {
        // #ifdef _WINDOWS
        // remove_production_from_stat_lists(prod_to_be_excised);
        // #endif

        ReteNode p_node = p.getReteNode();
        p.setReteNode(null, null); // mark production as not being in the rete anymore
        ReteNode parent = p_node.parent;

        // deallocate the variable name information
        p_node.b_p().parents_nvn = null;

        // cause all existing instantiations to retract, by removing any
        // tokens at the node
        while (p_node.a_np().tokens != null)
        {
            remove_token_and_subtree(p_node.a_np().tokens);
        }

        listener.removingProductionNode(this, p_node);

        // finally, excise the p_node
        p_node.remove_node_from_parents_list_of_children();

        // and propogate up the net
        if (parent.first_child == null)
        {
            ReteNode.deallocate_rete_node(this, parent);
        }
    }

    /**
     * Stand-in for taking the address of an object in C, where the address is
     * used for hashing, e.g. cn_node_left_addition() in rete.cpp
     * 
     * @param o The object (token, wme, etc)
     * @return A unique address for the the object
     */
    static int addressOf(Object o)
    {
        return System.identityHashCode(o);
    }

    /**
     * 
     * rete.cpp::xor_op
     * 
     * @param i
     * @param a
     * @param v
     * @return
     */
    private static int xor_op(int i, int a, int v)
    {
      return ((i) ^ (a) ^ (v));
    }
    
    /**
     * Add a WME to the rete.
     * 
     * rete.cpp:1552:add_wme_to_rete
     * 
     * @param w The WME to add
     */
    public void add_wme_to_rete (WmeImpl w)
    {
        /* --- add w to all_wmes_in_rete --- */
        all_wmes_in_rete.add(w);
        //w.in_rete.insertAtHead(all_wmes_in_rete);

        /* --- it's not in any right memories or tokens yet --- */
        w.clearRightMemories();
        w.tokens = null;

        /* --- add w to the appropriate alpha_mem in each of 8 possible tables --- */
        int hi = w.id.hash_id;
        int ha = w.attr.hash_id;
        int hv = w.value.hash_id;

        if (w.acceptable) {
          add_wme_to_aht (alpha_hash_tables.get(8),  xor_op( 0, 0, 0), w);
          add_wme_to_aht (alpha_hash_tables.get(9),  xor_op(hi, 0, 0), w);
          add_wme_to_aht (alpha_hash_tables.get(10), xor_op( 0,ha, 0), w);
          add_wme_to_aht (alpha_hash_tables.get(11), xor_op(hi,ha, 0), w);
          add_wme_to_aht (alpha_hash_tables.get(12), xor_op( 0, 0,hv), w);
          add_wme_to_aht (alpha_hash_tables.get(13), xor_op(hi, 0,hv), w);
          add_wme_to_aht (alpha_hash_tables.get(14), xor_op( 0,ha,hv), w);
          add_wme_to_aht (alpha_hash_tables.get(15), xor_op(hi,ha,hv), w);
        } else {
          add_wme_to_aht (alpha_hash_tables.get(0),  xor_op( 0, 0, 0), w);
          add_wme_to_aht (alpha_hash_tables.get(1),  xor_op(hi, 0, 0), w);
          add_wme_to_aht (alpha_hash_tables.get(2),  xor_op( 0,ha, 0), w);
          add_wme_to_aht (alpha_hash_tables.get(3),  xor_op(hi,ha, 0), w);
          add_wme_to_aht (alpha_hash_tables.get(4),  xor_op( 0, 0,hv), w);
          add_wme_to_aht (alpha_hash_tables.get(5),  xor_op(hi, 0,hv), w);
          add_wme_to_aht (alpha_hash_tables.get(6),  xor_op( 0,ha,hv), w);
          add_wme_to_aht (alpha_hash_tables.get(7),  xor_op(hi,ha,hv), w);
        }
        
        w.epmem_id = DefaultEpisodicMemory.EPMEM_NODEID_BAD;
        w.epmem_valid = 0; //NIL
        {
            //if ( thisAgent->epmem_db->get_status() == soar_module::connected )
            if(episodicMemory.epmem_enabled())
            {
                // if identifier-valued and short-term, known value
                IdentifierImpl id = w.value.asIdentifier();
                if(     (id!=null) &&
                        (id.epmem_id!=DefaultEpisodicMemory.EPMEM_NODEID_BAD) &&
                        (id.epmem_valid==episodicMemory.epmem_validation()) &&
                        (id.smem_lti==0)) //( !w->value->id.smem_lti )
                {
                    // add id ref count
                    //(*thisAgent->epmem_id_ref_counts)[ w->value->id.epmem_id ]->insert( w );
                    episodicMemory.addIdRefCount(id.epmem_id, w);
                }
                
                // if known id
                if((w.id.asIdentifier().epmem_id!=DefaultEpisodicMemory.EPMEM_NODEID_BAD) &&
                        (w.id.asIdentifier().epmem_valid==episodicMemory.epmem_validation()))
                {
                    // add to add set
                    episodicMemory.addWme(w.id);
                }
            }
        }

//      SJK: not necessary in JSoar?
//        if ( ( w->id->id.smem_lti ) && ( !thisAgent->smem_ignore_changes ) && smem_enabled( thisAgent ) && ( thisAgent->smem_params->mirroring->get_value() == soar_module::on ) )
//        {
//            std::pair< smem_pooled_symbol_set::iterator, bool > insert_result = thisAgent->smem_changed_ids->insert( w->id );
//            if ( insert_result.second )
//            {
//              symbol_add_ref( w->id );
//            }
//        }
    }
    
    /**
     * Remove a WME from the rete.
     * 
     * <p>rete.cpp:1591:remove_wme_from_rete
     * 
     * @param w The WME to remove
     */
    public void remove_wme_from_rete (WmeImpl w)
    {
        // TODO EPMEM update epmem when wme removed from rete
        
        /* --- remove w from all_wmes_in_rete --- */
        all_wmes_in_rete.remove(w);
        //w.in_rete.remove(all_wmes_in_rete);
        
        /* --- remove w from each alpha_mem it's in --- */
        while (w.getRightMemories() != null) {
          final RightMemory rm = w.getRightMemories();
          final AlphaMemory am = rm.am;
          /* --- found the alpha memory, first remove the wme from it --- */
          remove_wme_from_alpha_mem (rm);
          
          /* --- for left unlinking, then if the alpha memory just went to
             zero, left unlink any attached Pos or MP nodes --- */
          if (am.right_mems == null) {
              ReteNode next = null;
            for (ReteNode node=am.beta_nodes; node!=null; node=next) {
              next = node.b_posneg().next_from_alpha_mem;
              switch (node.node_type) {
              case POSITIVE_BNODE:
              case UNHASHED_POSITIVE_BNODE:
                node.unlink_from_left_mem();
                break;
              case MP_BNODE:
              case UNHASHED_MP_BNODE:
                node.make_mp_bnode_left_unlinked();
                break;
              } /* end of switch (node.node_type) */
            }
          }
        }
       
        /* --- tree-based removal of all tokens that involve w --- */
        while (w.tokens != null) {
          final Token tok = w.tokens;
          ReteNode node = tok.node;
          if (tok.parent == null) {
            /* Note: parent pointer is NIL only on negative node negrm tokens */
              RightToken rt = (RightToken) tok;
            LeftToken left = rt.getLeftToken();
            tok.removeFromWme();
            left.removeNegRightToken(rt);

            if (!left.hasNegRightTokens()) { /* just went to 0, so call children */
              for (ReteNode child=node.first_child; child!=null; child=child.next_sibling) {
                  executeLeftAddition(child, left, null);
              }
            }
          } else {
            remove_token_and_subtree (tok);
          }
        }
        
    }
    
    /**
     * rete.cpp:1403:get_next_alpha_mem_id
     * 
     * @return
     */
    int get_next_alpha_mem_id()
    {
      return alpha_mem_id_counter++;
    }
    
    /**
     * Adds a WME to an alpha memory (create a right_mem for it), but doesn't
     * inform any successors
     * 
     * rete.cpp:1408:add_wme_to_alpha_mem
     * 
     * @param w
     * @param am
     */
    void add_wme_to_alpha_mem(WmeImpl w, AlphaMemory am)
    {
        /* --- allocate new right_mem, fill it fields --- */
        final RightMemory rm = new RightMemory(w, am);

        /* --- add it to dll's for the hash bucket, alpha mem, and wme --- */
        final int hv = am.am_id ^ w.id.hash_id;
        right_ht.insertAtHeadOfBucket(hv, rm);
        am.insertRightMemoryAtHead(rm);
        w.addRightMemory(rm);
    }
    
    /**
     * Removes a WME (right_mem) from its alpha memory, but doesn't inform
     * any successors
     * 
     * rete.cpp:1429:remove_wme_from_alpha_mem
     * 
     * @param rm
     */
    void remove_wme_from_alpha_mem(RightMemory rm)
    {
        final WmeImpl w = rm.w;
        final AlphaMemory am = rm.am;

        /* --- remove it from dll's for the hash bucket, alpha mem, and wme --- */
        final int hv = am.am_id ^ w.id.hash_id;
        right_ht.removeFromBucket(hv, rm);
        am.removeRightMemory(rm);
        w.removeRightMemory(rm);
    }

    /**
     * <p>rete.cpp:1393:table_for_tests
     * 
     * @param id
     * @param attr
     * @param value
     * @param acceptable
     * @return
     */
    /*package*/ HashTable<AlphaMemory> table_for_tests(SymbolImpl id, SymbolImpl attr, SymbolImpl value, boolean acceptable)
    {
        final int index = ((id != null) ? 1 : 0) + ((attr != null) ? 2 : 0) +
                                              ((value != null) ? 4 : 0) +
                                              ((acceptable) ? 8 : 0);
        return alpha_hash_tables.get(index);
    }
    
    /**
     * Looks for an existing alpha mem, returns it or NIL if not found
     * 
     * <p>rete.cpp:1449:find_alpha_mem
     * 
     * @param id
     * @param attr
     * @param value
     * @param acceptable
     * @return
     */
    AlphaMemory find_alpha_mem(SymbolImpl id, SymbolImpl attr, SymbolImpl value, boolean acceptable)
    {
        HashTable<AlphaMemory> ht = table_for_tests(id, attr, value, acceptable);
        int hash_value = AlphaMemory.alpha_hash_value(id, attr, value, ht.getLog2Size());

        for (AlphaMemory am = ht.getBucket(hash_value); am != null; am = (AlphaMemory) am.next_in_hash_table)
        {
            if ((am.id == id) && (am.attr == attr) && (am.value == value) && (am.acceptable == acceptable))
            {
                return am;
            }
        }
        return null;
    }

    /**
     * Returns a list of all alpha memories for use only by {@link ReteNetWriter}
     * 
     * @return a list of all alpha memories 
     */
    List<AlphaMemory> getAllAlphaMemories()
    {
        final List<AlphaMemory> result = new ArrayList<AlphaMemory>();
        for(HashTable<AlphaMemory> ht : alpha_hash_tables)
        {
            result.addAll(ht.getAllItems());
        }
        return result;
    }
    
    /**
     * Find and share existing alpha memory, or create new one.  Adjusts the 
     * reference count on the alpha memory accordingly.
     * 
     * <p>rete.cpp:1467:find_or_make_alpha_mem
     * 
     * @param id
     * @param attr
     * @param value
     * @param acceptable
     * @return
     */
    AlphaMemory find_or_make_alpha_mem(SymbolImpl id, SymbolImpl attr, SymbolImpl value, boolean acceptable)
    {

        /* --- look for an existing alpha mem --- */
        AlphaMemory am = find_alpha_mem(id, attr, value, acceptable);
        if (am != null)
        {
            am.reference_count++;
            return am;
        }

        /* --- no existing alpha_mem found, so create a new one --- */
        am = new AlphaMemory(get_next_alpha_mem_id(), id, attr, value, acceptable);
        HashTable<AlphaMemory> ht = table_for_tests(id, attr, value, acceptable);
        ht.add_to_hash_table(am);

        /* --- fill new mem with any existing matching WME's --- */
        AlphaMemory more_general_am = null;
        if (id != null)
        {
            more_general_am = find_alpha_mem(null, attr, value, acceptable);
        }
        if (more_general_am == null && value != null)
        {
            more_general_am = find_alpha_mem(null, attr, null, acceptable);
        }
        if (more_general_am != null)
        {
            /* --- fill new mem using the existing more general one --- */
            for (RightMemory rm = more_general_am.right_mems; rm != null; rm = rm.next_in_am)
            {
                if (am.wme_matches_alpha_mem(rm.w))
                {
                    add_wme_to_alpha_mem(rm.w, am);
                }
            }
        }
        else
        {
            // couldn't find such an existing mem, so do it the hard way
            for(WmeImpl w : all_wmes_in_rete)
            {
                if (am.wme_matches_alpha_mem(w))
                {
                    add_wme_to_alpha_mem(w, am);
                }
            }
        }

        return am;
    }

    /**
     * Using the given hash table and hash value, try to find a matching alpha 
     * memory in the indicated hash bucket.  If we find one, we add the wme to 
     * it and inform successor nodes.
     * 
     * <p>rete.cpp:1524:add_wme_to_aht
     * 
     * @param ht
     * @param hash_value
     * @param w
     */
    void add_wme_to_aht(HashTable<AlphaMemory> ht, int hash_value, WmeImpl w)
    {
        // TODO: Move this op into getBucket()
        hash_value = hash_value & HashTable.masks_for_n_low_order_bits[ht.getLog2Size()];
        AlphaMemory am = ht.getBucket(hash_value);
        while (am != null)
        {
            if (am.wme_matches_alpha_mem(w))
            {
                // found the right alpha memory, first add the wme
                add_wme_to_alpha_mem(w, am);

                // now call the beta nodes
                ReteNode next = null;
                for (ReteNode node = am.beta_nodes; node != null; node = next)
                {
                    next = node.b_posneg().next_from_alpha_mem;
                    executeRightAddition(node, w);
                }
                // only one possible alpha memory per table could match
                return;
            }
            am = (AlphaMemory) am.next_in_hash_table;
        }
    }


    /**
     * <p>rete.cpp:1698:get_next_beta_node_id
     * 
     * @return
     */
    int get_next_beta_node_id()
    {
        return beta_node_id_counter++;
    }
    
    /**
     * The dummy top node always has one token in it (WME=NIL). This is just
     * there so that (real) root nodes in the beta net can be handled the same
     * as non-root nodes.
     * 
     * <p>rete.cpp:1711:init_dummy_top_node
     */
    void init_dummy_top_node()
    {
        dummy_top_node = ReteNode.createDummy();
        dummy_top_token = RightToken.createDummy(dummy_top_node);
    }
    

    /**
     * Calls a node's left-addition routine with each match (token) from 
     * the node's parent.  DO NOT call this routine on (positive, unmerged)
     * join nodes.
     * 
     * <p>rete.cpp:1765:update_node_with_matches_from_above
     * 
     * @param node
     */
    void update_node_with_matches_from_above(ReteNode child)
    {
        if (child.node_type.bnode_is_bottom_of_split_mp()) {
            throw new IllegalArgumentException("Internal error: update_node_with_matches_from_above called on split node");
        }
        
        ReteNode parent = child.parent;

        /* --- if parent is dummy top node, tell child about dummy top token --- */ 
        if (parent.node_type==ReteNodeType.DUMMY_TOP_BNODE) {
          executeLeftAddition(child, dummy_top_token, null);
          return;
        }

        /* --- if parent is positive: first do surgery on parent's child list,
               to replace the list with "child"; then call parent's add_right 
               routine with each wme in the parent's alpha mem; then do surgery 
               to restore previous child list of parent. --- */
        if (parent.node_type.bnode_is_positive()) {
          // If the node is right unlinked, then don't activate it.  This is
          //  important because some interpreter routines rely on the node
          //  being right linked whenever it gets right activated.
          if (parent.node_is_right_unlinked ()) { return;}
          ReteNode saved_parents_first_child = parent.first_child;
          ReteNode saved_childs_next_sibling = child.next_sibling;
          parent.first_child = child;
          child.next_sibling = null;
          /* to avoid double-counting these right adds */
          for(RightMemory rm = parent.b_posneg().alpha_mem_.right_mems; rm != null; rm = rm.next_in_am)
          {
              executeRightAddition(parent, rm.w);
          }
          parent.first_child = saved_parents_first_child;
          child.next_sibling = saved_childs_next_sibling;
          return;
        }
          
        // if parent is negative or cn: easy, just look at the list of tokens
        // on the parent node
        for(Token tok = parent.a_np().tokens; tok != null; tok = tok.next_of_node)
        {
            if(!((LeftToken)tok).hasNegRightTokens())
            {
                executeLeftAddition(child, tok, null);
            }
        }
    }
    
    /**
     * When a production is fired, we use an array of gensyms to store 
     * the bindings for the RHS unbound variables.  We have to grow the 
     * memory block allocated for this array any time a production comes 
     * along with more RHS unbound variables than we've ever seen before.
     * This procedure checks the number of RHS unbound variables for a new
     * production, and grows the array if necessary.
     * 
     * <p>rete.cpp:3480:update_max_rhs_unbound_variables
     * 
     * @param num_for_new_production
     */
    void update_max_rhs_unbound_variables(int num_for_new_production)
    {
        if (num_for_new_production > rhs_variable_bindings.length)
        {
            rhs_variable_bindings = new SymbolImpl[num_for_new_production]; // defaults to null.
        }
    }

    /**
     * This routine destructively modifies a given test, adding to it a test
     * for equality with a new gensym variable.
     * 
     * <p>rete.cpp:3821:add_gensymmed_equality_test
     * 
     * @param t
     * @param first_letter
     */
    private Test add_gensymmed_equality_test(Test t, char first_letter)
    {
        Variable New = syms.getVariableGenerator().generate_new_variable(Character.toString(first_letter));
        Test eq_test = SymbolImpl.makeEqualityTest(New);
        return Tests.add_new_test_to_test(t, eq_test);
    }

    /**
     * We're reconstructing the conditions for a production in top-down
     * fashion.  Suppose we come to a Rete test checking for equality with 
     * the "value" field 3 levels up.  In that case, for the current condition,
     * we want to include an equality test for whatever variable got bound
     * in the value field 3 levels up.  This function scans up the list
     * of conditions reconstructed so far, and finds the appropriate variable.
     * 
     * <p>rete.cpp:3845:var_bound_in_reconstructed_conds
     * 
     * @param cond
     * @param where_field_num
     * @param where_levels_up
     * @return the variable
     */
    public static SymbolImpl var_bound_in_reconstructed_conds(Condition cond, int where_field_num, int where_levels_up)
    {
        while (where_levels_up != 0)
        {
            where_levels_up--;
            cond = cond.prev;
        }

        final ThreeFieldCondition tfc = cond.asThreeFieldCondition();
        if(tfc ==  null)
        {
            throw new IllegalStateException("Expected ThreeFieldCondition, got " + cond);
        }
        
        final Test t;
        if (where_field_num == 0)
        {
            t = tfc.id_test;
        }
        else if (where_field_num == 1)
        {
            t = tfc.attr_test;
        }
        else
        {
            t = tfc.value_test;
        }

        if (Tests.isBlank(t))
        {
            // goto abort_var_bound_in_reconstructed_conds;
            throw new IllegalStateException("Internal error in var_bound_in_reconstructed_conds");
        }
        
        final EqualityTest eq = t.asEqualityTest();
        if (eq != null)
        {
            return eq.getReferent();
        }

        final ConjunctiveTest ct = t.asConjunctiveTest();
        if (ct != null)
        {
            for (Test c : ct.conjunct_list)
            {
                EqualityTest eq2 = c.asEqualityTest();
                if (!Tests.isBlank(c) && eq2 != null)
                {
                    return eq2.getReferent();
                }
            }
        }

        throw new IllegalStateException("Internal error in var_bound_in_reconstructed_conds");
    }

    /**
     * <p>This replaces left_addition_routines in CSoar. A simple switch 
     * statement was easier, faster, and simpler than emulating function
     * pointers with Java interfaces.
     * 
     * <p>TODO tok parameter should be LeftToken
     * 
     * @param node
     * @param tok
     * @param w
     */
    private void executeLeftAddition(ReteNode node, Token tok, WmeImpl w)
    {
        // TODO: These should be polymorphic methods of ReteNode
        // rete.cpp:8796 
        switch(node.node_type)
        {
        case MEMORY_BNODE: beta_memory_node_left_addition(node, tok, w); break;
        case UNHASHED_MEMORY_BNODE: unhashed_beta_memory_node_left_addition(node, tok, w); break;
        case MP_BNODE: mp_node_left_addition(node, tok, w); break;
        case UNHASHED_MP_BNODE: unhashed_mp_node_left_addition(node, tok, w); break;
        case CN_BNODE: cn_node_left_addition(node, tok, w); break;
        case CN_PARTNER_BNODE: cn_partner_node_left_addition(node, tok, w); break;
        case P_BNODE: p_node_left_addition(node, tok, w); break;
        case NEGATIVE_BNODE: negative_node_left_addition(node, tok, w); break;
        case UNHASHED_NEGATIVE_BNODE: unhashed_negative_node_left_addition(node, tok, w); break;
        case DUMMY_MATCHES_BNODE: dummy_matches_node_left_addition(node, tok, w); break;
        default:
            throw new IllegalStateException("Unhandled node type " + node.node_type);
        }
    }
    
    /**
     * <p>This replaces right_addition_routines in CSoar. A simple switch 
     * statement was easier, faster, and simpler than emulating function
     * pointers with Java interfaces.
     * 
     * @param node
     * @param tok
     * @param w
     */
    private void executeRightAddition(ReteNode node, WmeImpl w)
    {
        switch(node.node_type)
        {
        case POSITIVE_BNODE: positive_node_right_addition(node, w); break;
        case UNHASHED_POSITIVE_BNODE: unhashed_positive_node_right_addition(node, w); break;
        case MP_BNODE: mp_node_right_addition(node, w); break;
        case UNHASHED_MP_BNODE: unhashed_mp_node_right_addition(node, w); break;
        case NEGATIVE_BNODE: negative_node_right_addition(node, w); break;
        case UNHASHED_NEGATIVE_BNODE: unhashed_negative_node_right_addition(node, w); break;
        default:
            throw new IllegalStateException("Unhandled node type " + node.node_type);
        }
    }
    
    /**
     * <p>rete.cpp:4719:beta_memory_node_left_addition
     * 
     * @param node
     * @param tok
     * @param w
     */
    private void beta_memory_node_left_addition(ReteNode node, Token tok, WmeImpl w)
    {
        final SymbolImpl referent;
        {
            int levels_up = node.left_hash_loc_levels_up;
            if (levels_up == 1)
            {
                referent = w.getField(node.left_hash_loc_field_num);
            }
            else
            { 
                // levels_up > 1
                Token t = tok;
                for (t = tok, levels_up -= 2; levels_up != 0; levels_up--)
                {
                    t = t.parent;
                }
                referent = t.w.getField(node.left_hash_loc_field_num);
            }
        }

        final int hv = node.node_id ^ referent.hash_id;

        // build new left token, add it to the hash table
        final LeftToken New = new LeftToken(node, tok, w, referent);
        left_ht.insert_token_into_left_ht(New, hv);

        // inform each linked child (positive join) node
        ListItem<ReteNode> next = null;
        for (ListItem<ReteNode> child = node.b_mem().first_linked_child.first; child != null; child = next)
        {
            next = child.item.a_pos().from_beta_mem.next;
            positive_node_left_addition(child.item, New, referent);
        }
    }
    

    /**
     * <p>rete.cpp:4759:unhashed_beta_memory_node_left_addition
     * 
     * @param node
     * @param tok
     * @param w
     */
    private void unhashed_beta_memory_node_left_addition(ReteNode node, Token tok, WmeImpl w)
    {
        final int hv = node.node_id;

        // build new left token, add it to the hash table
        final LeftToken New = new LeftToken(node, tok, w, null);
        left_ht.insert_token_into_left_ht(New, hv);

        // inform each linked child (positive join) node
        ListItem<ReteNode> next = null;
        for (ListItem<ReteNode> child = node.b_mem().first_linked_child.first; child != null; child = next)
        {
            next = child.item.a_pos().from_beta_mem.next;
            unhashed_positive_node_left_addition(child.item, New);
        }
    }    

    /**
     * rete.cpp:4786:positive_node_left_addition
     * 
     * @param node
     * @param New
     * @param hash_referent
     */
    private void positive_node_left_addition(ReteNode node, LeftToken New, SymbolImpl hash_referent)
    {
        final AlphaMemory am = node.b_posneg().alpha_mem_;

        if (node.node_is_right_unlinked())
        {
            node.relink_to_right_mem();
            if (am.right_mems == null)
            {
                node.unlink_from_left_mem();
                return;
            }
        }

        // look through right memory for matches
        final int right_hv = am.am_id ^ hash_referent.hash_id;
        for (RightMemory rm = right_ht.right_ht_bucket(right_hv); rm != null; rm = rm.next_in_bucket)
        {
            if (rm.am != am)
                continue;
            /* --- does rm->w match New? --- */
            if (hash_referent != rm.w.id)
            {
                continue;
            }
            boolean failed_a_test = false;
            for (ReteTest rt = node.b_posneg().other_tests; rt != null; rt = rt.next) {
                if (!ReteTestRoutines.match_left_and_right(rt, New, rm.w))
                {
                    failed_a_test = true;
                    break;
                }
            }
            if (failed_a_test)
            {
                continue;
            }
            /* --- match found, so call each child node --- */
            for (ReteNode child = node.first_child; child != null; child = child.next_sibling)
            {
                executeLeftAddition(child, New, rm.w);
                //left_addition_routines[child.node_type.index()].execute(this, child, New, rm.w);
            }
        }
    }   

    /**
     * rete.cpp:4830:unhashed_positive_node_left_addition
     * 
     * @param node
     * @param New
     */
    private void unhashed_positive_node_left_addition(ReteNode node, LeftToken New)
    {
        if (node.node_is_right_unlinked())
        {
            node.relink_to_right_mem();
            if (node.b_posneg().alpha_mem_.right_mems == null)
            {
                node.unlink_from_left_mem();
                return;
            }
        }

        // look through right memory for matches
        for (RightMemory rm = node.b_posneg().alpha_mem_.right_mems; rm != null; rm = rm.next_in_am)
        {
            /* --- does rm->w match new? --- */
            boolean failed_a_test = false;
            for (ReteTest rt = node.b_posneg().other_tests; rt != null; rt = rt.next)
            {
                if (!ReteTestRoutines.match_left_and_right( rt, New, rm.w))
                {
                    failed_a_test = true;
                    break;
                }
            }
            if (failed_a_test)
            {
                continue;
            }
            /* --- match found, so call each child node --- */
            for (ReteNode child = node.first_child; child != null; child = child.next_sibling)
            {
                executeLeftAddition(child, New, rm.w);
                //left_addition_routines[child.node_type.index()].execute(this, child, New, rm.w);
            }
        }
    }   

    /**
     * rete.cpp:4866:mp_node_left_addition
     * 
     * @param node
     * @param tok
     * @param w
     */
    private void mp_node_left_addition(ReteNode node, Token tok, WmeImpl w)
    {
        final SymbolImpl referent;
        {
            int levels_up = node.left_hash_loc_levels_up;
            if (levels_up == 1)
            {
                referent = w.getField(node.left_hash_loc_field_num);
            }
            else
            { /* --- levels_up > 1 --- */
                Token t = tok;
                for (t = tok, levels_up -= 2; levels_up != 0; levels_up--)
                {
                    t = t.parent;
                }
                referent = t.w.getField(node.left_hash_loc_field_num);
            }
        }

        final int hv = node.node_id ^ referent.hash_id;

        /* --- build new left token, add it to the hash table --- */
        final LeftToken New = new LeftToken(node, tok, w, referent);
        left_ht.insert_token_into_left_ht(New, hv);

        if (node.mp_bnode_is_left_unlinked())
        {
            return;
        }

        final AlphaMemory am = node.b_posneg().alpha_mem_;

        if (node.node_is_right_unlinked())
        {
            node.relink_to_right_mem();
            if (am.right_mems == null)
            {
                node.make_mp_bnode_left_unlinked();
                return;
            }
        }

        /* --- look through right memory for matches --- */
        final int right_hv = am.am_id ^ referent.hash_id;
        for (RightMemory rm = right_ht.right_ht_bucket(right_hv); rm != null; rm = rm.next_in_bucket)
        {
            if (rm.am != am)
            {
                continue;
            }
            /* --- does rm->w match new? --- */
            if (referent != rm.w.id)
            {
                continue;
            }
            boolean failed_a_test = false;
            for (ReteTest rt = node.b_posneg().other_tests; rt != null; rt = rt.next)
                if (!ReteTestRoutines.match_left_and_right(rt, New, rm.w))
                {
                    failed_a_test = true;
                    break;
                }
            if (failed_a_test)
            {
                continue;
            }
            /* --- match found, so call each child node --- */
            for (ReteNode child = node.first_child; child != null; child = child.next_sibling)
            {
                executeLeftAddition(child, New, rm.w);
            }
        }
    }   
    
    /**
     * rete.cpp:4939:unhashed_mp_node_left_addition
     * 
     * @param node
     * @param tok
     * @param w
     */
    private void unhashed_mp_node_left_addition(ReteNode node, Token tok, WmeImpl w)
    {
        int hv = node.node_id;

        /* --- build new left token, add it to the hash table --- */
        LeftToken New = new LeftToken(node, tok, w, null);
        left_ht.insert_token_into_left_ht(New, hv);

        if (node.mp_bnode_is_left_unlinked())
        {
            return;
        }

        if (node.node_is_right_unlinked())
        {
            node.relink_to_right_mem();
            if (node.b_posneg().alpha_mem_.right_mems == null)
            {
                node.make_mp_bnode_left_unlinked();
                return;
            }
        }

        /* --- look through right memory for matches --- */
        for (RightMemory rm = node.b_posneg().alpha_mem_.right_mems; rm != null; rm = rm.next_in_am)
        {
            /* --- does rm->w match new? --- */
            boolean failed_a_test = false;
            for (ReteTest rt = node.b_posneg().other_tests; rt != null; rt = rt.next)
            {
                if (!ReteTestRoutines.match_left_and_right(rt, New, rm.w))
                {
                    failed_a_test = true;
                    break;
                }
            }
            if (failed_a_test)
            {
                continue;
            }
            /* --- match found, so call each child node --- */
            for (ReteNode child = node.first_child; child != null; child = child.next_sibling)
            {
                executeLeftAddition(child, New, rm.w);
                //left_addition_routines[child.node_type.index()].execute(this, child, New, rm.w);
            }
        }
    }

    /**
     * rete.cpp:4989:positive_node_right_addition
     * 
     * @param node
     * @param w
     */
    private void positive_node_right_addition(ReteNode node, WmeImpl w)
    {
        if (node.node_is_left_unlinked())
        {
            node.relink_to_left_mem();
            if (node.parent.a_np().tokens == null)
            {
                node.unlink_from_right_mem();
                return;
            }
        }

        final SymbolImpl referent = w.id;
        final int hv = node.parent.node_id ^ referent.hash_id;

        for (LeftToken tok = left_ht.left_ht_bucket(hv); tok != null; tok = tok.next_in_bucket)
        {
            if (tok.node != node.parent)
            {
                continue;
            }
            /* --- does tok match w? --- */
            if (tok.referent != referent)
            {
                continue;
            }
            boolean failed_a_test = false;
            for (ReteTest rt = node.b_posneg().other_tests; rt != null; rt = rt.next)
            {
                if (!ReteTestRoutines.match_left_and_right(rt, tok, w))
                {
                    failed_a_test = true;
                    break;
                }
            }
            if (failed_a_test)
                continue;
            /* --- match found, so call each child node --- */
            for (ReteNode child = node.first_child; child != null; child = child.next_sibling)
            {
                executeLeftAddition(child, tok, w);
                //left_addition_routines[child.node_type.index()].execute(this, child, tok, w);
            }
        }
    }

    /**
     * rete.cpp:5030:unhashed_positive_node_right_addition
     * 
     * @param node
     * @param w
     */
    private void unhashed_positive_node_right_addition(ReteNode node, WmeImpl w)
    {
        if (node.node_is_left_unlinked())
        {
            node.relink_to_left_mem();
            if (node.parent.a_np().tokens == null)
            {
                node.unlink_from_right_mem();
                return;
            }
        }

        int hv = node.parent.node_id;

        for (LeftToken tok = left_ht.left_ht_bucket(hv); tok != null; tok = tok.next_in_bucket)
        {
            if (tok.node != node.parent)
            {
                continue;
            }
            /* --- does tok match w? --- */
            boolean failed_a_test = false;
            for (ReteTest rt = node.b_posneg().other_tests; rt != null; rt = rt.next)
            {
                if (!ReteTestRoutines.match_left_and_right(rt, tok, w))
                {
                    failed_a_test = true;
                    break;
                }
            }
            if (failed_a_test)
            {
                continue;
            }
            /* --- match found, so call each child node --- */
            for (ReteNode child = node.first_child; child != null; child = child.next_sibling)
            {
                executeLeftAddition(child, tok, w);
                //left_addition_routines[child.node_type.index()].execute(this, child, tok, w);
            }
        }
    }

    /**
     * rete.cpp:5068:mp_node_right_addition
     * 
     * @param node
     * @param w
     */
    private void mp_node_right_addition(ReteNode node, WmeImpl w)
    {
        if (node.mp_bnode_is_left_unlinked())
        {
            node.make_mp_bnode_left_linked();
            if (node.a_np().tokens == null)
            {
                node.unlink_from_right_mem();
                return;
            }
        }

        final SymbolImpl referent = w.id;
        final int hv = node.node_id ^ referent.hash_id;

        for (LeftToken tok = left_ht.left_ht_bucket(hv); tok != null; tok = tok.next_in_bucket)
        {
            if (tok.node != node)
            {
                continue;
            }
            /* --- does tok match w? --- */
            if (tok.referent != referent)
            {
                continue;
            }
            boolean failed_a_test = false;
            for (ReteTest rt = node.b_posneg().other_tests; rt != null; rt = rt.next)
            {
                if (!ReteTestRoutines.match_left_and_right(rt, tok, w))
                {
                    failed_a_test = true;
                    break;
                }
            }
            if (failed_a_test)
            {
                continue;
            }
            /* --- match found, so call each child node --- */
            for (ReteNode child = node.first_child; child != null; child = child.next_sibling)
            {
                executeLeftAddition(child, tok, w);
                //left_addition_routines[child.node_type.index()].execute(this, child, tok, w);
            }
        }
    }
    

    /**
     * rete.cpp:5109:unhashed_mp_node_right_addition
     * @param node
     * @param w
     */
    private void unhashed_mp_node_right_addition(ReteNode node, WmeImpl w)
    {
        if (node.mp_bnode_is_left_unlinked())
        {
            node.make_mp_bnode_left_linked();
            if (node.a_np().tokens == null)
            {
                node.unlink_from_right_mem();
                return;
            }
        }

        int hv = node.node_id;

        for (LeftToken tok = left_ht.left_ht_bucket(hv); tok != null; tok = tok.next_in_bucket)
        {
            if (tok.node != node)
            {
                continue;
            }
            /* --- does tok match w? --- */
            boolean failed_a_test = false;
            for (ReteTest rt = node.b_posneg().other_tests; rt != null; rt = rt.next)
            {
                if (!ReteTestRoutines.match_left_and_right(rt, tok, w))
                {
                    failed_a_test = true;
                    break;
                }
            }
            if (failed_a_test)
            {
                continue;
            }
            /* --- match found, so call each child node --- */
            for (ReteNode child = node.first_child; child != null; child = child.next_sibling)
            {
                executeLeftAddition(child, tok, w);
                ///left_addition_routines[child.node_type.index()].execute(this, child, tok, w);
            }
        }
    }

    /**
     * rete.cpp:5153:negative_node_left_addition
     * 
     * @param node
     * @param tok
     * @param w
     */
    private void negative_node_left_addition(ReteNode node, Token tok, WmeImpl w)
    {
        if (node.node_is_right_unlinked())
        {
            node.relink_to_right_mem();
        }

        SymbolImpl referent = null;
        {
            int levels_up = node.left_hash_loc_levels_up;
            if (levels_up == 1)
            {
                referent = w.getField(node.left_hash_loc_field_num);
            }
            else
            { /* --- levels_up > 1 --- */
                Token t = tok;
                for (levels_up -= 2; levels_up != 0; levels_up--)
                {
                    t = t.parent;
                }
                referent = t.w.getField(node.left_hash_loc_field_num);
            }
        }

        int hv = node.node_id ^ referent.hash_id;

        /* --- build new token, add it to the hash table --- */
        LeftToken New = new LeftToken(node, tok, w, referent);
        left_ht.insert_token_into_left_ht(New, hv);

        /* --- look through right memory for matches --- */
        AlphaMemory am = node.b_posneg().alpha_mem_;
        int right_hv = am.am_id ^ referent.hash_id;
        for (RightMemory rm = right_ht.right_ht_bucket(right_hv); rm != null; rm = rm.next_in_bucket)
        {
            if (rm.am != am)
            {
                continue;
            }
            /* --- does rm->w match new? --- */
            if (referent != rm.w.id)
            {
                continue;
            }
            boolean failed_a_test = false;
            for (ReteTest rt = node.b_posneg().other_tests; rt != null; rt = rt.next)
            {
                if (!ReteTestRoutines.match_left_and_right(rt, New, rm.w))
                {
                    failed_a_test = true;
                    break;
                }
            }
            if (failed_a_test)
            {
                continue;
            }
            RightToken.create(node, null, rm.w, New);
        }

        // if no matches were found, call each child node
        if (!New.hasNegRightTokens())
        {
            for (ReteNode child = node.first_child; child != null; child = child.next_sibling)
            {
                executeLeftAddition(child, New, null);
                //left_addition_routines[child.node_type.index()].execute(this, child, New, null);
            }
        }
    }

    /**
     * rete.cpp:5227:unhashed_negative_node_left_addition
     * 
     * @param node
     * @param tok
     * @param w
     */
    private void unhashed_negative_node_left_addition(ReteNode node, Token tok, WmeImpl w)
    {
        if (node.node_is_right_unlinked())
        {
            node.relink_to_right_mem();
        }

        int hv = node.node_id;

        /* --- build new token, add it to the hash table --- */
        LeftToken New = new LeftToken(node, tok, w, null);
        left_ht.insert_token_into_left_ht(New, hv);

        /* --- look through right memory for matches --- */
        for (RightMemory rm = node.b_posneg().alpha_mem_.right_mems; rm != null; rm = rm.next_in_am)
        {
            /* --- does rm->w match new? --- */
            boolean failed_a_test = false;
            for (ReteTest rt = node.b_posneg().other_tests; rt != null; rt = rt.next)
            {
                if (!ReteTestRoutines.match_left_and_right(rt, New, rm.w))
                {
                    failed_a_test = true;
                    break;
                }
            }
            if (failed_a_test)
            {
                continue;
            }
            RightToken.create(node, null, rm.w, New);
        }

        // if no matches were found, call each child node
        if (!New.hasNegRightTokens())
        {
            for (ReteNode child = node.first_child; child != null; child = child.next_sibling)
            {
                executeLeftAddition(child, New, null);
                //left_addition_routines[child.node_type.index()].execute(this, child, New, null);
            }
        }
    }
    

    /**
     * rete.cpp:5282:negative_node_right_addition
     * 
     * @param node
     * @param w
     */
    private void negative_node_right_addition(ReteNode node, WmeImpl w)
    {
        SymbolImpl referent = w.id;
        int hv = node.node_id ^ referent.hash_id;

        for (LeftToken tok = left_ht.left_ht_bucket(hv); tok != null; tok = tok.next_in_bucket)
        {
            if (tok.node != node)
            {
                continue;
            }
            /* --- does tok match w? --- */
            if (tok.referent != referent)
            {
                continue;
            }
            boolean failed_a_test = false;
            for (ReteTest rt = node.b_posneg().other_tests; rt != null; rt = rt.next)
            {
                if (!ReteTestRoutines.match_left_and_right(rt, tok, w))
                {
                    failed_a_test = true;
                    break;
                }
            }
            if (failed_a_test)
            {
                continue;
            }
            /* --- match found: build new negrm token, remove descendent tokens --- */
            RightToken.create(node, null, w, tok);

            while (tok.first_child != null)
            {
                remove_token_and_subtree(tok.first_child);
            }
        }
    }

    /**
     * rete.cpp:5323:unhashed_negative_node_right_addition
     * 
     * @param node
     * @param w
     */
    private void unhashed_negative_node_right_addition(ReteNode node, WmeImpl w)
    {
        int hv = node.node_id;

        for (LeftToken tok = left_ht.left_ht_bucket(hv); tok != null; tok = tok.next_in_bucket)
        {
            if (tok.node != node)
            {
                continue;
            }
            /* --- does tok match w? --- */
            boolean failed_a_test = false;
            for (ReteTest rt = node.b_posneg().other_tests; rt != null; rt = rt.next)
                if (!ReteTestRoutines.match_left_and_right(rt, tok, w))
                {
                    failed_a_test = true;
                    break;
                }
            if (failed_a_test)
            {
                continue;
            }
            /* --- match found: build new negrm token, remove descendent tokens --- */
            RightToken.create(node, null, w, tok);
            while (tok.first_child != null)
            {
                remove_token_and_subtree(tok.first_child);
            }
        }
    }
    

    /**
     * rete.cpp:5370:cn_node_left_addition
     * 
     * @param node
     * @param tok
     * @param w
     */
    private void cn_node_left_addition(ReteNode node, Token tok, WmeImpl w)
    {
        final int hv = node.node_id ^ addressOf(tok) ^ addressOf(w);

        // look for a matching left token (since the partner node might have
        // heard about this new token already, in which case it would have done
        // the CN node's work already); if found, exit ---
        for (LeftToken t = left_ht.left_ht_bucket(hv); t != null; t = t.next_in_bucket)
        {
            if ((t.node == node) && (t.parent == tok) && (t.w == w))
            {
                return;
            }
        }

        // build left token, add it to the hash table
        final LeftToken New = new LeftToken(node, tok, w, null);
        left_ht.insert_token_into_left_ht(New, hv);

        // pass the new token on to each child node
        for (ReteNode child = node.first_child; child != null; child = child.next_sibling)
        {
            executeLeftAddition(child, New, null);
        }
    }

    /**
     * rete.cpp:5400:cn_partner_node_left_addition
     * 
     * @param node
     * @param tok
     * @param w
     */
    private void cn_partner_node_left_addition(ReteNode node, Token tok, WmeImpl w)
    {
        final ReteNode partner = node.b_cn().partner;

        // build new negrm token
        
        // Can this be created at "negrm_tok.left_token = left;" below so
        // that left_token can be final and list insertion can happen in constructor?
        // Answer: No. I tried this and things didn't work.
        final RightToken negrm_tok = RightToken.create(node, tok, w, null);

        // advance (tok,w) up to the token from the top of the branch
        ReteNode temp = node.parent;
        while (temp != partner.parent)
        {
            temp = temp.real_parent_node();
            w = tok.w;
            tok = tok.parent;
        }

        // look for the matching left token
        final int hv = partner.node_id ^ addressOf(tok) ^ addressOf(w);
        LeftToken left = null;
        for (LeftToken tempLeft = left_ht.left_ht_bucket(hv); tempLeft != null; tempLeft = tempLeft.next_in_bucket)
        {
            if ((tempLeft.node == partner) && (tempLeft.parent == tok) && (tempLeft.w == w))
            {
                left = tempLeft;
                break;
            }
        }

        // if not found, create a new left token
        if (left == null)
        {
            left = new LeftToken(partner, tok, w, null);
            left_ht.insert_token_into_left_ht(left, hv);
        }

        // add new negrm token to the left token
        negrm_tok.setLeftToken(left);

        // remove any descendent tokens of the left token
        while (left.first_child != null)
        {
            remove_token_and_subtree(left.first_child);
        }
    }
    
    /**
     * rete.cpp:5481:p_node_left_addition
     * 
     * @param node
     * @param tok
     * @param w
     */
    private void p_node_left_addition(ReteNode node, Token tok, WmeImpl w)
    {
        // build new left token (used only for tree-based remove)
        @SuppressWarnings("unused")
        LeftToken New = new LeftToken(node, tok, w, null);

        listener.p_node_left_addition(this, node, tok, w);
    }
    
    /**
     * This routine does tree-based removal of a token and its descendents.
     * Note that it uses a nonrecursive tree traversal; each iteration, the
     * leaf being deleted is the leftmost leaf in the tree.
     * 
     * rete.cpp:6083:remove_token_and_subtree
     * 
     * @param root
     */
    /*package*/ void remove_token_and_subtree(Token root)
    {
        Token tok = root;
        
        while (true) {
          // move down to the leftmost leaf
          while (tok.first_child != null) { tok = tok.first_child; }
          final Token next_value_for_tok = tok.getNextSiblingOrParent();

          // cleanup stuff common to all types of nodes
          final ReteNode node = tok.node;
          tok.removeFromNode();
          tok.removeFromParent();
          tok.removeFromWme();
          ReteNodeType node_type = node.node_type;

          // for merged Mem/Pos nodes
          if ((node_type==ReteNodeType.MP_BNODE)||(node_type==ReteNodeType.UNHASHED_MP_BNODE)) {
              LeftToken lt = (LeftToken) tok; // TODO: Assume this is safe?
              int hv = node.node_id ^ (lt.referent != null ? lt.referent.hash_id : 0);
              left_ht.remove_token_from_left_ht(lt, hv);
            if (! node.mp_bnode_is_left_unlinked()) {
              if (node.a_np().tokens == null) { node.unlink_from_right_mem (); }
            }

          // for P nodes
          } else if (node_type==ReteNodeType.P_BNODE) {
            listener.p_node_left_removal(this, node, tok.parent, tok.w);

          // for Negative nodes
          } else if ((node_type==ReteNodeType.NEGATIVE_BNODE) ||
                     (node_type==ReteNodeType.UNHASHED_NEGATIVE_BNODE)) {
            LeftToken lt = (LeftToken) tok; // TODO: Assume this is safe?
            int hv = node.node_id ^ (lt.referent != null ? lt.referent.hash_id : 0);
            left_ht.remove_token_from_left_ht(lt, hv);
            if (node.a_np().tokens == null) { node.unlink_from_right_mem(); }
            for (ListItem<RightToken> t = lt.getFirstNegRightToken(); t != null; t = t.next) {
                t.item.removeFromWme();
            }

          /* --- for Memory nodes --- */
          } else if ((node_type==ReteNodeType.MEMORY_BNODE)||(node_type==ReteNodeType.UNHASHED_MEMORY_BNODE)) {
              LeftToken lt = (LeftToken) tok; // TODO: Assume this is safe?
              int hv = node.node_id ^ (lt.referent != null ? lt.referent.hash_id : 0);
              left_ht.remove_token_from_left_ht(lt, hv);

//      #ifdef DO_ACTIVATION_STATS_ON_REMOVALS
//            /* --- if doing statistics stuff, then activate each attached node --- */
//            for (child=node->b.mem.first_linked_child; child!=NIL; child=next) {
//              next = child->a.pos.next_from_beta_mem;
//              left_node_activation (child,FALSE);
//            }
//      #endif
            /* --- for right unlinking, then if the beta memory just went to
               zero, right unlink any attached Pos nodes --- */
            if (node.a_np().tokens == null) {
                ListItem<ReteNode> next = null;
              for (ListItem<ReteNode> child=node.b_mem().first_linked_child.first; child!=null; child=next) {
                next = child.item.a_pos().from_beta_mem.next;
                child.item.unlink_from_right_mem();
              }
            }

          /* --- for CN nodes --- */
          } else if (node_type==ReteNodeType.CN_BNODE) {
              int hv = node.node_id ^ addressOf(tok.parent) ^ addressOf(tok.w);
            //int hv = node.node_id ^ (unsigned long)(tok->parent) ^ (unsigned long)(tok->w)
              left_ht.remove_token_from_left_ht((LeftToken) tok, hv); // TODO: Safe to assume this?  
            for(ListItem<RightToken> it = ((LeftToken) tok).getFirstNegRightToken(); it != null; it = it.next)
            {
                final Token t = it.item;
                t.removeFromWme();
                t.removeFromNode();
                t.removeFromParent();
            }

          /* --- for CN Partner nodes --- */
          } else if (node_type==ReteNodeType.CN_PARTNER_BNODE) {
            RightToken rt = (RightToken) tok; // TODO: Safe to assume this?
            LeftToken left = rt.getLeftToken();
            left.removeNegRightToken(rt);
            
            if (!left.hasNegRightTokens()) { /* just went to 0, so call children */
              for (ReteNode child=left.node.first_child; child!=null; child=child.next_sibling){
                executeLeftAddition(child, left, null);
              }
            }

          } else {
              throw new IllegalStateException("Internal error: bad node type " + node.node_type + " in remove_token_and_subtree");
          }
          
          if (tok==root) break; /* if leftmost leaf was the root, we're done */
          tok = next_value_for_tok; /* else go get the leftmost leaf again */
        } 
        
    }
    
    /**
     * P_node_to_conditions_and_nots() takes a p_node and (optionally) a
     * token/wme pair, and reconstructs the (optionally instantiated) LHS for
     * the production. If "actions" is non-NIL, it also reconstructs the RHS
     * actions, and fills in actions with the action list. Note: if tok!=NIL,
     * this routine also returns (in nots) the top-level positive {@code "<>"}
     * tests. If tok==NIL, nots is not used.
     * 
     * <p>rete.cpp:4350:p_node_to_conditions_and_nots
     * 
     * @param p_node
     * @param tok
     * @param w
     * @param doRhs If true, RHS will be filled in too
     * @return conditions and nots struct with the reconstructed rule
     */
    public ConditionsAndNots p_node_to_conditions_and_nots(ReteNode p_node, Token tok, WmeImpl w, boolean doRhs)
    {
        ConditionsAndNots result = new ConditionsAndNots();
        
        Production prod = p_node.b_p().prod;
        
        if (tok==null) w=null;  /* just for safety */
        syms.getVariableGenerator().reset(null, null); // we'll be gensymming new vars
        
        ReteNodeToConditionsResult rntc =  rete_node_to_conditions (p_node.parent,
                                 p_node.b_p().parents_nvn,
                                 dummy_top_node,
                                 tok, w, null);
        result.top = rntc.dest_top_cond;
        result.bottom = rntc.dest_bottom_cond;
        
        if (tok != null) result.nots = rntc.nots_found_in_production;
        rntc.nots_found_in_production = null; /* just for safety */
        if (doRhs) 
        {
           this.highest_rhs_unboundvar_index = -1;
           if (!prod.getRhsUnboundVariables().isEmpty()) 
           {
               int i = 0;
               for(SymbolImpl c : prod.getRhsUnboundVariables())
               {
                   this.rhs_variable_bindings[i++] = c;
                   this.highest_rhs_unboundvar_index++;
               }
           }
           result.actions = Action.copy_action_list_and_substitute_varnames (this, prod.getFirstAction(),
                                                                               result.bottom);
           int index = 0;
           while (index <= highest_rhs_unboundvar_index) rhs_variable_bindings[index++] = null;
        }
        
        return result;
    }

    /**
     * This routine adds (an equality test for) each variable in "vn" to the
     * given test "t", destructively modifying t. This is used for restoring the
     * original variables to test in a hand-coded production when we reconstruct
     * its conditions.
     * 
     * rete.cpp:4058:add_varnames_to_test
     * 
     * @param vn
     * @param t
     */
    Test add_varnames_to_test(/*VarNames*/ Object vn, Test t)
    {
        if (vn == null)
            return t;
        
        if (VarNames.varnames_is_one_var(vn))
        {
            Test New = SymbolImpl.makeEqualityTest(VarNames.varnames_to_one_var(vn));
            t = Tests.add_new_test_to_test(t, New);
        }
        else
        {
            for (Variable c : VarNames.varnames_to_var_list(vn))
            {
                Test New = SymbolImpl.makeEqualityTest(c);
                t = Tests.add_new_test_to_test(t, New);
            }
        }
        return t;
    }

    /**
     * Given the additional Rete tests (besides the hashed equality test) at a
     * certain node, we need to convert them into the equivalent tests in the
     * conditions being reconstructed. This procedure does this -- it
     * destructively modifies the given currently-being-reconstructed-cond by
     * adding any necessary extra tests to its three field tests.
     * 
     * rete.cpp:3896:add_rete_test_list_to_tests
     * 
     * @param cond
     * @param rtIn
     */
    void add_rete_test_list_to_tests(ThreeFieldCondition cond, final ReteTest rtIn)
    {
        for (ReteTest rt = rtIn; rt != null; rt = rt.next)
        {
            Test New = null;
            if (rt.type == ReteTest.ID_IS_GOAL)
            {
                New = GoalIdTest.INSTANCE;
            }
            else if (rt.type == ReteTest.ID_IS_IMPASSE)
            {
                New = ImpasseIdTest.INSTANCE;
            }
            else if (rt.type == ReteTest.DISJUNCTION)
            {
                // disjunction test is immutable so this is safe
                New = new DisjunctionTest(rt.disjunction_list);
            }
            else if (rt.test_is_constant_relational_test())
            {
                final int test_type = ReteBuilder.relational_test_type_to_test_type[rt.kind_of_relational_test()];
                SymbolImpl referent = rt.constant_referent;
                if (test_type == ReteBuilder.EQUAL_TEST_TYPE)
                {
                    New = SymbolImpl.makeEqualityTest(referent);
                }
                else
                {
                    New = new RelationalTest(test_type, referent);
                }
            }
            else if (rt.test_is_variable_relational_test())
            {
                final int test_type = ReteBuilder.relational_test_type_to_test_type[rt.kind_of_relational_test()];
                if (rt.variable_referent.levels_up == 0)
                {
                    /* --- before calling var_bound_in_reconstructed_conds, make sure 
                       there's an equality test in the referent location (add one if
                       there isn't one already there), otherwise there'd be no variable
                       there to test against --- */
                    if (rt.variable_referent.field_num == 0)
                    {
                        if (!Tests.test_includes_equality_test_for_symbol(cond.id_test, null))
                        {
                            cond.id_test = add_gensymmed_equality_test(cond.id_test, 's');
                        }
                    }
                    else if (rt.variable_referent.field_num == 1)
                    {
                        if (!Tests.test_includes_equality_test_for_symbol(cond.attr_test, null))
                        {
                            cond.attr_test = add_gensymmed_equality_test(cond.attr_test, 'a');
                        }
                    }
                    else
                    {
                        if (!Tests.test_includes_equality_test_for_symbol(cond.value_test, null))
                        {
                            cond.value_test = add_gensymmed_equality_test(cond.value_test, Tests.first_letter_from_test(cond.attr_test));
                        }
                    }
                }
                SymbolImpl referent = var_bound_in_reconstructed_conds(cond, rt.variable_referent.field_num,
                        rt.variable_referent.levels_up);

                if (test_type == ReteBuilder.EQUAL_TEST_TYPE)
                {
                    New = SymbolImpl.makeEqualityTest(referent);
                }
                else
                {
                    New = new RelationalTest(test_type, referent);
                }
            }
            else
            {

                throw new IllegalStateException("Error: bad test_type in add_rete_test_to_test");
            }

            if (rt.right_field_num == 0)
            {
                cond.id_test = Tests.add_new_test_to_test(cond.id_test, New);
            }
            else if (rt.right_field_num == 2)
            {
                cond.value_test = Tests.add_new_test_to_test(cond.value_test, New);
            }
            else
            {
                cond.attr_test = Tests.add_new_test_to_test(cond.attr_test, New);
            }
        }
    }

    /**
     * When we build the instantiated conditions for a production being fired,
     * we also record all the "<>" tests between pairs of identifiers. (This
     * information is used during chunking.) This procedure looks for any such <>
     * tests in the given Rete test list (from the "other tests" at a Rete
     * node), and adds records of them to the global variable
     * nots_found_in_production. "Right_wme" is the wme that matched the current
     * condition; "cond" is the currently-being-reconstructed condition.
     * 
     * rete.cpp:4000:collect_nots
     * 
     * @param rt
     * @param right_wme
     * @param cond
     * @param nots_found_in_production
     * @return
     */
    NotStruct collect_nots(ReteTest rt, WmeImpl right_wme, Condition cond, NotStruct nots_found_in_production)
    {
        for (; rt != null; rt = rt.next)
        {

            if (!rt.test_is_not_equal_test())
                continue;

            SymbolImpl right_sym = right_wme.getField(rt.right_field_num);

            if (right_sym.asIdentifier() == null)
                continue;

            if (rt.type == ReteTest.CONSTANT_RELATIONAL + ReteTest.RELATIONAL_NOT_EQUAL)
            {
                SymbolImpl referent = rt.constant_referent;
                if (referent.asIdentifier() == null)
                    continue;

                NotStruct new_not = new NotStruct(right_sym.asIdentifier(), referent.asIdentifier());
                new_not.next = nots_found_in_production;
                nots_found_in_production = new_not;
                continue;
            }

            if (rt.type == ReteTest.VARIABLE_RELATIONAL + ReteTest.RELATIONAL_NOT_EQUAL)
            {
                SymbolImpl referent = var_bound_in_reconstructed_conds(cond, rt.variable_referent.field_num,
                        rt.variable_referent.levels_up);
                if (referent.asIdentifier() == null)
                    continue;

                NotStruct new_not = new NotStruct(right_sym.asIdentifier(), referent.asIdentifier());
                new_not.next = nots_found_in_production;
                nots_found_in_production = new_not;
                continue;
            }
        }
        return nots_found_in_production;
    }

    
    /**
     * This routine adds an equality test to the id field test in a given
     * condition, destructively modifying that id test. The equality test is the
     * one appropriate for the given hash location (field_num/levels_up).
     * 
     * rete.cpp:4082:add_hash_info_to_id_test
     * 
     * @param cond
     * @param field_num
     * @param levels_up
     */
    void add_hash_info_to_id_test(ThreeFieldCondition cond, int field_num, int levels_up)
    {
        SymbolImpl temp = var_bound_in_reconstructed_conds(cond, field_num, levels_up);
        Test New = SymbolImpl.makeEqualityTest(temp);

        cond.id_test = Tests.add_new_test_to_test(cond.id_test, New);
    }   
    
    /**
     * This is the main routine for reconstructing the LHS source code, and for
     * building instantiated conditions when a production is fired. It builds
     * the conditions corresponding to the given rete node ("node") and all its
     * ancestors, up to the given "cutoff" node. The given node_varnames
     * structure "nvn", if non-NIL, should be the node_varnames corresponding to
     * "node". <tok,w> (if they are non-NIL) specifies the token/wme pair that
     * emerged from "node" -- these are used only when firing, not when
     * reconstructing. "conds_for_cutoff_and_up" should be the lowermost cond in
     * the already-constructed chain of conditions for the "cutoff" node and
     * higher. "Dest_top_cond" and "bottom" get filled in with the
     * highest and lowest conditions built by this procedure.
     * 
     * Note: Original return by ref parameters in CSoar were replaced by a
     * return structure in Java.
     * 
     * <p>rete.cpp:4113:rete_node_to_conditions
     * 
     * @param node
     * @param nvn
     * @param cutoff
     * @param tok
     * @param w
     * @param conds_for_cutoff_and_up
     * @return
     */
    ReteNodeToConditionsResult rete_node_to_conditions(ReteNode node, NodeVarNames nvn, ReteNode cutoff, Token tok,
            WmeImpl w, Condition conds_for_cutoff_and_up)
    {
        ReteNodeToConditionsResult result = new ReteNodeToConditionsResult();
        // Can't change Condition type on the fly, so this is a little differnt
        // than CSoar...
        Condition cond;
        if (node.node_type == ReteNodeType.CN_BNODE)
        {
            cond = new ConjunctiveNegationCondition();
        }
        else if (node.node_type.bnode_is_positive())
        {
            cond = new PositiveCondition();
        }
        else
        {
            cond = new NegativeCondition(new PositiveCondition());
        }

        if (node.real_parent_node() == cutoff)
        {
            cond.prev = conds_for_cutoff_and_up; /* if this is the top of an NCC, this
                                                        will get replaced by NIL later */
            result.dest_top_cond = cond;
        }
        else
        {
            ReteNodeToConditionsResult sub = rete_node_to_conditions(node.real_parent_node(), 
                    nvn != null ? nvn.parent : null, 
                    cutoff, 
                    tok != null ? tok.parent : null, 
                    tok != null ? tok.w : null,
                    conds_for_cutoff_and_up);
            result.dest_top_cond = sub.dest_top_cond;
            cond.prev = sub.dest_bottom_cond;
            
            // Splice in sub nots at front of result list
            if(sub.nots_found_in_production != null)
            {
                NotStruct tail = sub.nots_found_in_production;
                for(; tail.next != null; tail = tail.next) {}
                tail.next = result.nots_found_in_production;
                result.nots_found_in_production = sub.nots_found_in_production;
            }

            cond.prev.next = cond;
        }
        cond.next = null;
        result.dest_bottom_cond = cond;

        if (node.node_type == ReteNodeType.CN_BNODE)
        {
            ConjunctiveNegationCondition ncc = cond.asConjunctiveNegationCondition();
            ReteNodeToConditionsResult sub = rete_node_to_conditions(node.b_cn().partner.parent,
                    nvn != null ? nvn.bottom_of_subconditions : null, 
                    node.parent, null, null, cond.prev);
            ncc.top = sub.dest_top_cond;
            ncc.bottom = sub.dest_bottom_cond;
            
            // Splice in sub nots at front of result list
            if(sub.nots_found_in_production != null)
            {
                NotStruct tail = sub.nots_found_in_production;
                for(; tail.next != null; tail = tail.next) {}
                tail.next = result.nots_found_in_production;
                result.nots_found_in_production = sub.nots_found_in_production;
            }

            ncc.top.prev = null;
        }
        else
        {
            if (w != null && cond.asPositiveCondition() != null)
            {
                final PositiveCondition pc = cond.asPositiveCondition();
                // make simple tests and collect nots
                pc.id_test = SymbolImpl.makeEqualityTest(w.id);
                pc.attr_test = SymbolImpl.makeEqualityTest(w.attr);
                pc.value_test = SymbolImpl.makeEqualityTest(w.value);
                pc.test_for_acceptable_preference = w.acceptable;
                pc.bt().wme_ = w;
                if (node.b_posneg().other_tests != null)
                { /* don't bother if there are no tests*/
                    result.nots_found_in_production = collect_nots(node.b_posneg().other_tests, w, cond,
                            result.nots_found_in_production);
                }
            }
            else
            {
                // Here (because of w != null in test above), the condition can still be 
                // positive or negative, i.e. just a three-field condition
                final ThreeFieldCondition tfc = cond.asThreeFieldCondition();
                final AlphaMemory am = node.b_posneg().alpha_mem_;
                tfc.id_test = SymbolImpl.makeEqualityTest(am.id);
                tfc.attr_test = SymbolImpl.makeEqualityTest(am.attr);
                tfc.value_test = SymbolImpl.makeEqualityTest(am.value);
                tfc.test_for_acceptable_preference = am.acceptable;

                if (nvn != null)
                {
                    tfc.id_test = add_varnames_to_test(nvn.fields.id_varnames, tfc.id_test);
                    tfc.attr_test = add_varnames_to_test(nvn.fields.attr_varnames, tfc.attr_test);
                    tfc.value_test = add_varnames_to_test(nvn.fields.value_varnames, tfc.value_test);
                }

                // on hashed nodes, add equality test for the hash function
                if ((node.node_type == ReteNodeType.MP_BNODE) || (node.node_type == ReteNodeType.NEGATIVE_BNODE))
                {
                    add_hash_info_to_id_test(tfc, node.left_hash_loc_field_num, node.left_hash_loc_levels_up);
                }
                else if (node.node_type == ReteNodeType.POSITIVE_BNODE)
                {
                    add_hash_info_to_id_test(tfc, 
                                             node.parent.left_hash_loc_field_num,
                                             node.parent.left_hash_loc_levels_up);
                }

                // if there are other tests, add them too
                if (node.b_posneg().other_tests != null)
                {
                    add_rete_test_list_to_tests(tfc, node.b_posneg().other_tests);
                }

                // if we threw away the variable names, make sure there's some 
                //   equality test in each of the three fields
                if (nvn == null)
                {
                    if (!Tests.test_includes_equality_test_for_symbol(tfc.id_test, null))
                    {
                        tfc.id_test = add_gensymmed_equality_test(tfc.id_test, 's');
                    }
                    if (!Tests.test_includes_equality_test_for_symbol(tfc.attr_test, null))
                    {
                        tfc.attr_test = add_gensymmed_equality_test(tfc.attr_test, 'a');
                    }
                    if (!Tests.test_includes_equality_test_for_symbol(tfc.value_test, null))
                    {
                        tfc.value_test = add_gensymmed_equality_test(tfc.value_test, Tests.first_letter_from_test(tfc.attr_test));
                    }
                }
            }
        }
        return result;
    }
    
    public static void print_whole_token_wme(Printer printer, WmeImpl w, WmeTraceType wtt)
    {
        if (w != null)
        {
            if (wtt == WmeTraceType.TIMETAG)
                printer.print("%d", w.timetag);
            else if (wtt == WmeTraceType.FULL)
                printer.print("%s", w);
            if (wtt != WmeTraceType.NONE)
                printer.print(" ");
        }
    }

    /**
     * prints out a given token in the format appropriate for the given
     * wme_trace_type: either a list of timetags, a list of WMEs, or no printout
     * at all.
     * 
     * <p>rete.cpp:7579:print_whole_token
     * 
     * @param printer
     * @param t
     * @param wtt
     */
    void print_whole_token(Printer printer, Token t, WmeTraceType wtt)
    {
        if (t == dummy_top_token)
            return;
        print_whole_token(printer, t.parent, wtt);
        print_whole_token_wme(printer, t.w, wtt);
    }
    
    /**
     * <p>rete.cpp:7541:dummy_matches_node_left_addition
     * 
     * @param node
     * @param tok
     * @param w
     */
    private void dummy_matches_node_left_addition (ReteNode node, Token tok, WmeImpl w) 
    {
        assert node.node_type == ReteNodeType.DUMMY_MATCHES_BNODE;
        // just add a token record to dummy_matches_node_tokens
        Token New = Token.createMatchesToken(tok, w);
        New.next_of_node = dummy_matches_node_tokens;
        this.dummy_matches_node_tokens = New;
    }
    

    /**
     * <p>rete.cpp:7554:get_all_left_tokens_emerging_from_node
     * 
     * @param node
     * @return
     */
    private Token get_all_left_tokens_emerging_from_node(ReteNode node)
    {
        this.dummy_matches_node_tokens = null;
        ReteNode dummy = ReteNode.createMatchesNode(node);
        update_node_with_matches_from_above(dummy);
        Token result = this.dummy_matches_node_tokens;
        this.dummy_matches_node_tokens = null;
        return result;
    }

    /**
     * This is for the "matches" command. Print_partial_match_information() is
     * called from the interface routine; ppmi_aux() is a helper function. We
     * first call p_node_to_conditions_and_nots() to get the condition list for
     * the LHS. We then (conceptually) start at the top of the net, with the
     * first condition; for each condition, we collect the tokens output by the
     * previous node, to find the number of matches here. We print the # of
     * matches here; print this condition. If this is the first cond that didn't
     * have any match, then we also print its matches-for-left and
     * matches-for-right.
     * 
     * <p>Of course, we can't actually start at the top of the net and work our way
     * down, since we'd have no way to find our way the the correct p-node. So
     * instead, we use a recursive procedure that basically does the same thing.
     * 
     * <p>Print stuff for given node and higher, up to but not including the cutoff
     * node. Return number of matches at the given node/cond.
     * 
     * <p>rete.cpp:7611:ppmi_aux
     * 
     * @param printer The printer to print to
     * @param node  The current node
     * @param cutoff  Don't print cutoff node or any higher
     * @param cond  Condition for current node
     * @param wtt  WmeImpl trace type
     * @param indent Indention level
     * @return Number of mathces at this level
     */
    private int ppmi_aux(Printer printer, ReteNode node, ReteNode cutoff, Condition cond, WmeTraceType wtt, int indent)
    {
        int matches_at_this_level = getMatchCountForNode(node);

        // if we're at the cutoff node, we're done
        if (node == cutoff)
            return matches_at_this_level;

        // do stuff higher up
        ReteNode parent = node.real_parent_node();
        final int matches_one_level_up = ppmi_aux(printer, parent, cutoff, cond.prev, wtt, indent);

        // Form string for current match count: If an earlier cond had no
        // matches, just leave it blank; if this is the first 0, use ">>>>"
        String match_count_string = "";
        if (matches_one_level_up == 0)
        {
            match_count_string = "    ";
        }
        else if (matches_at_this_level == 0)
        {
            match_count_string = ">>>>";
        }
        else
        {
            match_count_string = String.format("%4d", matches_at_this_level);
        }

        // print extra indentation spaces
        printer.spaces(indent);

        ConjunctiveNegationCondition ncc = cond.asConjunctiveNegationCondition();
        if (ncc != null)
        {
            // recursively print match counts for the NCC subconditions
            printer.print("    -{\n");
            ppmi_aux(printer, node.b_cn().partner.real_parent_node(), parent, ncc.bottom, wtt, indent + 5);
            printer.spaces(indent).print("%s }\n", match_count_string);
        }
        else
        {
            printer.print("%s%s\n", match_count_string, cond);
            // if this is the first match-failure (0 matches), print info on
            // matches for left and right
            if (matches_one_level_up != 0 && matches_at_this_level == 0)
            {
                if (wtt != WmeTraceType.NONE)
                {
                    printer.spaces(indent).print("*** Matches For Left ***\n");
                    final Token parent_tokens = get_all_left_tokens_emerging_from_node(parent);
                    for (Token t = parent_tokens; t != null; t = t.next_of_node)
                    {
                        printer.spaces(indent);
                        print_whole_token(printer, t, wtt);
                        printer.print("\n");
                    }
                    printer.spaces(indent).print("*** Matches for Right ***\n").spaces(indent);
                    for (RightMemory rm = node.b_posneg().alpha_mem_.right_mems; rm != null; rm = rm.next_in_am)
                    {
                        if (wtt == WmeTraceType.TIMETAG)
                            printer.print("%d", rm.w.timetag);
                        else if (wtt == WmeTraceType.FULL)
                            printer.print("%s", rm.w);
                        printer.print(" ");
                    }
                    printer.print("\n");
                }
            }
        }

        return matches_at_this_level;
    }

    /**
     * Print partial matches for the given p-node. Client code should call
     * {@link Production#printPartialMatches(Printer, WmeTraceType)}
     * 
     * <p>rete.cpp:7700:print_partial_match_information
     * 
     * @param printer
     * @param p_node
     * @param wtt
     */
    public void print_partial_match_information(Printer printer, ReteNode p_node, WmeTraceType wtt)
    {
        ConditionsAndNots cans = p_node_to_conditions_and_nots(p_node, null, null, false);
        
        int n = ppmi_aux(printer, p_node.parent, dummy_top_node, cans.bottom, wtt, 0);
        
        printer.print("\n%d complete matches.\n", n);
        if (n != 0 && (wtt != WmeTraceType.NONE))
        {
            printer.print("*** Complete Matches ***\n");
            Token tokens = get_all_left_tokens_emerging_from_node(p_node.parent);
            for (Token t = tokens; t != null; t = t.next_of_node)
            {
                print_whole_token(printer, t, wtt);
                printer.print("\n");
            }
        }
    }

    private List<Entry> getPartialMatchesAux(List<Entry> entries, ReteNode node, ReteNode cutoff, Condition cond)
    {
        final int matches_at_this_level = getMatchCountForNode(node);

        // if we're at the cutoff node, we're done
        if (node == cutoff)
        {
            return entries;
        }

        // do stuff higher up
        final ReteNode parent = node.real_parent_node();
        getPartialMatchesAux(entries, parent, cutoff, cond.prev);

        final ConjunctiveNegationCondition ncc = cond.asConjunctiveNegationCondition();
        if (ncc != null)
        {
            // recursively print match counts for the NCC subconditions
            entries.add(new Entry(cond, matches_at_this_level, 
                    getPartialMatchesAux(new ArrayList<Entry>(), node.b_cn().partner.real_parent_node(), parent, ncc.bottom)));
        }
        else
        {
            entries.add(new Entry(cond, matches_at_this_level, null));
        }

        return entries;
    }

    public PartialMatches getPartialMatches(ReteNode p_node)
    {
        final ConditionsAndNots cans = p_node_to_conditions_and_nots(p_node, null, null, false);
        final List<PartialMatches.Entry> entries = getPartialMatchesAux(new ArrayList<Entry>(), p_node.parent, dummy_top_node , cans.bottom);
        return new PartialMatches(entries);
    }
    
    /**
     * Extracted from ppmi_aux method.
     * 
     * @param node a rete node
     * @return the number of matches for in the node (left token count)
     */
    private int getMatchCountForNode(ReteNode node)
    {
        // find the number of matches for this condition
        final Token tokens = get_all_left_tokens_emerging_from_node(node);
        int matches_at_this_level = 0;
        for (Token t = tokens; t != null; t = t.next_of_node)
            matches_at_this_level++;
        return matches_at_this_level;
    }
    
    
    /**
     * Returns a count of the number of tokens currently in use for the given
     * production. The count does not include:
     * <ul>
     * <li> tokens in the p_node (i.e., tokens representing complete matches) 
     * <li>local join result tokens on (real) tokens in negative/NCC nodes
     * </ul>
     * 
     * <p>Client code should call {@link Production#getReteTokenCount()}
     * 
     * <p>rete.cpp:7346:count_rete_tokens_for_production
     * 
     * @param p_node the production p-node
     * @return token count, or 0 if p_node is <code>null</code>
     */
    public int count_rete_tokens_for_production(ReteNode p_node)
    {
        if (p_node == null)
            return 0;
        ReteNode node = p_node.parent;
        int count = 0;
        while (node != dummy_top_node)
        {
            if ((node.node_type != ReteNodeType.POSITIVE_BNODE)
                    && (node.node_type != ReteNodeType.UNHASHED_POSITIVE_BNODE))
            {
                for (Token tok = node.a_np().tokens; tok != null; tok = tok.next_of_node)
                    count++;
            }
            if (node.node_type == ReteNodeType.CN_BNODE)
                node = node.b_cn().partner.parent;
            else
                node = node.parent;
        }
        return count;
    }
}
