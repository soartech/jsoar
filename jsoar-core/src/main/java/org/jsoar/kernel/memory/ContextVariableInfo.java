/*
 * Copyright (c) 2008  Dave Ray <daveray@gmail.com>
 *
 * Created on Oct 28, 2008
 */
package org.jsoar.kernel.memory;

import org.jsoar.kernel.Agent;
import org.jsoar.kernel.PredefinedSymbols;
import org.jsoar.kernel.symbols.Identifier;
import org.jsoar.kernel.symbols.IdentifierImpl;
import org.jsoar.kernel.symbols.Symbol;
import org.jsoar.kernel.symbols.Variable;

/**
 * Return structure used by {@link Agent#getContextVariableInfo(String)}
 * 
 * <p>utilities.cpp:132:get_context_var_info
 * 
 * @author ray
 */
public class ContextVariableInfo
{
    private final Identifier goal;
    private final Symbol attribute;
    private final Symbol value;
    
    /**
     * <p>Note: client code should use {@link Agent#getContextVariableInfo(String)}
     * 
     * <p>utilities.cpp:132:get_context_var_info
     * 
     * @param predefinedSyms
     * @param top_goal
     * @param bottom_goal
     * @param variable
     * @return information about the given context variable
     * @see Agent#getContextVariableInfo(String)
     */
    public static ContextVariableInfo get(PredefinedSymbols predefinedSyms, IdentifierImpl top_goal, IdentifierImpl bottom_goal, String variable)
    {
        Symbol attribute = null;
        int levels_up = 0;

        Variable v = predefinedSyms.getSyms().find_variable(variable);
        if (v == predefinedSyms.s_context_variable)
        {
            levels_up = 0;
            attribute = predefinedSyms.state_symbol;
        }
        else if (v == predefinedSyms.o_context_variable)
        {
            levels_up = 0;
            attribute = predefinedSyms.operator_symbol;
        }
        else if (v == predefinedSyms.ss_context_variable)
        {
            levels_up = 1;
            attribute = predefinedSyms.state_symbol;
        }
        else if (v == predefinedSyms.so_context_variable)
        {
            levels_up = 1;
            attribute = predefinedSyms.operator_symbol;
        }
        else if (v == predefinedSyms.sss_context_variable)
        {
            levels_up = 2;
            attribute = predefinedSyms.state_symbol;
        }
        else if (v == predefinedSyms.sso_context_variable)
        {
            levels_up = 2;
            attribute = predefinedSyms.operator_symbol;
        }
        else if (v == predefinedSyms.ts_context_variable)
        {
            levels_up = top_goal != null ? bottom_goal.level - top_goal.level
                    : 0;
            attribute = predefinedSyms.state_symbol;
        }
        else if (v == predefinedSyms.to_context_variable)
        {
            levels_up = top_goal != null ? bottom_goal.level - top_goal.level
                    : 0;
            attribute = predefinedSyms.operator_symbol;
        }
        else
        {
            return new ContextVariableInfo(null, null, null);
        }

        IdentifierImpl g = bottom_goal;
        while (g != null && levels_up != 0)
        {
            g = g.isa_goal.higher_goal;
            levels_up--;
        }

        if (g == null)
        {
            return new ContextVariableInfo(g, attribute, null);
        }

        Symbol value = null;
        if (attribute == predefinedSyms.state_symbol)
        {
            value = g;
        }
        else
        {
            WmeImpl w = g.isa_goal.operator_slot.getWmes();
            value = w != null ? w.getValue() : null;
        }
        return new ContextVariableInfo(g, attribute, value);

    }
    
    /**
     * @param goal
     * @param attribute
     * @param value
     */
    private ContextVariableInfo(Identifier goal, Symbol attribute, Symbol value)
    {
        this.goal = goal;
        this.attribute = attribute;
        this.value = value;
    }

    /**
     * @return the goal
     */
    public Identifier getGoal()
    {
        return goal;
    }

    /**
     * @return the attribute
     */
    public Symbol getAttribute()
    {
        return attribute;
    }

    /**
     * @return the value
     */
    public Symbol getValue()
    {
        return value;
    }
    
    
}
