/*
 * Copyright (c) 2008  Dave Ray <daveray@gmail.com>
 *
 * Created on Aug 27, 2008
 */
package org.jsoar.kernel.rete;

import org.jsoar.kernel.lhs.Condition;
import org.jsoar.kernel.lhs.ConjunctiveNegationCondition;
import org.jsoar.kernel.lhs.NegativeCondition;
import org.jsoar.kernel.lhs.PositiveCondition;
import org.jsoar.kernel.lhs.ThreeFieldCondition;
import org.jsoar.kernel.symbols.Variable;
import org.jsoar.util.ListHead;

/**
 * <p>rete.cpp:2508
 * <p>rete.cpp:2553:deallocate_node_varnames - not needed in Java
 * 
 * @author ray
 */
public class NodeVarNames
{
    public static class ThreeFieldVarNames
    {
        Object id_varnames;
        Object attr_varnames;
        Object value_varnames;
        
        /* (non-Javadoc)
         * @see java.lang.Object#toString()
         */
        @Override
        public String toString()
        {
            return "<" + id_varnames + ", " + attr_varnames + ", " + value_varnames + ">";
        }
    }
    
    final NodeVarNames parent;
    //union varname_data_union {
    final ThreeFieldVarNames fields = new ThreeFieldVarNames(); // TODO: Only allocate for non-CN_BNODE
    NodeVarNames bottom_of_subconditions;
    //} data;
    
    public NodeVarNames(NodeVarNames parent)
    {
        this.parent = parent;
    }
    
    /**
     * rete.cpp:2611:make_nvn_for_posneg_cond
     * 
     * @param cond
     * @param parent_nvn
     * @return
     */
    static NodeVarNames make_nvn_for_posneg_cond(ThreeFieldCondition cond, NodeVarNames parent_nvn)
    {
        NodeVarNames New = new NodeVarNames(parent_nvn);
        ListHead<Variable> vars_bound = ListHead.newInstance();

        /* --- fill in varnames for id test --- */
        New.fields.id_varnames = VarNames.add_unbound_varnames_in_test(cond.id_test, null);

        /* --- add sparse bindings for id, then get attr field varnames --- */
        Rete.bind_variables_in_test(cond.id_test, 0, 0, false, vars_bound);
        New.fields.attr_varnames = VarNames.add_unbound_varnames_in_test(cond.attr_test, null);

        /* --- add sparse bindings for attr, then get value field varnames --- */
        Rete.bind_variables_in_test(cond.attr_test, 0, 0, false, vars_bound);
        New.fields.value_varnames = VarNames.add_unbound_varnames_in_test(cond.value_test, null);

        /* --- Pop the variable bindings for these conditions --- */
        Rete.pop_bindings_and_deallocate_list_of_variables(vars_bound);

        return New;
    }  

    
    /**
     * rete.cpp:2642:get_nvn_for_condition_list
     * 
     * @param cond_list
     * @param parent_nvn
     * @return
     */
    static NodeVarNames get_nvn_for_condition_list(Condition cond_list, NodeVarNames parent_nvn)
    {
        NodeVarNames New = null;
        ListHead<Variable> vars = ListHead.newInstance();

        for (Condition cond = cond_list; cond != null; cond = cond.next)
        {
            PositiveCondition pc = cond.asPositiveCondition();
            if (pc != null)
            {
                New = make_nvn_for_posneg_cond(pc, parent_nvn);

                /* --- Add sparse variable bindings for this condition --- */
                Rete.bind_variables_in_test(pc.id_test, 0, 0, false, vars);
                Rete.bind_variables_in_test(pc.attr_test, 0, 0, false, vars);
                Rete.bind_variables_in_test(pc.value_test, 0, 0, false, vars);

            }
            NegativeCondition nc = cond.asNegativeCondition();
            if (nc != null)
            {
                New = make_nvn_for_posneg_cond(nc, parent_nvn);

            }
            ConjunctiveNegationCondition ncc = cond.asConjunctiveNegationCondition();
            if (ncc != null)
            {
                New = new NodeVarNames(parent_nvn);
                New.bottom_of_subconditions = get_nvn_for_condition_list(ncc.top, parent_nvn);
            }

            parent_nvn = New;
        }

        /* --- Pop the variable bindings for these conditions --- */
        Rete.pop_bindings_and_deallocate_list_of_variables(vars);

        return parent_nvn;
    }

    /* (non-Javadoc)
     * @see java.lang.Object#toString()
     */
    @Override
    public String toString()
    {
        return fields + "/" + bottom_of_subconditions;
    }
}
