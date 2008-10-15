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
public class IntConstant extends SymbolImpl implements IntegerSymbol
{
    private final int value;
    
    /**
     * @param hash_id
     */
    /*package*/ IntConstant(int hash_id, int value)
    {
        super(hash_id);
        this.value = value;
    }

    
    /**
     * @return the value
     */
    public int getValue()
    {
        return value;
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
     * @see org.jsoar.kernel.symbols.SymbolImpl#isSameTypeAs(org.jsoar.kernel.symbols.SymbolImpl)
     */
    @Override
    public boolean isSameTypeAs(SymbolImpl other)
    {
        return other.asIntConstant() != null;
    }


    /* (non-Javadoc)
     * @see org.jsoar.kernel.symbols.SymbolImpl#numericLess(org.jsoar.kernel.symbols.SymbolImpl)
     */
    @Override
    public boolean numericLess(SymbolImpl other)
    {
        IntConstant i = other.asIntConstant();
        if(i != null)
        {
            return getValue() < i.getValue();
        }
        FloatConstant f = other.asFloatConstant();
        
        return f != null ? getValue() < f.getValue() : super.numericLess(other);
    }

    /* (non-Javadoc)
     * @see org.jsoar.kernel.symbols.SymbolImpl#numericLessOrEqual(org.jsoar.kernel.symbols.SymbolImpl)
     */
    @Override
    public boolean numericLessOrEqual(SymbolImpl other)
    {
        IntConstant i = other.asIntConstant();
        if(i != null)
        {
            return getValue() <= i.getValue();
        }
        FloatConstant f = other.asFloatConstant();
        
        return f != null ? getValue() <= f.getValue() : super.numericLess(other);
    }

    /* (non-Javadoc)
     * @see org.jsoar.kernel.symbols.SymbolImpl#numericGreater(org.jsoar.kernel.symbols.SymbolImpl)
     */
    @Override
    public boolean numericGreater(SymbolImpl other)
    {
        IntConstant i = other.asIntConstant();
        if(i != null)
        {
            return getValue() > i.getValue();
        }
        FloatConstant f = other.asFloatConstant();
        
        return f != null ? getValue() > f.getValue() : super.numericLess(other);
    }

    /* (non-Javadoc)
     * @see org.jsoar.kernel.symbols.SymbolImpl#numericGreaterOrEqual(org.jsoar.kernel.symbols.SymbolImpl)
     */
    @Override
    public boolean numericGreaterOrEqual(SymbolImpl other)
    {
        IntConstant i = other.asIntConstant();
        if(i != null)
        {
            return getValue() >= i.getValue();
        }
        FloatConstant f = other.asFloatConstant();
        
        return f != null ? getValue() >= f.getValue() : super.numericLess(other);
    }
    
    /* (non-Javadoc)
     * @see java.lang.Object#toString()
     */
    @Override
    public String toString()
    {
        return Integer.toString(getValue());
    }


    /* (non-Javadoc)
     * @see java.util.Formattable#formatTo(java.util.Formatter, int, int, int)
     */
    @Override
    public void formatTo(Formatter formatter, int flags, int width, int precision)
    {
        formatter.format(Integer.toString(getValue()));
    }
    
    
}
