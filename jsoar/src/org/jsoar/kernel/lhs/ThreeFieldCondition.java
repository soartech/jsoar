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
public abstract class ThreeFieldCondition extends Condition
{
    public Test id_test;
    public Test attr_test;
    public Test value_test;
    
    /* (non-Javadoc)
     * @see org.jsoar.kernel.Condition#asThreeFieldCondition()
     */
    @Override
    public ThreeFieldCondition asThreeFieldCondition()
    {
        return this;
    }

    /* (non-Javadoc)
     * @see org.jsoar.kernel.Condition#addAllVariables(int, java.util.List)
     */
    @Override
    public void addAllVariables(int tc_number, List<Variable> var_list)
    {
        id_test.addAllVariables(tc_number, var_list);
        attr_test.addAllVariables(tc_number, var_list);
        value_test.addAllVariables(tc_number, var_list);
    }
}
