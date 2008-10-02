/*
 * Copyright (c) 2008  Dave Ray <daveray@gmail.com>
 *
 * Created on Aug 23, 2008
 */
package org.jsoar.kernel.rete;

import org.jsoar.kernel.memory.Wme;
import org.jsoar.util.AsListItem;

/**
 * @author ray
 */
public class RightToken extends Token
{
//  struct token_from_right_memory_of_negative_or_cn_node_struct {
//  struct token_struct *next_negrm, *prev_negrm;/*other local join results*/
//  struct token_struct *left_token; /* token this is local join result for*/
//} neg;
    
    final AsListItem<RightToken> negrm = new AsListItem<RightToken>(this); // part of other local join results dll
    /*final*/ Token left_token; // token this is a local join result for
    
    public static RightToken create(ReteNode current_node, Token parent_tok, Wme parent_wme, Token left_token)
    {
        assert current_node != null;
        assert parent_wme != null;

        return new RightToken(current_node, parent_tok, parent_wme, left_token);
    }
    
    public static RightToken createDummy(ReteNode current_node)
    {
        assert current_node != null && current_node.node_type == ReteNodeType.DUMMY_TOP_BNODE;
        RightToken t = new RightToken(current_node, null, null, null);
        current_node.a_np.tokens = t;
        return t;
    }
    
    /**
     * @param current_node
     * @param parent_tok
     * @param parent_wme
     */
    private  RightToken(ReteNode current_node, Token parent_tok, Wme parent_wme, Token left_token)
    {
        super(current_node, parent_tok, parent_wme);
        this.left_token = left_token;
        
        if(left_token != null)
        {
            negrm.insertAtHead(left_token.negrm_tokens);
        }
    }

    /* (non-Javadoc)
     * @see java.lang.Object#toString()
     */
    @Override
    public String toString()
    {
        return w != null ? w.toString() : "null";
    }
}
