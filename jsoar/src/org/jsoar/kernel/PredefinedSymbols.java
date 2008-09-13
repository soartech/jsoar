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
    private SymbolFactory syms = new SymbolFactory();
    
    SymConstant problem_space_symbol = syms.make_sym_constant("problem-space");
    SymConstant state_symbol = syms.make_sym_constant("state");
    public final SymConstant operator_symbol = syms.make_sym_constant("operator");
    public final SymConstant superstate_symbol = syms.make_sym_constant("superstate");
    SymConstant io_symbol = syms.make_sym_constant("io");
    SymConstant object_symbol = syms.make_sym_constant("object");
    SymConstant attribute_symbol = syms.make_sym_constant("attribute");
    SymConstant impasse_symbol = syms.make_sym_constant("impasse");
    SymConstant choices_symbol = syms.make_sym_constant("choices");
    SymConstant none_symbol = syms.make_sym_constant("none");
    SymConstant constraint_failure_symbol = syms.make_sym_constant("constraint-failure");
    SymConstant no_change_symbol = syms.make_sym_constant("no-change");
    SymConstant multiple_symbol = syms.make_sym_constant("multiple");
    
    // SBW 5/07
    SymConstant item_count_symbol = syms.make_sym_constant("item-count");

    SymConstant conflict_symbol = syms.make_sym_constant("conflict");
    SymConstant tie_symbol = syms.make_sym_constant("tie");
    SymConstant item_symbol = syms.make_sym_constant("item");
    SymConstant quiescence_symbol = syms.make_sym_constant("quiescence");
    SymConstant t_symbol = syms.make_sym_constant("t");
    public final SymConstant nil_symbol = syms.make_sym_constant("nil");
    SymConstant type_symbol = syms.make_sym_constant("type");
    SymConstant goal_symbol = syms.make_sym_constant("goal");
    public final SymConstant name_symbol = syms.make_sym_constant("name");

    Variable ts_context_variable = syms.make_variable ("<ts>");
    Variable to_context_variable = syms.make_variable ("<to>");
    Variable sss_context_variable = syms.make_variable ("<sss>");
    Variable sso_context_variable = syms.make_variable ("<sso>");
    Variable ss_context_variable = syms.make_variable ("<ss>");
    Variable so_context_variable = syms.make_variable ("<so>");
    Variable s_context_variable = syms.make_variable ("<s>");
    Variable o_context_variable = syms.make_variable ("<o>");

    /* REW: begin 10.24.97 */
    Variable wait_symbol = syms.make_variable ("wait");
    /* REW: end   10.24.97 */

    /* RPM 9/06 begin */
    SymConstant input_link_symbol = syms.make_sym_constant("input-link");
    SymConstant output_link_symbol = syms.make_sym_constant("output-link");
    /* RPM 9/06 end */

    SymConstant reward_link_symbol = syms.make_sym_constant("reward-link" );

    /**
     * @return the syms
     */
    public SymbolFactory getSyms()
    {
        return syms;
    }

    
}
