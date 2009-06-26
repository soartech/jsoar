/*
 * (c) 2008  Dave Ray
 *
 * Created on Aug 22, 2008
 */
package org.jsoar.kernel.rete;

import org.jsoar.kernel.Production;

/**
 * rete.cpp:401
 * 
 * @author ray
 */
public class ReteNode
{
    ReteNodeType node_type;                  /* tells what kind of node this is */

    /* -- used only on hashed nodes -- */
    /* field_num: 0=id, 1=attr, 2=value */
    int left_hash_loc_field_num;      
    /* left_hash_loc_levels_up: 0=current node's alphamem, 1=parent's, etc. */
    int left_hash_loc_levels_up; 
    /* node_id: used for hash function */
    int node_id;                   

    ReteNode parent;       /* points to parent node */
    ReteNode first_child;  /* used for dll of all children, */
    ReteNode next_sibling; /*   regardless of unlinking status */
    
    // TODO: Fix this union hack
    // union rete_node_a_union {
    PosNodeData a_pos = null;                   /* for pos. nodes */
    NonPosNodeData a_np = null;                /* for all other nodes */
    // } a;
    
    // TODO: Fix this union hack
    // union rete_node_b_union {
    PosNegNodeData b_posneg = null;            /* for pos, neg, mp nodes */
    BetaMemoryNodeData b_mem = null;          /* for beta memory nodes */
    ConjunctiveNegationNodeData b_cn = null; /* for cn, cn_partner nodes */
    public ProductionNodeData b_p = null;                      /* for p nodes */
    // } b;

      
    private ReteNode(ReteNodeType type)
    {
        this.node_type = type;

        if (type.bnode_is_positive() && type != ReteNodeType.P_BNODE)
        {
            a_pos = new PosNodeData(this);
        }
        else
        {
            a_np = new NonPosNodeData();
        }

        if (type == ReteNodeType.P_BNODE)
        {
            b_p = new ProductionNodeData();
        }
        else if (type == ReteNodeType.CN_BNODE || type == ReteNodeType.CN_PARTNER_BNODE)
        {
            b_cn = new ConjunctiveNegationNodeData();
        }
        else if (type == ReteNodeType.MEMORY_BNODE || type == ReteNodeType.UNHASHED_MEMORY_BNODE)
        {
            b_mem = new BetaMemoryNodeData();
        }
        else
        {
            b_posneg = new PosNegNodeData();
        }

        // Enforce a and b "unions"
        assert (a_pos != null && a_np == null) || (a_pos == null && a_np != null);
        assert (b_p != null && b_cn == null && b_mem == null && b_posneg == null)
                || (b_p == null && b_cn != null && b_mem == null && b_posneg == null)
                || (b_p == null && b_cn == null && b_mem != null && b_posneg == null)
                || (b_p == null && b_cn == null && b_mem == null && b_posneg != null);
    }
    
    private ReteNode(ReteNode other)
    {
        this.node_type = other.node_type;
        this.left_hash_loc_levels_up = other.left_hash_loc_levels_up;
        this.left_hash_loc_field_num = other.left_hash_loc_field_num;
        this.node_id = other.node_id;
        this.parent = other.parent;
        this.first_child = other.first_child;
        this.next_sibling = other.next_sibling;
        this.a_pos = other.a_pos != null ? new PosNodeData(other.a_pos) : null;
        this.a_np = other.a_np != null ? other.a_np.copy() : null;
        this.b_posneg = other.b_posneg != null ? other.b_posneg.copy() : null;
        this.b_mem = other.b_mem != null ? other.b_mem.copy() : null;
        this.b_cn = other.b_cn != null ? other.b_cn.copy() : null;
        this.b_p = other.b_p != null ? other.b_p.copy() : null;
        
        // Enforce a and b "unions"
        assert (a_pos != null && a_np == null) || (a_pos == null && a_np != null);
        assert (b_p != null && b_cn == null && b_mem == null && b_posneg == null)
                || (b_p == null && b_cn != null && b_mem == null && b_posneg == null)
                || (b_p == null && b_cn == null && b_mem != null && b_posneg == null)
                || (b_p == null && b_cn == null && b_mem == null && b_posneg != null);
    }
      
