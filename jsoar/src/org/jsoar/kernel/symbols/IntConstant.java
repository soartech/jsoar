/*
 * (c) 2008  Dave Ray
 *
 * Created on Aug 15, 2008
 */
package org.jsoar.kernel.symbols;

import java.util.Formatter;

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
     * @see org.jsoar.kernel.symbols.Symbol#isSameTypeAs(org.jsoar.kernel.symbols.Symbol)
     */
    @Override
    public boolean isSameTypeAs(Symbol other)
    {
        return other.asIntConstant() != null;
    }


    /* (non-Javadoc)
     * @see org.jsoar.kernel.symbols.Symbol#numericLess(org.jsoar.kernel.symbols.Symbol)
     */
    @Override
    public boolean numericLess(Symbol other)
    {
        IntConstant i = other.asIntConstant();
        if(i != null)
        {
            return value < i.value;
        }
        FloatConstant f = other.asFloatConstant();
        
        return f != null ? value < f.value : super.numericLess(other);
    }

    /* (non-Javadoc)
     * @see org.jsoar.kernel.symbols.Symbol#numericLessOrEqual(org.jsoar.kernel.symbols.Symbol)
     */
    @Override
    public boolean numericLessOrEqual(Symbol other)
    {
        IntConstant i = other.asIntConstant();
        if(i != null)
        {
            return value <= i.value;
        }
        FloatConstant f = other.asFloatConstant();
        
        return f != null ? value <= f.value : super.numericLess(other);
    }

    /* (non-Javadoc)
     * @see org.jsoar.kernel.symbols.Symbol#numericGreater(org.jsoar.kernel.symbols.Symbol)
     */
    @Override
    public boolean numericGreater(Symbol other)
    {
        IntConstant i = other.asIntConstant();
        if(i != null)
        {
            return value > i.value;
        }
        FloatConstant f = other.asFloatConstant();
        
        return f != null ? value > f.value : super.numericLess(other);
    }

    /* (non-Javadoc)
     * @see org.jsoar.kernel.symbols.Symbol#numericGreaterOrEqual(org.jsoar.kernel.symbols.Symbol)
     */
    @Override
    public boolean numericGreaterOrEqual(Symbol other)
    {
        IntConstant i = other.asIntConstant();
        if(i != null)
        {
            return value >= i.value;
        }
        FloatConstant f = other.asFloatConstant();
        
        return f != null ? value >= f.value : super.numericLess(other);
    }
    
    /* (non-Javadoc)
     * @see java.lang.Object#toString()
     */
    @Override
    public String toString()
    {
        return Integer.toString(value);
    }


    /* (non-Javadoc)
     * @see java.util.Formattable#formatTo(java.util.Formatter, int, int, int)
     */
    @Override
    public void formatTo(Formatter formatter, int flags, int width, int precision)
    {
        formatter.format(Integer.toString(value));
    }
    
    
}
