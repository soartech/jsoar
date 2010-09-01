/*
 * Copyright (c) 2010 Dave Ray <daveray@gmail.com>
 *
 * Created on Sep 1, 2010
 */
package org.jsoar.kernel.symbols;

import org.jsoar.kernel.memory.Slot;
import org.jsoar.kernel.rete.MatchSetChange;
import org.jsoar.util.ListHead;

/**
 * @author ray
 */
public class GoalIdentifierInfo
{
    public final ListHead<MatchSetChange> ms_o_assertions = ListHead.newInstance(); /* dll of o assertions at this level */
    public final ListHead<MatchSetChange> ms_i_assertions = ListHead.newInstance(); /* dll of i assertions at this level */
    public final ListHead<MatchSetChange> ms_retractions = ListHead.newInstance();  /* dll of retractions at this level */
    public Slot operator_slot;

}
