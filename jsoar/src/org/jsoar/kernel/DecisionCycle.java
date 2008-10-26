/*
 * Copyright (c) 2008  Dave Ray <daveray@gmail.com>
 *
 * Created on Sep 14, 2008
 */
package org.jsoar.kernel;

import java.io.IOException;
import java.io.Writer;
import java.util.List;
import java.util.Map;

import org.jsoar.kernel.events.AbstractPhaseEvent;
import org.jsoar.kernel.events.AfterDecisionCycleEvent;
import org.jsoar.kernel.events.AfterElaborationEvent;
import org.jsoar.kernel.events.AfterHaltEvent;
import org.jsoar.kernel.events.BeforeDecisionCycleEvent;
import org.jsoar.kernel.events.BeforeElaborationEvent;
import org.jsoar.kernel.events.PhaseEvents;
import org.jsoar.kernel.events.RunLoopEvent;
import org.jsoar.kernel.rhs.functions.AbstractRhsFunctionHandler;
import org.jsoar.kernel.rhs.functions.RhsFunctionException;
import org.jsoar.kernel.rhs.functions.RhsFunctionHandler;
import org.jsoar.kernel.symbols.IdentifierImpl;
import org.jsoar.kernel.symbols.Symbol;
import org.jsoar.kernel.symbols.SymbolFactory;
import org.jsoar.kernel.symbols.SymbolImpl;
import org.jsoar.kernel.tracing.Printer;
import org.jsoar.kernel.tracing.Trace.Category;
import org.jsoar.util.Arguments;
import org.jsoar.util.timing.ExecutionTimers;

/**
 * An attempt to encapsulate the Soar decision cycle
 * 
 * @author ray
 */
public class DecisionCycle
{
    private final Agent context;
    
    private static enum GoType
    {
        GO_PHASE, GO_ELABORATION, GO_DECISION,
        // unused: GO_STATE, GO_OPERATOR, GO_SLOT, GO_OUTPUT
    }
    
    private boolean system_halted = false;
    private boolean stop_soar = false;
    private String reason_for_stopping = null;
    
    /**
     * agent.h:324:current_phase
     * agent.cpp:153 (init)
     */
    public Phase current_phase = Phase.INPUT_PHASE;
    /**
     * agent.h:349:go_type
     * agent.cpp:146 (init)
     */
    private GoType go_type = GoType.GO_DECISION;
    
    int e_cycles_this_d_cycle;
    boolean input_cycle_flag;
    private int run_phase_count;
    private int run_elaboration_count;
    private int input_period;
    public int e_cycle_count;
    public int pe_cycle_count;
    private int pe_cycles_this_d_cycle;
    private int run_last_output_count;
    private int run_generated_output_count;
    public int d_cycle_count;
    public int decision_phases_count;
    
    /**
     * gsysparams.h::MAX_NIL_OUTPUT_CYCLES_SYSPARAM
     */
    private int maxNilOutputCycles = 15;
    
    /**
     * rhsfun.cpp:199:halt_rhs_function_code
     */
    private final RhsFunctionHandler haltHandler = new AbstractRhsFunctionHandler("halt") {

        @Override
        public SymbolImpl execute(SymbolFactory syms, List<Symbol> arguments) throws RhsFunctionException
        {
            system_halted = true;
            
            // callback AFTER_HALT_SOAR_CALLBACK is fired from decision cycle
            
            return null;
        }};
    
    private final AfterHaltEvent afterHaltEvent;
    private final BeforeElaborationEvent beforeElaborationEvent;
    private final AfterElaborationEvent afterElaborationEvent;
    private final BeforeDecisionCycleEvent beforeDecisionCycleEvent;
    private final RunLoopEvent pollEvent;
    private final Map<Phase, AbstractPhaseEvent> beforePhaseEvents;
    private final Map<Phase, AbstractPhaseEvent> afterPhaseEvents;
    
