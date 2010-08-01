/*
 * Copyright (c) 2010 Dave Ray <daveray@gmail.com>
 *
 * Created on Jul 27, 2010
 */
package org.jsoar.soarunit.jsoar;

import java.util.HashMap;
import java.util.Map;

import org.jsoar.kernel.DebuggerProvider;
import org.jsoar.kernel.SoarException;
import org.jsoar.kernel.DebuggerProvider.CloseAction;
import org.jsoar.runtime.ThreadedAgent;
import org.jsoar.soarunit.Test;
import org.jsoar.soarunit.TestAgent;
import org.jsoar.soarunit.TestAgentFactory;
import org.jsoar.soarunit.TestCase;
import org.jsoar.util.FileTools;

/**
 * @author ray
 */
public class JSoarTestAgentFactory implements TestAgentFactory
{

    /* (non-Javadoc)
     * @see org.jsoar.soarunit.TestAgentFactory#createTestAgent(org.jsoar.soarunit.Test)
     */
    @Override
    public TestAgent createTestAgent()
    {
        return new JSoarTestAgent();
    }

    /* (non-Javadoc)
     * @see org.jsoar.soarunit.TestAgentFactory#debugTest(org.jsoar.soarunit.Test)
     */
    @Override
    public void debugTest(Test test, boolean exitOnClose) throws SoarException, InterruptedException
    {
        final ThreadedAgent agent = ThreadedAgent.create(test.getName());
        
        TestRhsFunction.addTestFunction(agent.getAgent(), "pass");
        TestRhsFunction.addTestFunction(agent.getAgent(), "fail");
        
        final Map<String, Object> debugProps = new HashMap<String, Object>();
        debugProps.put(DebuggerProvider.CLOSE_ACTION, exitOnClose ? CloseAction.EXIT : CloseAction.DISPOSE);
        agent.getDebuggerProvider().setProperties(debugProps);
        agent.openDebuggerAndWait();
        
        final TestCase testCase = test.getTestCase();
        agent.getPrinter().print("SoarUnit: Debugging %s%n", test);
        try
        {
            agent.getInterpreter().eval(String.format("pushd \"%s\"", FileTools.getParent(testCase.getFile()).replace('\\', '/')));
            agent.getInterpreter().eval(testCase.getSetup());
            
            agent.getInterpreter().eval(test.getContent());
        }
        catch (SoarException e)
        {
            agent.getPrinter().error(e.getMessage());
        }
        agent.getPrinter().flush();
    }

}
