/*
 * Copyright (c) 2008  Dave Ray <daveray@gmail.com>
 *
 * Created on Dec 4, 2008
 */
package org.jsoar.demos.toh;

import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;

import org.jsoar.kernel.io.InputOutput;
import org.jsoar.kernel.io.InputWmes;
import org.jsoar.kernel.memory.Wme;

/**
 * @author ray
 */
public class Peg
{
    private final String name;
    private final LinkedList<Disk> disks = new LinkedList<Disk>();
    
    private Wme pegWme;
    
    /**
     * @param name
     */
    public Peg(String name)
    {
        this.name = name;
    }
    
    public void placeDisk(Disk disk)
    {
        disks.addLast(disk);
        disk.setPeg(this);
    }
    
    public String getName()
    {
        return name;
    }
    
    void removeDisk(Disk disk)
    {
        disks.remove(disk);
    }
    
    public void update(InputOutput io)
    {
        if(pegWme == null)
        {
            pegWme = InputWmes.add(io, "peg", name);
        }
        
        Disk below = null;
        for(Disk disk : disks)
        {
            disk.update(io, below);
            below = disk;
        }
    }
    
    public Disk getTopDisk()
    {
        return !disks.isEmpty() ? disks.getLast() : null;
    }
    
    public List<Disk> getDisks()
    {
        return new ArrayList<Disk>(disks);
    }
}
