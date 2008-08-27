/*
 * (c) 2008  Dave Ray
 *
 * Created on Aug 15, 2008
 */
package org.jsoar.kernel.symbols;

import org.jsoar.kernel.DeciderFlag;
import org.jsoar.kernel.Wme;

/**
 * @author ray
 */
public abstract class Symbol
{
    public DeciderFlag decider_flag;
    public Wme decider_wme;
    public int retesave_symindex;
    public int hash_id;
    
    public FloatConstant asFloatConstant()
    {
        return null;
    }
    
    public IntConstant asIntConstant()
    {
        return null;
    }
    
    public SymConstant asSymConstant()
    {
        return null;
    }
    
    public Variable asVariable()
    {
        return null;
    }
    
    public Identifier asIdentifier()
    {
        return null;
    }
    
    public char getFirstLetter()
    {
        return '*';
    }
    
    /* (non-Javadoc)
     * @see java.lang.Object#equals(java.lang.Object)
     */
    @Override
    public boolean equals(Object obj)
    {
        return super.equals(obj);
    }
    
    /* (non-Javadoc)
     * @see java.lang.Object#hashCode()
     */
    @Override
    public int hashCode()
    {
        return super.hashCode();
    }
    
}
