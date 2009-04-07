/*
 * Copyright (c) 2008  Dave Ray <daveray@gmail.com>
 *
 * Created on Dec 4, 2008
 */
package org.jsoar.demos.toh;

import org.jsoar.kernel.io.InputBuilder;
import org.jsoar.kernel.io.InputOutput;
import org.jsoar.kernel.memory.Wme;


/**
 * @author ray
 */
public class Disk
{
    private final int size;
    
    private Peg peg;
    private boolean moved;
    private Wme pegWme, aboveWme;

    /**
     * @param size
     */
    public Disk(int size)
    {
        this.size = size;
    }
    
    public int getSize()
    {
        return size;
    }

    void setPeg(Peg peg)
    {
        if(this.peg != null)
        {
            this.peg.removeDisk(this);
        }
        this.peg = peg;
        this.moved = true;
    }

    /**
     * @param io
     * @param below
     */
    public void update(InputOutput io, Disk below)
    {
        if(pegWme == null)
        {
            // Build initial structure
            InputBuilder builder = InputBuilder.create(io).
            add("disk", size).
            push("holds").
                add("on", peg.getName()).markWme("peg").
                add("disk", size).
                add("above", getAbove(below)).markWme("above").
                top();
            
            // Retrieve WMEs we need to update later
            pegWme = builder.getWme("peg");
            aboveWme = builder.getWme("above");
        }
        else if(moved)
        {
            pegWme = InputBuilder.update(io, pegWme, peg.getName());
            aboveWme = InputBuilder.update(io, aboveWme, getAbove(below));
            
            moved = false;
        }
    }
    
    private static Object getAbove(Disk below)
    {
        return below != null ? below.size : "none";
    }
}
