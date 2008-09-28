/*
 * (c) 2008  Dave Ray
 *
 * Created on Aug 16, 2008
 */
package org.jsoar.kernel.lhs;

import java.util.LinkedList;

import org.jsoar.kernel.symbols.Symbol;
import org.jsoar.kernel.symbols.Variable;

/**
 * @author ray
 */
public abstract class Test
{
    /**
     * 
     * <p>Polymorphized version of copy_test()
     * 
     * <p>production.cpp:187:copy_test
     * 
     * @return
     */
    public abstract Test copy();
    
    /**
     * Copy a test, safely handling the case of null tests.
     * 
     * <p>production.cpp:187:copy_test
     * 
     * @see {@link #copy()}
     * @param t A test, possibly <code>null</code>
     * @return Copy of t
     */
    public static Test copy(Test t)
    {
        return t != null ? t.copy() : null;
    }
    
    public static boolean isBlank(Test t)
    {
        return t == null;
    }
    
    public EqualityTest asEqualityTest()
    {
        return null;
    }
    
    public ComplexTest asComplexTest()
    {
        return null;
    }

    public ConjunctiveTest asConjunctiveTest()
    {
        return null;
    }

    public GoalIdTest asGoalIdTest()
    {
        return null;
    }

    public ImpasseIdTest asImpasseIdTest()
    {
        return null;
    }

    public DisjunctionTest asDisjunctionTest()
    {
        return null;
    }

    public RelationalTest asRelationalTest()
    {
        return null;
    }

    public void addAllVariables(int tc_number, LinkedList<Variable> var_list)
    {
        // Do nothing by default
    }

    /**
     * production.cpp:1113:add_bound_variables_in_test
     * 
     * @param tc_number
     * @param var_list
     */
    public void addBoundVariables(int tc_number, LinkedList<Variable> var_list)
    {
        // Do nothing by default
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
        if (isBlank(t))
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
            for (Symbol c : dt.disjunction_list)
                result = result + c.hash_id;
            return result;
        }
        ConjunctiveTest ct = t.asConjunctiveTest();
        if (ct != null)
        {
            int result = 100276;
            for (Test c : ct.conjunct_list)
                result = result + hash_test(c);
            return result;
        }
        RelationalTest rt = t.asRelationalTest();
        if (rt != null)
        {
            return (rt.type << 24) + rt.referent.hash_id;
        }
        throw new IllegalStateException("Error: bad test type in hash_test: " + t);
    }
}

