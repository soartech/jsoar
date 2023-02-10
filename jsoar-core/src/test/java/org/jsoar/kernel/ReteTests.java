/*
 * Copyright (c) 2008  Dave Ray <daveray@gmail.com>
 *
 * Created on Sep 19, 2008
 */
package org.jsoar.kernel;

import static org.junit.jupiter.api.Assertions.assertFalse;

import org.jsoar.util.adaptables.Adaptables;
import org.junit.jupiter.api.Test;

/**
 * @author ray
 */
public class ReteTests extends FunctionalTestHarness
{
    @Test
    public void testTowersOfHanoiProductionThatCrashesRete() throws Exception
    {
        // 9/24/2008 - This production caused a crash in the initial match of the
        // production. Nothing to test other than that no exceptions are thrown.
        agent.getProductions().loadProduction("towers-of-hanoi*propose*initialize\n" +
                "   (state <s> ^superstate nil\n" +
                "             -^name)\n" +
                "-->\n" +
                "   (<s> ^operator <o> +)\n" +
                "   (<o> ^name initialize-toh)");
    }
    
    @Test
    public void testTowersOfHanoiProductionThatCausesMaxElaborations() throws Exception
    {
        // 9/24/2008 - This production caused a crash in the initial match of the
        // production. Nothing to test other than that no exceptions are thrown.
        agent.getProductions().loadProduction("towers-of-hanoi*propose*initialize\n" +
                "   (state <s> ^superstate nil\n" +
                "             -^name)\n" +
                "-->\n" +
                "   (<s> ^operator <o> +)\n" +
                "   (<o> ^name initialize-toh)");
        
        agent.getProperties().set(SoarProperties.MAX_ELABORATIONS, 5);
        agent.runFor(1, RunType.DECISIONS);
        final DecisionCycle dc = Adaptables.adapt(agent, DecisionCycle.class);
        assertFalse(dc.isHitMaxElaborations());
    }
    
    @Test
    public void testSplitNode() throws Exception
    {
        agent.getProductions().loadProduction("first\n" +
                "   (<s> ^foo1 nil)\n" +
                "-->\n" +
                "   (interrupt)");
        agent.getProductions().loadProduction("second\n" +
                "   (<s> ^foo2 nil)\n" +
                "-->\n" +
                "   (interrupt)");
        
    }
}
