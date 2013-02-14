/*
 * Copyright (c) 2013 Bob Marinier <marinier@gmail.com>
 *
 * Created on Feb 8, 2013
 */

package org.jsoar.kernel.wma;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;

import org.jsoar.kernel.Decider;
import org.jsoar.kernel.DecisionCycle;
import org.jsoar.kernel.lhs.Condition;
import org.jsoar.kernel.lhs.PositiveCondition;
import org.jsoar.kernel.memory.Preference;
import org.jsoar.kernel.memory.PreferenceType;
import org.jsoar.kernel.memory.RecognitionMemory;
import org.jsoar.kernel.memory.Slot;
import org.jsoar.kernel.memory.Wme;
import org.jsoar.kernel.memory.WmeImpl;
import org.jsoar.kernel.memory.WorkingMemory;
import org.jsoar.kernel.rete.Rete;
import org.jsoar.kernel.symbols.IdentifierImpl;
import org.jsoar.kernel.tracing.Trace;
import org.jsoar.kernel.tracing.Trace.Category;
import org.jsoar.kernel.wma.DefaultWorkingMemoryActivationParams.ForgetWmeChoices;
import org.jsoar.kernel.wma.DefaultWorkingMemoryActivationParams.ForgettingChoices;
import org.jsoar.util.adaptables.Adaptable;
import org.jsoar.util.adaptables.Adaptables;
import org.jsoar.util.properties.PropertyChangeEvent;
import org.jsoar.util.properties.PropertyListener;
import org.jsoar.util.properties.PropertyManager;

//////////////////////////////////////////////////////////
//////////////////////////////////////////////////////////
//Bookmark strings to help navigate the code
//////////////////////////////////////////////////////////
//////////////////////////////////////////////////////////

//initialization               wma::init
//
//decay                        wma::decay
//forgetting                   wma::forget
//update                       wma::update
//
//api                          wma::api

/**
 * Default implementation of {@link WorkingMemoryActivation}
 * 
 * <h2>Variances from CSoar Implementation</h2>
 * <p>The wma_tc_number that was added to every wme in CSoar is instead maintained 
 * in a set in the method wma_calculate_initial_boost, which is the only place it
 * is used. If this turns out to be a performance problem, we can revert to the
 * CSoar solution.
 * <p>The wma_decay_element that was added to every wme in CSoar in instead maintained
 * in a map from wme to {@link wma_decay_element} in this class. This structure is never
 * accessed outside of WMA. If this turns out to be a performance problem, we can revert
 * to the CSoar solution.
 * <p>Removed method wma_remove_pref_o_set() because jsoar doesn't need to explicitly
 * clean up memory like that.
 * 
 * <h2>Typedef mappings</h2>
 * <ul>
 * <li>uintptr_t == long
 * <li>intptr_t == long
 * <li>wma_reference == long
 * <li>wma_d_cycle == long
 * <li>wma_decay_set == {@code Set<wma_decay_element>}
 * <li>wma_forget_p_queue == {@code Map<Long, Set<wma_decay_element>}
 * <li>wma_decay_cycle_set == {@code Set<Long>}
 * <li>wma_pooled_wme_set == {@code Set<Wme>}
 * <li>wma_sym_reference_map = {@code Map<SymbolImpl, long>}
 * <li>tc_number = {@code Marker}
 * </ul>
 * @author bob.marinier
 */
public class DefaultWorkingMemoryActivation implements WorkingMemoryActivation
{
    /**
     * How many references are expected per decision (this affects creation of
     * the power/approx cache)
     */
    private static final int WMA_REFERENCES_PER_DECISION = 50;

    /**
     * If an external caller asks for the activation level/value of a WME that
     * is not activated, then this is the value that is returned.
     */
    private static final double WMA_ACTIVATION_NONE = 1.0;

    private static final double WMA_TIME_SUM_NONE = 2.71828182845905;

    /**
     * If no history, this is a low number to report as activation
     */
    private static final double WMA_ACTIVATION_LOW = -1000000000;

    /**
     * If below decay thresh, but not forgotten, forget_cycle =
     */
    private static final long WMA_FORGOTTEN_CYCLE = 0;
   
    private Adaptable context;
    private Trace trace;
    private DecisionCycle decisionCycle;
    private RecognitionMemory recMemory;
    private Rete rete;
    private Decider decider;
    private WorkingMemory workingMemory;
    
    private DefaultWorkingMemoryActivationParams params; /* csoar: wma_params */
    private DefaultWorkingMemoryActivationStats stats; /* csoar: wma_stats */

    // RPM 2/13: timers not ported yet
    // wma_timer_container wma_timers;
    
    private Set<Wme> wma_touched_elements;  
    private TreeMap<Long, Set< wma_decay_element>> wma_forget_pq; // using TreeMap because this needs to be sorted and we will use TreeMap-specific methods
    private Set<Long> wma_touched_sets;
    private Map<Wme, wma_decay_element> wmaDecayElements = new HashMap<Wme, wma_decay_element>();
    
