/*
 * Copyright (c) 2012 Dave Ray <daveray@gmail.com>
 *
 * Created on Mar 20, 2012
 */
package org.jsoar.kernel.epmem;

import org.jsoar.util.properties.PropertyManager;

/**
 * @author voigtjr
 */
class DefaultEpisodicMemoryStats implements EpisodicMemoryStatistics
{
    private final PropertyManager properties;

    public DefaultEpisodicMemoryStats(PropertyManager properties)
    {
        this.properties = properties;
    }

}
