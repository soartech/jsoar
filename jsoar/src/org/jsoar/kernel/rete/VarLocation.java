/*
 * (c) 2008  Dave Ray
 *
 * Created on Aug 22, 2008
 */
package org.jsoar.kernel.rete;

import org.jsoar.kernel.Wme;
import org.jsoar.kernel.symbols.Symbol;

/**
 * rete.cpp:255
 * 
 * @author ray
 */
public class VarLocation
{
    int levels_up; /* 0=current node's alphamem, 1=parent's, etc. */
    int field_num;            /* 0=id, 1=attr, 2=value */

    public static boolean var_locations_equal(VarLocation v1, VarLocation v2)
    {
      return ( ((v1).levels_up==(v2).levels_up) && ((v1).field_num==(v2).field_num) );
    }
    
    public static Symbol field_from_wme(Wme wme, int field_num)
    {
        switch(field_num)
        {
        case 0: return wme.id;
        case 1: return wme.attr;
        case 2: return wme.value;
        }
        throw new IllegalArgumentException("field_num must be 0, 1, or 2, got" + field_num);
    }


}
