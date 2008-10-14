/*
 * Copyright (c) 2008  Dave Ray <daveray@gmail.com>
 *
 * Created on Sep 29, 2008
 */
package org.jsoar.kernel;

import java.io.Writer;

import org.apache.commons.io.output.NullWriter;
import org.jsoar.tcl.SoarTclException;
import org.jsoar.tcl.SoarTclInterface;

/**
 * @author ray
 */
public class PerformanceTimer
{
    /**
     * @param args
     * @throws SoarTclException 
     */
    public static void main(String[] args) throws SoarTclException
    {
        for(String arg : args)
        {
            if("--raw".equals(arg))
            {
                System.out.println("TotalCPU, TotalKernel");
            }
        }
        for(int i = 0; i < 15; ++i)
        {
            doRun(args);
        }
    }

    /**
     * @param args
     * @throws SoarTclException
     */
    private static void doRun(String[] args) throws SoarTclException
    {
        Agent agent = new Agent();
        agent.trace.setEnabled(false);
        Writer oldWriter = agent.getPrinter().getWriter();
        agent.getPrinter().setWriter(new NullWriter(), false);
        agent.initialize();
        SoarTclInterface ifc = new SoarTclInterface(agent);
        
        boolean raw = false;
        for(String arg : args)
        {
            if("--raw".equals(arg))
            {
                raw = true;
            }
            else
            {
                ifc.sourceFile(arg);
            }
        }
        
        agent.decisionCycle.runForever();
        
        agent.getPrinter().setWriter(oldWriter, true);
        if(!raw)
        {
            ifc.eval("stats");
        }
        else
        {
            agent.getPrinter().print("%f, %f\n", 
                                     agent.getTotalCpuTimer().getTotalSeconds(), 
                                     agent.getTotalKernelTimer().getTotalSeconds());
        }
        ifc.dispose();
    }
}
