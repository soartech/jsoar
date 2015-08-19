/*
 * Copyright (c) 2008  Dave Ray <daveray@gmail.com>
 *
 * Created on Oct 28, 2008
 */
package org.jsoar.kernel.memory;

import android.test.AndroidTestCase;

import org.jsoar.kernel.Agent;
import org.jsoar.kernel.Decider;
import org.jsoar.kernel.Phase;
import org.jsoar.kernel.PredefinedSymbols;
import org.jsoar.kernel.RunType;
import org.jsoar.kernel.symbols.Identifier;
import org.jsoar.util.adaptables.Adaptables;

/**
 * @author ray
 */
public class ContextVariableInfoTest extends AndroidTestCase
{

    /**
     * Test method for {@link org.jsoar.kernel.memory.ContextVariableInfo#get(org.jsoar.kernel.PredefinedSymbols, org.jsoar.kernel.symbols.IdentifierImpl, org.jsoar.kernel.symbols.IdentifierImpl, java.lang.String)}.
     */
    public void testGetCurrentOperator() throws Exception
    {
        final Agent agent = new Agent(getContext());
        
        agent.getProductions().loadProduction("propose (state <s> ^superstate nil -^done) --> (<s> ^operator <o>)(<o> ^name test)");
        agent.getProductions().loadProduction("apply (state <s> ^operator <o>) (<o> ^name test) --> (<s> ^done *yes*)");
        
        while(agent.getCurrentPhase() != Phase.APPLY)
        {
            agent.runFor(1, RunType.PHASES);
        }
        
        final Decider decider = Adaptables.adapt(agent, Decider.class);
        final PredefinedSymbols predefinedSyms = Adaptables.adapt(agent, PredefinedSymbols.class);
        ContextVariableInfo info = ContextVariableInfo.get(predefinedSyms, decider.top_goal, decider.bottom_goal, "<o>");
        final Identifier o1 = agent.getSymbols().findIdentifier('O', 1);
        assertNotNull(o1);
        assertSame(o1, info.getValue());
    }

}
