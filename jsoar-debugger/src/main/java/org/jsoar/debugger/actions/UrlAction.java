/*
 * Copyright (c) 2008  Dave Ray <daveray@gmail.com>
 *
 * Created on Oct 25, 2008
 */
package org.jsoar.debugger.actions;

import java.awt.Desktop;
import java.awt.event.ActionEvent;
import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;

import javax.swing.JOptionPane;

/**
 * An action that opens a URL in the default browser
 * 
 * @author ray
 */
public class UrlAction extends AbstractDebuggerAction
{
    private static final long serialVersionUID = -7639843952865259437L;

    private final String url;
    
    /**
     * @param manager the action manager
     * @param name the text of the action
     * @param url the url to open
     */
    public UrlAction(ActionManager manager, String name, String url)
    {
        super(manager, name);
        
        this.url = url;
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
        try
        {
            Desktop.getDesktop().browse(new URI(this.url));
        }
        catch (IOException e1)
        {
            JOptionPane.showMessageDialog(getApplication(), e1.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
        }
        catch (URISyntaxException e1)
        {
            JOptionPane.showMessageDialog(getApplication(), e1.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
        }
    }

}
