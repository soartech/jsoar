/*
 * (c) 2008  Dave Ray
 *
 * Created on Aug 17, 2008
 */
package org.jsoar.kernel.rhs;

import java.util.Formatter;

import org.jsoar.kernel.symbols.SymbolImpl;
import org.jsoar.kernel.symbols.Variable;
import org.jsoar.util.ListHead;
import org.jsoar.util.markers.Marker;

/**
 * A rhs value that is just a symbol. Objects of this type are immutable.
 * 
 * <p>TODO Memory usage: make this an interface implemented by SymbolImpl.
 * Addendum, 5/2010: FWIW, I tried this and it didn't make much difference
 * for count-test or long-running blocks world so I backed it out.
 * 
 * @author ray
 */
public class RhsSymbolValue extends AbstractRhsValue
{
    public final SymbolImpl sym;
    
    /**
     * Construct a RHS symbol value. <b>This should only be used by
     * {@link SymbolImpl}! If you want one of these, get it through
     * {@link SymbolImpl#toRhsValue()}.</b>
     * 
     * @param sym the symbol
     */
    public RhsSymbolValue(SymbolImpl sym)
    {
        this.sym = sym;
    }

    /**
     * "Change" the symbol this value refers to, this method creates and 
     * returns a new value, or returns <code>this</code> if the symbol
     * is the same as the one it already holds. This takes advantage of the
     * fact that these objects are immutable to avoid unnecessary allocations.
     * 
     * @param newSym The new symbol for the value
     * @return New RhsSymbolValue with the given symbol value
     */
    public RhsSymbolValue setSymbol(SymbolImpl newSym)
    {
        return sym == newSym ? this : newSym.toRhsValue();
    }
    
    /**
     * @return the sym
     */
    public SymbolImpl getSym()
    {
        return sym;
    }


    /* (non-Javadoc)
     * @see org.jsoar.kernel.RhsValue#asSymbolValue()
     */
    @Override
    public RhsSymbolValue asSymbolValue()
    {
        return this;
    }


    /* (non-Javadoc)
     * @see org.jsoar.kernel.rhs.RhsValue#copy()
     */
    @Override
    public RhsValue copy()
    {
        return this;
    }

    /* (non-Javadoc)
     * @see org.jsoar.kernel.RhsValue#getFirstLetter()
     */
    @Override
    public char getFirstLetter()
    {
        return sym.getFirstLetter();
    }


    /* (non-Javadoc)
     * @see org.jsoar.kernel.RhsValue#addAllVariables(int, java.util.List)
     */
    @Override
    public void addAllVariables(Marker tc_number, ListHead<Variable> var_list)
    {
        Variable var = getSym().asVariable();
        if(var != null)
        {
            var.markIfUnmarked(tc_number, var_list);
        }
    }


    /* (non-Javadoc)
     * @see java.lang.Object#toString()
     */
    @Override
    public String toString()
    {
        return sym.toString();
    }


    /* (non-Javadoc)
     * @see java.util.Formattable#formatTo(java.util.Formatter, int, int, int)
     */
    @Override
    public void formatTo(Formatter formatter, int flags, int width, int precision)
    {
        formatter.format("%s", getSym());
    }

    /* (non-Javadoc)
     * @see java.lang.Object#hashCode()
     */
    @Override
    public int hashCode()
    {
        final int prime = 31;
        int result = 1;
        result = prime * result + ((sym == null) ? 0 : sym.hashCode());
        return result;
    }

    /* (non-Javadoc)
     * @see java.lang.Object#equals(java.lang.Object)
     */
    @Override
    public boolean equals(Object obj)
    {
        if (this == obj)
            return true;
        if (obj == null)
            return false;
        if (!(obj instanceof RhsSymbolValue))
            return false;
        RhsSymbolValue other = (RhsSymbolValue) obj;
        
        return other.sym == sym;
    }
    
    
}