    public DecisionCycle(Agent context)
    {
        this.context = context;

        this.afterHaltEvent = new AfterHaltEvent(context);
        this.beforeElaborationEvent = new BeforeElaborationEvent(context);
        this.afterElaborationEvent = new AfterElaborationEvent(context);
        this.beforeDecisionCycleEvent = new BeforeDecisionCycleEvent(context, Phase.INPUT_PHASE);
        this.pollEvent = new RunLoopEvent(context);
        this.beforePhaseEvents = PhaseEvents.createBeforeEvents(context);
        this.afterPhaseEvents = PhaseEvents.createAfterEvents(context);
        
        context.getRhsFunctions().registerHandler(haltHandler);
    }
    
    /**
     * Extracted from reinitialize_soar()
     *  
     * init_soar.cpp:410:reinitialize_soar
     */
    public void reset()
    {
        // Reinitialize the various halt and stop flags
        system_halted = false;
        stop_soar = false;
        reason_for_stopping = null;
        go_type = GoType.GO_DECISION;
        input_cycle_flag = true;
        current_phase = Phase.INPUT_PHASE;
        
        resetStatistics();
    }

    /**
     * init_soar.cpp:297:reset_statistics
     */
    private void resetStatistics()
    {
        d_cycle_count = 0;
        decision_phases_count = 0;
        e_cycle_count = 0;
        e_cycles_this_d_cycle = 0;
        pe_cycle_count = 0;
        pe_cycles_this_d_cycle = 0;
        run_phase_count = 0 ;
        run_elaboration_count = 0 ;
        run_last_output_count = 0 ;
        run_generated_output_count = 0 ;
    }
    
    /**
     * runs Soar one top-level phases. Note that this does not start/stop the 
     * total_cpu_time timer--the caller must do this. 
     * 
     * <p>init_soar.cpp:474:do_one_top_level_phase
     */
    private void do_one_top_level_phase()
    {
        assert !system_halted;
        assert !stop_soar;
        
        ExecutionTimers.pause(context.getTotalKernelTimer());
        context.getEventManager().fireEvent(pollEvent);
        ExecutionTimers.start(context.getTotalKernelTimer());
        
        switch (current_phase)
        {
        case INPUT_PHASE:       doInputPhase();         break;
        case PROPOSE_PHASE:     doProposePhase();       break;
        case PREFERENCE_PHASE:  doPreferencePhase();    break;
        case WM_PHASE:          doWorkingMemoryPhase(); break;
        case APPLY_PHASE:       doApplyPhase();         break; 
        case OUTPUT_PHASE:      doOutputPhase();        break;
        case DECISION_PHASE:    doDecisionPhase();      break;
        default: throw new IllegalStateException("Invalid phase enumeration value " + current_phase);
        }

        // update WM size statistics
        context.workingMemory.updateStats(context.rete.num_wmes_in_rete);

        checkForSystemHalt();

        if (stop_soar)
        {
            if (reason_for_stopping != null && reason_for_stopping.length() > 0)
            {
                context.getPrinter().print("\n%s", reason_for_stopping);
            }
        }
    }
    
    private void beforePhase(Phase phase)
    {
        context.getEventManager().fireEvent(beforePhaseEvents.get(phase));    
    }
    
    private void afterPhase(Phase phase)
    {
        this.run_phase_count++;
        
        context.getEventManager().fireEvent(afterPhaseEvents.get(phase));
    }
    
    private void beforeElaboration()
    {
        // FIXME return the correct enum top_level_phase constant in soar_call_data?
        context.getEventManager().fireEvent(beforeElaborationEvent);
    }
    
    private void afterElaboration()
    {
        // FIXME return the correct enum top_level_phase constant in soar_call_data?
        context.getEventManager().fireEvent(afterElaborationEvent);
    }
    
    /**
     * 
     */
    private void pauseTopLevelTimers()
    {
        ExecutionTimers.pause(context.getTotalKernelTimer());
        ExecutionTimers.pause(context.getTotalCpuTimer());
    }

    /**
     * 
     */
    private void startTopLevelTimers()
    {
        ExecutionTimers.start(context.getTotalCpuTimer());
        ExecutionTimers.start(context.getTotalKernelTimer());
    }
    
