/*
 * (c) 2008  Dave Ray
 *
 * Created on Aug 15, 2008
 */
package org.jsoar.kernel.symbols;

import org.jsoar.kernel.Production;

/**
 * @author ray
 */
public class SymConstant extends Symbol
{
    public final String name;
    public Production production;
    
    
    /**
     * @param hash_id
     */
    /*package*/ SymConstant(int hash_id, String name)
    {
        super(hash_id);
        this.name = name;
    }

    /* (non-Javadoc)
     * @see org.jsoar.kernel.Symbol#asSymConstant()
     */
    @Override
    public SymConstant asSymConstant()
    {
        return this;
    }
    
    
    /* (non-Javadoc)
     * @see org.jsoar.kernel.symbols.Symbol#isSameTypeAs(org.jsoar.kernel.symbols.Symbol)
     */
    @Override
    public boolean isSameTypeAs(Symbol other)
    {
        return other.asSymConstant() != null;
    }

    /* (non-Javadoc)
     * @see org.jsoar.kernel.symbols.Symbol#getFirstLetter()
     */
    @Override
    public char getFirstLetter()
    {
        return name.charAt(0);
    }

    /* (non-Javadoc)
     * @see java.lang.Object#toString()
     */
    @Override
    public String toString()
    {
        return name;
    }
    
    
}
