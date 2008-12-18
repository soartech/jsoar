package org.jsoar.tcl;

import java.util.Calendar;

import org.jsoar.kernel.Agent;
import org.jsoar.kernel.ProductionType;
import org.jsoar.kernel.tracing.Printer;

import tcl.lang.Command;
import tcl.lang.Interp;
import tcl.lang.TclException;
import tcl.lang.TclObject;

/**
 * @author ray
 */
final class StatsCommand implements Command
{
    /**
     * 
     */
    private final SoarTclInterface ifc;

    /**
     * @param ifc
     */
    StatsCommand(SoarTclInterface ifc)
    {
        this.ifc = ifc;
    }

    @Override
    public void cmdProc(Interp interp, TclObject[] args) throws TclException
    {
        final Agent agent = ifc.getAgent();
        final Printer p = agent.getPrinter();
        
        p.startNewLine();
        
        p.print("jsoar 0.0.0 on %s at %s%n%n", System.getenv("HOSTNAME"), Calendar.getInstance().getTime());
        p.print("%d productions (%d default, %d user, %d chunks)%n   + %d justifications%n",
                agent.getProductions().getProductions(null).size(),
                agent.getProductions().getProductions(ProductionType.DEFAULT).size(),
                agent.getProductions().getProductions(ProductionType.USER).size(),
                agent.getProductions().getProductions(ProductionType.CHUNK).size(),
                agent.getProductions().getProductions(ProductionType.CHUNK).size());
        p.print("\n");
        p.print("Values from single timers:%n" +
        		" Kernel CPU Time: %f sec. %n" +
        		" Total  CPU Time: %f sec. %n%n",
        		agent.getTotalKernelTimer().getTotalSeconds(),
        		agent.getTotalCpuTimer().getTotalSeconds());
        
        p.print("%d decisions%n" +
        		"%d elaboration cycles%n" +
        		"%d inner elaboration cycles%n" +
        		"%d p-elaboration cycles",
        		agent.decisionCycle.decision_phases_count,
        		agent.decisionCycle.e_cycle_count,
        		agent.decisionCycle.inner_e_cycle_count,
        		agent.decisionCycle.pe_cycle_count);
    }
}