/*
 * (c) 2008  Dave Ray
 *
 * Created on Aug 15, 2008
 */
package org.jsoar.kernel;

import org.jsoar.kernel.memory.Wme;
import org.jsoar.kernel.rete.Instantiation;
import org.jsoar.kernel.rete.ReteNode;
import org.jsoar.kernel.rete.Token;
import org.jsoar.kernel.symbols.Identifier;
import org.jsoar.util.AsListItem;

/**
 * explain.h:81
 * 
 * @author ray
 */
public class MatchSetChange
{
    public final  AsListItem<MatchSetChange> next_prev = new AsListItem<MatchSetChange>(this); // dll for all p nodes
    public final AsListItem<MatchSetChange> of_node = new AsListItem<MatchSetChange>(this); // dll for just this p node
    public ReteNode p_node; // for retractions, this can be null if the p node has been excised
    public Token tok; // for assertions only
    public Wme w; // for assertions only
    
    
    public Instantiation inst;   // for retractions only
  /* REW: begin 08.20.97 */
    public Identifier goal;
    public int level;              // Level of the match of the assertion or retraction
    public final AsListItem<MatchSetChange> in_level = new AsListItem<MatchSetChange>(this); // dll for goal level
  /* REW: end   08.20.97 */

    /**
     * rete.cpp:1011:find_goal_for_match_set_change_assertion
     * @param msc
     * @return
     */
    public Identifier find_goal_for_match_set_change_assertion(Token dummy_top_token) {

//      #ifdef DEBUG_WATERFALL
//        print_with_symbols(thisAgent, "\nMatch goal for assertion: %y", msc->p_node->b.p.prod->name); 
//      #endif


        Wme lowest_goal_wme = null;
        int lowest_level_so_far = -1;

        if (this.w != null) {
            if (this.w.id.isa_goal) {
              lowest_goal_wme = this.w;
              lowest_level_so_far = this.w.id.level;
            }
        }

        for (Token tok=this.tok; tok!=dummy_top_token; tok=tok.parent) {
          if (tok.w != null) {
            /* print_wme(tok->w); */
            if (tok.w.id.isa_goal) {

              if (lowest_goal_wme == null)
                lowest_goal_wme = tok.w;
              
              else {
                if (tok.w.id.level > lowest_goal_wme.id.level)
                  lowest_goal_wme = tok.w;
              }
            }
             
          }
        } 

        if (lowest_goal_wme != null) {
//      #ifdef DEBUG_WATERFALL
//          print_with_symbols(thisAgent, " is [%y]", lowest_goal_wme->id);
//      #endif
             return lowest_goal_wme.id;
        }
//        { 
            throw new IllegalStateException("\nError: Did not find goal for ms_change assertion: " + this.p_node.b_p.prod.name);
            // TODO: Fatal Error
//            char msg[BUFFER_MSG_SIZE];
//        print_with_symbols(thisAgent, "\nError: Did not find goal for ms_change assertion: %y\n", msc->p_node->b.p.prod->name);
//        SNPRINTF(msg, BUFFER_MSG_SIZE,"\nError: Did not find goal for ms_change assertion: %s\n",
//               symbol_to_string(thisAgent, msc->p_node->b.p.prod->name,TRUE,NIL, 0));
//        msg[BUFFER_MSG_SIZE - 1] = 0; /* ensure null termination */
//        abort_with_fatal_error(thisAgent, msg);
//        }
//        return 0;
      }
    
    /**
     * rete.cpp:1065:find_goal_for_match_set_change_retraction
     * 
     * @param msc
     * @return
     */
    public Identifier find_goal_for_match_set_change_retraction()
    {
        // #ifdef DEBUG_WATERFALL
        // print_with_symbols(thisAgent, "\nMatch goal level for retraction:
        // %y", msc->inst->prod->name);
        // #endif

        if (this.inst.match_goal != null)
        {
            // If there is a goal, just return the goal
            // #ifdef DEBUG_WATERFALL
            // print_with_symbols(thisAgent, " is [%y]", msc->inst->match_goal);
            // #endif
            return this.inst.match_goal;
        }
        else
        {
            // #ifdef DEBUG_WATERFALL
            // print(" is NIL (nil goal retraction)");
            //        #endif 
            return null;
        }
    }
}
