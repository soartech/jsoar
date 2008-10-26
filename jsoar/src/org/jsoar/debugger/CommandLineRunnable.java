/*
 * Copyright (c) 2008  Dave Ray <daveray@gmail.com>
 *
 * Created on Oct 23, 2008
 */
package org.jsoar.debugger;

import org.jsoar.tcl.SoarTclException;
import org.jsoar.tcl.SoarTclInterface;

/**
 * @author ray
 */
public class CommandLineRunnable implements Runnable
{
    private final SoarTclInterface ifc;
    private final String command;
    
    /**
     * @param proxy
     * @param ifc
     * @param command
     */
    public CommandLineRunnable(SoarTclInterface ifc, String command)
    {
        this.ifc = ifc;
        this.command = command;
    }



    /* (non-Javadoc)
     * @see java.lang.Runnable#run()
     */
    @Override
    public void run()
    {
        try
        {
            ifc.eval(command);
        }
        catch (SoarTclException e)
        {
            ifc.getAgent().getPrinter().error(e.getMessage() + "\n");
        }
    }

}