    private int wma_power_size;
    private double wma_power_array[];
    private long wma_approx_array[];
    private double wma_thresh_exp;
    private boolean wma_initialized;
    //private Marker wma_tc_counter; //this only used in one function, eliminated in favor of a local solutionS
    private long wma_d_cycle_count;
    
    public DefaultWorkingMemoryActivation(Adaptable context)
    {
        this.context = context;
    }
    
    public void d_cycle_count_increment()
    {
        wma_d_cycle_count++;
    }
    
    public long get_d_cycle_count()
    {
        return wma_d_cycle_count;
    }
    
    public DefaultWorkingMemoryActivationParams getParams()
    {
        return params;
    }
    
    public DefaultWorkingMemoryActivationStats getStats()
    {
        return stats;
    }
    
    public void initialize()
    {
        this.trace = Adaptables.require(getClass(), context, Trace.class);
        this.decisionCycle = Adaptables.adapt(context, DecisionCycle.class); // not required because only used for printing
        this.recMemory = Adaptables.require(getClass(), context, RecognitionMemory.class);
        this.rete = Adaptables.require(getClass(), context, Rete.class);
        this.decider = Adaptables.require(getClass(), context, Decider.class);
        this.workingMemory = Adaptables.require(getClass(), context, WorkingMemory.class);
        
        final PropertyManager properties = Adaptables.require(DefaultWorkingMemoryActivation.class, context, PropertyManager.class);
        
        params = new DefaultWorkingMemoryActivationParams(properties);
        stats = new DefaultWorkingMemoryActivationStats(properties);
        //wma_timers = new wma_timer_container( );

        wma_forget_pq = new TreeMap<Long, Set<wma_decay_element>>();
        wma_touched_elements = new HashSet<Wme>();
        wma_touched_sets = new HashSet<Long>();

        // call wma_init/wma_deinit when wma is turned on/off
        properties.addListener(DefaultWorkingMemoryActivationParams.ACTIVATION, new PropertyListener<Boolean>()
                {
                    @Override
                    public void propertyChanged(PropertyChangeEvent<Boolean> event)
                    {
                        if ( event.getNewValue() != event.getOldValue() )
                        {
                            if ( event.getNewValue() )
                            {
                                wma_init();
                            }
                            else
                            {
                                wma_deinit();
                            }
                        }
                    }
        });
        
        wma_initialized = false;
    }
    
    /**
     * <p>init_soar.cpp:333:reset_statistics
     */
    public void reset()
    {
        this.wma_d_cycle_count = 0;
    }

    /**
     * wma.cpp:148:wma_enabled
     */
    @Override
    public boolean wma_enabled()
    {
        return params.activation.get();
    }

    //////////////////////////////////////////////////////////
    //////////////////////////////////////////////////////////
    //Initialization Functions (wma::init)
    //////////////////////////////////////////////////////////
    //////////////////////////////////////////////////////////
    
    /**
     * wma.cpp:207:wma_init
     */
    private void wma_init( )
    {
        if ( wma_initialized )
        {
            return;
        }
    
        final double decay_rate = params.decay_rate.get();
        final double decay_thresh = params.decay_thresh.get();
        final long max_pow_cache = params.max_pow_cache.get();
        
        // Pre-compute the integer powers of the decay exponent in order to avoid
        // repeated calls to pow() at runtime
        {
            // determine cache size
            {
                // computes how many powers to compute
                // basic idea: solve for the time that would just fall below the decay threshold, given decay rate and assumption of max references/decision
                // t = e^( ( thresh - ln( max_refs ) ) / -decay_rate )
                final double cache_full = ( Math.exp( ( decay_thresh - Math.log( WMA_REFERENCES_PER_DECISION ) ) / decay_rate ) );
                
                // we bound this by the max-pow-cache parameter to control the space vs. time tradeoff the cache supports
                // max-pow-cache is in MB, so do the conversion:
                // MB * 1024 bytes/KB * 1024 KB/MB
                final double cache_bound = ( ( max_pow_cache * 1024 * 1024 ) / ( /*sizeof( double )*/ 8 ) );
                
                wma_power_size = (int)( Math.ceil( ( cache_full > cache_bound )?( cache_bound ):( cache_full ) ) );
            }
            
            wma_power_array = new double[ wma_power_size ];
            
            wma_power_array[0] = 0.0;
            for( int i = 1; i < wma_power_size; i++ )
            {
                wma_power_array[ i ] = Math.pow( (double)( i ), decay_rate );
            }
        }
        
        // calculate the pre-log'd forgetting threshold, to avoid most
        // calls to log
        wma_thresh_exp = Math.exp( decay_thresh );
        
        // approximation cache
        if( params.forgetting.get() == ForgettingChoices.approx )
        {
            wma_approx_array = new long[ WMA_REFERENCES_PER_DECISION ];
            
            wma_approx_array[0] = 0;
            for ( int i = 1; i < WMA_REFERENCES_PER_DECISION; i++ )
            {
                wma_approx_array[i] = (long)( Math.ceil( Math.exp( decay_thresh - Math.log( (double)(i) ) ) / decay_rate ) );
            }
        }
        
        // note initialization
        wma_initialized = true;
    }
    