    /**
     * Returns a copy of this node with semantics equivalent to struct
     * assignment in C.
     * 
     * @return A copy of this node
     */
    private ReteNode copy()
    {
        return new ReteNode(this);
    }

    /**
     * rete.cpp:432:real_parent_node
     * 
     * @return
     */
    public ReteNode real_parent_node()
    {
        return (node_type.bnode_is_bottom_of_split_mp() ? parent.parent : parent);
    }
    
    /**
     * rete.cpp:448:node_is_right_unlinked
     * 
     * @return
     */
    public boolean node_is_right_unlinked()
    {
        return b_posneg.node_is_right_unlinked;
        // return (((unsigned long)((node)->b.posneg.next_from_alpha_mem)) & 1);
    }

    /**
     * rete.cpp:455:mark_node_as_right_unlinked
     */
    private void mark_node_as_right_unlinked()
    {
        b_posneg.node_is_right_unlinked = true;
        //(node)->b.posneg.next_from_alpha_mem = static_cast<rete_node_struct *>((void *)1);
    }
    
    /**
     * rete.cpp:483:relink_to_right_mem
     */
    /*package*/ void relink_to_right_mem()
    {
        /* find first ancestor that's linked */
        ReteNode rtrm_ancestor = b_posneg.nearest_ancestor_with_same_am;
        ReteNode rtrm_prev;
        while (rtrm_ancestor != null && rtrm_ancestor.node_is_right_unlinked())
        {
            rtrm_ancestor = rtrm_ancestor.b_posneg.nearest_ancestor_with_same_am;
        }
        if (rtrm_ancestor != null)
        {
            /* insert just before that ancestor */
            rtrm_prev = rtrm_ancestor.b_posneg.prev_from_alpha_mem;
            this.b_posneg.next_from_alpha_mem = rtrm_ancestor;
            this.b_posneg.prev_from_alpha_mem = rtrm_prev;
            rtrm_ancestor.b_posneg.prev_from_alpha_mem = this;
            if (rtrm_prev != null)
            {
                rtrm_prev.b_posneg.next_from_alpha_mem = this;
            }
            else
            {
                this.b_posneg.alpha_mem_.beta_nodes = this;
            }
        }
        else
        {
            /* no such ancestor, insert at tail of list */
            rtrm_prev = this.b_posneg.alpha_mem_.last_beta_node;
            this.b_posneg.next_from_alpha_mem = null;
            this.b_posneg.prev_from_alpha_mem = rtrm_prev;
            this.b_posneg.alpha_mem_.last_beta_node = this;
            if (rtrm_prev != null)
            {
                rtrm_prev.b_posneg.next_from_alpha_mem = this;
            }
            else
            {
                this.b_posneg.alpha_mem_.beta_nodes = this;
            }
        }
        this.b_posneg.node_is_right_unlinked = false;
    }
    
    /**
     * rete.cpp:512:unlink_from_right_mem
     */
    /*package*/ void unlink_from_right_mem() 
    { 
        if (this.b_posneg.next_from_alpha_mem == null) 
        {
            this.b_posneg.alpha_mem_.last_beta_node = this.b_posneg.prev_from_alpha_mem;
        }
        // The code below is an expansion of this remove_from_dll macro...
//        remove_from_dll (this.b_posneg.alpha_mem_.beta_nodes, this, 
//                         b.posneg.next_from_alpha_mem, 
//                         b.posneg.prev_from_alpha_mem); 
        if(this.b_posneg.next_from_alpha_mem != null)
        {
            this.b_posneg.next_from_alpha_mem.b_posneg.prev_from_alpha_mem = 
                this.b_posneg.prev_from_alpha_mem;
        }
        if(this.b_posneg.prev_from_alpha_mem != null)
        {
            this.b_posneg.prev_from_alpha_mem.b_posneg.next_from_alpha_mem = 
                this.b_posneg.next_from_alpha_mem;
        }
        else
        {
            this.b_posneg.alpha_mem_.beta_nodes = this.b_posneg.next_from_alpha_mem;
        }

        mark_node_as_right_unlinked(); 
    }

