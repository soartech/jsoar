/*
 * Copyright (c) 2008  Dave Ray <daveray@gmail.com>
 *
 * Created on Sep 12, 2008
 */
package org.jsoar.kernel;

import org.jsoar.kernel.Trace.Category;
import org.jsoar.kernel.memory.Preference;
import org.jsoar.kernel.memory.Slot;
import org.jsoar.kernel.memory.Wme;
import org.jsoar.kernel.symbols.Identifier;
import org.jsoar.kernel.symbols.Symbol;
import org.jsoar.util.Arguments;
import org.jsoar.util.ByRef;

/**
 * consistency.cpp
 * 
 * @author ray
 */
public class Consistency
{
    private static final boolean DEBUG_CONSISTENCY_CHECK = false;
    
    private static enum LevelChangeType
    {
        NEW_DECISION, SAME_LEVEL, HIGHER_LEVEL, LOWER_LEVEL, NIL_GOAL_RETRACTIONS
    }
    
    private final Agent context;
    /**
     * gsysparam.h:MAX_ELABORATIONS_SYSPARAM
     */
    private int maxElaborations = 100;
    
    /**
     * @param context
     */
    public Consistency(Agent context)
    {
        this.context = context;
    }
    
    
    /**
     * The current setting for max elaborations.
     * 
     * gsysparam.h::MAX_ELABORATIONS_SYSPARAM
     * 
     * @return the maxElaborations
     */
    public int getMaxElaborations()
    {
        return maxElaborations;
    }

    /**
     * The the value of max elaborations.
     * 
     * gsysparam.g::MAX_ELABORATIONS_SYSPARAM
     * 
     * @param maxElaborations the maxElaborations to set
     */
    public void setMaxElaborations(int maxElaborations)
    {
        Arguments.check(maxElaborations > 0, "max elaborations must be greater than zero");
        this.maxElaborations = maxElaborations;
    }

    /**
     * 
     * consistency.cpp:41:remove_operator_if_necessary
     * 
     * @param s
     * @param w
     */
    public void remove_operator_if_necessary(Slot s, Wme w)
    {
        // #ifndef NO_TIMING_STUFF
        // #ifdef DETAILED_TIMING_STATS
        // start_timer(thisAgent, &thisAgent->start_gds_tv);
        // #endif
        // #endif

        // Note: Deleted about 40 lines of commented printf debugging code here from CSoar

        if (!s.wmes.isEmpty())
        { /* If there is something in the context slot */
            if (s.wmes.getFirstItem().value == w.value)
            { /* The WME in the context slot is WME whose pref changed */
                context.trace.print(Category.TRACE_OPERAND2_REMOVALS_SYSPARAM,
                        "\n        REMOVING: Operator from context slot (proposal no longer matches): %s", w);
                context.decider.remove_wmes_for_context_slot(s);
                if (s.id.lower_goal != null)
                    context.decider.remove_existing_context_and_descendents(s.id.lower_goal);
            }
        }

        // #ifndef NO_TIMING_STUFF
        // #ifdef DETAILED_TIMING_STATS
        //  stop_timer(thisAgent, &thisAgent->start_gds_tv, 
        //             &thisAgent->gds_cpu_time[thisAgent->current_phase]);
        //  #endif
        //  #endif
    }


