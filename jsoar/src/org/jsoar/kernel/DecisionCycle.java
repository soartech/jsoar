/*
 * Copyright (c) 2008  Dave Ray <daveray@gmail.com>
 *
 * Created on Sep 14, 2008
 */
package org.jsoar.kernel;

import java.io.IOException;
import java.io.Writer;
import java.util.List;

import org.jsoar.kernel.rhs.functions.AbstractRhsFunctionHandler;
import org.jsoar.kernel.rhs.functions.RhsFunctionException;
import org.jsoar.kernel.rhs.functions.RhsFunctionHandler;
import org.jsoar.kernel.symbols.Identifier;
import org.jsoar.kernel.symbols.Symbol;
import org.jsoar.kernel.symbols.SymbolFactory;
import org.jsoar.kernel.tracing.Printer;
import org.jsoar.kernel.tracing.Trace.Category;
import org.jsoar.util.Arguments;

/**
 * An attempt to encapsulate the Soar decision cycle
 * 
 * @author ray
 */
public class DecisionCycle
{
    private final Agent context;
    
    public enum GoType
    {
        GO_PHASE, GO_ELABORATION, GO_DECISION,
        GO_STATE, GO_OPERATOR, GO_SLOT, GO_OUTPUT
    }
    
    public boolean system_halted = false;
    public boolean stop_soar = false;
    public String reason_for_stopping = null;
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
    private boolean input_cycle_flag;
    private int run_phase_count;
    private int run_elaboration_count;
    private int input_period;
    private boolean applyPhase;
    public int e_cycle_count;
    private int pe_cycle_count;
    private int pe_cycles_this_d_cycle;
    private int run_last_output_count;
    private int run_generated_output_count;
    public int d_cycle_count;
    private int decision_phases_count;
    
    /**
     * gsysparams.h::MAX_NIL_OUTPUT_CYCLES_SYSPARAM
     */
    private int maxNilOutputCycles = 15;
    
    /**
     * rhsfun.cpp:199:halt_rhs_function_code
     */
    private RhsFunctionHandler haltHandler = new AbstractRhsFunctionHandler("halt") {

        @Override
        public Symbol execute(SymbolFactory syms, List<Symbol> arguments) throws RhsFunctionException
        {
            system_halted = true;
            
            // TODO callback AFTER_HALT_SOAR_CALLBACK
            //soar_invoke_callbacks(thisAgent, AFTER_HALT_SOAR_CALLBACK, (soar_call_data) NULL);
            
            return null;
        }};
    
    public DecisionCycle(Agent context)
    {
        this.context = context;
        
        context.getRhsFunctions().registerHandler(haltHandler);
    }
    

