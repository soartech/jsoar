/*
 * Copyright (c) 2008  Dave Ray <daveray@gmail.com>
 *
 * Created on Sep 28, 2008
 */
package org.jsoar.kernel.learning;

import java.util.Iterator;
import java.util.LinkedList;

import org.jsoar.kernel.Agent;
import org.jsoar.kernel.PredefinedSymbols;
import org.jsoar.kernel.lhs.BackTraceInfo;
import org.jsoar.kernel.lhs.Condition;
import org.jsoar.kernel.lhs.PositiveCondition;
import org.jsoar.kernel.memory.Instantiation;
import org.jsoar.kernel.memory.Preference;
import org.jsoar.kernel.rete.NotStruct;
import org.jsoar.kernel.symbols.IdentifierImpl;
import org.jsoar.kernel.tracing.Printer;
import org.jsoar.kernel.tracing.Trace;
import org.jsoar.kernel.tracing.Trace.Category;
import org.jsoar.util.ByRef;
import org.jsoar.util.adaptables.Adaptables;
import org.jsoar.util.markers.DefaultMarker;
import org.jsoar.util.markers.Marker;

/**
 * <em>This is an internal interface. Don't use it unless you know what you're doing.</em>
 * 
 * <p>Global level functions and variables for backtracing</p>
 * 
 * <p>Four sets of conditions are maintained during backtracing: locals, grounds,
 * positive potentials, and negateds. Negateds are really potentials, but we
 * keep them separately throughout backtracing, and ground them at the very end.
 * Note that this means during backtracing, the grounds, positive potentials,
 * and locals are all instantiated top-level positive conditions, so they all
 * have a bt.wme_ on them.
 * 
 * <p>In order to avoid backtracing through the same instantiation twice, we mark
 * each instantiation as we BT it, by setting {@code inst->backtrace_number =
 * backtrace_number} (this is a global variable which gets incremented each time
 * we build a chunk).
 * 
 * <p>Locals, grounds, and positive potentials are kept on lists (see the global
 * variables below). These are consed lists of the conditions (that is, the
 * original instantiated conditions). Furthermore, we mark the bt.wme_'s on each
 * condition so we can quickly determine whether a given condition is already in
 * a given set. The "grounds_tc", "potentials_tc", "locals_tc", and
 * "chunker_bt_pref" fields on wme's are used for this. Wmes are marked as "in
 * the grounds" by setting {@code wme->grounds_tc = grounds_tc}. For potentials and
 * locals, we also must set {@code wme->chunker_bt_pref}: if the same wme was tested by
 * two instantiations created at different times--times at which the wme was
 * supported by two different preferences--then we really need to BT through
 * *both* preferences. Marking the wmes with just "locals_tc" or "potentials_tc"
 * alone would prevent the second preference from being BT'd.
 * 
 * <p>The add_to_grounds(), add_to_potentials(), and add_to_locals() macros below
 * are used to add conditions to these sets. The negated conditions are
 * maintained in the chunk_cond_set "negated_set."
 * 
 * <p>As we backtrace, each instantiation that has some Nots is added to the list
 * instantiations_with_nots. We have to go back afterwards and figure out which
 * Nots are between identifiers that ended up in the grounds.
 * 
 * 
 * <p>backtrace.cpp
 * 
 * @author ray
 */
public class Backtracer
{
    private final Agent context;
    private Chunker chunker;
    private PredefinedSymbols predefinedSyms;
    
    /**
     * <p>agent.h:514:backtrace_number
     * <p>Defaults to 0 in create_soar_agent()
     */
    int backtrace_number;
    
    /**
     * <p>agent.h:520:grounds
     */
    final LinkedList<Condition> grounds = new LinkedList<Condition>();
    /**
     * <p>agent.h:521:grounds_tc
     * <p>Defaults to 0 in create_soar_agent()
     */
    int grounds_tc;
    
    /**
     * <p>agent.h:523:locals
     */
    final LinkedList<PositiveCondition> locals = new LinkedList<PositiveCondition>();
    /**
     * <p>agent.h:524:locals_tc
     */
    int locals_tc = 0;
    /**
     * <p>agent.h:525:positive_potentials
     */
    final LinkedList<PositiveCondition> positive_potentials = new LinkedList<PositiveCondition>();
    /**
     * <p>agent.h:526:potentials_tc
     */
    int potentials_tc = 0;
    
    /**
     * @param context
     */
    public Backtracer(Agent context)
    {
        this.context = context;
    }
    
