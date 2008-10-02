/*
 * Copyright (c) 2008  Dave Ray <daveray@gmail.com>
 *
 * Created on Aug 27, 2008
 */
package org.jsoar.kernel.rete;

import java.util.LinkedList;

import org.jsoar.kernel.lhs.ConjunctiveTest;
import org.jsoar.kernel.lhs.EqualityTest;
import org.jsoar.kernel.lhs.Test;
import org.jsoar.kernel.lhs.TestTools;
import org.jsoar.kernel.symbols.Variable;

/**
 *  Varnames and Node_Varnames (NVN) structures are used to record the names
 *  of variables bound (i.e., equality tested) at rete nodes.  The only
 *  purpose of saving this information is so we can reconstruct the 
 *  original source code for a production when we want to print it.  For
 *  chunks, we don't save any of this information -- we just re-gensym 
 *  the variable names on each printing (unless discard_chunk_varnames
 *  is set to FALSE).
 *
 *  For each production, a chain of node_varnames structures is built,
 *  paralleling the structure of the rete net (i.e., the portion of the rete
 *  used for that production).  There is a node_varnames structure for
 *  each Mem, Neg, or NCC node in that part, giving the names of variables
 *  bound in the id, attr, and value fields of the condition at that node.
 *
 *  At each field, we could bind zero, one, or more variables.  To
 *  save space, we use some bit-twiddling here.  A "varnames" represents
 *  zero or more variables:   NIL means zero; a pointer (with the low-order
 *  bit being 0) to a variable means just that one variable; and any
 *  other pointer (with the low-order bit set to 1) points (minus 1, of
 *  course) to a consed list of variables.
 *
 *  Add_var_to_varnames() takes an existing varnames object (which can
 *  be NIL, for no variable names) and returns a new varnames object
 *  which adds (destructively!) a given variable to the previous one.
 *  Deallocate_varnames() deallocates a varnames object, removing references
 *  to symbols, etc.  Deallocate_node_varnames() deallocates a whole
 *  chain of node_varnames structures, scanning up the net, etc.
 *
 *  jsoar: We store either a Variable or LinkedList<Variable> as a Java Object. The
 *         original bit twiddling methods have been converted to use these types
 *         with the appropriate casts and instanceof calls.
 *         
 * rete.cpp:2453
 * @author ray
 */
public class VarNames
{
    public static Object one_var_to_varnames(Variable x) { return x; }
    public static Object var_list_to_varnames(LinkedList<Variable> x) { return x; }
    public static boolean varnames_is_var_list(Object x) { return x instanceof LinkedList<?>; }
    public static boolean varnames_is_one_var(Object x) { return (! (varnames_is_var_list(x))); }
    public static Variable varnames_to_one_var(Object x) { return (Variable) x; }
    @SuppressWarnings("unchecked")
    public static LinkedList<Variable> varnames_to_var_list(Object x) { return (LinkedList<Variable>) x; }

    /**
     * rete.cpp:2516:add_var_to_varnames
     * 
     * @param var
     * @param old_varnames
     * @return
     */
    public static Object add_var_to_varnames(Variable var, Object old_varnames)
    {
        // symbol_add_ref (var);
        if (old_varnames == null)
        {
            return one_var_to_varnames(var);
        }
        if (varnames_is_one_var(old_varnames))
        {
            LinkedList<Variable> new_varnames = new LinkedList<Variable>();
            new_varnames.add(var);
            new_varnames.add(varnames_to_one_var(old_varnames));
            return var_list_to_varnames(new_varnames);
        }
        /* --- otherwise old_varnames is a list --- */
        LinkedList<Variable> list = varnames_to_var_list(old_varnames);
        list.push(var);
        return var_list_to_varnames(list);
    }
    
    /**
     * TODO: I'm pretty sure we don't need this method.
     * 
     * rete.cpp:2539:deallocate_varnames
     * 
     * @param vn
     */
    public static void deallocate_varnames(Object vn)
    {

        if (vn == null)
        {
            return;
        }
        if (varnames_is_one_var(vn))
        {
            Variable sym = varnames_to_one_var(vn);
            // symbol_remove_ref (thisAgent, sym);
        }
        else
        {
            // LinkedList<Variable> symlist = varnames_to_var_list(vn);
            //          deallocate_symbol_list_removing_references (symlist);
        }
    }
    
    /**
     * Add_unbound_varnames_in_test() adds to an existing varnames object
     * the names of any currently-unbound variables equality-tested in a given test.
   
     * rete.cpp:2586:add_unbound_varnames_in_test
     * 
     * @param t
     * @param starting_vn
     * @return
     */
    public static Object add_unbound_varnames_in_test(Test t, Object starting_vn)
    {
        if (TestTools.isBlank(t))
        {
            return starting_vn;
        }
        EqualityTest eq = t.asEqualityTest();
        if (eq != null)
        {
            Variable referent = eq.getReferent().asVariable();
            if (referent != null)
            {
                if (!referent.var_is_bound())
                {
                    starting_vn = add_var_to_varnames(referent, starting_vn);
                }
            }
            return starting_vn;
        }

        ConjunctiveTest ct = t.asConjunctiveTest();
        if (ct != null)
        {
            for (Test c : ct.conjunct_list)
                starting_vn = add_unbound_varnames_in_test(c, starting_vn);
        }
        return starting_vn;
    }

}
