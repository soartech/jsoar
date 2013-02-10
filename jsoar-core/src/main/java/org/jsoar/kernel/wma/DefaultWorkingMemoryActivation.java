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

import org.jsoar.kernel.DecisionCycle;
import org.jsoar.kernel.lhs.Condition;
import org.jsoar.kernel.lhs.PositiveCondition;
import org.jsoar.kernel.memory.Preference;
import org.jsoar.kernel.memory.PreferenceType;
import org.jsoar.kernel.memory.Wme;
import org.jsoar.kernel.smem.DefaultSemanticMemory;
import org.jsoar.kernel.tracing.Trace;
import org.jsoar.kernel.tracing.Trace.Category;
import org.jsoar.util.adaptables.Adaptable;
import org.jsoar.util.adaptables.Adaptables;
import org.jsoar.util.markers.DefaultMarker;
import org.jsoar.util.markers.Marker;
import org.jsoar.util.properties.PropertyManager;

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
    public static final int WMA_REFERENCES_PER_DECISION = 50;

    /**
     * If an external caller asks for the activation level/value of a WME that
     * is not activated, then this is the value that is returned.
     */
    public static final double WMA_ACTIVATION_NONE = 1.0;

    public static final double WMA_TIME_SUM_NONE = 2.71828182845905;

    /**
     * If no history, this is a low number to report as activation
     */
    public static final double WMA_ACTIVATION_LOW = -1000000000;

    /**
     * If below decay thresh, but not forgotten, forget_cycle =
     */
    public static final double WMA_FORGOTTEN_CYCLE = 0;
   
    Adaptable context;
    Trace trace;
    DecisionCycle decisionCycle;
    
    DefaultWorkingMemoryActivationParams wma_params;
    DefaultWorkingMemoryActivationStats wma_stats;

    // RPM 2/13: timers not ported yet
    // wma_timer_container wma_timers;
    
    Set<Wme> wma_touched_elements;  
    Map<Long, Set< wma_decay_element>> wma_forget_pq;
    Set<Long> wma_touched_sets;
    Map<Wme, wma_decay_element> wmaDecayElements = new HashMap<Wme, wma_decay_element>();
    
    int wma_power_size;
    double wma_power_array[];
    long wma_approx_array[];
    double wma_thresh_exp;
    boolean wma_initialized;
    Marker wma_tc_counter;
    long wma_d_cycle_count;
    
    public DefaultWorkingMemoryActivation(Adaptable context)
    {
        this.context = context;
        this.trace = Adaptables.require(getClass(), context, Trace.class);
        this.decisionCycle = Adaptables.adapt(context, DecisionCycle.class);
    }
    
    public void initialize()
    {
        final PropertyManager properties = Adaptables.require(DefaultSemanticMemory.class, context, PropertyManager.class);
        wma_params = new DefaultWorkingMemoryActivationParams(properties);
        wma_stats = new DefaultWorkingMemoryActivationStats(properties);
        //wma_timers = new wma_timer_container( );

        wma_forget_pq = new HashMap<Long, Set<wma_decay_element>>();
        wma_touched_elements = new HashSet<Wme>();
        wma_touched_sets = new HashSet<Long>();

        wma_initialized = false;
        wma_tc_counter = DefaultMarker.create();
    }

    /**
     * wma.cpp:148:wma_enabled
     */
    @Override
    public boolean wma_enabled()
    {
        return wma_params.activation.get();
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
    
        double decay_rate = wma_params.decay_rate.get();
        double decay_thresh = wma_params.decay_thresh.get();
        long max_pow_cache = wma_params.max_pow_cache.get();
        
        // Pre-compute the integer powers of the decay exponent in order to avoid
        // repeated calls to pow() at runtime
        {
            // determine cache size
            {
                // computes how many powers to compute
                // basic idea: solve for the time that would just fall below the decay threshold, given decay rate and assumption of max references/decision
                // t = e^( ( thresh - ln( max_refs ) ) / -decay_rate )
                double cache_full = ( Math.exp( ( decay_thresh - Math.log( WMA_REFERENCES_PER_DECISION ) ) / decay_rate ) );
                
                // we bound this by the max-pow-cache parameter to control the space vs. time tradeoff the cache supports
                // max-pow-cache is in MB, so do the conversion:
                // MB * 1024 bytes/KB * 1024 KB/MB
                double cache_bound = ( ( max_pow_cache * 1024 * 1024 ) / ( /*sizeof( double )*/ 8 ) );
                
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
        if( wma_params.forgetting.get() == DefaultWorkingMemoryActivationParams.ForgettingChoices.approx )
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
        if ( wma_params.forgetting.get() == DefaultWorkingMemoryActivationParams.ForgettingChoices.approx )
        {
            wma_approx_array = null;
        }
        
        // clear touched
        wma_touched_elements.clear();
        wma_touched_sets.clear();
        
        // clear forgetting priority queue
        wma_forget_pq.clear();
        
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
    private int wma_history_next( int current )
    {
        return ( ( current == ( wma_history.WMA_DECAY_HISTORY - 1 ) )?( 0 ):( current + 1 ) );
    }
    
    /**
     * wma.cpp:311:wma_history_prev
     * @param current
     * @return
     */
    private int wma_history_prev( int current )
    {
        return ( ( current == 0 )?( wma_history.WMA_DECAY_HISTORY - 1 ):( current - 1 ) );
    }
    
    /**
     * wma.cpp:316:wma_should_have_decay_element
     * @param w
     * @return
     */
    private boolean wma_should_have_decay_element( Wme w )
    {
        Iterator<Preference> it = w.getPreferences();
        if(!it.hasNext()) return false;
        Preference preference = it.next();
        return ( ( preference.reference_count != 0 ) && ( preference.o_supported ) );
    }
    
    /**
     * wma.cpp:321:wma_pow
     * @param cycle_diff
     * @return
     */
    private double wma_pow( int cycle_diff )
    {
        if ( cycle_diff < wma_power_size )
        {
            return wma_power_array[ cycle_diff ];
        }
        else
        {
            return Math.pow( (double)cycle_diff, wma_params.decay_rate.get() );
        }
    }

    /**
     * wma.cpp:333:wma_sum_history
     * @param history
     * @param current_cycle
     * @return
     */
    private double wma_sum_history( wma_history history, long current_cycle )
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
        if ( wma_params.petrov_approx.get() )
        {
            // if ( n > k )
            if ( history.total_references > history.history_references )
            {
                // ( n - k ) * ( tn^(1-d) - tk^(1-d) )
                // -----------------------------------
                // ( 1 - d ) * ( tn - tk )

                // decay_rate is negated (for nice printing)
                double d_inv = ( 1 + wma_params.decay_rate.get() );
                
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
    private double wma_calculate_decay_activation( wma_decay_element decay_el, long current_cycle, boolean log_result )
    {
        wma_history history = decay_el.touches;
            
        if ( history.history_ct != 0 )
        {
            double history_sum = wma_sum_history( history, current_cycle );

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
    private long wma_calculate_initial_boost( Wme w )
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
    public void wma_activate_wme(Wme w, long num_references, Set<Wme> o_set)
    {
        wma_activate_wme(w, num_references, o_set, false);
    }

    @Override
    public void wma_activate_wme(Wme w, long num_references)
    {
        wma_activate_wme(w, num_references, null, false);
        
    }

    @Override
    public void wma_activate_wme(Wme w)
    {
        wma_activate_wme(w, 1, null, false);
        
    }
    
    @Override
    public void wma_activate_wme( Wme w, long num_references, Set<Wme> o_set, boolean o_only )
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

                if ( trace.isEnabled(Category.WMA) )
                {
                    String msg = "WMA @";
                    msg += this.decisionCycle.d_cycle_count;
                    
                    msg += ": ";
                    
                    msg += "add ";

                    msg += w.getTimetag();
                    msg += " ";

                    msg += w.getIdentifier();
                    
                    msg += " ";

                    msg += w.getAttribute();
                    
                    msg += " ";

                    msg += w.getValue();
                    
                    msg += "\n";

                    trace.getPrinter().print(msg);
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
            if ( my_o_set != null )
            {
                my_o_set = new HashSet<Wme>();
                
                w.getPreferences().next().wma_o_set = my_o_set;

                for ( Condition c = w.getPreferences().next().inst.top_of_instantiated_conditions; c != null; c = c.next )
                {
                    PositiveCondition pc = c.asPositiveCondition();
                    if ( c != null )
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
     */
    private void wma_deactivate_element( Wme w )
    {
        wma_decay_element temp_el = wmaDecayElements.get(w);

        if ( temp_el != null )
        {   
            if ( !temp_el.just_removed )
            {           
                wma_touched_elements.remove( w );

                if ( ( wma_params.forgetting.get() == DefaultWorkingMemoryActivationParams.ForgettingChoices.approx) || ( wma_params.forgetting.get() == DefaultWorkingMemoryActivationParams.ForgettingChoices.bsearch) )
                {
                    wma_forgetting_remove_from_p_queue( temp_el );
                }

                temp_el.just_removed = true;
            }
        }
    }
    
    @Override
    public void wma_remove_decay_element(Wme w)
    {
        wma_decay_element temp_el = wmaDecayElements.get(w);
        
        if ( temp_el != null )
        {
            // Deactivate the wme first
            if ( !temp_el.just_removed )
            {
                wma_deactivate_element( w );
            }

            // log
            if ( trace.isEnabled(Category.WMA) )
            {
                String msg = "WMA @";
                
                msg += decisionCycle.d_cycle_count;
                msg += ": ";
                
                msg += "remove ";

                msg += w.getTimetag();

                msg += "\n";

                trace.getPrinter().print(msg);
            }

            wmaDecayElements.remove(temp_el);
        }
    }
    
    /**
     * wma.cpp:705:wma_remove_pref_o_set
     */
    @Override
    public void wma_remove_pref_o_set(Preference pref)
    {
        if ( pref != null && pref.wma_o_set != null)
        {
            pref.wma_o_set = null;
        }
    }
    
    //////////////////////////////////////////////////////////
    //////////////////////////////////////////////////////////
    //Forgetting Functions (wma::forget)
    //////////////////////////////////////////////////////////
    //////////////////////////////////////////////////////////
    
    /**
     * wma.cpp:759:wma_forgetting_remove_from_p_queue
     * @param decay_el
     */
    void wma_forgetting_remove_from_p_queue( wma_decay_element decay_el )
    {
        if ( decay_el != null )
        {
            // try to find set for the element per cycle        
            Set<wma_decay_element> pq = wma_forget_pq.get( decay_el.forget_cycle );
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

    @Override
    public void wma_activate_wmes_in_pref(Preference pref)
    {
        // TODO Auto-generated method stub
        
    }

    @Override
    public void wma_activate_wmes_tested_in_prods()
    {
        // TODO Auto-generated method stub
        
    }

    @Override
    public void wma_go(wma_go_action go_action)
    {
        // TODO Auto-generated method stub
        
    }

    @Override
    public double wma_get_wme_activation(Wme w, boolean log_result)
    {
        // TODO Auto-generated method stub
        return 0;
    }

    @Override
    public String wma_get_wme_history(Wme w)
    {
        // TODO Auto-generated method stub
        return null;
    }

}
