/*
 * (c) 2008  Dave Ray
 *
 * Created on Aug 22, 2008
 */
package org.jsoar.kernel.rete;

/**
 * data for both positive and negative nodes
 *
 * <p>rete.cpp:363
 *
 * @author ray
 */
class PosNegNodeData implements BReteNodeData {
  ReteTest other_tests; /* tests other than the hashed test */
  AlphaMemory alpha_mem_; /* the alpha memory this node uses */
  ReteNode next_from_alpha_mem; /* dll of nodes using that */
  boolean node_is_right_unlinked;
  ReteNode prev_from_alpha_mem; /*   ... alpha memory */
  ReteNode nearest_ancestor_with_same_am;

  /** @return Shallow copy of this object */
  public PosNegNodeData copy() {
    PosNegNodeData n = new PosNegNodeData();
    n.other_tests = this.other_tests;
    n.alpha_mem_ = this.alpha_mem_;
    n.next_from_alpha_mem = this.next_from_alpha_mem;
    n.node_is_right_unlinked = this.node_is_right_unlinked;
    n.prev_from_alpha_mem = this.prev_from_alpha_mem;
    n.nearest_ancestor_with_same_am = this.nearest_ancestor_with_same_am;

    return n;
  }
}
