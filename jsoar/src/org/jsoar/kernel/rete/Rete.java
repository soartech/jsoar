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
import org.jsoar.kernel.lhs.ConjunctiveTest;
import org.jsoar.kernel.lhs.EqualityTest;
import org.jsoar.kernel.lhs.Test;
import org.jsoar.kernel.symbols.Identifier;
import org.jsoar.kernel.symbols.Symbol;
import org.jsoar.kernel.symbols.Variable;
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
    ReteNode dummy_top_node;
    
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

    /**
     * This routine finds the most recent place a variable was bound.
     * It does this simply by looking at the top of the binding stack
     * for that variable.  If there is any binding, its location is stored
     * in the parameter *result, and the function returns TRUE.  If no
     * binding is found, the function returns FALSE.
     * 
     * rete.cpp:2373
     * 
     * @param var
     * @param current_depth
     * @param result
     * @return
     */
    boolean find_var_location(Variable var, /* rete_node_level */int current_depth, VarLocation result)
    {
        if (!var.var_is_bound())
        {
            return false;
        }
        int dummy = var.rete_binding_locations.peek();
        result.levels_up = current_depth - Variable.dummy_to_varloc_depth(dummy);
        result.field_num = Variable.dummy_to_varloc_field_num(dummy);
        return true;
    }    

    /**
     * This routine pushes bindings for variables occurring (i.e., being
     * equality-tested) in a given test.  It can do this in DENSE fashion
     * (push a new binding for ANY variable) or SPARSE fashion (push a new
     * binding only for previously-unbound variables), depending on the
     * boolean "dense" parameter.  Any variables receiving new bindings
     * are also pushed onto the given "varlist".
     * 
     * rete.cpp:2394
     * 
     * @param t
     * @param depth
     * @param field_num
     * @param dense
     * @param varlist
     */
    void bind_variables_in_test(Test t, int depth, int field_num, boolean dense, List<Variable> varlist)
    {

        if (t.isBlank())
        {
            return;
        }
        EqualityTest eq = t.asEqualityTest();
        if (eq != null)
        {
            Variable referent = eq.getReferent().asVariable();
            if (referent == null) // not a variable
            {
                return;
            }
            if (!dense && referent.var_is_bound())
            {
                return;
            }
            referent.push_var_binding(depth, field_num);
            varlist.add(0, referent); // push(thisAgent, referent, *varlist);
            return;
        }

        ConjunctiveTest ct = t.asConjunctiveTest();
        if (ct != null)
        {
            for (Test c : ct.conjunct_list)
            {
                bind_variables_in_test(c, depth, field_num, dense, varlist);
            }
        }
    }

    /**
     * This routine takes a list of variables; for each item <v> on the
     * list, it pops a binding of <v>.  It also deallocates the list.
     * This is often used for un-binding a group of variables which got
     * bound in some procedure.
     * 
     * rete.cpp:2430
     * 
     * @param vars
     */
    void pop_bindings_and_deallocate_list_of_variables(List<Variable> vars)
    {
        for (Variable v : vars)
        {
            v.pop_var_binding();
        }
    } 
    
    /**
     * This routine does tree-based removal of a token and its descendents.
     * Note that it uses a nonrecursive tree traversal; each iteration, the
     * leaf being deleted is the leftmost leaf in the tree.
     * 
     * rete.cpp:6083
     * 
     * @param root
     */
    void remove_token_and_subtree(Token root)
    {
        Token tok = root;
        
        while (true) {
          /* --- move down to the leftmost leaf --- */
          while (!tok.first_child.isEmpty()) { tok = tok.first_child.first.get(); }
          Token next_value_for_tok = tok.sibling.next != null ? tok.sibling.next.get() : tok.parent;

          /* --- cleanup stuff common to all types of nodes --- */
          ReteNode node = tok.node;
          left_node_activation(node,false);
          tok.of_node.remove(node.a_np.tokens);
//          fast_remove_from_dll (node->a.np.tokens, tok, token, next_of_node,
//                                prev_of_node);
          tok.sibling.remove(tok.parent.first_child);
//          fast_remove_from_dll (tok->parent->first_child, tok, token,
//                                next_sibling,prev_sibling);
          if (tok.w != null) { 
              tok.from_wme.remove(tok.w.tokens);
//              fast_remove_from_dll (tok->w->tokens, tok, token,
//                                       next_from_wme, prev_from_wme);
          }
          int node_type = node.node_type;

          /* --- for merged Mem/Pos nodes --- */
          if ((node_type==ReteNode.MP_BNODE)||(node_type==ReteNode.UNHASHED_MP_BNODE)) {
              LeftToken lt = (LeftToken) tok; // TODO: Assume this is safe?
              int hv = node.node_id ^ (lt.referent != null ? lt.referent.hash_id : 0);
              left_ht.remove_token_from_left_ht(lt, hv);
            if (! node.mp_bnode_is_left_unlinked()) {
              if (node.a_np.tokens.isEmpty()) { node.unlink_from_right_mem (); }
            }

          /* --- for P nodes --- */
          } else if (node_type==ReteNode.P_BNODE) {
            p_node_left_removal(node, tok.parent, tok.w);

          /* --- for Negative nodes --- */
          } else if ((node_type==ReteNode.NEGATIVE_BNODE) ||
                     (node_type==ReteNode.UNHASHED_NEGATIVE_BNODE)) {
            LeftToken lt = (LeftToken) tok; // TODO: Assume this is safe?
            int hv = node.node_id ^ (lt.referent != null ? lt.referent.hash_id : 0);
            left_ht.remove_token_from_left_ht(lt, hv);
            if (node.a_np.tokens.isEmpty()) { node.unlink_from_right_mem(); }
            for (Token t : tok.negrm_tokens) {
                t.from_wme.remove(t.w.tokens);
//              fast_remove_from_dll(t->w->tokens,t,token,next_from_wme,prev_from_wme);
            }

          /* --- for Memory nodes --- */
          } else if ((node_type==ReteNode.MEMORY_BNODE)||(node_type==ReteNode.UNHASHED_MEMORY_BNODE)) {
              LeftToken lt = (LeftToken) tok; // TODO: Assume this is safe?
              int hv = node.node_id ^ (lt.referent != null ? lt.referent.hash_id : 0);
              left_ht.remove_token_from_left_ht(lt, hv);
// TODO
//      #ifdef DO_ACTIVATION_STATS_ON_REMOVALS
//            /* --- if doing statistics stuff, then activate each attached node --- */
//            for (child=node->b.mem.first_linked_child; child!=NIL; child=next) {
//              next = child->a.pos.next_from_beta_mem;
//              left_node_activation (child,FALSE);
//            }
//      #endif
            /* --- for right unlinking, then if the beta memory just went to
               zero, right unlink any attached Pos nodes --- */
            if (node.a_np.tokens.isEmpty()) {
                ReteNode next = null;
              for (ReteNode child=node.b_mem.first_linked_child; child!=null; child=next) {
                next = child.a_pos.next_from_beta_mem;
                child.unlink_from_right_mem();
              }
            }

          /* --- for CN nodes --- */
          } else if (node_type==ReteNode.CN_BNODE) {
              // TODO: Is it ok to use hashcode in place of hashing on the adress?
              int hv = node.node_id ^ tok.parent.hashCode() ^ tok.w.hashCode();
            //int hv = node.node_id ^ (unsigned long)(tok->parent) ^ (unsigned long)(tok->w)
              left_ht.remove_token_from_left_ht((LeftToken) tok, hv); // TODO: Safe to assume this?  
            for(Token t : tok.negrm_tokens)
            {
                if(t.w != null)
                {
                    t.from_wme.remove(t.w.tokens);
                }
                t.of_node.remove(t.node.a_np.tokens);
                t.sibling.remove(t.parent.first_child);
            }

          /* --- for CN Partner nodes --- */
          } else if (node_type==ReteNode.CN_PARTNER_BNODE) {
            RightToken rt = (RightToken) tok; // TODO: Safe to assume this?
            Token left = rt.left_token;
            rt.negrm.remove(left.negrm_tokens);
//            fast_remove_from_dll (left->negrm_tokens, tok, token,
//                                  a.neg.next_negrm, a.neg.prev_negrm);
            if (left.negrm_tokens.isEmpty()) { /* just went to 0, so call children */
              for (ReteNode child=left.node.first_child; child!=null; child=child.next_sibling){
                left_addition_routines[child.node_type].execute(this, child, left, null);
              }
            }

          } else {
              throw new IllegalArgumentException("Internal error: bad node type " + node.node_type + " in remove_token_and_subtree");
          }
          
          if (tok==root) break; /* if leftmost leaf was the root, we're done */
          tok = next_value_for_tok; /* else go get the leftmost leaf again */
        } 
        
    }

    /**
     * rete.cpp:5887
     * 
     * @param node
     * @param parent
     * @param w
     */
    private void p_node_left_removal(ReteNode node, Token parent, Wme w)
    {
        // TODO: port p_node_left_removal(), rete.cpp:5887
    }
}
