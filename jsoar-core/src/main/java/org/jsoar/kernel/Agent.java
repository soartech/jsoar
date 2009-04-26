/*
 * (c) 2008  Dave Ray
 *
 * Created on Aug 15, 2008
 */
package org.jsoar.kernel;

import java.io.IOException;
import java.io.OutputStreamWriter;
import java.io.Writer;
import java.util.Arrays;
import java.util.EnumSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Random;
import java.util.Set;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.jsoar.kernel.events.AfterInitSoarEvent;
import org.jsoar.kernel.events.BeforeInitSoarEvent;
import org.jsoar.kernel.exploration.Exploration;
import org.jsoar.kernel.io.InputOutput;
import org.jsoar.kernel.io.InputOutputImpl;
import org.jsoar.kernel.learning.Chunker;
import org.jsoar.kernel.learning.Explain;
import org.jsoar.kernel.learning.rl.ReinforcementLearning;
import org.jsoar.kernel.lhs.MultiAttributes;
import org.jsoar.kernel.memory.ContextVariableInfo;
import org.jsoar.kernel.memory.OSupport;
import org.jsoar.kernel.memory.RecognitionMemory;
import org.jsoar.kernel.memory.TemporaryMemory;
import org.jsoar.kernel.memory.Wme;
import org.jsoar.kernel.memory.WorkingMemory;
import org.jsoar.kernel.rete.Rete;
import org.jsoar.kernel.rete.SoarReteListener;
import org.jsoar.kernel.rhs.functions.RhsFunctionManager;
import org.jsoar.kernel.rhs.functions.StandardFunctions;
import org.jsoar.kernel.symbols.IdentifierImpl;
import org.jsoar.kernel.symbols.Symbol;
import org.jsoar.kernel.symbols.SymbolFactory;
import org.jsoar.kernel.symbols.SymbolFactoryImpl;
import org.jsoar.kernel.tracing.Printer;
import org.jsoar.kernel.tracing.Trace;
import org.jsoar.kernel.tracing.TraceFormatRestriction;
import org.jsoar.kernel.tracing.TraceFormats;
import org.jsoar.kernel.tracing.Trace.Category;
import org.jsoar.kernel.tracing.Trace.MatchSetTraceType;
import org.jsoar.kernel.tracing.Trace.WmeTraceType;
import org.jsoar.util.Arguments;
import org.jsoar.util.adaptables.AbstractAdaptable;
import org.jsoar.util.adaptables.Adaptables;
import org.jsoar.util.events.SoarEventManager;
import org.jsoar.util.properties.PropertyManager;
import org.jsoar.util.timing.DefaultExecutionTimer;
import org.jsoar.util.timing.ExecutionTimer;

/**
 * 
 * <p>The following symbols were removed:
 * <ul>
 * <li>agent.h:728:operand2_mode
 * </ul>
 * 
 * @author ray
 */
public class Agent extends AbstractAdaptable
{
    private static final Log logger = LogFactory.getLog(Agent.class);
    
    private String name = "JSoar Agent " + System.identityHashCode(this);
    
    private DebuggerProvider debuggerProvider = new DefaultDebuggerProvider();
    private Printer printer = new Printer(new OutputStreamWriter(System.out), true);
    
    /**
     * The random number generator used throughout the agent
     */
    private final Random random = new Random();
    
    private final PropertyManager properties = new PropertyManager();
    private final Trace trace = new Trace(printer);
    private final TraceFormats traceFormats = new TraceFormats(this);
    
    public final SymbolFactoryImpl syms = new SymbolFactoryImpl();
    public final PredefinedSymbols predefinedSyms = new PredefinedSymbols(syms);
    private final MultiAttributes multiAttrs = new MultiAttributes();
    private final Rete rete = new Rete(trace, syms);
    private final WorkingMemory workingMemory = new WorkingMemory();
    private final TemporaryMemory tempMemory = new TemporaryMemory();
    private final OSupport osupport = new OSupport(predefinedSyms, printer);
    private final SoarReteListener soarReteListener = new SoarReteListener(this, rete);
    public final RecognitionMemory recMemory = new RecognitionMemory(this);
    