    public void initialize()
    {
        this.chunker = Adaptables.adapt(context, Chunker.class);
        this.predefinedSyms = Adaptables.adapt(context, PredefinedSymbols.class);
    }
    
    /**
     * 
     * <p>backtrace.cpp:106:add_to_grounds
     * 
     * @param cond
     */
    private void add_to_grounds(PositiveCondition cond)
    {
        final BackTraceInfo bt = cond.bt();
        if(bt.wme_.grounds_tc != this.grounds_tc)
        {
            bt.wme_.grounds_tc = this.grounds_tc;
            this.grounds.push(cond);
        }
    }
    
    /**
     * <p>backtrace.cpp:113:add_to_potentials
     * 
     * @param cond
     */
    private void add_to_potentials(PositiveCondition cond)
    {
        final BackTraceInfo bt = cond.bt();
        if(bt.wme_.potentials_tc != this.potentials_tc)
        {
            bt.wme_.potentials_tc = this.potentials_tc;
            bt.wme_.chunker_bt_pref = bt.trace;
            this.positive_potentials.push(cond);
        }
        else if(bt.wme_.chunker_bt_pref != bt.trace)
        {
            this.positive_potentials.push(cond);
        }
    }
    
    /**
     * <p>backtrace.cpp:123:add_to_locals
     * 
     * @param cond
     */
    private void add_to_locals(PositiveCondition cond)
    {
        final BackTraceInfo bt = cond.bt();
        if(bt.wme_.locals_tc != this.locals_tc)
        {
            bt.wme_.locals_tc = this.locals_tc;
            bt.wme_.chunker_bt_pref = bt.trace;
            this.locals.push(cond);
        }
        else if(bt.wme_.chunker_bt_pref != bt.trace)
        {
            this.locals.push(cond);
        }
    }
    
    /**
     * <p>backtrace.cpp:149:print_consed_list_of_conditions
     * 
     * @param list
     * @param indent
     */
    private void print_consed_list_of_conditions(LinkedList<Condition> list, int indent)
    {
        final Printer p = context.getPrinter();
        for(Condition c : list)
        {
            p.spaces(indent).print("%s", c);
        }
    }
    
    /**
     * <p>backtrace.cpp:161:print_consed_list_of_condition_wmes
     * 
     * @param list
     * @param indent
     */
    private void print_consed_list_of_condition_wmes(LinkedList<Condition> list, int indent)
    {
        final Printer p = context.getPrinter();
        for(Condition c : list)
        {
            final PositiveCondition pc = c.asPositiveCondition();
            p.spaces(indent).print("     %s", pc != null ? pc.bt().wme_ : null);
        }
    }
    
