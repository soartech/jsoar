/*
 * Copyright (c) 2008  Dave Ray <daveray@gmail.com>
 *
 * Created on Aug 23, 2008
 */
package org.jsoar.kernel.rete;

import org.jsoar.kernel.memory.WmeImpl;
import org.jsoar.util.ListItem;

/** @author ray */
public class RightToken extends Token {
  final ListItem<RightToken> negrm =
      new ListItem<RightToken>(this); // part of other local join results dll
  private LeftToken left_token; // token this is a local join result for

  public static RightToken create(
      ReteNode current_node, Token parent_tok, WmeImpl parent_wme, LeftToken left_token) {
    assert current_node != null;
    // assert parent_wme != null;

    return new RightToken(current_node, parent_tok, parent_wme, left_token);
  }

  public static RightToken createDummy(ReteNode current_node) {
    assert current_node != null && current_node.node_type == ReteNodeType.DUMMY_TOP_BNODE;
    RightToken t = new RightToken(current_node, null, null, null);
    current_node.a_np().tokens = t;
    return t;
  }

  /**
   * @param current_node
   * @param parent_tok
   * @param parent_wme
   */
  private RightToken(
      ReteNode current_node, Token parent_tok, WmeImpl parent_wme, LeftToken left_token) {
    super(current_node, parent_tok, parent_wme, false);
    this.left_token = left_token;

    if (left_token != null) {
      left_token.addNegRightToken(this);
    }
  }

  public LeftToken getLeftToken() {
    return left_token;
  }

  public void setLeftToken(LeftToken leftToken) {
    // Note: this method can be called multiple times with different left tokens

    this.left_token = leftToken;
    if (leftToken != null) {
      leftToken.addNegRightToken(this);
    }
  }

  /* (non-Javadoc)
   * @see java.lang.Object#toString()
   */
  @Override
  public String toString() {
    return w != null ? w.toString() : "null";
  }
}
