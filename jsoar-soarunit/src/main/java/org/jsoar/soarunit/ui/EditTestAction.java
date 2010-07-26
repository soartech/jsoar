/*
 * Copyright (c) 2010 Dave Ray <daveray@gmail.com>
 *
 * Created on Jul 24, 2010
 */
package org.jsoar.soarunit.ui;

import java.awt.Desktop;
import java.awt.event.ActionEvent;
import java.io.IOException;

import javax.swing.AbstractAction;
import javax.swing.JOptionPane;

import org.jsoar.soarunit.Test;

/**
 * @author ray
 */
public class EditTestAction extends AbstractAction
{
    private static final long serialVersionUID = -3500496894588331412L;
    
    private final Test test;
    
    public EditTestAction(Test test)
    {
        super("Edit");
        
        this.test = test;
    }


    /* (non-Javadoc)
     * @see java.awt.event.ActionListener#actionPerformed(java.awt.event.ActionEvent)
     */
    @Override
    public void actionPerformed(ActionEvent e)
    {
        editTest(test);
    }


    public static void editTest(Test test)
    {
        try
        {
            Desktop.getDesktop().edit(test.getSuite().getFile());
        }
        catch (IOException e1)
        {
            JOptionPane.showMessageDialog(null, e1.getMessage(), "Error opening test", JOptionPane.ERROR_MESSAGE);
        }
    }

}
