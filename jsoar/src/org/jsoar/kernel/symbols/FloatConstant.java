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
     * @see org.jsoar.kernel.symbols.Symbol#isSameTypeAs(org.jsoar.kernel.symbols.Symbol)
     */
    @Override
    public boolean isSameTypeAs(Symbol other)
    {
        return other.asFloatConstant() != null;
    }
    
    /* (non-Javadoc)
     * @see org.jsoar.kernel.symbols.Symbol#numericLess(org.jsoar.kernel.symbols.Symbol)
     */
    @Override
    public boolean numericLess(Symbol other)
    {
        FloatConstant f = other.asFloatConstant();
        if(f != null)
        {
            return value < f.value;
        }
        IntConstant i = other.asIntConstant();
        
        return i != null ? value < i.value : super.numericLess(other);
    }

    /* (non-Javadoc)
     * @see org.jsoar.kernel.symbols.Symbol#numericLessOrEqual(org.jsoar.kernel.symbols.Symbol)
     */
    @Override
    public boolean numericLessOrEqual(Symbol other)
    {
        FloatConstant f = other.asFloatConstant();
        if(f != null)
        {
            return value <= f.value;
        }
        IntConstant i = other.asIntConstant();
        
        return i != null ? value <= i.value : super.numericLessOrEqual(other);
    }
    
    /* (non-Javadoc)
     * @see org.jsoar.kernel.symbols.Symbol#numericGreater(org.jsoar.kernel.symbols.Symbol)
     */
    @Override
    public boolean numericGreater(Symbol other)
    {
        FloatConstant f = other.asFloatConstant();
        if(f != null)
        {
            return value > f.value;
        }
        IntConstant i = other.asIntConstant();
        
        return i != null ? value > i.value : super.numericLess(other);
    }

    /* (non-Javadoc)
     * @see org.jsoar.kernel.symbols.Symbol#numericGreaterOrEqual(org.jsoar.kernel.symbols.Symbol)
     */
    @Override
    public boolean numericGreaterOrEqual(Symbol other)
    {
        FloatConstant f = other.asFloatConstant();
        if(f != null)
        {
            return value >= f.value;
        }
        IntConstant i = other.asIntConstant();
        
        return i != null ? value >= i.value : super.numericLessOrEqual(other);
    }
    
    /* (non-Javadoc)
     * @see java.lang.Object#toString()
     */
    @Override
    public String toString()
    {
        return Double.toString(value);
    }

    /* (non-Javadoc)
     * @see java.util.Formattable#formatTo(java.util.Formatter, int, int, int)
     */
    @Override
    public void formatTo(Formatter formatter, int flags, int width, int precision)
    {
        formatter.format(Double.toString(value));
    }
}