    /**
     * This code concerns the implementation of a 'consistency check' following each IE phases. The basic idea is that we
     * want context decisions to remain consistent with the current preferences, even if the proposal for some operator
     * is still acceptable 
     * 
     * consistency.cpp:116:decision_consistent_with_current_preferences
     * 
     * @param goal
     * @param s
     * @return
     */
    private boolean decision_consistent_with_current_preferences(Identifier goal, Slot s)
    {

        if (DEBUG_CONSISTENCY_CHECK)
        {
            if (s.isa_context_slot)
            {
                context.getPrinter().print(
                        "    slot (s)  isa context slot: " + "    Slot Identifier [%y] and attribute [%y]\n", s.id,
                        s.attr);
            }
            /* printf("    Address of s: %x\n", s); */
            context.getPrinter().print("    s->impasse_type: %s\n", s.impasse_type);
            if (s.impasse_id != null)
                context.getPrinter().print("    Impasse ID is set (non-NIL)\n");
        }

        /* Determine the current operator/impasse in the slot*/
        Wme current_operator;
        boolean operator_in_slot;
        if (!goal.operator_slot.wmes.isEmpty())
        {
            /* There is an operator in the slot */
            current_operator = goal.operator_slot.wmes.getFirstItem();
            operator_in_slot = true;
        }
        else
        {
            /* There is not an operator in the slot */
            current_operator = null;
            operator_in_slot = false;
        }

        boolean goal_is_impassed = false;
        ImpasseType current_impasse_type, new_impasse_type;
        Symbol current_impasse_attribute;
        if (goal.lower_goal != null)
        {
            /* the goal is impassed */
            goal_is_impassed = true;
            current_impasse_type = context.decider.type_of_existing_impasse(goal);
            current_impasse_attribute = context.decider.attribute_of_existing_impasse(goal);
            if (DEBUG_CONSISTENCY_CHECK)
            {
                context.getPrinter().print("    Goal is impassed:  Impasse type: %d: Impasse attribute: [%s]\n",
                        current_impasse_type, current_impasse_attribute);
            }
            /* Special case for an operator no-change */
            if ((operator_in_slot) && (current_impasse_type == ImpasseType.NO_CHANGE_IMPASSE_TYPE))
            {
                /* Operator no-change impasse: run_preference_semantics will return 0
                   and we only want to blow away this operator if another is better
                than it (checked in NONE_IMPASSE_TYPE switch) or if another kind
                of impasse would be generated (e.g., OPERATOR_TIE). So, we set
                the impasse type here to 0; that way we'll know that we should be
                comparing a previous decision for a unique operator against the
                current preference semantics. */
                if (DEBUG_CONSISTENCY_CHECK)
                {
                    context.getPrinter().print("    This is an operator no-change  impasse.\n");
                }
                current_impasse_type = ImpasseType.NONE_IMPASSE_TYPE;
            }
        }
        else
        {
            goal_is_impassed = false;
            current_impasse_type = ImpasseType.NONE_IMPASSE_TYPE;
            current_impasse_attribute = null;
            if (DEBUG_CONSISTENCY_CHECK)
            {
                context.getPrinter().print("    Goal is not impassed: ");
            }
        }

        /* Determine the new impasse type, based on the preferences that exist now */
        ByRef<Preference> candidates = ByRef.create(null);
        new_impasse_type = context.decider.run_preference_semantics_for_consistency_check(s, candidates);

        if (DEBUG_CONSISTENCY_CHECK)
        {
            context.getPrinter().print("    Impasse Type returned by run preference semantics: %d\n", new_impasse_type);

            for (Preference cand = candidates.value; cand != null; cand = cand.next_prev.getNextItem())
            {
                context.getPrinter().print("    Preference for slot: %s", cand);
            }

            for (Preference cand = candidates.value; cand != null; cand = cand.next_candidate)
            {
                context.getPrinter().print("\n    Candidate  for slot: %s", cand);
            }
        }

        if (current_impasse_type != new_impasse_type)
        {
            /* Then there is an inconsistency: no more work necessary */
            if (DEBUG_CONSISTENCY_CHECK)
            {
                context
                        .getPrinter()
                        .print(
                                "    Impasse types are different: Returning FALSE, preferences are not consistent with prior decision.\n");
            }
            return false;
        }

        /* in these cases, we know that the new impasse and the old impasse *TYPES* are the same.  We
           just want to check and make the actual impasses/decisions are the same. */
        switch (new_impasse_type)
        {

        case NONE_IMPASSE_TYPE:
            /* There are four cases to consider when NONE_IMPASSE_TYPE is returned: */
            /* 1.  Previous operator and operator returned by run_pref_sem are the same.
                   In this case, return TRUE (decision remains consistent) */

            /* This next if is meant to test that there actually is something in the slot but
               I'm nut quite certain that it will not always be true? */
            if (operator_in_slot)
            {
                if (DEBUG_CONSISTENCY_CHECK)
                {
                    context.getPrinter().print("    There is a WME in the operator slot:%s", current_operator);
                }

                /* Because of indifferent preferences, we need to compare all possible candidates
                with the current decision */
                for (Preference cand = candidates.value; cand != null; cand = cand.next_candidate)
                {
                    if (current_operator.value == cand.value)
                    {
                        if (DEBUG_CONSISTENCY_CHECK)
                        {
                            context.getPrinter().print(
                                    "       Operator slot ID [%s] and candidate ID [%s] are the same.\n",
                                    current_operator.value, cand.value);
                        }
                        return true;
                    }
                }

                /* 2.  A different operator is indicated for the slot than the one that is
                       currently installed.  In this case, we return FALSE (the decision is
                       not consistent with the preferences). */

                /* Now we know that the decision is inconsistent */
                return false;

                /* 3.  A single operator is suggested when an impasse existed previously.
                       In this case, return FALSE so that the impasse can be removed. */

            }
            else
            { /* There is no operator in the slot */
                if (goal.lower_goal != null)
                { /* But there is an impasse */
                    if (goal.lower_goal.isa_impasse)
                        context.getPrinter().warn("This goal is an impasse\n");
                    context.getPrinter().warn("      No Impasse Needed but Impasse exists: remove impasse now\n");
                    context.getPrinter().warn(
                            "\n\n   *************This should never be executed*******************\n\n");
                    return false;
                }
            }

            /* 4.  This is the bottom goal in the stack and there is no operator or
                   impasse for the operator slot created yet.  We shouldn't call this
                   routine in this case (this condition is checked before  
                   decision_consistent_with_current_preferences is called) but, for
                   completeness' sake, we check this condition and return TRUE
                   (because no decision has been made at this level, there is no 
                   need to remove anything). */
            context.getPrinter().warn("\n\n   *************This should never be executed*******************\n\n");
            return true;

        case CONSTRAINT_FAILURE_IMPASSE_TYPE:
            if (DEBUG_CONSISTENCY_CHECK)
                context.getPrinter().print("    Constraint Failure Impasse: Returning TRUE\n");
            return true;

        case CONFLICT_IMPASSE_TYPE:
            if (DEBUG_CONSISTENCY_CHECK)
                context.getPrinter().print("    Conflict Impasse: Returning TRUE\n");
            return true;

        case TIE_IMPASSE_TYPE:
            if (DEBUG_CONSISTENCY_CHECK)
                context.getPrinter().print("    Tie Impasse: Returning TRUE\n");
            return true;

        case NO_CHANGE_IMPASSE_TYPE:
            if (DEBUG_CONSISTENCY_CHECK)
                context.getPrinter().print("    No change Impasse: Returning TRUE\n");
            return true;
        }

        context.getPrinter().warn("\n   After switch................");
        context.getPrinter().warn("\n\n   *************This should never be executed*******************\n\n");
        return true;

    }

