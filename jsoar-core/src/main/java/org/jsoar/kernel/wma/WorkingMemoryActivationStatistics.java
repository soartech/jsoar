/*
 * Copyright (c) 2013 Bob Marinier <marinier@gmail.com>
 *
 * Created on Feb 9, 2013
 */
package org.jsoar.kernel.wma;

/**
 * Statistics information for wma.
 * 
 * @author bob.marinier
 */
public interface WorkingMemoryActivationStatistics 
{

    /**
     * @return Number of wmes that have been forgotten.
     */
    long getForgottenWmes();

}
