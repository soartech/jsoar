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
import java.util.List;
import java.util.Random;

import org.jsoar.kernel.events.AfterInitSoarEvent;
import org.jsoar.kernel.events.BeforeInitSoarEvent;
import org.jsoar.kernel.exploration.Exploration;
import org.jsoar.kernel.io.InputOutput;
import org.jsoar.kernel.io.InputOutputImpl;
import org.jsoar.kernel.learning.Backtracer;
import org.jsoar.kernel.learning.Chunker;
import org.jsoar.kernel.learning.Explain;
import org.jsoar.kernel.learning.rl.ReinforcementLearning;
import org.jsoar.kernel.lhs.MultiAttributes;
import org.jsoar.kernel.memory.ContextVariableInfo;
import org.jsoar.kernel.memory.OSupport;
import org.jsoar.kernel.memory.RecognitionMemory;
import org.jsoar.kernel.memory.TemporaryMemory;
import org.jsoar.kernel.memory.WorkingMemory;
import org.jsoar.kernel.rete.Rete;
import org.jsoar.kernel.rete.SoarReteListener;
import org.jsoar.kernel.rhs.functions.RhsFunctionManager;
import org.jsoar.kernel.rhs.functions.StandardFunctions;
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
import org.jsoar.util.events.SoarEventManager;
import org.jsoar.util.timing.DefaultExecutionTimer;
import org.jsoar.util.timing.ExecutionTimer;

/**
 * @author ray
 */
public class Agent
{
    private Printer printer = new Printer(new OutputStreamWriter(System.out), true);
    
    /**
     * The random number generator used throughout the agent
     */
    private final Random random = new Random();
    
    public final Trace trace = new Trace(printer);
    public final TraceFormats traceFormats = new TraceFormats(this);
    
    public final PredefinedSymbols predefinedSyms = new PredefinedSymbols();
    public final SymbolFactoryImpl syms = predefinedSyms.getSyms();
    public final VariableGenerator variableGenerator = new VariableGenerator(syms);
    public final MultiAttributes multiAttrs = new MultiAttributes();
    public final Rete rete = new Rete(trace, variableGenerator);
    public final WorkingMemory workingMemory = new WorkingMemory(this);
    public final TemporaryMemory tempMemory = new TemporaryMemory();
    public final OSupport osupport = new OSupport(predefinedSyms, printer);
    public final SoarReteListener soarReteListener = new SoarReteListener(this);
    public final RecognitionMemory recMemory = new RecognitionMemory(this);
    
    public final Exploration exploration = new Exploration(this);
    public final Decider decider = new Decider(this, exploration);
    public final Consistency consistency = new Consistency(this);
    
    public final Chunker chunker = new Chunker(this);
    public final Explain explain = new Explain(this);
    public final Backtracer backtrace = new Backtracer(this);
    public final ReinforcementLearning rl = new ReinforcementLearning(this);
    
    public final DecisionManipulation decisionManip = new DecisionManipulation(decider, random);
    public final InputOutputImpl io = new InputOutputImpl(this);
    
    private final RhsFunctionManager rhsFunctions = new RhsFunctionManager(recMemory.getRhsFunctionContext());
    public final DecisionCycle decisionCycle = new DecisionCycle(this);
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
     * false is Soar 7 mode
     * 
     * <p>agent.h:728
     */
    public boolean operand2_mode = true;
    
    /**
     * <p>agent.h:688:attribute_preferences_mode
     * <p>Defaults to 0 in agent.cpp
     */
    public int attribute_preferences_mode = 0;
    
    private boolean initialized = false;
    
    public Agent()
    {
        // Set up standard RHS functions
        new StandardFunctions(this);
        installDefaultTraceFormats();
        
        rete.setReteListener(soarReteListener);
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
    
    public void setEventManager(SoarEventManager eventManager)
    {
        this.eventManager = eventManager;
    }
    
    public ProductionManager getProductions()
    {
        return productions;
    }
    
    public InputOutput getInputOutput()
    {
        return io;
    }
    
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
        this.decisionCycle.runFor(n, runType);
        getTrace().flush();
    }
    
    /**
     * Run this agent forever, i.e. until an interrupt or halt. The agent is
     * run in the current thread.
     */
    public void runForever()
    {
        this.decisionCycle.runForever();
        getTrace().flush();
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
                decider.print_lowest_slot_in_context_stack (writer);
            }
            catch (IOException e)
            {
                // TODO Auto-generated catch block
                e.printStackTrace();
            }
        }
        decisionCycle.current_phase = Phase.INPUT;
        if (operand2_mode)
            decisionCycle.d_cycle_count++;

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

        if (operand2_mode)
        {
            decider.active_level = 0; // Signal that everything should be
                                        // retracted
            recMemory.FIRING_TYPE = SavedFiringType.IE_PRODS;
            // allow all i-instantiations to retract
            recMemory.do_preference_phase(decider.top_goal, osupport.o_support_calculation_type); 
        }
        else
        {
            // allow all instantiations to retract
            recMemory.do_preference_phase(decider.top_goal, osupport.o_support_calculation_type);
        }

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

}
