/*
 * Copyright (c) 2010 Dave Ray <daveray@gmail.com>
 *
 * Created on Jul 26, 2010
 */
package org.jsoar.soarunit;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

/**
 * @author ray
 */
public class FiringCounts
{
    final Map<String, Long> counts = new TreeMap<>();
    
    public double getCoverage()
    {
        int uncovered = countUncoveredRules();
        return counts.size() > 0 ? 1.0 - ((double) uncovered / (double) counts.size()) : 0.0;
    }
    
    public int countUncoveredRules()
    {
        int count = 0;
        for(Map.Entry<String, Long> e : getEntries())
        {
            if(e.getValue().longValue() == 0L)
            {
                count++;
            }
        }
        return count;
    }
    
    public List<String> getUncoveredRules()
    {
        final List<String> result = new ArrayList<>();
        for(Map.Entry<String, Long> e : getEntries())
        {
            if(e.getValue().longValue() == 0L)
            {
                result.add(e.getKey());
            }
        }
        return result;
    }
    
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
    
    public Iterable<Map.Entry<String, Long>> getEntries()
    {
        return counts.entrySet();
    }
    
    public Long getCount(String ruleName)
    {
        return counts.get(ruleName);
    }
}