    /**
     * wma.cpp:265:wma_deinit
     */
    private void wma_deinit()
    {
        if ( !wma_initialized )
        {
            return;
        }
        
        // release power array memory
        wma_power_array = null;
        
        // release approximation array memory (if applicable)
        if ( params.forgetting.get() == ForgettingChoices.approx )
        {
            wma_approx_array = null;
        }
        
        // clear touched
        wma_touched_elements.clear();
        wma_touched_sets.clear();
        
        // clear forgetting priority queue
        wma_forget_pq.clear();
        
        // jsoar modification: clear the decay elements (otherwise the wmes will never be garbage collected)
        wmaDecayElements.clear();
        
        // RPM 2/2013: should slot.wma_val_references be cleaned up somehow?
        //             Probably not a big deal since those will get cleaned up when the slots are removed
        
        wma_initialized = false;
    }
    
    //////////////////////////////////////////////////////////
    //////////////////////////////////////////////////////////
    //Decay Functions (wma::decay)
    //////////////////////////////////////////////////////////
    //////////////////////////////////////////////////////////
    
    /**
     * wma.cpp:306:wma_history_next
     * @param current
     * @return
     */
    private int wma_history_next( final int current )
    {
        return ( ( current == ( wma_history.WMA_DECAY_HISTORY - 1 ) )?( 0 ):( current + 1 ) );
    }
    
    /**
     * wma.cpp:311:wma_history_prev
     * @param current
     * @return
     */
    private int wma_history_prev( final int current )
    {
        return ( ( current == 0 )?( wma_history.WMA_DECAY_HISTORY - 1 ):( current - 1 ) );
    }
    
    /**
     * wma.cpp:316:wma_should_have_decay_element
     * @param w
     * @return
     */
    private boolean wma_should_have_decay_element( final Wme w )
    {
        final Iterator<Preference> it = w.getPreferences();
        if(!it.hasNext()) return false;
        Preference preference = it.next();
        return ( ( preference.reference_count != 0 ) && ( preference.o_supported ) );
    }
    
    /**
     * wma.cpp:321:wma_pow
     * @param cycle_diff
     * @return
     */
    private double wma_pow( final int cycle_diff )
    {
        if ( cycle_diff < wma_power_size )
        {
            return wma_power_array[ cycle_diff ];
        }
        else
        {
            return Math.pow( (double)cycle_diff, params.decay_rate.get() );
        }
    }

    /**
     * wma.cpp:333:wma_sum_history
     * @param history
     * @param current_cycle
     * @return
     */
    private double wma_sum_history( final wma_history history, final long current_cycle )
    {
        double return_val = 0.0;
        
        int p = history.next_p;
        int counter = history.history_ct;
        long cycle_diff = 0;

        //

        while ( counter != 0 )
        {
            p = wma_history_prev( p );

            cycle_diff = ( current_cycle - history.access_history[ p ].d_cycle );
            assert( cycle_diff > 0 );

            return_val += ( history.access_history[ p ].num_references * wma_pow( (int)cycle_diff ) );
            
            counter--;
        }

        // see (Petrov, 2006)
        if ( params.petrov_approx.get() )
        {
            // if ( n > k )
            if ( history.total_references > history.history_references )
            {
                // ( n - k ) * ( tn^(1-d) - tk^(1-d) )
                // -----------------------------------
                // ( 1 - d ) * ( tn - tk )

                // decay_rate is negated (for nice printing)
                double d_inv = ( 1 + params.decay_rate.get() );
                
                return_val += ( ( ( history.total_references - history.history_references ) * ( Math.pow( (double)( current_cycle - history.first_reference ), d_inv ) - Math.pow( (double)( cycle_diff ), d_inv ) ) ) / 
                                ( d_inv * ( ( current_cycle - history.first_reference ) - cycle_diff ) ) );
            }
        }

        return return_val;
    }

    /**
     * wma.cpp:376:wma_calculate_decay_activation
     * @param decay_el
     * @param current_cycle
     * @param log_result
     * @return
     */
    private double wma_calculate_decay_activation( final wma_decay_element decay_el, final long current_cycle, final boolean log_result )
    {
        wma_history history = decay_el.touches;
            
        if ( history.history_ct != 0 )
        {
            final double history_sum = wma_sum_history( history, current_cycle );

            if ( !log_result )
            {
                return history_sum;
            }

            if ( history_sum > 0.0 )
            {
                return Math.log( history_sum );
            }
            else
            {
                return WMA_ACTIVATION_LOW;
            }
        }
        else
        {
            return ( ( log_result )?( WMA_ACTIVATION_LOW ):( 0.0 ) );
        }
    }
    