    private final Exploration exploration = new Exploration(this);
    public final Decider decider = new Decider(this);
    private final Consistency consistency = new Consistency(this);
    
    private final Chunker chunker = new Chunker(this);
    private final Explain explain = new Explain(this);
    public final ReinforcementLearning rl = new ReinforcementLearning(this);
    
    private final DecisionManipulation decisionManip = new DecisionManipulation(decider, random);
    private final InputOutputImpl io = new InputOutputImpl(this);
    
    private final RhsFunctionManager rhsFunctions = new RhsFunctionManager(recMemory.getRhsFunctionContext());
    private final DecisionCycle decisionCycle = new DecisionCycle(this);
    private SoarEventManager eventManager = new SoarEventManager();
    private DefaultProductionManager productions = new DefaultProductionManager(this);
    
    /**
     * agent.h:480:total_cpu_time
     */
    private final ExecutionTimer totalCpuTimer = DefaultExecutionTimer.newInstance().setName("Total CPU time");
    /**
     * agent.h:482:total_kernel_time
     */
    private final ExecutionTimer totalKernelTimer = DefaultExecutionTimer.newInstance().setName("Total kernel time");
    
    /**
     * <p>agent.h:688:attribute_preferences_mode
     * <p>Defaults to 0 in agent.cpp
     */
    public int attribute_preferences_mode = 0;
    
    private boolean initialized = false;
    
    /**
     * The objects in this list are retrievable by requesting them, by class,
     * using the adaptables framework, i.e. {@link #getAdapter(Class)}
     * 
     * <p>For example:
     * <pre>{@code
     *    InputOutputImpl io = Adaptables.adapt(agent, InputOutputImpl.class);
     * }</pre>
     * 
     * This allows these fields to be private (not cluttering up the public interface) 
     * while still making them accessible to the spaghetti that is the kernel at the
     * moment. Hopefully, it will become less necessary as the system is cleaned up.
     */
    private final List<Object> adaptables = Arrays.asList((Object) 
            decisionManip, exploration, io, traceFormats, properties, 
            chunker, explain, decisionCycle, rete, predefinedSyms, 
            predefinedSyms.getSyms(), decider, printer, rhsFunctions,
            workingMemory, tempMemory, osupport, soarReteListener,
            consistency,
            debuggerProvider);
    
    public Agent()
    {
        // Initialize components that rely on adaptables lookup
        decider.initialize();
        decisionCycle.initialize();
        rl.initialize();
        recMemory.initialize();
        chunker.initialize();
        consistency.initialize();
        traceFormats.initalize();
        productions.initialize();
        workingMemory.initialize(this);
        io.initialize();
        soarReteListener.initialize();
        
        // Set up standard RHS functions
        new StandardFunctions(this);
        installDefaultTraceFormats();
    }
    
    /**
     * @return the name of the agent
     */
    public String getName()
    {
        return name;
    }

    /**
     * Set the name of the agent
     * 
     * @param name the name to set
     */
    public void setName(String name)
    {
        this.name = name;
    }

    /**
     * The agent's current debugger provider
     * 
     * @return the  current debugger provider
     */
    public DebuggerProvider getDebuggerProvider()
    {
        return debuggerProvider;
    }

    /**
     * Set the agent's current debugger provider. This is the mechanism used
     * by the debug RHS function to launch a debugger.
     * 
     * @param debuggerProvider the debuggerProvider to set
     */
    public void setDebuggerProvider(DebuggerProvider debuggerProvider)
    {
        Arguments.checkNotNull(debuggerProvider, "debuggerProvider");
        this.debuggerProvider = debuggerProvider;
    }

    /**
     * Must be called before the agent is run. This is separate from the 
     * constructor to give client code the change to register callbacks,
     * modify the trace level or printer, etc before the agent is initialized,
     * which may initiate these actions.
     *  
     * <p>If called again, this function is equivalent to the "init-soar" 
     * command.
     */
    public void initialize()
    {
        if(!initialized)
        {
            // TODO Call automatically if any function that requires it is called?
            init_agent_memory();
            initialized = true;
        }
        else
        {
            reinitialize_soar();
            init_agent_memory();
        }
    }
    
