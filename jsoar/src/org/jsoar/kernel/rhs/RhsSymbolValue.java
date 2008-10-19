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

/**
 * A rhs value that is just a symbol. Objects of this type are immutable.
 * 
 * @author ray
 */
public class RhsSymbolValue extends RhsValue
{
    public final SymbolImpl sym;
    
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
        return sym == newSym ? this : new RhsSymbolValue(newSym);
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
    public void addAllVariables(int tc_number, ListHead<Variable> var_list)
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
}
