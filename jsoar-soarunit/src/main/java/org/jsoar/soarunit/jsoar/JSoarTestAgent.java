/*
 * Copyright (c) 2010 Dave Ray <daveray@gmail.com>
 *
 * Created on Jul 27, 2010
 */
package org.jsoar.soarunit.jsoar;

import java.io.StringWriter;

import org.jsoar.kernel.Agent;
import org.jsoar.kernel.Production;
import org.jsoar.kernel.ProductionType;
import org.jsoar.kernel.RunType;
import org.jsoar.kernel.SoarException;
import org.jsoar.kernel.SoarProperties;
import org.jsoar.kernel.tracing.Printer;
import org.jsoar.kernel.tracing.Trace.WmeTraceType;
import org.jsoar.soarunit.FiringCounts;
import org.jsoar.soarunit.Test;
import org.jsoar.soarunit.TestAgent;
import org.jsoar.util.FileTools;
import org.jsoar.util.StringTools;

/**
 * @author ray
 */
public class JSoarTestAgent implements TestAgent
{
    private Agent agent;
    private StringWriter output;
    private TestRhsFunction passFunction;
    private TestRhsFunction failFunction;
    
    public JSoarTestAgent()
    {
    }

    /* (non-Javadoc)
     * @see org.jsoar.soarunit.TestAgent#dispose()
     */
    @Override
    public void dispose()
    {
        agent.dispose();
        agent = null;
        passFunction = null;
        failFunction = null;
        output = null;
    }

    /* (non-Javadoc)
     * @see org.jsoar.soarunit.TestAgent#getFiringCounts()
     */
    @Override
    public FiringCounts getFiringCounts()
    {
        final FiringCounts result = new FiringCounts();
        for(Production p : agent.getProductions().getProductions(ProductionType.USER))
        {
            result.adjust(p.getName(), p.getFiringCount());
        }
        return result;
    }

    /* (non-Javadoc)
     * @see org.jsoar.soarunit.TestAgent#getOutput()
     */
    @Override
    public String getOutput()
    {
        agent.getPrinter().flush();
        return output.toString();
    }

    /* (non-Javadoc)
     * @see org.jsoar.soarunit.TestAgent#isFailCalled()
     */
    @Override
    public boolean isFailCalled()
    {
        return failFunction.isCalled();
    }

    /* (non-Javadoc)
     * @see org.jsoar.soarunit.TestAgent#getFailMessage()
     */
    @Override
    public String getFailMessage()
    {
        return StringTools.join(failFunction.getArguments(), ", ");
    }

    /* (non-Javadoc)
     * @see org.jsoar.soarunit.TestAgent#printMatchesOnFailure()
     */
    @Override
    public void printMatchesOnFailure()
    {
        final Printer printer = agent.getPrinter();
        printer.startNewLine().print("# Matches for pass rules #\n");
        for(Production p : agent.getProductions().getProductions(null))
        {
            if(p.getName().startsWith("pass"))
            {
                printer.startNewLine().print("Partial matches for rule '%s'\n", p.getName());
                p.printPartialMatches(printer, WmeTraceType.NONE);
            }
        }
        printer.flush();
    }

    /* (non-Javadoc)
     * @see org.jsoar.soarunit.TestAgent#isPassCalled()
     */
    @Override
    public boolean isPassCalled()
    {
        return passFunction.isCalled();
    }

    /* (non-Javadoc)
     * @see org.jsoar.soarunit.TestAgent#getPassMessage()
     */
    @Override
    public String getPassMessage()
    {
        return StringTools.join(passFunction.getArguments(), ", ");
    }

    /* (non-Javadoc)
     * @see org.jsoar.soarunit.TestAgent#initialize(org.jsoar.soarunit.Test)
     */
    @Override
    public void initialize(Test test) throws SoarException
    {
        output = new StringWriter();
        agent = new Agent(test.getName());
        agent.getTrace().setWatchLevel(0);
        agent.getPrinter().addPersistentWriter(output);
        
        passFunction = TestRhsFunction.addTestFunction(agent, "pass");
        failFunction = TestRhsFunction.addTestFunction(agent, "fail");
        
        agent.getInterpreter().eval(String.format("pushd \"%s\"", FileTools.getParent(test.getTestCase().getFile()).replace('\\', '/')));
        agent.getInterpreter().eval(test.getTestCase().getSetup());
        agent.getInterpreter().eval(test.getContent());
    }

    /* (non-Javadoc)
     * @see org.jsoar.soarunit.TestAgent#run()
     */
    @Override
    public void run()
    {
        final int cycles = 50000; 
        agent.runFor(cycles, RunType.DECISIONS);
        agent.getPrinter().flush();
    }

    /* (non-Javadoc)
     * @see org.jsoar.soarunit.TestAgent#getCycleCount()
     */
    @Override
    public long getCycleCount()
    {
        return agent.getProperties().get(SoarProperties.D_CYCLE_COUNT);
    }

    
}
