/*
 * Copyright (c) 2008  Dave Ray <daveray@gmail.com>
 *
 * Created on Sep 29, 2008
 */
package org.jsoar.kernel;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import org.apache.commons.io.output.NullWriter;
import org.jsoar.tcl.SoarTclException;
import org.jsoar.tcl.SoarTclInterface;

/**
 * @author ray
 */
public class PerformanceTimer
{
    private static List<Double> cpuTimes = new ArrayList<Double>();
    private static List<Double> kernelTimes = new ArrayList<Double>();
    
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
        
        System.out.println("\n-----------------------------------------");
        Collections.sort(cpuTimes);
        Collections.sort(kernelTimes);
        System.out.printf("   CPU: min %f, med %f, max %f\n", cpuTimes.get(0), cpuTimes.get(cpuTimes.size() / 2) , cpuTimes.get(cpuTimes.size() - 1));
        System.out.printf("Kernel: min %f, med %f, max %f\n", kernelTimes.get(0), kernelTimes.get(kernelTimes.size() / 2) , kernelTimes.get(kernelTimes.size() - 1));
        
    }

    /**
     * @param args
     * @throws SoarTclException
     */
    private static void doRun(String[] args) throws SoarTclException
    {
        Agent agent = new Agent();
        agent.getTrace().setEnabled(false);
        agent.getPrinter().pushWriter(new NullWriter(), false);
        agent.initialize();
        SoarTclInterface ifc = SoarTclInterface.findOrCreate(agent);
        
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
        
        agent.getPrinter().popWriter();
        
        final double cpuTime = agent.getTotalCpuTimer().getTotalSeconds();
        final double kernelTime = agent.getTotalKernelTimer().getTotalSeconds();
        
        cpuTimes.add(cpuTime);
        kernelTimes.add(kernelTime);
        
        if(!raw)
        {
            ifc.eval("stats");
        }
        else
        {
            agent.getPrinter().print("%f, %f\n", cpuTime, kernelTime);
        }
        SoarTclInterface.dispose(ifc);
    }
}