    /**
     * wma.cpp:404:wma_calculate_initial_boost
     * @param w
     * @return
     */
    private long wma_calculate_initial_boost( final Wme w )
    {
        long return_val = 0;
        
        // this is a replacement for putting wma_tc_number on every wme
        final Set<Wme> alreadyProcessed = new HashSet<Wme>();

        long num_cond_wmes = 0;
        double combined_time_sum = 0.0;

        for ( Preference pref = w.getPreferences().next().slot.getPreferencesByType(PreferenceType.ACCEPTABLE); pref != null; pref = pref.next )
        {
            if ( ( pref.value == w.getValue() ) && ( pref.o_supported ) )
            {
                for ( Condition cond = pref.inst.top_of_instantiated_conditions; cond != null; cond=cond.next )
                {
                    PositiveCondition pc = cond.asPositiveCondition();
                    if ( ( pc != null ) && ( !alreadyProcessed.contains(pc.bt().wme_) ) )
                    {
                        Wme cond_wme = pc.bt().wme_;
                        alreadyProcessed.add(cond_wme);

                        wma_decay_element wma_decay_el = wmaDecayElements.get(cond_wme);
                        if ( wma_decay_el != null )
                        {
                            if ( !wma_decay_el.just_created )
                            {
                                num_cond_wmes++;
                                combined_time_sum += wma_get_wme_activation( cond_wme, false );
                            }
                        }
                        else if ( cond_wme.getPreferences().hasNext() )
                        {
                            final Preference p = cond_wme.getPreferences().next();
                            if ( p.wma_o_set != null )
                            {
                                for(Wme wme : p.wma_o_set)
                                {
                                    wma_decay_el = wmaDecayElements.get(wme);
                                    if ( !alreadyProcessed.contains(wme) && ( wma_decay_el == null || !wma_decay_el.just_created ) )
                                    {
                                        num_cond_wmes++;
                                        combined_time_sum += wma_get_wme_activation( wme, false );

                                        alreadyProcessed.add(wme);
                                    }
                                }
                            }
                        }
                        else
                        {
                            num_cond_wmes++;
                            combined_time_sum += wma_get_wme_activation(cond_wme, false );
                        }
                    }       
                }
            }
        }

        if ( num_cond_wmes != 0 )
        {
            return_val = (long)( Math.floor( combined_time_sum / num_cond_wmes ) );
        }

        return return_val;
    }
    
    /**
     * wma.cpp:470:wma_activate_wme
     * Several overrides provided here for default arg values
     */
    @Override
    public void wma_activate_wme(final Wme w, final long num_references, final Set<Wme> o_set)
    {
        wma_activate_wme(w, num_references, o_set, false);
    }

    @Override
    public void wma_activate_wme(final Wme w, final long num_references)
    {
        wma_activate_wme(w, num_references, null, false);
        
    }

    @Override
    public void wma_activate_wme(final Wme w)
    {
        wma_activate_wme(w, 1, null, false);
        
    }
    