    /**
     * consistency.cpp:299:remove_current_decision
     * 
     * @param s
     */
    private void remove_current_decision(Slot s)
    {
        if (s.wmes.isEmpty())
            context.trace.print(Category.TRACE_OPERAND2_REMOVALS_SYSPARAM,
                    "\n       REMOVING CONTEXT SLOT: Slot Identifier [%s] and attribute [%s]\n", s.id, s.attr);

        if (s.id != null)
            context.trace.print(Category.TRACE_OPERAND2_REMOVALS_SYSPARAM,
                    "\n          Decision for goal [%s] is inconsistent.  Replacing it with....\n", s.id);

        /* If there is an operator in the slot, remove it */
        context.decider.remove_wmes_for_context_slot(s);

        /* If there are any subgoals, remove those */
        if (s.id.lower_goal != null)
            context.decider.remove_existing_context_and_descendents(s.id.lower_goal);

        context.decider.do_buffered_wm_and_ownership_changes();
    }

    /**
     * This scans down the goal stack and checks the consistency of the current
     * decision versus the current preferences for the slot, if the preferences
     * have changed.
     * 
     * consistency.cpp:326:check_context_slot_decisions
     * 
     * @param level
     * @return
     */
    private boolean check_context_slot_decisions(int level)
    {
        if (DEBUG_CONSISTENCY_CHECK)
        {
            if (context.tempMemory.highest_goal_whose_context_changed != null)
                context.getPrinter().print("    Highest goal with changed context: [%s]\n",
                        context.tempMemory.highest_goal_whose_context_changed);
        }

        /* Check only those goals where preferences have changes that are at or above the level 
           of the consistency check */
        for (Identifier goal = context.tempMemory.highest_goal_whose_context_changed; goal != null
                && goal.level <= level; goal = goal.lower_goal)
        {
            if (DEBUG_CONSISTENCY_CHECK)
            {
                context.getPrinter().print("    Looking at goal [%s] to see if its preferences have changed\n", goal);
            }

            Slot s = goal.operator_slot;

            if ((goal.lower_goal != null) || (!s.wmes.isEmpty()))
            { /* If we are not at the bottom goal or if there is an operator in the
                         bottom goal's operator slot */
                if (DEBUG_CONSISTENCY_CHECK)
                {
                    context.getPrinter().print("      This is a goal that either has subgoals or, if the bottom goal, has an operator in the slot\n");
                }
                if (s.changed != null)
                { /* Only need to check a goal if its prefs have changed */
                    if (DEBUG_CONSISTENCY_CHECK)
                    {
                        context.getPrinter().print("      This goal's preferences have changed.\n");
                    }
                    if (!decision_consistent_with_current_preferences(goal, s))
                    {
                        if (DEBUG_CONSISTENCY_CHECK)
                        {
                            context.getPrinter().print(
                                            "   The current preferences indicate that the decision at [%s] needs to be removed.\n",
                                            goal);
                        }
                        /* This doesn;t seem like it should be necessary but evidently it is: see 2.008 */
                        remove_current_decision(s);
                        return false; /* No need to continue once a decision is removed */
                    }
                }
            }
            /*
            #ifdef DEBUG_CONSISTENCY_CHECK 
            else {
            printf("   This is a bottom goal with no operator in the slot\n");
            }
            #endif
            */
        }

        return true;
    }
    
    /**
     * consistency.cpp:378:i_activity_at_goal
     * 
     * @param goal
     * @return
     */
    private boolean i_activity_at_goal(Identifier goal)
    {
        /* print_with_symbols("\nLooking for I-activity at goal: %y\n", goal); */

        if (!goal.ms_i_assertions.isEmpty())
            return true;

        if (!goal.ms_retractions.isEmpty())
            return true;

        /* printf("\nNo instantiation found.  Returning FALSE\n");  */
        return false;
    }

