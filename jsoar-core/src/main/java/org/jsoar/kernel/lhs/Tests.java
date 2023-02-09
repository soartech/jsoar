/*
 * (c) 2008  Dave Ray
 *
 * Created on Aug 16, 2008
 */
package org.jsoar.kernel.lhs;

import java.util.Collections;
import java.util.List;

import org.jsoar.kernel.symbols.IdentifierImpl;
import org.jsoar.kernel.symbols.SymbolImpl;
import org.jsoar.kernel.symbols.Variable;
import org.jsoar.util.ByRef;
import org.jsoar.util.ListHead;
import org.jsoar.util.markers.Marker;

import com.google.common.collect.Lists;

/**
 * Various utility methods for working with Test objects.
 * 
 * For test_is_blank_or_equality_test, just use {@link Test#asEqualityTest()}.
 * 
 * @author ray
 */
public class Tests
{
    /**
     * Looks through a test, and returns a new copy of the first equality test
     * it finds. Signals an error if there is no equality test in the given
     * test.
     * 
     * production.cpp:686:copy_of_equality_test_found_in_test
     * 
     * @param t
     * @return Copy of the first equality test found in the given test
     * @throws IllegalStateException If there is no equality test found.
     */
    public static EqualityTest copy_of_equality_test_found_in_test(Test t)
    {
        if (Tests.isBlank(t))
        {
            throw new IllegalStateException("Internal error: can't find equality test in blank test");
        }
        
        EqualityTest eq = t.asEqualityTest();
        if (eq != null)
        {
            return (EqualityTest) eq.copy();
        }
        
        ConjunctiveTest ct = t.asConjunctiveTest();
        if (ct != null)
        {
            for (Test child : ct.conjunct_list)
            {
                if (!Tests.isBlank(child) && child.asEqualityTest() != null)
                {
                    return (EqualityTest) child.copy();
                }
            }
        }

        throw new IllegalStateException("Internal error: can't find equality test in test\n");
    }

    /**
     * Destructively modifies the first test (t) by adding the second one
     * (add_me) to it (usually as a new conjunct). The first test need not be a
     * conjunctive test.
     * 
     * <p>production.cpp:338:add_new_test_to_test
     * 
     * @param t
     * @param add_me
     * @return the new resulting test
     */
    public static Test add_new_test_to_test(Test t, Test add_me)
    {
        if (Tests.isBlank(add_me))
        {
            return t;
        }

        if (Tests.isBlank(t))
        {
            return add_me;
        }

        // if *t isn't already a conjunctive test, make it into one
        ConjunctiveTest ct = t.asConjunctiveTest();
        if (ct == null)
        {
            ct = new ConjunctiveTest();
            ct.conjunct_list.add(0, t); //.push(t);
        }
        // at this point, ct points to the complex test structure for *t

        // now add add_me to the conjunct list
        ct.conjunct_list.add(0, add_me); //.push(add_me);
        return ct;
    }

    public static boolean test_includes_equality_test_for_symbol(Test test,
            SymbolImpl sym)
    {
        if(Tests.isBlank(test)) { return false; }
        EqualityTest eq = test.asEqualityTest();
        if(eq != null)
        {
            if(sym != null) return eq.getReferent() == sym;
            return true;
        }
        
        ConjunctiveTest ct = test.asConjunctiveTest();
        if (ct != null) {
            for(Test child : ct.conjunct_list)
            {
                if(test_includes_equality_test_for_symbol(child, sym))
                {
                    return true;
                }
            }
        }
        return false;
    }
    
    public static boolean test_includes_goal_or_impasse_id_test(Test t,
            boolean look_for_goal, boolean look_for_impasse)
    {

        if (t.asEqualityTest() != null)
        {
            return false;
        }
        if (look_for_goal && t.asGoalIdTest() != null)
        {
            return true;
        }
        if (look_for_impasse && t.asImpasseIdTest() != null)
        {
            return true;
        }
        ConjunctiveTest ct = t.asConjunctiveTest();
        if (ct != null)
        {
            for (Test c : ct.conjunct_list)
            {
                if (test_includes_goal_or_impasse_id_test(c, look_for_goal, look_for_impasse))
                {
                    return true;
                }
            }
            return false;
        }
        return false;
    }

    /**
     * <p>reorder.cpp:999test_tests_for_root:
     * 
     * @param t the test
     * @param roots list of roots
     * @return true if the test tests for a root in {@code roots}
     */
    public static boolean test_tests_for_root(Test t, ListHead<Variable> roots)
    {
        if (Tests.isBlank(t))
        {
            return false;
        }

        EqualityTest eq = t.asEqualityTest();
        if (eq != null)
        {
            Variable referent = eq.getReferent().asVariable();
            return referent != null && roots.contains(referent);
        }

        ConjunctiveTest ct = t.asConjunctiveTest();
        if (ct != null)
        {
            for (Test c : ct.conjunct_list)
            {
                if (test_tests_for_root(c, roots))
                {
                    return true;
                }
            }
            return false;
        }

        RelationalTest rt = t.asRelationalTest();
        if (rt != null)
        {
            Variable referent = rt.referent.asVariable();
            return referent != null && roots.contains(referent);
        }

        return false;
    }

