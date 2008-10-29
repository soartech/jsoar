/*
 * (c) 2008  Dave Ray
 *
 * Created on Aug 15, 2008
 */
package org.jsoar.kernel;

import org.jsoar.kernel.symbols.StringSymbolImpl;
import org.jsoar.kernel.symbols.SymbolFactoryImpl;
import org.jsoar.kernel.symbols.Variable;

/**
 * @author ray
 */
public class PredefinedSymbols
{
    private final SymbolFactoryImpl syms = new SymbolFactoryImpl();
    
    final StringSymbolImpl problem_space_symbol = syms.createString("problem-space");
    public final StringSymbolImpl state_symbol = syms.createString("state");
    public final StringSymbolImpl operator_symbol = syms.createString("operator");
    public final StringSymbolImpl superstate_symbol = syms.createString("superstate");
    public StringSymbolImpl io_symbol = syms.createString("io");
    final StringSymbolImpl object_symbol = syms.createString("object");
    public final StringSymbolImpl attribute_symbol = syms.createString("attribute");
    final StringSymbolImpl impasse_symbol = syms.createString("impasse");
    final StringSymbolImpl choices_symbol = syms.createString("choices");
    final StringSymbolImpl none_symbol = syms.createString("none");
    final StringSymbolImpl constraint_failure_symbol = syms.createString("constraint-failure");
    final StringSymbolImpl no_change_symbol = syms.createString("no-change");
    final StringSymbolImpl multiple_symbol = syms.createString("multiple");
    
    final StringSymbolImpl item_count_symbol = syms.createString("item-count");

    final StringSymbolImpl conflict_symbol = syms.createString("conflict");
    final StringSymbolImpl tie_symbol = syms.createString("tie");
    final StringSymbolImpl item_symbol = syms.createString("item");
    public final StringSymbolImpl quiescence_symbol = syms.createString("quiescence");
    public final StringSymbolImpl t_symbol = syms.createString("t");
    public final StringSymbolImpl nil_symbol = syms.createString("nil");
    final StringSymbolImpl type_symbol = syms.createString("type");
    final StringSymbolImpl goal_symbol = syms.createString("goal");
    public final StringSymbolImpl name_symbol = syms.createString("name");

    public final Variable ts_context_variable = syms.make_variable ("<ts>");
    public final Variable to_context_variable = syms.make_variable ("<to>");
    public final Variable sss_context_variable = syms.make_variable ("<sss>");
    public final Variable sso_context_variable = syms.make_variable ("<sso>");
    public final Variable ss_context_variable = syms.make_variable ("<ss>");
    public final Variable so_context_variable = syms.make_variable ("<so>");
    public final Variable s_context_variable = syms.make_variable ("<s>");
    public final Variable o_context_variable = syms.make_variable ("<o>");

    final Variable wait_symbol = syms.make_variable ("wait");

    public final StringSymbolImpl input_link_symbol = syms.createString("input-link");
    public final StringSymbolImpl output_link_symbol = syms.createString("output-link");

    final StringSymbolImpl reward_link_symbol = syms.createString("reward-link" );

    /**
     * @return the symbol factory
     */
    public SymbolFactoryImpl getSyms()
    {
        return syms;
    }

    
}