    /**
     * This procedure returns TRUE if the current firing type is IE_PRODS and
     * there are no i-assertions (or any retractions) ready to fire in the
     * current GOAL. Else it returns FALSE.
     *
     * consistency.cpp:400:minor_quiescence_at_goal
     * 
     * @param goal
     * @return
     */
    private boolean minor_quiescence_at_goal(Identifier goal)
    {
        if ((context.recMemory.FIRING_TYPE == SavedFiringType.IE_PRODS) && (!i_activity_at_goal(goal)))
            /* firing IEs but no more to fire == minor quiescence */
            return true;
        else
            return false;
    }

    /**
     * Find the highest goal of activity among the current assertions and
     * retractions
     * 
     * We have to start at the top of the goal stack and go down because *any*
     * goal in the goal stack could be active (and we want to highest one).
     * However, we terminate as soon as a goal with assertions or retractions is
     * found. Propose cares only about ms_i_assertions & retractions *
     * 
     * consistency.cpp:420:highest_active_goal_propose
     * 
     * @return
     */
    private Identifier highest_active_goal_propose()
    {
        for (Identifier goal = context.decider.top_goal; goal != null; goal = goal.lower_goal)
        {

            /*
            #ifdef DEBUG_DETERMINE_LEVEL_PHASE      
            print(thisAgent, "In highest_active_goal_propose:\n");
            if (goal->id.ms_i_assertions) print_assertion(goal->id.ms_i_assertions);
            if (goal->id.ms_retractions)  print_retraction(goal->id.ms_retractions); 
            #endif
             */
            /* If there are any active productions at this goal, return the goal */
            if ((!goal.ms_i_assertions.isEmpty()) || (!goal.ms_retractions.isEmpty()))
                return goal;
        }

        /* This routine should only be called when !quiescence.  However, there is
           still the possibility that the only active productions are retractions
           that matched in a NIL goal.  If so, then we just return the bottom goal.
           If not, then all possibilities have been exausted and we have encounted
           an unrecoverable error. */
        /*
        #ifdef DEBUG_DETERMINE_LEVEL_PHASE     
           print(thisAgent, "WARNING: Returning NIL active goal because only NIL goal retractions are active.");
           xml_generate_warning(thisAgent, "WARNING: Returning NIL active goal because only NIL goal retractions are active.");
        #endif
        */
        if (!context.soarReteListener.nil_goal_retractions.isEmpty())
            return null;

        throw new IllegalStateException("Unable to find an active goal when not at quiescence.");
    }

    /**
     * consistency.cpp:457:highest_active_goal_apply
     * 
     * @return
     */
    private Identifier highest_active_goal_apply()
    {
        for (Identifier goal = context.decider.top_goal; goal != null; goal = goal.lower_goal)
        {
            /*
            #if 0 //DEBUG_DETERMINE_LEVEL_PHASE      
                 print(thisAgent, "In highest_active_goal_apply :\n");
                 if (goal->id.ms_i_assertions) print_assertion(goal->id.ms_i_assertions);
                 if (goal->id.ms_o_assertions) print_assertion(goal->id.ms_o_assertions);
                 if (goal->id.ms_retractions)  print_retraction(goal->id.ms_retractions); 
            #endif
            */

            /* If there are any active productions at this goal, return the goal */
            if ((!goal.ms_i_assertions.isEmpty()) || (!goal.ms_o_assertions.isEmpty())
                    || (!goal.ms_retractions.isEmpty()))
                return goal;
        }

        /* This routine should only be called when !quiescence.  However, there is
           still the possibility that the only active productions are retractions
           that matched in a NIL goal.  If so, then we just return the bottom goal.
           If not, then all possibilities have been exausted and we have encounted
           an unrecoverable error. */

        /*
        #ifdef DEBUG_DETERMINE_LEVEL_PHASE     
        print(thisAgent, "WARNING: Returning NIL active goal because only NIL goal retractions are active.");
        xml_generate_warning(thisAgent, "WARNING: Returning NIL active goal because only NIL goal retractions are active.");
        #endif
        */
        if (!context.soarReteListener.nil_goal_retractions.isEmpty())
            return null;

        throw new IllegalStateException("Unable to find an active goal when not at quiescence.");
    }
    
    /**
     * Determines type of productions active at some active level. If IE PRODS
     * are active, this value is returned (regardless of whether there are PEs
     * active or not). Note that this procedure will return erroneous values if
     * there is no activity at the current level. It should only be called when
     * activity at the active_level has been determined.
     * 
     * consistency.cpp::active_production_type_at_goal
     * @param goal
     * @return
     */
    private SavedFiringType active_production_type_at_goal(Identifier goal)
    {
        if (i_activity_at_goal(goal))
            return SavedFiringType.IE_PRODS;
        else
            return SavedFiringType.PE_PRODS;
    }
    

