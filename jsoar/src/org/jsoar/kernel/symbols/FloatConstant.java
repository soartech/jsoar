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
public class FloatConstant extends SymbolImpl implements DoubleSymbol
{
    private final double value;

    /**
     * @param hash_id
     */
    /*package*/ FloatConstant(int hash_id, double value)
    {
        super(hash_id);
        
        this.value = value;
    }

    public double getValue()
    {
        return value;
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
     * @see org.jsoar.kernel.symbols.SymbolImpl#isSameTypeAs(org.jsoar.kernel.symbols.SymbolImpl)
     */
    @Override
    public boolean isSameTypeAs(SymbolImpl other)
    {
        return other.asFloatConstant() != null;
    }
    
    /* (non-Javadoc)
     * @see org.jsoar.kernel.symbols.SymbolImpl#numericLess(org.jsoar.kernel.symbols.SymbolImpl)
     */
    @Override
    public boolean numericLess(SymbolImpl other)
    {
        FloatConstant f = other.asFloatConstant();
        if(f != null)
        {
            return value < f.value;
        }
        IntConstant i = other.asIntConstant();
        
        return i != null ? value < i.getValue() : super.numericLess(other);
    }

    /* (non-Javadoc)
     * @see org.jsoar.kernel.symbols.SymbolImpl#numericLessOrEqual(org.jsoar.kernel.symbols.SymbolImpl)
     */
    @Override
    public boolean numericLessOrEqual(SymbolImpl other)
    {
        FloatConstant f = other.asFloatConstant();
        if(f != null)
        {
            return value <= f.value;
        }
        IntConstant i = other.asIntConstant();
        
        return i != null ? value <= i.getValue() : super.numericLessOrEqual(other);
    }
    
    /* (non-Javadoc)
     * @see org.jsoar.kernel.symbols.SymbolImpl#numericGreater(org.jsoar.kernel.symbols.SymbolImpl)
     */
    @Override
    public boolean numericGreater(SymbolImpl other)
    {
        FloatConstant f = other.asFloatConstant();
        if(f != null)
        {
            return value > f.value;
        }
        IntConstant i = other.asIntConstant();
        
        return i != null ? value > i.getValue() : super.numericLess(other);
    }

    /* (non-Javadoc)
     * @see org.jsoar.kernel.symbols.SymbolImpl#numericGreaterOrEqual(org.jsoar.kernel.symbols.SymbolImpl)
     */
    @Override
    public boolean numericGreaterOrEqual(SymbolImpl other)
    {
        FloatConstant f = other.asFloatConstant();
        if(f != null)
        {
            return value >= f.value;
        }
        IntConstant i = other.asIntConstant();
        
        return i != null ? value >= i.getValue() : super.numericLessOrEqual(other);
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
