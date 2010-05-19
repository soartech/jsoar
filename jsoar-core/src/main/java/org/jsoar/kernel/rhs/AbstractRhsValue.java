/*
 * (c) 2010  Dave Ray
 *
 * Created on May 18, 2010
 */
package org.jsoar.kernel.rhs;

/**
 * @author ray
 */
public abstract class AbstractRhsValue implements RhsValue
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

    public char getFirstLetter()
    {
        return '*';
    }

}
