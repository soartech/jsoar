/*
 * (c) 2008  Dave Ray
 *
 * Created on Aug 22, 2008
 */
package org.jsoar.kernel.rete;

import java.util.ArrayList;
import java.util.List;

import org.jsoar.kernel.MatchSetChange;
import org.jsoar.kernel.Production;
import org.jsoar.kernel.Wme;
import org.jsoar.kernel.lhs.Condition;
import org.jsoar.kernel.symbols.Identifier;
import org.jsoar.kernel.symbols.Symbol;
import org.jsoar.util.AsListItem;
import org.jsoar.util.ListHead;
import org.jsoar.util.SoarHashTable;

/**
 * @author ray
 */
public class Rete
{

    
    public static final int NO_REFRACTED_INST = 0;              /* no refracted inst. was given */
    public static final int REFRACTED_INST_MATCHED = 1;         /* there was a match for the inst. */
    public static final int REFRACTED_INST_DID_NOT_MATCH = 2;   /* there was no match for it */
    public static final int DUPLICATE_PRODUCTION = 3;           /* the prod. was a duplicate */
    
    private static LeftAdditionRoutine[] left_addition_routines = new LeftAdditionRoutine[256];
    private static RightAdditionRoutine[] right_addition_routines = new RightAdditionRoutine[256];
    static
    {
        // rete.cpp:8796 
        // TODO
//        left_addition_routines[DUMMY_MATCHES_BNODE] = dummy_matches_node_left_addition;
//        left_addition_routines[MEMORY_BNODE] = beta_memory_node_left_addition;
//        left_addition_routines[UNHASHED_MEMORY_BNODE] = unhashed_beta_memory_node_left_addition;
//        left_addition_routines[MP_BNODE] = mp_node_left_addition;
//        left_addition_routines[UNHASHED_MP_BNODE] = unhashed_mp_node_left_addition;
//        left_addition_routines[CN_BNODE] = cn_node_left_addition;
//        left_addition_routines[CN_PARTNER_BNODE] = cn_partner_node_left_addition;
//        left_addition_routines[P_BNODE] = p_node_left_addition;
//        left_addition_routines[NEGATIVE_BNODE] = negative_node_left_addition;
//        left_addition_routines[UNHASHED_NEGATIVE_BNODE] = unhashed_negative_node_left_addition;
//
//        right_addition_routines[POSITIVE_BNODE] = positive_node_right_addition;
//        right_addition_routines[UNHASHED_POSITIVE_BNODE] = unhashed_positive_node_right_addition;
//        right_addition_routines[MP_BNODE] = mp_node_right_addition;
//        right_addition_routines[UNHASHED_MP_BNODE] = unhashed_mp_node_right_addition;
//        right_addition_routines[NEGATIVE_BNODE] = negative_node_right_addition;
//        right_addition_routines[UNHASHED_NEGATIVE_BNODE] = unhashed_negative_node_right_addition;
    }
    
    private LeftTokenHashTable left_ht = new LeftTokenHashTable();
    private RightMemoryHashTable right_ht = new RightMemoryHashTable();
    int rete_node_counts[] = new int[256];
    private RightToken dummy_top_token;
    /**
     * agent.h:728
     */
    private boolean operand2_mode = true;
    /**
     * agent.h:733
     * dll of all retractions for removed (ie nil) goals
     */
    private ListHead<MatchSetChange> nil_goal_retractions = new ListHead<MatchSetChange>();
    private int alpha_mem_id_counter; 
    private List<SoarHashTable<AlphaMemory>> alpha_hash_tables;
    private ListHead<Wme> all_wmes_in_rete = new ListHead<Wme>();
    private int num_wmes_in_rete= 0;
    private int beta_node_id_counter;
    private ReteNode dummy_top_node;
    
    public Rete()
    {
        // rete.cpp:8864
        alpha_hash_tables = new ArrayList<SoarHashTable<AlphaMemory>>(16);
        for(int i = 0; i < 16; ++i)
        {
            alpha_hash_tables.add(new SoarHashTable<AlphaMemory>(0, AlphaMemory.HASH_FUNCTION));
        }
    }
    
    public int add_production_to_rete (Production p, Condition lhs_top,
                                        Instantiation refracted_inst,
                                        boolean warn_on_duplicates, boolean ignore_rhs /*= false*/)
    {
        return 0;
    }
    
