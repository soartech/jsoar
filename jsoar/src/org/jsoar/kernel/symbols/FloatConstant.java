/*
 * (c) 2008  Dave Ray
 *
 * Created on Aug 15, 2008
 */
package org.jsoar.kernel.symbols;

/**
 * @author ray
 */
public class FloatConstant extends Symbol
{
    public final double value;

    /**
     * @param hash_id
     */
    /*package*/ FloatConstant(int hash_id, double value)
    {
        super(hash_id);
        
        this.value = value;
    }

    /* (non-Javadoc)
     * @see org.jsoar.kernel.Symbol#asFloatConstant()
     */
    @Override
    public FloatConstant asFloatConstant()
    {
        return this;
    }

    /* (non-Javadoc)
     * @see java.lang.Object#toString()
     */
    @Override
    public String toString()
    {
        return Double.toString(value);
    }
    
    
}