    /**
     * extracted from run_one_top_level_phase(), switch case DECISION_PHASE
     */
    private void doDecisionPhase()
    {
        assert current_phase == Phase.DECISION_PHASE;
        
        /* not yet cleaned up for 8.6.0 release */

        Phase.DECISION_PHASE.trace(context.trace, true);

        // #ifndef NO_TIMING_STUFF
        // start_timer (thisAgent, &thisAgent->start_phase_tv);
        // #endif
        
        /* d_cycle_count moved to input phases for Soar 8 new decision cycle */
        if (context.operand2_mode == false)
            this.d_cycle_count++;
        this.decision_phases_count++; // counts decisions, not cycles, for more accurate stats

        if (input_period == 0)
            this.input_cycle_flag = true;
        else if ((this.d_cycle_count % this.input_period) == 0)
            this.input_cycle_flag = true;

        beforePhase(Phase.DECISION_PHASE);
        
        context.decider.do_decision_phase(false);

        this.run_elaboration_count++; // All phases count as a run elaboration

        afterPhase(Phase.DECISION_PHASE);

        if (context.trace.isEnabled() && context.trace.isEnabled(Category.TRACE_CONTEXT_DECISIONS_SYSPARAM)) {
            final Writer writer = context.trace.getPrinter().getWriter();
            try
            {
                //writer.append("\n");
                context.decider.print_lowest_slot_in_context_stack (writer);
                writer.append("\n");
                writer.flush();
            }
            catch (IOException e)
            {
                // TODO Auto-generated catch block
                e.printStackTrace();
            }
         }

        if (context.operand2_mode == false)
        {
            // TODO xml
            // JRV: Get rid of the cached XML after every decision but before the after-decision-phases callback
            // xml_invoke_callback( thisAgent ); // invokes XML_GENERATION_CALLBACK, clears XML state

            context.getEventManager().fireEvent(new AfterDecisionCycleEvent(context, Phase.DECISION_PHASE));

            context.chunker.chunks_this_d_cycle = 0;

            Phase.DECISION_PHASE.trace(context.trace, false);

            current_phase = Phase.INPUT_PHASE;
        }
        
        // reset elaboration counter
        this.e_cycles_this_d_cycle = 0;
        this.pe_cycles_this_d_cycle = 0;

        if (context.operand2_mode == true)
        {
            // Note: AGGRESSIVE_ONC used to be here. Dropped from jsoar because 
            // it didn't look like it had been used in years.
            Phase.DECISION_PHASE.trace(context.trace, false);

            context.recMemory.FIRING_TYPE = SavedFiringType.PE_PRODS;
            current_phase = Phase.APPLY_PHASE;
        }

        //      #ifndef NO_TIMING_STUFF
        //      stop_timer (thisAgent, &thisAgent->start_phase_tv, 
        //          &thisAgent->decision_cycle_phase_timers[DECISION_PHASE]);
        //      #endif
    }

