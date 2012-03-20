/*
 * Copyright (c) 2012 Dave Ray <daveray@gmail.com>
 *
 * Created on Mar 20, 2012
 */
package org.jsoar.kernel.epmem;

import org.jsoar.util.adaptables.Adaptable;
import org.jsoar.util.adaptables.Adaptables;
import org.jsoar.util.properties.PropertyManager;

/**
 * @author voigtjr
 */
public class DefaultEpisodicMemory implements EpisodicMemory
{
    private Adaptable context;
    private DefaultEpisodicMemoryParams params;
    private DefaultEpisodicMemoryStats stats;

    private EpisodicMemoryDatabase db;

    public DefaultEpisodicMemory(Adaptable context)
    {
        this(context, null);
    }
    
    public DefaultEpisodicMemory(Adaptable context, EpisodicMemoryDatabase db)
    {
        this.context = context;
        this.db = db;
    }
    
    public void initialize()
    {
        final PropertyManager properties = Adaptables.require(DefaultEpisodicMemory.class, context, PropertyManager.class);
        params = new DefaultEpisodicMemoryParams(properties);
        stats = new DefaultEpisodicMemoryStats(properties);
    }

}