    /**
     * Same as copy_test(), only it doesn't include goal or impasse tests
     * in the new copy.  The caller should initialize the two flags to FALSE
     * before calling this routine; it sets them to TRUE if it finds a goal
     * or impasse test.
     * 
     * <p>production.cpp:231:copy_test_removing_goal_impasse_tests
     * 
     * @param t Test to search
     * @param removed_goal set to true if a goal test is removed
     * @param removed_impasse set to true if an impasse test is removed
     * @return Copy of the test
     */
    public static Test copy_test_removing_goal_impasse_tests(Test t,
            ByRef<Boolean> removed_goal, ByRef<Boolean> removed_impasse)
    {

        if (Tests.isBlank(t) || t.asEqualityTest() != null)
        {
            return t.copy();
        }

        if (t.asGoalIdTest() != null)
        {
            removed_goal.value = true;
            return null; // blank test
        }
        if (t.asImpasseIdTest() != null)
        {
            removed_impasse.value = true;
            return null; // blank test
        }
        ConjunctiveTest ct = t.asConjunctiveTest();
        if (ct != null)
        {
            Test new_t = null;
            for (Test c : ct.conjunct_list)
            {
                Test temp = copy_test_removing_goal_impasse_tests(c, removed_goal, removed_impasse);
                if (!Tests.isBlank(temp))
                {
                    new_t = add_new_test_to_test(new_t, temp);
                }
            }
            // TODO I don't think reverse is correct here.
            ConjunctiveTest newct = new_t.asConjunctiveTest();
            if (newct != null)
            {
                Collections.reverse(newct.conjunct_list);
            }
            return new_t;
        }
        return t.copy();
    }


    public static char first_letter_from_test (Test t) {
        
        if(Tests.isBlank(t)) { return '*'; }
        EqualityTest eq = t.asEqualityTest();
        if (eq != null) {
          return eq.getReferent().getFirstLetter();
        }

        if(t.asGoalIdTest() != null)
        {
            return 's';
        }
        if(t.asImpasseIdTest() != null)
        {
            return 'i';
        }
        ConjunctiveTest ct = t.asConjunctiveTest();
        if(ct != null)
        {
            for(Test child : ct.conjunct_list)
            {
                char ch = first_letter_from_test(child);
                if(ch != '*')
                {
                    return ch;
                }
            }
        }
        /* disjunction tests, and relational tests other than equality */
        return '*';
      }

    /**
     * Same as add_new_test_to_test(), only has no effect if the second
     * test is already included in the first one.
     * 
     * <p>production.cpp:384:add_new_test_to_test_if_not_already_there
     * 
     * @param t test to add new test to
     * @param add_me the test to add
     * @param neg if part of a negative condition
     * @return resulting test, possibly t, but not necessarily.
     */
    public static Test add_new_test_to_test_if_not_already_there(Test t, Test add_me, boolean neg)
    {
        if (tests_are_equal (t, add_me, neg)) {
          //deallocate_test (thisAgent, add_me);
          return t;
        }

        ConjunctiveTest ct = t.asConjunctiveTest();
        if(ct != null)
        {
            for(Test child : ct.conjunct_list)
            {
                if(tests_are_equal(child, add_me, neg))
                {
                    // deallocate_test (thisAgent, add_me);
                    return t;
                }
            }
        }

        return add_new_test_to_test (t, add_me);
    }
    
    /**
     * This routine pushes bindings for variables occurring (i.e., being
     * equality-tested) in a given test.  It can do this in DENSE fashion
     * (push a new binding for ANY variable) or SPARSE fashion (push a new
     * binding only for previously-unbound variables), depending on the
     * boolean "dense" parameter.  Any variables receiving new bindings
     * are also pushed onto the given "varlist".
     * 
     * <p>rete.cpp:2394:bind_variables_in_test
     * 
     * @param t
     * @param depth
     * @param field_num
     * @param dense
     * @param varlist
     */
    public static void bind_variables_in_test(Test t, int depth, int field_num, boolean dense, ListHead<Variable> varlist)
    {
        if (Tests.isBlank(t))
        {
            return;
        }
        final EqualityTest eq = t.asEqualityTest();
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

        final ConjunctiveTest ct = t.asConjunctiveTest();
        if (ct != null)
        {
            for (Test c : ct.conjunct_list)
            {
                bind_variables_in_test(c, depth, field_num, dense, varlist);
            }
        }
    }

