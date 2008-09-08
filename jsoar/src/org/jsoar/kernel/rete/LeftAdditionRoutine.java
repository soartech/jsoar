/*
 * Copyright (c) 2008  Dave Ray <daveray@gmail.com>
 *
 * Created on Aug 23, 2008
 */
package org.jsoar.kernel.rete;

import org.jsoar.kernel.memory.Wme;

/**
 * @author ray
 */
public interface LeftAdditionRoutine
{
    void execute(Rete rete, ReteNode node, Token tok, Wme w);
}