    /**
     * runs Soar one top-level phases. Note that this does not start/stop the 
     * total_cpu_time timer--the caller must do this. 
     * 
     * init_soar.cpp:474:do_one_top_level_phase
     */
    public void do_one_top_level_phase()
    {
        final Printer printer = context.getPrinter();

        if (system_halted)
        {
            printer.print("\nSystem halted.  Use (init-soar) before running Soar again.");
            // TODO xml generate
            // xml_generate_error(thisAgent, "System halted. Use (init-soar) before running Soar again.");
            stop_soar = true;
            reason_for_stopping = "System halted.";
            return;
        }

        switch (current_phase)
        {

        case INPUT_PHASE:
            current_phase.trace(context.trace, true);

            /* for Operand2 mode using the new decision cycle ordering,
             * we need to do some initialization in the INPUT PHASE, which
             * now comes first.  e_cycles are also zeroed before the APPLY Phase.
             */
            if (context.operand2_mode == true)
            {
                this.context.chunker.chunks_this_d_cycle = 0;
                this.e_cycles_this_d_cycle = 0;
            }
            // #ifndef NO_TIMING_STUFF /* REW: 28.07.96 */
            // start_timer (thisAgent, &thisAgent->start_phase_tv);
            // #endif

            /* we check e_cycle_count because Soar 7 runs multiple input cycles per decision */
            /* always true for Soar 8 */
            if (e_cycles_this_d_cycle == 0)
            {
                // TODO callback BEFORE_DECISION_CYCLE_CALLBACK/INPUT_PHASE
                // soar_invoke_callbacks(thisAgent,
                // BEFORE_DECISION_CYCLE_CALLBACK,
                // (soar_call_data) INPUT_PHASE);
            } /* end if e_cycles_this_d_cycle == 0 */

            // #ifdef REAL_TIME_BEHAVIOR /* RM Jones */
            // test_for_input_delay(thisAgent);
            // #endif
            // #ifdef ATTENTION_LAPSE /* RM Jones */
            // determine_lapsing(thisAgent);
            // #endif

            if (input_cycle_flag == true)
            { /* Soar 7 flag, but always true for Soar8 */
                // TODO callback BEFORE_INPUT_PHASE_CALLBACK/INPUT_PHASE
                // soar_invoke_callbacks(thisAgent,
                // BEFORE_INPUT_PHASE_CALLBACK,
                // (soar_call_data) INPUT_PHASE);

                context.io.do_input_cycle();

                run_phase_count++;
                run_elaboration_count++; // All phases count as a run elaboration
                // TODO callback AFTER_INPUT_PHASE_CALLBACK/INPUT_PHASE
                // soar_invoke_callbacks(thisAgent,
                // AFTER_INPUT_PHASE_CALLBACK,
                // (soar_call_data) INPUT_PHASE);

                if (input_period != 0)
                    input_cycle_flag = false;
            } /* END if (input_cycle_flag==TRUE) AGR REW1 this line and 1 previous line */

            Phase.INPUT_PHASE.trace(context.trace, false);

            // #ifndef NO_TIMING_STUFF /* REW: 28.07.96 */
            // stop_timer (thisAgent, &thisAgent->start_phase_tv,
            // &thisAgent->decision_cycle_phase_timers[INPUT_PHASE]);
            // #endif

            if (context.operand2_mode == true)
            {
                current_phase = Phase.PROPOSE_PHASE;
            }
            else
            { /* we're running in Soar7 mode */
                if (context.soarReteListener.any_assertions_or_retractions_ready())
                    current_phase = Phase.PREFERENCE_PHASE;
                else
                    current_phase = Phase.DECISION_PHASE;
            }

            break; /* END of INPUT PHASE */

        // ///////////////////////////////////////////////////////////////////////////////

        case PROPOSE_PHASE: /* added in 8.6 to clarify Soar8 decision cycle */

            // #ifndef NO_TIMING_STUFF
            // start_timer (thisAgent, &thisAgent->start_phase_tv);
            // #endif

            /* e_cycles_this_d_cycle will always be zero UNLESS we are 
             * running by ELABORATIONS.
             * We only want to do the following if we've just finished INPUT and are
             * starting PROPOSE.  If this is the second elaboration for PROPOSE, then 
             * just do the while loop below.   KJC  June 05
             */
            if (this.e_cycles_this_d_cycle < 1)
            {
                Phase.PROPOSE_PHASE.trace(context.trace, true);

                // TODO callback BEFORE_PROPOSE_PHASE_CALLBACK/PROPOSE_PHASE
                // soar_invoke_callbacks(thisAgent,
                // BEFORE_PROPOSE_PHASE_CALLBACK,
                // (soar_call_data) PROPOSE_PHASE);

                // TODO callback BEFORE_ELABORATION_CALLBACK
                // We need to generate this event here in case no elaborations fire...
                // FIXME return the correct enum top_level_phase constant in soar_call_data?
                /*(soar_call_data)((thisAgent->applyPhase == TRUE)? gSKI_K_APPLY_PHASE: gSKI_K_PROPOSAL_PHASE)*/
                // soar_invoke_callbacks(thisAgent, BEFORE_ELABORATION_CALLBACK, NULL ) ;
                /* 'Prime the decision for a new round of production firings at the end of
                 * REW:   05.05.97   *//*  KJC 04.05 moved here from INPUT_PHASE for 8.6.0 */
                context.consistency.initialize_consistency_calculations_for_new_decision();

                context.recMemory.FIRING_TYPE = SavedFiringType.IE_PRODS;
                this.applyPhase = false; /* KJC 04/05: do we still need this line?  gSKI does*/
                context.consistency.determine_highest_active_production_level_in_stack_propose();

                if (current_phase == Phase.DECISION_PHASE)
                { // no elaborations will fire this phases
                    this.run_elaboration_count++; // All phases count as a run elaboration

                    // TODO callback AFTER_ELABORATION_CALLBACK
                    // FIXME return the correct enum top_level_phase constant in soar_call_data?
                    /*(soar_call_data)((thisAgent->applyPhase == TRUE)? gSKI_K_APPLY_PHASE: gSKI_K_PROPOSAL_PHASE)*/
                    // soar_invoke_callbacks(thisAgent, AFTER_ELABORATION_CALLBACK, NULL ) ;
                }
            }

            /* max-elaborations are checked in determine_highest_active... and if they
            * are reached, the current phases is set to DECISION.  phases is also set
            * to DECISION when PROPOSE is done.
            */

            while (current_phase != Phase.DECISION_PHASE)
            {
                if (e_cycles_this_d_cycle != 0)
                {
                    // TODO callback BEFORE_ELABORATION_CALLBACK
                    // only for 2nd cycle or higher. 1st cycle fired above
                    // FIXME return the correct enum top_level_phase constant in soar_call_data?
                    // /*(soar_call_data)((thisAgent->applyPhase == TRUE)? gSKI_K_APPLY_PHASE: gSKI_K_PROPOSAL_PHASE)*/
                    // soar_invoke_callbacks(thisAgent, BEFORE_ELABORATION_CALLBACK, NULL ) ;
                }
                context.recMemory.do_preference_phase(context.decider.top_goal,
                        context.osupport.o_support_calculation_type);
                context.decider.do_working_memory_phase();
                /* Update accounting.  Moved here by KJC 04/05/05 */
                this.e_cycle_count++;
                this.e_cycles_this_d_cycle++;
                this.run_elaboration_count++;
                context.consistency.determine_highest_active_production_level_in_stack_propose();
                // TODO callback AFTER_ELABORATION_CALLBACK
                // FIXME return the correct enum top_level_phase constant in soar_call_data?
                // /*(soar_call_data)((thisAgent->applyPhase == TRUE)? gSKI_K_APPLY_PHASE: gSKI_K_PROPOSAL_PHASE)*/
                // soar_invoke_callbacks(thisAgent, AFTER_ELABORATION_CALLBACK, NULL ) ;
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

                this.run_phase_count++;
                // TODO callback AFTER_PROPOSE_PHASE_CALLBACK/PROPOSE_PHASE
                // soar_invoke_callbacks(thisAgent,
                // AFTER_PROPOSE_PHASE_CALLBACK,
                // (soar_call_data) PROPOSE_PHASE);
                this.current_phase = Phase.DECISION_PHASE;
            }

            // #ifndef NO_TIMING_STUFF
            // stop_timer (thisAgent, &thisAgent->start_phase_tv,
            // &thisAgent->decision_cycle_phase_timers[PROPOSE_PHASE]);
            // #endif

            break; /* END of Soar8 PROPOSE PHASE */

        // ///////////////////////////////////////////////////////////////////////////////
        case PREFERENCE_PHASE:
            /* starting with 8.6.0, PREFERENCE_PHASE is only Soar 7 mode -- applyPhase not valid here */
            /* needs to be updated for gSKI interface, and gSKI needs to accommodate Soar 7 */

            // TODO callback BEFORE_ELABORATION_CALLBACK
            // FIXME return the correct enum top_level_phase constant in soar_call_data?
            // /*(soar_call_data)((thisAgent->applyPhase == TRUE)? gSKI_K_APPLY_PHASE: gSKI_K_PROPOSAL_PHASE)*/
            // soar_invoke_callbacks(thisAgent, BEFORE_ELABORATION_CALLBACK, NULL ) ;
            // #ifndef NO_TIMING_STUFF /* REW: 28.07.96 */
            // start_timer (thisAgent, &thisAgent->start_phase_tv);
            // #endif
            // TODO callback BEFORE_PREFERENCE_PHASE_CALLBACK/PREFERENCE_PHASE
            // soar_invoke_callbacks(thisAgent,
            // BEFORE_PREFERENCE_PHASE_CALLBACK,
            // (soar_call_data) PREFERENCE_PHASE);
            context.recMemory
                    .do_preference_phase(context.decider.top_goal, context.osupport.o_support_calculation_type);

            this.run_phase_count++;
            this.run_elaboration_count++; // All phases count as a run elaboration
            // TODO callback AFTER_PREFERENCE_PHASE_CALLBACK/PREFERENCE_PHASE
            // soar_invoke_callbacks(thisAgent,
            // AFTER_PREFERENCE_PHASE_CALLBACK,
            // (soar_call_data) PREFERENCE_PHASE);
            current_phase = Phase.WM_PHASE;

            // #ifndef NO_TIMING_STUFF /* REW: 28.07.96 */
            // stop_timer (thisAgent, &thisAgent->start_phase_tv,
            // &thisAgent->decision_cycle_phase_timers[PREFERENCE_PHASE]);
            // #endif

            /* tell gSKI PREF_PHASE ending... 
            */
            break; /* END of Soar7 PREFERENCE PHASE */

        // ///////////////////////////////////////////////////////////////////////////////
        case WM_PHASE:
            /* starting with 8.6.0, WM_PHASE is only Soar 7 mode; see PROPOSE and APPLY */
            /* needs to be updated for gSKI interface, and gSKI needs to accommodate Soar 7 */

            /*  we need to tell gSKI WM Phase beginning... */

            // #ifndef NO_TIMING_STUFF /* REW: begin 28.07.96 */
            // start_timer (thisAgent, &thisAgent->start_phase_tv);
            // #endif
            // TODO callback BEFORE_WM_PHASE_CALLBACK/WM_PHASE
            // soar_invoke_callbacks(thisAgent,
            // BEFORE_WM_PHASE_CALLBACK,
            // (soar_call_data) WM_PHASE);
            context.decider.do_working_memory_phase();

            this.run_phase_count++;
            this.run_elaboration_count++; // All phases count as a run elaboration
            // TODO callback AFTER_WM_PHASE_CALLBACK/WM_PHASE
            // soar_invoke_callbacks(thisAgent, AFTER_WM_PHASE_CALLBACK, (soar_call_data) WM_PHASE);

            current_phase = Phase.OUTPUT_PHASE;

            // #ifndef NO_TIMING_STUFF /* REW: 28.07.96 */
            // stop_timer (thisAgent, &thisAgent->start_phase_tv,
            // &thisAgent->decision_cycle_phase_timers[WM_PHASE]);
            // #endif

            // TODO callback AFTER_ELABORATION_CALLBACK
            // FIXME return the correct enum top_level_phase constant in soar_call_data?
            // /*(soar_call_data)((thisAgent->applyPhase == TRUE)? gSKI_K_APPLY_PHASE: gSKI_K_PROPOSAL_PHASE)*/
            // soar_invoke_callbacks(thisAgent, AFTER_ELABORATION_CALLBACK, NULL ) ;

            break; /* END of Soar7 WM PHASE */

        // ///////////////////////////////////////////////////////////////////////////////
        case APPLY_PHASE: /* added in 8.6 to clarify Soar8 decision cycle */

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

                // TODO callback BEFORE_APPLY_PHASE_CALLBACK/APPLY_PHASE
                // soar_invoke_callbacks(thisAgent, BEFORE_APPLY_PHASE_CALLBACK, (soar_call_data) APPLY_PHASE);

                // TODO callback BEFORE_ELABORATION_CALLBACK
                // We need to generate this event here in case no elaborations fire...
                // FIXME return the correct enum top_level_phase constant in soar_call_data?
                // /*(soar_call_data)((thisAgent->applyPhase == TRUE)? gSKI_K_APPLY_PHASE: gSKI_K_PROPOSAL_PHASE)*/
                // soar_invoke_callbacks(thisAgent, BEFORE_ELABORATION_CALLBACK, NULL ) ;

                /* 'prime' the cycle for a new round of production firings 
                * in the APPLY (pref/wm) phases *//* KJC 04.05 moved here from end of DECISION */
                context.consistency.initialize_consistency_calculations_for_new_decision();

                context.recMemory.FIRING_TYPE = SavedFiringType.PE_PRODS; /* might get reset in det_high_active_prod_level... */
                applyPhase = true; /* KJC 04/05: do we still need this line?  gSKI does*/
                context.consistency.determine_highest_active_production_level_in_stack_apply();
                if (current_phase == Phase.OUTPUT_PHASE)
                { // no elaborations will fire this phases
                    this.run_elaboration_count++; // All phases count as a run elaboration

                    // TODO callback AFTER_ELABORATION_CALLBACK
                    // FIXME return the correct enum top_level_phase constant in soar_call_data?
                    // /*(soar_call_data)((thisAgent->applyPhase == TRUE)? gSKI_K_APPLY_PHASE: gSKI_K_PROPOSAL_PHASE)*/
                    // soar_invoke_callbacks(thisAgent, AFTER_ELABORATION_CALLBACK, NULL ) ;
                }
            }
            /* max-elaborations are checked in determine_highest_active... and if they
             * are reached, the current phases is set to OUTPUT.  phases is also set
             * to OUTPUT when APPLY is done.
             */

            while (current_phase != Phase.OUTPUT_PHASE)
            {
                if (this.e_cycles_this_d_cycle != 0)
                {
                    // TODO callback BEFORE_ELABORATION_CALLBACK
                    // only for 2nd cycle or higher. 1st cycle fired above
                    // FIXME return the correct enum top_level_phase constant in soar_call_data?
                    // /*(soar_call_data)((thisAgent->applyPhase == TRUE)? gSKI_K_APPLY_PHASE: gSKI_K_PROPOSAL_PHASE)*/
                    // soar_invoke_callbacks(thisAgent, BEFORE_ELABORATION_CALLBACK, NULL ) ;
                }
                context.recMemory.do_preference_phase(context.decider.top_goal,
                        context.osupport.o_support_calculation_type);
                context.decider.do_working_memory_phase();

                /* Update accounting.  Moved here by KJC 04/05/05 */
                this.e_cycle_count++;
                this.e_cycles_this_d_cycle++;
                this.run_elaboration_count++;

                if (context.recMemory.FIRING_TYPE == SavedFiringType.PE_PRODS)
                {
                    this.pe_cycle_count++;
                    this.pe_cycles_this_d_cycle++;
                }
                context.consistency.determine_highest_active_production_level_in_stack_apply();

                // TODO callback AFTER_ELABORATION_CALLBACK
                // FIXME return the correct enum top_level_phase constant in soar_call_data?
                // /*(soar_call_data)((thisAgent->applyPhase == TRUE)? gSKI_K_APPLY_PHASE: gSKI_K_PROPOSAL_PHASE)*/
                // soar_invoke_callbacks(thisAgent, AFTER_ELABORATION_CALLBACK, NULL ) ;

                if (this.go_type == GoType.GO_ELABORATION)
                    break;
            }

            /*  If we've finished APPLY, then current_phase will be equal to OUTPUT
             *  otherwise, we're only stopping because we're running by ELABORATIONS, so
             *  don't do the end-of-phases updating in that case.
             */
            if (current_phase == Phase.OUTPUT_PHASE)
            {
                /* This is a HACK for Soar 8.6.0 beta release... KCoulter April 05
                 * We got here, because we should move to OUTPUT, so APPLY is done
                 * Set phases back to APPLY, do print_phase, callbacks and reset phases to OUTPUT
                 */
                current_phase = Phase.APPLY_PHASE;
                Phase.APPLY_PHASE.trace(context.trace, false);

                this.run_phase_count++;
                // TODO callback AFTER_APPLY_PHASE_CALLBACK/APPLY_PHASE
                // soar_invoke_callbacks(thisAgent, AFTER_APPLY_PHASE_CALLBACK, (soar_call_data) APPLY_PHASE);

                current_phase = Phase.OUTPUT_PHASE;
            }

            // #ifndef NO_TIMING_STUFF
            // stop_timer (thisAgent, &thisAgent->start_phase_tv,
            // &thisAgent->decision_cycle_phase_timers[APPLY_PHASE]);
            // #endif

            break; /* END of Soar8 APPLY PHASE */

        // ///////////////////////////////////////////////////////////////////////////////
        case OUTPUT_PHASE:

            Phase.OUTPUT_PHASE.trace(context.trace, true);

            // #ifndef NO_TIMING_STUFF /* REW: 28.07.96 */
            // start_timer (thisAgent, &thisAgent->start_phase_tv);
            // #endif

            // TODO callback BEFORE_OUTPUT_PHASE_CALLBACK/OUTPUT_PHASE
            // soar_invoke_callbacks(thisAgent, BEFORE_OUTPUT_PHASE_CALLBACK, (soar_call_data) OUTPUT_PHASE);

            /** KJC June 05: moved output function timers into do_output_cycle ** */

            context.io.do_output_cycle();

            // Count the outputs the agent generates (or times reaching max-nil-outputs without sending output)
            if (context.io.output_link_changed || ((++(run_last_output_count)) >= maxNilOutputCycles))
            {
                this.run_last_output_count = 0;
                this.run_generated_output_count++;
            }

            this.run_phase_count++;
            this.run_elaboration_count++; // All phases count as a run elaboration
            // TODO callback AFTER_OUTPUT_PHASE_CALLBACK
            // soar_invoke_callbacks(thisAgent, AFTER_OUTPUT_PHASE_CALLBACK, (soar_call_data) OUTPUT_PHASE);

            /* REW: begin 09.15.96 */
            if (context.operand2_mode == true)
            {
                // TODO xml
                // JRV: Get rid of the cached XML after every decision but before the after-decision-phases callback
                // xml_invoke_callback( thisAgent ); // invokes XML_GENERATION_CALLBACK, clears XML state

                /* KJC June 05:  moved here from DECISION Phase */
                // TODO callback AFTER_DECISION_CYCLE_CALLBACK/OUTPUT_PHASE
                // soar_invoke_callbacks(thisAgent, AFTER_DECISION_CYCLE_CALLBACK, (soar_call_data) OUTPUT_PHASE);
                // #ifndef NO_TIMING_STUFF /* timers stopped KJC 10-04-98 */
                // stop_timer (thisAgent, &thisAgent->start_phase_tv,
                // &thisAgent->decision_cycle_phase_timers[OUTPUT_PHASE]);
                // #endif
                Phase.OUTPUT_PHASE.trace(context.trace, false);
                current_phase = Phase.INPUT_PHASE;
                this.d_cycle_count++;
                break;
            } /* REW: end 09.15.96 */

            /* ******************* otherwise we're in Soar7 mode ...  */

            this.e_cycle_count++;
            this.e_cycles_this_d_cycle++;
            this.run_elaboration_count++; // All phases count as a run elaboration

            Phase.OUTPUT_PHASE.trace(context.trace, false);

            /* MVP 6-8-94 */
            if (e_cycles_this_d_cycle >= 100
            /* TODO (unsigned long)(thisAgent->sysparams[MAX_ELABORATIONS_SYSPARAM])*/
            )
            {
                context.getPrinter().warn("Warning: reached max-elaborations; proceeding to decision phases.");
                // xml_generate_warning(thisAgent, "Warning: reached max-elaborations; proceeding to decision phases.");
                current_phase = Phase.DECISION_PHASE;
            }
            else
            {
                current_phase = Phase.INPUT_PHASE;
            }

            /* REW: begin 28.07.96 */
            // #ifndef NO_TIMING_STUFF
            // stop_timer (thisAgent, &thisAgent->start_phase_tv,
            // &thisAgent->decision_cycle_phase_timers[OUTPUT_PHASE]);
            // #endif
            /* REW: end 28.07.96 */

            break;

        // ///////////////////////////////////////////////////////////////////////////////
        case DECISION_PHASE:
            /* not yet cleaned up for 8.6.0 release */

            Phase.DECISION_PHASE.trace(context.trace, true);

            // #ifndef NO_TIMING_STUFF /* REW: 28.07.96 */
            // start_timer (thisAgent, &thisAgent->start_phase_tv);
            // #endif
            
            /* d_cycle_count moved to input phases for Soar 8 new decision cycle */
            if (context.operand2_mode == false)
                this.d_cycle_count++;
            this.decision_phases_count++; /* counts decisions, not cycles, for more accurate stats */

            /* AGR REW1 begin */
            if (input_period == 0)
                this.input_cycle_flag = true;
            else if ((this.d_cycle_count % this.input_period) == 0)
                this.input_cycle_flag = true;
            /* AGR REW1 end */

            // TODO callback BEFORE_DECISION_PHASE_CALLBACK/DECISION_PHASE
            // soar_invoke_callbacks(thisAgent, BEFORE_DECISION_PHASE_CALLBACK, (soar_call_data) DECISION_PHASE);
            context.decider.do_decision_phase(false);

            this.run_phase_count++;
            this.run_elaboration_count++; // All phases count as a run elaboration

            // TODO callback AFTER_DECISION_PHASE_CALLBACK/DECISION_PHASE
            // soar_invoke_callbacks(thisAgent, AFTER_DECISION_PHASE_CALLBACK, (soar_call_data) DECISION_PHASE);

            if (context.trace.isEnabled(Category.TRACE_CONTEXT_DECISIONS_SYSPARAM)) {
                final Writer writer = context.trace.getPrinter().getWriter();
                try
                {
                    writer.append("\n");
                    context.decider.print_lowest_slot_in_context_stack (writer);
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

                // TODO callback AFTER_DECISION_CYCLE_CALLBACK/DECISION_PHASE
                /* KJC June 05: Soar8 - moved AFTER_DECISION_CYCLE_CALLBACK to proper spot in OUTPUT */
                // soar_invoke_callbacks(thisAgent, AFTER_DECISION_CYCLE_CALLBACK, (soar_call_data) DECISION_PHASE);
                context.chunker.chunks_this_d_cycle = 0;

                Phase.DECISION_PHASE.trace(context.trace, false);

                current_phase = Phase.INPUT_PHASE;
            }
            /* reset elaboration counter */
            this.e_cycles_this_d_cycle = 0;
            this.pe_cycles_this_d_cycle = 0;

            /* REW: begin 09.15.96 */
            if (context.operand2_mode == true)
            {
                /*
                 TODO What to do about this blob of code... AGRESSIVE_ONC
                #ifdef AGRESSIVE_ONC
                // test for Operator NC, if TRUE, generate substate and go to OUTPUT
                if ((thisAgent->ms_o_assertions == NIL) &&
                    (thisAgent->bottom_goal->id.operator_slot->wmes != NIL)) 
                {

                    soar_invoke_callbacks(thisAgent, thisAgent, 
                                          BEFORE_DECISION_PHASE_CALLBACK,
                                          (soar_call_data) thisAgent->current_phase);
                
                    do_decision_phase(thisAgent);

                    soar_invoke_callbacks(thisAgent, thisAgent, AFTER_DECISION_PHASE_CALLBACK,
                                          (soar_call_data) thisAgent->current_phase);

                    if (thisAgent->sysparams[TRACE_CONTEXT_DECISIONS_SYSPARAM]) {
                //                  #ifdef USE_TCL
                        print_string (thisAgent, "\n");
                //                  #else
                //                if(thisAgent->printer_output_column != 1) print_string ("\n");
                //                  #endif 
                        print_lowest_slot_in_context_stack (thisAgent);
                    }
                    if (thisAgent->sysparams[TRACE_PHASES_SYSPARAM])           
                        print_phase (thisAgent, "\n--- END Decision Phase ---\n",1);

                    // set phases to OUTPUT
                    thisAgent->current_phase = OUTPUT_PHASE;

                    #ifndef NO_TIMING_STUFF
                    stop_timer (thisAgent, &thisAgent->start_phase_tv, 
                        &thisAgent->decision_cycle_phase_timers[DECISION_PHASE]);
                    #endif

                    break;
                
                } else 
                #endif //AGRESSIVE_ONC
                */
                {
                    Phase.DECISION_PHASE.trace(context.trace, false);

                    /* printf("\nSetting next phases to APPLY following a decision...."); */
                    this.applyPhase = true;
                    context.recMemory.FIRING_TYPE = SavedFiringType.PE_PRODS;
                    current_phase = Phase.APPLY_PHASE;
                }
            }

            /* REW: begin 28.07.96 */
            //      #ifndef NO_TIMING_STUFF
            //      stop_timer (thisAgent, &thisAgent->start_phase_tv, 
            //          &thisAgent->decision_cycle_phase_timers[DECISION_PHASE]);
            //      #endif
            /* REW: end 28.07.96 */

            break; /* end DECISION phases */

        /////////////////////////////////////////////////////////////////////////////////

        default: // 2/24/05: added default case to quell gcc compile warning
            throw new IllegalStateException("Invalid phases enumeration value " + current_phase);
        } /* end switch stmt for current_phase */

        /* --- update WM size statistics --- */
        if (context.rete.num_wmes_in_rete > context.workingMemory.max_wm_size)
            context.workingMemory.max_wm_size = context.rete.num_wmes_in_rete;
        context.workingMemory.cumulative_wm_size += context.rete.num_wmes_in_rete;
        context.workingMemory.num_wm_sizes_accumulated++;

        if (system_halted)
        {
            stop_soar = true;
            reason_for_stopping = "System halted.";
            // TODO callback AFTER_HALT_SOAR_CALLBACK/current_phase
            //soar_invoke_callbacks(thisAgent, AFTER_HALT_SOAR_CALLBACK, (soar_call_data) thisAgent->current_phase);

            // To model episodic task, after halt, perform RL update with next-state value 0
            if (context.rl.rl_enabled())
            {
                // TODO how about a method?
                for (Identifier g = context.decider.bottom_goal; g != null; g = g.higher_goal)
                {
                    context.rl.rl_tabulate_reward_value_for_goal(g);
                    context.rl.rl_perform_update(0, g);
                }
            }
        }

        if (stop_soar)
        {
            /* (voigtjr)
               this old test is nonsense, it compares pointers:

               if (thisAgent->reason_for_stopping != "")

               what really should happen here is reason_for_stopping should be
               set to NULL in the cases where nothing should be printed, instead 
               of being assigned a pointer to a zero length (NULL) string, then
               we could simply say:

               if (thisAgent->reason_for_stopping) 
             */
            if (reason_for_stopping != null && reason_for_stopping.length() > 0)
            {
                context.getPrinter().print("\n%s", reason_for_stopping);
            }
        }
    }

