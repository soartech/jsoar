/*
 * Copyright (c) 2008  Dave Ray <daveray@gmail.com>
 *
 * Created on Sep 29, 2008
 */
package org.jsoar.kernel;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import org.jsoar.util.NullWriter;
import org.jsoar.util.commands.SoarCommandInterpreter;
import org.jsoar.util.commands.SoarCommands;

/**
 * @author ray
 */
public class PerformanceTimer
{
    private static List<Double> cpuTimes = new ArrayList<Double>();
    private static List<Double> kernelTimes = new ArrayList<Double>();
    private static List<Integer> decisionCycles = new ArrayList<Integer>();
    private static List<Long> totalMemory = new ArrayList<Long>();
    
    /**
     * @param args
     * @throws SoarTclException 
     */
    public static void main(String[] args) throws Exception
    {
        for(String arg : args)
        {
            if("--raw".equals(arg))
            {
                System.out.println("TotalCPU, TotalKernel");
            }
            else if("--decisions".equals(arg))
            {
                System.out.println("Decisions!");
            }
        }
        for(int i = 0; i < 10; ++i)
        {
            doRun(args);
        }
        
        System.out.println("\n-----------------------------------------");
        Collections.sort(cpuTimes);
        Collections.sort(kernelTimes);
        Collections.sort(decisionCycles);
        Collections.sort(totalMemory);
        System.out.printf("   CPU: min %f, med %f, max %f\n", cpuTimes.get(0), cpuTimes.get(cpuTimes.size() / 2) , cpuTimes.get(cpuTimes.size() - 1));
        System.out.printf("Kernel: min %f, med %f, max %f\n", kernelTimes.get(0), kernelTimes.get(kernelTimes.size() / 2) , kernelTimes.get(kernelTimes.size() - 1));
        System.out.printf("DecCyc: min %8d, med %8d, max %8d\n", decisionCycles.get(0), decisionCycles.get(decisionCycles.size() / 2) , decisionCycles.get(decisionCycles.size() - 1));
        System.out.printf("TotMem: min %8d, med %8d, max %8d\n", totalMemory.get(0), totalMemory.get(totalMemory.size() / 2) , totalMemory.get(totalMemory.size() - 1));
        
    }

    /**
     * @param args
     * @throws SoarTclException
     * @throws SoarException 
     */
    private static void doRun(String[] args) throws SoarException
    {
        Agent agent = new Agent();
        agent.getTrace().setEnabled(false);
        agent.getPrinter().pushWriter(new NullWriter());
        agent.initialize();
        SoarCommandInterpreter ifc = agent.getInterpreter();
        
        boolean raw = false;
        long decisions = -1;
        for(String arg : args)
        {
            if(decisions == 0) 
            {
                decisions = Integer.valueOf(arg);
                if(decisions == 0) 
                    decisions = -1;
            } 
            else if("--raw".equals(arg))
            {
                raw = true;
            }
            else if("--decisions".equals(arg))
            {
                decisions = 0;
            }
            else
            {
                SoarCommands.source(ifc, arg);
            }
        }
        
        if(decisions > 0)
        {
            agent.runFor(decisions, RunType.DECISIONS);
        }
        else
        {
            agent.runForever();
        }
        
        agent.getPrinter().popWriter();
        
        final double cpuTime = agent.getTotalCpuTimer().getTotalSeconds();
        final double kernelTime = agent.getTotalKernelTimer().getTotalSeconds();
        final int dc = agent.getProperties().get(SoarProperties.D_CYCLE_COUNT).intValue();
        final long mem = Runtime.getRuntime().totalMemory();
        
        cpuTimes.add(cpuTime);
        kernelTimes.add(kernelTime);
        decisionCycles.add(dc);
        totalMemory.add(mem);
        
        if(!raw)
        {
            ifc.eval("stats");
        }
        else
        {
            agent.getPrinter().print("%f, %f, %d, %d\n", cpuTime, kernelTime, dc, mem);
        }
        agent.dispose();
    }
}

