/*
 * (c) 2008  Dave Ray
 *
 * Created on Aug 18, 2008
 */
package org.jsoar.kernel.rhs;

import java.util.Formatter;

import org.jsoar.kernel.symbols.Variable;
import org.jsoar.util.ListHead;
import org.jsoar.util.markers.Marker;

/**
 * Instances of this class are immutable
 * 
 * @author ray
 */
public class UnboundVariable extends AbstractRhsValue
{
    private final int index;

    // Pre-allocate common values rather than constantly reallocating
    // This is safe because instances are immutable
    private static final UnboundVariable[] COMMON = new UnboundVariable[100];
    static
    {
        for(int i = 0; i < COMMON.length; ++i)
        {
            COMMON[i] = new UnboundVariable(i);
        }
    }
    
    /**
     * Create an unbound variable
     * 
     * @param index the variable index
     * @return new, immutable unbound variable
     */
    public static UnboundVariable create(int index)
    {
        if(index < COMMON.length) 
        {
            return COMMON[index];
        }
        return new UnboundVariable(index);
    }
    
    /**
     * @param index
     */
    private UnboundVariable(int index)
    {
        this.index = index;
    }

    /**
     * @return the index
     */
    public int getIndex()
    {
        return index;
    }

    /* (non-Javadoc)
     * @see org.jsoar.kernel.RhsValue#asUnboundVariable()
     */
    @Override
    public UnboundVariable asUnboundVariable()
    {
        return this;
    }

    /* (non-Javadoc)
     * @see org.jsoar.kernel.rhs.RhsValue#copy()
     */
    @Override
    public RhsValue copy()
    {
        return this;
    }

    /* (non-Javadoc)
     * @see org.jsoar.kernel.RhsValue#addAllVariables(int, java.util.List)
     */
    @Override
    public void addAllVariables(Marker tc_number, ListHead<Variable> var_list)
    {
        // See comment in RhsValue.addAllVariables.
        throw new UnsupportedOperationException("addAllVariables not supported on UnboundVariable RhsValue");
    }

    /* (non-Javadoc)
     * @see java.lang.Object#toString()
     */
    @Override
    public String toString()
    {
        return "(unbound-var + " + index + ")";
    }

    /* (non-Javadoc)
     * @see java.util.Formattable#formatTo(java.util.Formatter, int, int, int)
     */
    @Override
    public void formatTo(Formatter formatter, int flags, int width, int precision)
    {
        throw new IllegalStateException("Internal error: rhs_value_to_string called on unbound variable.");
    }

    /* (non-Javadoc)
     * @see java.lang.Object#hashCode()
     */
    @Override
    public int hashCode()
    {
        final int prime = 31;
        int result = 1;
        result = prime * result + index;
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
        if (obj == null)
            return false;
        if (!(obj instanceof UnboundVariable))
            return false;
        UnboundVariable other = (UnboundVariable) obj;
        if (index != other.index)
            return false;
        return true;
    }
    
    
}
