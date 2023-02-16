/*
 * Copyright (c) 2009 Dave Ray <daveray@gmail.com>
 *
 * Created on Jun 20, 2009
 */
package org.jsoar.kernel.rhs.functions;

import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;

import org.jsoar.JSoarTest;
import org.jsoar.kernel.Agent;
import org.jsoar.kernel.RunType;
import org.jsoar.kernel.symbols.Symbol;
import org.jsoar.util.ByRef;
import org.junit.jupiter.api.Test;

/**
 * @author ray
 */
class SplitTest extends JSoarTest
{
    @Test
    void testSplit() throws Exception
    {
        final ByRef<Boolean> succeeded = ByRef.create(false);
        final Agent agent = new Agent();
        agent.getTrace().disableAll();
        agent.getRhsFunctions().registerHandler(new StandaloneRhsFunctionHandler("succeeded")
        {
            
            @Override
            public Symbol execute(RhsFunctionContext context,
                    List<Symbol> arguments) throws RhsFunctionException
            {
                succeeded.value = true;
                return null;
            }
        });
        agent.getProductions().loadProduction("" +
                "callSplit (state <s> ^superstate nil) " +
                "--> " +
                "(<s> ^result (split |string to split| | |))");
        agent.getProductions().loadProduction("" +
                "checkResult (state <s> ^superstate nil ^result <r>) " +
                "(<r> ^value string ^next <n1>)" +
                "(<n1> ^value to ^next <n2>)" +
                "(<n2> ^value split ^next nil) " +
                "-->" +
                "(succeeded)");
        
        agent.runFor(1, RunType.DECISIONS);
        assertTrue(succeeded.value);
    }
}
