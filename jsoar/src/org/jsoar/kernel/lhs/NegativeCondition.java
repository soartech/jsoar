/*
 * (c) 2008  Dave Ray
 *
 * Created on Aug 16, 2008
 */
package org.jsoar.kernel.lhs;

import org.jsoar.kernel.symbols.IdentifierImpl;
import org.jsoar.kernel.symbols.Variable;
import org.jsoar.util.ListHead;

/**
 * @author ray
 */
public class NegativeCondition extends ThreeFieldCondition
{
    public NegativeCondition()
    {
        
    }
    
    public NegativeCondition(PositiveCondition positiveCondition)
    {
        super(positiveCondition);
    }

    public PositiveCondition negate()
    {
        return new PositiveCondition(this);
    }
    
    /* (non-Javadoc)
     * @see org.jsoar.kernel.Condition#asNegativeCondition()
     */
    public NegativeCondition asNegativeCondition()
    {
        return this;
    }

    /* (non-Javadoc)
     * @see org.jsoar.kernel.Condition#addBoundVariables(int, java.util.List)
     */
    @Override
    public void addBoundVariables(int tc_number, ListHead<Variable> var_list)
    {
        // Do nothing
    }

    /* (non-Javadoc)
     * @see org.jsoar.kernel.lhs.Condition#add_cond_to_tc(int, java.util.LinkedList, java.util.LinkedList)
     */
    @Override
    public void add_cond_to_tc(int tc, ListHead<IdentifierImpl> id_list, ListHead<Variable> var_list)
    {
        // Do nothing
    }
    
    /* (non-Javadoc)
     * @see java.lang.Object#toString()
     */
    @Override
    public String toString()
    {
        // For debugging only
        return "-(" + super.toString() + ")";
    }
    
    

}
