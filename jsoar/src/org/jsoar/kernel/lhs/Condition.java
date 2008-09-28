/*
 * (c) 2008  Dave Ray
 *
 * Created on Aug 15, 2008
 */
package org.jsoar.kernel.lhs;

import java.util.LinkedList;

import org.jsoar.kernel.symbols.Identifier;
import org.jsoar.kernel.symbols.Variable;
import org.jsoar.util.ByRef;

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

    /**
     * Returns a hash value for the given condition.
     * 
     * <p>TODO At least make this polymorphic
     * 
     * <p>production.cpp:828:hash_condition
     * 
     * @param cond
     * @return
     */
    public static int hash_condition(Condition cond)
    {
        int result;

        PositiveCondition pc = cond.asPositiveCondition();
        if (pc != null)
        {
            result = Test.hash_test(pc.id_test);
            result = (result << 24) | (result >> 8);
            result ^= Test.hash_test(pc.attr_test);
            result = (result << 24) | (result >> 8);
            result ^= Test.hash_test(pc.value_test);
            if (cond.test_for_acceptable_preference)
                result++;

            return result;
        }

        NegativeCondition nc = cond.asNegativeCondition();
        if (nc != null)
        {
            result = 1267818;
            result ^= Test.hash_test(nc.id_test);
            result = (result << 24) | (result >> 8);
            result ^= Test.hash_test(nc.attr_test);
            result = (result << 24) | (result >> 8);
            result ^= Test.hash_test(nc.value_test);
            if (cond.test_for_acceptable_preference)
                result++;

            return result;
        }

        ConjunctiveNegationCondition ncc = cond.asConjunctiveNegationCondition();
        if (ncc != null)
        {
            result = 82348149;
            for (Condition c = ncc.top; c != null; c = c.next)
            {
                result ^= hash_condition(c);
                result = (result << 24) | (result >> 8);
            }
            return result;
        }

        throw new IllegalStateException("Internal error: bad cond type in hash_condition");
    }
    
    /**
     * Returns TRUE iff the two conditions are identical.
     * 
     * <p>production.cpp:794:conditions_are_equal
     * 
     * @param c1 First condition to test
     * @param c2 Second condition to test
     * @return true iff the two conditions are identical
     */
    public static boolean conditions_are_equal(Condition c1, Condition c2)
    {
        if (!c1.getClass().equals(c2.getClass()))
            return false;

        // Positive or negative
        ThreeFieldCondition tfc1 = c1.asThreeFieldCondition();
        if (tfc1 != null)
        {
            ThreeFieldCondition tfc2 = c2.asThreeFieldCondition();
            if (!TestTools.tests_are_equal(tfc1.id_test, tfc2.id_test))
                return false;
            if (!TestTools.tests_are_equal(tfc1.attr_test, tfc2.attr_test))
                return false;
            if (!TestTools.tests_are_equal(tfc1.value_test, tfc2.value_test))
                return false;
            if (c1.test_for_acceptable_preference != c2.test_for_acceptable_preference)
                return false;
            return true;
        }
        ConjunctiveNegationCondition ncc1 = c1.asConjunctiveNegationCondition();
        if (ncc1 != null)
        {
            ConjunctiveNegationCondition ncc2 = c2.asConjunctiveNegationCondition();
            for (c1 = ncc1.top, c2 = ncc2.top; ((c1 != null) && (c2 != null)); c1 = c1.next, c2 = c2.next)
                if (!conditions_are_equal(c1, c2))
                    return false;
            if (c1 == c2)
                return true; /* make sure they both hit end-of-list */
            return false;
        }

        throw new IllegalStateException("Unknown condition types: " + c1 + ", " + c2);
    }
    
    /**
     * Returns a new copy of the given condition.
     * 
     * <p>TODO Make copy_condition polymorphic
     * <p>production.cpp:741:copy_condition
     * 
     * @param cond
     * @return
     */
    public static Condition copy_condition(Condition cond)
    {
        if (cond == null)
            return null;

        PositiveCondition pc = cond.asPositiveCondition();
        if (pc != null)
        {
            PositiveCondition New = new PositiveCondition();
            New.id_test = Test.copy(pc.id_test);
            New.attr_test = Test.copy(pc.attr_test);
            New.value_test = Test.copy(pc.value_test);
            New.test_for_acceptable_preference = pc.test_for_acceptable_preference;
            return New;
        }
        NegativeCondition nc = cond.asNegativeCondition();
        if (nc != null)
        {
            NegativeCondition New = new NegativeCondition();
            New.id_test = Test.copy(nc.id_test);
            New.attr_test = Test.copy(nc.attr_test);
            New.value_test = Test.copy(nc.value_test);
            New.test_for_acceptable_preference = nc.test_for_acceptable_preference;
            return New;
        }
        ConjunctiveNegationCondition ncc = cond.asConjunctiveNegationCondition();
        if (ncc != null)
        {
            ConjunctiveNegationCondition New = new ConjunctiveNegationCondition();
            ByRef<Condition> ncc_top = ByRef.create(null);
            ByRef<Condition> ncc_bottom = ByRef.create(null);
            copy_condition_list(ncc.top, ncc_top, ncc_bottom);
            New.top = ncc_top.value;
            New.bottom = ncc_bottom.value;
            return New;
        }

        throw new IllegalStateException("Unkonwn condition to copy_condition(): " + cond);
    }

    /**
     * Copies the given condition list, returning pointers to the top-most and
     * bottom-most conditions in the new copy.
     * 
     * production.cpp:772:copy_condition_list
     * 
     * @param top_cond
     * @param dest_top
     * @param dest_bottom
     */
    public static void copy_condition_list(Condition top_cond, ByRef<Condition> dest_top, ByRef<Condition> dest_bottom)
    {
        Condition prev = null;
        while (top_cond != null)
        {
            Condition New = copy_condition(top_cond);
            
            if (prev != null)
                prev.next = New;
            else
                dest_top.value = New;
            
            New.prev = prev;
            prev = New;
            top_cond = top_cond.next;
        }
        if (prev != null)
            prev.next = null;
        else
            dest_top.value = null;
        
        dest_bottom.value = prev;
    }
}
