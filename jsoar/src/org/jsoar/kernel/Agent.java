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

import org.jsoar.kernel.exploration.Exploration;
import org.jsoar.kernel.io.InputOutput;
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
import org.jsoar.kernel.rete.Rete;
import org.jsoar.kernel.rete.SoarReteListener;
import org.jsoar.kernel.rhs.ActionReorderer;
import org.jsoar.kernel.rhs.ReordererException;
import org.jsoar.kernel.rhs.functions.RhsFunctionManager;
import org.jsoar.kernel.rhs.functions.StandardRhsFunctions;
import org.jsoar.kernel.symbols.SymbolFactory;
import org.jsoar.kernel.tracing.Printer;
import org.jsoar.kernel.tracing.Trace;
import org.jsoar.kernel.tracing.TraceFormatRestriction;
import org.jsoar.kernel.tracing.TraceFormats;
import org.jsoar.kernel.tracing.Trace.Category;

/**
 * @author ray
 */
public class Agent
{
    public int MAX_GOAL_DEPTH = 100;
    
    private Printer printer = new Printer(new OutputStreamWriter(System.out), true);
    public final Trace trace = new Trace(printer);
    public final TraceFormats traceFormats = new TraceFormats(this);
    
    public final PredefinedSymbols predefinedSyms = new PredefinedSymbols();
    public final SymbolFactory syms = predefinedSyms.getSyms();
    public final VariableGenerator variableGenerator = new VariableGenerator(syms);
    public final MultiAttributes multiAttrs = new MultiAttributes();
    public final Rete rete = new Rete(trace, variableGenerator);
    public final WorkingMemory workingMemory = new WorkingMemory(this);
    public final TemporaryMemory tempMemory = new TemporaryMemory();
    public final PreferenceMemory prefMemory = new PreferenceMemory(this);
    public final OSupport osupport = new OSupport(predefinedSyms, printer);
    public final SoarReteListener soarReteListener = new SoarReteListener(this);
    public final RecognitionMemory recMemory = new RecognitionMemory(this);
    
    public final Decider decider = new Decider(this);
    public final Consistency consistency = new Consistency(this);
    
    public final Chunker chunker = new Chunker(this);
    public final Explain explain = new Explain(this);
    public final Backtracer backtrace = new Backtracer(this);
    public final ReinforcementLearning rl = new ReinforcementLearning();
    
    public final DecisionManipulation decisionManip = new DecisionManipulation(decider);
    public final Exploration exploration = new Exploration();
    public final InputOutput io = new InputOutput(this);
    
    private final RhsFunctionManager rhsFunctions = new RhsFunctionManager(syms);
    public final DecisionCycle decisionCycle = new DecisionCycle(this);
    
    
    /**
     * false is Soar 7 mode
     * 
     * agent.h:728
     */
    public boolean operand2_mode = true;
    
    /**
     * agent.h:688:attribute_preferences_mode
     */
    public int attribute_preferences_mode = 0;

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
     */
    public void initialize()
    {
        // TODO reinitialize if called again
        // TODO Call automatically if any function that requires it is called?
        init_agent_memory();
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
 
    public void loadProduction(String productionBody) throws IOException, ReordererException
    {
        StringReader reader = new StringReader(productionBody);
        Lexer lexer = new Lexer(reader);
        Parser parser = new Parser(variableGenerator, lexer);
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
        // Reorder the production
        p.reorder(variableGenerator, 
                  new ConditionReorderer(variableGenerator, trace, multiAttrs, p.name.name), 
                  new ActionReorderer(printer, p.name.name), 
                  reorder_nccs);

        // Tell RL about the new production
        rl.addProduction(p);
        
        // Add it to the rete.
        rete.add_production_to_rete(p);
        // TODO (from parser.cpp)
    //  if (*rete_addition_result==DUPLICATE_PRODUCTION) {
//      excise_production (thisAgent, p, false);
//      p = null;
  //  }
    }
    
    /**
     * init_soar.cpp:1374:init_agent_memory()
     */
    private void init_agent_memory()
    {
        /* The following code was taken from the do_one_top_level_phase function
           near the top of this file */
        // If there is already a top goal this function should probably not be
        // called
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
                writer.append("\n");
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
        /* TODO reset timers
        reset_timer (&thisAgent->start_total_tv);
        reset_timer (&thisAgent->total_cpu_time);
        reset_timer (&thisAgent->start_kernel_tv);
        reset_timer (&thisAgent->start_phase_tv);
        reset_timer (&thisAgent->total_kernel_time);

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
                                       syms.make_sym_constant ("evaluate-object"),
                                       "%id (evaluate-object %o[object])");
        
        // add default stack trace formats
        traceFormats.add_trace_format (true, TraceFormatRestriction.FOR_STATES_TF, null,
                                       "%right[6,%dc]: %rsd[   ]==>S: %cs");
        traceFormats.add_trace_format (true, TraceFormatRestriction.FOR_OPERATORS_TF, null,
                                       "%right[6,%dc]: %rsd[   ]   O: %co");
    }

}
