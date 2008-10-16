/*
 * Copyright (c) 2008  Dave Ray <daveray@gmail.com>
 *
 * Created on Sep 2, 2008
 */
package org.jsoar.kernel.rete;

import org.jsoar.kernel.memory.WmeImpl;

/**
 * Interface to emulate XXX_rete_test_routine function pointers in rete.cpp
 * 
 * @author ray
 */
public interface ReteTestRoutine
{
    boolean execute(Rete rete, ReteTest rt, LeftToken left, WmeImpl w);
}
