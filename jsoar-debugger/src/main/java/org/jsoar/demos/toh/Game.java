/*
 * Copyright (c) 2008  Dave Ray <daveray@gmail.com>
 *
 * Created on Dec 4, 2008
 */
package org.jsoar.demos.toh;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.jsoar.kernel.events.OutputEvent;
import org.jsoar.kernel.io.InputOutput;
import org.jsoar.kernel.memory.Wme;
import org.jsoar.kernel.memory.Wmes;
import org.jsoar.kernel.symbols.SymbolFactory;

/**
 * @author ray
 */
public class Game
{
    private final int numPegs;
    private final int numDisks;
    private final Map<String, Peg> pegs = new LinkedHashMap<String, Peg>();
    
    public Game(int pegs, int disks)
    {
        this.numPegs = pegs;
        this.numDisks = disks;
        reset();
    }
    
    public synchronized List<Peg> getPegs()
    {
        return new ArrayList<Peg>(pegs.values());
    }
    
    public int getNumDisks()
    {
        return this.numDisks;
    }
    
    public synchronized void update(InputOutput io)
    {
        for(Peg p : pegs.values())
        {
            p.update(io);
        }
    }
    
    public synchronized void handleCommands(OutputEvent output)
    {
        for(Wme command : output.getInputOutput().getPendingCommands())
        {
            handleCommand(output.getInputOutput().getSymbols(), command);
        }
    }
 
    public synchronized void reset()
    {
        this.pegs.clear();
        char pegLetter = 'A';
        for(int i = 0; i < numPegs; ++i)
        {
            final String name = Character.toString((char) (pegLetter + i));
            this.pegs.put(name, new Peg(name));
        }
        
        Peg firstPeg = this.pegs.get("A");
        for(int i = numDisks; i > 0; --i)
        {
            firstPeg.placeDisk(new Disk(i));
        }
        
    }
    
    /**
     * @param command
     */
    private void handleCommand(SymbolFactory syms, Wme command)
    {
        final String name = command.getAttribute().toString();
     
        if(name.equals("move-disk"))
        {
            final Wme sourceWme = Wmes.matcher(syms).attr("source-peg").find(command);
            final Wme destWme = Wmes.matcher(syms).attr("destination-peg").find(command);
            
            final Peg sourcePeg = pegs.get(sourceWme.getValue().toString());
            final Peg destPeg = pegs.get(destWme.getValue().toString());
            destPeg.placeDisk(sourcePeg.getTopDisk());
        }
    }
    
}
