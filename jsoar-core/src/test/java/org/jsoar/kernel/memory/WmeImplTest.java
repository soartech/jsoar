/*
 * Copyright (c) 2010 Dave Ray <daveray@gmail.com>
 *
 * Created on Jun 12, 2010
 */
package org.jsoar.kernel.memory;

import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;

import org.jsoar.JSoarTest;
import org.jsoar.kernel.GoalDependencySet;
import org.jsoar.kernel.GoalDependencySetImpl;
import org.jsoar.kernel.symbols.GoalIdentifierInfo;
import org.jsoar.kernel.symbols.IdentifierImpl;
import org.jsoar.util.adaptables.Adaptables;
import org.junit.jupiter.api.Test;

class WmeImplTest extends JSoarTest
{
    
    @Test
    void testIsAdaptableToGoalDependencySet()
    {
        final IdentifierImpl id = syms.createIdentifier('S');
        id.goalInfo = new GoalIdentifierInfo(id);
        id.goalInfo.gds = new GoalDependencySetImpl(id);
        final WmeImpl wme = new WmeImpl(id, syms.createString("hi"), syms.createInteger(99), true, 0);
        assertNull(Adaptables.adapt(wme, GoalDependencySet.class));
        id.goalInfo.gds.addWme(wme);
        assertSame(id.goalInfo.gds, Adaptables.adapt(wme, GoalDependencySet.class));
        
    }
    
}
