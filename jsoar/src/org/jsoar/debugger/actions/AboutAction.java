/*
 * Copyright (c) 2008  Dave Ray <daveray@gmail.com>
 *
 * Created on Oct 25, 2008
 */
package org.jsoar.debugger.actions;

import java.awt.Component;
import java.awt.event.ActionEvent;

import javax.swing.SwingUtilities;

import org.jsoar.debugger.AboutDialog;

/**
 * @author ray
 */
public class AboutAction extends AbstractDebuggerAction
{
    private static final long serialVersionUID = -7639843952865259437L;

    public AboutAction(ActionManager manager)
    {
        super(manager, "About");
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
        AboutDialog.show(SwingUtilities.getRoot((Component)e.getSource()));
    }

}
