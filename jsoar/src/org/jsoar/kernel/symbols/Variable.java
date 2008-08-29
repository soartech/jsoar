/*
 * (c) 2008  Dave Ray
 *
 * Created on Aug 15, 2008
 */
package org.jsoar.kernel.symbols;

import java.util.Collection;
import java.util.List;
import java.util.Stack;

/**
 * @author ray
 */
public class Variable extends Symbol
{
    public String name;
    public int tc_number;
    public Symbol current_binding_value;
    /**
     * In C, this just gets shoved in the current_binding_value pointer. Can't do that in
     * Java, so it's maintained separately.
     */
    public int unbound_variable_index;
    
    public int gensym_number;
    
    /**
     * See rete.cpp:2285 for why this is a stack. 
     */
    public Stack<Integer> rete_binding_locations = new Stack<Integer>(); 
    
    public void markIfUnmarked(int tc_number, List<Variable> var_list)
    {
        if(this.tc_number != tc_number)
        {
            this.tc_number = tc_number;
            if(var_list != null)
            {
                var_list.add(this);
            }
        }
    }
    
    public static void unmark(Collection<Variable> vars)
    {
        for(Variable var : vars)
        {
            var.unmark();
        }
    }
    
    public void unmark()
    {
        this.tc_number = 0;
    }
    
    /**
     * rete.cpp:2312
     * 
     * @return
     */
    public boolean var_is_bound()
    {
        return !rete_binding_locations.isEmpty();
    }

    /**
     * rete.cpp:2323
     * 
     * @param depth
     * @param field_num
     * @return
     */
    private static int varloc_to_dummy(/*rete_node_level*/ int depth, int field_num)
    {
      return ((depth)<<2) + field_num;
    }

    /**
     * rete.cpp:2328
     * 
     * @param d
     * @return
     */
    public static int dummy_to_varloc_depth(int d)
    {
      return d >> 2;
    }

    /**
     * rete.cpp:2333
     * 
     * @param d
     * @return
     */
    public static int dummy_to_varloc_field_num(int d)
    {
      return d & 3;
    }

    /**
     * rete.cpp:2342
     * 
     * @param depth
     * @param field_num
     */
    public void push_var_binding(/*rete_node_level*/ int depth, int field_num)
    {
        int dummy_xy312 = varloc_to_dummy ((depth), (field_num));
        rete_binding_locations.push(dummy_xy312);
    }

    /**
     * rete.cpp:2355
     */
    public void pop_var_binding()
    {
        rete_binding_locations.pop();
    }
    
    /* (non-Javadoc)
     * @see org.jsoar.kernel.Symbol#asVariable()
     */
    @Override
    public Variable asVariable()
    {
        return this;
    }
    
    
    /* (non-Javadoc)
     * @see org.jsoar.kernel.symbols.Symbol#getFirstLetter()
     */
    @Override
    public char getFirstLetter()
    {
        return name.charAt(1);
    }


    /* (non-Javadoc)
     * @see java.lang.Object#hashCode()
     */
    @Override
    public int hashCode()
    {
        final int prime = 31;
        int result = super.hashCode();
        result = prime * result + ((name == null) ? 0 : name.hashCode());
        return result;
    }
    /* (non-Javadoc)
     * @see java.lang.Object#equals(java.lang.Object)
     */
    @Override
    public boolean equals(Object obj)
    {
        if (this == obj)
            return true;
        if (!super.equals(obj))
            return false;
        if (getClass() != obj.getClass())
            return false;
        final Variable other = (Variable) obj;
        if (name == null)
        {
            if (other.name != null)
                return false;
        }
        else if (!name.equals(other.name))
            return false;
        return true;
    }
    
}