    /**
     * rete.cpp:532:node_is_left_unlinked
     * 
     * @return True if this node is left unlinked
     */
    /*package*/ boolean node_is_left_unlinked()
    {
        return a_pos.node_is_left_unlinked;
        //return (((unsigned long)((node)->a.pos.next_from_beta_mem)) & 1);
    }

    /**
     * rete.cpp:539:mark_node_as_left_unlinked
     */
    /*package*/ void mark_node_as_left_unlinked()
    {
      a_pos.node_is_left_unlinked = true;
      //(node)->a.pos.next_from_beta_mem = static_cast<rete_node_struct *>((void *)1);
    }
    
    /**
     * rete.cpp:547:relink_to_left_mem
     */
    /*package*/ void relink_to_left_mem() 
    { 
        a_pos.from_beta_mem.insertAtHead(parent.b_mem.first_linked_child);
        a_pos.node_is_left_unlinked = false;
    }

    /**
     * rete.cpp:555:unlink_from_left_mem
     */
    /*package*/ void unlink_from_left_mem() 
    {
        a_pos.from_beta_mem.remove(parent.b_mem.first_linked_child);
        mark_node_as_left_unlinked(); 
    }
    
    /**
     * rete.cpp:570:make_mp_bnode_left_unlinked
     */
    /*package*/ void make_mp_bnode_left_unlinked() 
    {
      this.a_np.is_left_unlinked = true;
    }

    /**
     * rete.cpp:575:make_mp_bnode_left_linked
     */
    /*package*/ void make_mp_bnode_left_linked() 
    {
      this.a_np.is_left_unlinked = false;
    }

    /**
     * rete.cpp:580:mp_bnode_is_left_unlinked
     * 
     * @return
     */
    /*package*/ boolean mp_bnode_is_left_unlinked() 
    { 
      return this.a_np.is_left_unlinked;
    }
    
    /**
     * Splices a given node out of its parent's list of children.  This would
     * be a lot easier if the children lists were doubly-linked, but that
     * would take up a lot of extra space.
     * 
     * <p>rete.cpp:1744:remove_node_from_parents_list_of_children
     * 
     * @param node
     */
    /*package*/ void remove_node_from_parents_list_of_children()
    {
        ReteNode prev_sibling = this.parent.first_child;
        if (prev_sibling == this)
        {
            this.parent.first_child = this.next_sibling;
            return;
        }
        while (prev_sibling.next_sibling != this)
        {
            prev_sibling = prev_sibling.next_sibling;
        }
        prev_sibling.next_sibling = this.next_sibling;
    }
    
    /**
     * Scans up the net and finds the first (i.e., nearest) ancestor node
     * that uses a given alpha_mem.  Returns that node, or NIL if none exists.
     * 
     * <p>rete.cpp:1824:nearest_ancestor_with_same_am
     * 
     * @param am
     * @return
     */
    ReteNode nearest_ancestor_with_same_am(AlphaMemory am)
    {
        ReteNode node = this;
        while (node.node_type != ReteNodeType.DUMMY_TOP_BNODE)
        {
            if (node.node_type == ReteNodeType.CN_BNODE)
                node = node.b_cn.partner.parent;
            else
                node = node.real_parent_node();
            if (node.node_type.bnode_is_posneg() && (node.b_posneg.alpha_mem_ == am))
                return node;
        }
        return null;
    }
    
    static ReteNode createDummy()
    {
        return new ReteNode(ReteNodeType.DUMMY_TOP_BNODE);
    }
    
