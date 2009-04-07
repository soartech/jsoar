/*
 * (c) 2008  Dave Ray
 *
 * Created on Aug 16, 2008
 */
package org.jsoar.kernel.lhs;

import org.jsoar.kernel.symbols.IdentifierImpl;
import org.jsoar.kernel.symbols.Variable;
import org.jsoar.util.ListHead;

/**
 * @author ray
 */
public class PositiveCondition extends ThreeFieldCondition
{
    /**
     * for top-level positive cond's: used for BT and by the rete
     */
    public BackTraceInfo bt = new BackTraceInfo();
    
    
    public PositiveCondition()
    {
        
    }
    
    public PositiveCondition(NegativeCondition negativeCondition)
    {
        super(negativeCondition);
    }

    public NegativeCondition negate()
    {
        return new NegativeCondition(this);
    }
    
    /* (non-Javadoc)
     * @see org.jsoar.kernel.Condition#asPositiveCondition()
     */
    @Override
    public PositiveCondition asPositiveCondition()
    {
        return this;
    }

    /* (non-Javadoc)
     * @see org.jsoar.kernel.Condition#addBoundVariables(int, java.util.List)
     */
    @Override
    public void addBoundVariables(int tc_number, ListHead<Variable> var_list)
    {
        id_test.addBoundVariables(tc_number, var_list);
        attr_test.addBoundVariables(tc_number, var_list);
        value_test.addBoundVariables(tc_number, var_list);
    }

    /* (non-Javadoc)
     * @see org.jsoar.kernel.lhs.Condition#add_cond_to_tc(int, java.util.LinkedList, java.util.LinkedList)
     */
    @Override
    public void add_cond_to_tc(int tc, ListHead<IdentifierImpl> id_list, ListHead<Variable> var_list)
    {
        Tests.add_test_to_tc(id_test, tc, id_list, var_list);
        Tests.add_test_to_tc(value_test, tc, id_list, var_list);
    }

}
