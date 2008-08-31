/*
 * (c) 2008  Dave Ray
 *
 * Created on Aug 15, 2008
 */
package org.jsoar.kernel.symbols;

import java.util.ArrayList;
import java.util.List;

import org.jsoar.kernel.DeciderFlag;
import org.jsoar.kernel.Wme;

/**
 * @author ray
 */
public abstract class Symbol
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
     * 
     * production.cpp:128:copy_symbol_list_adding_references
     * 
     * @param syms
     * @return
     */
    public static List<Symbol> copy_symbol_list_adding_references(List<Symbol> syms)
    {
        // TODO: add refs?
        // TODO: What's the right type of list?
        return new ArrayList<Symbol>(syms);
    }    
}