    /**
     * extracted from run_one_top_level_phase(), switch case OUTPUT_PHASE
     */
    private void doOutputPhase()
    {
        assert current_phase == Phase.OUTPUT_PHASE;
        
        Phase.OUTPUT_PHASE.trace(context.trace, true);

        // #ifndef NO_TIMING_STUFF
        // start_timer (thisAgent, &thisAgent->start_phase_tv);
        // #endif

        beforePhase(Phase.OUTPUT_PHASE);
        context.io.do_output_cycle();

        // Count the outputs the agent generates (or times reaching max-nil-outputs without sending output)
        if (context.io.isOutputLinkChanged() || ((++(run_last_output_count)) >= maxNilOutputCycles))
        {
            this.run_last_output_count = 0;
            this.run_generated_output_count++;
        }

        this.run_elaboration_count++; // All phases count as a run elaboration
        
        afterPhase(Phase.OUTPUT_PHASE);

        if (context.operand2_mode == true)
        {
            context.getEventManager().fireEvent(new AfterDecisionCycleEvent(context, Phase.OUTPUT_PHASE));

            // #ifndef NO_TIMING_STUFF
            // stop_timer (thisAgent, &thisAgent->start_phase_tv,
            // &thisAgent->decision_cycle_phase_timers[OUTPUT_PHASE]);
            // #endif
            
            Phase.OUTPUT_PHASE.trace(context.trace, false);
            current_phase = Phase.INPUT_PHASE;
            this.d_cycle_count++;
            return;
        }

        /* ******************* otherwise we're in Soar7 mode ...  */

        this.e_cycle_count++;
        this.e_cycles_this_d_cycle++;
        this.run_elaboration_count++; // All phases count as a run elaboration

        Phase.OUTPUT_PHASE.trace(context.trace, false);

        if (e_cycles_this_d_cycle >= context.consistency.getMaxElaborations())
        {
            context.getPrinter().warn("Warning: reached max-elaborations; proceeding to decision phases.");
            // xml_generate_warning(thisAgent, "Warning: reached max-elaborations; proceeding to decision phases.");
            current_phase = Phase.DECISION_PHASE;
        }
        else
        {
            current_phase = Phase.INPUT_PHASE;
        }

        // #ifndef NO_TIMING_STUFF
        // stop_timer (thisAgent, &thisAgent->start_phase_tv, &thisAgent->decision_cycle_phase_timers[OUTPUT_PHASE]);
        // #endif
    }

    /**
     * extracted from run_one_top_level_phase(), switch case APPLY_PHASE
     */
    private void doApplyPhase()
    {
        assert current_phase == Phase.APPLY_PHASE;
        
        // added in 8.6 to clarify Soar8 decision cycle

        // #ifndef NO_TIMING_STUFF
        // start_timer (thisAgent, &thisAgent->start_phase_tv);
        // #endif

        /* e_cycle_count will always be zero UNLESS we are running by ELABORATIONS.
         * We only want to do the following if we've just finished DECISION and are
         * starting APPLY.  If this is the second elaboration for APPLY, then 
         * just do the while loop below.   KJC  June 05
         */
        if (this.e_cycles_this_d_cycle < 1)
        {
            Phase.APPLY_PHASE.trace(context.trace, true);

            beforePhase(Phase.APPLY_PHASE);

            // We need to generate this event here in case no elaborations fire...
            beforeElaboration();

            // 'prime' the cycle for a new round of production firings in the APPLY (pref/wm) phases
            context.consistency.initialize_consistency_calculations_for_new_decision();

            context.recMemory.FIRING_TYPE = SavedFiringType.PE_PRODS; /* might get reset in det_high_active_prod_level... */
            context.consistency.determine_highest_active_production_level_in_stack_apply();
            
            if (current_phase == Phase.OUTPUT_PHASE)
            { 
                // no elaborations will fire this phase
                this.run_elaboration_count++; // All phases count as a run elaboration

                afterElaboration();
            }
        }
        
        // max-elaborations are checked in determine_highest_active... and if they
        // are reached, the current phases is set to OUTPUT.  phases is also set
        // to OUTPUT when APPLY is done.
        while (current_phase != Phase.OUTPUT_PHASE)
        {
            if (this.e_cycles_this_d_cycle != 0)
            {
                // only for 2nd cycle or higher. 1st cycle fired above
                beforeElaboration();
            }
            context.recMemory.do_preference_phase(context.decider.top_goal, context.osupport.o_support_calculation_type);
            context.decider.do_working_memory_phase();

            // Update accounting
            this.e_cycle_count++;
            this.e_cycles_this_d_cycle++;
            this.run_elaboration_count++;

            if (context.recMemory.FIRING_TYPE == SavedFiringType.PE_PRODS)
            {
                this.pe_cycle_count++;
                this.pe_cycles_this_d_cycle++;
            }
            context.consistency.determine_highest_active_production_level_in_stack_apply();

            afterElaboration();

            if (this.go_type == GoType.GO_ELABORATION)
                break;
        }

        //  If we've finished APPLY, then current_phase will be equal to OUTPUT
        //  otherwise, we're only stopping because we're running by ELABORATIONS, so
        //  don't do the end-of-phases updating in that case.
        if (current_phase == Phase.OUTPUT_PHASE)
        {
            /* This is a HACK for Soar 8.6.0 beta release... KCoulter April 05
             * We got here, because we should move to OUTPUT, so APPLY is done
             * Set phases back to APPLY, do print_phase, callbacks and reset phases to OUTPUT
             */
            current_phase = Phase.APPLY_PHASE;
            Phase.APPLY_PHASE.trace(context.trace, false);
            
            afterPhase(Phase.APPLY_PHASE);

            current_phase = Phase.OUTPUT_PHASE;
        }

        // #ifndef NO_TIMING_STUFF
        // stop_timer (thisAgent, &thisAgent->start_phase_tv, &thisAgent->decision_cycle_phase_timers[APPLY_PHASE]);
        // #endif
        
        // END of Soar8 APPLY PHASE
    }

