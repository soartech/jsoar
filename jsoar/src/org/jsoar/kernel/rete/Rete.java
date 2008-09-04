/*
 * (c) 2008  Dave Ray
 *
 * Created on Aug 22, 2008
 */
package org.jsoar.kernel.rete;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedList;
import java.util.List;

import org.jsoar.kernel.AssertListType;
import org.jsoar.kernel.MatchSetChange;
import org.jsoar.kernel.PreferenceType;
import org.jsoar.kernel.Production;
import org.jsoar.kernel.ProductionSupport;
import org.jsoar.kernel.ProductionType;
import org.jsoar.kernel.SavedFiringType;
import org.jsoar.kernel.VariableGenerator;
import org.jsoar.kernel.Wme;
import org.jsoar.kernel.lhs.Condition;
import org.jsoar.kernel.lhs.ConjunctiveTest;
import org.jsoar.kernel.lhs.EqualityTest;
import org.jsoar.kernel.lhs.Test;
import org.jsoar.kernel.lhs.TestTools;
import org.jsoar.kernel.lhs.ThreeFieldCondition;
import org.jsoar.kernel.rhs.Action;
import org.jsoar.kernel.rhs.FunctionAction;
import org.jsoar.kernel.rhs.MakeAction;
import org.jsoar.kernel.rhs.ReteLocation;
import org.jsoar.kernel.rhs.RhsSymbolValue;
import org.jsoar.kernel.rhs.RhsValue;
import org.jsoar.kernel.symbols.Identifier;
import org.jsoar.kernel.symbols.Symbol;
import org.jsoar.kernel.symbols.Variable;
import org.jsoar.util.AsListItem;
import org.jsoar.util.ByRef;
import org.jsoar.util.ListHead;
import org.jsoar.util.SoarHashTable;

/**
 * @author ray
 */
public class Rete
{

    private static final LeftAdditionRoutine[] left_addition_routines = new LeftAdditionRoutine[256];
    static
    {
        // rete.cpp:8796 
// TODO:       left_addition_routines[DUMMY_MATCHES_BNODE] = dummy_matches_node_left_addition;
        left_addition_routines[ReteNodeType.MEMORY_BNODE.index()] = new LeftAdditionRoutine() {

            @Override
            public void execute(Rete rete, ReteNode node, Token tok, Wme w)
            {
                rete.beta_memory_node_left_addition(node, tok, w);
            }};
        left_addition_routines[ReteNodeType.UNHASHED_MEMORY_BNODE.index()] = new LeftAdditionRoutine() {

            @Override
            public void execute(Rete rete, ReteNode node, Token tok, Wme w)
            {
                rete.unhashed_beta_memory_node_left_addition(node, tok, w);
            }
            
        };
        left_addition_routines[ReteNodeType.MP_BNODE.index()] = new LeftAdditionRoutine() {

            @Override
            public void execute(Rete rete, ReteNode node, Token tok, Wme w)
            {
                rete.mp_node_left_addition(node, tok, w);
            } };
        left_addition_routines[ReteNodeType.UNHASHED_MP_BNODE.index()] = new LeftAdditionRoutine() {

            @Override
            public void execute(Rete rete, ReteNode node, Token tok, Wme w)
            {
                rete.unhashed_mp_node_left_addition(node, tok, w);
            }};
        left_addition_routines[ReteNodeType.CN_BNODE.index()] = new LeftAdditionRoutine() {

            @Override
            public void execute(Rete rete, ReteNode node, Token tok, Wme w)
            {
                rete.cn_node_left_addition(node, tok, w);
            }};
        left_addition_routines[ReteNodeType.CN_PARTNER_BNODE.index()] = new LeftAdditionRoutine() {

            @Override
            public void execute(Rete rete, ReteNode node, Token tok, Wme w)
            {
                rete.cn_partner_node_left_addition(node, tok, w);
            }};
        left_addition_routines[ReteNodeType.P_BNODE.index()] = new LeftAdditionRoutine() {

            @Override
            public void execute(Rete rete, ReteNode node, Token tok, Wme w)
            {
                rete.p_node_left_addition(node, tok, w);
            }};
        left_addition_routines[ReteNodeType.NEGATIVE_BNODE.index()] = new LeftAdditionRoutine() {

            @Override
            public void execute(Rete rete, ReteNode node, Token tok, Wme w)
            {
                rete.negative_node_left_addition(node, tok, w);
            }};
        left_addition_routines[ReteNodeType.UNHASHED_NEGATIVE_BNODE.index()] = new LeftAdditionRoutine() {

            @Override
            public void execute(Rete rete, ReteNode node, Token tok, Wme w)
            {
                rete.unhashed_negative_node_left_addition(node, tok, w);
            }};
    }
    private static final RightAdditionRoutine[] right_addition_routines = new RightAdditionRoutine[256];
    static
    {
        right_addition_routines[ReteNodeType.POSITIVE_BNODE.index()] = new RightAdditionRoutine() {

            @Override
            public void execute(Rete rete, ReteNode node, Wme w)
            {
                rete.positive_node_right_addition(node, w);
            }};
        right_addition_routines[ReteNodeType.UNHASHED_POSITIVE_BNODE.index()] = new RightAdditionRoutine() {

            @Override
            public void execute(Rete rete, ReteNode node, Wme w)
            {
                rete.unhashed_positive_node_right_addition(node, w);
            }};
        right_addition_routines[ReteNodeType.MP_BNODE.index()] = new RightAdditionRoutine() {

            @Override
            public void execute(Rete rete, ReteNode node, Wme w)
            {
                rete.mp_node_right_addition(node, w);
            }};
        right_addition_routines[ReteNodeType.UNHASHED_MP_BNODE.index()] = new RightAdditionRoutine() {

            @Override
            public void execute(Rete rete, ReteNode node, Wme w)
            {
                rete.unhashed_mp_node_right_addition(node, w);
            }};
        right_addition_routines[ReteNodeType.NEGATIVE_BNODE.index()] = new RightAdditionRoutine() {

            @Override
            public void execute(Rete rete, ReteNode node, Wme w)
            {
                rete.negative_node_right_addition(node, w);
            }};
        right_addition_routines[ReteNodeType.UNHASHED_NEGATIVE_BNODE.index()] = new RightAdditionRoutine(){

            @Override
            public void execute(Rete rete, ReteNode node, Wme w)
            {
                rete.unhashed_negative_node_right_addition(node, w);
            }};
    }
    
    /**
     * rete.cpp:4417:rete_test_routines
     */
    private static final ReteTestRoutine rete_test_routines[] = new ReteTestRoutine[256];
    static
    {
        // TODO
    }
    
    /* Set to FALSE to preserve variable names in chunks (takes extra space) */
    private final boolean discard_chunk_varnames = true;
    
    private LeftTokenHashTable left_ht = new LeftTokenHashTable();
    private RightMemoryHashTable right_ht = new RightMemoryHashTable();
    int rete_node_counts[] = new int[256];
    private RightToken dummy_top_token;
    
    /**
     * false is Soar 7 mode
     * 
     * agent.h:728
     */
    private boolean operand2_mode = true;
    
    /**
     * agent.h:733
     * dll of all retractions for removed (ie nil) goals
     */
    private final ListHead<MatchSetChange> nil_goal_retractions = new ListHead<MatchSetChange>();
    
    /**
     * changes to match set
     * 
     * agent.h:231
     */
    private final ListHead<MatchSetChange> ms_assertions = new ListHead<MatchSetChange>();
    
    /**
     * agent.h:231
     */
    private final ListHead<MatchSetChange> ms_retractions = new ListHead<MatchSetChange>();
    
    /**
     * changes to match set
     * 
     * agent.h:723
     */
    private final ListHead<MatchSetChange> ms_o_assertions = new ListHead<MatchSetChange>();
    private final ListHead<MatchSetChange> ms_i_assertions = new ListHead<MatchSetChange>();
    
    private int alpha_mem_id_counter; 
    private List<SoarHashTable<AlphaMemory>> alpha_hash_tables;
    private ListHead<Wme> all_wmes_in_rete = new ListHead<Wme>();
    private int num_wmes_in_rete= 0;
    private int beta_node_id_counter;
    ReteNode dummy_top_node;
    
    Symbol[] rhs_variable_bindings = {};
    private VariableGenerator variableGenerator;
    
    private Identifier operator_symbol;
    private int o_support_calculation_type = 4;
    
    public Rete(VariableGenerator variableGenerator)
    {
        this.variableGenerator = variableGenerator;
        // rete.cpp:8864
        alpha_hash_tables = new ArrayList<SoarHashTable<AlphaMemory>>(16);
        for(int i = 0; i < 16; ++i)
        {
            alpha_hash_tables.add(new SoarHashTable<AlphaMemory>(0, AlphaMemory.HASH_FUNCTION));
        }
        
        init_dummy_top_node();
    }
    
