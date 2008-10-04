/*
 * Copyright (c) 2008  Dave Ray <daveray@gmail.com>
 *
 * Created on Aug 23, 2008
 */
package org.jsoar.kernel.rete;

import org.jsoar.kernel.memory.Wme;
import org.jsoar.kernel.symbols.Symbol;

/**
 * <p>rete.cpp::token_in_hash_table_data_struct
 * 
 * @author ray
 */
public class LeftToken extends Token
{
    LeftToken next_in_bucket;
    private LeftToken prev_in_bucket; // part of hash bucket dll
    final Symbol referent; // referent of the hash test (thing we hashed on)
    
    public LeftToken(ReteNode current_node, Token parent_tok, Wme parent_wme, Symbol referent)
    {
        super(current_node, parent_tok, parent_wme);
        this.referent = referent;
    }
    
    LeftToken addToHashTable(LeftToken head)
    {
        next_in_bucket = head;
        prev_in_bucket = null;
        if(head != null)
        {
            head.prev_in_bucket = this;
        }
        return this;
    }
    
    LeftToken removeFromHashTable(LeftToken head)
    {
        if(next_in_bucket != null)
        {
            next_in_bucket.prev_in_bucket = prev_in_bucket;
        }
        if(prev_in_bucket != null)
        {
            prev_in_bucket.next_in_bucket = next_in_bucket;
        }
        else
        {
            head = next_in_bucket;
        }
        next_in_bucket = null;
        prev_in_bucket = null;
        
        return head;
    }    

    /* (non-Javadoc)
     * @see java.lang.Object#toString()
     */
    @Override
    public String toString()
    {
        return w + "/" + referent;
    }
    
    
}
