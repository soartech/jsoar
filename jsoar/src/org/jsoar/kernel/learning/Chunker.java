/*
 * Copyright (c) 2008  Dave Ray <daveray@gmail.com>
 *
 * Created on Sep 12, 2008
 */
package org.jsoar.kernel.learning;

import java.util.LinkedList;

import org.jsoar.kernel.Agent;
import org.jsoar.kernel.lhs.Condition;
import org.jsoar.kernel.lhs.ConjunctiveNegationCondition;
import org.jsoar.kernel.lhs.ConjunctiveTest;
import org.jsoar.kernel.lhs.EqualityTest;
import org.jsoar.kernel.lhs.GoalIdTest;
import org.jsoar.kernel.lhs.ImpasseIdTest;
import org.jsoar.kernel.lhs.PositiveCondition;
import org.jsoar.kernel.lhs.RelationalTest;
import org.jsoar.kernel.lhs.Test;
import org.jsoar.kernel.lhs.TestTools;
import org.jsoar.kernel.lhs.ThreeFieldCondition;
import org.jsoar.kernel.memory.Preference;
import org.jsoar.kernel.memory.Slot;
import org.jsoar.kernel.memory.Wme;
import org.jsoar.kernel.rete.Instantiation;
import org.jsoar.kernel.rete.NotStruct;
import org.jsoar.kernel.rhs.MakeAction;
import org.jsoar.kernel.rhs.RhsSymbolValue;
import org.jsoar.kernel.symbols.Identifier;
import org.jsoar.kernel.symbols.Symbol;
import org.jsoar.kernel.symbols.Variable;
import org.jsoar.kernel.tracing.Trace.Category;
import org.jsoar.util.AsListItem;
import org.jsoar.util.ByRef;
import org.jsoar.util.ListHead;

/**
 * chunking.cpp
 * 
 * @author ray
 */
public class Chunker
{
    private final Agent context;
    public int chunks_this_d_cycle;
    private int results_match_goal_level;
    private int results_tc_number;
    private Preference results;
    private ListHead<Preference> extra_result_prefs_from_instantiation;
    private boolean variablize_this_chunk;
    private int variablization_tc;
    private final LinkedList<Condition> grounds = new LinkedList<Condition>();
    private final ChunkConditionSet negated_set = new ChunkConditionSet();
    
    /**
     * <p>gsysparam.h:179:CHUNK_THROUGH_LOCAL_NEGATIONS_SYSPARAM
     * <p>Defaults to true in init_soar()
     */
    private boolean chunkThroughLocalNegations = true;
    /**
     * <p>agent.h:534:quiescence_t_flag
     */
    private boolean quiescence_t_flag = false;

    /**
     * @param context
     */
    public Chunker(Agent context)
    {
        this.context = context;
    }

    /**
     * <p>chunk.cpp:77:add_results_if_needed
     * 
     * @param sym
     */
    private void add_results_if_needed(Symbol sym)
    {
        Identifier id = sym.asIdentifier();
        if (id != null)
            if ((id.level >= results_match_goal_level) && (id.tc_number != results_tc_number))
                add_results_for_id(id);
    }

    /**
     * chunk.cpp:86:add_pref_to_results
     * 
     * @param pref
     */
    private void add_pref_to_results(Preference pref)
    {
        // if an equivalent pref is already a result, don't add this one
        for (Preference p = this.results; p != null; p = p.next_result)
        {
            if (p.id != pref.id)
                continue;
            if (p.attr != pref.attr)
                continue;
            if (p.value != pref.value)
                continue;
            if (p.type != pref.type)
                continue;
            if (pref.type.isUnary())
                return;
            if (p.referent != pref.referent)
                continue;
            return;
        }

        // if pref isn't at the right level, find a clone that is
        if (pref.inst.match_goal_level != this.results_match_goal_level)
        {
            Preference p = null;
            for (p = pref.next_clone; p != null; p = p.next_clone)
                if (p.inst.match_goal_level == this.results_match_goal_level)
                    break;
            if (p == null)
                for (p = pref.prev_clone; p != null; p = p.prev_clone)
                    if (p.inst.match_goal_level == this.results_match_goal_level)
                        break;
            if (p == null)
                return; /* if can't find one, it isn't a result */
            pref = p;
        }

        // add this preference to the result list
        pref.next_result = this.results;
        this.results = pref;

        // follow transitive closuse through value, referent links
        add_results_if_needed(pref.value);
        if (pref.type.isBinary())
            add_results_if_needed(pref.referent);
    }

