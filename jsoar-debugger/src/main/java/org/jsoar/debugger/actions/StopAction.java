/*
 * Copyright (c) 2008  Dave Ray <daveray@gmail.com>
 *
 * Created on Oct 25, 2008
 */
package org.jsoar.debugger.actions;

import java.awt.event.ActionEvent;

import javax.swing.KeyStroke;

import org.jsoar.debugger.Images;

/**
 * @author ray
 */
public class StopAction extends AbstractDebuggerAction
{
    private static final long serialVersionUID = -7639843952865259437L;

    public StopAction(ActionManager manager)
    {
        super(manager, "Stop", Images.STOP);
        
        setAcceleratorKey(KeyStroke.getKeyStroke("F6"));
    }

    /* (non-Javadoc)
     * @see org.jsoar.debugger.actions.AbstractDebuggerAction#update()
     */
    @Override
    public void update()
    {
        setEnabled(getApplication().getAgent().isRunning());
    }

    /* (non-Javadoc)
     * @see java.awt.event.ActionListener#actionPerformed(java.awt.event.ActionEvent)
     */
    @Override
    public void actionPerformed(ActionEvent arg0)
    {
        getApplication().getAgent().stop();
    }

}