    /**
     * Make a new beta memory node, return a pointer to it.
     * 
     * <p>rete.cpp:1840:make_new_mem_node
     * 
     * @param rete
     * @param parent
     * @param node_type
     * @param left_hash_loc
     * @return
     */
    static ReteNode make_new_mem_node(Rete rete, ReteNode parent, ReteNodeType node_type, VarLocation left_hash_loc)
    {
        ReteNode node = new ReteNode(node_type);

        node.parent = parent;
        node.next_sibling = parent.first_child;
        parent.first_child = node;

        /* These hash fields are not used for unhashed node types */
        node.left_hash_loc_field_num = left_hash_loc.field_num;
        node.left_hash_loc_levels_up = left_hash_loc.levels_up;

        node.node_id = rete.get_next_beta_node_id();

        /* --- call new node's add_left routine with all the parent's tokens --- */
        rete.update_node_with_matches_from_above(node);

        return node;
    }

    /**
     * Make a new positive join node, return a pointer to it.
     * 
     * <p>rete.cpp:1873:make_new_positive_node
     * 
     * @param rete
     * @param parent_mem
     * @param node_type
     * @param am
     * @param rt
     * @param prefer_left_unlinking
     * @return
     */
    static ReteNode make_new_positive_node(Rete rete, ReteNode parent_mem, ReteNodeType node_type, AlphaMemory am, ReteTest rt,
            boolean prefer_left_unlinking)
    {
        ReteNode node = new ReteNode(node_type);

        node.parent = parent_mem;
        node.next_sibling = parent_mem.first_child;
        parent_mem.first_child = node;
        node.relink_to_left_mem();
        node.b_posneg.other_tests = rt;
        node.b_posneg.alpha_mem_ = am;
        node.b_posneg.nearest_ancestor_with_same_am = node.nearest_ancestor_with_same_am(am);
        node.relink_to_right_mem();

        //don't need to force WM through new node yet, as it's just a join
        // node with no children

        // unlink the join node from one side if possible
        if (parent_mem.a_np.tokens == null)
        {
            node.unlink_from_right_mem();
        }
        if ((am.right_mems.isEmpty()) && !node.node_is_right_unlinked())
        {
            node.unlink_from_left_mem();
        }
        if (prefer_left_unlinking && (parent_mem.a_np.tokens == null) && (am.right_mems.isEmpty()))
        {
            node.relink_to_right_mem();
            node.unlink_from_left_mem();
        }

        return node;
    }
    
    /**
     * Split a given MP node into separate M and P nodes, return a pointer
     * to the new Memory node.
     *  
     * <p>rete.cpp:1916:split_mp_node
     * 
     * @param mp_node
     * @return
     */
    static ReteNode split_mp_node(Rete rete, ReteNode mp_node)
    {
        ReteNodeType mem_node_type = null;

        // determine appropriate node types for new M and P nodes
        ReteNodeType node_type = null;
        if (mp_node.node_type == ReteNodeType.MP_BNODE)
        {
            node_type = ReteNodeType.POSITIVE_BNODE;
            mem_node_type = ReteNodeType.MEMORY_BNODE;
        }
        else
        {
            node_type = ReteNodeType.UNHASHED_POSITIVE_BNODE;
            mem_node_type = ReteNodeType.UNHASHED_MEMORY_BNODE;
        }

        // save a copy of the MP data, then kill the MP node
        ReteNode mp_copy = mp_node.copy();
        ReteNode parent = mp_node.parent;
        mp_node.remove_node_from_parents_list_of_children();
        // TODO update_stats_for_destroying_node (thisAgent, mp_node); /* clean
        // up rete stats stuff */

        // the old MP node will get transmogrified into the new Pos node
        ReteNode pos_node = mp_node;

        // create the new M node, transfer the MP node's tokens to it
        ReteNode mem_node = new ReteNode(mem_node_type);

        mem_node.parent = parent;
        mem_node.next_sibling = parent.first_child;
        parent.first_child = mem_node;
        mem_node.first_child = pos_node;
        mem_node.left_hash_loc_field_num = mp_copy.left_hash_loc_field_num;
        mem_node.left_hash_loc_levels_up = mp_copy.left_hash_loc_levels_up;
        mem_node.node_id = mp_copy.node_id;

        mem_node.a_np.tokens = mp_node.a_np.tokens;
        for (Token t = mp_node.a_np.tokens; t != null; t = t.next_of_node)
        {
            t.node = mem_node;
        }

        // transmogrify the old MP node into the new Pos node
        // TODO All of this feels yucky
        // init_new_rete_node_with_type (thisAgent, pos_node, node_type);
        pos_node.node_type = node_type;
        pos_node.a_np = null;
        pos_node.a_pos = new PosNodeData(pos_node);
        // TODO rete.rete_node_counts[pos_node.node_type.index()]++;
        pos_node.parent = mem_node;
        pos_node.first_child = mp_copy.first_child;
        pos_node.next_sibling = null;
        pos_node.b_posneg = mp_copy.b_posneg;
        pos_node.relink_to_left_mem(); /* for now, but might undo this below */

        // set join node's unlinking status according to mp_copy's
        if (mp_copy.mp_bnode_is_left_unlinked())
        {
            pos_node.unlink_from_left_mem();
        }

        return mem_node;
    }
    
