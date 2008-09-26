/*
 * Copyright (c) 2008  Dave Ray <daveray@gmail.com>
 *
 * Created on Sep 25, 2008
 */
package org.jsoar.kernel.lhs;

import java.util.HashMap;
import java.util.Map;

import org.jsoar.kernel.symbols.Symbol;
import org.jsoar.util.Arguments;

/**
 * @author ray
 */
public class MultiAttributes
{
    private Map<Symbol, Integer> costMap = new HashMap<Symbol, Integer>();
    
    public void setCost(Symbol referent, int value)
    {
        Arguments.check(value > 0, "multi-attribute cost must be greater than zero");
        
        costMap.put(referent, value);
    }
    
    /**
     * Returns the user set value of the expected match cost of the
     * multi-attribute, or 1 if the input symbol isn't in the user set list.
     * 
     * reorder.cpp:699:get_cost_of_possible_multi_attribute
     * 
     * @param referent
     * @return
     */
    public int getCost(Symbol referent, int defValue)
    {
        Integer cost = costMap.get(referent);
        return cost != null ? cost.intValue() : defValue;
    }
    
}