    /**
     * extracted from do_one_top_level_phase(), switch case WM_PHASE
     */
    private void doWorkingMemoryPhase()
    {
        assert current_phase == Phase.WM_PHASE;
        
        // starting with 8.6.0, WM_PHASE is only Soar 7 mode; see PROPOSE and APPLY
        // needs to be updated for gSKI interface, and gSKI needs to accommodate Soar 7

        /*  we need to tell gSKI WM Phase beginning... */

        // #ifndef NO_TIMING_STUFF
        // start_timer (thisAgent, &thisAgent->start_phase_tv);
        // #endif
        beforePhase(Phase.WM_PHASE);
        context.decider.do_working_memory_phase();

        this.run_elaboration_count++; // All phases count as a run elaboration
        
        afterPhase(Phase.WM_PHASE);

        current_phase = Phase.OUTPUT_PHASE;

        // #ifndef NO_TIMING_STUFF
        // stop_timer (thisAgent, &thisAgent->start_phase_tv,
        // &thisAgent->decision_cycle_phase_timers[WM_PHASE]);
        // #endif

        afterElaboration();
        
        // END of Soar7 WM PHASE    
    }

    /**
     * extracted from do_one_top_level_phase(), switch case PROPOSE_PHASE
     */
    private void doProposePhase()
    {
        assert current_phase == Phase.PROPOSE_PHASE;
        
        /* added in 8.6 to clarify Soar8 decision cycle */

        // #ifndef NO_TIMING_STUFF
        // start_timer (thisAgent, &thisAgent->start_phase_tv);
        // #endif

        /* e_cycles_this_d_cycle will always be zero UNLESS we are running by ELABORATIONS.
         * We only want to do the following if we've just finished INPUT and are
         * starting PROPOSE.  If this is the second elaboration for PROPOSE, then 
         * just do the while loop below.   KJC  June 05
         */
        if (this.e_cycles_this_d_cycle < 1)
        {
            Phase.PROPOSE_PHASE.trace(context.trace, true);

            beforePhase(Phase.PROPOSE_PHASE);

            // We need to generate this event here in case no elaborations fire...
            beforeElaboration();

            // 'Prime the decision for a new round of production firings at the end of
            context.consistency.initialize_consistency_calculations_for_new_decision();

            context.recMemory.FIRING_TYPE = SavedFiringType.IE_PRODS;
            context.consistency.determine_highest_active_production_level_in_stack_propose();

            if (current_phase == Phase.DECISION_PHASE)
            { 
                // no elaborations will fire this phases
                this.run_elaboration_count++; // All phases count as a run elaboration

                afterElaboration();
            }
        }

        // max-elaborations are checked in determine_highest_active... and if they
        // are reached, the current phases is set to DECISION.  phases is also set
        // to DECISION when PROPOSE is done.
        while (current_phase != Phase.DECISION_PHASE)
        {
            if (e_cycles_this_d_cycle != 0)
            {
                beforeElaboration();
            }
            context.recMemory.do_preference_phase(context.decider.top_goal, context.osupport.o_support_calculation_type);
            context.decider.do_working_memory_phase();
            
            // Update accounting.
            this.e_cycle_count++;
            this.e_cycles_this_d_cycle++;
            this.run_elaboration_count++;
            
            context.consistency.determine_highest_active_production_level_in_stack_propose();
            
            afterElaboration();
            
            if (this.go_type == GoType.GO_ELABORATION)
                break;
        }

        /*  If we've finished PROPOSE, then current_phase will be equal to DECISION
         *  otherwise, we're only stopping because we're running by ELABORATIONS, so
         *  don't do the end-of-phases updating in that case.
         */
        if (current_phase == Phase.DECISION_PHASE)
        {
            /* This is a HACK for Soar 8.6.0 beta release... KCoulter April 05
             * We got here, because we should move to DECISION, so PROPOSE is done
             * Set phases back to PROPOSE, do print_phase, callbacks, and then
             * reset phases to DECISION
             */
            this.current_phase = Phase.PROPOSE_PHASE;
            Phase.PROPOSE_PHASE.trace(context.trace, false);

            afterPhase(Phase.PROPOSE_PHASE);
            this.current_phase = Phase.DECISION_PHASE;
        }

        // #ifndef NO_TIMING_STUFF
        // stop_timer (thisAgent, &thisAgent->start_phase_tv, &thisAgent->decision_cycle_phase_timers[PROPOSE_PHASE]);
        // #endif

        // END of Soar8 PROPOSE PHASE    
    }