    /**
     * Merge a given Memory node and its one positive join child into an
     * MP node, returning a pointer to the MP node.
     * 
     * <p>rete.cpp:1979:merge_into_mp_node
     * 
     * @param mem_node
     * @return
     */
    static ReteNode merge_into_mp_node(Rete rete, ReteNode mem_node)
    {
        ReteNode pos_node = mem_node.first_child;
        ReteNode parent = mem_node.parent;

        // sanity check: Mem node must have exactly one child
        if (pos_node == null || pos_node.next_sibling != null)
        {
            throw new IllegalArgumentException("Internal error: tried to merge_into_mp_node, but <>1 child");
        }

        // determine appropriate node type for new MP node
        ReteNodeType node_type = null;
        if (mem_node.node_type == ReteNodeType.MEMORY_BNODE)
        {
            node_type = ReteNodeType.MP_BNODE;
        }
        else
        {
            node_type = ReteNodeType.UNHASHED_MP_BNODE;
        }

        // save a copy of the Pos data, then kill the Pos node
        ReteNode pos_copy = pos_node.copy();
        // TODO update_stats_for_destroying_node (thisAgent, pos_node); /* clean
        // up rete stats stuff */

        // the old Pos node gets transmogrified into the new MP node
        ReteNode mp_node = pos_node;
        // init_new_rete_node_with_type (thisAgent, mp_node, node_type);
        mp_node.node_type = node_type;
        // TODO rete.rete_node_counts[mp_node.node_type.index()]++;
        mp_node.b_posneg = pos_copy.b_posneg; // TODO: Should this be .copy()?

        // transfer the Mem node's tokens to the MP node
        if(mp_node.a_np == null)
        {
            // TODO: This is very yucky
            mp_node.a_pos = null;
            mp_node.a_np = new NonPosNodeData();
        }
        mp_node.a_np.tokens = mem_node.a_np.tokens;
        for (Token t = mem_node.a_np.tokens; t != null; t = t.next_of_node)
        {
            t.node = mp_node;
        }
        mp_node.left_hash_loc_field_num = mem_node.left_hash_loc_field_num;
        mp_node.left_hash_loc_levels_up = mem_node.left_hash_loc_levels_up;
        mp_node.node_id = mem_node.node_id;

        // replace the Mem node with the new MP node
        mp_node.parent = parent;
        mp_node.next_sibling = parent.first_child;
        parent.first_child = mp_node;
        mp_node.first_child = pos_copy.first_child;

        mem_node.remove_node_from_parents_list_of_children();
        // TODO update_stats_for_destroying_node (thisAgent, mem_node); /* clean
        // up rete stats stuff */

        // set MP node's unlinking status according to pos_copy's
        mp_node.make_mp_bnode_left_linked();
        if (pos_copy.node_is_left_unlinked())
        {
            mp_node.make_mp_bnode_left_unlinked();
        }

        return mp_node;
    }
    
    
    /**
     * Create a new MP node
     * 
     * <p>rete.cpp:2043:make_new_mp_node
     * 
     * @param rete
     * @param parent
     * @param node_type
     * @param left_hash_loc
     * @param am
     * @param rt
     * @param prefer_left_unlinking
     * @return
     */
    static ReteNode make_new_mp_node(Rete rete, ReteNode parent, ReteNodeType node_type, VarLocation left_hash_loc,
            AlphaMemory am, ReteTest rt, boolean prefer_left_unlinking)
    {
        ReteNodeType mem_node_type = null, pos_node_type = null;

        if (node_type == ReteNodeType.MP_BNODE)
        {
            pos_node_type = ReteNodeType.POSITIVE_BNODE;
            mem_node_type = ReteNodeType.MEMORY_BNODE;
        }
        else
        {
            pos_node_type = ReteNodeType.UNHASHED_POSITIVE_BNODE;
            mem_node_type = ReteNodeType.UNHASHED_MEMORY_BNODE;
        }
        ReteNode mem_node = make_new_mem_node(rete, parent, mem_node_type, left_hash_loc);
        @SuppressWarnings("unused")
        ReteNode pos_node = make_new_positive_node(rete, mem_node, pos_node_type, am, rt, prefer_left_unlinking);
        return merge_into_mp_node(rete, mem_node);
    }
    
