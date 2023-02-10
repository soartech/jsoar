/*
 * (c) 2008  Dave Ray
 *
 * Created on Aug 16, 2008
 */
package org.jsoar.kernel.lhs;

import org.jsoar.kernel.symbols.IdentifierImpl;
import org.jsoar.kernel.symbols.Variable;
import org.jsoar.util.ListHead;
import org.jsoar.util.markers.Marker;

/**
 * @author ray
 */
public class ConjunctiveNegationCondition extends Condition
{
    public Condition top;
    public Condition bottom;
    
    /*
     * (non-Javadoc)
     * 
     * @see org.jsoar.kernel.Condition#asConjunctiveNegationCondition()
     */
    @Override
    public ConjunctiveNegationCondition asConjunctiveNegationCondition()
    {
        return this;
    }
    
    /*
     * (non-Javadoc)
     * 
     * @see org.jsoar.kernel.Condition#addAllVariables(int, java.util.List)
     */
    @Override
    public void addAllVariables(Marker tc_number, ListHead<Variable> var_list)
    {
        addAllVariables(top, tc_number, var_list);
    }
    
    /*
     * (non-Javadoc)
     * 
     * @see org.jsoar.kernel.Condition#addBoundVariables(int, java.util.List)
     */
    @Override
    public void addBoundVariables(Marker tc_number, ListHead<Variable> var_list)
    {
        // Do nothing
    }
    
    /*
     * (non-Javadoc)
     * 
     * @see org.jsoar.kernel.lhs.Condition#add_cond_to_tc(int, java.util.LinkedList, java.util.LinkedList)
     */
    @Override
    public void add_cond_to_tc(Marker tc, ListHead<IdentifierImpl> id_list, ListHead<Variable> var_list)
    {
        // Do nothing
    }
    
    /*
     * (non-Javadoc)
     * 
     * @see org.jsoar.kernel.lhs.Condition#cond_is_in_tc(int)
     */
    @Override
    public boolean cond_is_in_tc(Marker tc)
    {
        // conjunctive negations: keep trying to add stuff to the TC
        final ListHead<IdentifierImpl> new_ids = ListHead.newInstance();
        final ListHead<Variable> new_vars = ListHead.newInstance();
        
        for(Condition c = top; c != null; c = c.next)
            c.already_in_tc = false;
        
        while(true)
        {
            boolean anything_changed = false;
            for(Condition c = top; c != null; c = c.next)
                if(!c.already_in_tc)
                    if(c.cond_is_in_tc(tc))
                    {
                        c.add_cond_to_tc(tc, new_ids, new_vars);
                        c.already_in_tc = true;
                        anything_changed = true;
                    }
            if(!anything_changed)
                break;
        }
        
        // complete TC found, look for anything that didn't get hit
        boolean result = true;
        for(Condition c = top; c != null; c = c.next)
            if(!c.already_in_tc)
                result = false;
            
        // unmark identifiers and variables that we just marked
        IdentifierImpl.unmark(new_ids);
        Variable.unmark(new_vars);
        
        return result;
    }
    
}
