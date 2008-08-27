/*
 * (c) 2008  Dave Ray
 *
 * Created on Aug 16, 2008
 */
package org.jsoar.kernel.lhs;

import java.util.List;

import org.jsoar.kernel.symbols.Variable;

/**
 * @author ray
 */
public abstract class Test
{
    public abstract Test copy();
    
    public boolean isBlank()
    {
        return false;
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

    public void addAllVariables(int tc_number, List<Variable> var_list)
    {
        // Do nothing by default
    }

    public void addBoundVariables(int tc_number, List<Variable> var_list)
    {
        // Do nothing by default
    }
}
