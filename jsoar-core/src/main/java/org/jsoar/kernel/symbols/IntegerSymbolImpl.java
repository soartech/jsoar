/*
 * (c) 2008  Dave Ray
 *
 * Created on Aug 15, 2008
 */
package org.jsoar.kernel.symbols;

import java.util.Formatter;

/**
 * <em>This is an internal interface. Don't use it unless you know what you're doing.</em>
 * 
 * @author ray
 */
public class IntegerSymbolImpl extends SymbolImpl implements IntegerSymbol
{
    private final long value;
    
    /**
     * @param hash_id
     */
    IntegerSymbolImpl(SymbolFactory factory, int hash_id, long value)
    {
        super(factory, hash_id);
        this.value = value;
    }

    
    /* (non-Javadoc)
     * @see org.jsoar.kernel.symbols.IntegerSymbol#getValue()
     */
    public long getValue()
    {
        return value;
    }


    /* (non-Javadoc)
     * @see org.jsoar.kernel.Symbol#asIntConstant()
     */
    @Override
    public IntegerSymbolImpl asInteger()
    {
        return this;
    }

    /* (non-Javadoc)
     * @see org.jsoar.kernel.symbols.SymbolImpl#importInto(org.jsoar.kernel.symbols.SymbolFactory)
     */
    @Override
    Symbol importInto(SymbolFactory factory)
    {
        return factory.createInteger(value);
    }

    /* (non-Javadoc)
     * @see org.jsoar.kernel.symbols.SymbolImpl#isSameTypeAs(org.jsoar.kernel.symbols.SymbolImpl)
     */
    @Override
    public boolean isSameTypeAs(SymbolImpl other)
    {
        return other.asInteger() != null;
    }


    /* (non-Javadoc)
     * @see org.jsoar.kernel.symbols.SymbolImpl#numericLess(org.jsoar.kernel.symbols.SymbolImpl)
     */
    @Override
    public boolean numericLess(SymbolImpl other)
    {
        IntegerSymbolImpl i = other.asInteger();
        if(i != null)
        {
            return getValue() < i.getValue();
        }
        DoubleSymbolImpl f = other.asDouble();
        
        return f != null ? getValue() < f.getValue() : super.numericLess(other);
    }

    /* (non-Javadoc)
     * @see org.jsoar.kernel.symbols.SymbolImpl#numericLessOrEqual(org.jsoar.kernel.symbols.SymbolImpl)
     */
    @Override
    public boolean numericLessOrEqual(SymbolImpl other)
    {
        IntegerSymbolImpl i = other.asInteger();
        if(i != null)
        {
            return getValue() <= i.getValue();
        }
        DoubleSymbolImpl f = other.asDouble();
        
        return f != null ? getValue() <= f.getValue() : super.numericLess(other);
    }

    /* (non-Javadoc)
     * @see org.jsoar.kernel.symbols.SymbolImpl#numericGreater(org.jsoar.kernel.symbols.SymbolImpl)
     */
    @Override
    public boolean numericGreater(SymbolImpl other)
    {
        IntegerSymbolImpl i = other.asInteger();
        if(i != null)
        {
            return getValue() > i.getValue();
        }
        DoubleSymbolImpl f = other.asDouble();
        
        return f != null ? getValue() > f.getValue() : super.numericLess(other);
    }

    /* (non-Javadoc)
     * @see org.jsoar.kernel.symbols.SymbolImpl#numericGreaterOrEqual(org.jsoar.kernel.symbols.SymbolImpl)
     */
    @Override
    public boolean numericGreaterOrEqual(SymbolImpl other)
    {
        IntegerSymbolImpl i = other.asInteger();
        if(i != null)
        {
            return getValue() >= i.getValue();
        }
        DoubleSymbolImpl f = other.asDouble();
        
        return f != null ? getValue() >= f.getValue() : super.numericLess(other);
    }
    
    /* (non-Javadoc)
     * @see java.lang.Object#toString()
     */
    @Override
    public String toString()
    {
        return Long.toString(getValue());
    }


    /* (non-Javadoc)
     * @see java.util.Formattable#formatTo(java.util.Formatter, int, int, int)
     */
    @Override
    public void formatTo(Formatter formatter, int flags, int width, int precision)
    {
        formatter.format(Long.toString(getValue()));
    }
    
    
}
