/*
 * (c) 2008  Dave Ray
 *
 * Created on Aug 15, 2008
 */
package org.jsoar.kernel.symbols;


/**
 * 
 * @author ray
 */
public interface Symbol
{
    public DoubleSymbol asFloatConstant();
    
    public IntegerSymbol asIntConstant();
    
    public StringSymbol asSymConstant();
    
    public IdSymbol asIdentifier();
}
