/*
 * (c) 2008  Dave Ray
 *
 * Created on Aug 17, 2008
 */
package org.jsoar.kernel.rhs;

import java.util.Formatter;

import org.jsoar.kernel.symbols.Symbol;
import org.jsoar.kernel.symbols.Variable;
import org.jsoar.util.ListHead;

/**
 * @author ray
 */
public class RhsSymbolValue extends RhsValue
{
    public Symbol sym;
    
    public RhsSymbolValue(Symbol sym)
    {
        this.sym = sym;
    }

    
    /**
     * @return the sym
     */
    public Symbol getSym()
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
