/*
 * Copyright (c) 2010 Dave Ray <daveray@gmail.com>
 *
 * Created on Sep 3, 2010
 */
package org.jsoar.kernel;


import junit.framework.Assert;

import org.jsoar.kernel.lhs.PositiveCondition;
import org.jsoar.kernel.rhs.MakeAction;

public class ProductionTest
{

    public void testSetBreakpointEnabledHasNoAffectWhenInterruptFlagIsSet()
    {
        final Production p = Production.newBuilder()
            .type(ProductionType.USER)
            .name("test")
            .conditions(new PositiveCondition(), new PositiveCondition())
            .actions(new MakeAction())
            .interrupt(true)
            .build();

        Assert.assertTrue(p.isBreakpointEnabled());
        p.setBreakpointEnabled(false);
        Assert.assertTrue(p.isBreakpointEnabled());
    }
    
    public void testSetBreakpointEnabledHasAffectWhenInterruptFlagIsNotSet()
    {
        final Production p = Production.newBuilder()
            .type(ProductionType.USER)
            .name("test")
            .conditions(new PositiveCondition(), new PositiveCondition())
            .actions(new MakeAction())
            .build();
        
        Assert.assertFalse(p.isBreakpointEnabled());
        p.setBreakpointEnabled(true);
        Assert.assertTrue(p.isBreakpointEnabled());
    }
    
}
