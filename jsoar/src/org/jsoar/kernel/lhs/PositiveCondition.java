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
public class PositiveCondition extends ThreeFieldCondition
{
    
    public PositiveCondition()
    {
        
    }
    
    public PositiveCondition(NegativeCondition negativeCondition)
    {
        // TODO Auto-generated constructor stub
    }

    public NegativeCondition negate()
    {
        return new NegativeCondition(this);
    }
    
    /* (non-Javadoc)
     * @see org.jsoar.kernel.Condition#asPositiveCondition()
     */
    @Override
    public PositiveCondition asPositiveCondition()
    {
        return this;
    }

    /* (non-Javadoc)
     * @see org.jsoar.kernel.Condition#addBoundVariables(int, java.util.List)
     */
    @Override
    public void addBoundVariables(int tc_number, List<Variable> var_list)
    {
        id_test.addBoundVariables(tc_number, var_list);
        attr_test.addBoundVariables(tc_number, var_list);
        value_test.addBoundVariables(tc_number, var_list);
    }
    
    
}
