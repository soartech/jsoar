/*
 * Copyright (c) 2008  Dave Ray <daveray@gmail.com>
 *
 * Created on Aug 23, 2008
 */
package org.jsoar.kernel.rete;

import org.jsoar.kernel.memory.WmeImpl;
import org.jsoar.kernel.symbols.SymbolImpl;
import org.jsoar.util.ListHead;
import org.jsoar.util.ListItem;

/**
 * rete.cpp::token_in_hash_table_data_struct
 *
 * @author ray
 */
public class LeftToken extends Token {
  LeftToken next_in_bucket;
  private LeftToken prev_in_bucket; // part of hash bucket dll
  final SymbolImpl referent; // referent of the hash test (thing we hashed on)
  private ListHead<RightToken> negrm_tokens = null; /* join results: for Neg, CN nodes only */

  public LeftToken(
      ReteNode current_node, Token parent_tok, WmeImpl parent_wme, SymbolImpl referent) {
    super(current_node, parent_tok, parent_wme, true);
    this.referent = referent;
  }

  LeftToken addToHashTable(LeftToken head) {
    next_in_bucket = head;
    prev_in_bucket = null;
    if (head != null) {
      head.prev_in_bucket = this;
    }
    return this;
  }

  LeftToken removeFromHashTable(LeftToken head) {
    if (next_in_bucket != null) {
      next_in_bucket.prev_in_bucket = prev_in_bucket;
    }
    if (prev_in_bucket != null) {
      prev_in_bucket.next_in_bucket = next_in_bucket;
    } else {
      head = next_in_bucket;
    }
    next_in_bucket = null;
    prev_in_bucket = null;

    return head;
  }

  boolean hasNegRightTokens() {
    return negrm_tokens != null && !negrm_tokens.isEmpty();
  }

  private ListHead<RightToken> getNegRightTokens() {
    if (negrm_tokens == null) {
      negrm_tokens = ListHead.newInstance();
    }
    return negrm_tokens;
  }

  ListItem<RightToken> getFirstNegRightToken() {
    return negrm_tokens != null ? negrm_tokens.first : null;
  }

  void addNegRightToken(RightToken rt) {
    rt.negrm.insertAtHead(getNegRightTokens());
  }

  void removeNegRightToken(RightToken rt) {
    rt.negrm.remove(getNegRightTokens());
  }

  /* (non-Javadoc)
   * @see java.lang.Object#toString()
   */
  @Override
  public String toString() {
    return w + "/" + referent;
  }
}
