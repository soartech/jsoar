/*
 * Copyright (c) 2010 Dave Ray <daveray@gmail.com>
 *
 * Created on Sep 20, 2010
 */
package org.jsoar.debugger.wm;

import java.awt.geom.Rectangle2D;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.jsoar.kernel.memory.Wme;
import org.jsoar.kernel.symbols.Identifier;
import org.jsoar.kernel.symbols.Symbol;

class WmeRow extends Row
{
    final Value parent;
    final Identifier id;
    Rectangle2D idBounds;
    
    final Symbol attr;
    Rectangle2D attrBounds;
    
    final List<Value> values = new ArrayList<Value>();

    public WmeRow(RootRow root, Value parent, Identifier id, Symbol attr)
    {
        super(root, parent != null ? parent.row.level + 1 : 0);
        
        this.parent = parent;
        this.id = id;
        this.attr = attr;
    }
    
    /* (non-Javadoc)
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
        return value;
    }
    
    static class Value
    {
        final long ts;
        final WmeRow row;
        final Wme wme;
        boolean expanded;
        final Map<Symbol, WmeRow> children = new HashMap<Symbol, WmeRow>();
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
    }
}