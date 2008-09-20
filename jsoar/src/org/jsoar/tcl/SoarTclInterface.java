/*
 * Copyright (c) 2008  Dave Ray <daveray@gmail.com>
 *
 * Created on Sep 18, 2008
 */
package org.jsoar.tcl;

import java.io.IOException;

import org.jsoar.kernel.Agent;
import org.jsoar.kernel.Phase;

import tcl.lang.Command;
import tcl.lang.Interp;
import tcl.lang.TclException;
import tcl.lang.TclNumArgsException;
import tcl.lang.TclObject;

/**
 * @author ray
 */
public class SoarTclInterface
{
    private final Agent agent;
    private Interp interp = new Interp();
    
    private Command spCommand = new Command() {

        @Override
        public void cmdProc(Interp interp, TclObject[] args) throws TclException
        {
            if(args.length != 2)
            {
                throw new TclNumArgsException(interp, 1, args, "body");
            }
            
            try
            {
                agent.loadProduction(args[1].toString());
            }
            catch (IOException e)
            {
                throw new TclException(interp, e.getMessage());
            }
        }};

    /**
     * @param agent
     */
    public SoarTclInterface(Agent agent)
    {
        this.agent = agent;
        
        interp.createCommand("sp", spCommand);
    }
    
    public void dispose()
    {
        interp.dispose();
    }
    
    public Agent getAgent()
    {
        return agent;
    }
    
    public void sourceFile(String file) throws TclException
    {
        interp.evalFile(file);
    }
    
    public void sourceResource(String resource) throws TclException
    {
        interp.evalResource(resource);
    }
    
    public static void main(String[] args) throws TclException
    {
        Agent agent = new Agent();
        SoarTclInterface ifc = new SoarTclInterface(agent);
        
        ifc.sourceFile("c:/waterjug.soar");
        
        agent.trace.enableAll();
        for(int i = 0; i< 200; ++i)
        {
            agent.decisionCycle.do_one_top_level_phase();
            if(agent.decisionCycle.current_phase == Phase.INPUT_PHASE)
            {
                agent.getPrinter().print("State = %s", agent.decider.bottom_goal);
            }
            agent.getPrinter().flush();
        }
//        for(String arg : args)
//        {
//            ifc.interp.evalFile(arg);
//        }
    }
}
