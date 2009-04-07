
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
        if (value == null) { return null; }

        MemoryNode node = new MemoryNode();
        
        IntegerSymbol ie = value.asInteger();
        if (ie != null) { node.setIntValue(ie.getValue()); return node; }

        DoubleSymbol fe = value.asDouble();
        if (fe != null) { node.setDoubleValue(fe.getValue()); return node; }
        
        StringSymbol se = value.asString();
        if (se != null) { node.setStringValue(se.getValue()); return node; }
        
        node.clearValue();
        return node;
    }
    
    public boolean isString()
    {
        return value instanceof String;
    }

    public boolean isInt()
    {
        return value instanceof Integer;
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

    public void setStringValue(String strVal)
    {
        assert(strVal != null);
        this.value = strVal;
    }

    public void setIntValue(int intVal)
    {
        this.value = intVal;
    }

    public void setDoubleValue(double doubleVal)
    {
        this.value = doubleVal;
    }

    public String getStringValue()
    {
        return value != null ? value.toString() : ""; 
    }

    public Object getValue()
    {
        return value;
    }
    
    public void setValue(MemoryNode other)
    {
        this.value = other.value;
    }
    
    public int getIntValue()
    {
        if(value instanceof Number)
        {
            return ((Number) value).intValue();
        }
        else if(value != null)
        {
            return Integer.parseInt(value.toString());
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
        if (value == null)
        {
            return "[NODE]";
        }
        return value.toString();
    }
     
    public String toString()
    {
        return super.toString() + valueToString();
    }
}