    /**
     * chunk.cpp:121:add_results_for_id
     * 
     * @param id
     */
    private void add_results_for_id(Identifier id)
    {
        id.tc_number = this.results_tc_number;

        // scan through all preferences and wmes for all slots for this id
        for (Wme w : id.input_wmes)
            add_results_if_needed(w.value);
        for (Slot s : id.slots)
        {
            for (Preference pref : s.all_preferences)
                add_pref_to_results(pref);
            for (Wme w : s.wmes)
                add_results_if_needed(w.value);
        }

        // now scan through extra prefs and look for any with this id
        for (Preference pref : this.extra_result_prefs_from_instantiation)
        {
            if (pref.id == id)
                add_pref_to_results(pref);
        }
    }
 

    /**
     * 
     * chunk.cpp:144:get_results_for_instantiation
     * 
     * @param inst
     * @return
     */
    private Preference get_results_for_instantiation(Instantiation inst)
    {
        this.results = null;
        this.results_match_goal_level = inst.match_goal_level;
        this.results_tc_number = context.syms.get_new_tc_number();
        this.extra_result_prefs_from_instantiation = inst.preferences_generated;
        for (Preference pref : inst.preferences_generated)
            if ((pref.id.level < this.results_match_goal_level) && (pref.id.tc_number != this.results_tc_number))
            {
                add_pref_to_results(pref);
            }
        return this.results;
    }
    
    /**
     * chunk.cpp:181:variablize_symbol
     * 
     * <p>Note: In jsoar, modified to return new variable rather than replace 
     * byref argument
     * 
     * @param sym
     * @return
     */
    private Symbol variablize_symbol(Symbol sym)
    {
        Identifier id = sym.asIdentifier();
        if (id == null)
            return sym;
        if (!this.variablize_this_chunk)
            return sym;

        if (id.tc_number == this.variablization_tc)
        {
            // it's already been variablized, so use the existing variable
            return id.variablization;
        }

        // need to create a new variable
        id.tc_number = this.variablization_tc;
        Variable var = context.variableGenerator.generate_new_variable(Character.toString(id.name_letter));
        id.variablization = var;
        return var;
    }

    /**
     * chunk.cpp:207:variablize_test
     * @param t
     */
    private void variablize_test(Test t)
    {
        if (Test.isBlank(t))
            return;
        
        EqualityTest eq = t.asEqualityTest();
        if (eq != null)
        {
            eq.sym = variablize_symbol(eq.sym);
            /* Warning: this relies on the representation of tests */
            return;
        }

        if (t.asGoalIdTest() != null || t.asImpasseIdTest() != null || t.asDisjunctionTest() != null)
        {
            return;
        }

        ConjunctiveTest ct = t.asConjunctiveTest();
        if (ct != null)
        {
            for (Test c : ct.conjunct_list)
            {
                variablize_test(c);
            }
            return;
        }
        // relational tests other than equality
        RelationalTest rt = t.asRelationalTest();
        rt.referent = variablize_symbol(rt.referent);
    }

    /**
     * chunk.cpp:235:variablize_condition_list
     * 
     * @param cond
     */
    private void variablize_condition_list(Condition cond)
    {
        for (; cond != null; cond = cond.next)
        {
            ThreeFieldCondition tfc = cond.asThreeFieldCondition();
            if (tfc != null)
            {
                variablize_test(tfc.id_test);
                variablize_test(tfc.attr_test);
                variablize_test(tfc.value_test);
            }

            ConjunctiveNegationCondition ncc = cond.asConjunctiveNegationCondition();
            if (ncc != null)
            {
                variablize_condition_list(ncc.top);
            }
        }
    }

    /**
     * 
     * <p>chunk.cpp:251:copy_and_variablize_result_list
     * 
     * @param pref
     * @return
     */
    private MakeAction copy_and_variablize_result_list(Preference pref)
    {
        if (pref == null)
            return null;

        MakeAction a = new MakeAction();

        a.id = new RhsSymbolValue(variablize_symbol(pref.id));
        a.attr = new RhsSymbolValue(variablize_symbol(pref.attr));
        a.value = new RhsSymbolValue(variablize_symbol(pref.value));

        a.preference_type = pref.type;

        if (pref.type.isBinary())
        {
            a.referent = new RhsSymbolValue(variablize_symbol(pref.referent));
        }

        a.next = copy_and_variablize_result_list(pref.next_result);
        return a;
    }