    /**
     * This routine BT's through a given instantiation. The general method is as
     * follows:
     * 
     * 1. If we've already BT'd this instantiation, then skip it.
     * 2. Mark the TC (in the instantiated conditions) of all higher goal ids tested in
     * top-level positive conditions
     * 3. Scan through the instantiated conditions; add each one to the appropriate set
     * (locals, positive_potentials, grounds, negated_set).
     * 4. If the instantiation has any Nots, add this instantiation to the list of
     * instantiations_with_nots.
     * 
     * <p>backtrace.cpp:176:backtrace_through_instantiation
     * 
     * @param inst
     * @param grounds_level
     * @param trace_cond
     * @param indent
     */
    void backtrace_through_instantiation(Instantiation inst, int grounds_level, Condition trace_cond, ByRef<Boolean> reliable, int indent)
    {
        final Trace trace = context.getTrace();
        final Printer p = trace.getPrinter();
        final boolean traceBacktracing = trace.isEnabled(Category.BACKTRACING);
        if(traceBacktracing)
        {
            
            p.spaces(indent).print("... BT through instantiation of %s\n",
                    (inst.prod != null ? inst.prod.getName() : "[dummy production]"));
            
        }
        
        // if the instantiation has already been BT'd, don't repeat it
        if(inst.backtrace_number == this.backtrace_number)
        {
            if(traceBacktracing)
            {
                
                p.spaces(indent).print("(We already backtraced through this instantiation.)\n");
            }
            return;
        }
        inst.backtrace_number = this.backtrace_number;
        
        // Record information on the production being backtraced through
        Backtrace temp_explain_backtrace = null;
        if(chunker.explain.isEnabled())
        {
            temp_explain_backtrace = new Backtrace();
            temp_explain_backtrace.trace_cond = trace_cond; /* Not copied yet */
            if(trace_cond == null) /* Backtracing for a result */
            {
                temp_explain_backtrace.result = true;
            }
            else
            {
                temp_explain_backtrace.result = false;
            }
            
            temp_explain_backtrace.grounds = null;
            temp_explain_backtrace.potentials = null;
            temp_explain_backtrace.locals = null;
            temp_explain_backtrace.negated = null;
            
            temp_explain_backtrace.prod_name = inst.prod != null ? inst.prod.getName() : "Dummy production";
            temp_explain_backtrace.next_backtrace = null;
        }
        
        if(!inst.reliable)
        {
            reliable.value = false;
        }
        
        // mark transitive closure of each higher goal id that was tested in
        // the id field of a top-level positive condition
        Marker tc = DefaultMarker.create(); // use this to mark ids in the ground set
        Marker tc2 = DefaultMarker.create(); // use this to mark other ids we see
        boolean need_another_pass = false;
        
        for(Condition c = inst.top_of_instantiated_conditions; c != null; c = c.next)
        {
            final PositiveCondition pc = c.asPositiveCondition();
            if(pc == null)
            {
                continue;
            }
            
            final IdentifierImpl id = pc.id_test.asEqualityTest().getReferent().asIdentifier();
            
            if(id.tc_number == tc)
            {
                // id is already in the TC, so add in the value
                final IdentifierImpl valueId = pc.value_test.asEqualityTest().getReferent().asIdentifier();
                if(valueId != null)
                {
                    // if we already saw it before, we're going to have to go
                    // back and make another pass to get the complete TC
                    if(valueId.tc_number == tc2)
                    {
                        need_another_pass = true;
                    }
                    valueId.tc_number = tc;
                }
            }
            else if((id.isGoal()) && (pc.bt().level <= grounds_level))
            {
                // id is a higher goal id that was tested: so add id to the TC
                id.tc_number = tc;
                final IdentifierImpl valueId = pc.value_test.asEqualityTest().getReferent().asIdentifier();
                if(valueId != null)
                {
                    // if we already saw it before, we're going to have to go
                    // back and make another pass to get the complete TC
                    if(valueId.tc_number == tc2)
                    {
                        need_another_pass = true;
                    }
                    valueId.tc_number = tc;
                }
            }
            else
            {
                // as far as we know so far, id shouldn't be in the tc: so mark
                // it with number "tc2" to indicate that it's been seen already
                id.tc_number = tc2;
            }
        }
        
        // if necessary, make more passes to get the complete TC through the
        // top-level positive conditions (recall that they're all super-simple
        // wme tests--all three fields are equality tests
        while(need_another_pass)
        {
            need_another_pass = false;
            for(Condition c = inst.top_of_instantiated_conditions; c != null; c = c.next)
            {
                PositiveCondition pc = c.asPositiveCondition();
                if(pc == null)
                {
                    continue;
                }
                if(pc.id_test.asEqualityTest().getReferent().asIdentifier().tc_number != tc)
                {
                    continue;
                }
                IdentifierImpl valueId = pc.value_test.asEqualityTest().getReferent().asIdentifier();
                if(valueId != null)
                {
                    if(valueId.tc_number != tc)
                    {
                        valueId.tc_number = tc;
                        need_another_pass = true;
                    }
                }
            }
        }
        
        // scan through conditions, collect grounds, potentials, & locals
        final LinkedList<Condition> grounds_to_print = new LinkedList<Condition>();
        final LinkedList<Condition> pots_to_print = new LinkedList<Condition>();
        final LinkedList<Condition> locals_to_print = new LinkedList<Condition>();
        final LinkedList<Condition> negateds_to_print = new LinkedList<Condition>();
        
        // Record the conds in the print_lists even if not going to be printed
        final boolean traceBacktracingOrExplain = traceBacktracing || chunker.explain.isEnabled();
        for(Condition c = inst.top_of_instantiated_conditions; c != null; c = c.next)
        {
            PositiveCondition pc = c.asPositiveCondition();
            if(pc != null)
            {
                // positive cond's are grounds, potentials, or locals
                if(pc.id_test.asEqualityTest().getReferent().asIdentifier().tc_number == tc)
                {
                    add_to_grounds(pc);
                    if(traceBacktracingOrExplain)
                    {
                        grounds_to_print.push(c);
                    }
                }
                else if(pc.bt().level <= grounds_level)
                {
                    add_to_potentials(pc);
                    if(traceBacktracingOrExplain)
                    {
                        pots_to_print.push(c);
                    }
                }
                else
                {
                    add_to_locals(pc);
                    if(traceBacktracingOrExplain)
                    {
                        locals_to_print.push(c);
                    }
                }
            }
            else
            {
                // negative or nc cond's are either grounds or potentials
                chunker.negated_set.add_to_chunk_cond_set(ChunkCondition.make_chunk_cond_for_condition(c));
                if(traceBacktracingOrExplain)
                {
                    negateds_to_print.push(c);
                }
            }
        }
        
        // add new nots to the not set
        if(inst.nots != null)
        {
            chunker.instantiations_with_nots.push(inst);
        }
        
        /* Now record the sets of conditions. Note that these are not necessarily */
        /* the final resting place for these wmes. In particular potentials may */
        /* move over to become grounds, but since all we really need for explain is */
        /* the list of wmes, this will do as a place to record them. */
        
        if(chunker.explain.isEnabled())
        {
            chunker.explain.explain_add_temp_to_backtrace_list(temp_explain_backtrace, grounds_to_print, pots_to_print,
                    locals_to_print, negateds_to_print);
        }
        
        // if tracing BT, print the resulting conditions, etc.
        if(traceBacktracing)
        {
            p.spaces(indent).print("  -->Grounds:\n");
            print_consed_list_of_condition_wmes(grounds_to_print, indent);
            p.print("\n").spaces(indent).print("\n  -->Potentials:\n");
            print_consed_list_of_condition_wmes(pots_to_print, indent);
            p.print("\n").spaces(indent).print("  -->Locals:\n");
            print_consed_list_of_condition_wmes(locals_to_print, indent);
            p.print("\n").spaces(indent).print("  -->Negated:\n");
            print_consed_list_of_conditions(negateds_to_print, indent);
            p.print("\n").spaces(indent).print("  -->Nots:\n");
            
            for(NotStruct not1 = inst.nots; not1 != null; not1 = not1.next)
            {
                p.print("    %s <> %s\n", not1.s1, not1.s2);
            }
        }
    }
    
