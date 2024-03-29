/*
 * Copyright (c) 2008  Dave Ray <daveray@gmail.com>
 *
 * Created on Sep 16, 2008
 */
package org.jsoar.kernel;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.jsoar.kernel.symbols.IdentifierImpl;
import org.jsoar.kernel.symbols.SymbolFactoryImpl;
import org.jsoar.util.adaptables.Adaptables;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/**
 * @author ray
 */
class DecisionCycleTest
{
    private Agent agent;
    private DecisionCycle decisionCycle;
    
    /**
     * @throws java.lang.Exception
     */
    @BeforeEach
    void setUp() throws Exception
    {
        this.agent = new Agent();
        this.agent.getTrace().enableAll();
        
        this.decisionCycle = Adaptables.adapt(this.agent, DecisionCycle.class);
    }
    
    /**
     * @throws java.lang.Exception
     */
    @AfterEach
    void tearDown() throws Exception
    {
        this.agent.getPrinter().flush();
    }
    
    @Test
    void testDoOneTopLevelPhaseWithEmptyAgent() throws Exception
    {
        final Decider decider = Adaptables.adapt(agent, Decider.class);
        for(int i = 1; i < 10; ++i)
        {
            assertEquals(Phase.INPUT, this.decisionCycle.current_phase.get());
            this.decisionCycle.runFor(1, RunType.PHASES);
            assertEquals(Phase.PROPOSE, this.decisionCycle.current_phase.get());
            this.decisionCycle.runFor(1, RunType.PHASES);
            assertEquals(Phase.DECISION, this.decisionCycle.current_phase.get());
            this.decisionCycle.runFor(1, RunType.PHASES);
            assertEquals(Phase.APPLY, this.decisionCycle.current_phase.get());
            this.decisionCycle.runFor(1, RunType.PHASES);
            assertEquals(Phase.OUTPUT, this.decisionCycle.current_phase.get());
            this.decisionCycle.runFor(1, RunType.PHASES);
            
            // Verify that new states are being generated
            assertEquals("S" + (1 + 2 * i), decider.bottom_goal.toString());
        }
    }
    
    @Test
    void testDoOneTopLevelPhaseWithSimpleProduction() throws Exception
    {
        agent.getProductions().loadProduction("test1 (state <s> ^superstate nil) --> (<s> ^foo 1)");
        agent.getProductions().loadProduction("test2 (state <s> ^superstate nil ^foo 1) --> (write (crlf) |test2 matched!|)");
        
        assertNull(agent.getProductions().getProduction("test2").instantiations);
        
        assertEquals(Phase.INPUT, this.decisionCycle.current_phase.get());
        this.decisionCycle.runFor(1, RunType.PHASES);
        assertEquals(Phase.PROPOSE, this.decisionCycle.current_phase.get());
        this.decisionCycle.runFor(1, RunType.PHASES);
        assertEquals(Phase.DECISION, this.decisionCycle.current_phase.get());
        this.decisionCycle.runFor(1, RunType.PHASES);
        assertEquals(Phase.APPLY, this.decisionCycle.current_phase.get());
        this.decisionCycle.runFor(1, RunType.PHASES);
        assertEquals(Phase.OUTPUT, this.decisionCycle.current_phase.get());
        this.decisionCycle.runFor(1, RunType.PHASES);
        
        // verify that (S1 foo 1) is being added to the rete by checking that test2 fired
        assertNotNull(agent.getProductions().getProduction("test2").instantiations);
        
        // Verify that new states are being generates
        final Decider decider = Adaptables.adapt(agent, Decider.class);
        assertEquals("S3", decider.bottom_goal.toString());
    }
    
    @Test
    void testWaitOperatorOnStateNoChange() throws Exception
    {
        // A production that just proposes a wait operator every cycle
        agent.getProductions().loadProduction(
                "top-state*propose*wait\n" +
                        "   (state <s> ^attribute state\n" +
                        "              ^choices none\n" +
                        "             -^operator.name wait)\n" +
                        "-->\n" +
                        "   (<s> ^operator <o> +)\n" +
                        "   (<o> ^name wait)");
        
        for(int i = 1; i < 10; ++i)
        {
            assertEquals(Phase.INPUT, this.decisionCycle.current_phase.get());
            this.decisionCycle.runFor(1, RunType.PHASES);
            assertEquals(Phase.PROPOSE, this.decisionCycle.current_phase.get());
            this.decisionCycle.runFor(1, RunType.PHASES);
            assertEquals(Phase.DECISION, this.decisionCycle.current_phase.get());
            this.decisionCycle.runFor(1, RunType.PHASES);
            assertEquals(Phase.APPLY, this.decisionCycle.current_phase.get());
            this.decisionCycle.runFor(1, RunType.PHASES);
            assertEquals(Phase.OUTPUT, this.decisionCycle.current_phase.get());
            this.decisionCycle.runFor(1, RunType.PHASES);
            
            // Verify that one state-no-change occurs, producing S3, but no further
            // states are generated. Also verify that the current operator.
            final Decider decider = Adaptables.adapt(agent, Decider.class);
            assertEquals("S3", decider.bottom_goal.toString());
            validateLastOperator(i);
        }
    }
    
    @Test
    void testHaltRHS() throws Exception
    {
        agent.getProductions().loadProduction("test1 (state <s> ^superstate nil) --> (<s> ^operator.name halt)");
        agent.getProductions().loadProduction("test2 (state <s> ^operator.name halt) --> (halt)");
        for(int i = 0; i < 3; i++)
        {
            if(i <= 3)
            {
                this.decisionCycle.runFor(1, RunType.PHASES);
                assertFalse(decisionCycle.isHalted());
                assertFalse(decisionCycle.isStopped());
            }
            else
            {
                this.decisionCycle.runFor(1, RunType.PHASES);
                assertTrue(decisionCycle.isHalted());
                assertTrue(decisionCycle.isStopped());
            }
        }
    }
    
    private void validateLastOperator(long number)
    {
        final SymbolFactoryImpl syms = Adaptables.adapt(agent, SymbolFactoryImpl.class);
        IdentifierImpl last = syms.findIdentifier('O', number);
        assertNotNull(last);
        assertTrue(last.isa_operator > 0);
        
        IdentifierImpl next = syms.findIdentifier('O', number + 1);
        assertNull(next);
    }
}