    /**
     * Returns the property manager for this agent. All agent configuration
     * (waitsnc, learn, etc) should be performed programmatically through
     * the property manager
     * 
     * @return The agent's property manager
     */
    public PropertyManager getProperties()
    {
        return properties;
    }
    
    /**
     * @return the agent's printer object
     */
    public Printer getPrinter()
    {
        return printer;
    }
    
    /**
     * @return the agent's trace object
     */
    public Trace getTrace()
    {
        return trace;
    }
    
    /**
     * Returns the agents RHS function manager. Use this interface to register
     * new RHS functions.
     * 
     * @return the agent's RHS function interface
     */
    public RhsFunctionManager getRhsFunctions()
    {
        return rhsFunctions;
    }
    
    public MultiAttributes getMultiAttributes()
    {
        return multiAttrs;
    }
 
    public SoarEventManager getEventManager()
    {
        return eventManager;
    }
    
    /**
     * Returns the agent's production manager. Use this interface to access
     * loaded productions, load new productions, or excise existing productions.
     * 
     * @return the agent's production manager
     */
    public ProductionManager getProductions()
    {
        return productions;
    }
    
    /**
     * Returns this agent's input/output interface
     * 
     * @return the agent's input/output interface
     */
    public InputOutput getInputOutput()
    {
        return io;
    }
    
    /**
     * Returns this agent's symbol factory. The symbol factory is the interface
     * to use to create new symbols or find existing ones.
     * 
     * @return the agent's symbol factory
     */
    public SymbolFactory getSymbols()
    {
        return syms;
    }
    
    /**
     *
     * <p>utilities.cpp:132:get_context_var_info
     * 
     * @param variable A variable, e.g. {@code <s>}, {@code <ts>}, etc
     * @return info about that variable
     */
    public ContextVariableInfo getContextVariableInfo(String variable)
    {
        return ContextVariableInfo.get(predefinedSyms, decider.top_goal, decider.bottom_goal, variable);
    }
    
    public Symbol readIdentifierOrContextVariable(String t)
    {
        ContextVariableInfo info = ContextVariableInfo.get(predefinedSyms, decider.top_goal, decider.bottom_goal, t);
        if(info.getValue() != null)
        {
            return info.getValue();
        }
        if(t.length() < 2 || !Character.isLetter(t.charAt(0))) return null;
        
        final char letter = Character.toUpperCase(t.charAt(0));
        int number = 1;
        try
        {
            number = Integer.parseInt(t.substring(1));
        }
        catch(NumberFormatException e)
        {
            return null;
        }
        
        return syms.findIdentifier(letter, number);
    }
    
    /**
     * <p>sml_KernelHelpers.cpp:83:PrintStackTrace
     * 
     * @param states
     * @param operators
     */
    public void printStackTrace(boolean states, boolean operators)
    {
        if(!states && !operators)
        {
            states = operators = true;
        }
        // We don't want to keep printing forever (in case we're in a state no change cascade).
        final int maxStates = 500 ;
        int stateCount = 0 ;

        final Writer writer = printer.getWriter();
        
        for (IdentifierImpl g = decider.top_goal; g != null; g = g.lower_goal) 
        {
            stateCount++ ;

            if (stateCount > maxStates)
                continue ;

            try
            {
                if (states)
                {
                    traceFormats.print_stack_trace (writer,g, g, TraceFormatRestriction.FOR_STATES_TF, false);
                    writer.append('\n');
                }
                if (operators && g.operator_slot.getWmes() != null) 
                {
                    traceFormats.print_stack_trace (writer, g.operator_slot.getWmes().value,
                        g, TraceFormatRestriction.FOR_OPERATORS_TF, false);
                    writer.append('\n');
                }
            }
            catch (IOException e)
            {
            }
        }

        if (stateCount > maxStates)
        {
            printer.print ("...Stack goes on for another %d states\n", stateCount - maxStates);
        }
        printer.flush();
    }
    
    /**
     * @return the agent's random number generator. It is safe to call setSeed()
     *      on the returned generator.
     */
    public Random getRandom()
    {
        return random;
    }
    
    /**
     * @return the totalCpuTimer
     */
    public ExecutionTimer getTotalCpuTimer()
    {
        return totalCpuTimer;
    }

    /**
     * @return the totalKernelTimer
     */
    public ExecutionTimer getTotalKernelTimer()
    {
        return totalKernelTimer;
    }