    /**
     * <p>This routine is called once backtracing is finished. It goes through the
     * ground conditions and builds a chunk_cond (see above) for each one. The
     * chunk_cond includes two new copies of the condition: one to be used for
     * the initial instantiation of the chunk, and one to be (variablized and)
     * used for the chunk itself.
     * 
     * <p>This routine also goes through the negated conditions and adds to the
     * ground set (again building chunk_cond's) any negated conditions that are
     * connected to the grounds.
     * 
     * <p>At exit, the "dest_top" and "dest_bottom" arguments are set to point to
     * the first and last chunk_cond in the ground set. The "tc_to_use" argument
     * is the tc number that this routine will use to mark the TC of the ground
     * set. At exit, this TC indicates the set of identifiers in the grounds.
     * (This is used immediately afterwards to figure out which Nots must be
     * added to the chunk.)
     * 
     * <p>chunk.cpp:409:build_chunk_conds_for_grounds_and_add_negateds
     * 
     * @param dest_top
     * @param dest_bottom
     * @param tc_to_use
     */
    private void build_chunk_conds_for_grounds_and_add_negateds(ByRef<ChunkCondition> dest_top,
            ByRef<ChunkCondition> dest_bottom, int tc_to_use)
    {
        AsListItem<ChunkCondition> first_cc = null; /* unnecessary, but gcc -Wall warns without it */

        // build instantiated conds for grounds and setup their TC
        AsListItem<ChunkCondition> prev_cc = null;
        while (!grounds.isEmpty())
        {
            Condition ground = grounds.pop();
            // make the instantiated condition
            ChunkCondition cc = new ChunkCondition(ground);
            cc.instantiated_cond = Condition.copy_condition(cc.cond);
            cc.variablized_cond = Condition.copy_condition(cc.cond);
            if (prev_cc != null)
            {
                prev_cc.next = cc.next_prev;
                cc.next_prev.previous = prev_cc;
                cc.variablized_cond.prev = prev_cc.get().variablized_cond;
                prev_cc.get().variablized_cond.next = cc.variablized_cond;
            }
            else
            {
                first_cc = cc.next_prev;
                cc.next_prev.previous = null;
                cc.variablized_cond.prev = null;
            }
            prev_cc = cc.next_prev;
            // add this in to the TC
            // TODO eliminate dummy lists
            ground.add_cond_to_tc(tc_to_use, new LinkedList<Identifier>(), new LinkedList<Variable>());
        }

        // scan through negated conditions and check which ones are connected
        // to the grounds
        context.trace.print(Category.TRACE_BACKTRACING_SYSPARAM, "\n\n*** Adding Grounded Negated Conditions ***\n");

        while (!negated_set.all.isEmpty())
        {
            ChunkCondition cc = negated_set.all.getFirstItem();
            negated_set.remove_from_chunk_cond_set(cc);
            if (cc.cond.cond_is_in_tc(tc_to_use))
            {
                // negated cond is in the TC, so add it to the grounds

                // TODO implement print_condition as formattable
                context.trace.print(Category.TRACE_BACKTRACING_SYSPARAM, "\n-.Moving to grounds: %s", cc.cond);

                cc.instantiated_cond = Condition.copy_condition(cc.cond);
                cc.variablized_cond = Condition.copy_condition(cc.cond);
                if (prev_cc != null)
                {
                    prev_cc.next = cc.next_prev;
                    cc.next_prev.previous = prev_cc;
                    cc.variablized_cond.prev = prev_cc.get().variablized_cond;
                    prev_cc.get().variablized_cond.next = cc.variablized_cond;
                }
                else
                {
                    first_cc = cc.next_prev;
                    cc.next_prev.previous = null;
                    cc.variablized_cond.prev = null;
                }
                prev_cc = cc.next_prev;
            }
            else
            {
                /* --- not in TC, so discard the condition --- */

                if (!chunkThroughLocalNegations)
                {
                    // this chunk will be overgeneral! don't create it

                    // SBW 5/07
                    // report what local negations are preventing the chunk,
                    // and set flags like we saw a ^quiescence t so it won't be
                    // created
                    // TODO Implement report_local_negation()
                    // report_local_negation ( thisAgent, cc.cond ); // in backtrace.cpp
                    this.quiescence_t_flag = true;
                    this.variablize_this_chunk = false;
                }

                cc = null; // free_with_pool (&thisAgent.chunk_cond_pool, cc);
            }
        }

        if (prev_cc != null)
        {
            prev_cc.next = null;
            prev_cc.get().variablized_cond.next = null;
        }
        else
        {
            first_cc = null;
        }

        dest_top.value = first_cc.get();
        dest_bottom.value = prev_cc.get();
    }
    
