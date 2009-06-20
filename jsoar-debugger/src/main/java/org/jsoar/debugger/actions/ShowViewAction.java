/*
 * Copyright (c) 2009 Dave Ray <daveray@gmail.com>
 *
 * Created on Jun 20, 2009
 */
package org.jsoar.debugger.actions;

import java.awt.event.ActionEvent;

import javax.swing.KeyStroke;

import org.flexdock.view.View;

/**
 * @author ray
 */
public class ShowViewAction extends AbstractDebuggerAction
{
    private static final long serialVersionUID = -2280441315098977116L;
    
    private final View view;
    
    /**
     * @param view
     */
    public ShowViewAction(View view, String key)
    {
        super(view.getTitle());
        
        this.view = view;
        
        setAcceleratorKey(KeyStroke.getKeyStroke(key));
    }

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
        view.setVisible(true);
        view.setActive(true);
    }

}