    public List<ExecutionTimer> getAllTimers()
    {
        return Arrays.asList(totalCpuTimer, totalKernelTimer);
    }
      
    /**
     * Run this agent for the given number of steps with the given step type. 
     * The agent is run in the current thread.
     * 
     * @param n Number of steps. Ignored if runType is {@link RunType#FOREVER}.
     * @param runType The run type
     */
    public void runFor(int n, RunType runType)
    {
        enusreInitialized();
        this.decisionCycle.runFor(n, runType);
        getTrace().flush();
    }

    /**
     * Run this agent forever, i.e. until an interrupt or halt. The agent is
     * run in the current thread.
     */
    public void runForever()
    {
        enusreInitialized();
        this.decisionCycle.runForever();
        getTrace().flush();
    }
    
    /**
     * Checks that the agent has been initialized and throws an exception if
     * not.
     * 
     * @throws IllegalStateException if agent has not been initialized
     */
    private void enusreInitialized()
    {
        if(!initialized)
        {
            throw new IllegalStateException("Agent has not been initialized.");
        }
    }
    
    /**
     * Request that the agent stop, i.e. exit the active call to {@link #runFor(int, RunType)}
     * of {@link #runForever()}. 
     * 
     * <p>This method is not thread safe. It must be called from the same thread that the
     * agent is running in. 
     */
    public void stop()
    {
        this.decisionCycle.stop();
    }
    
    /**
     * @return the reason the agent stopped, or <code>null</code> if the agent
     *  has not stopped.
     */
    public String getReasonForStop()
    {
        return decisionCycle.getReasonForStop();
    }
    
    /**
     * Returns the current phase of the decision cycle
     * 
     * @return the current phase of the decision cycle
     */
    public Phase getCurrentPhase()
    {
        return this.decisionCycle.current_phase;
    }
    
    /**
     * Returns a <b>copy</b> of the set of all WMEs currently in the rete.
     * 
     * @return a <b>copy</b> of the set of all WMEs currently in the rete
     */
    public Set<Wme> getAllWmesInRete()
    {
        return new LinkedHashSet<Wme>(rete.getAllWmes());
    }
    
    /**
     * @return Number of WMEs currently in the rete
     */
    public int getNumWmesInRete()
    {
        return rete.getAllWmes().size();
    }
    
    /**
     * Print the current match set for the agent
     * 
     * <p>rete.cpp:7756:print_match_set
     * 
     * @param printer The printer to print to
     * @param wtt The WME trace level
     * @param mst The match set trace settings
     */
    public void printMatchSet(Printer printer, WmeTraceType wtt, EnumSet<MatchSetTraceType> mst)
    {
        this.soarReteListener.print_match_set(printer, wtt, mst);
    }
    
    public MatchSet getMatchSet()
    {
        return soarReteListener.getMatchSet();
    }
    
    /**
     * <p>init_soar.cpp:1374:init_agent_memory()
     */
    private void init_agent_memory()
    {
        // If there is already a top goal this function should probably not be called
        if (decider.top_goal != null)
        {
            throw new IllegalStateException("There should be no top goal when init_agent_memory is called!");
        }

        decider.create_top_goal();

        if (trace.isEnabled() && trace.isEnabled(Category.CONTEXT_DECISIONS))
        {
            final Writer writer = trace.getPrinter().getWriter();
            try
            {
                writer.write("\n");
                traceFormats.print_lowest_slot_in_context_stack (writer, decider.bottom_goal);
            }
            catch (IOException e)
            {
                logger.error("IOException while printing initial stack trace. Ignoring.", e);
            }
        }
        decisionCycle.current_phase = Phase.INPUT;
        decisionCycle.d_cycle_count.increment();

        io.init_agent_memory();

        // KJC & RPM 10/06
        // A lot of stuff isn't initialized properly until the input and output
        // cycles are run the first time. Because of this, SW had to put in a 
        // hack to work around changes made to the output-link in the first
        // dc not being visible. (see comment near end of update_for_top_state_wme_addition). 
        // This change added an item to the associated_output_links list. But the 
        // ol->ids_in_tc is still not initialized until the first output phases, 
        // so if we exit before that, remove_output_link_tc_info doesn't see it 
        // and doesn't clean up the associated_output_links list.
        // If we do run an output phases, though, the same item is added to the
        // associated_output_links list twice.
        // ol->ids_in_tc gets initialized, so remove_output_link_tc_info -- but
        // it only cleans up the first copy of the item.
        // All of these problems come back to things not being initialized
        // properly, so we run the input and output phases here to force proper 
        // initialization (and have commented out SW's changes to
        // update_for_top_state_wme_addition). This will cause somecallbacks to
        // be triggered, but we don't think this is a problem for two reasons:
        // 1) these events are currently not exposed through SML, so no clients
        // will see them
        // 2) even if these events were exposed, they are being fired during
        // agent creation. Since the agent hasn't been created yet, no client 
        // could have registered for the events anyway.
        // Note that this change replaces the do_buffered_wm_and_ownership_changes 
        // call which attempted to do some initialization (including triggering 
        // SW's changes).
        io.do_input_cycle();
        io.do_output_cycle();

        // executing the IO cycles above, increments the timers, so reset
        // Initializing all the timer structures */
        for (ExecutionTimer timer : getAllTimers())
        {
            timer.reset();
        }
    }
    
