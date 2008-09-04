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
    public final int value;
    
    /**
     * @param hash_id
     */
    /*package*/ IntConstant(int hash_id, int value)
    {
        super(hash_id);
        this.value = value;
    }

    
    /* (non-Javadoc)
     * @see org.jsoar.kernel.Symbol#asIntConstant()
     */
    @Override
    public IntConstant asIntConstant()
    {
        return this;
    }


    /* (non-Javadoc)
     * @see java.lang.Object#toString()
     */
    @Override
    public String toString()
    {
        return Integer.toString(value);
    }
    
    
}
