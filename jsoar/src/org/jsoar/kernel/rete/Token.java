/*
 * Copyright (c) 2008  Dave Ray <daveray@gmail.com>
 *
 * Created on Aug 23, 2008
 */
package org.jsoar.kernel.rete;

import org.jsoar.kernel.memory.Wme;
import org.jsoar.util.AsListItem;
import org.jsoar.util.ListHead;

/**
 * 
 * rete.cpp:590
 * 
 * @author ray
 */
public class Token
{
    /* --- Note: "parent" is NIL on negative node negrm (local join result) 
    tokens, non-NIL on all other tokens including CN and CN_P stuff.
    I put "parent" at offset 0 in the structure, so that upward scans
    are fast (saves doing an extra integer addition in the inner loop) --- */
    public final Token parent;
    ReteNode node;
    public final Wme w;
    
    final ListHead<Token> first_child = new ListHead<Token>(); // head of dll of childen
    final AsListItem<Token> sibling = new AsListItem<Token>(this); // Part of dll of children
    final AsListItem<Token> of_node = new AsListItem<Token>(this); // Part of dll of tokens at node
    final AsListItem<Token> from_wme = new AsListItem<Token>(this); // Part of dll from wme, tree-based remove
    
    final ListHead<Token> negrm_tokens = new ListHead<Token>(); /* join results: for Neg, CN nodes only */
    
    public Token(ReteNode current_node, Token parent_tok, Wme parent_wme)
    {
        assert current_node != null;
        
        this.node = current_node;
        this.of_node.insertAtHead(node.a_np.tokens);
        
        this.parent = parent_tok;
        if(this.parent != null)
        {
            this.sibling.insertAtHead(parent_tok.first_child);
        }
        
        this.w = parent_wme;
        if (parent_wme != null)
        {
            this.from_wme.insertAtHead(parent_wme.tokens);
        }
    }

}
