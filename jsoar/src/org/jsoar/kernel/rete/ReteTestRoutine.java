/*
 * Copyright (c) 2008  Dave Ray <daveray@gmail.com>
 *
 * Created on Sep 2, 2008
 */
package org.jsoar.kernel.rete;

import org.jsoar.kernel.Wme;

/**
 * @author ray
 */
public interface ReteTestRoutine
{
    boolean execute(Rete rete, ReteTest rt, LeftToken left, Wme w);
}
