/*
 * (c) 2008  Dave Ray
 *
 * Created on Aug 16, 2008
 */
package org.jsoar.kernel.lhs;

import java.util.Collections;
import java.util.List;

import org.jsoar.kernel.symbols.Symbol;
import org.jsoar.kernel.symbols.Variable;
import org.jsoar.util.ByRef;

/**
 * @author ray
 */
public class TestTools
{
    public static EqualityTest copy_of_equality_test_found_in_test(Test t)
    {

        if (Test.isBlank(t))
        {
            throw new IllegalArgumentException(
                    "Internal error: can't find equality test in blank test");
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
                if (!Test.isBlank(child) && child.asEqualityTest() != null)
                {
                    return (EqualityTest) child.copy();
                }
            }
        }

        // TODO
        throw new IllegalStateException(
                "Internal error: can't find equality test in test\n");
        // strncpy (msg, "Internal error: can't find equality test in
        // test\n",BUFFER_MSG_SIZE);
        // abort_with_fatal_error(thisAgent, msg);
        // return 0; /* unreachable, but without it, gcc -Wall warns here */
    }

    /**
     * Destructively modifies the first test (t) by adding the second one
     * (add_me) to it (usually as a new conjunct). The first test need not be a
     * conjunctive test.
     * 
     * TODO Rather than the dumb ByRef, just return the new test. Functional!
     * 
     * production.cpp:338:add_new_test_to_test
     * 
     * @param t
     * @param add_me
     */
    public static void add_new_test_to_test(ByRef<Test> t, Test add_me)
    {
        if (Test.isBlank(add_me))
        {
            return;
        }

        if (Test.isBlank(t.value))
        {
            t.value = add_me;
            return;
        }

        /* --- if *t isn't already a conjunctive test, make it into one --- */
        boolean already_a_conjunctive_test = t.value.asConjunctiveTest() != null;

        if (!already_a_conjunctive_test)
        {
            ConjunctiveTest ct = new ConjunctiveTest();
            ct.conjunct_list.push(t.value);
            t.value = ct;
        }
        /* --- at this point, ct points to the complex test structure for *t --- */

        /* --- now add add_me to the conjunct list --- */
        t.value.asConjunctiveTest().conjunct_list.push(add_me);
    }

    public static boolean test_includes_equality_test_for_symbol(Test test,
            Symbol sym)
    {
        if(Test.isBlank(test)) { return false; }
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
                return false;
            }
        }
        return false;
    }

    /**
     * 
     * reorder.cpp:999test_tests_for_root:
     * 
     * @param t
     * @param roots
     * @return
     */
    public static boolean test_tests_for_root(Test t, List<Variable> roots)
    {
        if (Test.isBlank(t))
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
     * production.cpp: 231
     * 
     * @param t
     * @param removed_goal
     * @param removed_impasse
     * @return
     */
    public static Test copy_test_removing_goal_impasse_tests(Test t,
            ByRef<Boolean> removed_goal, ByRef<Boolean> removed_impasse)
    {

        if (Test.isBlank(t) || t.asEqualityTest() != null)
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
            ByRef<Test> new_t = new ByRef<Test>(null);
            for (Test c : ct.conjunct_list)
            {
                Test temp = copy_test_removing_goal_impasse_tests(c,
                        removed_goal, removed_impasse);
                if (!Test.isBlank(temp))
                {
                    add_new_test_to_test(new_t, temp);
                }
            }
            // TODO I don't think reverse is correct here.
            ConjunctiveTest newct = new_t.value.asConjunctiveTest();
            if (newct != null)
            {
                Collections.reverse(newct.conjunct_list);
            }
            return new_t.value;
        }
        return t.copy();
    }


    public static char first_letter_from_test (Test t) {
        
        if(Test.isBlank(t)) { return '*'; }
        EqualityTest eq = t.asEqualityTest();
        if (eq != null) {
          return eq.sym.getFirstLetter();
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
     * production.cpp:384
     * 
     * @param t
     * @param the_test
     */
    public static void add_new_test_to_test_if_not_already_there(ByRef<Test> t,
            Test add_me)
    {
        if (tests_are_equal (t.value, add_me)) {
          //deallocate_test (thisAgent, add_me);
          return;
        }

        ConjunctiveTest ct = t.value.asConjunctiveTest();
        if(ct != null)
        {
            for(Test child : ct.conjunct_list)
            {
                if(tests_are_equal(child, add_me))
                {
                    // deallocate_test (thisAgent, add_me);
                    return;
                }
            }
        }

        add_new_test_to_test (t, add_me);
        
    }

    /**
     * production.cpp: 412
     * 
     * @param value
     * @param add_me
     * @return
     */
    private static boolean tests_are_equal(Test t1, Test t2)
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
        if(eq1 != null && eq1.sym == t2.asEqualityTest().sym)
        {
            return true;
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
            for(int i = 0; i < c1.size(); ++i)
            {
                if(!tests_are_equal(c1.get(i), c2.get(i)))
                {
                    return false;
                }
            }
            return true;
        }

        return t1.asRelationalTest().referent == t2.asRelationalTest().referent;
    }

}
