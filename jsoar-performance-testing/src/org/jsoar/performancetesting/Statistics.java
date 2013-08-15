package org.jsoar.performancetesting;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;

public class Statistics
{
    public static Double calculateAverage(Collection<Double> c)
    {
        Double sum = 0.0;
        
        if (!c.isEmpty())
        {
            for (Double d : c)
            {
                sum += d;
            }
            
            return sum / (double) c.size();
        }
        
        return sum;
    }
    
    public static Double calculateMedian(Collection<Double> c)
    {
        int middle = (new Double((double)c.size() / 2.0)).intValue();
        
        List<Double> list = new ArrayList<Double>(c);
        
        Collections.sort(list);
        
        if (c.size() % 2 == 1)
        {   
            return list.get(middle);
        }
        else
        {
            return (list.get(middle-1) + list.get(middle)) / 2.0;
        }
    }
    
    public static Double calculateTotal(Collection<Double> c)
    {
        double sum = 0.0;
        
        for (Double d : c)
        {
            sum += d;
        }
        
        return sum;
    }
    
    public static Double calculateDeviation(Collection<Double> c)
    {
        return Collections.max(c) - calculateAverage(c);
    }
}