    /**
     * This routine looks through all the Nots in the instantiations in
     * instantiations_with_nots, and returns copies of the ones involving pairs
     * of identifiers in the grounds. Before this routine is called, the ids in
     * the grounds must be marked with "tc_of_grounds."
     * 
     * <p>chunk.cpp:512:get_nots_for_instantiated_conditions
     * 
     * @param instantiations_with_nots
     * @param tc_of_grounds
     * @return
     */
    private NotStruct get_nots_for_instantiated_conditions(LinkedList<Instantiation> instantiations_with_nots,
            int tc_of_grounds)
    {
        // collect nots for which both id's are marked
        NotStruct collected_nots = null;
        while (!instantiations_with_nots.isEmpty())
        {
            Instantiation inst = instantiations_with_nots.pop();

            for (NotStruct n1 = inst.nots; n1 != null; n1 = n1.next)
            {
                // Are both id's marked? If no, goto next loop iteration
                if (n1.s1.tc_number != tc_of_grounds)
                    continue;
                if (n1.s2.tc_number != tc_of_grounds)
                    continue;

                // If the pair already in collected_nots, goto next iteration
                NotStruct n2;
                for (n2 = collected_nots; n2 != null; n2 = n2.next)
                {
                    if ((n2.s1 == n1.s1) && (n2.s2 == n1.s2))
                        break;
                    if ((n2.s1 == n1.s2) && (n2.s2 == n1.s1))
                        break;
                }
                if (n2 != null)
                    continue;

                // Add the pair to collected_nots
                NotStruct new_not = new NotStruct(n1.s1, n1.s2);
                new_not.next = collected_nots;
                collected_nots = new_not;
            }
        }

        return collected_nots;
    }

    /**
     * This routine goes through the given list of Nots and, for each one,
     * inserts a variablized copy of it into the given condition list at the
     * earliest possible location. (The given condition list should be the
     * previously-variablized condition list that will become the chunk's LHS.)
     * The given condition list is destructively modified; the given Not list is
     * unchanged.
     * 
     * <p>chunk.cpp:561:variablize_nots_and_insert_into_conditions
     * 
     * @param nots
     * @param conds
     */
    private void variablize_nots_and_insert_into_conditions(NotStruct nots, Condition conds)
    {
        // don't bother Not-ifying justifications
        if (!variablize_this_chunk)
            return;

        for (NotStruct n = nots; n != null; n = n.next)
        {
            Symbol var1 = n.s1.variablization;
            Symbol var2 = n.s2.variablization;
            // find where var1 is bound, and add "<> var2" to that test
            RelationalTest t = new RelationalTest(RelationalTest.NOT_EQUAL_TEST, var2);
            boolean added_it = false;
            for (Condition c = conds; c != null; c = c.next)
            {
                PositiveCondition pc = c.asPositiveCondition();
                if (pc == null)
                    continue;

                if (TestTools.test_includes_equality_test_for_symbol(pc.id_test, var1))
                {
                    ByRef<Test> id_test = ByRef.create(pc.id_test);
                    TestTools.add_new_test_to_test(id_test, t);
                    pc.id_test = id_test.value;
                    added_it = true;
                    break;
                }
                if (TestTools.test_includes_equality_test_for_symbol(pc.attr_test, var1))
                {
                    ByRef<Test> attr_test = ByRef.create(pc.attr_test);
                    TestTools.add_new_test_to_test(attr_test, t);
                    pc.attr_test = attr_test.value;
                    added_it = true;
                    break;
                }
                if (TestTools.test_includes_equality_test_for_symbol(pc.value_test, var1))
                {
                    ByRef<Test> value_test = ByRef.create(pc.value_test);
                    TestTools.add_new_test_to_test(value_test, t);
                    pc.value_test = value_test.value;
                    added_it = true;
                    break;
                }
            }
            if (!added_it)
            {
                throw new IllegalStateException("Internal error: couldn't add Not test to chunk");
            }
        }
    }
    