    /**
     * 
     * consistency.cpp:516:goal_stack_consistent_through_goal
     * 
     * @param goal
     * @return
     */
    private boolean goal_stack_consistent_through_goal(Identifier goal)
    {
        // #ifndef NO_TIMING_STUFF
        // #ifdef DETAILED_TIMING_STATS
        // start_timer(thisAgent, &thisAgent->start_gds_tv);
        // #endif
        // #endif

        if (DEBUG_CONSISTENCY_CHECK)
        {
            context.getPrinter().print("\nStart: CONSISTENCY CHECK at level %d\n", goal.level);

            /* Just a bunch of debug stuff for now */
            if (context.tempMemory.highest_goal_whose_context_changed != null)
            {
                context.getPrinter().print("current_agent(highest_goal_whose_context_changed) = [%s]\n",
                        context.tempMemory.highest_goal_whose_context_changed);
            }
            else
            {
                context.getPrinter().print("Evidently, nothing has changed: not checking slots\n");
            }
        }

        boolean test = check_context_slot_decisions(goal.level);

        if (DEBUG_CONSISTENCY_CHECK)
        {
            context.getPrinter().print("\nEnd:   CONSISTENCY CHECK\n");
        }

        //#ifndef NO_TIMING_STUFF
        //#ifdef DETAILED_TIMING_STATS
        //   stop_timer(thisAgent, &thisAgent->start_gds_tv, 
        //      &thisAgent->gds_cpu_time[thisAgent->current_phase]);
        //#endif
        //#endif

        return test;
    }
    
    /**
     * consistency.cpp:559:initialize_consistency_calculations_for_new_decision
     */
    public void initialize_consistency_calculations_for_new_decision()
    {
        /*
        if(DEBUG_DETERMINE_LEVEL_PHASE){
        context.getPrinter().print("\nInitialize consistency calculations for new decision.\n"); 
        }
        */

        /* No current activity level */
        context.decider.active_level = 0;
        context.decider.active_goal = null;

        /* Clear any interruption flags on the goals....*/
        for (Identifier goal = context.decider.top_goal; goal != null; goal = goal.lower_goal)
            goal.saved_firing_type = SavedFiringType.NO_SAVED_PRODS;
    }

