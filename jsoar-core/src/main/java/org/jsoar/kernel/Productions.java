/*
 * (c) 2010  Soar Technology, Inc.
 *
 * Created on May 19, 2010
 */
package org.jsoar.kernel;

import org.jsoar.util.ByRef;

/**
 * Utilities related to productions.
 * 
 * @author ray
 * @see Production
 */
public class Productions
{
    public static String generateUniqueName(ProductionManager prods, String prefix, ByRef<Integer> number)
    {
        String name = prefix + number.value++;
        Production p = prods.getProduction(name);
        while(p != null)
        {
            name = prefix + number.value++;
            p = prods.getProduction(name);
        }
        return name;
    }
}
