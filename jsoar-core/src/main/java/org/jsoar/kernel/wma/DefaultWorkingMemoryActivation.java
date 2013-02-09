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

import org.jsoar.kernel.memory.Preference;
import org.jsoar.kernel.memory.Wme;
import org.jsoar.kernel.smem.DefaultSemanticMemory;
import org.jsoar.util.adaptables.Adaptable;
import org.jsoar.util.adaptables.Adaptables;
import org.jsoar.util.markers.DefaultMarker;
import org.jsoar.util.markers.Marker;
import org.jsoar.util.properties.PropertyManager;

/**
 * Default implementation of {@link WorkingMemoryActivation}
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
    
    DefaultWorkingMemoryActivationParams wma_params;
    DefaultWorkingMemoryActivationStats wma_stats;

    // RPM 2/13: timers not ported yet
    // wma_timer_container wma_timers;
    
    Set<Wme> wma_touched_elements;  
    Map<Long, Set< wma_decay_element>> wma_forget_pq;
    Set<Long> wma_touched_sets;

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
    
    void wma_init( )
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
    
    void wma_deinit()
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
    
    int wma_history_next( int current )
    {
        return ( ( current == ( wma_history.WMA_DECAY_HISTORY - 1 ) )?( 0 ):( current + 1 ) );
    }
    
    int wma_history_prev( int current )
    {
        return ( ( current == 0 )?( wma_history.WMA_DECAY_HISTORY - 1 ):( current - 1 ) );
    }
    
    boolean wma_should_have_decay_element( Wme w )
    {
        Iterator<Preference> it = w.getPreferences();
        if(!it.hasNext()) return false;
        Preference preference = it.next();
        return ( ( preference.reference_count != 0 ) && ( preference.o_supported ) );
    }
    
    double wma_pow( int cycle_diff )
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

    
    
    
    @Override
    public void wma_activate_wme(Wme w, long num_references, Set<Wme> o_set,
            boolean o_only)
    {
        // TODO Auto-generated method stub
        
    }

    @Override
    public void wma_activate_wme(Wme w, long num_references, Set<Wme> o_set)
    {
        // TODO Auto-generated method stub
        
    }

    @Override
    public void wma_activate_wme(Wme w, long num_references)
    {
        // TODO Auto-generated method stub
        
    }

    @Override
    public void wma_activate_wme(Wme w)
    {
        // TODO Auto-generated method stub
        
    }

    @Override
    public void wma_remove_decay_element(Wme w)
    {
        // TODO Auto-generated method stub
        
    }

    @Override
    public void wma_remove_pref_o_set(Preference pref)
    {
        // TODO Auto-generated method stub
        
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
