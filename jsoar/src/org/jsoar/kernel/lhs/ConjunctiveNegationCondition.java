/*
 * (c) 2008  Dave Ray
 *
 * Created on Aug 16, 2008
 */
package org.jsoar.kernel.lhs;

import java.util.List;

import org.jsoar.kernel.symbols.Variable;

/**
 * @author ray
 */
public class ConjunctiveNegationCondition extends Condition
{
    public Condition top;
    public Condition bottom;
    
    /* (non-Javadoc)
     * @see org.jsoar.kernel.Condition#asConjunctiveNegationCondition()
     */
    @Override
    public ConjunctiveNegationCondition asConjunctiveNegationCondition()
    {
        return this;
    }

    /* (non-Javadoc)
     * @see org.jsoar.kernel.Condition#addAllVariables(int, java.util.List)
     */
    @Override
    public void addAllVariables(int tc_number, List<Variable> var_list)
    {
        addAllVariables(top, tc_number, var_list);
    }

    /* (non-Javadoc)
     * @see org.jsoar.kernel.Condition#addBoundVariables(int, java.util.List)
     */
    @Override
    public void addBoundVariables(int tc_number, List<Variable> var_list)
    {
        // Do nothing
    }
}
