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
import org.jsoar.kernel.symbols.SymConstant;
import org.jsoar.kernel.symbols.Variable;
import org.jsoar.util.Arguments;
import org.jsoar.util.ByRef;
import org.jsoar.util.ListHead;

public class Production
{
    public ProductionType type;
    public SymConstant name;
    public String documentation;
    public Condition condition_list;
    public Action action_list;
    public ProductionSupport declared_support = ProductionSupport.UNDECLARED_SUPPORT;
    public boolean interrupt = false;
    public int firing_count = 0;
    public boolean trace_firings = false;
    public ReteNode p_node;
    public final ListHead<Instantiation> instantiations = new ListHead<Instantiation>();
    public final LinkedList<Variable> rhs_unbound_variables = new LinkedList<Variable>();
    public boolean already_fired = false; /* RPM test workaround for bug #139 */
    public AssertListType OPERAND_which_assert_list = AssertListType.O_LIST;
    /**
     * @param type
     * @param name
     * @param documentation
     * @param action_list
     */
    public Production(VariableGenerator varGen, ProductionType type, SymConstant name,
                      Condition lhs_top_in, Condition lhs_bottom_in, Action rhs_top_in, boolean reorder_nccs)
    {
        Arguments.checkNotNull(varGen, "varGen");
        Arguments.checkNotNull(type, "type");
        Arguments.checkNotNull(name, "name");
        Arguments.checkNotNull(lhs_top_in, "lhs_top_in");
        Arguments.checkNotNull(lhs_bottom_in, "lhs_bottom_in");
        Arguments.checkNotNull(rhs_top_in, "rhs_top_in");
        
        if(name.production != null)
        {
            throw new IllegalStateException("Internal error: Production with name '" + name + "' already exists");
        }
        
        this.type = type;
        this.name = name;
        
        ByRef<Condition> lhs_top = ByRef.create(lhs_top_in);
        ByRef<Condition> lhs_bottom = ByRef.create(lhs_bottom_in);
        ByRef<Action> rhs_top = ByRef.create(rhs_top_in);
        // ??? thisAgent->name_of_production_being_reordered = name->sc.name;

        if (type!=ProductionType.JUSTIFICATION_PRODUCTION_TYPE) {
          varGen.reset(lhs_top.value, rhs_top.value);
          int tc = varGen.getSyms().get_new_tc_number();
          Condition.addBoundVariables(lhs_top.value, tc, null);
          
          ActionReorderer actionReorder = new ActionReorderer(this.name.name);
          actionReorder.reorder_action_list (rhs_top, tc);
          
          ConditionReorderer conditionReorderer = new ConditionReorderer(varGen);
          conditionReorderer.reorder_lhs (lhs_top, lhs_bottom, reorder_nccs);
          
          // TODO: Is this necessary since this is the default value?
          for(Action a = rhs_top.value; a != null; a = a.next)
          {
              a.support = ActionSupport.UNKNOWN_SUPPORT;
          }
        } else {
          /* --- for justifications --- */
          /* force run-time o-support (it'll only be done once) */
            
            // TODO: Is this necessary since this is the default value?
            for(Action a = rhs_top.value; a != null; a = a.next)
            {
                a.support = ActionSupport.UNKNOWN_SUPPORT;
            }
        }

        name.production = this;
        // TODO insert_at_head_of_dll (thisAgent->all_productions_of_type[type], p, next, prev);
        // TODO thisAgent->num_productions_of_type[type]++;
        this.p_node = null;               /* it's not in the Rete yet */
        this.condition_list = lhs_top.value;
        this.action_list = rhs_top.value;
        
        // Soar-RL stuff
        // TODO p->rl_update_count = 0;
        // TODO p->rl_rule = false;
        if ( ( type != ProductionType.JUSTIFICATION_PRODUCTION_TYPE ) && ( type != ProductionType.TEMPLATE_PRODUCTION_TYPE ) ) 
        {
            // TODO p->rl_rule = rl_valid_rule( p );  
        }
        // TODO rl_update_template_tracking( thisAgent, name->sc.name );
        
        // TODO - parser.cpp
//        if ( prod_type == ProductionType.TEMPLATE_PRODUCTION_TYPE )
//        {
//            if ( !rl_valid_template( p ) )
//            {
//                print_with_symbols( thisAgent, "Invalid Soar-RL template (%y)\n\n", name );
//                excise_production( thisAgent, p, false );
//                return null;
//            }
//        }

    }
    
    
}
