/*
 * Copyright (c) 2008  Dave Ray <daveray@gmail.com>
 *
 * Created on Oct 25, 2008
 */
package org.jsoar.debugger;

import java.awt.Component;

import javax.swing.JOptionPane;

import org.jsoar.kernel.JSoarVersion;

/**
 * @author ray
 */
public class AboutDialog
{
    public static void show(Component parent)
    {
        // Someday we'll make a fancy about box. For now, this will do.
        JSoarVersion version = JSoarVersion.getInstance();
        String message = "<html>" + 
        "<b>JSoar Demonstration Debugger</b><br>" +
        "Copyright 2009, Dave Ray &lt;daveray@gmail.com><br>" +
        "<br>" +
        "<b>Version</b>: " + version + "<br>" +
        "<b>Built on</b>: " + version.getBuildDate() + "<br>" + 
        "<b>Built by</b>: " + version.getBuiltBy() + "<br>" +
        "<b>SVN URL</b>: " + version.getSvnUrl() + "<br>" +
        "<b>SVN revision</b>: " + version.getSvnRevision() +
        "</html>";
        JOptionPane.showMessageDialog(parent, message, "About JSoar", 
                                      JOptionPane.PLAIN_MESSAGE);
    }
}
