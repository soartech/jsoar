/*
 * Copyright (c) 2008  Dave Ray <daveray@gmail.com>
 *
 * Created on Aug 28, 2008
 */
package org.jsoar.kernel.rete;

import java.util.Arrays;
import java.util.LinkedList;
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
import org.jsoar.kernel.rhs.ReteLocation;
import org.jsoar.kernel.rhs.RhsFunctionCall;
import org.jsoar.kernel.rhs.RhsSymbolValue;
import org.jsoar.kernel.rhs.RhsValue;
import org.jsoar.kernel.rhs.UnboundVariable;
import org.jsoar.kernel.symbols.Symbol;
import org.jsoar.kernel.symbols.Variable;
import org.jsoar.util.Arguments;
import org.jsoar.util.ByRef;

/**
 * @author ray
 */
public class ReteBuilder
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
    
    
/* ------------------------------------------------------------------------
                         Add Rete Tests for Test

------------------------------------------------------------------------ */

    /**
     * This is used for converting tests (from conditions) into the appropriate
     * rete_test's and/or constant-to-be-tested-by-the-alpha-network.  It takes
     * all sub-tests from a given test, converts them into the necessary Rete
     * tests (if any -- note that an equality test with a previously-unbound
     * variable can be ignored), and destructively adds the Rete tests to
     * the given "rt" parameter.  The "current_depth" and "field_num" params
     * tell where the current test originated.
     * 
     * For any field, we can handle one equality-with-a-constant test in the
     * alpha net.  If the "*alpha_constant" parameter is initially NIL, this
     * routine may also set *alpha_constant to point to the constant symbol
     * for the alpha net to test (rather than creating the corresponding
     * rete_test).
     * 
     * Before calling this routine, variables should be bound densely for
     * parent and higher conditions, and sparsely for the current condition.
     * 
     * rete.cpp:2819:add_rete_tests_for_test
     * 
     * @param rete
     * @param t
     * @param current_depth
     * @param field_num
     * @param rt
     * @param alpha_constant
     */
    void add_rete_tests_for_test(Rete rete, Test t, int current_depth, int field_num, ByRef<ReteTest> rt,
            ByRef<Symbol> alpha_constant)
    {
        if (t.isBlank())
        {
            return;
        }

        EqualityTest eq = t.asEqualityTest();
        if (eq != null)
        {
            Symbol referent = eq.getReferent();

            /* --- if constant test and alpha=NIL, install alpha test --- */
            if (referent.asVariable() == null && alpha_constant.value == null)
            {
                alpha_constant.value = referent;
                return;
            }

            /* --- if constant, make = constant test --- */
            if (referent.asVariable() == null)
            {
                ReteTest new_rt = new ReteTest();
                new_rt.right_field_num = field_num;
                new_rt.type = ReteTest.CONSTANT_RELATIONAL_RETE_TEST + ReteTest.RELATIONAL_EQUAL_RETE_TEST;
                new_rt.constant_referent = referent;
                // symbol_add_ref (referent);
                new_rt.next = rt.value;
                rt.value = new_rt;
                return;
            }

            /* --- variable: if binding is for current field, do nothing --- */
            VarLocation where = new VarLocation();
            if (!rete.find_var_location(referent.asVariable(), current_depth, where))
            {
                throw new IllegalStateException("Error: Rete build found test of unbound var: " + referent);
            }
            if ((where.levels_up == 0) && (where.field_num == field_num))
            {
                return;
            }

            /* --- else make variable equality test --- */
            ReteTest new_rt = new ReteTest();
            new_rt.right_field_num = field_num;
            new_rt.type = ReteTest.VARIABLE_RELATIONAL_RETE_TEST + ReteTest.RELATIONAL_EQUAL_RETE_TEST;
            new_rt.variable_referent = where;
            new_rt.next = rt.value;
            rt.value = new_rt;
            return;
        }

        RelationalTest relational = t.asRelationalTest();
        if (relational != null)
        {

            /* --- if constant, make constant test --- */
            if (relational.referent.asVariable() == null)
            {
                ReteTest new_rt = new ReteTest();
                new_rt.right_field_num = field_num;
                new_rt.type = ReteTest.CONSTANT_RELATIONAL_RETE_TEST
                        + test_type_to_relational_test_type[relational.type];
                new_rt.constant_referent = relational.referent;
                // symbol_add_ref (ct->data.referent);
                new_rt.next = rt.value;
                rt.value = new_rt;
                return;
            }
            /* --- else make variable test --- */
            VarLocation where = new VarLocation();
            if (!rete.find_var_location(relational.referent.asVariable(), current_depth, where))
            {
                throw new IllegalStateException("Error: Rete build found test of unbound var: " + relational.referent);
            }
            ReteTest new_rt = new ReteTest();
            new_rt.right_field_num = field_num;
            new_rt.type = ReteTest.VARIABLE_RELATIONAL_RETE_TEST + test_type_to_relational_test_type[relational.type];
            new_rt.variable_referent = where;
            new_rt.next = rt.value;
            rt.value = new_rt;
            return;
        }

        DisjunctionTest dt = t.asDisjunctionTest();
        if (dt != null)
        {
            ReteTest new_rt = new ReteTest();
            new_rt.right_field_num = field_num;
            new_rt.type = ReteTest.DISJUNCTION_RETE_TEST;
            new_rt.disjunction_list = Symbol.copy_symbol_list_adding_references(dt.disjunction_list);
            new_rt.next = rt.value;
            rt.value = new_rt;
            return;
        }

        ConjunctiveTest ct = t.asConjunctiveTest();
        if (ct != null)
        {
            for (Test c : ct.conjunct_list)
            {
                add_rete_tests_for_test(rete, c, current_depth, field_num, rt, alpha_constant);
            }
            return;
        }

        GoalIdTest gid = t.asGoalIdTest();
        if (gid != null)
        {
            ReteTest new_rt = new ReteTest();
            new_rt.type = ReteTest.ID_IS_GOAL_RETE_TEST;
            new_rt.right_field_num = 0;
            new_rt.next = rt.value;
            rt.value = new_rt;
            return;
        }

        ImpasseIdTest iid = t.asImpasseIdTest();
        if (iid != null)
        {
            ReteTest new_rt = new ReteTest();
            new_rt.type = ReteTest.ID_IS_IMPASSE_RETE_TEST;
            new_rt.right_field_num = 0;
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
     * Single_rete_tests_are_identical() checks whether two (non-conjunctive)
     * Rete tests are the same.  (Note that in the case of disjunction tests,
     * the symbols in the disjunction have to be in the same order; this 
     * simplifies and speeds up the code here, but unnecessarily reduces
     * sharing.)
     * 
     * rete.cpp:2979:single_rete_tests_are_identical
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
     * rete.cpp:3016
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
     * rete.cpp:3036:extract_rete_test_to_hash_with
     * 
     * @param rt
     * @param dest_hash_loc
     * @return
     */
    private static boolean extract_rete_test_to_hash_with(ByRef<ReteTest> rt, VarLocation dest_hash_loc)
    {

        /* --- look through rt list, find the first variable equality test --- */
        ReteTest current = null, prev = null;
        for (current = rt.value; current != null; prev = current, current = current.next)
        {
            if (current.type == ReteTest.VARIABLE_RELATIONAL_RETE_TEST + ReteTest.RELATIONAL_EQUAL_RETE_TEST)
            {
                break;

            }
        }

        /* no variable equality test was found */
        if (current == null)
        {
            return false;
        }

        /* --- unlink it from rt --- */
        if (prev != null)
        {
            prev.next = current.next;
        }
        else
        {
            rt.value = current.next;
        }

        /* --- extract info, and deallocate that single test --- */
        dest_hash_loc.assign(current.variable_referent);
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
     * rete.cpp:3069:make_node_for_positive_cond
     * 
     * @param rete
     * @param cond
     * @param current_depth
     * @param parent
     * @return
     */
    private ReteNode make_node_for_positive_cond(Rete rete, PositiveCondition cond, int current_depth, ReteNode parent)
    {
        Arguments.checkNotNull(rete, "rete");
        Arguments.checkNotNull(cond, "cond");
        Arguments.check(current_depth >= 0, "current_depth >= 0");
        Arguments.checkNotNull(parent, "parent");
        
        LinkedList<Variable> vars_bound_here = new LinkedList<Variable>();

        /* --- Add sparse variable bindings for this condition --- */
        Rete.bind_variables_in_test(cond.id_test, current_depth, 0, false, vars_bound_here);
        Rete.bind_variables_in_test(cond.attr_test, current_depth, 1, false, vars_bound_here);
        Rete.bind_variables_in_test(cond.value_test, current_depth, 2, false, vars_bound_here);

        /* --- Get Rete tests, alpha constants, and hash location --- */
        ByRef<Symbol> alpha_id = ByRef.create(null);
        ByRef<Symbol> alpha_attr = ByRef.create(null);
        ByRef<Symbol> alpha_value = ByRef.create(null);
        ByRef<ReteTest> rt = ByRef.create(null);
        add_rete_tests_for_test(rete, cond.id_test, current_depth, 0, rt, alpha_id);
        VarLocation left_hash_loc = new VarLocation();
        boolean hash_this_node = extract_rete_test_to_hash_with(rt, left_hash_loc);
        add_rete_tests_for_test(rete, cond.attr_test, current_depth, 1, rt, alpha_attr);
        add_rete_tests_for_test(rete, cond.value_test, current_depth, 2, rt, alpha_value);

        /* --- Pop sparse variable bindings for this condition --- */
        Rete.pop_bindings_and_deallocate_list_of_variables(vars_bound_here);

        /* --- Get alpha memory --- */
        AlphaMemory am = rete.find_or_make_alpha_mem(alpha_id.value, alpha_attr.value, alpha_value.value,
                cond.test_for_acceptable_preference);

        /*
         * --- Algorithm for adding node: 1. look for matching mem node; if
         * found then look for matching join node; create new one if no match 2.
         * no matching mem node: look for mp node with matching mem if found, if
         * join part matches too, then done else delete mp node, create mem node
         * and 2 joins if not matching mem node, create new mp node.
         */

        /* --- determine desired node types --- */
        ReteNodeType pos_node_type, mem_node_type, mp_node_type;
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

        /* --- look for a matching existing memory node --- */
        ReteNode node, mem_node;
        for (mem_node = parent.first_child; mem_node != null; mem_node = mem_node.next_sibling)
        {
            if ((mem_node.node_type == mem_node_type)
                    && ((!hash_this_node) || ((mem_node.left_hash_loc_field_num == left_hash_loc.field_num) && (mem_node.left_hash_loc_levels_up == left_hash_loc.levels_up))))
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
            { /* --- A matching join node was found --- */
                ReteTest.deallocate_rete_test_list(rt.value);
                am.remove_ref_to_alpha_mem(rete);
                return node;
            }
            else
            { /* --- No match was found, so create a new node --- */
                node = ReteNode.make_new_positive_node(rete, mem_node, pos_node_type, am, rt.value, false);
                return node;
            }
        }

        /*
         * --- No matching memory node was found; look for MP with matching M
         * ---
         */
        ReteNode mp_node;
        for (mp_node = parent.first_child; mp_node != null; mp_node = mp_node.next_sibling)
        {
            if ((mp_node.node_type == mp_node_type)
                    && ((!hash_this_node) || ((mp_node.left_hash_loc_field_num == left_hash_loc.field_num) && (mp_node.left_hash_loc_levels_up == left_hash_loc.levels_up))))
            {
                break;
            }
        }

        if (mp_node != null)
        { /* --- Found matching M part of MP --- */
            if ((am == mp_node.b_posneg.alpha_mem_)
                    && rete_test_lists_are_identical(mp_node.b_posneg.other_tests, rt.value))
            {
                /* --- Complete MP match was found --- */
                ReteTest.deallocate_rete_test_list(rt.value);
                am.remove_ref_to_alpha_mem(rete);
                return mp_node;
            }

            /* --- Delete MP node, replace it with M and two positive joins --- */
            mem_node = ReteNode.split_mp_node(rete, mp_node);
            node = ReteNode.make_new_positive_node(rete, mem_node, pos_node_type, am, rt.value, false);
            return node;
        }

        /* --- Didn't even find a matching M part of MP, so make a new MP node --- */
        return ReteNode.make_new_mp_node(rete, parent, mp_node_type, left_hash_loc, am, rt.value, false);
    }  

    /**
     * Finds or creates a node for the given single condition <cond>, which
     * must be a simple negative (not ncc) condition.  The node is made a
     * child of the given <parent> node.  Variables for earlier conditions 
     * should be bound densely before this routine is called.  The routine 
     * returns a pointer to the (newly-created or shared) node.
     * 
     * rete.cpp:3194:make_node_for_negative_cond
     * 
     * @param rete
     * @param cond
     * @param current_depth
     * @param parent
     * @return
     */
    private ReteNode make_node_for_negative_cond(Rete rete, NegativeCondition cond, int current_depth, ReteNode parent)
    {
        LinkedList<Variable> vars_bound_here = new LinkedList<Variable>();

        /* --- Add sparse variable bindings for this condition --- */
        Rete.bind_variables_in_test(cond.id_test, current_depth, 0, false, vars_bound_here);
        Rete.bind_variables_in_test(cond.attr_test, current_depth, 1, false, vars_bound_here);
        Rete.bind_variables_in_test(cond.value_test, current_depth, 2, false, vars_bound_here);

        /* --- Get Rete tests, alpha constants, and hash location --- */
        ByRef<Symbol> alpha_id = ByRef.create(null);
        ByRef<Symbol> alpha_attr = ByRef.create(null);
        ByRef<Symbol> alpha_value = ByRef.create(null);
        ByRef<ReteTest> rt = ByRef.create(null);
        add_rete_tests_for_test(rete, cond.id_test, current_depth, 0, rt, alpha_id);
        VarLocation left_hash_loc = new VarLocation();
        boolean hash_this_node = extract_rete_test_to_hash_with(rt, left_hash_loc);
        add_rete_tests_for_test(rete, cond.attr_test, current_depth, 1, rt, alpha_attr);
        add_rete_tests_for_test(rete, cond.value_test, current_depth, 2, rt, alpha_value);

        /* --- Pop sparse variable bindings for this condition --- */
        Rete.pop_bindings_and_deallocate_list_of_variables(vars_bound_here);

        /* --- Get alpha memory --- */
        AlphaMemory am = rete.find_or_make_alpha_mem(alpha_id.value, alpha_attr.value, alpha_value.value,
                cond.test_for_acceptable_preference);

        /* --- determine desired node type --- */
        ReteNodeType node_type = hash_this_node ? ReteNodeType.NEGATIVE_BNODE : ReteNodeType.UNHASHED_NEGATIVE_BNODE;

        /* --- look for a matching existing node --- */
        ReteNode node;
        for (node = parent.first_child; node != null; node = node.next_sibling)
        {
            if ((node.node_type == node_type)
                    && (am == node.b_posneg.alpha_mem_)
                    && ((!hash_this_node) || ((node.left_hash_loc_field_num == left_hash_loc.field_num) && (node.left_hash_loc_levels_up == left_hash_loc.levels_up)))
                    && rete_test_lists_are_identical(node.b_posneg.other_tests, rt.value))
            {
                break;
            }
        }
        if (node != null)
        { /* --- A matching node was found --- */
            ReteTest.deallocate_rete_test_list(rt.value);
            am.remove_ref_to_alpha_mem(rete);
            return node;
        }
        else
        { /* --- No match was found, so create a new node --- */
            node = ReteNode.make_new_negative_node(rete, parent, node_type, left_hash_loc, am, rt.value);
            return node;
        }
    }
    
    /**
     * This routine builds or shares the Rete network for the conditions in 
     * the given <cond_list>.  <Depth_of_first_cond> tells the depth of the 
     * first condition/node; <parent> gives the parent node under which the
     * network should be built or shared.

     * Three "dest" parameters may be used for returing results from this
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
     * rete.cpp:3257:build_network_for_condition_list
     * 
     * @param rete
     * @param cond_list
     * @param depth_of_first_cond
     * @param parent
     * @param dest_bottom_node
     * @param dest_bottom_depth
     * @param dest_vars_bound
     */
    /*package*/ void build_network_for_condition_list(Rete rete, Condition cond_list, int depth_of_first_cond,
            ReteNode parent, ByRef<ReteNode> dest_bottom_node, ByRef<Integer> dest_bottom_depth,
            ByRef<LinkedList<Variable>> dest_vars_bound)
    {
        ReteNode node = parent;
        ReteNode new_node = null;
        ByRef<ReteNode> subconditions_bottom_node = ByRef.create(null);
        int current_depth = depth_of_first_cond;
        LinkedList<Variable> vars_bound = new LinkedList<Variable>();

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
     * When this routine is called, variables should be bound (densely) for
     * the entire LHS.
     * 
     * rete.cpp:3424:fixup_rhs_value_variable_references
     * 
     * @param rete
     * @param rv
     * @param bottom_depth
     * @param rhs_unbound_vars_for_new_prod
     * @param num_rhs_unbound_vars_for_new_prod
     * @param rhs_unbound_vars_tc
     */
    /*package*/ void fixup_rhs_value_variable_references(Rete rete, ByRef<RhsValue> rv, int bottom_depth,
            LinkedList<Variable> rhs_unbound_vars_for_new_prod, ByRef<Integer> num_rhs_unbound_vars_for_new_prod,
            int rhs_unbound_vars_tc)
    {
        RhsSymbolValue rvsym = rv.value.asSymbolValue();
        if (rvsym != null)
        {
            Variable var = rvsym.getSym().asVariable();
            if (var == null)
            {
                return;
            }
            /* --- Found a variable. Is is bound on the LHS? --- */
            VarLocation var_loc = new VarLocation();
            if (rete.find_var_location(var, bottom_depth + 1, var_loc))
            {
                /* --- Yes, replace it with reteloc --- */
                // symbol_remove_ref (thisAgent, sym);
                rv.value = new ReteLocation(var_loc.field_num, var_loc.levels_up - 1);
            }
            else
            {
                /* --- No, replace it with rhs_unboundvar --- */
                int index = 0;
                if (var.tc_number != rhs_unbound_vars_tc)
                {
                    // symbol_add_ref (sym);
                    rhs_unbound_vars_for_new_prod.push(var);
                    var.tc_number = rhs_unbound_vars_tc;
                    
                    // Note: This originally just used ++, but crashed with a VerifyError
                    // which is actually a bug in Java:
                    //    http://bugs.sun.com/bugdatabase/view_bug.do;jsessionid=eb3fcd8f72ab4713f96e378a7575?bug_id=6614974
                    num_rhs_unbound_vars_for_new_prod.value = num_rhs_unbound_vars_for_new_prod.value + 1;
                    index = num_rhs_unbound_vars_for_new_prod.value;
                    var.unbound_variable_index = index;
                }
                else
                {
                    index = var.unbound_variable_index;
                }
                rv.value = new UnboundVariable(index);
                // symbol_remove_ref (thisAgent, sym);
            }
            return;
        }

        RhsFunctionCall fc = rv.value.asFunctionCall();
        if (fc != null)
        {
            List<RhsValue> args = fc.getArguments();
            assert args == fc.getArguments(); // just make sure this stays by
                                                // ref
            for (int i = 0; i < args.size(); ++i)
            {
                ByRef<RhsValue> v = ByRef.create(args.get(i));
                fixup_rhs_value_variable_references(rete, v, bottom_depth, rhs_unbound_vars_for_new_prod,
                        num_rhs_unbound_vars_for_new_prod, rhs_unbound_vars_tc);
                args.set(i, v.value);
            }
        }
    }
}