    @Override
    public void wma_activate_wme( final Wme w, final long num_references, final Set<Wme> o_set, final boolean o_only )
    {   
        // o-supported, non-architectural WME
        if ( wma_should_have_decay_element( w ) )
        {
            wma_decay_element temp_el = wmaDecayElements.get(w);

            // if decay structure doesn't exist, create it
            if ( temp_el == null )
            {
                temp_el = new wma_decay_element();
                
                temp_el.this_wme = w;          
                temp_el.just_removed = false;          
                
                temp_el.just_created = true;
                temp_el.num_references = wma_calculate_initial_boost( w );
                
                temp_el.touches.history_ct = 0;
                temp_el.touches.next_p = 0;

                for ( int i = 0; i < wma_history.WMA_DECAY_HISTORY; i++ )
                {
                    temp_el.touches.access_history[ i ].d_cycle = 0;
                    temp_el.touches.access_history[ i ].num_references = 0;
                }

                temp_el.touches.history_references = 0;
                temp_el.touches.total_references = 0;
                temp_el.touches.first_reference = 0;

                // prevents confusion with delayed forgetting
                temp_el.forget_cycle = -1L;
                
                wmaDecayElements.put(w, temp_el);

                if (trace.isEnabled(Category.WMA))
                {
                    trace.getPrinter().print(
                            "WMA @" + this.decisionCycle.d_cycle_count + ": "
                                    + "add " + w.getTimetag() + " "
                                    + w.getIdentifier() + " "
                                    + w.getAttribute() + " " + w.getValue()
                                    + "\n");
                }
            }

            // add to o_set if necessary
            if ( o_set != null )
            {
                o_set.add( w );
            }
            // otherwise update the decay element
            else
            {
                temp_el.num_references += num_references;
                wma_touched_elements.add( w );
            }
        }
        // i-supported, non-architectural WME
        else if ( !o_only && ( w.getPreferences().hasNext() ) && ( w.getPreferences().next().reference_count != 0 ) )
        {       
            Set<Wme> my_o_set = w.getPreferences().next().wma_o_set;
            
            // if doesn't have an o_set, populate
            if ( my_o_set == null )
            {
                my_o_set = new HashSet<Wme>();
                
                w.getPreferences().next().wma_o_set = my_o_set;

                for ( Condition c = w.getPreferences().next().inst.top_of_instantiated_conditions; c != null; c = c.next )
                {
                    PositiveCondition pc = c.asPositiveCondition();
                    if ( pc != null )
                    {
                        wma_activate_wme( pc.bt().wme_, 0, my_o_set );
                    }
                }
            }   

            // iterate over the o_set
            for ( Wme wme : my_o_set )
            {
                // if populating o_set, add
                if ( o_set != null )
                {
                    o_set.add( wme );
                }
                // otherwise, "activate" the wme if it is
                // non-architectural (avoids dereferencing
                // the wme preference)
                else
                {
                    final wma_decay_element wma_decay_el = wmaDecayElements.get(wme);
                    if ( wma_decay_el != null )
                    {
                        wma_decay_el.num_references += num_references;
                        wma_touched_elements.add( wme );
                    }
                }
            }
        }
        // architectural
        else if ( !o_only && !w.getPreferences().hasNext() )
        {
            // only action is to add it to the o_set
            if ( o_set != null )
            {
                o_set.add( w );
            }
        }
    }
    
    /**
     * wma.cpp:647:wma_deactivate_element
     * @param w
     * @param temp_el : jsoar: adding this param to avoid looking it up again (the caller already looks it up)
     */
    private void wma_deactivate_element( final Wme w, final wma_decay_element temp_el )
    {
        if ( temp_el != null )
        {   
            if ( !temp_el.just_removed )
            {           
                wma_touched_elements.remove( w );

                if ( ( params.forgetting.get() == ForgettingChoices.approx) || ( params.forgetting.get() == ForgettingChoices.bsearch) )
                {
                    wma_forgetting_remove_from_p_queue( temp_el );
                }

                temp_el.just_removed = true;
            }
        }
    }
    
    @Override
    public void wma_remove_decay_element(final Wme w)
    {
        final wma_decay_element temp_el = wmaDecayElements.get(w);
        
        if ( temp_el != null )
        {
            // Deactivate the wme first
            if ( !temp_el.just_removed )
            {
                wma_deactivate_element( w, temp_el );
            }

            // log
            if (trace.isEnabled(Category.WMA))
            {
                trace.getPrinter().print(
                        "WMA @" + decisionCycle.d_cycle_count + ": "
                                + "remove " + w.getTimetag() + "\n");
            }

            wmaDecayElements.remove(temp_el);
        }
    }
    
    //////////////////////////////////////////////////////////
    //////////////////////////////////////////////////////////
    //Forgetting Functions (wma::forget)
    //////////////////////////////////////////////////////////
    //////////////////////////////////////////////////////////
    
    /**
     * wma.cpp:732:wma_forgetting_add_to_p_queue
     * @param decay_el
     * @param new_cycle
     */
    private void wma_forgetting_add_to_p_queue( final wma_decay_element decay_el, final long new_cycle )
    {
        if ( decay_el != null )
        {
            decay_el.forget_cycle = new_cycle;

            final Set<wma_decay_element> pq = wma_forget_pq.get(new_cycle);
            if ( pq == null )
            {
                Set<wma_decay_element> newbie = new HashSet<wma_decay_element>();
                
                newbie.add( decay_el );
                
                wma_forget_pq.put( new_cycle, newbie );
            }
            else
            {
                pq.add( decay_el );
            }
        }
    }
    
    /**
     * wma.cpp:759:wma_forgetting_remove_from_p_queue
     * @param decay_el
     */
    private void wma_forgetting_remove_from_p_queue( final wma_decay_element decay_el )
    {
        if ( decay_el != null )
        {
            // try to find set for the element per cycle        
            final Set<wma_decay_element> pq = wma_forget_pq.get( decay_el.forget_cycle );
            if ( pq != null )
            {
                if ( pq.contains(decay_el) ) 
                {
                    pq.remove( decay_el );

                    if ( pq.isEmpty() )
                    {
                        wma_touched_sets.add( decay_el.forget_cycle );
                    }
                }
            }
        }
    }

    private void wma_forgetting_move_in_p_queue( final wma_decay_element decay_el, final long new_cycle )
    {
        if ( decay_el != null && ( decay_el.forget_cycle != new_cycle ) )
        {
            wma_forgetting_remove_from_p_queue( decay_el );
            wma_forgetting_add_to_p_queue( decay_el, new_cycle );
        }
    }
    
