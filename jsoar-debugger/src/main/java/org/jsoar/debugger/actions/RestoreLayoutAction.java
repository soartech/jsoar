/*
 * Copyright (c) 2008  Dave Ray <daveray@gmail.com>
 *
 * Created on Oct 25, 2008
 */
package org.jsoar.debugger.actions;

import java.awt.event.ActionEvent;

/**
 * @author ray
 */
public class RestoreLayoutAction extends AbstractDebuggerAction
{
    private static final long serialVersionUID = -7639843952865259437L;

    public RestoreLayoutAction(ActionManager manager)
    {
        super(manager, "Restore layout");
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
    public void actionPerformed(ActionEvent arg0)
    {
        getApplication().restoreLayout();
    }

}
