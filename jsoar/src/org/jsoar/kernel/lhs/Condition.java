/*
 * (c) 2008  Dave Ray
 *
 * Created on Aug 15, 2008
 */
package org.jsoar.kernel.lhs;

import java.io.StringWriter;
import java.util.Formattable;
import java.util.Formatter;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;

import org.jsoar.kernel.symbols.IdentifierImpl;
import org.jsoar.kernel.symbols.Variable;
import org.jsoar.kernel.tracing.Printer;
import org.jsoar.util.ByRef;
import org.jsoar.util.ListHead;

public abstract class Condition implements Formattable
{
    public boolean already_in_tc;                 /* used only by cond_is_in_tc stuff */
    public boolean test_for_acceptable_preference;   /* for pos, neg cond's only */
    public Condition  next, prev;

    /**
     * for top-level positive cond's: used for BT and by the rete
     * 
     * <p>TODO Move this down to PositiveCondition
     */
    public BackTraceInfo bt = new BackTraceInfo();
    
    /**
     * used only during reordering. TODO: PositiveCondition only?
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
    
    public static void addAllVariables(Condition header, int tc_number, ListHead<Variable> var_list)
    {
        for(Condition c = header; c != null; c = c.next)
        {
            c.addAllVariables(tc_number, var_list);
        }
    }
    
    public static void addBoundVariables(Condition header, int tc_number, ListHead<Variable> var_list)
    {
        for(Condition c = header; c != null; c = c.next)
        {
            c.addBoundVariables(tc_number, var_list);
        }
    }
    
    public abstract void addBoundVariables(int tc_number, ListHead<Variable> var_list);

    /**
     * production.cpp:1191:add_all_variables_in_condition
     * 
     * @param tc_number
     * @param var_list
     */
    public abstract void addAllVariables(int tc_number, ListHead<Variable> var_list);

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
    public abstract boolean cond_is_in_tc(int tc);

    /**
     * <p>production.cpp:1327:add_cond_to_tc
     * <p>Note: polymorphized in JSoar
     * 
     * @param tc
     * @param id_list
     * @param var_list
     */
    public abstract void add_cond_to_tc(int tc, ListHead<IdentifierImpl> id_list, ListHead<Variable> var_list);

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
            result = TestTools.hash_test(pc.id_test);
            result = (result << 24) | (result >> 8);
            result ^= TestTools.hash_test(pc.attr_test);
            result = (result << 24) | (result >> 8);
            result ^= TestTools.hash_test(pc.value_test);
            if (cond.test_for_acceptable_preference)
                result++;

            return result;
        }

        NegativeCondition nc = cond.asNegativeCondition();
        if (nc != null)
        {
            result = 1267818;
            result ^= TestTools.hash_test(nc.id_test);
            result = (result << 24) | (result >> 8);
            result ^= TestTools.hash_test(nc.attr_test);
            result = (result << 24) | (result >> 8);
            result ^= TestTools.hash_test(nc.value_test);
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
            New.id_test = TestTools.copy(pc.id_test);
            New.attr_test = TestTools.copy(pc.attr_test);
            New.value_test = TestTools.copy(pc.value_test);
            New.test_for_acceptable_preference = pc.test_for_acceptable_preference;
            New.bt = pc.bt.copy();
            return New;
        }
        NegativeCondition nc = cond.asNegativeCondition();
        if (nc != null)
        {
            NegativeCondition New = new NegativeCondition();
            New.id_test = TestTools.copy(nc.id_test);
            New.attr_test = TestTools.copy(nc.attr_test);
            New.value_test = TestTools.copy(nc.value_test);
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
     * print.cpp:1103:print_list_of_conditions
     * 
     * @param printer
     * @param cond
     */
    public static void print_list_of_conditions(Printer printer, Condition cond)
    {
        while (cond != null)
        {
            if (printer.get_printer_output_column() >= printer.getColumnsPerLine() - 20)
                printer.print("\n      %s\n", cond);
            cond = cond.next;
        }
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
    

    /**
     * <p>print.cpp:488:print_condition_list
     * 
     * @param printer
     * @param conds
     * @param indent
     * @param internal
     */
    public static void print_condition_list(Printer printer, Condition conds, int indent, boolean internal)
    {
        if (conds == null)
            return;

        // build dl_list of all the actions
        LinkedList<Condition> conds_not_yet_printed = new LinkedList<Condition>();

        for (Condition c = conds; c != null; c = c.next)
        {
            conds_not_yet_printed.add(c);
        }

        // main loop: find all conds for first id, print them together
        boolean did_one_line_already = false;
        while (!conds_not_yet_printed.isEmpty())
        {
            if (did_one_line_already)
            {
                printer.print("\n").spaces(indent);
            }
            else
            {
                did_one_line_already = true;
            }

            final Condition c = conds_not_yet_printed.pop();
            final ConjunctiveNegationCondition ncc = c.asConjunctiveNegationCondition();
            if (ncc != null)
            {
                printer.print("-{");
                print_condition_list(printer, ncc.top, indent + 2, internal);
                printer.print("}");
                continue;
            }

            // normal pos/neg conditions
            ThreeFieldCondition tfc = c.asThreeFieldCondition();
            ByRef<Boolean> removed_goal_test = ByRef.create(false);
            ByRef<Boolean> removed_impasse_test = ByRef.create(false);

            Test id_test = TestTools.copy_test_removing_goal_impasse_tests(tfc.id_test, removed_goal_test,
                    removed_impasse_test);
            Test id_test_to_match = TestTools.copy_of_equality_test_found_in_test(id_test);

            // collect all cond's whose id test matches this one, removing them
            // from the conds_not_yet_printed list
            LinkedList<ThreeFieldCondition> conds_for_this_id = new LinkedList<ThreeFieldCondition>();
            conds_for_this_id.add(tfc);
            if (!internal)
            {
                Iterator<Condition> it = conds_not_yet_printed.iterator();
                while (it.hasNext())
                {
                    final Condition n = it.next();

                    // pick_conds_with_matching_id_test
                    ThreeFieldCondition ntfc = n.asThreeFieldCondition();
                    if (ntfc != null && TestTools.tests_are_equal(id_test_to_match, ntfc.id_test))
                    {
                        conds_for_this_id.add(ntfc);
                        it.remove();
                    }
                }
            }

            // print the collected cond's all together
            printer.print(" (");
            if (removed_goal_test.value)
            {
                printer.print("state ");
            }

            if (removed_impasse_test.value)
            {
                printer.print("impasse ");
            }

            printer.print("%s", id_test);

            while (!conds_for_this_id.isEmpty())
            {
                final ThreeFieldCondition tc = conds_for_this_id.pop();

                { // build and print attr/value test for condition c
                    final StringBuilder gs = new StringBuilder();
                    gs.append(" ");
                    if (tc.asNegativeCondition() != null)
                    {
                        gs.append("-");
                    }

                    gs.append("^");
                    gs.append(String.format("%s", tc.attr_test));
                    if (!TestTools.isBlank(tc.value_test))
                    {
                        gs.append(String.format(" %s", tc.value_test));
                        if (tc.test_for_acceptable_preference)
                        {
                            gs.append(" +");
                        }
                    }
                    if (printer.get_printer_output_column() + gs.length() >= printer.getColumnsPerLine())
                    {
                        printer.print("\n").spaces(indent + 6);
                    }
                    printer.print(gs.toString());
                }
            }
            printer.print(")");
        }
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
        print_condition_list (new Printer(writer, false), this, 0, true);
        
        this.next = old_next;
        this.prev = old_prev;
        
        formatter.format(writer.toString());
    }

    
}
