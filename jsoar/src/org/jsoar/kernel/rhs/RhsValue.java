/*
 * (c) 2008  Dave Ray
 *
 * Created on Aug 16, 2008
 */
package org.jsoar.kernel.rhs;

import java.util.List;

import org.jsoar.kernel.symbols.Variable;

public abstract class RhsValue
{
    public RhsSymbolValue asSymbolValue()
    {
        return null;
    }
    
    public RhsFunctionCall asFunctionCall()
    {
        return null;
    }
    
    public ReteLocation asReteLocation()
    {
        return null;
    }
    
    public UnboundVariable asUnboundVariable()
    {
        return null;
    }

    public RhsValue copy()
    {
        return this;
    }
    
    public char getFirstLetter()
    {
        return '*';
    }

    public abstract void addAllVariables(int tc_number, List<Variable> var_list);
}
