/*
 * (c) 2008  Dave Ray
 *
 * Created on Aug 16, 2008
 */
package org.jsoar.kernel.lhs;

import java.util.LinkedList;

import org.jsoar.kernel.symbols.Symbol;
import org.jsoar.kernel.symbols.Variable;

/**
 * @author ray
 */
public class EqualityTest extends Test
{
    public Symbol sym;
    
    /**
     * @param sym
     */
    public EqualityTest(Symbol sym)
    {
        this.sym = sym;
    }

    /* (non-Javadoc)
     * @see org.jsoar.kernel.Test#copy()
     */
    @Override
    public Test copy()
    {
        return new EqualityTest(sym);
    }

    public boolean isBlank()
    {
        return sym == null;
    }
    
    /**
     * gdatastructs.h:395:referent_of_equality_test
     * 
     * @return
     */
    public Symbol getReferent()
    {
        return sym;
    }
    
    /* (non-Javadoc)
     * @see org.jsoar.kernel.Test#asBlankTest()
     */
    @Override
    public EqualityTest asEqualityTest()
    {
        return this;
    }

    /* (non-Javadoc)
     * @see org.jsoar.kernel.Test#addAllVariables(int, java.util.List)
     */
    @Override
    public void addAllVariables(int tc_number, LinkedList<Variable> var_list)
    {
        Variable var = sym != null ? sym.asVariable() : null;
        if(var != null)
        {
            var.markIfUnmarked(tc_number, var_list);
        }
    }

    /* (non-Javadoc)
     * @see org.jsoar.kernel.Test#addBoundVariables(int, java.util.List)
     */
    @Override
    public void addBoundVariables(int tc_number, LinkedList<Variable> var_list)
    {
        Variable var = sym != null ? sym.asVariable() : null;
        if(var != null)
        {
            var.markIfUnmarked(tc_number, var_list);
        }
    }
    
    

    
    
}