    /**
     * extracted from do_one_top_level_phase(), switch case INPUT_PHASE
     */
    private void doInputPhase()
    {
        assert current_phase == Phase.INPUT_PHASE;
        
        Phase.INPUT_PHASE.trace(context.trace, true);

        // for Operand2 mode using the new decision cycle ordering,
        // we need to do some initialization in the INPUT PHASE, which
        // now comes first.  e_cycles are also zeroed before the APPLY Phase.
        if (context.operand2_mode == true)
        {
            this.context.chunker.chunks_this_d_cycle = 0;
            this.e_cycles_this_d_cycle = 0;
        }
        
        // #ifndef NO_TIMING_STUFF
        // start_timer (thisAgent, &thisAgent->start_phase_tv);
        // #endif

        // we check e_cycle_count because Soar 7 runs multiple input cycles per decision
        // always true for Soar 8
        if (e_cycles_this_d_cycle == 0)
        {
            context.getEventManager().fireEvent(beforeDecisionCycleEvent);
        }

        // #ifdef REAL_TIME_BEHAVIOR /* RM Jones */
        // test_for_input_delay(thisAgent);
        // #endif
        // #ifdef ATTENTION_LAPSE /* RM Jones */
        // determine_lapsing(thisAgent);
        // #endif

        if (input_cycle_flag == true)
        { 
            // Soar 7 flag, but always true for Soar8
            beforePhase(Phase.INPUT_PHASE);

            context.io.do_input_cycle();

            run_elaboration_count++; // All phases count as a run elaboration
            
            afterPhase(Phase.INPUT_PHASE);

            if (input_period != 0)
                input_cycle_flag = false;
        }

        Phase.INPUT_PHASE.trace(context.trace, false);

        // #ifndef NO_TIMING_STUFF /* REW: 28.07.96 */
        // stop_timer (thisAgent, &thisAgent->start_phase_tv, &thisAgent->decision_cycle_phase_timers[INPUT_PHASE]);
        // #endif

        if (context.operand2_mode == true)
        {
            current_phase = Phase.PROPOSE_PHASE;
        }
        else
        { 
            // we're running in Soar7 mode
            if (context.soarReteListener.any_assertions_or_retractions_ready())
                current_phase = Phase.PREFERENCE_PHASE;
            else
                current_phase = Phase.DECISION_PHASE;
        }

    }