    /**
     * This routine is responsible for implementing the DETERMINE_LEVEL_PHASE.
     * In the Waterfall version of Soar, the DETERMINE_LEVEL_PHASE makes the
     * determination of what goal level is active in the stack. Activity
     * proceeds from top goal to bottom goal so the active goal is the goal
     * highest in the stack with productions waiting to fire. This procedure
     * also recognizes quiescence (no productions active anywhere) and
     * mini-quiescence (no more IE_PRODS are waiting to fire in some goal for a
     * goal that fired IE_PRODS in the previous elaboration). Mini-quiescence is
     * followed by a consistency check.
     * 
     * consistency.cpp:590:determine_highest_active_production_level_in_stack_apply
     */
    public void determine_highest_active_production_level_in_stack_apply()
    {
        /*
        #ifdef DEBUG_DETERMINE_LEVEL_PHASE
        printf("\nDetermining the highest active level in the stack....\n"); 
        #endif
        */

        if (!context.soarReteListener.any_assertions_or_retractions_ready())
        {
            /* This is quiescence */
            /*
            #ifdef DEBUG_DETERMINE_LEVEL_PHASE
            printf("\n(Full) APPLY phases Quiescence has been reached...going to output\n");
            #endif
            */

            /* Need to determine if this quiescence is also a minor quiescence,
            otherwise, an inconsistent decision could get retained here (because
            the consistency check was never run). (2.008).  Therefore, if
            in the previous preference phases, IE_PRODS fired, then force a 
            consistency check over the entire stack (by checking at the
            bottom goal). */

            if (minor_quiescence_at_goal(context.decider.bottom_goal))
            {
                goal_stack_consistent_through_goal(context.decider.bottom_goal);
            }

            // TODO why is this here?
            /* regardless of the outcome, we go to the output phases */
            context.decisionCycle.current_phase = Phase.OUTPUT_PHASE;
            return;
        }

        /* Not Quiescence */

        /* Check for Max ELABORATIONS EXCEEDED */

        if (context.decisionCycle.e_cycles_this_d_cycle >= maxElaborations )
        {
            context.getPrinter().warn("\nWarning: reached max-elaborations(%d); proceeding to output phases.", maxElaborations);
            context.decisionCycle.current_phase = Phase.OUTPUT_PHASE;
            return;
        }

        /* Save the old goal and level (must save level explicitly in case goal is NIL) */
        context.decider.previous_active_goal = context.decider.active_goal;
        context.decider.previous_active_level = context.decider.active_level;

        /* Determine the new highest level of activity */
        context.decider.active_goal = highest_active_goal_apply();
        if (context.decider.active_goal != null)
            context.decider.active_level = context.decider.active_goal.level;
        else
            context.decider.active_level = 0; /* Necessary for get_next_retraction */

        /*
        #ifdef DEBUG_DETERMINE_LEVEL_PHASE
        printf("\nHighest level of activity is....%d", thisAgent->active_level); 
        printf("\n   Previous level of activity is....%d", thisAgent->previous_active_level);
        #endif
        */

        LevelChangeType level_change_type;
        if (context.decider.active_goal == null)
            /* Only NIL goal retractions */
            level_change_type = LevelChangeType.NIL_GOAL_RETRACTIONS;
        else if (context.decider.previous_active_level == 0)
            level_change_type = LevelChangeType.NEW_DECISION;
        else
        {
            int diff = context.decider.active_level - context.decider.previous_active_level;
            if (diff == 0)
                level_change_type = LevelChangeType.SAME_LEVEL;
            else if (diff > 0)
                level_change_type = LevelChangeType.LOWER_LEVEL;
            else
                level_change_type = LevelChangeType.HIGHER_LEVEL;
        }

        switch (level_change_type)
        {
        case NIL_GOAL_RETRACTIONS:
            /*
            #ifdef DEBUG_DETERMINE_LEVEL_PHASE
            print(thisAgent, "\nOnly NIL goal retractions are active");
            #endif
            */
            context.recMemory.FIRING_TYPE = SavedFiringType.IE_PRODS;
            // thisAgent->current_phase = PREFERENCE_PHASE;
            break;

        case NEW_DECISION:
            /*
            #ifdef DEBUG_DETERMINE_LEVEL_PHASE
            print(thisAgent, "\nThis is a new decision....");
            #endif
            */
            context.recMemory.FIRING_TYPE = active_production_type_at_goal(context.decider.active_goal);
            /* in APPLY phases, we can test for ONC here, check ms_o_assertions */
            // KJC: thisAgent->current_phase = PREFERENCE_PHASE;
            break;

        case LOWER_LEVEL:
            /*
            #ifdef DEBUG_DETERMINE_LEVEL_PHASE
            print(thisAgent, "\nThe level is lower than the previous level....");  
            #endif
            */
            /* Is there a minor quiescence at the previous level? */
            if (minor_quiescence_at_goal(context.decider.previous_active_goal))
            {
                /*
                #ifdef DEBUG_DETERMINE_LEVEL_PHASE
                printf("\nMinor quiescence at level %d", thisAgent->previous_active_level); 
                #endif
                */
                if (!goal_stack_consistent_through_goal(context.decider.previous_active_goal))
                {
                    context.decisionCycle.current_phase = Phase.OUTPUT_PHASE;
                    break;
                }
            }

            /* else: check if return to interrupted level */

            Identifier goal = context.decider.active_goal;

            /*
            #ifdef DEBUG_DETERMINE_LEVEL_PHASE
            if (goal->id.saved_firing_type == IE_PRODS)
               print(thisAgent, "\nSaved production type: IE _PRODS");
            if (goal->id.saved_firing_type == PE_PRODS)
               print(thisAgent, "\nSaved production type: PE _PRODS");
            if (goal->id.saved_firing_type == NO_SAVED_PRODS)
               print(thisAgent, "\nSaved production type: NONE");
            #endif
            */

            if (goal.saved_firing_type != SavedFiringType.NO_SAVED_PRODS)
            {
                /*
                #ifdef DEBUG_DETERMINE_LEVEL_PHASE
                print(thisAgent, "\nRestoring production type from previous processing at this level"); 
                #endif
                */
                context.recMemory.FIRING_TYPE = goal.saved_firing_type;
                // KJC 04.05 commented the next line after reworking the phases
                // in init_soar.cpp
                // thisAgent->current_phase = DETERMINE_LEVEL_PHASE;
                // Reluctant to make this a recursive call, but somehow we need
                // to go thru
                // and determine which level we should start with now that we've
                // returned from a lower level (solved a subgoal or changed the
                // conditions).
                // We could return a flag instead and test it everytime thru
                // loop in APPLY.
                determine_highest_active_production_level_in_stack_apply();

                break;
            }

            /* else: just do a preference phases */
            context.recMemory.FIRING_TYPE = active_production_type_at_goal(context.decider.active_goal);
            // KJC: thisAgent->current_phase = PREFERENCE_PHASE;
            break;

        case SAME_LEVEL:
            /*
            #ifdef DEBUG_DETERMINE_LEVEL_PHASE
            print(thisAgent, "\nThe level is the same as the previous level...."); 
            #endif
            */
            if (minor_quiescence_at_goal(context.decider.active_goal))
            {
                /*
                #ifdef DEBUG_DETERMINE_LEVEL_PHASE
                printf("\nMinor quiescence at level %d", thisAgent->active_level); 
                #endif
                */
                if (!goal_stack_consistent_through_goal(context.decider.active_goal))
                {
                    context.decisionCycle.current_phase = Phase.OUTPUT_PHASE;
                    break;
                }
            }
            context.recMemory.FIRING_TYPE = active_production_type_at_goal(context.decider.active_goal);
            // thisAgent->current_phase = PREFERENCE_PHASE;
            break;

        case HIGHER_LEVEL:
            /*
            #ifdef DEBUG_DETERMINE_LEVEL_PHASE
            print(thisAgent, "\nThe level is higher than the previous level...."); 
            #endif
            */

            goal = context.decider.previous_active_goal;
            goal.saved_firing_type = context.recMemory.FIRING_TYPE;
            /*
            #ifdef DEBUG_DETERMINE_LEVEL_PHASE       
            if (goal->id.saved_firing_type == IE_PRODS)
               print(thisAgent, "\n Saving current firing type as IE_PRODS");
            else if (goal->id.saved_firing_type == PE_PRODS)
               print(thisAgent, "\n Saving current firing type as PE_PRODS");
            else if (goal->id.saved_firing_type == NO_SAVED_PRODS)
               print(thisAgent, "\n Saving current firing type as NO_SAVED_PRODS");
            else
               print(thisAgent, "\n Unknown SAVED firing type???????");
            #endif
            */

            /* run consistency check at new active level *before* firing any
               productions there */
            /*
            #ifdef DEBUG_DETERMINE_LEVEL_PHASE       
            printf("\nMinor quiescence at level %d", thisAgent->active_level);
            #endif
            */

            if (!goal_stack_consistent_through_goal(context.decider.active_goal))
            {
                context.decisionCycle.current_phase = Phase.OUTPUT_PHASE;
                break;
            }

            /* If the decision is consistent, then just start processing at this level */
            context.recMemory.FIRING_TYPE = active_production_type_at_goal(context.decider.active_goal);
            //thisAgent->current_phase = PREFERENCE_PHASE;
            break;
        }

    }    