    private long wma_forgetting_estimate_cycle( final wma_decay_element decay_el, final boolean fresh_reference )
    {   
        long return_val = wma_d_cycle_count;
        final ForgettingChoices forgetting = params.forgetting.get();
        
        if ( fresh_reference && ( forgetting == ForgettingChoices.approx ) )
        {
            long to_add = 0;
            
            final wma_history history = decay_el.touches;
            int p = history.next_p;
            int counter = history.history_ct;
            
            while ( counter != 0 )
            {
                p = wma_history_prev( p );

                final long cycle_diff = ( return_val - history.access_history[ p ].d_cycle );

                final int approx_ref = (int)( ( history.access_history[ p ].num_references < WMA_REFERENCES_PER_DECISION )?( history.access_history[ p ].num_references ):( WMA_REFERENCES_PER_DECISION-1 ) );
                if ( wma_approx_array[ approx_ref ] > cycle_diff )
                {
                    to_add += ( wma_approx_array[ approx_ref ] - cycle_diff );
                }
                
                counter--;
            }

            return_val += to_add;
        }
        
        if ( return_val == wma_d_cycle_count )
        {
            final double my_thresh = wma_thresh_exp;
            
            // binary parameter search
            {
                long to_add = 1;
                double act = wma_calculate_decay_activation( decay_el, ( return_val + to_add ), false );

                if ( act >= my_thresh )
                {
                    while ( act >= my_thresh )
                    {
                        to_add *= 2;
                        act = wma_calculate_decay_activation( decay_el, ( return_val + to_add ), false );
                    }

                    //

                    long upper_bound = to_add;
                    long lower_bound, mid;
                    if ( to_add < 4 )
                    {
                        lower_bound = upper_bound;
                    }
                    else
                    {
                        lower_bound = ( to_add / 2 );
                    }

                    while ( lower_bound != upper_bound )
                    {
                        mid = ( ( lower_bound + upper_bound ) / 2 );
                        act = wma_calculate_decay_activation( decay_el, ( return_val + mid ), false );

                        if ( act < my_thresh )
                        {
                            upper_bound = mid;

                            if ( upper_bound - lower_bound <= 1 )
                            {
                                lower_bound = mid;
                            }
                        }
                        else
                        {
                            lower_bound = mid;

                            if ( upper_bound - lower_bound <= 1 )
                            {
                                lower_bound = upper_bound;
                            }
                        }
                    }

                    to_add = upper_bound;
                }

                return_val += to_add;
            }
        }
        
        return return_val;  
    }

    /**
     * wma.cpp:890:wma_forgetting_forget_wme
     * @param w
     * @return
     */
    private boolean wma_forgetting_forget_wme( final Wme w )
    {   
        boolean return_val = false;
        final boolean fake = params.fake_forgetting.get();
        
        if ( w.getPreferences().hasNext() && w.getPreferences().next().slot != null )
        {
            Preference p = w.getPreferences().next().slot.getAllPreferences();
            Preference next_p;

            while ( p != null )
            {
                next_p = p.nextOfSlot;

                if ( p.o_supported && p.isInTempMemory() && ( p.value == w.getValue() ) )
                {
                    if ( !fake )
                    {
                        recMemory.remove_preference_from_tm(p);
                        return_val = true;              
                    }
                }

                p = next_p;
            }
        }

        return return_val;
    }
    
