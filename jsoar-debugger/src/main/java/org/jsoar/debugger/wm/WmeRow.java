/*
 * Copyright (c) 2010 Dave Ray <daveray@gmail.com>
 *
 * Created on Sep 20, 2010
 */
package org.jsoar.debugger.wm;

import java.awt.geom.Rectangle2D;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.jsoar.kernel.memory.Wme;
import org.jsoar.kernel.symbols.DoubleSymbol;
import org.jsoar.kernel.symbols.Identifier;
import org.jsoar.kernel.symbols.IntegerSymbol;
import org.jsoar.kernel.symbols.StringSymbol;
import org.jsoar.kernel.symbols.Symbol;

class WmeRow extends Row
{
    final Value parent;
    final Identifier id;
    Rectangle2D idBounds;
    
    final Symbol attr;
    Rectangle2D attrBounds;
    
    final List<Value> values = new ArrayList<>();
    
    public WmeRow(RootRow root, Value parent, Identifier id, Symbol attr)
    {
        super(root, parent != null ? parent.row.level + 1 : 0);
        
        this.parent = parent;
        this.id = id;
        this.attr = attr;
    }
    
    /*
     * (non-Javadoc)
     * 
     * @see org.jsoar.debugger.wm.Row#asWme()
     */
    @Override
    public WmeRow asWme()
    {
        return this;
    }
    
    public Value getValue(Wme wme)
    {
        for(Value value : values)
        {
            if(wme == value.wme)
            {
                return value;
            }
        }
        return null;
    }
    
    public Value addValue(long ts, Wme wme)
    {
        final Value value = new Value(ts, this, wme);
        values.add(value);
        Collections.sort(values);
        return value;
    }
    
    static class Value implements Comparable<Value>
    {
        final long ts;
        final WmeRow row;
        final Wme wme;
        boolean expanded;
        final Map<Symbol, WmeRow> children = new HashMap<>();
        Rectangle2D bounds;
        
        private Value(long ts, WmeRow row, Wme wme)
        {
            this.ts = ts;
            this.row = row;
            this.wme = wme;
        }
        
        public WmeRow addChild(RootRow root, Identifier id, Symbol attr)
        {
            final WmeRow newRow = new WmeRow(root, this, id, attr);
            if(null != children.put(attr, newRow))
            {
                throw new IllegalStateException("Multiple children with same attribute!");
            }
            return newRow;
        }
        
        public void removeChild(WmeRow child)
        {
            children.remove(child.attr);
        }
        
        /*
         * (non-Javadoc)
         * 
         * @see java.lang.Comparable#compareTo(java.lang.Object)
         */
        @Override
        public int compareTo(Value o)
        {
            final Symbol mine = wme.getValue();
            final Symbol theirs = o.wme.getValue();
            
            if(mine == theirs)
            {
                return 0;
            }
            
            // Identifier is "greatest"
            final Identifier mineId = mine.asIdentifier();
            final Identifier theirId = theirs.asIdentifier();
            if(mineId != null)
            {
                if(theirId != null)
                {
                    final int letter = mineId.getNameLetter() - theirId.getNameLetter();
                    final long number = mineId.getNameNumber() - theirId.getNameNumber();
                    if(letter < 0)
                    {
                        return -1;
                    }
                    else if(letter == 0)
                    {
                        return number < 0 ? -1 : 1;
                    }
                    else
                    {
                        return 1;
                    }
                }
                else
                {
                    return 1;
                }
            }
            else if(theirId != null)
            {
                return -1;
            }
            
            // String is next
            final StringSymbol myString = mine.asString();
            final StringSymbol theirString = theirs.asString();
            if(myString != null)
            {
                if(theirString != null)
                {
                    return myString.getValue().compareTo(theirString.getValue());
                }
                else
                {
                    return 1;
                }
            }
            else if(theirString != null)
            {
                return -1;
            }
            
            // int is next
            final IntegerSymbol myInt = mine.asInteger();
            final IntegerSymbol theirInt = theirs.asInteger();
            if(myInt != null)
            {
                if(theirInt != null)
                {
                    final long d = myInt.getValue() - theirInt.getValue();
                    return d < 0 ? -1 : 1;
                }
                else
                {
                    return 1;
                }
            }
            else if(theirInt != null)
            {
                return -1;
            }
            
            // double is next
            final DoubleSymbol myDouble = mine.asDouble();
            final DoubleSymbol theirDouble = theirs.asDouble();
            if(myDouble != null)
            {
                if(theirDouble != null)
                {
                    final double d = myDouble.getValue() - theirDouble.getValue();
                    return d < 0 ? -1 : 1;
                }
                else
                {
                    return 1;
                }
            }
            else if(theirDouble != null)
            {
                return -1;
            }
            
            return 0;
        }
    }
}
