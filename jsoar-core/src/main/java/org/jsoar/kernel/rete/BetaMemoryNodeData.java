/*
 * (c) 2008  Dave Ray
 *
 * Created on Aug 22, 2008
 */
package org.jsoar.kernel.rete;

import org.jsoar.util.ListHead;

/**
 * data for beta memory nodes only
 *
 * <p>rete.cpp:372
 *
 * @author ray
 */
class BetaMemoryNodeData implements BReteNodeData {
  // first pos node child that is left-linked
  final ListHead<ReteNode> first_linked_child;

  public BetaMemoryNodeData() {
    this.first_linked_child = ListHead.newInstance();
  }

  private BetaMemoryNodeData(BetaMemoryNodeData other) {
    this.first_linked_child = ListHead.newInstance(other.first_linked_child);
  }

  /** @return Shallow copy of this object */
  public BetaMemoryNodeData copy() {
    return new BetaMemoryNodeData(this);
  }
}
