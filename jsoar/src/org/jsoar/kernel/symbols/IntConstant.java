/*
 * (c) 2008  Dave Ray
 *
 * Created on Aug 15, 2008
 */
package org.jsoar.kernel.symbols;

/**
 * @author ray
 */
public class IntConstant extends Symbol
{
    public int value;

    
    /* (non-Javadoc)
     * @see org.jsoar.kernel.Symbol#asIntConstant()
     */
    @Override
    public IntConstant asIntConstant()
    {
        return this;
    }

    /* (non-Javadoc)
     * @see java.lang.Object#hashCode()
     */
    @Override
    public int hashCode()
    {
        final int prime = 31;
        int result = super.hashCode();
        result = prime * result + value;
        return result;
    }

    /* (non-Javadoc)
     * @see java.lang.Object#equals(java.lang.Object)
     */
    @Override
    public boolean equals(Object obj)
    {
        if (this == obj)
            return true;
        if (!super.equals(obj))
            return false;
        if (getClass() != obj.getClass())
            return false;
        final IntConstant other = (IntConstant) obj;
        if (value != other.value)
            return false;
        return true;
    }
    
    
}
