/*
 * Copyright (c) 2008  Dave Ray <daveray@gmail.com>
 *
 * Created on Sep 29, 2008
 */
package org.jsoar.kernel;

import android.content.Context;

import org.jsoar.util.NullWriter;
import org.jsoar.util.commands.SoarCommandInterpreter;
import org.jsoar.util.commands.SoarCommands;

import java.util.ArrayList;
import java.util.List;

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
     * @throws SoarException 
     */
    private static void doRun(String[] args, Context androidContext) throws SoarException
    {
        Agent agent = new Agent(androidContext);
        agent.getTrace().setEnabled(false);
        agent.getPrinter().pushWriter(new NullWriter());
        SoarCommandInterpreter ifc = agent.getInterpreter();
        
        boolean raw = false, runs = false;
        long decisions = -1;
        for(String arg : args)
        {
            if(decisions == 0) 
            {
                decisions = Integer.valueOf(arg);
                if(decisions == 0) 
                    decisions = -1;
            }
            else if(runs) { runs = false; /* skip arg */ }
            else if("--raw".equals(arg))
            {
                raw = true;
            }
            else if("--decisions".equals(arg))
            {
                decisions = 0;
            }
            else if("--runs".equals(arg))
            {
                runs = true;
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

