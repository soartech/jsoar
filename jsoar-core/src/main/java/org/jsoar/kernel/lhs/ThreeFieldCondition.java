/*
 * (c) 2008  Dave Ray
 *
 * Created on Aug 16, 2008
 */
package org.jsoar.kernel.lhs;

import org.jsoar.kernel.symbols.Variable;
import org.jsoar.util.ListHead;
import org.jsoar.util.markers.Marker;

/**
 * @author ray
 */
public abstract class ThreeFieldCondition extends Condition
{
    public Test id_test;
    public Test attr_test;
    public Test value_test;
    public boolean test_for_acceptable_preference;   /* for pos, neg cond's only */
    
    protected ThreeFieldCondition()
    {
        
    }
    
    /**
     * Copy constructor used to negate pos and neg conditions. Test fields are
     * only a shallow copy!
     */
    protected ThreeFieldCondition(ThreeFieldCondition other)
    {
        this.id_test = other.id_test;
        this.attr_test = other.attr_test;
        this.value_test = other.value_test;
        this.test_for_acceptable_preference = other.test_for_acceptable_preference;
    }
    
    /* (non-Javadoc)
     * @see org.jsoar.kernel.Condition#asThreeFieldCondition()
     */
    @Override
    public ThreeFieldCondition asThreeFieldCondition()
    {
        return this;
    }

    /* (non-Javadoc)
     * @see org.jsoar.kernel.Condition#addAllVariables(int, java.util.List)
     */
    @Override
    public void addAllVariables(Marker tc_number, ListHead<Variable> var_list)
    {
        if(id_test != null)
        {
            id_test.addAllVariables(tc_number, var_list);
        }
        if(attr_test != null)
        {
            attr_test.addAllVariables(tc_number, var_list);
        }
        if(value_test != null)
        {
            value_test.addAllVariables(tc_number, var_list);
        }
    }

    /* (non-Javadoc)
     * @see org.jsoar.kernel.lhs.Condition#cond_is_in_tc(int)
     */
    @Override
    public boolean cond_is_in_tc(Marker tc)
    {
        return Tests.test_is_in_tc(id_test, tc);
    }
    
    /* (non-Javadoc)
     * @see java.lang.Object#toString()
     */
    @Override
    public String toString()
    {
        // For debugging only
        return id_test + " ^" + attr_test + " " + value_test;
    }

}
