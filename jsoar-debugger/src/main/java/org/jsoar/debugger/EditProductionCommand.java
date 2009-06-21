/*
 * Copyright (c) 2009 Dave Ray <daveray@gmail.com>
 *
 * Created on Jun 20, 2009
 */
package org.jsoar.debugger;

import org.jsoar.kernel.SoarException;
import org.jsoar.util.adaptables.Adaptables;
import org.jsoar.util.commands.SoarCommand;

/**
 * @author ray
 */
public class EditProductionCommand implements SoarCommand
{
    private final JSoarDebugger debugger;

    public EditProductionCommand(JSoarDebugger debugger)
    {
        this.debugger = debugger;
    }

    /* (non-Javadoc)
     * @see org.jsoar.util.commands.SoarCommand#execute(java.lang.String[])
     */
    @Override
    public String execute(String[] args) throws SoarException
    {
        if(args.length != 2)
        {
            throw new SoarException("Expected a single production name");
        }
        final ProductionEditView view = Adaptables.adapt(debugger, ProductionEditView.class);
        if(view != null)
        {
            view.editProduction(args[1]);
        }
        else
        {
            throw new SoarException("Could not locate production editor");
        }
        
        return "";
    }

}
