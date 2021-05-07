/*
 * Copyright (c) 2008  Dave Ray <daveray@gmail.com>
 *
 * Created on Sep 17, 2008
 */
package org.jsoar.kernel.rete;

import org.jsoar.kernel.lhs.Condition;

/** @author ray */
public class ReteNodeToConditionsResult {
  Condition dest_top_cond;
  Condition dest_bottom_cond;
  NotStruct nots_found_in_production;
}
