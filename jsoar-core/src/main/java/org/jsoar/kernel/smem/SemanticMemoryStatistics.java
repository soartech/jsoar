/*
 * Copyright (c) 2012 Dave Ray <daveray@gmail.com>
 *
 * Created on Mar 3, 2012
 */
package org.jsoar.kernel.smem;

/**
 * Statistics information for SMem.
 * 
 * @author voigtjr
 */
public interface SemanticMemoryStatistics 
{

    /**
     * @return Number of times the <code>retrieve</code> command has been issued.
     */
    long getRetrieves();

    /**
     * @return Number of times the <code>query</code> command has been issued.
     */
    long getQueries();

    /**
     * @return Number of times the <code>store</code> command has been issued.
     */
    long getStores();

}
