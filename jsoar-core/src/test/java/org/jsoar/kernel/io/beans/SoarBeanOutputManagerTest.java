/*
 * Copyright (c) 2009 Dave Ray <daveray@gmail.com>
 *
 * Created on May 24, 2009
 */
package org.jsoar.kernel.io.beans;


import static org.junit.Assert.*;

import org.jsoar.kernel.Agent;
import org.jsoar.kernel.Phase;
import org.jsoar.kernel.RunType;
import org.jsoar.kernel.SoarProperties;
import org.jsoar.util.ByRef;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

/**
 * @author ray
 */
public class SoarBeanOutputManagerTest
{
    private Agent agent;
    private SoarBeanOutputManager output;
    
    /**
     * @throws java.lang.Exception
     */
    @Before
    public void setUp() throws Exception
    {
        this.agent = new Agent(false);
        
        this.output = new SoarBeanOutputManager(agent.getEvents());
        
        // Since this is the SoarBeanOutput tests, these tests have to stop after output
        // (ie. before INPUT). I changed this to Phase.APPLY so this broke all the tests.
        // - ALT
        this.agent.setStopPhase(Phase.INPUT);
        this.agent.initialize();
        this.agent.getProperties().set(SoarProperties.WAITSNC, true);
    }

    /**
     * @throws java.lang.Exception
     */
    @After
    public void tearDown() throws Exception
    {
        this.output.dispose();
    }

    public static class Point
    {
        public int x;
        public int y;
    }
    
    public static class MoveToPoint
    {
        public Point target;
        public double speed;
    }
    
    @Test
    public void testRegisterHandler() throws Exception
    {
        final ByRef<MoveToPoint> commandHolder = ByRef.create(null);
        final SoarBeanOutputHandler<MoveToPoint> handler = new SoarBeanOutputHandler<MoveToPoint>() {

            @Override
            public void handleOutputCommand(SoarBeanOutputContext context, MoveToPoint bean)
            {
                assertEquals("move-to-point", context.getCommand().getAttribute().toString());
                assertSame(agent.getInputOutput(), context.getInputOutput());
                commandHolder.value = bean;
                context.setStatus("all done");
            }};
        output.registerHandler("move-to-point", handler, MoveToPoint.class);
        
        agent.getProductions().loadProduction("testRegisterHandler\n" +
                "(state <s> ^superstate nil ^io.output-link <ol>)" +
                "-->\n" +
                "(<ol> ^move-to-point <mtp>)\n" +
                "(<mtp> ^speed 5 ^target <tgt>)\n" +
                "(<tgt> ^x 1 ^y 2)\n" +
                "");
        
        agent.runFor(1, RunType.DECISIONS);
        
        assertNotNull(commandHolder.value);
        assertEquals(5.0, commandHolder.value.speed, 0.00001);
        final Point target = commandHolder.value.target;
        assertNotNull(target);
        assertEquals(1, target.x);
        assertEquals(2, target.y);
        
        commandHolder.value = null;
        
        agent.runFor(1, RunType.DECISIONS);
        assertNull(commandHolder.value);
        
    }
}