    /**
     * Make a new negative node and return it
     * 
     * <p>rete.cpp:2069:make_new_negative_node
     * 
     * @param rete
     * @param parent
     * @param node_type
     * @param left_hash_loc
     * @param am
     * @param rt
     * @return
     */
    static ReteNode make_new_negative_node(Rete rete, ReteNode parent, ReteNodeType node_type, VarLocation left_hash_loc,
            AlphaMemory am, ReteTest rt)
    {
        ReteNode node = new ReteNode(node_type);

        node.parent = parent;
        node.next_sibling = parent.first_child;
        parent.first_child = node;
        node.left_hash_loc_field_num = left_hash_loc.field_num;
        node.left_hash_loc_levels_up = left_hash_loc.levels_up;
        node.b_posneg.other_tests = rt;
        node.b_posneg.alpha_mem_ = am;
        node.b_posneg.nearest_ancestor_with_same_am = node.nearest_ancestor_with_same_am(am);
        node.relink_to_right_mem();

        node.node_id = rete.get_next_beta_node_id();

        // call new node's add_left routine with all the parent's tokens
        rete.update_node_with_matches_from_above(node);

        // if no tokens arrived from parent, unlink the node
        if (node.a_np.tokens == null)
        {
            node.unlink_from_right_mem();
        }

        return node;
    }

    
    
    /**
     * Make new CN and CN_PARTNER nodes, return a pointer to the CN node.
     * 
     * <p>rete.cpp:2107:make_new_cn_node
     * 
     * @param rete
     * @param parent
     * @param bottom_of_subconditions
     * @return
     */
    static ReteNode make_new_cn_node(Rete rete, ReteNode parent, ReteNode bottom_of_subconditions)
    {
        // Find top node in the subconditions branch
        ReteNode ncc_subconditions_top_node = null;
        for (ReteNode node = bottom_of_subconditions; node != parent; node = node.parent)
        {
            ncc_subconditions_top_node = node;
        }

        ReteNode node = new ReteNode(ReteNodeType.CN_BNODE);
        ReteNode partner = new ReteNode(ReteNodeType.CN_PARTNER_BNODE);

        /*
         * NOTE: for improved efficiency, <node> should be on the parent's
         * children list *after* the ncc subcontitions top node
         */
        ncc_subconditions_top_node.remove_node_from_parents_list_of_children();
        node.parent = parent;
        node.next_sibling = parent.first_child;
        ncc_subconditions_top_node.next_sibling = node;
        parent.first_child = ncc_subconditions_top_node;
        node.first_child = null;

        node.b_cn.partner = partner;
        node.node_id = rete.get_next_beta_node_id();

        partner.parent = bottom_of_subconditions;
        partner.next_sibling = bottom_of_subconditions.first_child;
        bottom_of_subconditions.first_child = partner;
        partner.first_child = null;
        partner.b_cn.partner = node;

        // call partner's add_left routine with all the parent's tokens
        rete.update_node_with_matches_from_above(partner);
        // call new node's add_left routine with all the parent's tokens
        rete.update_node_with_matches_from_above(node);

        return node;
    }
    
