/*
 * Copyright (c) 2013 Bob Marinier <marinier@gmail.com>
 *
 * Created on Feb 8, 2013
 */
package org.jsoar.kernel.wma;

import java.util.Set;

import org.jsoar.kernel.memory.Preference;
import org.jsoar.kernel.memory.Wme;

public interface WorkingMemoryActivation
{
    //////////////////////////////////////////////////////////
    // Parameter Functions
    //////////////////////////////////////////////////////////
    
    /**
     * shortcut for determining if WMA is enabled
     * wma.h:wma_enabled
     */
    boolean wma_enabled();
    
    //////////////////////////////////////////////////////////
    // Add/Remove Decay Element/Set
    //////////////////////////////////////////////////////////
    
    /**
     * generic call to activate a wme
     * wma.h:wma_activate_wme
     * 
     * @param w
     * @param num_references
     * @param o_set
     * @param o_only
     */
    void wma_activate_wme(Wme w, long num_references /* = 1 */, Set<Wme> o_set /* = null */, boolean o_only /* = false */ );
    
    void wma_activate_wme(Wme w, long num_references /* = 1 */, Set<Wme> o_set /* = null */ );
    
    void wma_activate_wme(Wme w, long num_references /* = 1 */ );
    
    void wma_activate_wme(Wme w);
    
    /**
     * Removes a decay element from an existing WME so that it is no longer activated.
     * wma.h:wma_remove_decay_element
     * 
     * @param w
     */
    void wma_remove_decay_element(Wme w);
    
    //////////////////////////////////////////////////////////
    // Updating Activation
    //////////////////////////////////////////////////////////
    
    /**
     * Given a preference, this routine increments the
     * reference count of all its WMEs (as necessary).
     * wma.h:wma_activate_wmes_in_pref
     */
    void wma_activate_wmes_in_pref(Preference pref);
    
    /**
     * This routine performs WME activation
     * and forgetting at the end of each cycle.
     * wma.h:wma_go
     */
    void wma_go(wma_go_action go_action);
    
    //////////////////////////////////////////////////////////
    // Retrieving Activation
    //////////////////////////////////////////////////////////
    
    /**
     * Retrieve wme activation exact/approximate
     * wma.h:wma_get_wme_activation
     */
    double wma_get_wme_activation(Wme w, boolean log_result);
    
    /**
     * Debugging: get list of wme references
     * wma.h:wma_get_wme_history
     */
    String wma_get_wme_history(Wme w);
}
