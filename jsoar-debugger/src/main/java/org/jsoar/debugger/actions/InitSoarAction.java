/*
 * Copyright (c) 2008  Dave Ray <daveray@gmail.com>
 *
 * Created on Oct 25, 2008
 */
package org.jsoar.debugger.actions;

import java.awt.event.ActionEvent;

import org.jsoar.debugger.Images;

/**
 * @author ray
 */
public class InitSoarAction extends AbstractDebuggerAction
{
    private static final long serialVersionUID = -1460902354871319429L;

    /**
     * @param manager the owning action manager
     */
    public InitSoarAction(ActionManager manager)
    {
        super(manager, "init-soar", Images.UNDO);
        
        setToolTip("init-soar");
    }

    /* (non-Javadoc)
     * @see org.jsoar.debugger.actions.AbstractDebuggerAction#update()
     */
    @Override
    public void update()
    {
        setEnabled(!getApplication().getAgentProxy().isRunning());
    }

    /* (non-Javadoc)
     * @see java.awt.event.ActionListener#actionPerformed(java.awt.event.ActionEvent)
     */
    @Override
    public void actionPerformed(ActionEvent arg0)
    {
        getApplication().getAgentProxy().initialize();
    }

}