    /**
     * Make a new production node, return a pointer to it.
     *
     * Does not handle the following tasks:
     *   - filling in p_node->b.p.parents_nvn or discarding chunk variable names 
     *   - filling in stuff on new_prod (except does fill in new_prod->p_node)
     *   - using update_node_with_matches_from_above (p_node) or handling
     *     an initial refracted instantiation
     *
     * <p>rete.cpp:2163:make_new_production_node
     * 
     * @param rete
     * @param parent
     * @param new_prod
     * @return
     */
    static ReteNode make_new_production_node(Rete rete, ReteNode parent, Production new_prod)
    {
        ReteNode p_node = new ReteNode(ReteNodeType.P_BNODE);

        new_prod.setReteNode(rete, p_node);
        p_node.parent = parent;
        p_node.next_sibling = parent.first_child;
        parent.first_child = p_node;
        p_node.first_child = null;
        p_node.b_p.prod = new_prod;
        return p_node;
    }
    
    /**
     * Create a new dummy matches node with the given parent
     * 
     * @param parent The parent node
     * @return New dummy matches node
     */
    public static ReteNode createMatchesNode(ReteNode parent)
    {
        ReteNode dummy = new ReteNode(ReteNodeType.DUMMY_MATCHES_BNODE);
        dummy.parent = parent;
        return dummy;
    }
    
    /**
     * <p>rete.cpp:2218:deallocate_rete_node
     * 
     * @param rete
     * @param node
     */
    static void deallocate_rete_node(Rete rete, ReteNode node)
    {
        // don't deallocate the dummy top node
        if (node == rete.dummy_top_node)
            return;

        // sanity check
        if (node.node_type == ReteNodeType.P_BNODE)
        {
            throw new IllegalArgumentException("deallocate_rete_node() called on p-node");
        }

        ReteNode parent = node.parent;

        // if a cn node, deallocate its partner first
        if (node.node_type == ReteNodeType.CN_BNODE)
        {
            deallocate_rete_node(rete, node.b_cn.partner);
        }

        // clean up any tokens at the node
        if (!node.node_type.bnode_is_bottom_of_split_mp())
        {
            while (node.a_np.tokens != null)
            {
                rete.remove_token_and_subtree(node.a_np.tokens);
            }
        }

        // stuff for posneg nodes only
        if (node.node_type.bnode_is_posneg())
        {
            node.b_posneg.other_tests = null;
            
            // right unlink the node, cleanup alpha memory
            if (!node.node_is_right_unlinked())
            {
                node.unlink_from_right_mem();
            }
            node.b_posneg.alpha_mem_.remove_ref_to_alpha_mem(rete);
        }

        // remove the node from its parent's list
        node.remove_node_from_parents_list_of_children();

        // for unmerged pos. nodes: unlink, maybe merge its parent
        if (node.node_type.bnode_is_bottom_of_split_mp())
        {
            if (!node.node_is_left_unlinked())
            {
                node.unlink_from_left_mem();
            }
            // if parent is mem node with just one child, merge them
            if (parent.first_child != null && parent.first_child.next_sibling == null)
            {
                merge_into_mp_node(rete, parent);
                parent = null;
            }
        }

        // if parent has no other children, deallocate it, and recurse
        /*
         * Added check to make sure that parent wasn't deallocated in previous
         * merge
         */
        if (parent != null && parent.first_child == null)
        {
            deallocate_rete_node (rete, parent);
        }
    }

    /* (non-Javadoc)
     * @see java.lang.Object#toString()
     */
    @Override
    public String toString()
    {
        return node_id + ":" + node_type;
    }

}
