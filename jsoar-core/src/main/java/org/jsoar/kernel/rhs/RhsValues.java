/*
 * (c) 2010  Dave Ray
 *
 * Created on May 18, 2010
 */
package org.jsoar.kernel.rhs;

import org.jsoar.kernel.lhs.Condition;
import org.jsoar.kernel.rete.Rete;
import org.jsoar.kernel.symbols.SymbolImpl;

/**
 * Helper methods associated with {@link RhsValue} interface
 * 
 * @author ray
 */
public class RhsValues
{
    /**
     * When we print a production (but not when we fire one), we have to
     * reconstruct the RHS actions. This is because many of the variables in the
     * RHS have been replaced by references to Rete locations (i.e., rather than
     * specifying {@code <v>}, we specify "value field 3 levels up" or "the 7th RHS
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
     * @return new RHS value object
     */
    public static RhsValue copy_rhs_value_and_substitute_varnames(Rete rete, RhsValue rv, Condition cond,
            char first_letter)
    {
        final ReteLocation rl = rv.asReteLocation();
        if(rl != null)
        {
            SymbolImpl sym = Rete.var_bound_in_reconstructed_conds(cond, rl.getFieldNum(), rl.getLevelsUp());
            return sym.toRhsValue();
        }
        
        final UnboundVariable uv = rv.asUnboundVariable();
        if(uv != null)
        {
            final int index = uv.getIndex();
            SymbolImpl sym = rete.getRhsVariableBinding(index);
            if(sym == null)
            {
                sym = rete.getSymbols().getVariableGenerator().generate_new_variable(Character.toString(uv.getFirstLetter()));
                rete.setRhsVariableBinding(index, sym);
            }
            return sym.toRhsValue();
        }
        
        final RhsFunctionCall fc = rv.asFunctionCall();
        if(fc != null)
        {
            
            final RhsFunctionCall newFc = new RhsFunctionCall(fc.getName(), fc.isStandalone());
            for(RhsValue c : fc.getArguments())
            {
                newFc.addArgument(copy_rhs_value_and_substitute_varnames(rete, c, cond, first_letter));
            }
            
            return newFc;
        }
        
        return rv.asSymbolValue().getSym().toRhsValue();
    }
    
}
