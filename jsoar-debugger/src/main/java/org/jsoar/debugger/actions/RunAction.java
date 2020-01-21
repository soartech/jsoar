/*
 * Copyright (c) 2008  Dave Ray <daveray@gmail.com>
 *
 * Created on Oct 25, 2008
 */
package org.jsoar.debugger.actions;

import java.awt.event.ActionEvent;

import javax.swing.KeyStroke;

import org.jsoar.debugger.Images;
import org.jsoar.debugger.RunControlModel;

/**
 * @author ray
 */
public class RunAction extends AbstractDebuggerAction
{
    private static final long serialVersionUID = -1460902354871319429L;

    /**
     * @param manager the owning action manager
     */
    public RunAction(ActionManager manager)
    {
        super(manager, "Run", Images.START);
        setAcceleratorKey(KeyStroke.getKeyStroke("F5"));
    }

    /* (non-Javadoc)
     * @see org.jsoar.debugger.actions.AbstractDebuggerAction#update()
     */
    @Override
    public void update()
    {
        setEnabled(!getApplication().getAgent().isRunning());
    }

    /* (non-Javadoc)
     * @see java.awt.event.ActionListener#actionPerformed(java.awt.event.ActionEvent)
     */
    @Override
    public void actionPerformed(ActionEvent arg0)
    {
        final RunControlModel model = getApplication().getRunControlModel();
        getApplication().getAgent().runFor(model.getCount(), model.getType());
        getApplication().updateActionsAndStatus();
    }

}
