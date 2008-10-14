/*
 * (c) 2008  Dave Ray
 *
 * Created on Aug 15, 2008
 */
package org.jsoar.kernel;

import org.jsoar.kernel.symbols.SymConstant;
import org.jsoar.kernel.symbols.SymbolFactory;
import org.jsoar.kernel.symbols.Variable;

/**
 * @author ray
 */
public class PredefinedSymbols
{
    private final SymbolFactory syms = new SymbolFactory();
    
    final SymConstant problem_space_symbol = syms.make_sym_constant("problem-space");
    public final SymConstant state_symbol = syms.make_sym_constant("state");
    public final SymConstant operator_symbol = syms.make_sym_constant("operator");
    public final SymConstant superstate_symbol = syms.make_sym_constant("superstate");
    public SymConstant io_symbol = syms.make_sym_constant("io");
    final SymConstant object_symbol = syms.make_sym_constant("object");
    public final SymConstant attribute_symbol = syms.make_sym_constant("attribute");
    final SymConstant impasse_symbol = syms.make_sym_constant("impasse");
    final SymConstant choices_symbol = syms.make_sym_constant("choices");
    final SymConstant none_symbol = syms.make_sym_constant("none");
    final SymConstant constraint_failure_symbol = syms.make_sym_constant("constraint-failure");
    final SymConstant no_change_symbol = syms.make_sym_constant("no-change");
    final SymConstant multiple_symbol = syms.make_sym_constant("multiple");
    
    final SymConstant item_count_symbol = syms.make_sym_constant("item-count");

    final SymConstant conflict_symbol = syms.make_sym_constant("conflict");
    final SymConstant tie_symbol = syms.make_sym_constant("tie");
    final SymConstant item_symbol = syms.make_sym_constant("item");
    public final SymConstant quiescence_symbol = syms.make_sym_constant("quiescence");
    public final SymConstant t_symbol = syms.make_sym_constant("t");
    public final SymConstant nil_symbol = syms.make_sym_constant("nil");
    final SymConstant type_symbol = syms.make_sym_constant("type");
    final SymConstant goal_symbol = syms.make_sym_constant("goal");
    public final SymConstant name_symbol = syms.make_sym_constant("name");

    final Variable ts_context_variable = syms.make_variable ("<ts>");
    final Variable to_context_variable = syms.make_variable ("<to>");
    final Variable sss_context_variable = syms.make_variable ("<sss>");
    final Variable sso_context_variable = syms.make_variable ("<sso>");
    final Variable ss_context_variable = syms.make_variable ("<ss>");
    final Variable so_context_variable = syms.make_variable ("<so>");
    final Variable s_context_variable = syms.make_variable ("<s>");
    final Variable o_context_variable = syms.make_variable ("<o>");

    final Variable wait_symbol = syms.make_variable ("wait");

    public final SymConstant input_link_symbol = syms.make_sym_constant("input-link");
    public final SymConstant output_link_symbol = syms.make_sym_constant("output-link");

    final SymConstant reward_link_symbol = syms.make_sym_constant("reward-link" );

    /**
     * @return the symbol factory
     */
    public SymbolFactory getSyms()
    {
        return syms;
    }

    
}
