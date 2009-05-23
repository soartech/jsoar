/*
 * Copyright (c) 2008  Dave Ray <daveray@gmail.com>
 *
 * Created on Oct 28, 2008
 */
package org.jsoar.kernel.memory;

import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertSame;

import org.jsoar.kernel.Agent;
import org.jsoar.kernel.Phase;
import org.jsoar.kernel.RunType;
import org.jsoar.kernel.symbols.Identifier;
import org.junit.Test;

/**
 * @author ray
 */
public class ContextVariableInfoTest
{

    /**
     * Test method for {@link org.jsoar.kernel.memory.ContextVariableInfo#get(org.jsoar.kernel.PredefinedSymbols, org.jsoar.kernel.symbols.IdentifierImpl, org.jsoar.kernel.symbols.IdentifierImpl, java.lang.String)}.
     */
    @Test
    public void testGetCurrentOperator() throws Exception
    {
        Agent agent = new Agent();
        agent.initialize();
        
        agent.getProductions().loadProduction("propose (state <s> ^superstate nil -^done) --> (<s> ^operator <o>)(<o> ^name test)");
        agent.getProductions().loadProduction("apply (state <s> ^operator <o>) (<o> ^name test) --> (<s> ^done *yes*)");
        
        while(agent.getCurrentPhase() != Phase.APPLY)
        {
            agent.runFor(1, RunType.PHASES);
        }
        ContextVariableInfo info = ContextVariableInfo.get(agent.predefinedSyms, agent.decider.top_goal, agent.decider.bottom_goal, "<o>");
        final Identifier o1 = agent.getSymbols().findIdentifier('O', 1);
        assertNotNull(o1);
        assertSame(o1, info.getValue());
    }

}