    /**
     * wma.cpp:920:wma_forgetting_update_p_queue
     * @return
     */
    private boolean wma_forgetting_update_p_queue()
    {
        boolean return_val = false;
        boolean do_forget = false;
                
        if ( !wma_forget_pq.isEmpty() )
        {
            final long current_cycle = wma_d_cycle_count;
            final double decay_thresh = wma_thresh_exp;
            final boolean forget_only_lti = ( params.forget_wme.get() == ForgetWmeChoices.lti);

            final Map.Entry<Long, Set<wma_decay_element>> pq = wma_forget_pq.firstEntry();
            if ( pq.getKey() == current_cycle )
            {
                for ( wma_decay_element current : pq.getValue() )
                {
                    if ( wma_calculate_decay_activation( current, current_cycle, false ) < decay_thresh )
                    {
                        current.forget_cycle = WMA_FORGOTTEN_CYCLE;
                        
                        if ( !forget_only_lti || ((IdentifierImpl)current.this_wme.getIdentifier()).smem_lti != 0 )
                        {
                            do_forget = true;

                            // implements all-or-nothing check for lti mode
                            if ( forget_only_lti )
                            {
                                for ( Slot s = ((IdentifierImpl)current.this_wme.getIdentifier()).slots; (s != null && do_forget); s = s.next )
                                {
                                    for ( WmeImpl w = s.getWmes(); (w != null && do_forget); w = w.next )
                                    {
                                        final wma_decay_element wma_decay_el = wmaDecayElements.get(w);
                                        if ( w.preference.o_supported && ( wma_decay_el == null || ( wma_decay_el.forget_cycle != WMA_FORGOTTEN_CYCLE ) ) )
                                        {
                                            do_forget = false;
                                        }
                                    }
                                }
                            }
                            
                            if ( do_forget )
                            {
                                if ( forget_only_lti )
                                {
                                    // implements all-or-nothing forget for lti mode
                                    for ( Slot s= ((IdentifierImpl)current.this_wme.getIdentifier()).slots; (s != null && do_forget); s = s.next )
                                    {
                                        for ( WmeImpl w = s.getWmes(); (w != null && do_forget); w = w.next )
                                        {
                                            if ( wma_forgetting_forget_wme( w ) )
                                            {
                                                return_val = true;
                                            }
                                        }
                                    }
                                }
                                else
                                {
                                    if ( wma_forgetting_forget_wme( current.this_wme ) )
                                    {
                                        return_val = true;
                                    }
                                }
                            }
                        }
                    }
                    else
                    {
                        wma_forgetting_move_in_p_queue( current, wma_forgetting_estimate_cycle( current, false ) );
                    }
                }

                // clean up decay set
                wma_touched_sets.add( pq.getKey() );
                pq.getValue().clear();
            }

            // clean up touched sets
            for(long touched : wma_touched_sets)
            {
                final Set<wma_decay_element> pq_v = wma_forget_pq.get(touched);
                
                if ( pq_v != null && pq_v.isEmpty() )
                {
                    wma_forget_pq.remove( pq.getKey() );
                }
            }
            wma_touched_sets.clear();
        }

        return return_val;
    }
    
    /**
     * wma.cpp:1022:wma_forgetting_naive_sweep
     * @return
     */
    private boolean wma_forgetting_naive_sweep()
    {
        long current_cycle = wma_d_cycle_count;
        double decay_thresh = wma_thresh_exp;
        final boolean forget_only_lti = ( params.forget_wme.get() == ForgetWmeChoices.lti );
        boolean return_val = false;

        for ( Wme w :rete.getAllWmes() )
        {
            final wma_decay_element wma_decay_el = wmaDecayElements.get(w);
            if ( wma_decay_el != null && ( !forget_only_lti || ( ((IdentifierImpl)w.getIdentifier()).smem_lti != 0 ) ) )
            {
                // to be forgotten, wme must...
                // - have been accessed (can't imagine why not, but just in case)
                // - not have been accessed this cycle (i.e. no decay)
                // - have activation less than threshold
                if ( ( wma_decay_el.touches.total_references > 0 ) && 
                     ( wma_decay_el.touches.access_history[ wma_history_prev( wma_decay_el.touches.next_p ) ].d_cycle < current_cycle ) && 
                     ( wma_calculate_decay_activation( wma_decay_el, current_cycle, false ) < decay_thresh ) )
                {
                    if ( wma_forgetting_forget_wme( w ) )
                    {
                        return_val = true;
                    }
                }
            }
        }

        return return_val;
    }
    
    //////////////////////////////////////////////////////////
    //////////////////////////////////////////////////////////
    //Activation Update Functions (wma::update)
    //////////////////////////////////////////////////////////
    //////////////////////////////////////////////////////////
    
    /**
     * wma.cpp:1062:wma_activate_wmes_in_pref
     */
    @Override
    public void wma_activate_wmes_in_pref(final Preference pref)
    {
        if ( pref.type == PreferenceType.ACCEPTABLE )
        {
            WmeImpl w = pref.slot.getWmes();
            while ( w != null )
            {
                // id and attr should already match so just compare the value
                if ( w.getValue() == pref.value )
                {
                    wma_activate_wme( w );
                }

                w = w.next;
            }
        }
    }

