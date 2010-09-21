/*
 * Copyright (c) 2010 Dave Ray <daveray@gmail.com>
 *
 * Created on Sep 20, 2010
 */
package org.jsoar.debugger.wm;

import java.awt.geom.Rectangle2D;
import java.util.ArrayList;
import java.util.List;

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

    public Value addValue(Wme wme)
    {
        final Value value = new Value(this, wme);
        values.add(value);
        return value;
    }
    
    static class Value
    {
        final WmeRow row;
        final Wme wme;
        boolean expanded;
        Rectangle2D bounds;

        private Value(WmeRow row, Wme wme)
        {
            this.row = row;
            this.wme = wme;
        }
    }
}