package org.jsoar.debugger.stopcommand;

import org.jsoar.debugger.AboutDialog;
import org.jsoar.debugger.JSoarDebugger;
import org.jsoar.debugger.actions.AbstractDebuggerAction;
import org.jsoar.debugger.actions.ActionManager;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;

public class StopCommandAction extends AbstractDebuggerAction
{

    public StopCommandAction(ActionManager manager)
    {
        super(manager, "Stop Command");
    }

    /* (non-Javadoc)
     * @see org.jsoar.debugger.actions.AbstractDebuggerAction#update()
     */
    @Override
    public void update()
    {
    }

    /* (non-Javadoc)
     * @see java.awt.event.ActionListener#actionPerformed(java.awt.event.ActionEvent)
     */
    @Override
    public void actionPerformed(ActionEvent e)
    {
        getApplication().addStopCommandView();
    }
}