    /**
     * init_soar.cpp:1123:run_for_n_phases
     * 
     * @param n Number of phases to run. Must be non-negative
     * @throws IllegalArgumentException if n is negative
     */
    public void run_for_n_phases(int n)
    {
        Arguments.check(n >= 0, "n must be non-negative");
        
        // #ifndef NO_TIMING_STUFF
        // start_timer (thisAgent, &thisAgent->start_total_tv);
        // start_timer (thisAgent, &thisAgent->start_kernel_tv);
        // #endif
        
        stop_soar = false;
        reason_for_stopping = null;
        context.consistency.setHitMaxElaborations(false);
        while (!stop_soar && n != 0)
        {
            do_one_top_level_phase();
            n--;
        }
        
        // #ifndef NO_TIMING_STUFF
        // stop_timer (thisAgent, &thisAgent->start_kernel_tv,
        // &thisAgent->total_kernel_time);
        //  stop_timer (thisAgent, &thisAgent->start_total_tv, &thisAgent->total_cpu_time);
        //#endif
    }
    

    /**
     * Run for n elaboration cycles
     * 
     * init_soar.cpp:1142:run_for_n_elaboration_cycles
     * 
     * @param n Number of elaboration cycles to run. Must be non-negative
     * @throws IllegalArgumentException if n is negative
     */
    public void run_for_n_elaboration_cycles(int n)
    {
        Arguments.check(n >= 0, "n must be non-negative");

        // #ifndef NO_TIMING_STUFF
        // start_timer (thisAgent, &thisAgent->start_total_tv);
        // start_timer (thisAgent, &thisAgent->start_kernel_tv);
        // #endif

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

        //#ifndef NO_TIMING_STUFF
        //  stop_timer (thisAgent, &thisAgent->start_total_tv, &thisAgent->total_cpu_time);
        //  stop_timer (thisAgent, &thisAgent->start_kernel_tv, &thisAgent->total_kernel_time);
        //#endif
    }

