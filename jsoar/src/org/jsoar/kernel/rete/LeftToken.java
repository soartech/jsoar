/*
 * Copyright (c) 2008  Dave Ray <daveray@gmail.com>
 *
 * Created on Aug 23, 2008
 */
package org.jsoar.kernel.rete;

import org.jsoar.kernel.Wme;
import org.jsoar.kernel.symbols.Symbol;
import org.jsoar.util.AsListItem;

/**
 * @author ray
 */
public class LeftToken extends Token
{
//  struct token_in_hash_table_data_struct {
//  struct token_struct *next_in_bucket, *prev_in_bucket; /*hash bucket dll*/
//  Symbol *referent; /* referent of the hash test (thing we hashed on) */
//} ht;

    AsListItem<LeftToken> in_bucket = new AsListItem<LeftToken>(this); // part of hash bucket dll
    Symbol referent; // referent of the hash test (thing we hashed on)
    
    public LeftToken(ReteNode current_node, Token parent_tok, Wme parent_wme, Symbol referent)
    {
        super(current_node, parent_tok, parent_wme);
        this.referent = referent;
    }
}
