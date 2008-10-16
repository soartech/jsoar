/*
 * (c) 2008  Dave Ray
 *
 * Created on Aug 15, 2008
 */
package org.jsoar.kernel;

import java.util.LinkedList;

import org.jsoar.kernel.lhs.Condition;
import org.jsoar.kernel.lhs.ConditionReorderer;
import org.jsoar.kernel.rete.ConditionsAndNots;
import org.jsoar.kernel.rete.Instantiation;
import org.jsoar.kernel.rete.Rete;
import org.jsoar.kernel.rete.ReteNode;
import org.jsoar.kernel.rhs.Action;
import org.jsoar.kernel.rhs.ActionReorderer;
import org.jsoar.kernel.rhs.ActionSupport;
import org.jsoar.kernel.rhs.ReordererException;
import org.jsoar.kernel.symbols.StringSymbolImpl;
import org.jsoar.kernel.symbols.Variable;
import org.jsoar.kernel.tracing.Printer;
import org.jsoar.util.Arguments;
import org.jsoar.util.ByRef;
import org.jsoar.util.ListHead;
import org.jsoar.util.StringTools;

public class Production
{
    public final ProductionType type;
    public final StringSymbolImpl name;
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
    private int reference_count = 1;
    
    private boolean reordered = false;
    

    /**
     * Function introduced while trying to tease apart production construction
     * 
     * <p>production.cpp:1507:make_production
     * 
     * @param p
     */
    public Production(ProductionType type, StringSymbolImpl name,
                      Condition lhs_top_in, Condition lhs_bottom_in, Action rhs_top_in)
    {
        Arguments.checkNotNull(type, "type");
        Arguments.checkNotNull(name, "name");
        Arguments.checkNotNull(lhs_top_in, "lhs_top_in");
        Arguments.checkNotNull(lhs_bottom_in, "lhs_bottom_in");
        
        this.type = type;
        this.name = name;
        this.p_node = null; // it's not in the Rete yet
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
     * <p>production.cpp:1507:make_production
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

    public int getReferenceCount()
    {
        return reference_count;
    }
    
    /**
     * <p>production.h:380:production_add_ref
     */
    public void production_add_ref()
    {
        reference_count++;
    }

    /**
     * <p>production.h:385:production_remove_ref
     */
    public void production_remove_ref()
    {
        reference_count--;
        if (reference_count == 0)
        {
            if (!instantiations.isEmpty())
            {
                throw new IllegalStateException("Internal error: deallocating prod. that still has inst's");
            }
        }
    }
    
    /* (non-Javadoc)
     * @see java.lang.Object#toString()
     */
    @Override
    public String toString()
    {
        return name.toString() + " (" + type + ")";
    }
    
    /**
     * This prints a production.  The "internal" parameter, if TRUE,
     * indicates that the LHS and RHS should be printed in internal format.
     * 
     * <p>print.cpp:762:print_production
     * 
     * @param rete
     * @param printer
     * @param internal
     */
    public void print_production(Rete rete, Printer printer, boolean internal)
    {
        // print "sp" and production name
        printer.print("sp {%s\n", this.name);

        // print optional documentation string
        if (documentation != null)
        {
            printer.print("    %s\n", StringTools.string_to_escaped_string(documentation, '"'));
        }

        // print any flags
        switch (type)
        {
        case DEFAULT_PRODUCTION_TYPE:
            printer.print("    :default\n");
            break;
        case USER_PRODUCTION_TYPE:
            break;
        case CHUNK_PRODUCTION_TYPE:
            printer.print("    :chunk\n");
            break;
        case JUSTIFICATION_PRODUCTION_TYPE:
            printer.print("    :justification ;# not reloadable\n");
            break;
        case TEMPLATE_PRODUCTION_TYPE:
            printer.print("   :template\n");
            break;
        }

        if (declared_support == ProductionSupport.DECLARED_O_SUPPORT)
        {
            printer.print("    :o-support\n");
        }
        else if (declared_support == ProductionSupport.DECLARED_I_SUPPORT)
        {
            printer.print("    :i-support\n");
        }

        // print the LHS and RHS
        ConditionsAndNots cns = rete.p_node_to_conditions_and_nots(p_node, null, null, true);
        printer.print("   ");

        Condition.print_condition_list(printer, cns.dest_top_cond, 3, internal);

        printer.print("\n    -->\n  ");
        printer.print("  ");
        Action.print_action_list(printer, cns.dest_rhs, 4, internal);
        printer.print("\n}\n");
    } 
    
}