    /**
     * This routine backtraces through locals, and keeps doing so until there
     * are no more locals to BT.
     * 
     * <p>backtrace.cpp:443:trace_locals
     * 
     * @param grounds_level
     */
    void trace_locals(int grounds_level, ByRef<Boolean> reliable)
    {
        final Trace trace = context.getTrace();
        final Printer printer = trace.getPrinter();
        final boolean traceBacktracing = trace.isEnabled(Category.BACKTRACING);
        if(traceBacktracing)
        {
            printer.print("\n\n*** Tracing Locals ***\n");
        }
        
        while(!locals.isEmpty())
        {
            final PositiveCondition cond = locals.pop();
            final BackTraceInfo bt = cond.bt();
            if(traceBacktracing)
            {
                printer.print("\nFor local %s ", bt.wme_);
            }
            
            Preference bt_pref = Preference.find_clone_for_level(bt.trace, grounds_level + 1);
            // if it has a trace at this level, backtrace through it
            if(bt_pref != null)
            {
                
                backtrace_through_instantiation(bt_pref.inst, grounds_level, cond, reliable, 0);
                
                // Check for any CDPS prefs and backtrace through them
                if(bt.hasContextDependentPreferences())
                {
                    for(Preference p : bt)
                    {
                        if(traceBacktracing)
                        {
                            printer.print("     Backtracing through CDPS preference: %s", p);
                        }
                        backtrace_through_instantiation(p.inst, grounds_level, cond, reliable, 6);
                    }
                }
                continue;
            }
            
            if(traceBacktracing)
            {
                printer.print("...no trace, can't BT");
            }
            // for augmentations of the local goal id, either handle the
            // "^quiescence t" test or discard it
            if(cond.id_test.asEqualityTest().getReferent().asIdentifier().isGoal())
            {
                if((cond.attr_test.asEqualityTest().getReferent() == predefinedSyms.quiescence_symbol)
                        && (cond.value_test.asEqualityTest().getReferent() == predefinedSyms.t_symbol)
                        && (!cond.test_for_acceptable_preference))
                {
                    reliable.value = false;
                }
                continue;
            }
            
            // otherwise add it to the potential set
            if(traceBacktracing)
            {
                printer.print(" --> make it a potential.");
            }
            add_to_potentials(cond);
            
        }
    }
    