    /**
     * init_soar.cpp:1181:run_for_n_modifications_of_output
     * 
     * @param n Number of modifications. Must be non-negative.
     * @throws IllegalArgumentException if n is negative
     */
    public void run_for_n_modifications_of_output(int n)
    {
        Arguments.check(n >= 0, "n must be non-negative");

        // #ifndef NO_TIMING_STUFF
        // start_timer (thisAgent, &thisAgent->start_total_tv);
        // start_timer (thisAgent, &thisAgent->start_kernel_tv);
        // #endif

        stop_soar = false;
        reason_for_stopping = null;
        int count = 0;
        while (!stop_soar && n != 0)
        {
            boolean was_output_phase = current_phase == Phase.OUTPUT_PHASE;
            do_one_top_level_phase();
            if (was_output_phase)
            {
                if (context.io.output_link_changed)
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
        //#ifndef NO_TIMING_STUFF
        //  stop_timer (thisAgent, &thisAgent->start_kernel_tv, &thisAgent->total_kernel_time);
        //  stop_timer (thisAgent, &thisAgent->start_total_tv, &thisAgent->total_cpu_time);
        //#endif
    }

    /**
     * Run for n decision cycles
     * 
     * @param n Number of cycles to run. Must be non-negative
     * @throws IllegalArgumentException if n is negative
     */
    public void run_for_n_decision_cycles(int n)
    {
        Arguments.check(n >= 0, "n must be non-negative");

        // #ifndef NO_TIMING_STUFF
        // start_timer (thisAgent, &thisAgent->start_total_tv);
        // start_timer (thisAgent, &thisAgent->start_kernel_tv);
        // #endif

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
        // #ifndef NO_TIMING_STUFF
        // stop_timer (thisAgent, &thisAgent->start_total_tv,
        // &thisAgent->total_cpu_time);
        //  stop_timer (thisAgent, &thisAgent->start_kernel_tv, &thisAgent->total_kernel_time);
        //#endif
    }
}
