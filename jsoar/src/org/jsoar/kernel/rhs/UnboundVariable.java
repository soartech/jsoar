/*
 * (c) 2008  Dave Ray
 *
 * Created on Aug 18, 2008
 */
package org.jsoar.kernel.rhs;

import java.util.LinkedList;

import org.jsoar.kernel.symbols.Variable;

/**
 * @author ray
 */
public class UnboundVariable extends RhsValue
{
    private int index;

    /**
     * @param index
     */
    public UnboundVariable(int index)
    {
        this.index = index;
    }

    /**
     * @return the index
     */
    public int getIndex()
    {
        return index;
    }

    /* (non-Javadoc)
     * @see org.jsoar.kernel.RhsValue#asUnboundVariable()
     */
    @Override
    public UnboundVariable asUnboundVariable()
    {
        return this;
    }

    /* (non-Javadoc)
     * @see org.jsoar.kernel.RhsValue#addAllVariables(int, java.util.List)
     */
    @Override
    public void addAllVariables(int tc_number, LinkedList<Variable> var_list)
    {
        // TODO: anything?
    }

    /* (non-Javadoc)
     * @see java.lang.Object#toString()
     */
    @Override
    public String toString()
    {
        return "(unbound-var + " + index + ")";
    }
    
    
    
}