    /**
     * This routine looks for positive potentials that are in the TC of the
     * ground set, and moves them over to the ground set. This process is
     * repeated until no more positive potentials are in the TC of the grounds.
     * 
     * <p>backtrace.cpp:550:trace_grounded_potentials
     */
    void trace_grounded_potentials()
    {
        final Trace trace = context.getTrace();
        final Printer printer = trace.getPrinter();
        final boolean traceBacktracing = trace.isEnabled(Category.BACKTRACING);
        if(traceBacktracing)
        {
            printer.print("\n\n*** Tracing Grounded Potentials ***\n");
        }
        
        // setup the tc of the ground set
        Marker tc = DefaultMarker.create();
        for(Condition c : grounds)
        {
            c.add_cond_to_tc(tc, null, null);
        }
        
        boolean need_another_pass = true;
        while(need_another_pass)
        {
            need_another_pass = false;
            // look for any potentials that are in the tc now
            Iterator<PositiveCondition> it = positive_potentials.iterator();
            while(it.hasNext())
            {
                final PositiveCondition pot = it.next();
                if(pot.cond_is_in_tc(tc))
                {
                    final BackTraceInfo bt = pot.bt();
                    // pot is a grounded potential, move it over to ground set
                    if(traceBacktracing)
                    {
                        printer.print("\n-->Moving to grounds: %s", bt.wme_);
                    }
                    it.remove();
                    if(bt.wme_.grounds_tc != grounds_tc)
                    { /* add pot to grounds */
                        bt.wme_.grounds_tc = grounds_tc;
                        grounds.push(pot);
                        pot.add_cond_to_tc(tc, null, null);
                        need_another_pass = true;
                    }
                    else
                    {
                        // pot was already in the grounds, do don't add it
                        // free_cons (thisAgent, c);
                    }
                }
            }
        }
        
    }
    
    /**
     * This routine backtraces through ungrounded potentials. At entry, all
     * potentials must be ungrounded. This BT's through each potential that has
     * some trace (at the right level) that we can BT through. Other potentials
     * are left alone. TRUE is returned if anything was BT'd; FALSE if nothing
     * changed.
     * 
     * <p>backtrace.cpp:610:trace_ungrounded_potentials
     * 
     */
    boolean trace_ungrounded_potentials(int grounds_level, ByRef<Boolean> reliable)
    {
        final Trace trace = context.getTrace();
        final Printer printer = trace.getPrinter();
        final boolean traceBacktracing = trace.isEnabled(Category.BACKTRACING);
        
        if(traceBacktracing)
        {
            printer.print("\n\n*** Tracing Ungrounded Potentials ***\n");
        }
        
        // scan through positive potentials, pick out the ones that have
        // a preference we can backtrace through
        final LinkedList<PositiveCondition> pots_to_bt = new LinkedList<PositiveCondition>();
        final Iterator<PositiveCondition> it = positive_potentials.iterator();
        while(it.hasNext())
        {
            final PositiveCondition potential = it.next();
            final Preference bt_pref = Preference.find_clone_for_level(potential.bt().trace, grounds_level + 1);
            if(bt_pref != null)
            {
                // Remove potential from positive_potentials and add to
                // pots_to_bt
                it.remove();
                pots_to_bt.push(potential);
            }
        }
        
        // if none to BT, exit
        if(pots_to_bt.isEmpty())
        {
            return false;
        }
        
        // backtrace through each one
        while(!pots_to_bt.isEmpty())
        {
            final PositiveCondition potential = pots_to_bt.pop();
            final BackTraceInfo bt = potential.bt();
            if(traceBacktracing)
            {
                printer.print("\nFor ungrounded potential %s ", bt.wme_);
            }
            Preference bt_pref = Preference.find_clone_for_level(bt.trace, grounds_level + 1);
            
            backtrace_through_instantiation(bt_pref.inst, grounds_level, potential, reliable, 0);
            if(bt.hasContextDependentPreferences())
            {
                for(Preference p : bt)
                {
                    if(traceBacktracing)
                    {
                        printer.print("     Backtracing through CDPS preference: %s", p);
                    }
                    backtrace_through_instantiation(p.inst, grounds_level, potential, reliable, 6);
                }
            }
        }
        
        return true;
    }
    
    /**
     * <p>backtrace.cpp:693:report_local_negation
     * 
     * @param c
     */
    void report_local_negation(Condition c)
    {
        final Trace trace = context.getTrace();
        if(trace.isEnabled(Category.BACKTRACING))
        {
            // use the same code as the backtracing above
            LinkedList<Condition> negated_to_print = new LinkedList<Condition>();
            negated_to_print.push(c);
            
            trace.getPrinter().print("\n*** Chunk won't be formed due to local negation in backtrace ***\n");
            print_consed_list_of_conditions(negated_to_print, 2);
        }
    }
}
