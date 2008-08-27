/*
 * (c) 2008  Dave Ray
 *
 * Created on Aug 15, 2008
 */
package org.jsoar.kernel.symbols;

import java.util.Collection;
import java.util.List;

/**
 * @author ray
 */
public class Variable extends Symbol
{
    public String name;
    public int tc_number;
    public Symbol current_binding_value;
    public int gensym_number;
    // TODO: rete_binding_locations;
    
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
