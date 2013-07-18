/*
 * Copyright (c) 2010 Dave Ray <daveray@gmail.com>
 *
 * Created on Jul 27, 2010
 */
package org.jsoar.soarunit.jsoar;

import java.io.StringWriter;
import java.util.HashMap;
import java.util.Map;

import org.jsoar.kernel.Agent;
import org.jsoar.kernel.DebuggerProvider;
import org.jsoar.kernel.Production;
import org.jsoar.kernel.ProductionType;
import org.jsoar.kernel.RunType;
import org.jsoar.kernel.SoarException;
import org.jsoar.kernel.SoarProperties;
import org.jsoar.kernel.DebuggerProvider.CloseAction;
import org.jsoar.kernel.events.BeforeInitSoarEvent;
import org.jsoar.kernel.events.InputEvent;
import org.jsoar.kernel.io.InputOutput;
import org.jsoar.kernel.io.InputWme;
import org.jsoar.kernel.io.InputWmes;
import org.jsoar.kernel.symbols.SymbolFactory;
import org.jsoar.kernel.tracing.Printer;
import org.jsoar.kernel.tracing.Trace.WmeTraceType;
import org.jsoar.runtime.ThreadedAgent;
import org.jsoar.soarunit.FiringCounts;
import org.jsoar.soarunit.Test;
import org.jsoar.soarunit.TestAgent;
import org.jsoar.soarunit.TestCase;
import org.jsoar.util.FileTools;
import org.jsoar.util.StringTools;
import org.jsoar.util.events.SoarEvent;
import org.jsoar.util.events.SoarEventListener;

/**
 * @author ray
 */
public class JSoarTestAgent implements TestAgent
{
    private ThreadedAgent agent; // we need a ThreadedAgent to use the debugger
    private StringWriter output;
    private TestRhsFunction passFunction;
    private TestRhsFunction failFunction;
    
    private final InputListener listener;
    private final InitSoarListener initListener;
    
    SymbolFactory syms;
    private InputWme soarUnitWme;
    private InputWme cycleCountWme;
    
    public JSoarTestAgent()
    {
        this.listener = new InputListener();
        this.initListener = new InitSoarListener();
    }

    /* (non-Javadoc)
     * @see org.jsoar.soarunit.TestAgent#dispose()
     */
    @Override
    public void dispose()
    {
        agent.getEvents().removeListener(null, listener);
        agent.getEvents().removeListener(null, initListener);
        
        // Schedule removal of wme at next input cycle.
        if(cycleCountWme != null)
        {
            this.cycleCountWme.remove();
            this.cycleCountWme = null;
        }
        
        if(soarUnitWme != null)
        {
            this.soarUnitWme.remove();
            this.soarUnitWme = null;
        }
        
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
        commonInitialize(test);
        agent.getTrace().setWatchLevel(0);
        output = new StringWriter();
        agent.getPrinter().addPersistentWriter(output);
        agent.initialize();
        loadTestCode(test);
    }
    
    public void commonInitialize(Test test)
    {
        agent = ThreadedAgent.create(test.getName());
        
        initializeRhsFunctions();
        
        agent.getEvents().addListener(InputEvent.class, listener);
        agent.getEvents().addListener(BeforeInitSoarEvent.class, initListener);
        
        agent.getEvents().addListener(BeforeInitSoarEvent.class, new SoarEventListener() {

            @Override
            public void onEvent(SoarEvent event)
            {
                cycleCountWme = null;                
            }
            
        });
        
        agent.getEvents().addListener(InputEvent.class, new SoarEventListener() {

            @Override
            public void onEvent(SoarEvent event)
            {
                final InputEvent ie = (InputEvent) event;
                update(ie.getInputOutput());
            }
        }); 
        
    }

    private void initializeRhsFunctions()
    {
        passFunction = TestRhsFunction.addTestFunction(agent, "pass");
        failFunction = TestRhsFunction.addTestFunction(agent, "fail");
    }
    
    private void loadTestCode(Test test) throws SoarException
    {
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
        // run using the underlying, non-threaded agent so that we block until the run completes
        agent.getAgent().runFor(cycles, RunType.DECISIONS);
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
    
    /**
     * Updates the input-link. This should only be called during the input cycle.
     */
    private void update(InputOutput io)
    {
        if(cycleCountWme == null)
        {
            soarUnitWme = InputWmes.add(io, "soar-unit", io.getSymbols().createIdentifier('C'));
            cycleCountWme = InputWmes.add(soarUnitWme, "cycle-count", getCycleCount());
        }
        else
        {
            InputWmes.update(cycleCountWme, getCycleCount());
        }
    }
    
    private class InputListener implements SoarEventListener
    {
        /* (non-Javadoc)
         * @see org.jsoar.kernel.events.SoarEventListener#onEvent(org.jsoar.kernel.events.SoarEvent)
         */
        @Override
        public void onEvent(SoarEvent event)
        {
            final InputEvent ie = (InputEvent) event;
            update(ie.getInputOutput());
        }
    }
    
    private class InitSoarListener implements SoarEventListener
    {
        /* (non-Javadoc)
         * @see org.jsoar.util.events.SoarEventListener#onEvent(org.jsoar.util.events.SoarEvent)
         */
        @Override
        public void onEvent(SoarEvent event)
        {
            cycleCountWme = null;
            soarUnitWme = null;
        }
        
    }

    public void debug(Test test, boolean exitOnClose) throws SoarException, InterruptedException
    {
        commonInitialize(test);
        
        final Map<String, Object> debugProps = new HashMap<String, Object>();
        debugProps.put(DebuggerProvider.CLOSE_ACTION, exitOnClose ? CloseAction.EXIT : CloseAction.DISPOSE);
        agent.getDebuggerProvider().setProperties(debugProps);
        agent.openDebuggerAndWait();
       
        loadTestCode(test);
        
        agent.getPrinter().print("SoarUnit: Debugging %s%n", test);
        agent.getPrinter().flush();
        
    }
}