    public ProductionAddResult add_production_to_rete(Production p)
    {
        return add_production_to_rete(p, p.condition_list, null, true, false);
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
     * rete.cpp:3515:add_production_to_rete
     * 
     * @param p
     * @param lhs_top
     * @param refracted_inst
     * @param warn_on_duplicates
     * @param ignore_rhs
     * @return
     */
    public ProductionAddResult add_production_to_rete(Production p, Condition lhs_top, Instantiation refracted_inst,
            boolean warn_on_duplicates, boolean ignore_rhs)
    {
        ProductionAddResult production_addition_result;

        ReteBuilder builder = new ReteBuilder();
        ByRef<ReteNode> bottom_node = ByRef.create(null);
        ByRef<Integer> bottom_depth = ByRef.create(0);
        ByRef<LinkedList<Variable>> vars_bound = ByRef.create(null);
        /* --- build the network for all the conditions --- */
        builder.build_network_for_condition_list(this, lhs_top, 1, dummy_top_node, bottom_node, bottom_depth,
                vars_bound);

        /*
         * --- change variable names in RHS to Rete location references or
         * unbound variable indices ---
         */
        LinkedList<Variable> rhs_unbound_vars_for_new_prod = new LinkedList<Variable>();
        ByRef<Integer> num_rhs_unbound_vars_for_new_prod = ByRef.create(0);
        int rhs_unbound_vars_tc = variableGenerator.getSyms().get_new_tc_number();
        for (Action a = p.action_list; a != null; a = a.next)
        {
            MakeAction ma = a.asMakeAction();
            if (ma != null)
            {
                // TODO: ByRef usage here is pretty bad. Refactor.
                ByRef<RhsValue> a_value = ByRef.create(ma.value);
                builder.fixup_rhs_value_variable_references(this, a_value, bottom_depth.value,
                        rhs_unbound_vars_for_new_prod, num_rhs_unbound_vars_for_new_prod, rhs_unbound_vars_tc);
                ma.value = a_value.value;

                ByRef<RhsValue> a_id = ByRef.create(ma.id);
                builder.fixup_rhs_value_variable_references(this, a_id, bottom_depth.value,
                        rhs_unbound_vars_for_new_prod, num_rhs_unbound_vars_for_new_prod, rhs_unbound_vars_tc);
                ma.id = (RhsSymbolValue) a_id.value; // TODO: Yucky

                ByRef<RhsValue> a_attr = ByRef.create(ma.attr);
                builder.fixup_rhs_value_variable_references(this, a_attr, bottom_depth.value,
                        rhs_unbound_vars_for_new_prod, num_rhs_unbound_vars_for_new_prod, rhs_unbound_vars_tc);
                ma.attr = a_attr.value;

                if (a.preference_type.isBinary())
                {
                    ByRef<RhsValue> a_referent = ByRef.create(ma.referent);
                    builder.fixup_rhs_value_variable_references(this, a_referent, bottom_depth.value,
                            rhs_unbound_vars_for_new_prod, num_rhs_unbound_vars_for_new_prod, rhs_unbound_vars_tc);
                    ma.referent = a_referent.value;
                }
            }
            else
            {
                FunctionAction fa = a.asFunctionAction();
                ByRef<RhsValue> a_value = ByRef.create(fa.call);
                builder.fixup_rhs_value_variable_references(this, a_value, bottom_depth.value,
                        rhs_unbound_vars_for_new_prod, num_rhs_unbound_vars_for_new_prod, rhs_unbound_vars_tc);

            }
        }

        /* --- clean up variable bindings created by build_network...() --- */

        pop_bindings_and_deallocate_list_of_variables(vars_bound.value);

        update_max_rhs_unbound_variables(num_rhs_unbound_vars_for_new_prod.value);

        /* --- look for an existing p node that matches --- */
        for (ReteNode p_node = bottom_node.value.first_child; p_node != null; p_node = p_node.next_sibling)
        {
            if (p_node.node_type != ReteNodeType.P_BNODE)
            {
                continue;
            }
            if (!ignore_rhs && !Action.same_rhs(p_node.b_p.prod.action_list, p.action_list))
            {
                continue;
            }
            /* --- duplicate production found --- */
            if (warn_on_duplicates)
            {
                // TODO: Warn
                // TODO: Test
                // std::stringstream output;
                // output << "\nIgnoring "
                // << symbol_to_string( thisAgent, p->name, TRUE, 0, 0 )
                // << " because it is a duplicate of "
                // << symbol_to_string( thisAgent, p_node->b.p.prod->name, TRUE,
                // 0, 0 )
                // << " ";
                // xml_generate_warning( thisAgent, output.str().c_str() );
                //
                // print_with_symbols (thisAgent, "\nIgnoring %y because it is a
                // duplicate of %y ",
                // p->name, p_node->b.p.prod->name);
            }
            // deallocate_symbol_list_removing_references (thisAgent,
            // rhs_unbound_vars_for_new_prod);
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
         * situation: a PE chunk was created during the IE phase. that
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

        /*
         * --- handle initial refraction by adding it to tentative_retractions
         * ---
         */
        if (refracted_inst != null)
        {
            refracted_inst.inProdList.insertAtHead(p.instantiations);
            refracted_inst.rete_token = null;
            refracted_inst.rete_wme = null;
            MatchSetChange msc = new MatchSetChange();
            msc.inst = refracted_inst;
            msc.p_node = p_node;
            /* REW: begin 08.20.97 */
            /*
             * Because the RETE 'artificially' refracts this instantiation (ie,
             * it is not actually firing -- the original instantiation fires but
             * not the chunk), we make the refracted instantiation of the chunk
             * a nil_goal retraction, rather than associating it with the
             * activity of its match goal. In p_node_left_addition, where the
             * tentative assertion will be generated, we make it a point to look
             * at the goal value and exrtac from the appropriate list; here we
             * just make a a simplifying assumption that the goal is NIL
             * (although, in reality), it never will be.
             */

            /*
             * This initialization is necessary (for at least safety reasons,
             * for all msc's, regardless of the mode
             */
            msc.level = 0;
            msc.goal = null;
            if (operand2_mode)
            {

                // #ifdef DEBUG_WATERFALL
                // print_with_symbols(thisAgent, "\n %y is a refracted
                // instantiation",
                // refracted_inst->prod->name);
                // #endif
                msc.in_level.insertAtHead(nil_goal_retractions);
            }
            /* REW: end 08.20.97 */

            // TODO: Is BUG_139_WORKAROUND needed?
            // #ifdef BUG_139_WORKAROUND
            msc.p_node.b_p.prod.already_fired = false; // RPM workaround for bug #139; mark prod as not fired yet */
            // #endif
            msc.next_prev.insertAtHead(ms_retractions);
            msc.of_node.insertAtHead(p_node.b_p.tentative_retractions);
        }

        /* --- call new node's add_left routine with all the parent's tokens --- */
        update_node_with_matches_from_above(p_node);

        /* --- store result indicator --- */
        if (refracted_inst == null)
        {
            production_addition_result = ProductionAddResult.NO_REFRACTED_INST;
        }
        else
        {
            refracted_inst.inProdList.remove(p.instantiations);
            if (!p_node.b_p.tentative_retractions.isEmpty())
            {
                production_addition_result = ProductionAddResult.REFRACTED_INST_DID_NOT_MATCH;
                MatchSetChange msc = p_node.b_p.tentative_retractions.first.get();
                p_node.b_p.tentative_retractions.first = null;
                msc.next_prev.remove(ms_retractions);
                /* REW: begin 10.03.97 *//* BUGFIX 2.125 */
                if (operand2_mode)
                {
                    if (msc.goal != null)
                    {
                        msc.in_level.remove(msc.goal.ms_retractions);
                    }
                    else
                    {
                        msc.in_level.remove(nil_goal_retractions);
                    }
                }
                /* REW: end   10.03.97 */

            }
            else
            {
                production_addition_result = ProductionAddResult.REFRACTED_INST_MATCHED;
            }
        }

        /* --- if not a chunk, store variable name information --- */
        if ((p.type == ProductionType.CHUNK_PRODUCTION_TYPE) && discard_chunk_varnames)
        {
            p.p_node.b_p.parents_nvn = null;
            p.rhs_unbound_variables.clear();
            //deallocate_symbol_list_removing_references (thisAgent, rhs_unbound_vars_for_new_prod);
        }
        else
        {
            p.p_node.b_p.parents_nvn = NodeVarNames.get_nvn_for_condition_list(lhs_top, null);
            p.rhs_unbound_variables.addAll(rhs_unbound_vars_for_new_prod);
            Collections.reverse(p.rhs_unbound_variables);
            //p->rhs_unbound_variables = destructively_reverse_list (rhs_unbound_vars_for_new_prod);
        }

        /* --- invoke callback functions --- */
        // TODO: Callback
        //  soar_invoke_callbacks (thisAgent, PRODUCTION_JUST_ADDED_CALLBACK,
        //                         (soar_call_data) p);
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
        // TODO: Callback
        // soar_invoke_callbacks (thisAgent,
        // PRODUCTION_JUST_ABOUT_TO_BE_EXCISED_CALLBACK,
        // (soar_call_data) p);

        // #ifdef _WINDOWS
        // remove_production_from_stat_lists(prod_to_be_excised);
        // #endif

        ReteNode p_node = p.p_node;
        p.p_node = null; /* mark production as not being in the rete anymore */
        ReteNode parent = p_node.parent;

        /* --- deallocate the variable name information --- */
        if (p_node.b_p.parents_nvn != null)
        {
            NodeVarNames.deallocate_node_varnames(parent, dummy_top_node, p_node.b_p.parents_nvn);
        }

        /*
         * --- cause all existing instantiations to retract, by removing any
         * tokens at the node ---
         */
        while (!p_node.a_np.tokens.isEmpty())
        {
            remove_token_and_subtree(p_node.a_np.tokens);
        }

        /*
         * --- At this point, there are no tentative_assertion's. Now set the
         * p_node field of all tentative_retractions to NIL, to indicate that
         * the p_node is being excised ---
         */
        for (MatchSetChange msc : p_node.b_p.tentative_retractions)
        {
            msc.p_node = null;
        }

        /* --- finally, excise the p_node --- */
        p_node.remove_node_from_parents_list_of_children();
        // update_stats_for_destroying_node (thisAgent, p_node); // TODO: clean
        // up rete stats stuff

        /* --- and propogate up the net --- */
        if (parent.first_child == null)
        {
            ReteNode.deallocate_rete_node(this, parent);
        }
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
    public void add_wme_to_rete (Wme w)
    {
        /* --- add w to all_wmes_in_rete --- */
        w.in_rete.insertAtHead(all_wmes_in_rete);
        num_wmes_in_rete++;

        /* --- it's not in any right memories or tokens yet --- */
        w.right_mems.first = null;
        w.tokens.first = null;

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
        
    }
    
    /**
     * Remove a WME from the rete.
     * 
     * rete.cpp:1591:remove_wme_from_rete
     * 
     * @param w The WME to remove
     */
    public void remove_wme_from_rete (Wme w)
    {
        /* --- remove w from all_wmes_in_rete --- */
        w.in_rete.remove(all_wmes_in_rete);
        num_wmes_in_rete--;
        
        /* --- remove w from each alpha_mem it's in --- */
        while (!w.right_mems.isEmpty()) {
          RightMemory rm = w.right_mems.first.get();
          AlphaMemory am = rm.am;
          /* --- found the alpha memory, first remove the wme from it --- */
          remove_wme_from_alpha_mem (rm);
          
//      #ifdef DO_ACTIVATION_STATS_ON_REMOVALS
//          /* --- if doing statistics stuff, then activate each attached node --- */
//          for (node=am->beta_nodes; node!=NIL; node=next) {
//            next = node->b.posneg.next_from_alpha_mem;
//            right_node_activation (node,FALSE);
//          }
//      #endif
          
          /* --- for left unlinking, then if the alpha memory just went to
             zero, left unlink any attached Pos or MP nodes --- */
          if (am.right_mems.isEmpty()) {
              ReteNode next = null;
            for (ReteNode node=am.beta_nodes; node!=null; node=next) {
              next = node.b_posneg.next_from_alpha_mem;
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
        while (!w.tokens.isEmpty()) {
          Token tok = w.tokens.first.get();
          ReteNode node = tok.node;
          if (tok.parent == null) {
            /* Note: parent pointer is NIL only on negative node negrm tokens */
              RightToken rt = (RightToken) tok;
            Token left = rt.left_token;
            tok.from_wme.remove(w.tokens);
            rt.negrm.remove(left.negrm_tokens);

            if (left.negrm_tokens.isEmpty()) { /* just went to 0, so call children */
              for (ReteNode child=node.first_child; child!=null; child=child.next_sibling) {
                left_addition_routines[child.node_type.index()].execute(this, child, left, null);
              }
            }
          } else {
            remove_token_and_subtree (w.tokens);
          }
        }
        
    }
    
    /**
     * rete.cpp:6083:remove_token_and_subtree
     * 
     * @param tokens
     */
    private void remove_token_and_subtree(ListHead<Token> root)
    {
        // TODO: implement remove_token_and_subtree()
    }

    /* --- Invoked on every right activation; add=TRUE means right addition --- */
    /* NOT invoked on removals unless DO_ACTIVATION_STATS_ON_REMOVALS is set */
    void right_node_activation(ReteNode node, boolean add)
    {
        // TODO: Delete this?
      //null_activation_stats_for_right_activation(node, null);
    }

    /* --- Invoked on every left activation; add=TRUE means left addition --- */
    /* NOT invoked on removals unless DO_ACTIVATION_STATS_ON_REMOVALS is set */
    void left_node_activation(ReteNode node, boolean add)
    {
        // TODO: Delete this?
      //null_activation_stats_for_left_activation(node);
    }
    /**
     * rete.cpp:1011:find_goal_for_match_set_change_assertion
     * @param msc
     * @return
     */
    Identifier find_goal_for_match_set_change_assertion(MatchSetChange msc) {

//      #ifdef DEBUG_WATERFALL
//        print_with_symbols(thisAgent, "\nMatch goal for assertion: %y", msc->p_node->b.p.prod->name); 
//      #endif


        Wme lowest_goal_wme = null;
        int lowest_level_so_far = -1;

        if (msc.w != null) {
            if (msc.w.id.isa_goal) {
              lowest_goal_wme = msc.w;
              lowest_level_so_far = msc.w.id.level;
            }
        }

        for (Token tok=msc.tok; tok!=dummy_top_token; tok=tok.parent) {
          if (tok.w != null) {
            /* print_wme(tok->w); */
            if (tok.w.id.isa_goal) {

              if (lowest_goal_wme == null)
                lowest_goal_wme = tok.w;
              
              else {
                if (tok.w.id.level > lowest_goal_wme.id.level)
                  lowest_goal_wme = tok.w;
              }
            }
             
          }
        } 

        if (lowest_goal_wme != null) {
//      #ifdef DEBUG_WATERFALL
//          print_with_symbols(thisAgent, " is [%y]", lowest_goal_wme->id);
//      #endif
             return lowest_goal_wme.id;
        }
//        { 
            throw new IllegalStateException("\nError: Did not find goal for ms_change assertion: " + msc.p_node.b_p.prod.name);
            // TODO: Fatal Error
//            char msg[BUFFER_MSG_SIZE];
//        print_with_symbols(thisAgent, "\nError: Did not find goal for ms_change assertion: %y\n", msc->p_node->b.p.prod->name);
//        SNPRINTF(msg, BUFFER_MSG_SIZE,"\nError: Did not find goal for ms_change assertion: %s\n",
//               symbol_to_string(thisAgent, msc->p_node->b.p.prod->name,TRUE,NIL, 0));
//        msg[BUFFER_MSG_SIZE - 1] = 0; /* ensure null termination */
//        abort_with_fatal_error(thisAgent, msg);
//        }
//        return 0;
      }
    
    /**
     * rete.cpp:1065:find_goal_for_match_set_change_retraction
     * 
     * @param msc
     * @return
     */
    Identifier find_goal_for_match_set_change_retraction(MatchSetChange msc)
    {
        // #ifdef DEBUG_WATERFALL
        // print_with_symbols(thisAgent, "\nMatch goal level for retraction:
        // %y", msc->inst->prod->name);
        // #endif

        if (msc.inst.match_goal != null)
        {
            // If there is a goal, just return the goal
            // #ifdef DEBUG_WATERFALL
            // print_with_symbols(thisAgent, " is [%y]", msc->inst->match_goal);
            // #endif
            return msc.inst.match_goal;
        }
        else
        {
            // #ifdef DEBUG_WATERFALL
            // print(" is NIL (nil goal retraction)");
            //        #endif 
            return null;
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
    void add_wme_to_alpha_mem(Wme w, AlphaMemory am)
    {
        /* --- allocate new right_mem, fill it fields --- */
        RightMemory rm = new RightMemory();
        rm.w = w;
        rm.am = am;

        /* --- add it to dll's for the hash bucket, alpha mem, and wme --- */
        int hv = am.am_id ^ w.id.hash_id;
        ListHead<RightMemory> header = right_ht.right_ht_bucket(hv);
        rm.in_bucket.insertAtHead(header);
        rm.in_am.insertAtHead(am.right_mems);
        rm.from_wme.insertAtHead(w.right_mems);
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
        Wme w = rm.w;
        AlphaMemory am = rm.am;

        /* --- remove it from dll's for the hash bucket, alpha mem, and wme --- */
        int hv = am.am_id ^ w.id.hash_id;
        ListHead<RightMemory> header = right_ht.right_ht_bucket(hv);
        rm.in_bucket.remove(header);
        rm.in_am.remove(am.right_mems);
        rm.from_wme.remove(w.right_mems);
    }

    /**
     * rete.cpp:1393:table_for_tests
     * 
     * @param id
     * @param attr
     * @param value
     * @param acceptable
     * @return
     */
    /*package*/ SoarHashTable<AlphaMemory> table_for_tests(Symbol id, Symbol attr, Symbol value, boolean acceptable)
    {
        int index = ((id != null) ? 1 : 0) + ((attr != null) ? 2 : 0) +
                                              ((value != null) ? 4 : 0) +
                                              ((acceptable) ? 8 : 0);
        return alpha_hash_tables.get(index);
    }
    
    /**
     * Looks for an existing alpha mem, returns it or NIL if not found
     * 
     * rete.cpp:1449:find_alpha_mem
     * 
     * @param id
     * @param attr
     * @param value
     * @param acceptable
     * @return
     */
    AlphaMemory find_alpha_mem(Symbol id, Symbol attr, Symbol value, boolean acceptable)
    {
        SoarHashTable<AlphaMemory> ht = table_for_tests(id, attr, value, acceptable);
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
     * Find and share existing alpha memory, or create new one.  Adjusts the 
     * reference count on the alpha memory accordingly.
     * 
     * rete.cpp:1467:find_or_make_alpha_mem
     * 
     * @param id
     * @param attr
     * @param value
     * @param acceptable
     * @return
     */
    AlphaMemory find_or_make_alpha_mem(Symbol id, Symbol attr, Symbol value, boolean acceptable)
    {

        /* --- look for an existing alpha mem --- */
        AlphaMemory am = find_alpha_mem(id, attr, value, acceptable);
        if (am != null)
        {
            // TODO: am->reference_count++;
            return am;
        }

        /* --- no existing alpha_mem found, so create a new one --- */
        am = new AlphaMemory(get_next_alpha_mem_id(), id, attr, value, acceptable);
        SoarHashTable<AlphaMemory> ht = table_for_tests(id, attr, value, acceptable);
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
            for (AsListItem<RightMemory> rm = more_general_am.right_mems.first; rm != null; rm = rm.next)
            {
                if (am.wme_matches_alpha_mem(rm.get().w))
                {
                    add_wme_to_alpha_mem(rm.get().w, am);
                }
            }
        }
        else
        {
            /* --- couldn't find such an existing mem, so do it the hard way --- */
            for (AsListItem<Wme> w = all_wmes_in_rete.first; w != null; w = w.next)
            {
                if (am.wme_matches_alpha_mem(w.get()))
                {
                    add_wme_to_alpha_mem(w.get(), am);
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
     * rete.cpp:1524:add_wme_to_aht
     * 
     * @param ht
     * @param hash_value
     * @param w
     */
    void add_wme_to_aht(SoarHashTable<AlphaMemory> ht, int hash_value, Wme w)
    {
        // TODO: Move this op into getBucket()
        hash_value = hash_value & SoarHashTable.masks_for_n_low_order_bits[ht.getLog2Size()];
        AlphaMemory am = ht.getBucket(hash_value);
        while (am != null)
        {
            if (am.wme_matches_alpha_mem(w))
            {
                /* --- found the right alpha memory, first add the wme --- */
                add_wme_to_alpha_mem(w, am);

                /* --- now call the beta nodes --- */
                ReteNode next = null;
                for (ReteNode node = am.beta_nodes; node != null; node = next)
                {
                    next = node.b_posneg.next_from_alpha_mem;
                    right_addition_routines[node.node_type.index()].execute(this, node, w);
                }
                return; /* only one possible alpha memory per table could match */
            }
            am = (AlphaMemory) am.next_in_hash_table;
        }
    }


    /**
     * rete.cpp:1698:get_next_beta_node_id
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
     * rete.cpp:1711:init_dummy_top_node
     */
    void init_dummy_top_node()
    {
        /* --- create the dummy top node --- */
        dummy_top_node = new ReteNode(ReteNodeType.DUMMY_TOP_BNODE);

        /* --- create the dummy top token --- */
        dummy_top_token = new RightToken(dummy_top_node, null, null, null);
        dummy_top_node.a_np.tokens.first = dummy_top_token.of_node;
    }
    

    /**
     * Calls a node's left-addition routine with each match (token) from 
     * the node's parent.  DO NOT call this routine on (positive, unmerged)
     * join nodes.
     * 
     * rete.cpp:1765:update_node_with_matches_from_above
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
          left_addition_routines[child.node_type.index()].execute(this, child, dummy_top_token, null);
          return;
        }

        /* --- if parent is positive: first do surgery on parent's child list,
               to replace the list with "child"; then call parent's add_right 
               routine with each wme in the parent's alpha mem; then do surgery 
               to restore previous child list of parent. --- */
        if (parent.node_type.bnode_is_positive()) {
          /* --- If the node is right unlinked, then don't activate it.  This is
             important because some interpreter routines rely on the node
             being right linked whenever it gets right activated. */
          if (parent.node_is_right_unlinked ()) { return;}
          ReteNode saved_parents_first_child = parent.first_child;
          ReteNode saved_childs_next_sibling = child.next_sibling;
          parent.first_child = child;
          child.next_sibling = null;
          /* to avoid double-counting these right adds */
          for(RightMemory rm : parent.b_posneg.alpha_mem_.right_mems)
          {
              right_addition_routines[parent.node_type.index()].execute(this, parent, rm.w);
          }
          parent.first_child = saved_parents_first_child;
          child.next_sibling = saved_childs_next_sibling;
          return;
        }
          
        /* --- if parent is negative or cn: easy, just look at the list of tokens
               on the parent node. --- */
        //for (tok=parent->a.np.tokens; tok!=NIL; tok=tok->next_of_node)
        for(Token tok : parent.a_np.tokens)
        {
            if(tok.negrm_tokens.isEmpty())
            {
                left_addition_routines[child.node_type.index()].execute(this, child, tok, null);
            }
        }
    }

    /**
     * This routine finds the most recent place a variable was bound.
     * It does this simply by looking at the top of the binding stack
     * for that variable.  If there is any binding, its location is stored
     * in the parameter *result, and the function returns TRUE.  If no
     * binding is found, the function returns FALSE.
     * 
     * rete.cpp:2373:find_var_location
     * 
     * @param var
     * @param current_depth
     * @param result
     * @return
     */
    boolean find_var_location(Variable var, /* rete_node_level */int current_depth, VarLocation result)
    {
        if (!var.var_is_bound())
        {
            return false;
        }
        int dummy = var.rete_binding_locations.peek();
        result.levels_up = current_depth - Variable.dummy_to_varloc_depth(dummy);
        result.field_num = Variable.dummy_to_varloc_field_num(dummy);
        return true;
    }    

    /**
     * This routine pushes bindings for variables occurring (i.e., being
     * equality-tested) in a given test.  It can do this in DENSE fashion
     * (push a new binding for ANY variable) or SPARSE fashion (push a new
     * binding only for previously-unbound variables), depending on the
     * boolean "dense" parameter.  Any variables receiving new bindings
     * are also pushed onto the given "varlist".
     * 
     * rete.cpp:2394:bind_variables_in_test
     * 
     * @param t
     * @param depth
     * @param field_num
     * @param dense
     * @param varlist
     */
    static void bind_variables_in_test(Test t, int depth, int field_num, boolean dense, LinkedList<Variable> varlist)
    {

        if (t.isBlank())
        {
            return;
        }
        EqualityTest eq = t.asEqualityTest();
        if (eq != null)
        {
            Variable referent = eq.getReferent().asVariable();
            if (referent == null) // not a variable
            {
                return;
            }
            if (!dense && referent.var_is_bound())
            {
                return;
            }
            referent.push_var_binding(depth, field_num);
            varlist.push(referent); // push(thisAgent, referent, *varlist);
            return;
        }

        ConjunctiveTest ct = t.asConjunctiveTest();
        if (ct != null)
        {
            for (Test c : ct.conjunct_list)
            {
                bind_variables_in_test(c, depth, field_num, dense, varlist);
            }
        }
    }

    /**
     * This routine takes a list of variables; for each item <v> on the
     * list, it pops a binding of <v>.  It also deallocates the list.
     * This is often used for un-binding a group of variables which got
     * bound in some procedure.
     * 
     * rete.cpp:2430:pop_bindings_and_deallocate_list_of_variables
     * 
     * @param vars
     */
    static void pop_bindings_and_deallocate_list_of_variables(List<Variable> vars)
    {
        for (Variable v : vars)
        {
            v.pop_var_binding();
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
     * rete.cpp:3480:update_max_rhs_unbound_variables
     * 
     * @param num_for_new_production
     */
    void update_max_rhs_unbound_variables(int num_for_new_production)
    {
        if (num_for_new_production > rhs_variable_bindings.length)
        {
            rhs_variable_bindings = new Symbol[num_for_new_production]; // defaults to null.
        }
    }

    /**
     * This routine destructively modifies a given test, adding to it a test
     * for equality with a new gensym variable.
     * 
     * rete.cpp:3821:add_gensymmed_equality_test
     * 
     * @param t
     * @param first_letter
     */
    private void add_gensymmed_equality_test(Test t, char first_letter)
    {
        Variable New = variableGenerator.generate_new_variable(Character.toString(first_letter));
        Test eq_test = new EqualityTest(New);
        // symbol_remove_ref (thisAgent, New);
        TestTools.add_new_test_to_test(ByRef.create(t), eq_test);
    }

    /**
     * We're reconstructing the conditions for a production in top-down
     * fashion.  Suppose we come to a Rete test checking for equality with 
     * the "value" field 3 levels up.  In that case, for the current condition,
     * we want to include an equality test for whatever variable got bound
     * in the value field 3 levels up.  This function scans up the list
     * of conditions reconstructed so far, and finds the appropriate variable.
     * 
     * rete.cpp:3845:var_bound_in_reconstructed_conds
     * 
     * @param cond
     * @param where_field_num
     * @param where_levels_up
     * @return
     */
    private Symbol var_bound_in_reconstructed_conds(Condition cond, int where_field_num, int where_levels_up)
    {
        while (where_levels_up != 0)
        {
            where_levels_up--;
            cond = cond.prev;
        }

        ThreeFieldCondition tfc = cond.asThreeFieldCondition();
        if(tfc ==  null)
        {
            throw new IllegalStateException("Expected ThreeFieldCondition, got " + cond);
        }
        Test t = null;
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

        if (t.isBlank())
        {
            // goto abort_var_bound_in_reconstructed_conds;
            throw new IllegalStateException("Internal error in var_bound_in_reconstructed_conds");
        }
        EqualityTest eq = t.asEqualityTest();
        if (eq != null)
        {
            return eq.getReferent();
        }

        ConjunctiveTest ct = t.asConjunctiveTest();
        if (ct != null)
        {
            for (Test c : ct.conjunct_list)
            {
                EqualityTest eq2 = c.asEqualityTest();
                if (c.isBlank() && eq2 != null)
                {
                    return eq2.getReferent();
                }
            }
        }

        throw new IllegalStateException("Internal error in var_bound_in_reconstructed_conds");
    }

    /**
     * rete.cpp:4391:get_symbol_from_rete_loc
     * 
     * @param levels_up
     * @param field_num
     * @param tok
     * @param w
     * @return
     */
    private static Symbol get_symbol_from_rete_loc(int levels_up, int field_num, Token tok, Wme w)
    {
        while (levels_up != 0)
        {
            levels_up--;
            w = tok.w;
            tok = tok.parent;
        }
        if (field_num == 0)
            return w.id;
        if (field_num == 1)
            return w.attr;
        return w.value;
    }
    
    /**
     * rete.cpp:4441:match_left_and_right
     * 
     * @param _rete_test
     * @param left
     * @param w
     * @return
     */
    private boolean match_left_and_right(ReteTest _rete_test, LeftToken left, Wme w)
    {
        return rete_test_routines[(_rete_test).type].execute(this, _rete_test, left, w);
    }
    
    /**
     * rete.cpp:4719:beta_memory_node_left_addition
     * 
     * @param node
     * @param tok
     * @param w
     */
    private void beta_memory_node_left_addition(ReteNode node, Token tok, Wme w)
    {
        left_node_activation(node, true);

        Symbol referent = null;
        {
            int levels_up = node.left_hash_loc_levels_up;
            if (levels_up == 1)
            {
                referent = VarLocation.field_from_wme(w, node.left_hash_loc_field_num);
            }
            else
            { /* --- levels_up > 1 --- */
                Token t = tok;
                for (t = tok, levels_up -= 2; levels_up != 0; levels_up--)
                {
                    t = t.parent;
                }
                referent = VarLocation.field_from_wme(t.w, node.left_hash_loc_field_num);
            }
        }

        int hv = node.node_id ^ referent.hash_id;

        /* --- build new left token, add it to the hash table --- */
        LeftToken New = new LeftToken(node, tok, w, referent);
        left_ht.insert_token_into_left_ht(New, hv);

        /* --- inform each linked child (positive join) node --- */
        ReteNode next = null;
        for (ReteNode child = node.b_mem.first_linked_child; child != null; child = next)
        {
            next = child.a_pos.next_from_beta_mem;
            positive_node_left_addition(child, New, referent);
        }
    }
    

    /**
     * rete.cpp:4759:unhashed_beta_memory_node_left_addition
     * 
     * @param node
     * @param tok
     * @param w
     */
    private void unhashed_beta_memory_node_left_addition(ReteNode node, Token tok, Wme w)
    {
        left_node_activation(node, true);

        int hv = node.node_id;

        /* --- build new left token, add it to the hash table --- */
        LeftToken New = new LeftToken(node, tok, w, null);
        left_ht.insert_token_into_left_ht(New, hv);

        /* --- inform each linked child (positive join) node --- */
        ReteNode next = null;
        for (ReteNode child = node.b_mem.first_linked_child; child != null; child = next)
        {
            next = child.a_pos.next_from_beta_mem;
            unhashed_positive_node_left_addition(child, New);
        }
    }    

    /**
     * rete.cpp:4786:positive_node_left_addition
     * 
     * @param node
     * @param New
     * @param hash_referent
     */
    private void positive_node_left_addition(ReteNode node, LeftToken New, Symbol hash_referent)
    {
        left_node_activation(node, true);

        AlphaMemory am = node.b_posneg.alpha_mem_;

        if (node.node_is_right_unlinked())
        {
            node.relink_to_right_mem();
            if (am.right_mems.isEmpty())
            {
                node.unlink_from_left_mem();
                return;
            }
        }

        /* --- look through right memory for matches --- */
        int right_hv = am.am_id ^ hash_referent.hash_id;
        for (RightMemory rm : right_ht.right_ht_bucket(right_hv))
        {
            if (rm.am != am)
                continue;
            /* --- does rm->w match New? --- */
            if (hash_referent != rm.w.id)
            {
                continue;
            }
            boolean failed_a_test = false;
            for (ReteTest rt = node.b_posneg.other_tests; rt != null; rt = rt.next) {
                if (!match_left_and_right(rt, New, rm.w))
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
                left_addition_routines[child.node_type.index()].execute(this, child, New, rm.w);
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
        left_node_activation(node, true);

        if (node.node_is_right_unlinked())
        {
            node.relink_to_right_mem();
            if (node.b_posneg.alpha_mem_.right_mems.isEmpty())
            {
                node.unlink_from_left_mem();
                return;
            }
        }

        /* --- look through right memory for matches --- */
        for (RightMemory rm : node.b_posneg.alpha_mem_.right_mems)
        {
            /* --- does rm->w match new? --- */
            boolean failed_a_test = false;
            for (ReteTest rt = node.b_posneg.other_tests; rt != null; rt = rt.next)
            {
                if (!match_left_and_right(rt, New, rm.w))
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
                left_addition_routines[child.node_type.index()].execute(this, child, New, rm.w);
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
    private void mp_node_left_addition(ReteNode node, Token tok, Wme w)
    {
        left_node_activation(node, true);

        Symbol referent = null;
        {
            int levels_up = node.left_hash_loc_levels_up;
            if (levels_up == 1)
            {
                referent = VarLocation.field_from_wme(w, node.left_hash_loc_field_num);
            }
            else
            { /* --- levels_up > 1 --- */
                Token t = tok;
                for (t = tok, levels_up -= 2; levels_up != 0; levels_up--)
                {
                    t = t.parent;
                }
                referent = VarLocation.field_from_wme(t.w, node.left_hash_loc_field_num);
            }
        }

        int hv = node.node_id ^ referent.hash_id;

        /* --- build new left token, add it to the hash table --- */
        LeftToken New = new LeftToken(node, tok, w, referent);
        left_ht.insert_token_into_left_ht(New, hv);

        if (node.mp_bnode_is_left_unlinked())
        {
            return;
        }

        AlphaMemory am = node.b_posneg.alpha_mem_;

        if (node.node_is_right_unlinked())
        {
            node.relink_to_right_mem();
            if (am.right_mems.isEmpty())
            {
                node.make_mp_bnode_left_unlinked();
                return;
            }
        }

        /* --- look through right memory for matches --- */
        int right_hv = am.am_id ^ referent.hash_id;
        for (RightMemory rm : right_ht.right_ht_bucket(right_hv))
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
            for (ReteTest rt = node.b_posneg.other_tests; rt != null; rt = rt.next)
                if (!match_left_and_right(rt, New, rm.w))
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
                left_addition_routines[child.node_type.index()].execute(this, child, New, rm.w);
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
    private void unhashed_mp_node_left_addition(ReteNode node, Token tok, Wme w)
    {
        left_node_activation(node, true);

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
            if (node.b_posneg.alpha_mem_.right_mems.isEmpty())
            {
                node.make_mp_bnode_left_unlinked();
                return;
            }
        }

        /* --- look through right memory for matches --- */
        for (RightMemory rm : node.b_posneg.alpha_mem_.right_mems)
        {
            /* --- does rm->w match new? --- */
            boolean failed_a_test = false;
            for (ReteTest rt = node.b_posneg.other_tests; rt != null; rt = rt.next)
            {
                if (!match_left_and_right(rt, New, rm.w))
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
                left_addition_routines[child.node_type.index()].execute(this, child, New, rm.w);
            }
        }
    }

    /**
     * rete.cpp:4989:positive_node_right_addition
     * 
     * @param node
     * @param w
     */
    void positive_node_right_addition(ReteNode node, Wme w)
    {
        right_node_activation(node, true);

        if (node.node_is_left_unlinked())
        {
            node.relink_to_left_mem();
            if (node.parent.a_np.tokens.isEmpty())
            {
                node.unlink_from_right_mem();
                return;
            }
        }

        Symbol referent = w.id;
        int hv = node.parent.node_id ^ referent.hash_id;

        for (LeftToken tok : left_ht.left_ht_bucket(hv))
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
            for (ReteTest rt = node.b_posneg.other_tests; rt != null; rt = rt.next)
            {
                if (!match_left_and_right(rt, tok, w))
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
                left_addition_routines[child.node_type.index()].execute(this, child, tok, w);
            }
        }
    }

    /**
     * rete.cpp:5030:unhashed_positive_node_right_addition
     * 
     * @param node
     * @param w
     */
    void unhashed_positive_node_right_addition(ReteNode node, Wme w)
    {
        right_node_activation(node, true);

        if (node.node_is_left_unlinked())
        {
            node.relink_to_left_mem();
            if (node.parent.a_np.tokens.isEmpty())
            {
                node.unlink_from_right_mem();
                return;
            }
        }

        int hv = node.parent.node_id;

        for (LeftToken tok : left_ht.left_ht_bucket(hv))
        {
            if (tok.node != node.parent)
            {
                continue;
            }
            /* --- does tok match w? --- */
            boolean failed_a_test = false;
            for (ReteTest rt = node.b_posneg.other_tests; rt != null; rt = rt.next)
            {
                if (!match_left_and_right(rt, tok, w))
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
                left_addition_routines[child.node_type.index()].execute(this, child, tok, w);
            }
        }
    }

    /**
     * rete.cpp:5068:mp_node_right_addition
     * 
     * @param node
     * @param w
     */
    void mp_node_right_addition(ReteNode node, Wme w)
    {
        right_node_activation(node, true);

        if (node.mp_bnode_is_left_unlinked())
        {
            node.make_mp_bnode_left_linked();
            if (node.a_np.tokens.isEmpty())
            {
                node.unlink_from_right_mem();
                return;
            }
        }

        Symbol referent = w.id;
        int hv = node.node_id ^ referent.hash_id;

        for (LeftToken tok : left_ht.left_ht_bucket(hv))
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
            for (ReteTest rt = node.b_posneg.other_tests; rt != null; rt = rt.next)
            {
                if (!match_left_and_right(rt, tok, w))
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
                left_addition_routines[child.node_type.index()].execute(this, child, tok, w);
            }
        }
    }
    

    /**
     * rete.cpp:5109:unhashed_mp_node_right_addition
     * @param node
     * @param w
     */
    void unhashed_mp_node_right_addition(ReteNode node, Wme w)
    {
        right_node_activation(node, true);

        if (node.mp_bnode_is_left_unlinked())
        {
            node.make_mp_bnode_left_linked();
            if (node.a_np.tokens.isEmpty())
            {
                node.unlink_from_right_mem();
                return;
            }
        }

        int hv = node.node_id;

        for (LeftToken tok : left_ht.left_ht_bucket(hv))
        {
            if (tok.node != node)
            {
                continue;
            }
            /* --- does tok match w? --- */
            boolean failed_a_test = false;
            for (ReteTest rt = node.b_posneg.other_tests; rt != null; rt = rt.next)
            {
                if (!match_left_and_right(rt, tok, w))
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
                left_addition_routines[child.node_type.index()].execute(this, child, tok, w);
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
    void negative_node_left_addition(ReteNode node, Token tok, Wme w)
    {
        left_node_activation(node, true);

        if (node.node_is_right_unlinked())
        {
            node.relink_to_right_mem();
        }

        Symbol referent = null;
        {
            int levels_up = node.left_hash_loc_levels_up;
            if (levels_up == 1)
            {
                referent = VarLocation.field_from_wme(w, node.left_hash_loc_field_num);
            }
            else
            { /* --- levels_up > 1 --- */
                Token t = tok;
                for (levels_up -= 2; levels_up != 0; levels_up--)
                {
                    t = t.parent;
                }
                referent = VarLocation.field_from_wme(t.w, node.left_hash_loc_field_num);
            }
        }

        int hv = node.node_id ^ referent.hash_id;

        /* --- build new token, add it to the hash table --- */
        LeftToken New = new LeftToken(node, tok, w, referent);
        left_ht.insert_token_into_left_ht(New, hv);

        /* --- look through right memory for matches --- */
        AlphaMemory am = node.b_posneg.alpha_mem_;
        int right_hv = am.am_id ^ referent.hash_id;
        for (RightMemory rm : right_ht.right_ht_bucket(right_hv))
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
            for (ReteTest rt = node.b_posneg.other_tests; rt != null; rt = rt.next)
            {
                if (!match_left_and_right(rt, New, rm.w))
                {
                    failed_a_test = true;
                    break;
                }
            }
            if (failed_a_test)
            {
                continue;
            }
            new RightToken(node, null, rm.w, New);
        }

        /* --- if no matches were found, call each child node --- */
        if (New.negrm_tokens.isEmpty())
        {
            for (ReteNode child = node.first_child; child != null; child = child.next_sibling)
            {
                left_addition_routines[child.node_type.index()].execute(this, child, New, null);
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
    void unhashed_negative_node_left_addition(ReteNode node, Token tok, Wme w)
    {
        left_node_activation(node, true);

        if (node.node_is_right_unlinked())
        {
            node.relink_to_right_mem();
        }

        int hv = node.node_id;

        /* --- build new token, add it to the hash table --- */
        LeftToken New = new LeftToken(node, tok, w, null);
        left_ht.insert_token_into_left_ht(New, hv);

        /* --- look through right memory for matches --- */
        for (RightMemory rm : node.b_posneg.alpha_mem_.right_mems)
        {
            /* --- does rm->w match new? --- */
            boolean failed_a_test = false;
            for (ReteTest rt = node.b_posneg.other_tests; rt != null; rt = rt.next)
            {
                if (!match_left_and_right(rt, New, rm.w))
                {
                    failed_a_test = true;
                    break;
                }
            }
            if (failed_a_test)
            {
                continue;
            }
            new RightToken(node, null, rm.w, New);
        }

        /* --- if no matches were found, call each child node --- */
        if (New.negrm_tokens.isEmpty())
        {
            for (ReteNode child = node.first_child; child != null; child = child.next_sibling)
            {
                left_addition_routines[child.node_type.index()].execute(this, child, New, null);
            }
        }
    }
    

    /**
     * rete.cpp:5282:negative_node_right_addition
     * 
     * @param node
     * @param w
     */
    void negative_node_right_addition(ReteNode node, Wme w)
    {
        right_node_activation(node, true);

        Symbol referent = w.id;
        int hv = node.node_id ^ referent.hash_id;

        for (LeftToken tok : left_ht.left_ht_bucket(hv))
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
            for (ReteTest rt = node.b_posneg.other_tests; rt != null; rt = rt.next)
            {
                if (!match_left_and_right(rt, tok, w))
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
            new RightToken(node, null, w, tok);

            while (!tok.first_child.isEmpty())
            {
                remove_token_and_subtree(tok.first_child.first.get());
            }
        }
    }

    /**
     * rete.cpp:5323:unhashed_negative_node_right_addition
     * 
     * @param node
     * @param w
     */
    void unhashed_negative_node_right_addition(ReteNode node, Wme w)
    {
        right_node_activation(node, true);

        int hv = node.node_id;

        for (LeftToken tok : left_ht.left_ht_bucket(hv))
        {
            if (tok.node != node)
            {
                continue;
            }
            /* --- does tok match w? --- */
            boolean failed_a_test = false;
            for (ReteTest rt = node.b_posneg.other_tests; rt != null; rt = rt.next)
                if (!match_left_and_right(rt, tok, w))
                {
                    failed_a_test = true;
                    break;
                }
            if (failed_a_test)
            {
                continue;
            }
            /* --- match found: build new negrm token, remove descendent tokens --- */
            new RightToken(node, null, w, tok);
            while (!tok.first_child.isEmpty())
            {
                remove_token_and_subtree(tok.first_child.first.get());
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
    void cn_node_left_addition(ReteNode node, Token tok, Wme w)
    {
        left_node_activation(node, true);

        // TODO: Is it ok to use hashcode in place of hashing on the address?
        int hv = node.node_id ^ tok.hashCode() ^ w.hashCode();

        /*
         * --- look for a matching left token (since the partner node might have
         * heard about this new token already, in which case it would have done
         * the CN node's work already); if found, exit ---
         */
        for (LeftToken t : left_ht.left_ht_bucket(hv))
        {
            if ((t.node == node) && (t.parent == tok) && (t.w == w))
            {
                return;
            }
        }

        /* --- build left token, add it to the hash table --- */
        LeftToken New = new LeftToken(node, tok, w, null);
        left_ht.insert_token_into_left_ht(New, hv);

        /* --- pass the new token on to each child node --- */
        for (ReteNode child = node.first_child; child != null; child = child.next_sibling)
        {
            left_addition_routines[child.node_type.index()].execute(this, child, New, null);
        }
    }

    /**
     * rete.cpp:5400:cn_partner_node_left_addition
     * 
     * @param node
     * @param tok
     * @param w
     */
    void cn_partner_node_left_addition(ReteNode node, Token tok, Wme w)
    {
        left_node_activation(node, true);

        ReteNode partner = node.b_cn.partner;

        /* --- build new negrm token --- */
        // TODO: Can this be created at "negrm_tok.left_token = left;" below so
        // that
        // left_toke can be final and list insertion can happen in constructor?
        RightToken negrm_tok = new RightToken(node, tok, w, null);

        /* --- advance (tok,w) up to the token from the top of the branch --- */
        ReteNode temp = node.parent;
        while (temp != partner.parent)
        {
            temp = temp.real_parent_node();
            w = tok.w;
            tok = tok.parent;
        }

        /* --- look for the matching left token --- */
        // TODO: Is it ok to use hashcode in place of hashing on the address?
        int hv = partner.node_id ^ tok.hashCode() ^ w.hashCode();
        LeftToken left = null;
        for (LeftToken tempLeft : left_ht.left_ht_bucket(hv))
        {
            if ((tempLeft.node == partner) && (tempLeft.parent == tok) && (tempLeft.w == w))
            {
                left = tempLeft;
                break;
            }
        }

        /* --- if not found, create a new left token --- */
        if (left == null)
        {
            left = new LeftToken(partner, tok, w, null);
            left_ht.insert_token_into_left_ht(left, hv);
        }

        /* --- add new negrm token to the left token --- */
        negrm_tok.left_token = left;
        negrm_tok.negrm.insertAtHead(left.negrm_tokens);

        /* --- remove any descendent tokens of the left token --- */
        while (!left.first_child.isEmpty())
        {
            remove_token_and_subtree(left.first_child.first.get());
        }
    }
    
    /**
     * Algorithm:
     * 
     * Does this token match (wme's equal) one of tentative_retractions?
     *   (We have to check instantiation structure for this--when an
     *   instantiation retracts then re-asserts in one e-cycle, the
     *   token itself will be different, but all the wme's tested positively
     *   will be the same.)
     * If so, remove that tentative_retraction.
     * If not, store this new token in tentative_assertions.
     * 
     * rete.cpp:5481:p_node_left_addition
     * 
     * @param node
     * @param tok
     * @param w
     */
    void p_node_left_addition(ReteNode node, Token tok, Wme w)
    {
        left_node_activation(node, true);

        /* --- build new left token (used only for tree-based remove) --- */
        LeftToken New = new LeftToken(node, tok, w, null);

        /* --- check for match in tentative_retractions --- */
        boolean match_found = false;
        MatchSetChange msc = null;
        for (MatchSetChange mscTemp : node.b_p.tentative_retractions)
        {
            msc = mscTemp;
            match_found = true;
            Condition cond = msc.inst.bottom_of_instantiated_conditions;
            Token current_token = tok;
            Wme current_wme = w;
            ReteNode current_node = node.parent;
            while (current_node.node_type != ReteNodeType.DUMMY_TOP_BNODE)
            {
                if (current_node.node_type.bnode_is_positive())
                {
                    if (current_wme != cond.bt.wme_)
                    {
                        match_found = false;
                        break;
                    }
                }
                current_node = current_node.real_parent_node();
                current_wme = current_token.w;
                current_token = current_token.parent;
                cond = cond.prev;
            }
            if (match_found)
            {
                break;
            }
        }

        // TODO: Is BUG_139_WORKAROUND needed?
        // #ifdef BUG_139_WORKAROUND
        /*
         * --- test workaround for bug #139: don't rematch justifications; let
         * them be removed ---
         */
        /*
         * note that the justification is added to the retraction list when it
         * is first created, so we let it match the first time, but not after
         * that
         */
        if (match_found && node.b_p.prod.type == ProductionType.JUSTIFICATION_PRODUCTION_TYPE)
        {
            if (node.b_p.prod.already_fired)
            {
                return;
            }
            else
            {
                node.b_p.prod.already_fired = true;
            }
        }
        // #endif

        /* --- if match found tentative_retractions, remove it --- */
        if (match_found)
        {
            msc.inst.rete_token = tok;
            msc.inst.rete_wme = w;
            msc.of_node.remove(node.b_p.tentative_retractions);
            msc.next_prev.remove(ms_retractions);
            /* REW: begin 08.20.97 */
            if (msc.goal != null)
            {
                msc.in_level.remove(msc.goal.ms_retractions);
            }
            else
            {
                // BUGBUG FIXME BADBAD TODO
                // RPM 6/05
                // This if statement is to avoid a crash we get on most
                // platforms in Soar 7 mode
                // It's unknown what consequences it has, but the Soar 7 demos
                // seem to work
                // To return things to how they were, simply remove the if
                // statement (but leave
                // the remove_from_dll line).
                if (!nil_goal_retractions.isEmpty())
                {
                    msc.in_level.remove(nil_goal_retractions);
                }
            }
            /* REW: end 08.20.97 */

            // #ifdef DEBUG_RETE_PNODES
            // print_with_symbols (thisAgent, "\nRemoving tentative retraction:
            // %y",
            // node->b.p.prod->name);
            // #endif
            return;
        }

        /* --- no match found, so add new assertion --- */
        // #ifdef DEBUG_RETE_PNODES
        // print_with_symbols (thisAgent, "\nAdding tentative assertion: %y",
        // node->b.p.prod->name);
        // #endif
        msc = new MatchSetChange();
        msc.tok = tok;
        msc.w = w;
        msc.p_node = node;

        /* RCHONG: begin 10.11 */

        /*
         * (this is a RCHONG comment, but might also apply to Operand2...?)
         * 
         * what we have to do now is to, essentially, determine the kind of
         * support this production would get based on its present complete
         * matches. once i know the support, i can then know into which match
         * set list to put "msc".
         * 
         * this code is used to make separate PE productions from IE productions
         * by putting them into different match set lists. in non-OPERAND, these
         * matches would all go into one list.
         * 
         * BUGBUG i haven't tested this with a production that has more than one
         * match where the matches could have different support. is that even
         * possible???
         * 
         */

        /* operand code removed 1/22/99 - kjc */

        /* REW: begin 09.15.96 */
        if (operand2_mode)
        {

            /* REW: begin 08.20.97 */
            /* Find the goal and level for this ms change */
            msc.goal = find_goal_for_match_set_change_assertion(msc);
            msc.level = msc.goal.level;
            /* REW: end 08.20.97 */

            SavedFiringType prod_type = SavedFiringType.IE_PRODS;

            if (node.b_p.prod.declared_support == ProductionSupport.DECLARED_O_SUPPORT)
            {
                prod_type = SavedFiringType.PE_PRODS;
            }
            else if (node.b_p.prod.declared_support == ProductionSupport.DECLARED_I_SUPPORT)
            {
                prod_type = SavedFiringType.IE_PRODS;
            }
            else if (node.b_p.prod.declared_support == ProductionSupport.UNDECLARED_SUPPORT)
            {

                /*
                 * check if the instantiation is proposing an operator. if it
                 * is, then this instantiation is i-supported.
                 */

                boolean operator_proposal = false;

                for (Action act = node.b_p.prod.action_list; act != null; act = act.next)
                {
                    MakeAction ma = act.asMakeAction();
                    if (ma != null && (ma.attr.asSymbolValue() != null))
                    {
                        if ("operator".equals(ma.attr.asSymbolValue().toString())
                                && (act.preference_type == PreferenceType.ACCEPTABLE_PREFERENCE_TYPE))
                        {
                            operator_proposal = true;
                            prod_type = SavedFiringType.IE_PRODS; // TODO ???
                                                                    // !PE_PRODS;
                                                                    // ???
                            break;
                        }
                    }
                }

                if (!operator_proposal)
                {

                    // examine all the different matches for this productions

                    for (Token OPERAND_curr_tok : node.a_np.tokens)
                    {

                        /*
                         * 
                         * i'll need to make two passes over each set of wmes
                         * that match this production. the first pass looks for
                         * the lowest goal identifier. the second pass looks for
                         * a wme of the form:
                         *  (<lowest-goal-id> ^operator ...)
                         * 
                         * if such a wme is found, then this production is a
                         * PE_PROD. otherwise, it's a IE_PROD.
                         * 
                         * admittedly, this implementation is kinda sloppy. i
                         * need to clean it up some.
                         * 
                         * BUGBUG this check only looks at positive conditions.
                         * we haven't really decided what testing the absence of
                         * the operator will do. this code assumes that such a
                         * productions (instantiation) would get i-support.
                         * 
                         * Modified 1/00 by KJC for operand2_mode == TRUE AND
                         * o-support-mode == 3: prods that have ONLY operator
                         * elaborations (<o> ^attr ^value) are IE_PROD. If prod
                         * has both operator applications and <o> elabs, then
                         * it's PE_PROD and the user is warned that <o> elabs
                         * will be o-supported.
                         * 
                         */
                        boolean op_elab = false;
                        Wme lowest_goal_wme = null;

                        for (int pass = 0; pass != 2; pass++)
                        {

                            Token temp_tok = OPERAND_curr_tok;
                            while (temp_tok != null)
                            {
                                while (temp_tok.w == null)
                                {
                                    temp_tok = temp_tok.parent;
                                    if (temp_tok == null)
                                    {
                                        break;
                                    }
                                }
                                if (temp_tok == null)
                                {
                                    break;
                                }
                                if (temp_tok.w == null)
                                {
                                    break;
                                }

                                if (pass == 0)
                                {
                                    if (temp_tok.w.id.isa_goal)
                                    {
                                        if (lowest_goal_wme == null)
                                        {
                                            lowest_goal_wme = temp_tok.w;
                                        }
                                        else
                                        {
                                            if (temp_tok.w.id.level > lowest_goal_wme.id.level)
                                            {
                                                lowest_goal_wme = temp_tok.w;
                                            }
                                        }
                                    }
                                }
                                else
                                {
                                    if ((temp_tok.w.attr == operator_symbol) && (temp_tok.w.acceptable == false)
                                            && (temp_tok.w.id == lowest_goal_wme.id))
                                    {
                                        if ((o_support_calculation_type == 3) || (o_support_calculation_type == 4))
                                        {
                                            /*
                                             * iff RHS has only operator
                                             * elaborations then it's IE_PROD,
                                             * otherwise PE_PROD, so look for
                                             * non-op-elabs in the actions KJC
                                             * 1/00
                                             */

                                            /*
                                             * We also need to check reteloc's
                                             * to see if they are referring to
                                             * operator augmentations before
                                             * determining if this is an
                                             * operator elaboration
                                             */

                                            for (Action act = node.b_p.prod.action_list; act != null; act = act.next)
                                            {
                                                MakeAction ma = act.asMakeAction();
                                                if (ma != null)
                                                {
                                                    RhsSymbolValue rhsSym = ma.id.asSymbolValue();
                                                    ReteLocation rl = ma.id.asReteLocation();
                                                    if ((rhsSym != null) &&

                                                    /***************************
                                                     * shouldn't this be either
                                                     * symbol_to_rhs_value
                                                     * (act->id) == or act->id ==
                                                     * rhs_value_to_symbol(temp..)
                                                     **************************/
                                                    (rhsSym.sym == temp_tok.w.value))
                                                    {
                                                        op_elab = true;
                                                    }
                                                    else if ((o_support_calculation_type == 4)
                                                            && (rl != null)
                                                            && (temp_tok.w.value == get_symbol_from_rete_loc(rl
                                                                    .getLevelsUp(), rl.getFieldNum(), tok, w)))
                                                    {
                                                        op_elab = true;
                                                    }
                                                    else
                                                    {
                                                        /*
                                                         * this is not an
                                                         * operator elaboration
                                                         */
                                                        prod_type = SavedFiringType.PE_PRODS;
                                                    }
                                                } // act->type == MAKE_ACTION
                                            } // for
                                        }
                                        else
                                        {
                                            prod_type = SavedFiringType.PE_PRODS;
                                            break;
                                        }
                                    }
                                } /* end if (pass == 0) ... */
                                temp_tok = temp_tok.parent;
                            } /* end while (temp_tok != NIL) ... */

                            if (prod_type == SavedFiringType.PE_PRODS)
                                if ((o_support_calculation_type != 3) && (o_support_calculation_type != 4))
                                {
                                    break;
                                }
                                else if (op_elab)
                                {

                                    /* warn user about mixed actions */

                                    if ((o_support_calculation_type == 3) /*
                                                                             * &&
                                                                             * thisAgent->sysparams[PRINT_WARNINGS_SYSPARAM]
                                                                             */)
                                    {
                                        // TODO: Warning
                                        // print_with_symbols(thisAgent,
                                        // "\nWARNING: operator elaborations
                                        // mixed with operator applications\nget
                                        // o_support in prod %y",
                                        // node->b.p.prod->name);
                                        //                                    
                                        // // XML generation
                                        // growable_string gs =
                                        // make_blank_growable_string(thisAgent);
                                        // add_to_growable_string(thisAgent,
                                        // &gs, "WARNING: operator elaborations
                                        // mixed with operator applications\nget
                                        // o_support in prod ");
                                        // add_to_growable_string(thisAgent,
                                        // &gs, symbol_to_string(thisAgent,
                                        // node->b.p.prod->name, true, 0, 0));
                                        // xml_generate_warning(thisAgent,
                                        // text_of_growable_string(gs));
                                        // free_growable_string(thisAgent, gs);

                                        prod_type = SavedFiringType.PE_PRODS;
                                        break;
                                    }
                                    else if ((o_support_calculation_type == 4) /*
                                                                                 * &&
                                                                                 * thisAgent->sysparams[PRINT_WARNINGS_SYSPARAM]
                                                                                 */)
                                    {
                                        // TODO: Warning
                                        // print_with_symbols(thisAgent,
                                        // "\nWARNING: operator elaborations
                                        // mixed with operator applications\nget
                                        // i_support in prod %y",
                                        // node->b.p.prod->name);
                                        //
                                        // // XML generation
                                        // growable_string gs =
                                        // make_blank_growable_string(thisAgent);
                                        // add_to_growable_string(thisAgent,
                                        // &gs, "WARNING: operator elaborations
                                        // mixed with operator applications\nget
                                        // i_support in prod ");
                                        //                                    add_to_growable_string(thisAgent, &gs, symbol_to_string(thisAgent, node->b.p.prod->name, true, 0, 0));
                                        //                                    xml_generate_warning(thisAgent, text_of_growable_string(gs));
                                        //                                    free_growable_string(thisAgent, gs);

                                        prod_type = SavedFiringType.IE_PRODS;
                                        break;
                                    }
                                }
                        } /* end for pass =  */
                    } /* end for loop checking all matches */

                    /* BUG:  IF you print lowest_goal_wme here, you don't get what
                    you'd expect.  Instead of the lowest goal WME, it looks like
                    you get the lowest goal WME in the first/highest assertion of
                    all the matches for this production.  So, if there is a single
                    match, you get the right number.  If there are multiple matches
                    for the same production, you get the lowest goal of the
                    highest match goal production (or maybe just the first to
                    fire?).  I don;t know for certain if this is the behavior
                    Ron C. wanted or if it's a bug --
                    i need to talk to him about it. */

                } /* end if (operator_proposal == FALSE) */

            } /* end UNDECLARED_SUPPORT */

            if (prod_type == SavedFiringType.PE_PRODS)
            {
                msc.next_prev.insertAtHead(ms_o_assertions);

                /* REW: begin 08.20.97 */
                msc.in_level.insertAtHead(msc.goal.ms_o_assertions);
                /* REW: end   08.20.97 */

                node.b_p.prod.OPERAND_which_assert_list = AssertListType.O_LIST;

                // TODO: verbose
                //        if (thisAgent->soar_verbose_flag == TRUE) {
                //           print_with_symbols(thisAgent, "\n   RETE: putting [%y] into ms_o_assertions",
                //                              node->b.p.prod->name);
                //           char buf[256];
                //           SNPRINTF(buf, 254, "RETE: putting [%s] into ms_o_assertions", symbol_to_string(thisAgent, node->b.p.prod->name, true, 0, 0));
                //           xml_generate_verbose(thisAgent, buf);
                //        }
            }

            else
            {
                msc.next_prev.insertAtHead(ms_i_assertions);

                /* REW: end 08.20.97 */
                msc.in_level.insertAtHead(msc.goal.ms_i_assertions);
                /* REW: end 08.20.97 */

                node.b_p.prod.OPERAND_which_assert_list = AssertListType.I_LIST;

                // TODO: Verbose
                //        if (thisAgent->soar_verbose_flag == TRUE) {
                //           print_with_symbols(thisAgent, "\n   RETE: putting [%y] into ms_i_assertions",
                //                              node->b.p.prod->name);
                //           char buf[256];
                //           SNPRINTF(buf, 254, "RETE: putting [%s] into ms_i_assertions", symbol_to_string(thisAgent, node->b.p.prod->name, true, 0, 0));
                //           xml_generate_verbose(thisAgent, buf);
                //        }
            }
        }
        /* REW: end   09.15.96 */

        else
        { /* non-Operand* flavor Soar */
            msc.next_prev.insertAtHead(ms_assertions);
        }
        ///
        // Location for Match Interrupt

        /* RCHONG: end 10.11 */

        msc.of_node.insertAtHead(node.b_p.tentative_assertions);
    }
    
    /**
     * Does this token match (eq) one of the tentative_assertions?
     * If so, just remove that tentative_assertion.
     * If not, find the instantiation corresponding to this token
     * and add it to tentative_retractions.
     * 
     * BUGBUG shouldn't need to pass in both tok and w -- should have the
     * p-node's token get passed in instead, and have it point to the
     * corresponding instantiation structure.
     * 
     * rete.cpp:5885:p_node_left_removal
     * 
     * @param node
     * @param tok
     * @param w
     */
    void p_node_left_removal(ReteNode node, Token tok, Wme w)
    {

        /* --- check for match in tentative_assertions --- */
        for (MatchSetChange msc : node.b_p.tentative_assertions)
        {
            if ((msc.tok == tok) && (msc.w == w))
            {
                /* --- match found in tentative_assertions, so remove it --- */
                msc.of_node.remove(node.b_p.tentative_assertions);

                /* REW: begin 09.15.96 */
                if (operand2_mode)
                {
                    if (node.b_p.prod.OPERAND_which_assert_list == AssertListType.O_LIST)
                    {
                        msc.next_prev.remove(ms_o_assertions);
                        /* REW: begin 08.20.97 */
                        /*
                         * msc already defined for the assertion so the goal
                         * should be defined as well.
                         */
                        msc.in_level.remove(msc.goal.ms_o_assertions);
                        /* REW: end 08.20.97 */
                    }
                    else if (node.b_p.prod.OPERAND_which_assert_list == AssertListType.I_LIST)
                    {
                        msc.next_prev.remove(ms_i_assertions);
                        /* REW: begin 08.20.97 */
                        msc.in_level.remove(msc.goal.ms_i_assertions);
                        /* REW: end 08.20.97 */
                    }
                }
                /* REW: end 09.15.96 */

                else
                {
                    msc.next_prev.remove(ms_assertions);
                }
                // #ifdef DEBUG_RETE_PNODES
                // print_with_symbols (thisAgent, "\nRemoving tentative
                // assertion: %y",
                // node->b.p.prod->name);
                // #endif
                return;
            }
        } /* end of for loop */

        /* --- find the instantiation corresponding to this token --- */
        Instantiation inst = null;
        for (Instantiation instTemp : node.b_p.prod.instantiations)
        {
            inst = instTemp;
            if ((inst.rete_token == tok) && (inst.rete_wme == w))
            {
                break;
            }
        }

        if (inst != null)
        {
            /* --- add that instantiation to tentative_retractions --- */
            // #ifdef DEBUG_RETE_PNODES
            // print_with_symbols (thisAgent, "\nAdding tentative retraction:
            // %y",
            // node->b.p.prod->name);
            // #endif
            inst.rete_token = null;
            inst.rete_wme = null;
            MatchSetChange msc = new MatchSetChange();
            msc.inst = inst;
            msc.p_node = node;
            msc.of_node.insertAtHead(node.b_p.tentative_retractions);

            /* REW: begin 08.20.97 */

            if (operand2_mode)
            {
                /*
                 * Determine what the goal of the msc is and add it to that
                 * goal's list of retractions
                 */
                msc.goal = find_goal_for_match_set_change_retraction(msc);
                msc.level = msc.goal.level;

                // #ifdef DEBUG_WATERFALL
                // print("\n Level of retraction is: %d", msc->level);
                // #endif

                if (msc.goal.link_count == 0)
                {
                    /*
                     * BUG (potential) (Operand2/Waterfall: 2.101) When a goal
                     * is removed in the stack, it is not immediately garbage
                     * collected, meaning that the goal pointer is still valid
                     * when the retraction is created. So the goal for a
                     * retraction will always be valid, even though, for
                     * retractions caused by goal removals, the goal will be
                     * removed at the next WM phase. (You can see this by
                     * printing the identifier for the goal in the elaboration
                     * cycle after goal removal. It's still there, although
                     * nothing is attacjed to it. One elab later, the identifier
                     * itself is removed.) Because Waterfall needs to know if
                     * the goal is valid or not, I look at the link_count on the
                     * symbol. A link_count of 0 is the trigger for the garbage
                     * collection so this solution should work -- I just make
                     * the pointer NIL to ensure that the retractions get added
                     * to the NIL_goal_retraction list. However, if the
                     * link_count is never not* zero for an already removed
                     * goal, this solution will fail, resulting in both the
                     * retraction never being able to fire and a memory leak
                     * (because the items on the ms_change list on the symbol
                     * will never be freed).
                     */
                    /*
                     * print("\nThis goal is being removed. Changing msc goal
                     * pointer to NIL.");
                     */
                    msc.goal = null;
                }

                /* Put on the original retraction list */
                msc.next_prev.insertAtHead(ms_retractions);
                if (msc.goal != null)
                { /* Goal exists */
                    msc.in_level.insertAtHead(msc.goal.ms_retractions);
                }
                else
                { /* NIL Goal; put on the NIL Goal list */
                    msc.in_level.insertAtHead(nil_goal_retractions);
                }

                // #ifdef DEBUG_WATERFALL
                // print_with_symbols(thisAgent, "\nRetraction: %y",
                // msc->inst->prod->name);
                // print(" is active at level %d\n", msc->level);
                //
                // { ms_change *assertion;
                // print("\n Retractions list:\n");
                // for (assertion=thisAgent->ms_retractions;
                // assertion;
                // assertion=assertion->next) {
                // print_with_symbols(thisAgent, " Retraction: %y ",
                // assertion->p_node->b.p.prod->name);
                // print(" at level %d\n", assertion->level);
                // }
                //
                // if (thisAgent->nil_goal_retractions) {
                // print("\nCurrent NIL Goal list:\n");
                // assertion = NIL;
                // for (assertion=thisAgent->nil_goal_retractions;
                // assertion;
                // assertion=assertion->next_in_level) {
                //            print_with_symbols(thisAgent, "     Retraction: %y ",
                //                               assertion->p_node->b.p.prod->name);
                //            print(" at level %d\n", assertion->level);
                //            if (assertion->goal) print("This assertion has non-NIL goal pointer.\n");
                //          } 
                //        }
                //        }
                //#endif 
                /* REW: end   08.20.97 */

            }
            else
            { /* For Reg. Soar just add it to the list */
                msc.next_prev.insertAtHead(ms_retractions);
            }
            return;
        }

        /* REW: begin 09.15.96 */

        // TODO: verbose
        //  if (operand2_mode &&
        //      (thisAgent->soar_verbose_flag == TRUE)) {
        //          print_with_symbols (thisAgent, "\n%y: ",node->b.p.prod->name);
        //          char buf[256];
        //          SNPRINTF(buf, 254, "%s: ", symbol_to_string(thisAgent, node->b.p.prod->name, true, 0, 0));
        //          xml_generate_verbose(thisAgent, buf);
        //      }
        /* REW: end   09.15.96 */
        //#ifdef BUG_139_WORKAROUND
        if (node.b_p.prod.type == ProductionType.JUSTIFICATION_PRODUCTION_TYPE)
        {
            // TODO Warning
            //#ifdef BUG_139_WORKAROUND_WARNING
            //        print(thisAgent, "\nWarning: can't find an existing inst to retract (BUG 139 WORKAROUND)\n");
            //        xml_generate_warning(thisAgent, "Warning: can't find an existing inst to retract (BUG 139 WORKAROUND)");
            //#endif
            return;
        }
        //#endif

        throw new IllegalStateException("Internal error: can't find existing instantiation to retract");
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
    void remove_token_and_subtree(Token root)
    {
        Token tok = root;
        
        while (true) {
          /* --- move down to the leftmost leaf --- */
          while (!tok.first_child.isEmpty()) { tok = tok.first_child.first.get(); }
          Token next_value_for_tok = tok.sibling.next != null ? tok.sibling.next.get() : tok.parent;

          /* --- cleanup stuff common to all types of nodes --- */
          ReteNode node = tok.node;
          left_node_activation(node,false);
          tok.of_node.remove(node.a_np.tokens);
//          fast_remove_from_dll (node->a.np.tokens, tok, token, next_of_node,
//                                prev_of_node);
          tok.sibling.remove(tok.parent.first_child);
//          fast_remove_from_dll (tok->parent->first_child, tok, token,
//                                next_sibling,prev_sibling);
          if (tok.w != null) { 
              tok.from_wme.remove(tok.w.tokens);
//              fast_remove_from_dll (tok->w->tokens, tok, token,
//                                       next_from_wme, prev_from_wme);
          }
          ReteNodeType node_type = node.node_type;

          /* --- for merged Mem/Pos nodes --- */
          if ((node_type==ReteNodeType.MP_BNODE)||(node_type==ReteNodeType.UNHASHED_MP_BNODE)) {
              LeftToken lt = (LeftToken) tok; // TODO: Assume this is safe?
              int hv = node.node_id ^ (lt.referent != null ? lt.referent.hash_id : 0);
              left_ht.remove_token_from_left_ht(lt, hv);
            if (! node.mp_bnode_is_left_unlinked()) {
              if (node.a_np.tokens.isEmpty()) { node.unlink_from_right_mem (); }
            }

          /* --- for P nodes --- */
          } else if (node_type==ReteNodeType.P_BNODE) {
            p_node_left_removal(node, tok.parent, tok.w);

          /* --- for Negative nodes --- */
          } else if ((node_type==ReteNodeType.NEGATIVE_BNODE) ||
                     (node_type==ReteNodeType.UNHASHED_NEGATIVE_BNODE)) {
            LeftToken lt = (LeftToken) tok; // TODO: Assume this is safe?
            int hv = node.node_id ^ (lt.referent != null ? lt.referent.hash_id : 0);
            left_ht.remove_token_from_left_ht(lt, hv);
            if (node.a_np.tokens.isEmpty()) { node.unlink_from_right_mem(); }
            for (Token t : tok.negrm_tokens) {
                t.from_wme.remove(t.w.tokens);
//              fast_remove_from_dll(t->w->tokens,t,token,next_from_wme,prev_from_wme);
            }

          /* --- for Memory nodes --- */
          } else if ((node_type==ReteNodeType.MEMORY_BNODE)||(node_type==ReteNodeType.UNHASHED_MEMORY_BNODE)) {
              LeftToken lt = (LeftToken) tok; // TODO: Assume this is safe?
              int hv = node.node_id ^ (lt.referent != null ? lt.referent.hash_id : 0);
              left_ht.remove_token_from_left_ht(lt, hv);
// TODO
//      #ifdef DO_ACTIVATION_STATS_ON_REMOVALS
//            /* --- if doing statistics stuff, then activate each attached node --- */
//            for (child=node->b.mem.first_linked_child; child!=NIL; child=next) {
//              next = child->a.pos.next_from_beta_mem;
//              left_node_activation (child,FALSE);
//            }
//      #endif
            /* --- for right unlinking, then if the beta memory just went to
               zero, right unlink any attached Pos nodes --- */
            if (node.a_np.tokens.isEmpty()) {
                ReteNode next = null;
              for (ReteNode child=node.b_mem.first_linked_child; child!=null; child=next) {
                next = child.a_pos.next_from_beta_mem;
                child.unlink_from_right_mem();
              }
            }

          /* --- for CN nodes --- */
          } else if (node_type==ReteNodeType.CN_BNODE) {
              // TODO: Is it ok to use hashcode in place of hashing on the adress?
              int hv = node.node_id ^ tok.parent.hashCode() ^ tok.w.hashCode();
            //int hv = node.node_id ^ (unsigned long)(tok->parent) ^ (unsigned long)(tok->w)
              left_ht.remove_token_from_left_ht((LeftToken) tok, hv); // TODO: Safe to assume this?  
            for(Token t : tok.negrm_tokens)
            {
                if(t.w != null)
                {
                    t.from_wme.remove(t.w.tokens);
                }
                t.of_node.remove(t.node.a_np.tokens);
                t.sibling.remove(t.parent.first_child);
            }

          /* --- for CN Partner nodes --- */
          } else if (node_type==ReteNodeType.CN_PARTNER_BNODE) {
            RightToken rt = (RightToken) tok; // TODO: Safe to assume this?
            Token left = rt.left_token;
            rt.negrm.remove(left.negrm_tokens);
//            fast_remove_from_dll (left->negrm_tokens, tok, token,
//                                  a.neg.next_negrm, a.neg.prev_negrm);
            if (left.negrm_tokens.isEmpty()) { /* just went to 0, so call children */
              for (ReteNode child=left.node.first_child; child!=null; child=child.next_sibling){
                left_addition_routines[child.node_type.index()].execute(this, child, left, null);
              }
            }

          } else {
              throw new IllegalArgumentException("Internal error: bad node type " + node.node_type + " in remove_token_and_subtree");
          }
          
          if (tok==root) break; /* if leftmost leaf was the root, we're done */
          tok = next_value_for_tok; /* else go get the leftmost leaf again */
        } 
        
    }
}
