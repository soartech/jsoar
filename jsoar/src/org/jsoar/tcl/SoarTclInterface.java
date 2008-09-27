/*
 * Copyright (c) 2008  Dave Ray <daveray@gmail.com>
 *
 * Created on Sep 18, 2008
 */
package org.jsoar.tcl;

import java.io.IOException;

import org.jsoar.kernel.Agent;
import org.jsoar.kernel.rhs.ReordererException;
import org.jsoar.kernel.symbols.SymConstant;

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
            catch (ReordererException e)
            {
                throw new TclException(interp, e.getMessage());
            }
        }};
        
    private Command multiAttrCommand = new Command() {

        @Override
        public void cmdProc(Interp interp, TclObject[] args) throws TclException
        {
            if(args.length != 3)
            {
                throw new TclNumArgsException(interp, 2, args, "attr cost");
            }
            
            SymConstant attr = agent.syms.make_sym_constant(args[1].toString());
            int cost = Integer.valueOf(args[2].toString());
            agent.getMultiAttributes().setCost(attr, cost);
        }
        
    };

    /**
     * @param agent
     */
    public SoarTclInterface(Agent agent)
    {
        this.agent = agent;
        
        interp.createCommand("sp", spCommand);
        interp.createCommand("multi-attributes", multiAttrCommand);
    }
    
    public void dispose()
    {
        interp.dispose();
    }
    
    public Agent getAgent()
    {
        return agent;
    }
    
    public void sourceFile(String file) throws SoarTclException
    {
        try
        {
            interp.evalFile(file);
        }
        catch (TclException e)
        {
            throw new SoarTclException(interp);
        }
    }
    
    public void sourceResource(String resource) throws SoarTclException
    {
        try
        {
            interp.evalResource(resource);
        }
        catch (TclException e)
        {
            throw new SoarTclException(interp);
        }
    }
    
    public static void main(String[] args) throws SoarTclException
    {
        Agent agent = new Agent();
        SoarTclInterface ifc = new SoarTclInterface(agent);
        agent.initialize();
        
        ifc.sourceFile("single.soar");
        
        agent.trace.setEnabled(false);
        agent.decisionCycle.run_for_n_decision_cycles(3000);
    }
}
