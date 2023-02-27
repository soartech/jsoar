/*
 * Copyright (c) 2010 Dave Ray <daveray@gmail.com>
 *
 * Created on Sep 20, 2010
 */
package org.jsoar.debugger.wm;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.Callable;

import javax.swing.JButton;

import org.jsoar.debugger.Images;
import org.jsoar.kernel.symbols.Identifier;
import org.jsoar.kernel.symbols.Symbol;

/**
 * @author ray
 */
class RootRow extends Row
{
    final Object key;
    private final Callable<Identifier> getter;
    private Identifier id;
    final Map<Symbol, WmeRow> children = new HashMap<>();
    final JButton deleteButton = new JButton();
    long ts;
    
    public RootRow(long ts, Object key, Callable<Identifier> getter)
    {
        super(null, 0);
        
        this.ts = ts;
        this.key = key;
        this.getter = getter;
        try
        {
            this.id = getter.call();
        }
        catch(Exception e)
        {
            if(e instanceof InterruptedException)
            {
                Thread.currentThread().interrupt();
            }
            else
            {
                throw new RuntimeException(e.getMessage(), e);
            }
        }
        
        deleteButton.setBorderPainted(false);
        deleteButton.setOpaque(false);
        deleteButton.setContentAreaFilled(false);
        deleteButton.setIcon(Images.DELETE);
    }
    
    public Identifier getId()
    {
        return id;
    }
    
    public boolean update(long ts)
    {
        final Identifier oldId = id;
        try
        {
            this.id = getter.call();
        }
        catch(Exception e)
        {
            if(e instanceof InterruptedException)
            {
                Thread.currentThread().interrupt();
            }
            else
            {
                throw new RuntimeException(e.getMessage(), e);
            }
        }
        
        return oldId != id;
    }
    
    /*
     * (non-Javadoc)
     * 
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
