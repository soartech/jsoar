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
     * @return the fieldNum
     */
    public int getFieldNum()
    {
        return fieldNum;
    }
    /**
     * @return the levelsUp
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
        // For debugging only
        return "(rete-location " + levelsUp + ":" + fieldNum + ")";
    }
    
    
}
