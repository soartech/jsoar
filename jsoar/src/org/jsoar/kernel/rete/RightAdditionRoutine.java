/*
 * Copyright (c) 2008  Dave Ray <daveray@gmail.com>
 *
 * Created on Aug 23, 2008
 */
package org.jsoar.kernel.rete;

import org.jsoar.kernel.Wme;

/**
 * @author ray
 */
public interface RightAdditionRoutine
{
    void execute(Rete rete, ReteNode node, Wme w);
}
