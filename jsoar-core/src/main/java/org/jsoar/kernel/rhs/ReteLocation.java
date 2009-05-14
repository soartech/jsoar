/*
 * (c) 2008  Dave Ray
 *
 * Created on Aug 18, 2008
 */
package org.jsoar.kernel.rhs;

import java.util.Formatter;

import org.jsoar.kernel.memory.WmeImpl;
import org.jsoar.kernel.rete.Token;
import org.jsoar.kernel.symbols.SymbolImpl;
import org.jsoar.kernel.symbols.Variable;
import org.jsoar.util.ListHead;

/**
 * Stores a relative reference to a rete field. Instances of this class are 
 * immutable.
 * 
 * @author ray
 */
public class ReteLocation extends RhsValue
{
    private final int fieldNum;
    private final int levelsUp;
    
    // Pre-allocate rete locations for up to 64 levels. These objects are
    // immutable so this is safe and we might as well avoid allocating
    // the same objects over and over.
    private static final int NUM_FIELDS = 3;
    private static final int NUM_LEVELS = 64;
    private static final ReteLocation[][] COMMON = new ReteLocation[NUM_FIELDS][NUM_LEVELS];
    static
    {
        for(int i = 0; i < COMMON.length; ++i)
        {
            for(int j = 0; j < COMMON[i].length; ++j)
            {
                COMMON[i][j] = new ReteLocation(i, j);
            }
        }
    }
    
    public static ReteLocation create(int fieldNum, int levelsUp)
    {
        if(fieldNum < NUM_FIELDS && levelsUp < NUM_LEVELS)
        {
            return COMMON[fieldNum][levelsUp];
        }
        return new ReteLocation(fieldNum, levelsUp);
    }
    
    private ReteLocation(int fieldNum, int levelsUp)
    {
        this.fieldNum = fieldNum;
        this.levelsUp = levelsUp;
    }
    
    /**
     * @return The rete field number
     */
    public int getFieldNum()
    {
        return fieldNum;
    }
    
    /**
     * @return Number of rete levels up
     */
    public int getLevelsUp()
    {
        return levelsUp;
    }
    
    /**
     * <p>rete.cpp:4391:get_symbol_from_rete_loc
     * 
     * @param levels_up
     * @param field_num
     * @param tok
     * @param w
     * @return
     */
    public SymbolImpl lookupSymbol(Token tok, WmeImpl w)
    {
        int levels_up = levelsUp;
        while (levels_up != 0)
        {
            levels_up--;
            w = tok.w;
            tok = tok.parent;
        }
        if (fieldNum == 0)
            return w.id;
        if (fieldNum == 1)
            return w.attr;
        
        return w.value;
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
     * @see org.jsoar.kernel.RhsValue#asReteLocation()
     */
    @Override
    public ReteLocation asReteLocation()
    {
        return this;
    }
    
    /* (non-Javadoc)
     * @see org.jsoar.kernel.RhsValue#addAllVariables(int, java.util.List)
     */
    @Override
    public void addAllVariables(int tc_number, ListHead<Variable> var_list)
    {
        // See comment in RhsValue.addAllVariables.
        throw new UnsupportedOperationException("addAllVariables not supported on ReteLocation RhsValue");
    }
    
    /* (non-Javadoc)
     * @see java.lang.Object#toString()
     */
    @Override
    public String toString()
    {
        // For debugging only
        return "(rete-location " + levelsUp + ":" + fieldNum + ")";
    }
    
    /* (non-Javadoc)
     * @see java.util.Formattable#formatTo(java.util.Formatter, int, int, int)
     */
    @Override
    public void formatTo(Formatter formatter, int arg1, int arg2, int arg3)
    {
        throw new IllegalStateException("Internal error: rhs_value_to_string called on reteloc.");
    }

    /* (non-Javadoc)
     * @see java.lang.Object#hashCode()
     */
    @Override
    public int hashCode()
    {
        final int prime = 31;
        int result = 1;
        result = prime * result + fieldNum;
        result = prime * result + levelsUp;
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
        if (!(obj instanceof ReteLocation))
            return false;
        ReteLocation other = (ReteLocation) obj;
        if (fieldNum != other.fieldNum)
            return false;
        if (levelsUp != other.levelsUp)
            return false;
        return true;
    }
    
}