    public void excise_production_from_rete (Production p)
    {
        
    }

    private static int xor_op(int i, int a, int v)
    {
      return ((i) ^ (a) ^ (v));
    }
    
    /**
     * Add a WME to the rete.
     * 
     * rete.cpp:1552
     * 
     * @param w The WME to add
     */
    public void add_wme_to_rete (Wme w)
    {
        /* --- add w to all_wmes_in_rete --- */
        w.in_rete.insertAtHead(all_wmes_in_rete);
        num_wmes_in_rete++;

        /* --- it's not in any right memories or tokens yet --- */
        w.right_mems.first = null;
        w.tokens.first = null;

        /* --- add w to the appropriate alpha_mem in each of 8 possible tables --- */
        int hi = w.id.hash_id;
        int ha = w.attr.hash_id;
        int hv = w.value.hash_id;

        if (w.acceptable) {
          add_wme_to_aht (alpha_hash_tables.get(8),  xor_op( 0, 0, 0), w);
          add_wme_to_aht (alpha_hash_tables.get(9),  xor_op(hi, 0, 0), w);
          add_wme_to_aht (alpha_hash_tables.get(10), xor_op( 0,ha, 0), w);
          add_wme_to_aht (alpha_hash_tables.get(11), xor_op(hi,ha, 0), w);
          add_wme_to_aht (alpha_hash_tables.get(12), xor_op( 0, 0,hv), w);
          add_wme_to_aht (alpha_hash_tables.get(13), xor_op(hi, 0,hv), w);
          add_wme_to_aht (alpha_hash_tables.get(14), xor_op( 0,ha,hv), w);
          add_wme_to_aht (alpha_hash_tables.get(15), xor_op(hi,ha,hv), w);
        } else {
          add_wme_to_aht (alpha_hash_tables.get(0),  xor_op( 0, 0, 0), w);
          add_wme_to_aht (alpha_hash_tables.get(1),  xor_op(hi, 0, 0), w);
          add_wme_to_aht (alpha_hash_tables.get(2),  xor_op( 0,ha, 0), w);
          add_wme_to_aht (alpha_hash_tables.get(3),  xor_op(hi,ha, 0), w);
          add_wme_to_aht (alpha_hash_tables.get(4),  xor_op( 0, 0,hv), w);
          add_wme_to_aht (alpha_hash_tables.get(5),  xor_op(hi, 0,hv), w);
          add_wme_to_aht (alpha_hash_tables.get(6),  xor_op( 0,ha,hv), w);
          add_wme_to_aht (alpha_hash_tables.get(7),  xor_op(hi,ha,hv), w);
        }
        
    }
    
    /**
     * Remove a WME from the rete.
     * 
     * rete.cpp:1591
     * 
     * @param w The WME to remove
     */
    public void remove_wme_from_rete (Wme w)
    {
        /* --- remove w from all_wmes_in_rete --- */
        w.in_rete.remove(all_wmes_in_rete);
        num_wmes_in_rete--;
        
        /* --- remove w from each alpha_mem it's in --- */
        while (!w.right_mems.isEmpty()) {
          RightMemory rm = w.right_mems.first.get();
          AlphaMemory am = rm.am;
          /* --- found the alpha memory, first remove the wme from it --- */
          remove_wme_from_alpha_mem (rm);
          
//      #ifdef DO_ACTIVATION_STATS_ON_REMOVALS
//          /* --- if doing statistics stuff, then activate each attached node --- */
//          for (node=am->beta_nodes; node!=NIL; node=next) {
//            next = node->b.posneg.next_from_alpha_mem;
//            right_node_activation (node,FALSE);
//          }
//      #endif
          
          /* --- for left unlinking, then if the alpha memory just went to
             zero, left unlink any attached Pos or MP nodes --- */
          if (am.right_mems.isEmpty()) {
              ReteNode next = null;
            for (ReteNode node=am.beta_nodes; node!=null; node=next) {
              next = node.b_posneg.next_from_alpha_mem;
              switch (node.node_type) {
              case ReteNode.POSITIVE_BNODE:
              case ReteNode.UNHASHED_POSITIVE_BNODE:
                node.unlink_from_left_mem();
                break;
              case ReteNode.MP_BNODE:
              case ReteNode.UNHASHED_MP_BNODE:
                node.make_mp_bnode_left_unlinked();
                break;
              } /* end of switch (node.node_type) */
            }
          }
        }
       
        /* --- tree-based removal of all tokens that involve w --- */
        while (!w.tokens.isEmpty()) {
          Token tok = w.tokens.first.get();
          ReteNode node = tok.node;
          if (tok.parent == null) {
            /* Note: parent pointer is NIL only on negative node negrm tokens */
              RightToken rt = (RightToken) tok;
            Token left = rt.left_token;
            tok.from_wme.remove(w.tokens);
            rt.negrm.remove(left.negrm_tokens);

            if (left.negrm_tokens.isEmpty()) { /* just went to 0, so call children */
              for (ReteNode child=node.first_child; child!=null; child=child.next_sibling) {
                left_addition_routines[child.node_type].execute(this, child, left, null);
              }
            }
          } else {
            remove_token_and_subtree (w.tokens);
          }
        }
        
    }
    
