/*
 * Copyright (c) 2010 Dave Ray <daveray@gmail.com>
 *
 * Created on Jul 24, 2010
 */
package org.jsoar.soarunit.ui;

import java.awt.Toolkit;
import java.awt.datatransfer.Clipboard;
import java.awt.datatransfer.ClipboardOwner;
import java.awt.datatransfer.StringSelection;
import java.awt.datatransfer.Transferable;
import java.awt.event.ActionEvent;
import java.net.MalformedURLException;
import java.net.URISyntaxException;

import javax.swing.AbstractAction;
import javax.swing.JOptionPane;

import org.jsoar.soarunit.SoarUnitCommand;
import org.jsoar.soarunit.Test;
import org.jsoar.util.UrlTools;

/**
 * @author ray
 */
public class CopyDebugTestToClipboardAction extends AbstractAction implements ClipboardOwner
{
    private static final long serialVersionUID = -3500496894588331412L;
    
    private final Test test;
    
    public CopyDebugTestToClipboardAction(Test test)
    {
        super("Copy debug command to clipboard");
        
        this.test = test;
    }


    /* (non-Javadoc)
     * @see java.awt.event.ActionListener#actionPerformed(java.awt.event.ActionEvent)
     */
    @Override
    public void actionPerformed(ActionEvent e)
    {
        final String command;
        try
        {
            command = String.format("%s --ui --debug \"%s\" \"%s\"",
                    SoarUnitCommand.NAME,
                    test.getName(),
                    UrlTools.toFile(test.getTestCase().getUrl()).getAbsolutePath().replace('\\', '/'));
            final StringSelection ss = new StringSelection(command);
            final Clipboard cb = Toolkit.getDefaultToolkit().getSystemClipboard();
            cb.setContents(ss, this);
        } catch (RuntimeException | MalformedURLException | URISyntaxException e1) {
            JOptionPane.showMessageDialog(null, e1.getMessage(), "Error creating debug test string", JOptionPane.ERROR_MESSAGE);
        }
    }

    /* (non-Javadoc)
     * @see java.awt.datatransfer.ClipboardOwner#lostOwnership(java.awt.datatransfer.Clipboard, java.awt.datatransfer.Transferable)
     */
    @Override
    public void lostOwnership(Clipboard clipboard, Transferable contents)
    {
    }
}
