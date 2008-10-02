/*
 * (c) 2008  Dave Ray
 *
 * Created on Aug 15, 2008
 */
package org.jsoar.kernel.symbols;

import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;

import org.jsoar.kernel.DeciderFlag;
import org.jsoar.kernel.lhs.EqualityTest;
import org.jsoar.kernel.memory.Wme;

/**
 * @author ray
 */
public abstract class Symbol extends EqualityTest
{
    public DeciderFlag decider_flag;
    public Wme decider_wme;
    public int retesave_symindex;
    public final int hash_id;
    
    /*package*/ Symbol(int hash_id)
    {
        this.hash_id = hash_id;
    }
    
    public FloatConstant asFloatConstant()
    {
        return null;
    }
    
    public IntConstant asIntConstant()
    {
        return null;
    }
    
    public SymConstant asSymConstant()
    {
        return null;
    }
    
    public Variable asVariable()
    {
        return null;
    }
    
    public Identifier asIdentifier()
    {
        return null;
    }
    
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
    public abstract boolean isSameTypeAs(Symbol other);
    
    /**
     * Return true if this symbol's numeric value is less than the numeric value of
     * other. If neither symbol is numeric, returns false.
     * 
     * @param other Symbol to compare against
     * @return Result of numeric less-than comparison
     */
    public boolean numericLess(Symbol other)
    {
        return false;
    }
    
    /**
     * Return true if this symbol's numeric value is less than or equal to the numeric value of
     * other. If neither symbol is numeric, returns false.
     * 
     * @param other Symbol to compare against
     * @return Result of numeric less-than comparison
     */
    public boolean numericLessOrEqual(Symbol other)
    {
        return false;
    }
    
    /**
     * Return true if this symbol's numeric value is greater than the numeric value of
     * other. If neither symbol is numeric, returns false.
     * 
     * @param other Symbol to compare against
     * @return Result of numeric less-than comparison
     */
    public boolean numericGreater(Symbol other)
    {
        return false;
    }
    
    /**
     * Return true if this symbol's numeric value is greater than or equal to the numeric value of
     * other. If neither symbol is numeric, returns false.
     * 
     * @param other Symbol to compare against
     * @return Result of numeric less-than comparison
     */
    public boolean numericGreaterOrEqual(Symbol other)
    {
        return false;
    }
    

    /**
     * 
     * production.cpp:128:copy_symbol_list_adding_references
     * 
     * @param syms
     * @return
     */
    public static List<Symbol> copy_symbol_list_adding_references(List<Symbol> syms)
    {
        // TODO: What's the right type of list?
        return new ArrayList<Symbol>(syms);
    }  
    
    /**
     * <p>production.cpp:1299:add_symbol_to_tc
     * 
     * @param tc
     * @param id_list
     * @param var_list
     */
    public void add_symbol_to_tc(int tc, LinkedList<Identifier> id_list, LinkedList<Variable> var_list)
    {
        // DO nothing by default
    }
    
    /**
     * <p>production.cpp:1346:symbol_is_in_tc
     * 
     * @param tc
     * @return
     */
    public boolean symbol_is_in_tc(int tc)
    {
        // False by default
        return false;
    }
    
    public static EqualityTest makeEqualityTest(Symbol sym)
    {
        return sym;
    }
    
    /* (non-Javadoc)
     * @see org.jsoar.kernel.lhs.EqualityTest#getReferent()
     */
    @Override
    public Symbol getReferent()
    {
        return this;
    }
    
    
}
