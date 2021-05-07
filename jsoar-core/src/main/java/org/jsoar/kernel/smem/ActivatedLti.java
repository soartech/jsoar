/*
 * Copyright (c) 2010 Dave Ray <daveray@gmail.com>
 *
 * Created on Jun 23, 2010
 */
package org.jsoar.kernel.smem;

import java.util.PriorityQueue;

/**
 * semantic_memory.h:303:smem_activated_lti
 *
 * @author ray
 */
class ActivatedLti implements Comparable<ActivatedLti> {
  public static PriorityQueue<ActivatedLti> newPriorityQueue() {
    return new PriorityQueue<ActivatedLti>();
  }

  final /*double*/ double first;
  final /*smem_lti_id*/ long second;

  public ActivatedLti(double first, long second) {
    this.first = first;
    this.second = second;
  }

  @Override
  public int compareTo(ActivatedLti o) {
    // semantic_memory.h:305:smem_compare_activated_lti
    return first > o.first ? 1 : (first < o.first ? -1 : 0);
  }
}
