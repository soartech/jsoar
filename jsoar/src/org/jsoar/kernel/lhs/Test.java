/*
 * (c) 2008  Dave Ray
 *
 * Created on Aug 16, 2008
 */
package org.jsoar.kernel.lhs;

import java.util.Formattable;

import org.jsoar.kernel.symbols.Variable;
import org.jsoar.util.ListHead;

/**
 * @author ray
 */
public abstract class Test implements Formattable
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

    public void addAllVariables(int tc_number, ListHead<Variable> var_list)
    {
        // Do nothing by default
    }

    /**
     * production.cpp:1113:add_bound_variables_in_test
     * 
     * @param tc_number
     * @param var_list
     */
    public void addBoundVariables(int tc_number, ListHead<Variable> var_list)
    {
        // Do nothing by default
    }
}

