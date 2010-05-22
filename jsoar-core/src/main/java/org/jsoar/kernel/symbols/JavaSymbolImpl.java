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
public class JavaSymbolImpl extends SymbolImpl implements JavaSymbol
{
    private final Object value;

    /**
     * @param hash_id
     */
    JavaSymbolImpl(SymbolFactory factory, int hash_id, Object value)
    {
        super(factory, hash_id);
        
        this.value = value;
    }

    public Object getValue()
    {
        return value;
    }
    
    /* (non-Javadoc)
     * @see org.jsoar.kernel.Symbol#asJava()
     */
    @Override
    public JavaSymbolImpl asJava()
    {
        return this;
    }
    
    /* (non-Javadoc)
     * @see org.jsoar.kernel.symbols.SymbolImpl#importInto(org.jsoar.kernel.symbols.SymbolFactory)
     */
    @Override
    Symbol importInto(SymbolFactory factory)
    {
        return factory.createJavaSymbol(value);
    }

    /* (non-Javadoc)
     * @see org.jsoar.kernel.symbols.SymbolImpl#isSameTypeAs(org.jsoar.kernel.symbols.SymbolImpl)
     */
    @Override
    public boolean isSameTypeAs(SymbolImpl other)
    {
        // TODO for Java symbols, do deep type comparison
        return other.asJava() != null;
    }
    
    /* (non-Javadoc)
     * @see java.lang.Object#toString()
     */
    @Override
    public String toString()
    {
        return value != null ? value.toString() : "null";
    }

    /* (non-Javadoc)
     * @see java.util.Formattable#formatTo(java.util.Formatter, int, int, int)
     */
    @Override
    public void formatTo(Formatter formatter, int flags, int width, int precision)
    {
        formatter.format("%s", value);
    }
}
