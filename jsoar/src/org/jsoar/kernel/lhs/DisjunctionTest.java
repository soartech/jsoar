/*
 * (c) 2008  Dave Ray
 *
 * Created on Aug 16, 2008
 */
package org.jsoar.kernel.lhs;

import java.util.ArrayList;
import java.util.List;

import org.jsoar.kernel.symbols.Symbol;

/**
 * @author ray
 */
public class DisjunctionTest extends ComplexTest
{
    public List<Symbol> disjunction_list = new ArrayList<Symbol>();
    
    public DisjunctionTest()
    {
        
    }
    private DisjunctionTest(DisjunctionTest other)
    {
        this.disjunction_list.addAll(other.disjunction_list);
    }
    
    public DisjunctionTest asDisjunctionTest()
    {
        return this;
    }
    /* (non-Javadoc)
     * @see org.jsoar.kernel.Test#copy()
     */
    @Override
    public Test copy()
    {
        return new DisjunctionTest(this);
    }

}
