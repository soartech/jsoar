/*
 * (c) 2008  Dave Ray
 *
 * Created on Aug 22, 2008
 */
package org.jsoar.kernel.rete;

import org.jsoar.kernel.memory.Wme;
import org.jsoar.kernel.symbols.Symbol;

/**
 * rete.cpp:255
 * 
 * @author ray
 */
public class VarLocation
{
    int levels_up; // 0=current node's alphamem, 1=parent's, etc.
    int field_num; // 0=id, 1=attr, 2=value

    /**
     * Copy another var location into this one
     * 
     * @param other The other var location 
     */
    public void assign(VarLocation other)
    {
        this.levels_up = other.levels_up;
        this.field_num = other.field_num;
    }
    
    /**
     * rete.cpp:263:var_locations_equal
     * 
     * @param v1
     * @param v2
     * @return
     */
    public static boolean var_locations_equal(VarLocation v1, VarLocation v2)
    {
      return ( ((v1).levels_up==(v2).levels_up) && ((v1).field_num==(v2).field_num) );
    }
    
    /**
     * rete.cpp:273:field_from_wme
     * 
     * @param wme
     * @param field_num
     * @return
     */
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

    /* (non-Javadoc)
     * @see java.lang.Object#toString()
     */
    @Override
    public String toString()
    {
        return levels_up + ":" + field_num;
    }

    

}
