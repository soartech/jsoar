/*
 * (c) 2008  Dave Ray
 *
 * Created on Aug 15, 2008
 */
package org.jsoar.kernel;

import org.jsoar.kernel.symbols.StringSymbolImpl;
import org.jsoar.kernel.symbols.SymbolFactoryImpl;
import org.jsoar.kernel.symbols.SymbolImpl;
import org.jsoar.kernel.symbols.Variable;

/**
 * @author ray
 */
public class PredefinedSymbols
{
    private final SymbolFactoryImpl syms;
    
    //final StringSymbolImpl problem_space_symbol;
    //final StringSymbolImpl goal_symbol;
    //final Variable wait_symbol;
    
    public final StringSymbolImpl state_symbol;
    public final StringSymbolImpl operator_symbol;
    public final StringSymbolImpl superstate_symbol;
    public final StringSymbolImpl attribute_symbol;
    
    // Symbols used in Decider only
    final StringSymbolImpl object_symbol;
    final StringSymbolImpl impasse_symbol;
    final StringSymbolImpl choices_symbol;
    final StringSymbolImpl none_symbol;
    final StringSymbolImpl constraint_failure_symbol;
    final StringSymbolImpl no_change_symbol;
    final StringSymbolImpl multiple_symbol;
    final StringSymbolImpl item_count_symbol;
    final StringSymbolImpl conflict_symbol;
    final StringSymbolImpl tie_symbol;
    final StringSymbolImpl item_symbol;
    final StringSymbolImpl type_symbol;

    // Symbols used in Decider and Backtracer
    public final StringSymbolImpl quiescence_symbol;
    public final StringSymbolImpl t_symbol;
    
    // Used in Decider and OSupport
    public final StringSymbolImpl nil_symbol;
    
    // Symbols used in TraceFormats only
    public final StringSymbolImpl name_symbol;

    // Symbols used in ContextVariableInfo only
    public final Variable ts_context_variable;
    public final Variable to_context_variable;
    public final Variable sss_context_variable;
    public final Variable sso_context_variable;
    public final Variable ss_context_variable;
    public final Variable so_context_variable;
    public final Variable s_context_variable;
    public final Variable o_context_variable;

    // Symbols used only in InputOutputImpl
    public final StringSymbolImpl io_symbol;
    public final StringSymbolImpl input_link_symbol;
    public final StringSymbolImpl output_link_symbol;

    // RL symbols
    public final StringSymbolImpl rl_sym_reward_link;
    public final SymbolImpl rl_sym_reward;
    public final SymbolImpl rl_sym_value;

    public PredefinedSymbols(SymbolFactoryImpl syms)
    {
        this.syms = syms;

        //problem_space_symbol = syms.createString("problem-space");
        state_symbol = syms.createString("state");
        operator_symbol = syms.createString("operator");
        superstate_symbol = syms.createString("superstate");
        io_symbol = syms.createString("io");
        object_symbol = syms.createString("object");
        attribute_symbol = syms.createString("attribute");
        impasse_symbol = syms.createString("impasse");
        choices_symbol = syms.createString("choices");
        none_symbol = syms.createString("none");
        constraint_failure_symbol = syms.createString("constraint-failure");
        no_change_symbol = syms.createString("no-change");
        multiple_symbol = syms.createString("multiple");
        
        item_count_symbol = syms.createString("item-count");

        conflict_symbol = syms.createString("conflict");
        tie_symbol = syms.createString("tie");
        item_symbol = syms.createString("item");
        quiescence_symbol = syms.createString("quiescence");
        t_symbol = syms.createString("t");
        nil_symbol = syms.createString("nil");
        type_symbol = syms.createString("type");
        //goal_symbol = syms.createString("goal");
        name_symbol = syms.createString("name");

        ts_context_variable = syms.make_variable ("<ts>");
        to_context_variable = syms.make_variable ("<to>");
        sss_context_variable = syms.make_variable ("<sss>");
        sso_context_variable = syms.make_variable ("<sso>");
        ss_context_variable = syms.make_variable ("<ss>");
        so_context_variable = syms.make_variable ("<so>");
        s_context_variable = syms.make_variable ("<s>");
        o_context_variable = syms.make_variable ("<o>");

        //wait_symbol = syms.make_variable ("wait");

        input_link_symbol = syms.createString("input-link");
        output_link_symbol = syms.createString("output-link");

        rl_sym_reward_link = syms.createString("reward-link" );
        rl_sym_reward = syms.createString("reward");
        rl_sym_value = syms.createString("value");
    }
    
    /**
     * @return the symbol factory
     */
    public SymbolFactoryImpl getSyms()
    {
        return syms;
    }

    
}