    /**
     * rete.cpp: 6083
     * 
     * @param tokens
     */
    private void remove_token_and_subtree(ListHead<Token> root)
    {
        // TODO: implement remove_token_and_subtree()
    }

    /* --- Invoked on every right activation; add=TRUE means right addition --- */
    /* NOT invoked on removals unless DO_ACTIVATION_STATS_ON_REMOVALS is set */
    void right_node_activation(ReteNode node, boolean add)
    {
        // TODO: Delete this?
      //null_activation_stats_for_right_activation(node, null);
    }

    /* --- Invoked on every left activation; add=TRUE means left addition --- */
    /* NOT invoked on removals unless DO_ACTIVATION_STATS_ON_REMOVALS is set */
    void left_node_activation(ReteNode node, boolean add)
    {
        // TODO: Delete this?
      //null_activation_stats_for_left_activation(node);
    }
    /**
     * rete.cpp:1011
     * @param msc
     * @return
     */
    Identifier find_goal_for_match_set_change_assertion(MatchSetChange msc) {

//      #ifdef DEBUG_WATERFALL
//        print_with_symbols(thisAgent, "\nMatch goal for assertion: %y", msc->p_node->b.p.prod->name); 
//      #endif


        Wme lowest_goal_wme = null;
        int lowest_level_so_far = -1;

        if (msc.w != null) {
            if (msc.w.id.isa_goal) {
              lowest_goal_wme = msc.w;
              lowest_level_so_far = msc.w.id.level;
            }
        }

        for (Token tok=msc.tok; tok!=dummy_top_token; tok=tok.parent) {
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
            throw new IllegalStateException("\nError: Did not find goal for ms_change assertion: " + msc.p_node.b_p.prod.name);
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
     * rete.cpp:1065
     * 
     * @param msc
     * @return
     */
    Symbol find_goal_for_match_set_change_retraction(MatchSetChange msc)
    {
        // #ifdef DEBUG_WATERFALL
        // print_with_symbols(thisAgent, "\nMatch goal level for retraction:
        // %y", msc->inst->prod->name);
        // #endif

        if (msc.inst.match_goal != null)
        {
            // If there is a goal, just return the goal
            // #ifdef DEBUG_WATERFALL
            // print_with_symbols(thisAgent, " is [%y]", msc->inst->match_goal);
            // #endif
            return msc.inst.match_goal;
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
     * rete.cpp:1403
     * 
     * @return
     */
    int get_next_alpha_mem_id()
    {
      return alpha_mem_id_counter++;
    }
    
    /**
     * Adds a WME to an alpha memory (create a right_mem for it), but doesn't
     * inform any successors
     * 
     * rete.cpp:1408
     * 
     * @param w
     * @param am
     */
    void add_wme_to_alpha_mem(Wme w, AlphaMemory am)
    {
        /* --- allocate new right_mem, fill it fields --- */
        RightMemory rm = new RightMemory();
        rm.w = w;
        rm.am = am;

        /* --- add it to dll's for the hash bucket, alpha mem, and wme --- */
        int hv = am.am_id ^ w.id.hash_id;
        ListHead<RightMemory> header = right_ht.right_ht_bucket(hv);
        rm.in_bucket.insertAtHead(header);
        rm.in_am.insertAtHead(am.right_mems);
        rm.from_wme.insertAtHead(w.right_mems);
    }
    
    /**
     * Removes a WME (right_mem) from its alpha memory, but doesn't inform
     * any successors
     * 
     * rete.cpp:1429
     * 
     * @param rm
     */
    void remove_wme_from_alpha_mem(RightMemory rm)
    {
        Wme w = rm.w;
        AlphaMemory am = rm.am;

        /* --- remove it from dll's for the hash bucket, alpha mem, and wme --- */
        int hv = am.am_id ^ w.id.hash_id;
        ListHead<RightMemory> header = right_ht.right_ht_bucket(hv);
        rm.in_bucket.remove(header);
        rm.in_am.remove(am.right_mems);
        rm.from_wme.remove(w.right_mems);
    }

    /**
     * rete.cpp:1393
     * 
     * @param id
     * @param attr
     * @param value
     * @param acceptable
     * @return
     */
    /*package*/ SoarHashTable<AlphaMemory> table_for_tests(Symbol id, Symbol attr, Symbol value, boolean acceptable)
    {
        int index = ((id != null) ? 1 : 0) + ((attr != null) ? 2 : 0) +
                                              ((value != null) ? 4 : 0) +
                                              ((acceptable) ? 8 : 0);
        return alpha_hash_tables.get(index);
    }
    
    /**
     * Looks for an existing alpha mem, returns it or NIL if not found
     * 
     * rete.cpp:1449
     * 
     * @param id
     * @param attr
     * @param value
     * @param acceptable
     * @return
     */
    AlphaMemory find_alpha_mem(Symbol id, Symbol attr, Symbol value, boolean acceptable)
    {
        SoarHashTable<AlphaMemory> ht = table_for_tests(id, attr, value, acceptable);
        int hash_value = AlphaMemory.alpha_hash_value(id, attr, value, ht.getLog2Size());

        for (AlphaMemory am = ht.getBucket(hash_value); am != null; am = (AlphaMemory) am.next_in_hash_table)
        {
            if ((am.id == id) && (am.attr == attr) && (am.value == value) && (am.acceptable == acceptable))
            {
                return am;
            }
        }
        return null;
    }

    /**
     * Find and share existing alpha memory, or create new one.  Adjusts the 
     * reference count on the alpha memory accordingly.
     * 
     * rete.cpp:1467
     * 
     * @param id
     * @param attr
     * @param value
     * @param acceptable
     * @return
     */
    AlphaMemory find_or_make_alpha_mem(Symbol id, Symbol attr, Symbol value, boolean acceptable)
    {

        /* --- look for an existing alpha mem --- */
        AlphaMemory am = find_alpha_mem(id, attr, value, acceptable);
        if (am != null)
        {
            // TODO: am->reference_count++;
            return am;
        }

        /* --- no existing alpha_mem found, so create a new one --- */
        am = new AlphaMemory();
        am.next_in_hash_table = null;
        // TODO am->reference_count = 1;
        am.id = id;
        // if (id) symbol_add_ref (id);
        am.attr = attr;
        // if (attr) symbol_add_ref (attr);
        am.value = value;
        // if (value) symbol_add_ref (value);
        am.acceptable = acceptable;
        am.am_id = get_next_alpha_mem_id();
        SoarHashTable<AlphaMemory> ht = table_for_tests(id, attr, value, acceptable);
        ht.add_to_hash_table(am);

        /* --- fill new mem with any existing matching WME's --- */
        AlphaMemory more_general_am = null;
        if (id != null)
        {
            more_general_am = find_alpha_mem(null, attr, value, acceptable);
        }
        if (more_general_am == null && value != null)
        {
            more_general_am = find_alpha_mem(null, attr, null, acceptable);
        }
        if (more_general_am != null)
        {
            /* --- fill new mem using the existing more general one --- */
            for (AsListItem<RightMemory> rm = more_general_am.right_mems.first; rm != null; rm = rm.next)
            {
                if (am.wme_matches_alpha_mem(rm.get().w))
                {
                    add_wme_to_alpha_mem(rm.get().w, am);
                }
            }
        }
        else
        {
            /* --- couldn't find such an existing mem, so do it the hard way --- */
            for (AsListItem<Wme> w = all_wmes_in_rete.first; w != null; w = w.next)
            {
                if (am.wme_matches_alpha_mem(w.get()))
                {
                    add_wme_to_alpha_mem(w.get(), am);
                }
            }
        }

        return am;
    }

    /**
     * Using the given hash table and hash value, try to find a matching alpha 
     * memory in the indicated hash bucket.  If we find one, we add the wme to 
     * it and inform successor nodes.
     * 
     * rete.cpp:1524
     * 
     * @param ht
     * @param hash_value
     * @param w
     */
    void add_wme_to_aht(SoarHashTable<AlphaMemory> ht, int hash_value, Wme w)
    {
        // TODO: Move this op into getBucket()
        hash_value = hash_value & SoarHashTable.masks_for_n_low_order_bits[ht.getLog2Size()];
        AlphaMemory am = ht.getBucket(hash_value);
        while (am != null)
        {
            if (am.wme_matches_alpha_mem(w))
            {
                /* --- found the right alpha memory, first add the wme --- */
                add_wme_to_alpha_mem(w, am);

                /* --- now call the beta nodes --- */
                ReteNode next = null;
                for (ReteNode node = am.beta_nodes; node != null; node = next)
                {
                    next = node.b_posneg.next_from_alpha_mem;
                    right_addition_routines[node.node_type].execute(this, node, w);
                }
                return; /* only one possible alpha memory per table could match */
            }
            am = (AlphaMemory) am.next_in_hash_table;
        }
    }


    /**
     * rete.cpp:1698
     * 
     * @return
     */
    int get_next_beta_node_id()
    {
      return beta_node_id_counter++;
    }
    
    /**
     * The dummy top node always has one token in it (WME=NIL). This is just
     * there so that (real) root nodes in the beta net can be handled the same
     * as non-root nodes.
     * 
     * rete.cpp:1711
     */
    void init_dummy_top_node()
    {
        /* --- create the dummy top node --- */
        dummy_top_node = new ReteNode(ReteNode.DUMMY_TOP_BNODE);

        /* --- create the dummy top token --- */
        dummy_top_token = new RightToken(dummy_top_node, null, null, null);
        dummy_top_node.a_np.tokens.first = dummy_top_token.of_node;
    }
    

    /**
     * Calls a node's left-addition routine with each match (token) from 
     * the node's parent.  DO NOT call this routine on (positive, unmerged)
     * join nodes.
     * 
     * rete.cpp:1765
     * 
     * @param node
     */
    void update_node_with_matches_from_above(ReteNode child)
    {
        if (ReteNode.bnode_is_bottom_of_split_mp(child.node_type)) {
            throw new IllegalArgumentException("Internal error: update_node_with_matches_from_above called on split node");
        }
        
        ReteNode parent = child.parent;

        /* --- if parent is dummy top node, tell child about dummy top token --- */ 
        if (parent.node_type==ReteNode.DUMMY_TOP_BNODE) {
          left_addition_routines[child.node_type].execute(this, child, dummy_top_token, null);
          return;
        }

        /* --- if parent is positive: first do surgery on parent's child list,
               to replace the list with "child"; then call parent's add_right 
               routine with each wme in the parent's alpha mem; then do surgery 
               to restore previous child list of parent. --- */
        if (ReteNode.bnode_is_positive(parent.node_type)) {
          /* --- If the node is right unlinked, then don't activate it.  This is
             important because some interpreter routines rely on the node
             being right linked whenever it gets right activated. */
          if (parent.node_is_right_unlinked ()) { return;}
          ReteNode saved_parents_first_child = parent.first_child;
          ReteNode saved_childs_next_sibling = child.next_sibling;
          parent.first_child = child;
          child.next_sibling = null;
          /* to avoid double-counting these right adds */
          for(RightMemory rm : parent.b_posneg.alpha_mem_.right_mems)
          {
              right_addition_routines[parent.node_type].execute(this, parent, rm.w);
          }
          parent.first_child = saved_parents_first_child;
          child.next_sibling = saved_childs_next_sibling;
          return;
        }
          
        /* --- if parent is negative or cn: easy, just look at the list of tokens
               on the parent node. --- */
        //for (tok=parent->a.np.tokens; tok!=NIL; tok=tok->next_of_node)
        for(Token tok : parent.a_np.tokens)
        {
            if(tok.negrm_tokens.isEmpty())
            {
                left_addition_routines[child.node_type].execute(this, child, tok, null);
            }
        }
    }
}