    /**
     * This routine adds goal id or impasse id tests to the variablized
     * conditions. For each id in the grounds that happens to be the identifier
     * of a goal or impasse, we add a goal/impasse id test to the variablized
     * conditions, to make sure that in the resulting chunk, the variablization
     * of that id is constrained to match against a goal/impasse. (Note:
     * actually, in the current implementation of chunking, it's impossible for
     * an impasse id to end up in the ground set. So part of this code is
     * unnecessary.)
     * 
     * <p>
     * chunk.cpp:628:add_goal_or_impasse_tests
     * 
     * @param all_ccs
     */
    private void add_goal_or_impasse_tests(AsListItem<ChunkCondition> all_ccs)
    {
        int tc = context.syms.get_new_tc_number();
        for (AsListItem<ChunkCondition> ccIter = all_ccs; ccIter != null; ccIter = ccIter.next)
        {
            ChunkCondition cc = ccIter.get();
            PositiveCondition pc = cc.instantiated_cond.asPositiveCondition();
            if (pc == null)
                continue;

            // TODO Assumes id_test is equality test of identifier
            Identifier id = pc.id_test.asEqualityTest().getReferent().asIdentifier();

            if ((id.isa_goal || id.isa_impasse) && (id.tc_number != tc))
            {
                Test t = id.isa_goal ? new GoalIdTest() : new ImpasseIdTest();
                // TODO Assumes variablized_cond is three-field (put this
                // assumption in class?)
                ByRef<Test> id_test = ByRef.create(cc.variablized_cond.asThreeFieldCondition().id_test);
                TestTools.add_new_test_to_test(id_test, t);
                cc.variablized_cond.asThreeFieldCondition().id_test = id_test.value;

                id.tc_number = tc;
            }
        }
    }

    /**
     * <p>The Rete routines require the instantiated conditions (on the
     * instantiation structure) to be in the same order as the original
     * conditions from which the Rete was built. This means that the initial
     * instantiation of the chunk must have its conditions in the same order as
     * the variablized conditions. The trouble is, the variablized conditions
     * get rearranged by the reorderer. So, after reordering, we have to
     * rearrange the instantiated conditions to put them in the same order as
     * the now-scrambled variablized ones. This routine does this.
     * 
     * <p>Okay, so the obvious way is to have each variablized condition (VCond)
     * point to the corresponding instantiated condition (ICond). Then after
     * reordering the VConds, we'd scan through the VConds and say
     *    VCond->Icond->next = VCond->next->Icond 
     *    VCond->Icond->prev = VCond->prev->Icond 
     * (with some extra checks for the first and last VCond in the list).
     * 
     * <p>The problem with this is that it takes an extra 4 bytes per condition,
     * for the "ICond" field. Conditions were taking up a lot of memory in my
     * test cases, so I wanted to shrink them. This routine avoids needing the 4
     * extra bytes by using the following trick: first "swap out" 4 bytes from
     * each VCond; then use that 4 bytes for the "ICond" field. Now run the
     * above algorithm. Finally, swap those original 4 bytes back in.
     * 
     * <p>chunk.cpp:680:reorder_instantiated_conditions
     * 
     * @param top_cc
     * @param dest_inst_top
     * @param dest_inst_bottom
     */
    private void reorder_instantiated_conditions(ListHead<ChunkCondition> top_cc, ByRef<Condition> dest_inst_top,
            ByRef<Condition> dest_inst_bottom)
    {
        // Step 1: swap prev pointers out of variablized conds into chunk_conds,
        // and swap pointer to the corresponding instantiated conds into the
        // variablized conds' prev pointers
        for (ChunkCondition cc : top_cc)
        {
            cc.saved_prev_pointer_of_variablized_cond = cc.variablized_cond.prev;
            cc.variablized_cond.prev = cc.instantiated_cond;
        }

        // Step 2: do the reordering of the instantiated conds
        for (ChunkCondition cc : top_cc)
        {
            if (cc.variablized_cond.next != null)
            {
                cc.instantiated_cond.next = cc.variablized_cond.next.prev;
            }
            else
            {
                cc.instantiated_cond.next = null;
                dest_inst_bottom.value = cc.instantiated_cond;
            }

            if (cc.saved_prev_pointer_of_variablized_cond != null)
            {
                cc.instantiated_cond.prev = cc.saved_prev_pointer_of_variablized_cond.prev;
            }
            else
            {
                cc.instantiated_cond.prev = null;
                dest_inst_top.value = cc.instantiated_cond;
            }
        }

        // Step 3:  restore the prev pointers on variablized conds
        for (ChunkCondition cc : top_cc)
        {
            cc.variablized_cond.prev = cc.saved_prev_pointer_of_variablized_cond;
        }
    }
    /**
     * @param inst
     * @param b
     */
    public void chunk_instantiation(Instantiation inst, boolean b)
    {
        // TODO Implement chunk_instantiation
        //throw new UnsupportedOperationException("chunk_instantiation not implemented");
    }


}
