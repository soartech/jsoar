/*
 * (c) 2008  Dave Ray
 *
 * Created on Aug 15, 2008
 */
package org.jsoar.kernel.lhs;

import java.util.LinkedList;

import org.jsoar.kernel.symbols.Identifier;
import org.jsoar.kernel.symbols.Variable;

public abstract class Condition
{
    public boolean already_in_tc;                 /* used only by cond_is_in_tc stuff */
    public boolean test_for_acceptable_preference;   /* for pos, neg cond's only */
    public Condition  next, prev;

    /**
     * for top-level positive cond's: used for BT and by the rete
     */
    public final BackTraceInfo bt = new BackTraceInfo();
    /**
     * used only during reordering. TODO: PositiveCondition only?
     */
    public final ReorderInfo reorder = new ReorderInfo();

    public static Condition insertAtHead(Condition header, Condition c)
    {
        c.next = header;
        c.prev = null;
        if(header != null)
        {
            header.prev = c;
        }
        return c;
    }
    
    public static Condition insertAtEnd(Condition tail, Condition c)
    {
        c.next = null;
        c.prev = tail;
        if(tail != null)
        {
            tail.next = c;
        }
        return c;
    }
    
    public static Condition removeFromList(Condition header, Condition c)
    {
        if(c.next != null)
        {
            c.next.prev = c.prev;
        }
        if(c.prev != null)
        {
            c.prev.next = c.next;
        }
        else
        {
            header = c.next;
        }
        return header;
    }
    
    public static void addAllVariables(Condition header, int tc_number, LinkedList<Variable> var_list)
    {
        for(Condition c = header; c != null; c = c.next)
        {
            c.addAllVariables(tc_number, var_list);
        }
    }
    
    public static void addBoundVariables(Condition header, int tc_number, LinkedList<Variable> var_list)
    {
        for(Condition c = header; c != null; c = c.next)
        {
            c.addBoundVariables(tc_number, var_list);
        }
    }
    
    public abstract void addBoundVariables(int tc_number, LinkedList<Variable> var_list);

    /**
     * production.cpp:1191:add_all_variables_in_condition
     * 
     * @param tc_number
     * @param var_list
     */
    public abstract void addAllVariables(int tc_number, LinkedList<Variable> var_list);

    public ThreeFieldCondition asThreeFieldCondition()
    {
        return null;
    }
    
    public PositiveCondition asPositiveCondition()
    {
        return null;
    }
    public NegativeCondition asNegativeCondition()
    {
        return null;
    }
    public ConjunctiveNegationCondition asConjunctiveNegationCondition()
    {
        return null;
    }

    /**
     * @param tc
     * @return
     */
    public boolean cond_is_in_tc(int tc)
    {
        // TODO cond_is_in_tc: implement for sub-classes
        throw new UnsupportedOperationException("Not implemented");
        //return false;
    }

    /**
     * @param tc
     * @param id_list
     * @param var_list
     */
    public void add_cond_to_tc(int tc, LinkedList<Identifier> id_list, LinkedList<Variable> var_list)
    {
        // TODO add_cond_to_tc: implement for sub-classes
        throw new UnsupportedOperationException("Not implemented");
    }


}