    /**
     * agent.cpp:90:init_soar_agent
     */
    private void installDefaultTraceFormats()
    {
        // add default object trace formats
        traceFormats.add_trace_format (false, TraceFormatRestriction.FOR_ANYTHING_TF, null,
                                       "%id %ifdef[(%v[name])]");
        traceFormats.add_trace_format (false, TraceFormatRestriction.FOR_STATES_TF, null,
                                       "%id %ifdef[(%v[attribute] %v[impasse])]");
        traceFormats.add_trace_format (false, TraceFormatRestriction.FOR_OPERATORS_TF, 
                                       syms.createString ("evaluate-object"),
                                       "%id (evaluate-object %o[object])");
        
        // add default stack trace formats
        traceFormats.add_trace_format (true, TraceFormatRestriction.FOR_STATES_TF, null,
                                       "%right[6,%dc]: %rsd[   ]==>S: %cs");
        traceFormats.add_trace_format (true, TraceFormatRestriction.FOR_OPERATORS_TF, null,
                                       "%right[6,%dc]: %rsd[   ]   O: %co");
    }

    /**
     * init_soar.cpp:350:reinitialize_soar
     */
    private void reinitialize_soar()
    {
        getEventManager().fireEvent(new BeforeInitSoarEvent(this));

        // Temporarily disable tracing
        boolean traceState = trace.isEnabled();
        trace.setEnabled(false);

        rl.rl_reset_data();
        decider.clear_goal_stack();
        io.do_input_cycle(); // tell input functions that the top state is gone
        io.do_output_cycle(); // tell output functions that output commands are gone
        rl.rl_reset_stats();

        decider.active_level = 0; // Signal that everything should be retracted
        recMemory.FIRING_TYPE = SavedFiringType.IE_PRODS;
        // allow all i-instantiations to retract
        recMemory.do_preference_phase(decider.top_goal); 

        explain.reset_explain();
        syms.reset();
        workingMemory.reset();
        decisionCycle.reset();
        recMemory.reset();

        reset_statistics();

        
        // Restore trace state
        trace.setEnabled(traceState);
        
        getEventManager().fireEvent(new AfterInitSoarEvent(this));
    }

    /**
     * <p>init_soar.cpp:297:reset_statistics
     */
    private void reset_statistics()
    {
        chunker.chunks_this_d_cycle = 0;

        productions.resetStatistics();

        for (ExecutionTimer timer : getAllTimers())
        {
            timer.reset();
        }
    }

    /* (non-Javadoc)
     * @see org.jsoar.util.adaptables.AbstractAdaptable#getAdapter(java.lang.Class)
     */
    @Override
    public Object getAdapter(Class<?> klass)
    {
        Object o = Adaptables.findAdapter(adaptables, klass);
        if(o != null)
        {
            return o;
        }
        return super.getAdapter(klass);
    }

    /* (non-Javadoc)
     * @see java.lang.Object#toString()
     */
    @Override
    public String toString()
    {
        return name;
    }
}
