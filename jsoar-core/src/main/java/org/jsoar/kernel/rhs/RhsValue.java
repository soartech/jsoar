/*
 * (c) 2008  Dave Ray
 *
 * Created on Aug 16, 2008
 */
package org.jsoar.kernel.rhs;

import java.util.Formattable;

import org.jsoar.kernel.lhs.Condition;
import org.jsoar.kernel.rete.Rete;
import org.jsoar.kernel.symbols.SymbolImpl;
import org.jsoar.kernel.symbols.Variable;
import org.jsoar.util.ListHead;
import org.jsoar.util.markers.Marker;

/**
 * <p>rhs_value_to_string is handled by Formattable implementation
 * <p>TODO make this an interface
 * 
 * @author ray
 */
public abstract class RhsValue implements Formattable
{
    public RhsSymbolValue asSymbolValue()
    {
        return null;
    }
    
    public RhsFunctionCall asFunctionCall()
    {
        return null;
    }
    
    public ReteLocation asReteLocation()
    {
        return null;
    }
    
    public UnboundVariable asUnboundVariable()
    {
        return null;
    }

    /**
     * Returns a copy of this rhs value. If a value is immutable it may return 
     * <code>this</code>.
     * 
     * @return A copy of this rhs value
     */
    public abstract RhsValue copy();
    
    public char getFirstLetter()
    {
        return '*';
    }

    /**
     * Finding all variables from rhs_value's, actions, and action lists
     * 
     * These routines collect all the variables in rhs_value's, etc. Their
     * "var_list" arguments should either be NIL or else should point to the
     * header of the list of marked variables being constructed.
     * 
     * Warning: These are part of the reorderer and handle only productions in
     * non-reteloc, etc. format. They don't handle reteloc's or RHS unbound
     * variables.
     * 
     * <p>production.cpp:1223:add_all_variables_in_rhs_value
     * 
     * @param tc_number
     * @param var_list
     */
    public abstract void addAllVariables(Marker tc_number, ListHead<Variable> var_list);
    
    /**
     * When we print a production (but not when we fire one), we have to
     * reconstruct the RHS actions. This is because many of the variables in the
     * RHS have been replaced by references to Rete locations (i.e., rather than
     * specifying <v>, we specify "value field 3 levels up" or "the 7th RHS
     * unbound variable". The routines below copy rhs_value's and actions, and
     * substitute variable names for such references. For RHS unbound variables,
     * we gensym new variable names.
     * 
     * <p>rete.cpp:4234:copy_rhs_value_and_substitute_varnames
     * 
     * <p>TODO This function doesn't belong here. It creates a circular dependency with the rete package
     * <p>TODO This function should be polymorphic on RhsValue
     * 
     * @param rete
     * @param rv
     * @param cond
     * @param first_letter
     * @return
     */
    public static RhsValue copy_rhs_value_and_substitute_varnames(Rete rete, RhsValue rv, Condition cond,
            char first_letter)
    {
        final ReteLocation rl = rv.asReteLocation();
        if (rl != null)
        {
            SymbolImpl sym = Rete.var_bound_in_reconstructed_conds(cond, rl.getFieldNum(), rl.getLevelsUp());
            return sym.toRhsValue();
        }

        final UnboundVariable uv = rv.asUnboundVariable();
        if (uv != null)
        {
            final int index = uv.getIndex();
            SymbolImpl sym = rete.getRhsVariableBinding(index);
            if (sym == null)
            {
                sym = rete.variableGenerator.generate_new_variable(Character.toString(uv.getFirstLetter()));
                rete.setRhsVariableBinding(index, sym);
            }
            return sym.toRhsValue();
        }

        final RhsFunctionCall fc = rv.asFunctionCall();
        if (fc != null)
        {

            final RhsFunctionCall newFc = new RhsFunctionCall(fc.getName(), fc.isStandalone());
            for (RhsValue c : fc.getArguments())
            {
                newFc.addArgument(copy_rhs_value_and_substitute_varnames(rete, c, cond, first_letter));
            }

            return newFc;
        }

        return rv.asSymbolValue().getSym().toRhsValue();
    }

}
