/*
 * Copyright (c) 2008  Dave Ray <daveray@gmail.com>
 *
 * Created on Oct 25, 2008
 */
package org.jsoar.debugger.actions;

import java.awt.event.ActionEvent;

import javax.swing.KeyStroke;

import org.jsoar.debugger.RunControlModel;
import org.jsoar.kernel.RunType;

/**
 * @author ray
 */
public class StepAction extends AbstractDebuggerAction
{
    private static final long serialVersionUID = -1460902354871319429L;

    private final RunType runType;
    
    /**
     * @param manager the owning action manager
     */
    public StepAction(ActionManager manager, String text, RunType runType, String keyStroke)
    {
        super(manager, text);
        setAcceleratorKey(KeyStroke.getKeyStroke(keyStroke));
        this.runType = runType;
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
        model.setType(runType);
        getApplication().getAgent().runFor(1, model.getType());
        getApplication().updateActionsAndStatus();
    }

}