   /* determine_highest_active_production_level_in_stack_propose()

   This routine is called from the Propose Phase 
   under the new reordering of the Decision Cycle.
   In the Waterfall version of Soar, the this routine makes the
   determination of what goal level is active in the stack.  Activity
   proceeds from top goal to bottom goal so the active goal is the goal
   highest in the stack with productions waiting to fire.  This procedure
   also recognizes quiescence (no productions active anywhere) and
   mini-quiescence (no more IE_PRODS are waiting to fire in some goal for a
   goal that fired IE_PRODS in the previous elaboration).  Mini-quiescence is
   followed by a consistency check. */

   /* This routine could be further pruned, since with 8.6.0 we have a
      PROPOSE Phase, and don't have to keep toggling IE_PRODS
      KJC  april 2005 */

    /**
     * This routine is called from the Propose Phase under the new reordering of
     * the Decision Cycle. In the Waterfall version of Soar, the this routine
     * makes the determination of what goal level is active in the stack.
     * Activity proceeds from top goal to bottom goal so the active goal is the
     * goal highest in the stack with productions waiting to fire. This
     * procedure also recognizes quiescence (no productions active anywhere) and
     * mini-quiescence (no more IE_PRODS are waiting to fire in some goal for a
     * goal that fired IE_PRODS in the previous elaboration). Mini-quiescence is
     * followed by a consistency check.
     * 
     * This routine could be further pruned, since with 8.6.0 we have a PROPOSE
     * Phase, and don't have to keep toggling IE_PRODS KJC april 2005
     * 
     * consistency.cpp:821:determine_highest_active_production_level_in_stack_propose
     */
    public void determine_highest_active_production_level_in_stack_propose()
    {
        /*
        #ifdef DEBUG_DETERMINE_LEVEL_PHASE
        printf("\n(Propose) Determining the highest active level in the stack....\n"); 
        #endif
        */

        // KJC 01.24.06 Changed logic for testing for IE prods. Was incorrectly
        // checking only the bottom goal. Need to check at all levels. A
        // previous
        // code change required #define, but it was never defined.
        /* We are only checking for i_assertions, not o_assertions, since we don't
         *  want operators to fire in the proposal phases
         */
        if (!(!context.soarReteListener.ms_retractions.isEmpty() || !context.soarReteListener.ms_i_assertions.isEmpty()))
        {
            if (minor_quiescence_at_goal(context.decider.bottom_goal))
            {
                /* This is minor quiescence */
                /*
                #ifdef DEBUG_DETERMINE_LEVEL_PHASE
                printf("\n Propose Phase Quiescence has been reached...going to decision\n");
                #endif
                */

                /* Force a consistency check over the entire stack (by checking at
                the bottom goal). */
                goal_stack_consistent_through_goal(context.decider.bottom_goal);

                /* Decision phases is always next */

                context.decisionCycle.current_phase = Phase.DECISION_PHASE;
                return;
            }
        }

        /* Not Quiescence, there are elaborations ready to fire at some level. */

        /* Check for Max ELABORATIONS EXCEEDED */

        if (context.decisionCycle.e_cycles_this_d_cycle >= maxElaborations)
        {
            context.getPrinter().warn("Warning: reached max-elaborations(%d); proceeding to decision phases.", maxElaborations);
            context.decisionCycle.current_phase = Phase.DECISION_PHASE;
            return;
        }

        /* not Max Elaborations */

        /* Save the old goal and level (must save level explicitly in case
           goal is NIL) */
        context.decider.previous_active_goal = context.decider.active_goal;
        context.decider.previous_active_level = context.decider.active_level;

        /* Determine the new highest level of activity */
        context.decider.active_goal = highest_active_goal_propose();
        if (context.decider.active_goal != null)
            context.decider.active_level = context.decider.active_goal.level;
        else
            context.decider.active_level = 0; /* Necessary for get_next_retraction */
        /*
        #ifdef DEBUG_DETERMINE_LEVEL_PHASE
        printf("\nHighest level of activity is....%d", thisAgent->active_level); 
        printf("\n   Previous level of activity is....%d", thisAgent->previous_active_level);
        #endif
        */

        LevelChangeType level_change_type;
        if (context.decider.active_goal == null)
            /* Only NIL goal retractions */
            level_change_type = LevelChangeType.NIL_GOAL_RETRACTIONS;
        else if (context.decider.previous_active_level == 0)
            level_change_type = LevelChangeType.NEW_DECISION;
        else
        {
            int diff = context.decider.active_level - context.decider.previous_active_level;
            if (diff == 0)
                level_change_type = LevelChangeType.SAME_LEVEL;
            else if (diff > 0)
                level_change_type = LevelChangeType.LOWER_LEVEL;
            else
                level_change_type = LevelChangeType.HIGHER_LEVEL;
        }

        switch (level_change_type)
        {
        case NIL_GOAL_RETRACTIONS:
            /*
            #ifdef DEBUG_DETERMINE_LEVEL_PHASE
            print(thisAgent, "\nOnly NIL goal retractions are active");
            #endif
            */
            context.recMemory.FIRING_TYPE = SavedFiringType.IE_PRODS;
            // thisAgent->current_phase = PREFERENCE_PHASE;
            break;

        case NEW_DECISION:
            /*
            #ifdef DEBUG_DETERMINE_LEVEL_PHASE
            print(thisAgent, "\nThis is a new decision....");
            #endif
            */
            context.recMemory.FIRING_TYPE = SavedFiringType.IE_PRODS;
            // thisAgent->current_phase = PREFERENCE_PHASE;
            break;

        case LOWER_LEVEL:
            /*
            #ifdef DEBUG_DETERMINE_LEVEL_PHASE
            print(thisAgent, "\nThe level is lower than the previous level....");  
            #endif
            */
            /* There is always a minor quiescence at the previous level
               in the propose phases, so check for consistency. */
            if (!goal_stack_consistent_through_goal(context.decider.previous_active_goal))
            {
                context.decisionCycle.current_phase = Phase.DECISION_PHASE;
                break;
            }
            /* else: just do a preference phases */
            context.recMemory.FIRING_TYPE = SavedFiringType.IE_PRODS;
            // thisAgent->current_phase = PREFERENCE_PHASE;
            break;

        case SAME_LEVEL:
            /*
            #ifdef DEBUG_DETERMINE_LEVEL_PHASE
            print(thisAgent, "\nThe level is the same as the previous level...."); 
            #endif
            */
            context.recMemory.FIRING_TYPE = SavedFiringType.IE_PRODS;
            // thisAgent->current_phase = PREFERENCE_PHASE;
            break;

        case HIGHER_LEVEL:
            /*
            #ifdef DEBUG_DETERMINE_LEVEL_PHASE
            print(thisAgent, "\nThe level is higher than the previous level...."); 
            #endif
            */

            Identifier goal = context.decider.previous_active_goal;
            goal.saved_firing_type = context.recMemory.FIRING_TYPE;

            /*
            #ifdef DEBUG_DETERMINE_LEVEL_PHASE       
            if (goal->id.saved_firing_type == IE_PRODS)
               print(thisAgent, "\n Saving current firing type as IE_PRODS");
            else if (goal->id.saved_firing_type == PE_PRODS)
               print(thisAgent, "\n Saving current firing type as PE_PRODS");
            else if (goal->id.saved_firing_type == NO_SAVED_PRODS)
               print(thisAgent, "\n Saving current firing type as NO_SAVED_PRODS");
            else
               print(thisAgent, "\n Unknown SAVED firing type???????");
            #endif
            */

            /* run consistency check at new active level *before* firing any
               productions there */

            /*
            #ifdef DEBUG_DETERMINE_LEVEL_PHASE       
            printf("\nMinor quiescence at level %d", thisAgent->active_level);
            #endif
            */
            if (!goal_stack_consistent_through_goal(context.decider.active_goal))
            {
                context.decisionCycle.current_phase = Phase.DECISION_PHASE;
                break;
            }

            /* If the decision is consistent, then just keep processing
               at this level */

            context.recMemory.FIRING_TYPE = SavedFiringType.IE_PRODS;
            // thisAgent->current_phase = PREFERENCE_PHASE;
            break;
        }
    }
}
