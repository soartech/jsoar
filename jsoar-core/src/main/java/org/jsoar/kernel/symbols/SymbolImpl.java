/*
 * (c) 2008  Dave Ray
 *
 * Created on Aug 15, 2008
 */
package org.jsoar.kernel.symbols;

import org.jsoar.kernel.DeciderFlag;
import org.jsoar.kernel.lhs.EqualityTest;
import org.jsoar.kernel.memory.WmeImpl;
import org.jsoar.kernel.rhs.RhsSymbolValue;
import org.jsoar.util.ListHead;
import org.jsoar.util.adaptables.Adaptables;
import org.jsoar.util.markers.Marker;

/**
 * This is the internal implementation class for symbols. It should
 * only be used in the kernel. External code (I/O and RHS functions) should use
 * {@link Symbol}
 * 
 * <p>symtab.h:251:SymbolImpl
 * 
 * @author ray
 */
public abstract class SymbolImpl extends EqualityTest implements Symbol
{
    final SymbolFactory factory;
    public DeciderFlag decider_flag;
    public WmeImpl decider_wme;
    public final int hash_id;
    private RhsSymbolValue rhsValue;
    
    /*package*/ SymbolImpl(SymbolFactory factory, int hash_id)
    {
        this.factory = factory;
        this.hash_id = hash_id;
    }
    
    public RhsSymbolValue toRhsValue()
    {
        if(rhsValue == null)
        {
            rhsValue = new RhsSymbolValue(this);
        }
        return rhsValue;
    }
    
    /* (non-Javadoc)
     * @see org.jsoar.kernel.symbols.Symbol#asFloatConstant()
     */
    public DoubleSymbolImpl asDouble()
    {
        return null;
    }
    
    /* (non-Javadoc)
     * @see org.jsoar.kernel.symbols.Symbol#asIntConstant()
     */
    public IntegerSymbolImpl asInteger()
    {
        return null;
    }
    
    /* (non-Javadoc)
     * @see org.jsoar.kernel.symbols.Symbol#asSymConstant()
     */
    public StringSymbolImpl asString()
    {
        return null;
    }
    
    /* (non-Javadoc)
     * @see org.jsoar.kernel.symbols.Symbol#asVariable()
     */
    public Variable asVariable()
    {
        return null;
    }
    
    /* (non-Javadoc)
     * @see org.jsoar.kernel.symbols.Symbol#asIdentifier()
     */
    public IdentifierImpl asIdentifier()
    {
        return null;
    }
    
    /* (non-Javadoc)
     * @see org.jsoar.kernel.symbols.Symbol#asJava()
     */
    @Override
    public JavaSymbol asJava()
    {
        return null;
    }

    /**
     * @return the first letter of the symbol
     */
    public char getFirstLetter()
    {
        return '*';
    }
    
    /**
     * Return true if this symbol is the same type as other symbol. This is
     * a replacement for comparing symbol type enums in the C kernel
     * 
     * @param other The symbol to test against
     * @return True if this symbol has the same type as other
     */
    public abstract boolean isSameTypeAs(SymbolImpl other);
    
    /**
     * Return true if this symbol's numeric value is less than the numeric value of
     * other. If neither symbol is numeric, returns false.
     * 
     * @param other SymbolImpl to compare against
     * @return Result of numeric less-than comparison
     */
    public boolean numericLess(SymbolImpl other)
    {
        return false;
    }
    
    /**
     * Return true if this symbol's numeric value is less than or equal to the numeric value of
     * other. If neither symbol is numeric, returns false.
     * 
     * @param other SymbolImpl to compare against
     * @return Result of numeric less-than comparison
     */
    public boolean numericLessOrEqual(SymbolImpl other)
    {
        return false;
    }
    
    /**
     * Return true if this symbol's numeric value is greater than the numeric value of
     * other. If neither symbol is numeric, returns false.
     * 
     * @param other SymbolImpl to compare against
     * @return Result of numeric less-than comparison
     */
    public boolean numericGreater(SymbolImpl other)
    {
        return false;
    }
    
    /**
     * Return true if this symbol's numeric value is greater than or equal to the numeric value of
     * other. If neither symbol is numeric, returns false.
     * 
     * @param other SymbolImpl to compare against
     * @return Result of numeric less-than comparison
     */
    public boolean numericGreaterOrEqual(SymbolImpl other)
    {
        return false;
    }

    /**
     * <p>production.cpp:1299:add_symbol_to_tc
     * 
     * @param tc
     * @param id_list
     * @param var_list
     */
    public void add_symbol_to_tc(Marker tc, ListHead<IdentifierImpl> id_list, ListHead<Variable> var_list)
    {
        // DO nothing by default
    }
    
    /**
     * <p>production.cpp:1346:symbol_is_in_tc
     * 
     * @param tc the tc marker
     * @return true if this symbols is in the transitive closure
     */
    public boolean symbol_is_in_tc(Marker tc)
    {
        // False by default
        return false;
    }
    
    public static EqualityTest makeEqualityTest(SymbolImpl sym)
    {
        return sym;
    }
    
    /* (non-Javadoc)
     * @see org.jsoar.kernel.lhs.EqualityTest#getReferent()
     */
    @Override
    public SymbolImpl getReferent()
    {
        return this;
    }
    
    /* (non-Javadoc)
     * @see com.soartech.simjr.Adaptable#getAdapter(java.lang.Class)
     */
    public Object getAdapter(Class<?> klass)
    {
        return Adaptables.adapt(this, klass, false);
    }

    abstract Symbol importInto(SymbolFactory factory);
    
    public boolean belongsTo(SymbolFactory factory) { return this.factory == factory; }
    
}
