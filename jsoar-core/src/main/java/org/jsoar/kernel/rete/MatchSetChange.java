/*
 * (c) 2008  Dave Ray
 *
 * Created on Aug 15, 2008
 */
package org.jsoar.kernel.rete;

import org.jsoar.kernel.Production;
import org.jsoar.kernel.memory.Instantiation;
import org.jsoar.kernel.memory.WmeImpl;
import org.jsoar.kernel.symbols.IdentifierImpl;
import org.jsoar.util.ListItem;

/**
 * <p>explain.h:81
 * 
 * @author ray
 */
public class MatchSetChange
{
    MatchSetChange next_of_all, prev_of_all; // for all pnodes
    MatchSetChange next_of_node, prev_of_node; // for a single p node
    
    public ReteNode p_node; // for retractions, this can be null if the p node has been excised
    public final Token tok; // for assertions only
    public final WmeImpl w; // for assertions only
    
    public final Instantiation inst;   // for retractions only

    public IdentifierImpl goal;
    public int level;              // Level of the match of the assertion or retraction
    public final ListItem<MatchSetChange> in_level = new ListItem<MatchSetChange>(this); // dll for goal level

    public static MatchSetChange createAssertion(ReteNode p_node, Token tok, WmeImpl w)
    {
        return new MatchSetChange(p_node, tok, w);
    }
    
    /**
     * <p>Extracted from rete.cpp:5953
     * 
     * @param p_node the production node
     * @param inst the instantiation
     * @return a new match set change
     */
    public static MatchSetChange createRetraction(ReteNode p_node, Instantiation inst)
    {
        return new MatchSetChange(p_node, inst);
    }
    
    public static MatchSetChange createRefracted(ReteNode p_node, Instantiation inst)
    {
        return new MatchSetChange(p_node, inst);
    }
    
    private MatchSetChange(ReteNode p_node, Token tok, WmeImpl w)
    {
        assert p_node.node_type == ReteNodeType.P_BNODE;
        assert p_node.b_p() != null;
        
        this.p_node = p_node;
        this.tok = tok;
        this.w = w;
        this.inst = null;
    }
    
    private MatchSetChange(ReteNode p_node, Instantiation inst)
    {
        assert p_node.node_type == ReteNodeType.P_BNODE;
        assert p_node.b_p() != null;
        assert inst != null;
        assert inst.prod == p_node.b_p().prod;
        
        this.p_node = p_node;
        this.inst = inst;
        
        this.w = null;
        this.tok = null;
    }
    
    
    /**
     * @return the production associated with this match set change
     */
    public Production getProduction()
    {
        return inst != null ? inst.prod : p_node.b_p().prod;
    }

    /**
     * <p>rete.cpp:1011:find_goal_for_match_set_change_assertion
     * 
     * @param dummy_top_token the dummy top token of the rete
     * @return the goal
     * @throws IllegalStateException if the goal is not found
     */
    public IdentifierImpl find_goal_for_match_set_change_assertion(Token dummy_top_token) {

//      #ifdef DEBUG_WATERFALL
//        print_with_symbols(thisAgent, "\nMatch goal for assertion: %y", msc->p_node->b.p.prod->name); 
//      #endif

        WmeImpl lowest_goal_wme = null;
        //int lowest_level_so_far = -1;

        if (this.w != null) {
            if (this.w.id.isGoal()) {
              lowest_goal_wme = this.w;
              //lowest_level_so_far = this.w.id.level;
            }
        }

        for (Token tok=this.tok; tok!=dummy_top_token; tok=tok.parent) {
          if (tok.w != null) {
            /* print_wme(tok->w); */
            if (tok.w.id.isGoal()) {

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
            throw new IllegalStateException("\nError: Did not find goal for ms_change assertion: " + this.p_node.b_p().prod.getName());
      }
    
    /**
     * <p>rete.cpp:1065:find_goal_for_match_set_change_retraction
     * 
     * @return the goal or {@code null} if not found
     */
    public IdentifierImpl find_goal_for_match_set_change_retraction()
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
    
    /**
     * Insert this MSC at the head of a list of MSC in a node.
     *  
     * @param currentHead the current list head
     * @return the new list head (this)
     * @see ProductionNodeData#tentative_assertions
     * @see ProductionNodeData#tentative_retractions
     */
    MatchSetChange addToHeadOfNodeList(MatchSetChange currentHead)
    {
        next_of_node = currentHead;
        prev_of_node = null;
        if(currentHead != null)
        {
            currentHead.prev_of_node = this;
        }
        return this;
    }
    
    /**
     * Remove this MSC from a list of MSC in a node.
     *  
     * @param currentHead the current list head
     * @return the new list head
     * @see ProductionNodeData#tentative_assertions
     * @see ProductionNodeData#tentative_retractions
     */
    MatchSetChange removeFromNodeList(MatchSetChange currentHead)
    {
        if(currentHead == this)
        {
            currentHead = next_of_node;
            if(next_of_node != null)
            {
                next_of_node.prev_of_node = null;
            }
        }
        else
        {
            prev_of_node.next_of_node = this.next_of_node;
            if(this.next_of_node != null)
            {
                this.next_of_node.prev_of_node = this.prev_of_node;
            }
        }
        next_of_node = prev_of_node = null;
        return currentHead;
    }
    
    /**
     * Insert this MSC at the head of a global rete MSC list
     * 
     * @param currentHead the current list head
     * @return the new list head (this)
     * @see SoarReteListener#ms_assertions and friends
     */
    MatchSetChange addToHeadOfAllList(MatchSetChange currentHead)
    {
        next_of_all = currentHead;
        prev_of_all = null;
        if(currentHead != null)
        {
            currentHead.prev_of_all = this;
        }
        return this;
    }
    
    /**
     * Remove this MSC from a global rete MSC list
     * 
     * @param currentHead the current list head
     * @return the new list head
     * @see SoarReteListener#ms_assertions and friends
     */
    MatchSetChange removeFromAllList(MatchSetChange currentHead)
    {
        if(currentHead == this)
        {
            currentHead = next_of_all;
            if(next_of_all != null)
            {
                next_of_all.prev_of_all = null;
            }
        }
        else
        {
            prev_of_all.next_of_all = this.next_of_all;
            if(this.next_of_all != null)
            {
                this.next_of_all.prev_of_all = this.prev_of_all;
            }
        }
        next_of_all = prev_of_all = null;
        return currentHead;
    }
}
