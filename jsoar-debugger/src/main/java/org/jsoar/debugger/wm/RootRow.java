/*
 * Copyright (c) 2010 Dave Ray <daveray@gmail.com>
 *
 * Created on Sep 20, 2010
 */
package org.jsoar.debugger.wm;

import org.jsoar.kernel.symbols.Identifier;

/**
 * @author ray
 */
class RootRow extends Row
{
    final Identifier id;

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
}