    /**
     * extracted from do_one_top_level_phase, switch case PREFERENCE_PHASE
     */
    private void doPreferencePhase()
    {
        assert current_phase == Phase.PREFERENCE_PHASE;
        
        // starting with 8.6.0, PREFERENCE_PHASE is only Soar 7 mode -- applyPhase not valid here
        // needs to be updated for gSKI interface, and gSKI needs to accommodate Soar 7

        beforeElaboration();

        // #ifndef NO_TIMING_STUFF
        // start_timer (thisAgent, &thisAgent->start_phase_tv);
        // #endif
        beforePhase(Phase.PREFERENCE_PHASE);
        context.recMemory.do_preference_phase(context.decider.top_goal, context.osupport.o_support_calculation_type);

        this.run_elaboration_count++; // All phases count as a run elaboration
        
        afterPhase(Phase.PREFERENCE_PHASE);

        current_phase = Phase.WM_PHASE;

        // #ifndef NO_TIMING_STUFF
        // stop_timer (thisAgent, &thisAgent->start_phase_tv, &thisAgent->decision_cycle_phase_timers[PREFERENCE_PHASE]);
        // #endif
        
        // END of Soar7 PREFERENCE PHASE
    }

    /**
     * At the beginning of a top level phase, we first check whether the agent 
     * is halted and immediately bail out if it is, printing out a halted
     * message.
     * 
     * @return true if the system is halted, false otherwise
     */
    private boolean checkForSystemHaltedAtStartOfTopLevel()
    {
        final Printer printer = context.getPrinter();

        if (system_halted)
        {
            printer.print("\nSystem halted.  Use (init-soar) before running Soar again.");
            printer.flush();
            stop_soar = true;
            reason_for_stopping = "System halted.";
            return true;
        }
        return false;
    }

    /**
     * At the end of a top level phase, we check whether the system has been
     * halted, and if so, fire the halt event and do some cleanup.
     */
    private void checkForSystemHalt()
    {
        if (system_halted)
        {
            stop_soar = true;
            reason_for_stopping = "System halted.";
            
            context.getEventManager().fireEvent(afterHaltEvent);

            // To model episodic task, after halt, perform RL update with next-state value 0
            if (context.rl.rl_enabled())
            {
                // TODO reinforcement learning: how about a method?
                for (IdentifierImpl g = context.decider.bottom_goal; g != null; g = g.higher_goal)
                {
                    context.rl.rl_tabulate_reward_value_for_goal(g);
                    context.rl.rl_perform_update(0, g);
                }
            }
        }
    }

    /**
     * init_soar.cpp:1123:run_for_n_phases
     * 
     * @param n Number of phases to run. Must be non-negative
     * @throws IllegalArgumentException if n is negative
     */
    private void run_for_n_phases(int n)
    {
        Arguments.check(n >= 0, "n must be non-negative");
        
        startTopLevelTimers();
        
        stop_soar = false;
        reason_for_stopping = null;
        context.consistency.setHitMaxElaborations(false);
        while (!stop_soar && n != 0)
        {
            do_one_top_level_phase();
            n--;
        }
        
        pauseTopLevelTimers();
    }

    /**
     * Run for n elaboration cycles
     * 
     * init_soar.cpp:1142:run_for_n_elaboration_cycles
     * 
     * @param n Number of elaboration cycles to run. Must be non-negative
     * @throws IllegalArgumentException if n is negative
     */
    private void run_for_n_elaboration_cycles(int n)
    {
        Arguments.check(n >= 0, "n must be non-negative");

        startTopLevelTimers();

        stop_soar = false;
        reason_for_stopping = null;
        int e_cycles_at_start = e_cycle_count;
        int d_cycles_at_start = d_cycle_count;
        int elapsed_cycles = 0;
        GoType save_go_type = GoType.GO_PHASE;
        if (context.operand2_mode)
        {
            elapsed_cycles = -1;
            save_go_type = go_type;
            go_type = GoType.GO_ELABORATION;
            // need next line or runs only the input phases for "d 1" after init-soar
            if (d_cycles_at_start == 0)
                d_cycles_at_start++;
        }
        while (!stop_soar)
        {
            if (context.operand2_mode)
            {
                elapsed_cycles++;
            }
            else
            {
                elapsed_cycles = (d_cycle_count - d_cycles_at_start) + (e_cycle_count - e_cycles_at_start);
            }
            if (n == elapsed_cycles)
                break;
            do_one_top_level_phase();
        }
        if (context.operand2_mode)
        {
            go_type = save_go_type;
        }

        pauseTopLevelTimers();
    }

