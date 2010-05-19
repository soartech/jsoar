/*
 * Copyright (c) 2008  Dave Ray <daveray@gmail.com>
 *
 * Created on Aug 28, 2008
 */
package org.jsoar.kernel.rete;

import java.util.Arrays;
import java.util.List;

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
import org.jsoar.kernel.rhs.ReteLocation;
import org.jsoar.kernel.rhs.RhsFunctionCall;
import org.jsoar.kernel.rhs.RhsSymbolValue;
import org.jsoar.kernel.rhs.RhsValue;
import org.jsoar.kernel.rhs.UnboundVariable;
import org.jsoar.kernel.symbols.SymbolImpl;
import org.jsoar.kernel.symbols.Variable;
import org.jsoar.util.Arguments;
import org.jsoar.util.ByRef;
import org.jsoar.util.ListHead;
import org.jsoar.util.markers.Marker;

/**
 * Functions and data structures for building the rete network. Extracted
 * from rete.cpp for sanity.
 * 
 * @author ray
 */
/*package*/ class ReteBuilder
{

    /* ---------------------------------------------------------------------

           Test Type <---> Relational (Rete) Test Type Conversion Tables

       These tables convert from xxx_TEST's (defined in soarkernel.h for various
       kinds of complex_test's) to xxx_RETE_TEST's (defined in rete.cpp for
       the different kinds of Rete tests), and vice-versa.  We might just 
       use the same set of constants for both purposes, but we want to be
       able to do bit-twiddling on the RETE_TEST types.

       (This stuff probably doesn't belong under "Building the Rete Net",
       but I wasn't sure where else to put it.)
    --------------------------------------------------------------------- */

    /* Warning: the two items below must not be the same as any xxx_TEST's defined
    in soarkernel.h for the types of complex_test's */
    /*package*/ static final int EQUAL_TEST_TYPE = 254; 
    private static final int ERROR_TEST_TYPE = 255;
    private static final int test_type_to_relational_test_type[];
    /*package*/ static final int relational_test_type_to_test_type[];


    static
    {
        // rete.cpp:2773:init_test_type_conversion_tables
        test_type_to_relational_test_type = new int[256];
        Arrays.fill(test_type_to_relational_test_type, ERROR_TEST_TYPE);

        relational_test_type_to_test_type = new int[256];
        Arrays.fill(relational_test_type_to_test_type, ERROR_TEST_TYPE);

        /* we don't need ...[equal test] */
        test_type_to_relational_test_type[RelationalTest.NOT_EQUAL_TEST] = ReteTest.RELATIONAL_NOT_EQUAL_RETE_TEST;
        test_type_to_relational_test_type[RelationalTest.LESS_TEST] = ReteTest.RELATIONAL_LESS_RETE_TEST;
        test_type_to_relational_test_type[RelationalTest.GREATER_TEST] = ReteTest.RELATIONAL_GREATER_RETE_TEST;
        test_type_to_relational_test_type[RelationalTest.LESS_OR_EQUAL_TEST] = ReteTest.RELATIONAL_LESS_OR_EQUAL_RETE_TEST;
        test_type_to_relational_test_type[RelationalTest.GREATER_OR_EQUAL_TEST] = ReteTest.RELATIONAL_GREATER_OR_EQUAL_RETE_TEST;
        test_type_to_relational_test_type[RelationalTest.SAME_TYPE_TEST] = ReteTest.RELATIONAL_SAME_TYPE_RETE_TEST;

        relational_test_type_to_test_type[ReteTest.RELATIONAL_EQUAL_RETE_TEST] = EQUAL_TEST_TYPE;
        relational_test_type_to_test_type[ReteTest.RELATIONAL_NOT_EQUAL_RETE_TEST] = RelationalTest.NOT_EQUAL_TEST;
        relational_test_type_to_test_type[ReteTest.RELATIONAL_LESS_RETE_TEST] = RelationalTest.LESS_TEST;
        relational_test_type_to_test_type[ReteTest.RELATIONAL_GREATER_RETE_TEST] = RelationalTest.GREATER_TEST;
        relational_test_type_to_test_type[ReteTest.RELATIONAL_LESS_OR_EQUAL_RETE_TEST] = RelationalTest.LESS_OR_EQUAL_TEST;
        relational_test_type_to_test_type[ReteTest.RELATIONAL_GREATER_OR_EQUAL_RETE_TEST] = RelationalTest.GREATER_OR_EQUAL_TEST;
        relational_test_type_to_test_type[ReteTest.RELATIONAL_SAME_TYPE_RETE_TEST] = RelationalTest.SAME_TYPE_TEST;
    }
    
    private ReteBuilder()
    {
        
    }
    
    /**
     * This is used for converting tests (from conditions) into the appropriate
     * rete_test's and/or constant-to-be-tested-by-the-alpha-network.  It takes
     * all sub-tests from a given test, converts them into the necessary Rete
     * tests (if any -- note that an equality test with a previously-unbound
     * variable can be ignored), and destructively adds the Rete tests to
     * the given "rt" parameter.  The "current_depth" and "field_num" params
     * tell where the current test originated.
     * 
     * <p>For any field, we can handle one equality-with-a-constant test in the
     * alpha net.  If the "*alpha_constant" parameter is initially NIL, this
     * routine may also set *alpha_constant to point to the constant symbol
     * for the alpha net to test (rather than creating the corresponding
     * rete_test).
     * 
     * <p>Before calling this routine, variables should be bound densely for
     * parent and higher conditions, and sparsely for the current condition.
     * 
     * <p>rete.cpp:2819:add_rete_tests_for_test
     * 
     * @param rete
     * @param t
     * @param current_depth
     * @param field_num
     * @param rt
     * @param alpha_constant
     */
    static void add_rete_tests_for_test(Rete rete, Test t, int current_depth, int field_num, ByRef<ReteTest> rt,
            ByRef<SymbolImpl> alpha_constant)
    {
        if (Tests.isBlank(t))
        {
            return;
        }

        final EqualityTest eq = t.asEqualityTest();
        if (eq != null)
        {
            final SymbolImpl referent = eq.getReferent();

            // if constant test and alpha=NIL, install alpha test
            if (referent.asVariable() == null && alpha_constant.value == null)
            {
                alpha_constant.value = referent;
                return;
            }

            // if constant, make = constant test
            if (referent.asVariable() == null)
            {
                ReteTest new_rt = ReteTest.createConstantTest(ReteTest.RELATIONAL_EQUAL_RETE_TEST, field_num, referent);
                
                new_rt.next = rt.value;
                rt.value = new_rt;
                return;
            }

            // variable: if binding is for current field, do nothing
            final VarLocation where = VarLocation.find(referent.asVariable(), current_depth);
            if (where == null)
            {
                throw new IllegalStateException("Error: Rete build found test of unbound var: " + referent);
            }
            if ((where.levels_up == 0) && (where.field_num == field_num))
            {
                return;
            }

            // else make variable equality test
            final ReteTest new_rt = ReteTest.createVariableTest(ReteTest.RELATIONAL_EQUAL_RETE_TEST, field_num, where);
            
            new_rt.next = rt.value;
            rt.value = new_rt;
            return;
        }

        final RelationalTest relational = t.asRelationalTest();
        if (relational != null)
        {

            // if constant, make constant test
            if (relational.referent.asVariable() == null)
            {
                ReteTest new_rt = ReteTest.createConstantTest(test_type_to_relational_test_type[relational.type], field_num, relational.referent);
                
                new_rt.next = rt.value;
                rt.value = new_rt;
                return;
            }
            // else make variable test
            VarLocation where = VarLocation.find(relational.referent.asVariable(), current_depth);
            if (where == null)
            {
                throw new IllegalStateException("Error: Rete build found test of unbound var: " + relational.referent);
            }
            ReteTest new_rt = ReteTest.createVariableTest(test_type_to_relational_test_type[relational.type], field_num, where);
            
            new_rt.next = rt.value;
            rt.value = new_rt;
            return;
        }

        final DisjunctionTest dt = t.asDisjunctionTest();
        if (dt != null)
        {
            // disjunct list is immutable so it's safe to just pass in
            ReteTest new_rt = ReteTest.createDisjunctionTest(field_num, dt.disjunction_list);
            
            new_rt.next = rt.value;
            rt.value = new_rt;
            return;
        }

        final ConjunctiveTest ct = t.asConjunctiveTest();
        if (ct != null)
        {
            for (Test c : ct.conjunct_list)
            {
                add_rete_tests_for_test(rete, c, current_depth, field_num, rt, alpha_constant);
            }
            return;
        }

        final GoalIdTest gid = t.asGoalIdTest();
        if (gid != null)
        {
            ReteTest new_rt = ReteTest.createGoalIdTest();
            
            new_rt.next = rt.value;
            rt.value = new_rt;
            return;
        }

        final ImpasseIdTest iid = t.asImpasseIdTest();
        if (iid != null)
        {
            ReteTest new_rt = ReteTest.createImpasseIdTest();
            
            new_rt.next = rt.value;
            rt.value = new_rt;
            return;
        }

        throw new IllegalStateException("Error: found unknown test type while building rete: " + t);
    }


    /**
     * This is used for checking whether an existing Rete node can be 
     * shared, instead of building a new one.
     * 
     * <p>Single_rete_tests_are_identical() checks whether two (non-conjunctive)
     * Rete tests are the same.  (Note that in the case of disjunction tests,
     * the symbols in the disjunction have to be in the same order; this 
     * simplifies and speeds up the code here, but unnecessarily reduces
     * sharing.)
     * 
     * <p>rete.cpp:2979:single_rete_tests_are_identical
     * 
     * @param rt1
     * @param rt2
     * @return
     */
    private static boolean single_rete_tests_are_identical(ReteTest rt1, ReteTest rt2)
    {
        if (rt1.type != rt2.type)
        {
            return false;
        }

        if (rt1.right_field_num != rt2.right_field_num)
        {
            return false;
        }

        if (ReteTest.test_is_variable_relational_test(rt1.type))
        {
            return VarLocation.var_locations_equal(rt1.variable_referent, rt2.variable_referent);
        }

        if (ReteTest.test_is_constant_relational_test(rt1.type))
        {
            return (rt1.constant_referent == rt2.constant_referent);
        }

        if (rt1.type == ReteTest.ID_IS_GOAL_RETE_TEST)
        {
            return true;
        }
        if (rt1.type == ReteTest.ID_IS_IMPASSE_RETE_TEST)
        {
            return true;
        }

        if (rt1.type == ReteTest.DISJUNCTION_RETE_TEST)
        {
            return rt1.disjunction_list.equals(rt2.disjunction_list);
        }
        throw new IllegalStateException("Error: found unknown rete test type while building rete: " + rt1 + ", " + rt2);
    }

    /**
     * Rete_test_lists_are_identical() checks whether two lists of Rete tests
     * are identical.  (Note that the lists have to be in the order; the code
     * here doesn't check all possible orderings.)
     * 
     * <p>rete.cpp:3016
     * 
     * @param rt1
     * @param rt2
     * @return
     */
    private static boolean rete_test_lists_are_identical(ReteTest rt1, ReteTest rt2)
    {
        while (rt1 != null && rt2 != null)
        {
            if (!single_rete_tests_are_identical(rt1, rt2))
                return false;
            rt1 = rt1.next;
            rt2 = rt2.next;
        }
        return rt1 == rt2; // make sure they both hit end-of-list
    }

    /**
     * Extracts from a Rete test list the variable equality test to use for
     * hashing.  Returns TRUE if successful, or FALSE if there was no such
     * test to use for hashing.  The Rete test list ("rt") is destructively
     * modified to splice out the extracted test.
     * 
     * <p>rete.cpp:3042:extract_rete_test_to_hash_with
     * 
     * @param rt
     * @param dest_hash_loc
     * @return
     */
    private static boolean extract_rete_test_to_hash_with(ByRef<ReteTest> rt, ByRef<VarLocation> dest_hash_loc)
    {
        // look through rt list, find the first variable equality test
        ReteTest current = null, prev = null;
        for (current = rt.value; current != null; prev = current, current = current.next)
        {
            if (current.type == ReteTest.VARIABLE_RELATIONAL_RETE_TEST + ReteTest.RELATIONAL_EQUAL_RETE_TEST)
            {
                break;
            }
        }

        // no variable equality test was found
        if (current == null)
        {
            dest_hash_loc.value = VarLocation.DEFAULT;
            return false;
        }

        // unlink it from rt
        if (prev != null)
        {
            prev.next = current.next;
        }
        else
        {
            rt.value = current.next;
        }

        // extract info, and deallocate that single test
        dest_hash_loc.value = current.variable_referent;
        current.next = null;
        //deallocate_rete_test_list (thisAgent, current);
        return true;
    }

    /**
     * Finds or creates a node for the given single condition <cond>, which
     * must be a simple positive condition.  The node is made a child of the
     * given <parent> node.  Variables for earlier conditions should be bound
     * densely before this routine is called.  The routine returns a pointer 
     * to the (newly-created or shared) node.
     * 
     * <p>rete.cpp:3069:make_node_for_positive_cond
     * 
     * @param rete
     * @param cond
     * @param current_depth
     * @param parent
     * @return
     */
    private static ReteNode make_node_for_positive_cond(Rete rete, PositiveCondition cond, int current_depth, ReteNode parent)
    {
        Arguments.checkNotNull(rete, "rete");
        Arguments.checkNotNull(cond, "cond");
        Arguments.check(current_depth >= 0, "current_depth >= 0");
        Arguments.checkNotNull(parent, "parent");
        
        final ListHead<Variable> vars_bound_here = ListHead.newInstance();

        // Add sparse variable bindings for this condition
        Rete.bind_variables_in_test(cond.id_test, current_depth, 0, false, vars_bound_here);
        Rete.bind_variables_in_test(cond.attr_test, current_depth, 1, false, vars_bound_here);
        Rete.bind_variables_in_test(cond.value_test, current_depth, 2, false, vars_bound_here);

        // Get Rete tests, alpha constants, and hash location
        final ByRef<SymbolImpl> alpha_id = ByRef.create(null);
        final ByRef<SymbolImpl> alpha_attr = ByRef.create(null);
        final ByRef<SymbolImpl> alpha_value = ByRef.create(null);
        final ByRef<ReteTest> rt = ByRef.create(null);
        add_rete_tests_for_test(rete, cond.id_test, current_depth, 0, rt, alpha_id);
        final ByRef<VarLocation> left_hash_loc = ByRef.create(null);
        boolean hash_this_node = extract_rete_test_to_hash_with(rt, left_hash_loc);
        add_rete_tests_for_test(rete, cond.attr_test, current_depth, 1, rt, alpha_attr);
        add_rete_tests_for_test(rete, cond.value_test, current_depth, 2, rt, alpha_value);

        // Pop sparse variable bindings for this condition
        Rete.pop_bindings_and_deallocate_list_of_variables(vars_bound_here);

        // Get alpha memory
        final AlphaMemory am = rete.find_or_make_alpha_mem(alpha_id.value, alpha_attr.value, alpha_value.value,
                cond.test_for_acceptable_preference);

        /*
         * Algorithm for adding node: 
         * 1. look for matching mem node; if found then look for matching join node; 
         *    create new one if no match 
         * 2. no matching mem node: 
         *    look for mp node with matching mem if found, 
         *    if join part matches too, then done 
         *    else delete mp node, create mem node and 2 joins 
         *    if not matching mem node, create new mp node.
         */

        // determine desired node types
        final ReteNodeType pos_node_type, mem_node_type, mp_node_type;
        if (hash_this_node)
        {
            pos_node_type = ReteNodeType.POSITIVE_BNODE;
            mem_node_type = ReteNodeType.MEMORY_BNODE;
            mp_node_type = ReteNodeType.MP_BNODE;
        }
        else
        {
            pos_node_type = ReteNodeType.UNHASHED_POSITIVE_BNODE;
            mem_node_type = ReteNodeType.UNHASHED_MEMORY_BNODE;
            mp_node_type = ReteNodeType.UNHASHED_MP_BNODE;
        }

        // look for a matching existing memory node
        ReteNode node, mem_node;
        for (mem_node = parent.first_child; mem_node != null; mem_node = mem_node.next_sibling)
        {
            if ((mem_node.node_type == mem_node_type)
                    && ((!hash_this_node) || ((mem_node.left_hash_loc_field_num == left_hash_loc.value.field_num) && (mem_node.left_hash_loc_levels_up == left_hash_loc.value.levels_up))))
            {
                break;
            }
        }

        if (mem_node != null)
        { /* -- A matching memory node was found --- */
            /* --- look for a matching existing join node --- */
            for (node = mem_node.first_child; node != null; node = node.next_sibling)
            {
                if ((node.node_type == pos_node_type) && (am == node.b_posneg.alpha_mem_)
                        && rete_test_lists_are_identical(node.b_posneg.other_tests, rt.value))
                {
                    break;
                }
            }

            if (node != null)
            { 
                // A matching join node was found
                rt.value = null; 
                am.remove_ref_to_alpha_mem(rete);
                return node;
            }
            else
            { 
                // No match was found, so create a new node
                node = ReteNode.make_new_positive_node(rete, mem_node, pos_node_type, am, rt.value, false);
                return node;
            }
        }

        // No matching memory node was found; look for MP with matching M
        ReteNode mp_node;
        for (mp_node = parent.first_child; mp_node != null; mp_node = mp_node.next_sibling)
        {
            if ((mp_node.node_type == mp_node_type)
                    && ((!hash_this_node) || 
                        ((mp_node.left_hash_loc_field_num == left_hash_loc.value.field_num) && 
                        (mp_node.left_hash_loc_levels_up == left_hash_loc.value.levels_up))))
            {
                break;
            }
        }

        if (mp_node != null)
        { 
            // Found matching M part of MP
            if ((am == mp_node.b_posneg.alpha_mem_)
                    && rete_test_lists_are_identical(mp_node.b_posneg.other_tests, rt.value))
            {
                // Complete MP match was found
                rt.value = null;
                am.remove_ref_to_alpha_mem(rete);
                return mp_node;
            }

            // Delete MP node, replace it with M and two positive joins
            mem_node = ReteNode.split_mp_node(rete, mp_node);
            node = ReteNode.make_new_positive_node(rete, mem_node, pos_node_type, am, rt.value, false);
            return node;
        }

        // Didn't even find a matching M part of MP, so make a new MP node
        return ReteNode.make_new_mp_node(rete, parent, mp_node_type, left_hash_loc.value, am, rt.value, false);
    }  

    /**
     * Finds or creates a node for the given single condition <cond>, which
     * must be a simple negative (not ncc) condition.  The node is made a
     * child of the given <parent> node.  Variables for earlier conditions 
     * should be bound densely before this routine is called.  The routine 
     * returns a pointer to the (newly-created or shared) node.
     * 
     * <p>rete.cpp:3194:make_node_for_negative_cond
     * 
     * @param rete
     * @param cond
     * @param current_depth
     * @param parent
     * @return
     */
    private static ReteNode make_node_for_negative_cond(Rete rete, NegativeCondition cond, int current_depth, ReteNode parent)
    {
        ListHead<Variable> vars_bound_here = ListHead.newInstance();

        /* --- Add sparse variable bindings for this condition --- */
        Rete.bind_variables_in_test(cond.id_test, current_depth, 0, false, vars_bound_here);
        Rete.bind_variables_in_test(cond.attr_test, current_depth, 1, false, vars_bound_here);
        Rete.bind_variables_in_test(cond.value_test, current_depth, 2, false, vars_bound_here);

        /* --- Get Rete tests, alpha constants, and hash location --- */
        final ByRef<SymbolImpl> alpha_id = ByRef.create(null);
        final ByRef<ReteTest> rt = ByRef.create(null);
        add_rete_tests_for_test(rete, cond.id_test, current_depth, 0, rt, alpha_id);
        
        final ByRef<VarLocation> left_hash_loc = ByRef.create(null);
        boolean hash_this_node = extract_rete_test_to_hash_with(rt, left_hash_loc);
        
        final ByRef<SymbolImpl> alpha_attr = ByRef.create(null);
        add_rete_tests_for_test(rete, cond.attr_test, current_depth, 1, rt, alpha_attr);
        
        final ByRef<SymbolImpl> alpha_value = ByRef.create(null);
        add_rete_tests_for_test(rete, cond.value_test, current_depth, 2, rt, alpha_value);

        /* --- Pop sparse variable bindings for this condition --- */
        Rete.pop_bindings_and_deallocate_list_of_variables(vars_bound_here);

        /* --- Get alpha memory --- */
        final AlphaMemory am = rete.find_or_make_alpha_mem(alpha_id.value, alpha_attr.value, alpha_value.value,
                cond.test_for_acceptable_preference);

        /* --- determine desired node type --- */
        final ReteNodeType node_type = hash_this_node ? ReteNodeType.NEGATIVE_BNODE : ReteNodeType.UNHASHED_NEGATIVE_BNODE;

        /* --- look for a matching existing node --- */
        ReteNode node;
        for (node = parent.first_child; node != null; node = node.next_sibling)
        {
            if ((node.node_type == node_type)
                    && (am == node.b_posneg.alpha_mem_)
                    && ((!hash_this_node) || ((node.left_hash_loc_field_num == left_hash_loc.value.field_num) && (node.left_hash_loc_levels_up == left_hash_loc.value.levels_up)))
                    && rete_test_lists_are_identical(node.b_posneg.other_tests, rt.value))
            {
                break;
            }
        }
        
        if (node != null)
        { 
            // A matching node was found
            rt.value = null;
            am.remove_ref_to_alpha_mem(rete);
            return node;
        }
        else
        { /* --- No match was found, so create a new node --- */
            node = ReteNode.make_new_negative_node(rete, parent, node_type, left_hash_loc.value, am, rt.value);
            return node;
        }
    }
    
    /**
     * This routine builds or shares the Rete network for the conditions in 
     * the given <cond_list>.  <Depth_of_first_cond> tells the depth of the 
     * first condition/node; <parent> gives the parent node under which the
     * network should be built or shared.

     * <p>Three "dest" parameters may be used for returing results from this
     * routine.  If <dest_bottom_node> is given as non-NIL, this routine
     * fills it in with a pointer to the lowermost node in the resulting
     * network.  If <dest_bottom_depth> is non-NIL, this routine fills it
     * in with the depth of the lowermost node.  If <dest_vars_bound> is
     * non_NIL, this routine fills it in with a list of variables bound
     * in the given <cond_list>, and does not pop the bindings for those
     * variables, in which case the caller is responsible for popping theose
     * bindings.  If <dest_vars_bound> is given as NIL, then this routine
     * pops the bindings, and the caller does not have to do the cleanup.
     * 
     * <p>rete.cpp:3257:build_network_for_condition_list
     * 
     * @param rete
     * @param cond_list
     * @param depth_of_first_cond
     * @param parent
     * @param dest_bottom_node
     * @param dest_bottom_depth
     * @param dest_vars_bound
     */
    static /*package*/ void build_network_for_condition_list(Rete rete, Condition cond_list, int depth_of_first_cond,
            ReteNode parent, ByRef<ReteNode> dest_bottom_node, ByRef<Integer> dest_bottom_depth,
            ByRef<ListHead<Variable>> dest_vars_bound)
    {
        ReteNode node = parent;
        ReteNode new_node = null;
        final ByRef<ReteNode> subconditions_bottom_node = ByRef.create(null);
        int current_depth = depth_of_first_cond;
        final ListHead<Variable> vars_bound = ListHead.newInstance();

        for (Condition cond = cond_list; cond != null; cond = cond.next)
        {
            PositiveCondition pc = cond.asPositiveCondition();
            NegativeCondition nc = cond.asNegativeCondition();
            ConjunctiveNegationCondition ncc = cond.asConjunctiveNegationCondition();
            if (pc != null)
            {
                new_node = make_node_for_positive_cond(rete, pc, current_depth, node);
                /* --- Add dense variable bindings for this condition --- */
                Rete.bind_variables_in_test(pc.id_test, current_depth, 0, true, vars_bound);
                Rete.bind_variables_in_test(pc.attr_test, current_depth, 1, true, vars_bound);
                Rete.bind_variables_in_test(pc.value_test, current_depth, 2, true, vars_bound);
            }
            else if (nc != null)
            {
                new_node = make_node_for_negative_cond(rete, nc, current_depth, node);
            }
            else if (ncc != null)
            {
                /* --- first, make the subconditions part of the rete --- */
                build_network_for_condition_list(rete, ncc.top, current_depth, node, subconditions_bottom_node, null,
                        null);
                /* --- look for an existing CN node --- */
                ReteNode child;
                for (child = node.first_child; child != null; child = child.next_sibling)
                {
                    if (child.node_type == ReteNodeType.CN_BNODE)
                    {
                        if (child.b_cn.partner.parent == subconditions_bottom_node.value)
                        {
                            break;
                        }
                    }
                }
                /* --- share existing node or build new one --- */
                if (child != null)
                {
                    new_node = child;
                }
                else
                {
                    new_node = ReteNode.make_new_cn_node(rete, node, subconditions_bottom_node.value);
                }
            }
            else
            {
                throw new IllegalStateException("Unexpected condition type: " + cond);
            }

            node = new_node;
            current_depth++;
        }

        /* --- return results to caller --- */
        if (dest_bottom_node != null)
        {
            dest_bottom_node.value = node;
        }
        if (dest_bottom_depth != null)
        {
            dest_bottom_depth.value = current_depth - 1;
        }
        if (dest_vars_bound != null)
        {
            dest_vars_bound.value = vars_bound;
        }
        else
        {
            Rete.pop_bindings_and_deallocate_list_of_variables(vars_bound);
        }
    }

    /**
     * After we've built the network for a production, we go through its 
     * RHS and replace all the variables with reteloc's and unboundvar indices.
     * For each variable <v> on the RHS, if <v> is bound on the LHS, then
     * we replace RHS references to it with a specification of where its
     * LHS binding can be found, e.g., "the value field four levels up".
     * Each RHS variable <v> not bound on the LHS is replaced with an index,
     * e.g., "unbound varible number 6".  As we're doing this, we keep track
     * of the names of all the unbound variables.
     *
     * <p>When this routine is called, variables should be bound (densely) for
     * the entire LHS.
     * 
     * <p>NOTE: In CSoar, variables are added to the front of the unbound var
     * list and then reversed later by the calling code (because they're
     * using CSoar's built in cons list). Here, we just add to the end of
     * the list and omit the reversal.
     * 
     * <p>rete.cpp:3424:fixup_rhs_value_variable_references
     * 
     * @param rete
     * @param rv RHS value to fix up
     * @param bottom_depth
     * @param rhs_unbound_vars_for_new_prod Receives unbound variables
     * @param rhs_unbound_vars_tc TC number for finding unbound variables
     * @return The value to replace rv, possibly rv itself
     */
    /*package*/ static RhsValue fixup_rhs_value_variable_references(Rete rete, RhsValue rv, int bottom_depth,
            List<Variable> rhs_unbound_vars_for_new_prod, Marker rhs_unbound_vars_tc)
    {
        final RhsSymbolValue rvsym = rv.asSymbolValue();
        if (rvsym != null)
        {
            final Variable var = rvsym.getSym().asVariable();
            if (var == null)
            {
                return rv;
            }
            /* --- Found a variable. Is is bound on the LHS? --- */
            final VarLocation var_loc = VarLocation.find(var, bottom_depth + 1);
            if (var_loc != null)
            {
                /* --- Yes, replace it with reteloc --- */
                return ReteLocation.create(var_loc.field_num, var_loc.levels_up - 1);
            }
            else
            {
                /* --- No, replace it with rhs_unboundvar --- */
                int index = 0;
                if (var.tc_number != rhs_unbound_vars_tc)
                {
                    // index of v
                    index = rhs_unbound_vars_for_new_prod.size();
                    
                    rhs_unbound_vars_for_new_prod.add(var);
                    var.tc_number = rhs_unbound_vars_tc;
                    
                    var.unbound_variable_index = index;
                }
                else
                {
                    index = var.unbound_variable_index;
                }
                return UnboundVariable.create(index);
            }
        }

        final RhsFunctionCall fc = rv.asFunctionCall();
        if (fc != null)
        {
            final List<RhsValue> args = fc.getArguments();
            assert args == fc.getArguments(); // just make sure this stays by
                                                // ref
            for (int i = 0; i < args.size(); ++i)
            {
                RhsValue newV = fixup_rhs_value_variable_references(rete, args.get(i), bottom_depth, rhs_unbound_vars_for_new_prod,
                        rhs_unbound_vars_tc);
                args.set(i, newV);
            }
        }
        else
        {
            throw new IllegalArgumentException("Unknown value type: " + rv);
        }
        return rv;
    }
}
