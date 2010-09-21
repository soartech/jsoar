/*
 * Copyright (c) 2010 Dave Ray <daveray@gmail.com>
 *
 * Created on Sep 20, 2010
 */
package org.jsoar.debugger.wm;

import java.util.HashMap;
import java.util.Map;

import org.jsoar.kernel.symbols.Identifier;
import org.jsoar.kernel.symbols.Symbol;

/**
 * @author ray
 */
class RootRow extends Row
{
    final Identifier id;
    final Map<Symbol, WmeRow> children = new HashMap<Symbol, WmeRow>();

    public RootRow(Identifier id)
    {
        super(null, 0);
        
        this.id = id;
    }

    /* (non-Javadoc)
     * @see org.jsoar.debugger.wm.Row#asRoot()
     */
    @Override
    public RootRow asRoot()
    {
        return this;
    }
    
    public WmeRow addChild(Identifier id, Symbol attr)
    {
        final WmeRow newRow = new WmeRow(this, null, id, attr);
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
