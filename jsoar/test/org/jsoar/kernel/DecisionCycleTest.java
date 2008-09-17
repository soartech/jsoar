/*
 * Copyright (c) 2008  Dave Ray <daveray@gmail.com>
 *
 * Created on Sep 16, 2008
 */
package org.jsoar.kernel;


import org.junit.After;
import org.junit.Before;
import org.junit.Test;

/**
 * @author ray
 */
public class DecisionCycleTest
{
    private Agent agent;
    
    /**
     * @throws java.lang.Exception
     */
    @Before
    public void setUp() throws Exception
    {
        this.agent = new Agent();
        this.agent.trace.enableAll();
    }

    /**
     * @throws java.lang.Exception
     */
    @After
    public void tearDown() throws Exception
    {
        this.agent.getPrinter().flush();
    }

    @Test
    public void testDoOneTopLevelPhase() throws Exception
    {
        this.agent.decisionCycle.do_one_top_level_phase();
        this.agent.decisionCycle.do_one_top_level_phase();
        // TODO keep going on this test :)
//        this.agent.decisionCycle.do_one_top_level_phase();
//        this.agent.decisionCycle.do_one_top_level_phase();
    }
}
