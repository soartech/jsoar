/*
 * (c) 2008  Dave Ray
 *
 * Created on Aug 15, 2008
 */
package org.jsoar.kernel;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicLong;

import org.jsoar.kernel.lhs.Condition;
import org.jsoar.kernel.lhs.ConditionReorderer;
import org.jsoar.kernel.lhs.Conditions;
import org.jsoar.kernel.memory.Instantiation;
import org.jsoar.kernel.rete.ConditionsAndNots;
import org.jsoar.kernel.rete.PartialMatches;
import org.jsoar.kernel.rete.Rete;
import org.jsoar.kernel.rete.ReteNode;
import org.jsoar.kernel.rhs.Action;
import org.jsoar.kernel.rhs.ActionReorderer;
import org.jsoar.kernel.rhs.ActionSupport;
import org.jsoar.kernel.rhs.ReordererException;
import org.jsoar.kernel.symbols.SymbolImpl;
import org.jsoar.kernel.symbols.Variable;
import org.jsoar.kernel.symbols.VariableGenerator;
import org.jsoar.kernel.tracing.Printer;
import org.jsoar.kernel.tracing.Trace.WmeTraceType;
import org.jsoar.util.Arguments;
import org.jsoar.util.ByRef;
import org.jsoar.util.SourceLocation;
import org.jsoar.util.StringTools;
import org.jsoar.util.markers.DefaultMarker;
import org.jsoar.util.markers.Marker;

public class Production
{
    private static final List<Variable> EMPTY_RHS_UNBOUND_VARS_LIST = Collections.emptyList();
    
    public enum AssertListType
    {
        O_LIST, // 0     /* moved here from soarkernel.h.  only used in rete.cpp */
        I_LIST  // 1     /*   values for prod->OPERAND_which_assert_list */
    }
    
    private final ProductionType type;
    private final SourceLocation location;
    private final String name;
    private final String documentation;
    public Condition condition_list;
    private Condition bottomOfConditionList;
    public Action action_list;
    public ProductionSupport declared_support = ProductionSupport.UNDECLARED;

    private final AtomicBoolean breakpointEnabled = new AtomicBoolean();
    private final AtomicBoolean breakOnFire = new AtomicBoolean();
    
    private final AtomicLong firingCount = new AtomicLong(0);
    private final AtomicBoolean traceFirings = new AtomicBoolean();
    
    private Rete rete;
    private ReteNode p_node;
    /**
     * List of instantiations of this production. Use {@link Instantiation#nextInProdList}
     * to iterate.
     * 
     * @see Instantiation#insertAtHeadOfProdList(Instantiation)
     * @see Instantiation#removeFromProdList(Instantiation)
     */
    public Instantiation instantiations;
    private List<Variable> rhs_unbound_variables = null;
    public boolean already_fired = false; /* RPM test workaround for bug #139 */
    public AssertListType OPERAND_which_assert_list = AssertListType.O_LIST;
    private int reference_count = 1;
    
    private boolean reordered = false;
    
    public boolean rl_rule = false;                 /* if true, is a Soar-RL rule */
    public double rl_update_count;       /* number of (potentially fractional) updates to this rule */
    public Condition rl_template_conds;  // RL-9.3.0
    public Set<Map<SymbolImpl, SymbolImpl>> rl_template_instantiations; // RL-9.3.0
    public double rl_ecr; // RL-9.3.0
    public double rl_efr; // RL-9.3.0
    
    /**
     * Function introduced while trying to tease apart production construction
     * 
     * <p>production.cpp:1507:make_production
     * 
     * @param type The type of production
     * @param name The name of the production
     * @param lhs_top_in Top of LHS conditions
     * @param lhs_bottom_in Bottom of LHS conditions
     * @param rhs_top_in Top of RHS actions
     */
    public Production(ProductionType type, SourceLocation location, String name, String doc,
                      Condition lhs_top_in, Condition lhs_bottom_in, Action rhs_top_in)
    {
        Arguments.checkNotNull(type, "type");
        Arguments.checkNotNull(location, "location");
        Arguments.checkNotNull(name, "name");
        Arguments.checkNotNull(lhs_top_in, "lhs_top_in");
        Arguments.checkNotNull(lhs_bottom_in, "lhs_bottom_in");
        
        this.type = type;
        this.location = location;
        this.name = name;
        this.documentation = doc;
        this.p_node = null; // it's not in the Rete yet
        this.condition_list = lhs_top_in;
        this.bottomOfConditionList = lhs_bottom_in;
        this.action_list = rhs_top_in;
    }
    
    /**
     * @return the type of this production
     */
    public ProductionType getType()
    {
        return type;
    }
    
    /**
     * @return the location
     */
    public SourceLocation getLocation()
    {
        return location;
    }

    /**
     * @return the name of this production
     */
    public String getName()
    {
        return name;
    }

    /**
     * @return the documentation string of this production
     */
    public String getDocumentation()
    {
        return documentation != null ? documentation : "";
    }
    
    /**
     * @return the current firing count of this production
     */
    public long getFiringCount()
    {
        return firingCount.get();
    }
    
    /**
     * Reset the production's firing count to {@code 0}.
     */
    public void resetFiringCount()
    {
        this.firingCount.set(0);
    }
    
