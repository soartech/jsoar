/*
 * Copyright (c) 2010 Dave Ray <daveray@gmail.com>
 *
 * Created on Sep 20, 2010
 */
package org.jsoar.debugger.wm;

import java.awt.geom.Rectangle2D;


/**
 * @author ray
 */
abstract class Row
{
    final RootRow root;
    final int level;
    int row;
    Rectangle2D bounds = new Rectangle2D.Double();
    
    public Row(RootRow root, int level)
    {
        this.root = root != null ? root : (RootRow) this;
        this.level = level;
    }
    
    public WmeRow asWme() { return null; }
    public RootRow asRoot() { return null; }
}