    /**
     * wma.cpp:1122:wma_update_decay_histories
     */
    private void wma_update_decay_histories()
    {
        final long current_cycle = wma_d_cycle_count;
        final boolean forgetting = ( ( params.forgetting.get() == ForgettingChoices.approx ) || ( params.forgetting.get() == ForgettingChoices.bsearch ) );

        // add to history for changed elements
        for (Wme w : wma_touched_elements)
        {
            final wma_decay_element temp_el = wmaDecayElements.get(w);

            // update number of references in the current history
            // (has to come before history overwrite)
            temp_el.touches.history_references += ( temp_el.num_references - temp_el.touches.access_history[ temp_el.touches.next_p ].num_references );
            
            // set history
            temp_el.touches.access_history[ temp_el.touches.next_p ].d_cycle = current_cycle;
            temp_el.touches.access_history[ temp_el.touches.next_p ].num_references = temp_el.num_references;

            // log
            if (trace.isEnabled(Category.WMA))
            {
                trace.getPrinter().print(
                        "WMA @" + decisionCycle.d_cycle_count + ": "
                                + "activate " + temp_el.this_wme.getTimetag()
                                + " " + temp_el.num_references + "\n");
            }

            // keep track of first reference
            if ( temp_el.touches.total_references == 0 )
            {
                temp_el.touches.first_reference = current_cycle;
            }
            
            // update counters
            if ( temp_el.touches.history_ct < wma_history.WMA_DECAY_HISTORY )
            {
                temp_el.touches.history_ct++;
            }
            temp_el.touches.next_p = wma_history_next( temp_el.touches.next_p );
            temp_el.touches.total_references += temp_el.num_references;

            // reset cycle counter
            temp_el.num_references = 0;

            // update forgetting stuff as needed
            if ( forgetting )
            {
                if ( temp_el.just_created )
                {
                    wma_forgetting_add_to_p_queue( temp_el, wma_forgetting_estimate_cycle( temp_el, true ) );
                }
                else
                {
                    wma_forgetting_move_in_p_queue( temp_el, wma_forgetting_estimate_cycle( temp_el, true ) );
                }
            }

            temp_el.just_created = false;
        }
        wma_touched_elements.clear();
    }
    
    //////////////////////////////////////////////////////////
    //////////////////////////////////////////////////////////
    //API Functions (wma::api)
    //////////////////////////////////////////////////////////
    //////////////////////////////////////////////////////////
    
    /**
     * wma.cpp:1212:wma_get_wme_activation
     */
    @Override
    public double wma_get_wme_activation(final Wme w, final boolean log_result)
    {
        double return_val = ( log_result )?( WMA_ACTIVATION_NONE ):( WMA_TIME_SUM_NONE );

        final wma_decay_element wma_decay_el = wmaDecayElements.get(w);
        if ( wma_decay_el != null )
        {
            return_val = wma_calculate_decay_activation( wma_decay_el, wma_d_cycle_count, log_result );
        }

        return return_val;
    }
    
    @Override
    public String wma_get_wme_history(final Wme w)
    {
        String ret = "";
        final wma_decay_element wma_decay_el = wmaDecayElements.get(w);
        if ( wma_decay_el != null )
        {
            final wma_history history = wma_decay_el.touches;
            final long current_cycle = wma_d_cycle_count;

            ret += "history (" + history.history_references + "/" + history.total_references + ", first @ d" + history.first_reference + "):";

            int p = history.next_p;
            int counter = history.history_ct;
            while ( counter != 0 )
            {
                p = wma_history_prev( p );
                counter--;

                ret += "\n"
                    +  history.access_history[ p ].toString(current_cycle);
            }

            final ForgettingChoices forget = params.forgetting.get();

            if ( ( forget == ForgettingChoices.bsearch ) || ( forget == ForgettingChoices.approx ) )
            {
                ret += "\n\n"
                    +  "considering WME for decay @ d"
                    +  wma_decay_el.forget_cycle;
            }
        }
        else
        {
            ret = "WME has no decay history";
        }
        
        return ret;
    }
    
    /**
     * wma.cpp:1308:wma_go
     */
    @Override
    public void wma_go(final wma_go_action go_action)
    {
     // update history for all touched elements
        if ( go_action == wma_go_action.wma_histories )
        {
            //my_agent.wma_timers.history.start();
            
            wma_update_decay_histories();

            //my_agent.wma_timers.history.stop();
        }
        // check forgetting queue
        else if ( go_action == wma_go_action.wma_forgetting )
        {
            final ForgettingChoices forgetting = params.forgetting.get();

            if ( forgetting != ForgettingChoices.off )
            {
                //my_agent.wma_timers.forgetting.start();

                boolean forgot_something = false;

                if ( forgetting == ForgettingChoices.naive )
                {
                    forgot_something = wma_forgetting_naive_sweep();
                }
                else
                {           
                    forgot_something = wma_forgetting_update_p_queue();
                }

                if ( forgot_something )
                {
                    if ( trace.isEnabled(Category.WM_CHANGES) )
                    {
                        trace.getPrinter().print("\n\nWMA: BEGIN FORGOTTEN WME LIST\n\n");
                    }

                    long wm_removal_diff = workingMemory.getWmeRemovalCount();
                    {
                        decider.do_working_memory_phase();
                    }
                    wm_removal_diff = ( workingMemory.getWmeRemovalCount() - wm_removal_diff );

                    if ( wm_removal_diff > 0 )
                    {
                        stats.forgotten_wmes.set( stats.forgotten_wmes.get() + wm_removal_diff );
                    }

                    if ( trace.isEnabled(Category.WM_CHANGES) )
                    {
                        trace.getPrinter().print("\nWMA: END FORGOTTEN WME LIST\n\n");
                    }
                }

                //my_agent.wma_timers.forgetting.stop();
            }
        }
        
    }

}