    /**
     * production.cpp:412:tests_are_equal
     * 
     * @param t1 first test to compare
     * @param t2 second test to compare
     * @param neg if tests are part of a negation
     * @return true if the two tests are equal
     */
    static boolean tests_are_equal(Test t1, Test t2, boolean neg)
    {
        if(t1 == t2)
        {
            return true;
        }
        if(t1.getClass() != t2.getClass())
        {
            return false;
        }
        
        EqualityTest eq1 = t1.asEqualityTest();
        if(eq1 != null)
        {
        	if (neg)
        	{
        		// ignore variables in negation tests, bug 510
        		if (eq1.getReferent().asVariable() != null)
        		{
        			EqualityTest eq2 = t2.asEqualityTest();
        			if (eq2 != null && eq2.getReferent().asVariable() != null)
        			{
        				return true;
        			}
        		}
        	}
            return eq1.getReferent() == t2.asEqualityTest().getReferent();
        }

        if(t1.asGoalIdTest() != null || t1.asImpasseIdTest() != null)
        {
            return true;
        }
        DisjunctionTest dt = t1.asDisjunctionTest();
        if(dt != null)
        {
            return dt.disjunction_list.equals(t2.asDisjunctionTest().disjunction_list);
        }

        ConjunctiveTest ct = t1.asConjunctiveTest();
        if(ct != null)
        {
            List<Test> c1 = ct.conjunct_list;
            List<Test> c2 = t2.asConjunctiveTest().conjunct_list;
            if(c1.size() != c2.size())
            {
                return false;
            }
            // ignore order of test members in conjunctions, bug 510
            // TODO: consider reorder before comparing
            final List<Test> copy2 = Lists.newLinkedList(c2);
            for(int i = 0; i < c1.size(); ++i)
            {
            	for(int j = 0; j < copy2.size(); ++j)
            	{
            		if (tests_are_equal(c1.get(i), copy2.get(j), neg))
            		{
            			copy2.remove(j);
            			break;
            		}
            	}
            }
            return copy2.size() == 0;
        }

        return t1.asRelationalTest().referent == t2.asRelationalTest().referent;
    }

    /**
     * 
     * <p>TODO Make add_test_to_tc polymorphic on Test
     * <p>production.cpp:1308:add_test_to_tc
     * 
     * @param t
     * @param tc
     * @param id_list
     * @param var_list
     */
    static void add_test_to_tc(Test t, Marker tc, ListHead<IdentifierImpl> id_list, ListHead<Variable> var_list)
    {
        if (Tests.isBlank(t))
            return;
        EqualityTest eq = t.asEqualityTest();
        if (eq != null)
        {
            eq.getReferent().add_symbol_to_tc(tc, id_list, var_list);
            return;
        }

        ConjunctiveTest ct = t.asConjunctiveTest();
        if (ct != null)
        {
            for (Test c : ct.conjunct_list)
                add_test_to_tc(c, tc, id_list, var_list);
        }
    }

    /**
     * <p>production.cpp:1354:test_is_in_tc
     * 
     * @param t the test
     * @param tc the TC marker
     * @return true if {@code t} is in the transitive closure
     */
    static boolean test_is_in_tc(Test t, Marker tc)
    {
        if (Tests.isBlank(t))
            return false;
        EqualityTest eq = t.asEqualityTest();
        if (eq != null)
        {
            return eq.getReferent().symbol_is_in_tc(tc);
        }

        ConjunctiveTest ct = t.asConjunctiveTest();
        if (ct != null)
        {
            for (Test c : ct.conjunct_list)
                if (test_is_in_tc(c, tc))
                    return true;
            return false;
        }
        return false;
    }

    /**
     * Returns a hash value for the given test.
     * 
     * <p>TODO make this polymorphic
     * <p>production.cpp:450:hash_test
     * 
     * @param t The test to hash
     * @return The has value for the test
     */
    public static int hash_test(Test t)
    {
        if (Tests.isBlank(t))
            return 0;
    
        EqualityTest eq = t.asEqualityTest();
        if (eq != null)
            return eq.getReferent().hash_id;
    
        if (t.asGoalIdTest() != null)
        {
            return 34894895; /* just use some unusual number */
        }
        if (t.asImpasseIdTest() != null)
        {
            return 2089521;
        }
        DisjunctionTest dt = t.asDisjunctionTest();
        if (dt != null)
        {
            int result = 7245;
            for (SymbolImpl c : dt.disjunction_list)
                result = result + c.hash_id;
            return result;
        }
        ConjunctiveTest ct = t.asConjunctiveTest();
        if (ct != null)
        {
            int result = 100276;
			// bug 510: conjunctive tests' order needs to be ignored
            //for (Test c : ct.conjunct_list)
            //    result = result + hash_test(c);
            return result;
        }
        RelationalTest rt = t.asRelationalTest();
        if (rt != null)
        {
            return (rt.type << 24) + rt.referent.hash_id;
        }
        throw new IllegalStateException("Error: bad test type in hash_test: " + t);
    }

    /**
     * Copy a test, safely handling the case of null tests.
     * 
     * <p>production.cpp:187:copy_test
     * 
     * @param t A test, possibly <code>null</code>
     * @return Copy of t
     * @see Test#copy()
     */
    public static Test copy(Test t)
    {
        return t != null ? t.copy() : null;
    }

    /**
     * <p>production.cpp::test_is_blank_test
     * 
     * @param t the test
     * @return true if the test is blank, i.e. {@code null}
     */
    public static boolean isBlank(Test t)
    {
        return t == null;
    }

}