    /**
     * init_soar.cpp:1181:run_for_n_modifications_of_output
     * 
     * @param n Number of modifications. Must be non-negative.
     * @throws IllegalArgumentException if n is negative
     */
    private void run_for_n_modifications_of_output(int n)
    {
        Arguments.check(n >= 0, "n must be non-negative");

        startTopLevelTimers();

        stop_soar = false;
        reason_for_stopping = null;
        int count = 0;
        while (!stop_soar && n != 0)
        {
            boolean was_output_phase = current_phase == Phase.OUTPUT_PHASE;
            do_one_top_level_phase();
            if (was_output_phase)
            {
                if (context.io.isOutputLinkChanged())
                {
                    n--;
                }
                else
                {
                    count++;
                }
            }
            if (count >= this.maxNilOutputCycles)
            {
                stop_soar = true;
                reason_for_stopping = "exceeded max_nil_output_cycles with no output";
            }
        }
        
        pauseTopLevelTimers();
    }

    /**
     * Run for n decision cycles
     * 
     * @param n Number of cycles to run. Must be non-negative
     * @throws IllegalArgumentException if n is negative
     */
    private void run_for_n_decision_cycles(int n)
    {
        Arguments.check(n >= 0, "n must be non-negative");

        startTopLevelTimers();

        stop_soar = false;
        reason_for_stopping = null;
        int d_cycles_at_start = d_cycle_count;
        /* need next line or runs only the input phases for "d 1" after init-soar */
        if (context.operand2_mode && (d_cycles_at_start == 0))
            d_cycles_at_start++;
        while (!stop_soar)
        {
            if (n == (d_cycle_count - d_cycles_at_start))
                break;
            do_one_top_level_phase();
        }
        
        pauseTopLevelTimers();
    }
    
    public void runFor(int n, RunType runType)
    {
        if(checkForSystemHaltedAtStartOfTopLevel())
        {
            return;
        }
        switch(runType)
        {
        case ELABORATIONS: run_for_n_elaboration_cycles(n); break;
        case DECISIONS: run_for_n_decision_cycles(n); break;
        case PHASES: run_for_n_phases(n); break;
        case MODIFICATIONS_OF_OUTPUT: run_for_n_modifications_of_output(n); break;
        case FOREVER: runForever(); break;
        default:
            throw new IllegalArgumentException("Unknown run type: " + runType);
        }
    }
    
    /**
     * init_soar.cpp:1105:run_forever
     */
    public void runForever()
    {
        if(checkForSystemHaltedAtStartOfTopLevel())
        {
            return;
        }
        startTopLevelTimers();
        
        stop_soar = false;
        reason_for_stopping = null;
        while (!stop_soar)
        {
            do_one_top_level_phase();
        }
        
        pauseTopLevelTimers();
    }
    
        
    /**
     * @return true if this agent has been stopped, halted, or interrupted
     */
    public boolean isStopped()
    {
        return stop_soar;
    }
    
    /**
     * @return The reason for stopping, or <code>null</code> if {@link #isStopped()}
     *      is false.
     */
    public String getReasonForStop()
    {
        return reason_for_stopping;
    }
    
    /**
     * Stop the agent.
     */
    public void stop()
    {
        if(!stop_soar)
        {
            this.stop_soar = true;
            this.reason_for_stopping = "Stopped by user.";
        }
    }
    
    /**
     * Interrupt this agent.
     * 
     * <p>rhsfun.cpp:231
     * 
     * @param production The name of the production causing the interrupt, or 
     *  <code>null</code> if unknown.
     */
    public void interrupt(String production)
    {
        this.stop_soar = true;
        this.reason_for_stopping = "*** Interrupt from production " + production + " ***";
    }

    /**
     * @return true if the agent is halted
     */
    public boolean isHalted()
    {
        return system_halted;
    }
    
    /**
     * Halt this agent. A halted agent can only be restarted with an init-soar.
     * 
     * @param string Reason for the halt
     */
    public void halt(String string)
    {
        stop_soar = true;
        system_halted = true;
        reason_for_stopping = "Max Goal Depth exceeded.";
    }
}
