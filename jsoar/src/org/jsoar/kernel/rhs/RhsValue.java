/*
 * (c) 2008  Dave Ray
 *
 * Created on Aug 16, 2008
 */
package org.jsoar.kernel.rhs;

import java.util.LinkedList;

import org.jsoar.kernel.lhs.Condition;
import org.jsoar.kernel.rete.Rete;
import org.jsoar.kernel.symbols.Symbol;
import org.jsoar.kernel.symbols.Variable;

public abstract class RhsValue
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

    public RhsValue copy()
    {
        return this;
    }
    
    public char getFirstLetter()
    {
        return '*';
    }

    public abstract void addAllVariables(int tc_number, LinkedList<Variable> var_list);
    
    

    /**
     * When we print a production (but not when we fire one), we have to
     * reconstruct the RHS actions. This is because many of the variables in the
     * RHS have been replaced by references to Rete locations (i.e., rather than
     * specifying <v>, we specify "value field 3 levels up" or "the 7th RHS
     * unbound variable". The routines below copy rhs_value's and actions, and
     * substitute variable names for such references. For RHS unbound variables,
     * we gensym new variable names.
     * 
     * rete.cpp:4234:copy_rhs_value_and_substitute_varnames
     * 
     * TODO This function doesn't belong here. It creates a circular dependency with the rete package
     * TODO This function should be polymorphic on RhsValue
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
            Symbol sym = rete.var_bound_in_reconstructed_conds(cond, rl.getFieldNum(), rl.getLevelsUp());
            return new RhsSymbolValue(sym);
        }

        final UnboundVariable uv = rv.asUnboundVariable();
        if (uv != null)
        {
            final int index = uv.getIndex();
            Symbol sym = null;
            if (rete.rhs_variable_bindings[index] == null)
            {
                sym = rete.variableGenerator.generate_new_variable(Character.toString(uv.getFirstLetter()));
                rete.rhs_variable_bindings[index] = sym;

                if (rete.highest_rhs_unboundvar_index < index)
                {
                    rete.highest_rhs_unboundvar_index = index;
                }
            }
            else
            {
                sym = rete.rhs_variable_bindings[index];
            }
            return new RhsSymbolValue(sym);
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

        return new RhsSymbolValue(rv.asSymbolValue().getSym());
    }

}
