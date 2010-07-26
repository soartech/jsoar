/*
 * Copyright (c) 2010 Dave Ray <daveray@gmail.com>
 *
 * Created on Jul 26, 2010
 */
package org.jsoar.soarunit;

import java.util.HashMap;
import java.util.Map;

/**
 * @author ray
 */
public class FiringCounts
{
    final Map<String, Long> counts = new HashMap<String, Long>();
    
    public void adjust(String key, long amount)
    {
        final Long current = counts.get(key);
        final Long newValue = (current != null ? current : 0L) + amount;
        counts.put(key, newValue);
    }
    
    public void merge(FiringCounts other)
    {
        for(Map.Entry<String, Long> e : other.counts.entrySet())
        {
            adjust(e.getKey(), e.getValue());
        }
    }
}
