/*
 * Copyright (c) 2008  Dave Ray <daveray@gmail.com>
 *
 * Created on Sep 12, 2008
 */
package org.jsoar.kernel.rete;

import org.jsoar.kernel.lhs.Condition;
import org.jsoar.kernel.rhs.Action;

/**
 * @author ray
 */
public class ConditionsAndNots
{
    public Condition dest_top_cond;
    public Condition dest_bottom_cond;
    public NotStruct dest_nots;
    public Action dest_rhs;
}
