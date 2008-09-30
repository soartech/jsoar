/*
 * (c) 2008  Dave Ray
 *
 * Created on Aug 15, 2008
 */
package org.jsoar.kernel;

import java.util.LinkedList;

import org.jsoar.kernel.lhs.Condition;
import org.jsoar.kernel.lhs.ConditionReorderer;
import org.jsoar.kernel.rete.Instantiation;
import org.jsoar.kernel.rete.ReteNode;
import org.jsoar.kernel.rhs.Action;
import org.jsoar.kernel.rhs.ActionReorderer;
import org.jsoar.kernel.rhs.ActionSupport;
import org.jsoar.kernel.rhs.ReordererException;
import org.jsoar.kernel.symbols.SymConstant;
import org.jsoar.kernel.symbols.Variable;
import org.jsoar.util.Arguments;
import org.jsoar.util.ByRef;
import org.jsoar.util.ListHead;

public class Production
{
    public final ProductionType type;
    public final SymConstant name;
    public String documentation;
    public Condition condition_list;
    private Condition bottomOfConditionList;
    public Action action_list;
    public ProductionSupport declared_support = ProductionSupport.UNDECLARED_SUPPORT;
    public boolean interrupt = false;
    public int firing_count = 0;
    public boolean trace_firings = false;
    public ReteNode p_node;
    public final ListHead<Instantiation> instantiations = ListHead.newInstance();
    public final LinkedList<Variable> rhs_unbound_variables = new LinkedList<Variable>();
    public boolean already_fired = false; /* RPM test workaround for bug #139 */
    public AssertListType OPERAND_which_assert_list = AssertListType.O_LIST;
    public int reference_count = 1;
    
    private boolean reordered = false;
    

    /**
     * Function introduced while trying to tease apart production construction
     * 
     * production.cpp:1507:make_production
     * 
     * @param p
     */
    public Production(ProductionType type, SymConstant name,
                      Condition lhs_top_in, Condition lhs_bottom_in, Action rhs_top_in)
    {
        Arguments.checkNotNull(type, "type");
        Arguments.checkNotNull(name, "name");
        Arguments.checkNotNull(lhs_top_in, "lhs_top_in");
        Arguments.checkNotNull(lhs_bottom_in, "lhs_bottom_in");
        
        if(name.production != null)
        {
            throw new IllegalStateException("Internal error: Production with name '" + name + "' already exists");
        }
        
        this.type = type;
        this.name = name;
        name.production = this;
        // TODO insert_at_head_of_dll (thisAgent->all_productions_of_type[type], p, next, prev);
        // TODO thisAgent->num_productions_of_type[type]++;
        this.p_node = null;               /* it's not in the Rete yet */
        
        this.condition_list = lhs_top_in;
        this.bottomOfConditionList = lhs_bottom_in;
        this.action_list = rhs_top_in;
    }
    
    /**
     * Performs reordering of the LHS and RHS of the production using the given
     * reorderer objects. This will modify the conditions and actions of the
     * production. 
     * 
     * <p>Function introduced while trying to tease apart production construction
     * 
     * production.cpp:1507:make_production
     * 
     * @param varGen A variable generator
     * @param cr A condition reorderer
     * @param ar An action reorderer
     * @param reorder_nccs True if NCCs should be reordered.
     * @throws ReordererException 
     * @throws IllegalStateException if the production has already been reordered
     */
    public void reorder(VariableGenerator varGen, ConditionReorderer cr, ActionReorderer ar, boolean reorder_nccs) throws ReordererException
    {
        if (reordered)
        {
            throw new IllegalStateException("Production '" + name + "' already reordered");
        }
        if (type != ProductionType.JUSTIFICATION_PRODUCTION_TYPE)
        {
            ByRef<Condition> lhs_top = ByRef.create(condition_list);
            ByRef<Condition> lhs_bottom = ByRef.create(bottomOfConditionList);
            ByRef<Action> rhs_top = ByRef.create(action_list);
            // ??? thisAgent->name_of_production_being_reordered =
            // name->sc.name;

            varGen.reset(lhs_top.value, rhs_top.value);
            int tc = varGen.getSyms().get_new_tc_number();
            Condition.addBoundVariables(lhs_top.value, tc, null);

            ar.reorder_action_list(rhs_top, tc);
            cr.reorder_lhs(lhs_top, lhs_bottom, reorder_nccs);

            // TODO: Is this necessary since this is the default value?
            for (Action a = rhs_top.value; a != null; a = a.next)
            {
                a.support = ActionSupport.UNKNOWN_SUPPORT;
            }

            this.condition_list = lhs_top.value;
            this.action_list = rhs_top.value;
        }
        else
        {
            /* --- for justifications --- */
            /* force run-time o-support (it'll only be done once) */

            // TODO: Is this necessary since this is the default value?
            for (Action a = action_list; a != null; a = a.next)
            {
                a.support = ActionSupport.UNKNOWN_SUPPORT;
            }
        }

        reordered = true;
    }
    
    /**
     * @param b
     */
    public void excise_production(boolean b)
    {
        // TODO implement excise_production
        throw new UnsupportedOperationException("excise_production not implemented");
        
    }

    /* (non-Javadoc)
     * @see java.lang.Object#toString()
     */
    @Override
    public String toString()
    {
        return name.toString() + " (" + type + ")";
    }
    
 
    
}
