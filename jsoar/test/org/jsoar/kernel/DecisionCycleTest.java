/*
 * Copyright (c) 2008  Dave Ray <daveray@gmail.com>
 *
 * Created on Sep 16, 2008
 */
package org.jsoar.kernel;


import static org.junit.Assert.*;

import org.jsoar.kernel.symbols.IdentifierImpl;
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
        this.agent.initialize();
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
    public void testDoOneTopLevelPhaseWithEmptyAgent() throws Exception
    {
        for(int i = 1; i < 10; ++i)
        {
            assertEquals(Phase.INPUT_PHASE, this.agent.decisionCycle.current_phase);
            this.agent.decisionCycle.do_one_top_level_phase();
            assertEquals(Phase.PROPOSE_PHASE, this.agent.decisionCycle.current_phase);
            this.agent.decisionCycle.do_one_top_level_phase();
            assertEquals(Phase.DECISION_PHASE, this.agent.decisionCycle.current_phase);
            this.agent.decisionCycle.do_one_top_level_phase();
            assertEquals(Phase.APPLY_PHASE, this.agent.decisionCycle.current_phase);
            this.agent.decisionCycle.do_one_top_level_phase();
            assertEquals(Phase.OUTPUT_PHASE, this.agent.decisionCycle.current_phase);
            this.agent.decisionCycle.do_one_top_level_phase();
            
            // Verify that new states are being generates
            assertEquals("S" + (i + 1), agent.decider.bottom_goal.toString());
        }
    }
    
    @Test
    public void testDoOneTopLevelPhaseWithSimpleProduction() throws Exception
    {
        agent.loadProduction("test1 (state <s> ^superstate nil) --> (<s> ^foo 1)");
        agent.loadProduction("test2 (state <s> ^superstate nil ^foo 1) --> (write (crlf) |test2 matched!|)");
        
        assertTrue(agent.getProduction("test2").instantiations.isEmpty());
        
        assertEquals(Phase.INPUT_PHASE, this.agent.decisionCycle.current_phase);
        this.agent.decisionCycle.do_one_top_level_phase();
        assertEquals(Phase.PROPOSE_PHASE, this.agent.decisionCycle.current_phase);
        this.agent.decisionCycle.do_one_top_level_phase();
        assertEquals(Phase.DECISION_PHASE, this.agent.decisionCycle.current_phase);
        this.agent.decisionCycle.do_one_top_level_phase();
        assertEquals(Phase.APPLY_PHASE, this.agent.decisionCycle.current_phase);
        this.agent.decisionCycle.do_one_top_level_phase();
        assertEquals(Phase.OUTPUT_PHASE, this.agent.decisionCycle.current_phase);
        this.agent.decisionCycle.do_one_top_level_phase();
        
        // verify that (S1 foo 1) is being added to the rete by checking that test2 fired
        assertFalse(agent.getProduction("test2").instantiations.isEmpty());
        
        // Verify that new states are being generates
        assertEquals("S2", agent.decider.bottom_goal.toString());
    }
    
    @Test
    public void testWaitOperatorOnStateNoChange() throws Exception
    {
        // A production that just proposes a wait operator every cycle
        agent.loadProduction(
                "top-state*propose*wait\n" +
                "   (state <s> ^attribute state\n" +
                "              ^choices none\n" +
                "             -^operator.name wait)\n" +
                "-->\n" +
                "   (<s> ^operator <o> +)\n" +
                "   (<o> ^name wait)");
        
        for(int i = 1; i < 10; ++i)
        {
            assertEquals(Phase.INPUT_PHASE, this.agent.decisionCycle.current_phase);
            this.agent.decisionCycle.do_one_top_level_phase();
            assertEquals(Phase.PROPOSE_PHASE, this.agent.decisionCycle.current_phase);
            this.agent.decisionCycle.do_one_top_level_phase();
            assertEquals(Phase.DECISION_PHASE, this.agent.decisionCycle.current_phase);
            this.agent.decisionCycle.do_one_top_level_phase();
            assertEquals(Phase.APPLY_PHASE, this.agent.decisionCycle.current_phase);
            this.agent.decisionCycle.do_one_top_level_phase();
            assertEquals(Phase.OUTPUT_PHASE, this.agent.decisionCycle.current_phase);
            this.agent.decisionCycle.do_one_top_level_phase();
            
            // Verify that one state-no-change occurs, producting S2, but no further
            // states are generated. Also verify that the current operator.
            assertEquals("S2", agent.decider.bottom_goal.toString());
            validateLastOperator(i);
        }
    }
    
    private void validateLastOperator(int number)
    {
        IdentifierImpl last = agent.syms.findIdentifier('O', number);
        assertNotNull(last);
        assertTrue(last.isa_operator > 0);
        
        IdentifierImpl next = agent.syms.findIdentifier('O', number + 1);
        assertNull(next);
    }
}
