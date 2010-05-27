/*
 * Copyright (c) 2010 Dave Ray <daveray@gmail.com>
 *
 * Created on May 26, 2010
 */
package org.jsoar.kernel.modules;

import com.google.common.base.Predicate;

/**
 * @author ray
 */
public class Predicates
{
    public static Predicate<Double> betweenDouble(final double min, final double max, final boolean inclusive)
    {
        return new Predicate<Double>() {
            @Override
            public boolean apply(Double val)
            {
                return inclusive ? val >= min && val <= max : val > min && val < max;
            }
        };
    }
    
    public static Predicate<Double> greaterThanDouble(final double min, final boolean inclusive)
    {
        return betweenDouble(min, Double.POSITIVE_INFINITY, inclusive);
    }

    public static Predicate<Double> lessThanDouble(final double max, final boolean inclusive)
    {
        return betweenDouble(Double.NEGATIVE_INFINITY, max, inclusive);
    }
    
    public static Predicate<Integer> betweenInteger(final int min, final int max, final boolean inclusive)
    {
        return new Predicate<Integer>() {
            @Override
            public boolean apply(Integer val)
            {
                return inclusive ? val >= min && val <= max : val > min && val < max;
            }
        };
    }
    
    public static Predicate<Integer> greaterThanInteger(final Integer min, final boolean inclusive)
    {
        return betweenInteger(min, Integer.MAX_VALUE, inclusive);
    }

    public static Predicate<Integer> lessThanInteger(final Integer max, final boolean inclusive)
    {
        return betweenInteger(Integer.MIN_VALUE, max, inclusive);
    }

}
