
package org.jsoar.kernel.io.quick;

import org.jsoar.kernel.symbols.DoubleSymbol;
import org.jsoar.kernel.symbols.IntegerSymbol;
import org.jsoar.kernel.symbols.StringSymbol;
import org.jsoar.kernel.symbols.Symbol;

class MemoryNode
{
    private Object value;
    
    static MemoryNode create(Symbol value)
    {
        if(value == null)
        {
            return null;
        }
        
        MemoryNode node = new MemoryNode();
        
        IntegerSymbol ie = value.asInteger();
        if(ie != null)
        {
            node.setIntValue(ie.getValue());
            return node;
        }
        
        DoubleSymbol fe = value.asDouble();
        if(fe != null)
        {
            node.setDoubleValue(fe.getValue());
            return node;
        }
        
        StringSymbol se = value.asString();
        if(se != null)
        {
            node.setStringValue(se.getValue());
            return node;
        }
        
        node.clearValue();
        return node;
    }
    
    public boolean isString()
    {
        return value instanceof String;
    }
    
    public boolean isInt()
    {
        return value instanceof Long;
    }
    
    public boolean isDouble()
    {
        return value instanceof Double;
    }
    
    public boolean isNumeric()
    {
        return isInt() || isDouble();
    }
    
    public boolean isLeaf()
    {
        return value != null;
    }
    
    public boolean setStringValue(String strVal)
    {
        assert (strVal != null);
        final boolean changed = !strVal.equals(this.value);
        this.value = strVal;
        return changed;
    }
    
    public boolean setIntValue(long intVal)
    {
        final boolean changed = !isInt() || intVal != getIntValue();
        this.value = intVal;
        return changed;
    }
    
    public boolean setDoubleValue(double doubleVal)
    {
        final boolean changed = !isDouble() || doubleVal != getDoubleValue();
        this.value = doubleVal;
        return changed;
    }
    
    public String getStringValue()
    {
        return value != null ? value.toString() : "";
    }
    
    public Object getValue()
    {
        return value;
    }
    
    public boolean setValue(MemoryNode other)
    {
        final boolean changed = other != this.value;
        this.value = other.value;
        return changed;
    }
    
    public long getIntValue()
    {
        if(value instanceof Number)
        {
            return ((Number) value).longValue();
        }
        else if(value != null)
        {
            return Long.parseLong(value.toString());
        }
        return 0;
    }
    
    public double getDoubleValue()
    {
        if(value instanceof Number)
        {
            return ((Number) value).doubleValue();
        }
        else if(value != null)
        {
            return Double.parseDouble(value.toString());
        }
        return 0;
    }
    
    public void clearValue()
    {
        value = null;
    }
    
    public boolean hasSameType(MemoryNode node)
    {
        if((value == null && node.value != null) ||
                (value != null && node.value == null))
        {
            return false;
        }
        return (value == null && node.value == null) ||
                (value.getClass().equals(node.value.getClass()));
    }
    
    public boolean valueIsEqual(MemoryNode other)
    {
        if(value != null)
        {
            return value.equals(other.value);
        }
        return value == other.value;
    }
    
    public String valueToString()
    {
        if(value == null)
        {
            return "[NODE]";
        }
        return value.toString();
    }
    
    @Override
    public String toString()
    {
        return super.toString() + valueToString();
    }
}
