/*
 * Copyright (c) 2013 Bob Marinier <marinier@gmail.com>
 *
 * Created on Feb 8, 2013
 */

package org.jsoar.kernel.wma;

public class wma_cycle_reference
{
    long num_references;
    long d_cycle;
    
    /**
     * wma.cpp:1224:_wma_ref_to_str
     * @param current_cycle
     * @return
     */
    String toString(long current_cycle )
    {
        final long cycle_diff = ( current_cycle - d_cycle );
        return num_references + " @ d" + d_cycle + " (-" + cycle_diff + ")";
    }
}