    /**
     * Increment the production's firing count and return the new count.
     * 
     * @return the new firing count
     */
    public long incrementFiringCount()
    {
        return this.firingCount.incrementAndGet();
    }
        
    public boolean isBreakpointEnabled()
    {
        return breakpointEnabled.get();
    }
    
    public void setBreakpointEnabled(boolean v)
    {
        breakpointEnabled.set(v);
    }
    
    /**
     * @return true if firings of this rule should be traced
     */
    public boolean isTraceFirings()
    {
        return traceFirings.get();
    }
    
    /**
     * Set whether firings of this rule should be traced.
     * 
     * @param value new value
     */
    public void setTraceFirings(boolean value)
    {
        traceFirings.set(value);
    }

    /**
     * Print partial match information for this production to the given printer
     * 
     * @param printer The printer to print to
     * @param wtt The WME trace level
     */
    public void printPartialMatches(Printer printer, WmeTraceType wtt)
    {
        if(rete == null)
        {
            return;
        }
        
        rete.print_partial_match_information(printer, p_node, wtt);
    }
    
    /**
     * Returns a structured partial matches for this rule
     * 
     * @return partial matches
     */
    public PartialMatches getPartialMatches()
    {
        if(rete == null)
        {
            return new PartialMatches(new ArrayList<PartialMatches.Entry>());
        }
        return rete.getPartialMatches(p_node);
    }
    
    /**
     * Returns a count of the number of tokens currently in use for this
     * production. The count does not include:
     * <ul>
     * <li> tokens in the p_node (i.e., tokens representing complete matches) 
     * <li>local join result tokens on (real) tokens in negative/NCC nodes
     * </ul>
     * 
     * <p>Note that this method is not fast for large match sets
     * 
     * @return token count, or 0 if not in rete
     */
    public int getReteTokenCount()
    {
        return rete != null ? rete.count_rete_tokens_for_production(p_node) : 0;
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
        if (type != ProductionType.JUSTIFICATION)
        {
            ByRef<Condition> lhs_top = ByRef.create(condition_list);
            ByRef<Condition> lhs_bottom = ByRef.create(bottomOfConditionList);
            ByRef<Action> rhs_top = ByRef.create(action_list);
            // ??? thisAgent->name_of_production_being_reordered =
            // name->sc.name;

            varGen.reset(lhs_top.value, rhs_top.value);
            Marker tc = DefaultMarker.create();
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
            if (instantiations != null)
            {
                throw new IllegalStateException("Internal error: deallocating prod. that still has inst's");
            }
        }
    }
    
    public List<Variable> getRhsUnboundVariables()
    {
        return rhs_unbound_variables != null ? rhs_unbound_variables : EMPTY_RHS_UNBOUND_VARS_LIST;
    }
    
    public void clearRhsUnboundVariables()
    {
        rhs_unbound_variables = null;
    }
    
    /**
     * Set the RHS unbound variables of the production. This method takes
     * ownership of the passed in list rather than copying it!
     * 
     * @param unboundVars List of unbound vars 
     */
    public void setRhsUnboundVariables(List<Variable> unboundVars)
    {
        rhs_unbound_variables = unboundVars;
    }
    
    /**
     * Set this productions rete node. This should only be called by the rete.
     * 
     * @param rete
     * @param p_node
     */
    public void setReteNode(Rete rete, ReteNode p_node)
    {
        if((this.rete != null || this.p_node != null) && (rete != null || p_node != null))
        {
            throw new IllegalStateException("Production " + this + " is already in rete");
        }
        
        this.rete = rete;
        this.p_node = p_node;
    }
    
    /**
     * Get this production's rete node. This is not a public API.
     * 
     * @return This production's rete node.
     */
    public ReteNode getReteNode()
    {
        return p_node;
    }
    
    /* (non-Javadoc)
     * @see java.lang.Object#toString()
     */
    @Override
    public String toString()
    {
        return name.toString() + " (" + type + ") " + firingCount;
    }
    
    /**
     * This prints a production.  The "internal" parameter, if TRUE,
     * indicates that the LHS and RHS should be printed in internal format.
     * 
     * <p>print.cpp:762:print_production
     * 
     * @param printer The printer to print to
     * @param internal true for internal representation, false otherwise
     */
    public void print(Printer printer, boolean internal)
    {
        if(rete == null || p_node == null)
        {
            printer.print("%s has been excised", this.name);
            return;
        }
        
        // print "sp" and production name
        printer.print("sp {%s\n", this.name);

        // print optional documentation string
        if (documentation != null && documentation.length() > 0)
        {
            printer.print("    %s\n", StringTools.string_to_escaped_string(documentation, '"'));
        }

        // print any flags
        switch (type)
        {
        case DEFAULT:
            printer.print("    :default\n");
            break;
        case USER:
            break;
        case CHUNK:
            printer.print("    :chunk\n");
            break;
        case JUSTIFICATION:
            printer.print("    :justification ;# not reloadable\n");
            break;
        case TEMPLATE:
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

        Conditions.print_condition_list(printer, cns.top, 3, internal);

        printer.print("\n    -->\n  ");
        printer.print("  ");
        Action.print_action_list(printer, cns.actions, 4, internal);
        printer.print("\n}\n").flush();
    } 
    
}
