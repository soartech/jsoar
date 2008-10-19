/*
 * (c) 2008  Dave Ray
 *
 * Created on Aug 15, 2008
 */
package org.jsoar.kernel;

import java.io.IOException;
import java.io.OutputStreamWriter;
import java.io.StringReader;
import java.io.Writer;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.EnumMap;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.Set;

import org.jsoar.kernel.DecisionCycle.GoType;
import org.jsoar.kernel.events.AfterInitSoarEvent;
import org.jsoar.kernel.events.BeforeInitSoarEvent;
import org.jsoar.kernel.events.SoarEventManager;
import org.jsoar.kernel.exploration.Exploration;
import org.jsoar.kernel.io.InputOutput;
import org.jsoar.kernel.io.InputOutputImpl;
import org.jsoar.kernel.learning.Backtracer;
import org.jsoar.kernel.learning.Chunker;
import org.jsoar.kernel.learning.Explain;
import org.jsoar.kernel.learning.ReinforcementLearning;
import org.jsoar.kernel.lhs.ConditionReorderer;
import org.jsoar.kernel.lhs.MultiAttributes;
import org.jsoar.kernel.memory.OSupport;
import org.jsoar.kernel.memory.PreferenceMemory;
import org.jsoar.kernel.memory.RecognitionMemory;
import org.jsoar.kernel.memory.TemporaryMemory;
import org.jsoar.kernel.memory.WorkingMemory;
import org.jsoar.kernel.parser.Lexer;
import org.jsoar.kernel.parser.Parser;
import org.jsoar.kernel.parser.ParserException;
import org.jsoar.kernel.rete.ProductionAddResult;
import org.jsoar.kernel.rete.Rete;
import org.jsoar.kernel.rete.SoarReteListener;
import org.jsoar.kernel.rhs.ActionReorderer;
import org.jsoar.kernel.rhs.ReordererException;
import org.jsoar.kernel.rhs.functions.RhsFunctionManager;
import org.jsoar.kernel.rhs.functions.StandardRhsFunctions;
import org.jsoar.kernel.symbols.StringSymbolImpl;
import org.jsoar.kernel.symbols.SymbolFactory;
import org.jsoar.kernel.symbols.SymbolFactoryImpl;
import org.jsoar.kernel.tracing.Printer;
import org.jsoar.kernel.tracing.Trace;
import org.jsoar.kernel.tracing.TraceFormatRestriction;
import org.jsoar.kernel.tracing.TraceFormats;
import org.jsoar.kernel.tracing.Trace.Category;
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
    public final PreferenceMemory prefMemory = new PreferenceMemory(this);
    public final OSupport osupport = new OSupport(predefinedSyms, printer);
    public final SoarReteListener soarReteListener = new SoarReteListener(this);
    public final RecognitionMemory recMemory = new RecognitionMemory(this);
    
    public final Exploration exploration = new Exploration(this);
    public final Decider decider = new Decider(this, exploration);
    public final Consistency consistency = new Consistency(this);
    
    public final Chunker chunker = new Chunker(this);
    public final Explain explain = new Explain(this);
    public final Backtracer backtrace = new Backtracer(this);
    public final ReinforcementLearning rl = new ReinforcementLearning();
    
    public final DecisionManipulation decisionManip = new DecisionManipulation(decider, random);
    public final InputOutputImpl io = new InputOutputImpl(this);
    
    private final RhsFunctionManager rhsFunctions = new RhsFunctionManager(syms);
    public final DecisionCycle decisionCycle = new DecisionCycle(this);
    private final SoarEventManager eventManager = new SoarEventManager();
    
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
     * agent.h:728
     */
    public boolean operand2_mode = true;
    
    /**
     * <p>agent.h:688:attribute_preferences_mode
     * <p>Defaults to 0 in agent.cpp
     */
    public int attribute_preferences_mode = 0;
    
    private boolean initialized = false;
    private int totalProductions = 0;
    private EnumMap<ProductionType, Set<Production>> productionsByType = new EnumMap<ProductionType, Set<Production>>(ProductionType.class);
    {
        for(ProductionType type : ProductionType.values())
        {
            productionsByType.put(type, new LinkedHashSet<Production>());
        }
    }
    private Map<StringSymbolImpl, Production> productionsByName = new HashMap<StringSymbolImpl, Production>();
    
    public Agent()
    {
        // Set up standard RHS functions
        new StandardRhsFunctions(this);
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
     * @return the printer
     */
    public Printer getPrinter()
    {
        return printer;
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
    
    public InputOutput getInputOutput()
    {
        return io;
    }
    
    public SymbolFactory getSymbols()
    {
        return syms;
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
    
    public void loadProduction(String productionBody) throws IOException, ReordererException, ParserException
    {
        StringReader reader = new StringReader(productionBody);
        Lexer lexer = new Lexer(printer, reader);
        Parser parser = new Parser(variableGenerator, lexer, operand2_mode);
        lexer.getNextLexeme();
        addProduction(parser.parse_production(), true);
    }
    
    /**
     * Add the given production to the agent.
     * 
     * <p>This is part of a refactoring of make_production().
     * 
     * @param p
     * @param reorder_nccs
     * @throws ReordererException 
     */
    private void addProduction(Production p, boolean reorder_nccs) throws ReordererException
    {
        // if there's already a prod with this name, excise it
        // Note, in csoar, this test was done in parse_production as soon as the name
        // of the production was known. We do this here so we can eliminate the
        // production field of StringSymbolImpl.
        Production existing = getProduction(p.name.getValue());
        if (existing != null) 
        {
            exciseProduction(existing, trace.isEnabled(Category.TRACE_LOADING_SYSPARAM));
        }

        // Reorder the production
        p.reorder(variableGenerator, 
                  new ConditionReorderer(variableGenerator, trace, multiAttrs, p.name.getValue()), 
                  new ActionReorderer(printer, p.name.getValue()), 
                  reorder_nccs);

        // Tell RL about the new production
        rl.addProduction(p);
        
        // Add it to the rete.
        ProductionAddResult result = rete.add_production_to_rete(p);
        // from parser.cpp
        if (result==ProductionAddResult.DUPLICATE_PRODUCTION) 
        {
            exciseProduction (p, false);
            return;
        }
        
        totalProductions++;
        productionsByType.get(p.type).add(p);
        productionsByName.put(p.name, p);
    }
    
    /**
     * Add the given production to the agent.
     * 
     * <p>This is part of a refactoring of make_production().
     * 
     * @param p
     * @param reorder_nccs
     * @throws ReordererException 
     */
    public void addChunk(Production p) throws ReordererException
    {
        // Reorder the production
        p.reorder(variableGenerator, 
                  new ConditionReorderer(variableGenerator, trace, multiAttrs, p.name.getValue()), 
                  new ActionReorderer(printer, p.name.getValue()), 
                  false);

        // Tell RL about the new production
        rl.addProduction(p);
        
        // Production is added to the rete by the chunker

        totalProductions++;
        productionsByType.get(p.type).add(p);
        productionsByName.put(p.name, p);
    }
    
    public Production getProduction(String name)
    {
        StringSymbolImpl sc = syms.findString(name);
        return productionsByName.get(sc);
    }
    
    /**
     * Returns a list of productions of a particular type, or all productions
     * if type is <code>null</code>
     * 
     * @param type Type of production, or <code>null</code> for all productions.
     * @return List of productions, ordered by type and then by order of addition
     */
    public List<Production> getProductions(ProductionType type)
    {
        List<Production> result;
        if(type != null)
        {
            Set<Production> ofType = productionsByType.get(type);
            result = new ArrayList<Production>(ofType);
        }
        else
        {
            result = new ArrayList<Production>(totalProductions);
            for(Set<Production> ofType : productionsByType.values())
            {
                result.addAll(ofType);
            }
        }
        return result;
    }
    
    /**
     * 
     * <p>production.cpp:1595:excise_production
     * 
     * @param prod
     * @param print_sharp_sign
     */
    public void exciseProduction(Production prod, boolean print_sharp_sign)
    {
        // TODO if (prod->trace_firings) remove_pwatch (thisAgent, prod);
        
        totalProductions--;
        productionsByType.get(prod.type).remove(prod);
        productionsByName.remove(prod.name);

        rl.exciseProduction(prod);

        if (print_sharp_sign)
        {
            getPrinter().print("#");
        }
        if (prod.p_node != null)
        {
            rete.excise_production_from_rete(prod);
        }
        prod.production_remove_ref();
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

        if (trace.isEnabled() && trace.isEnabled(Category.TRACE_CONTEXT_DECISIONS_SYSPARAM))
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
        decisionCycle.current_phase = Phase.INPUT_PHASE;
        if (operand2_mode)
            decisionCycle.d_cycle_count++;

        io.init_agent_memory();

        // KJC & RPM 10/06
        // A lot of stuff isn't initialized properly until the input and output
        // cycles are run the first time.
        // Because of this, SW had to put in a hack to work around changes made
        // to the output-link in the first
        // dc not being visible. (see comment near end of
        // update_for_top_state_wme_addition). This change added
        // an item to the associated_output_links list.
        // But the ol->ids_in_tc is still not initialized until the first output
        // phases, so if we exit before that,
        // remove_output_link_tc_info doesn't see it and doesn't clean up the
        // associated_output_links list.
        // If we do run an output phases, though, the same item is added to the
        // associated_output_links list twice.
        // ol->ids_in_tc gets initialized, so remove_output_link_tc_info -- but
        // it only cleans up the first copy
        // of the item.
        // All of these problems come back to things not being initialized
        // properly, so we run the input and output
        // phases here to force proper initialization (and have commented out
        // SW's changes to
        // update_for_top_state_wme_addition). This will cause somecallbacks to
        // be triggered, but we don't think
        // this is a problem for two reasons:
        // 1) these events are currently not exposed through SML, so no clients
        // will see them
        // 2) even if these events were exposed, they are being fired during
        // agent creation. Since the agent
        // hasn't been created yet, no client could have registered for the
        // events anyway.
        // Note that this change replaces the
        // do_buffered_wm_and_ownership_changes call which attempted to do some
        // initialization (including triggering SW's changes).
        io.do_input_cycle();
        io.do_output_cycle();
        // do_buffered_wm_and_ownership_changes(thisAgent);

        /* executing the IO cycles above, increments the timers, so reset */
        /* Initializing all the timer structures */
        totalCpuTimer.reset();
        totalKernelTimer.reset();
        /* TODO reset timers
        reset_timer (&thisAgent->start_phase_tv);

        reset_timer (&thisAgent->input_function_cpu_time);
        reset_timer (&thisAgent->output_function_cpu_time);

        reset_timer (&thisAgent->start_gds_tv);
        reset_timer (&thisAgent->total_gds_time);

        for (int ii=0;ii < NUM_PHASE_TYPES; ii++) {
           reset_timer (&thisAgent->decision_cycle_phase_timers[ii]);
           reset_timer (&thisAgent->monitors_cpu_time[ii]);
           reset_timer (&thisAgent->ownership_cpu_time[ii]);
           reset_timer (&thisAgent->chunking_cpu_time[ii]);
           reset_timer (&thisAgent->match_cpu_time[ii]);
           reset_timer (&thisAgent->gds_cpu_time[ii]);
        }
        */

        // This is an important part of the state of the agent for io purposes
        // (see io.cpp for details)
        decider.prev_top_state = decider.top_state;

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

    private void reinitialize_soar()
    {
        getEventManager().fireEvent(new BeforeInitSoarEvent(this));

        // Temporarily disable tracing
        boolean traceState = trace.isEnabled();
        trace.setEnabled(false);

        // TODO rl_reset_data( thisAgent );
        decider.clear_goal_stack();
        io.do_input_cycle(); // tell input functions that the top state is gone
        io.do_output_cycle(); // tell output functions that output commands are gone
        // TODO rl_reset_stats( thisAgent );

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
        syms.reset_id_counters();
        workingMemory.reset_wme_timetags();

        reset_statistics();

        // Reinitialize the various halt and stop flags
        decisionCycle.system_halted = false;
        decisionCycle.stop_soar = false;
        decisionCycle.reason_for_stopping = null;
        decisionCycle.go_type = GoType.GO_DECISION;

        // Restore trace state
        trace.setEnabled(traceState);

        getEventManager().fireEvent(new AfterInitSoarEvent(this));

        decisionCycle.input_cycle_flag = true; /* reinitialize flag  AGR REW1 */
        decisionCycle.current_phase = Phase.INPUT_PHASE; /* moved here June 05 from loop below.  KJC */

        if (operand2_mode)
        {
            recMemory.FIRING_TYPE = SavedFiringType.IE_PRODS; /* KJC 10.05.98 was PE */
        }
    }

    /**
     * <p>init_soar.cpp:297:reset_statistics
     */
    private void reset_statistics()
    {

        decisionCycle.reset_statistics();
        chunker.chunks_this_d_cycle = 0;
        recMemory.reset_statistics();
        workingMemory.reset_statistics();

        // reset_production_firing_counts(thisAgent);
        for (Production p : this.productionsByName.values())
        {
            p.firing_count = 0;
        }

        for (ExecutionTimer timer : getAllTimers())
        {
            timer.reset();
        }
    }
}
