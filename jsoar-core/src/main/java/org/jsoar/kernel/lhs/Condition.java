/*
 * (c) 2008  Dave Ray
 *
 * Created on Aug 15, 2008
 */
package org.jsoar.kernel.lhs;

import java.io.StringWriter;
import java.util.Formattable;
import java.util.Formatter;
import java.util.List;

import org.jsoar.kernel.symbols.IdentifierImpl;
import org.jsoar.kernel.symbols.Variable;
import org.jsoar.kernel.tracing.Printer;
import org.jsoar.util.ByRef;
import org.jsoar.util.ListHead;
import org.jsoar.util.markers.Marker;

/**
 * gdatastructs.h:524:condition
 * 
 * @author ray
 */
public abstract class Condition implements Formattable
{
    public boolean already_in_tc;                 /* used only by cond_is_in_tc stuff */
    public boolean test_for_acceptable_preference;   /* for pos, neg cond's only */
    public Condition  next, prev;

    /**
     * used only during reordering.
     */
    List<Variable> reorder_vars_requiring_bindings = null;
    Condition reorder_next_min_cost = null;

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
    
    public static void addAllVariables(Condition header, Marker tc_number, ListHead<Variable> var_list)
    {
        for(Condition c = header; c != null; c = c.next)
        {
            c.addAllVariables(tc_number, var_list);
        }
    }
    
    public static void addBoundVariables(Condition header, Marker tc_number, ListHead<Variable> var_list)
    {
        for(Condition c = header; c != null; c = c.next)
        {
            c.addBoundVariables(tc_number, var_list);
        }
    }
    
    public abstract void addBoundVariables(Marker tc_number, ListHead<Variable> var_list);

    /**
     * production.cpp:1191:add_all_variables_in_condition
     * 
     * @param tc_number
     * @param var_list
     */
    public abstract void addAllVariables(Marker tc_number, ListHead<Variable> var_list);

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
     * <p>Note: polymorphized in JSoar
     * <p>production.cpp:1372:cond_is_in_tc
     * 
     * @param tc
     * @return
     */
    public abstract boolean cond_is_in_tc(Marker tc);

    /**
     * <p>production.cpp:1327:add_cond_to_tc
     * <p>Note: polymorphized in JSoar
     * 
     * @param tc
     * @param id_list
     * @param var_list
     */
    public abstract void add_cond_to_tc(Marker tc, ListHead<IdentifierImpl> id_list, ListHead<Variable> var_list);

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
            result = Tests.hash_test(pc.id_test);
            result = (result << 24) | (result >> 8);
            result ^= Tests.hash_test(pc.attr_test);
            result = (result << 24) | (result >> 8);
            result ^= Tests.hash_test(pc.value_test);
            if (cond.test_for_acceptable_preference)
                result++;

            return result;
        }

        NegativeCondition nc = cond.asNegativeCondition();
        if (nc != null)
        {
            result = 1267818;
            result ^= Tests.hash_test(nc.id_test);
            result = (result << 24) | (result >> 8);
            result ^= Tests.hash_test(nc.attr_test);
            result = (result << 24) | (result >> 8);
            result ^= Tests.hash_test(nc.value_test);
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
        	// treat variables in negations slightly differently, bug 510
        	boolean neg = c1.asNegativeCondition() != null;
            ThreeFieldCondition tfc2 = c2.asThreeFieldCondition();
            if (!Tests.tests_are_equal(tfc1.id_test, tfc2.id_test, neg))
                return false;
            if (!Tests.tests_are_equal(tfc1.attr_test, tfc2.attr_test, neg))
                return false;
            if (!Tests.tests_are_equal(tfc1.value_test, tfc2.value_test, neg))
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
            New.id_test = Tests.copy(pc.id_test);
            New.attr_test = Tests.copy(pc.attr_test);
            New.value_test = Tests.copy(pc.value_test);
            New.test_for_acceptable_preference = pc.test_for_acceptable_preference;
            New.bt = pc.bt.copy();
            return New;
        }
        NegativeCondition nc = cond.asNegativeCondition();
        if (nc != null)
        {
            NegativeCondition New = new NegativeCondition();
            New.id_test = Tests.copy(nc.id_test);
            New.attr_test = Tests.copy(nc.attr_test);
            New.value_test = Tests.copy(nc.value_test);
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
    

    /**
     * <p>explain.cpp:117:copy_cond_list
     * 
     * @param top_list
     * @return
     */
    public static Condition copy_cond_list(Condition top_list)
    {
        ByRef<Condition> new_top = ByRef.create(null);
        ByRef<Condition> new_bottom = ByRef.create(null);

        copy_condition_list(top_list, new_top, new_bottom);
        return new_top.value;
    }

    /**
     * <p>explain.cpp:129:copy_conds_from_list
     * 
     * @param top_list
     * @return
     */
    public static Condition copy_conds_from_list(List<Condition> top_list)
    {
        Condition top = null, prev = null;

        for (Condition cc : top_list)
        {
            Condition cond = copy_condition(cc);
            cond.prev = prev;
            cond.next = null;

            if (prev == null)
                top = cond;
            else
                prev.next = cond;

            prev = cond;
        }
        return (top);
    }
    
    /**
     * <p>Moved from explain.cpp since this is fairly generic
     * 
     * <p>explain.cpp:353:explain_find_cond
     * 
     * @param target
     * @param cond_list
     * @return
     */
    public static Condition explain_find_cond(Condition target, Condition cond_list)
    {
        for (Condition cond = cond_list; cond != null; cond = cond.next)
        {
            if (conditions_are_equal(target, cond))
                return cond;
        }
        return null;
    }
    

    /* (non-Javadoc)
     * @see java.util.Formattable#formatTo(java.util.Formatter, int, int, int)
     */
    @Override
    public void formatTo(Formatter formatter, int flags, int width, int precision)
    {
        // <p>print.cpp:871:print_condition
        
        Condition old_next = this.next;
        Condition old_prev = this.prev;
        this.next = null;
        this.prev = null;
        
        StringWriter writer = new StringWriter();
        Conditions.print_condition_list (new Printer(writer, false), this, 0, true);
        
        this.next = old_next;
        this.prev = old_prev;
        
        formatter.format(writer.toString());
    }

    
}
