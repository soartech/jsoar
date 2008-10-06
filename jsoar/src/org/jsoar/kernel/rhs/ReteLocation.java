/*
 * (c) 2008  Dave Ray
 *
 * Created on Aug 18, 2008
 */
package org.jsoar.kernel.rhs;

import java.util.Formatter;

import org.jsoar.kernel.symbols.Variable;
import org.jsoar.util.ListHead;

/**
 * @author ray
 */
public class ReteLocation extends RhsValue
{
    private int fieldNum;
    private int levelsUp;
    
    /**
     * @param fieldNum
     * @param levelsUp
     */
    public ReteLocation(int fieldNum, int levelsUp)
    {
        this.fieldNum = fieldNum;
        this.levelsUp = levelsUp;
    }
    /**
     * @return The rete field number
     */
    public int getFieldNum()
    {
        return fieldNum;
    }
    /**
     * @return Number of rete levels up
     */
    public int getLevelsUp()
    {
        return levelsUp;
    }
    
    /* (non-Javadoc)
     * @see org.jsoar.kernel.RhsValue#asReteLocation()
     */
    @Override
    public ReteLocation asReteLocation()
    {
        return this;
    }
    
    /* (non-Javadoc)
     * @see org.jsoar.kernel.RhsValue#addAllVariables(int, java.util.List)
     */
    @Override
    public void addAllVariables(int tc_number, ListHead<Variable> var_list)
    {
        // See comment in RhsValue.addAllVariables.
        throw new UnsupportedOperationException("addAllVariables not supported on ReteLocation RhsValue");
    }
    
    /* (non-Javadoc)
     * @see java.lang.Object#toString()
     */
    @Override
    public String toString()
    {
        // For debugging only
        return "(rete-location " + levelsUp + ":" + fieldNum + ")";
    }
    
    /* (non-Javadoc)
     * @see java.util.Formattable#formatTo(java.util.Formatter, int, int, int)
     */
    @Override
    public void formatTo(Formatter formatter, int arg1, int arg2, int arg3)
    {
        throw new IllegalStateException("Internal error: rhs_value_to_string called on reteloc.");
    }
    
    
}
