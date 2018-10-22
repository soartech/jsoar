/*
 * Copyright (c) 2008  Dave Ray <daveray@gmail.com>
 *
 * Created on Dec 17, 2008
 */
package org.jsoar.debugger;

import java.awt.BorderLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.prefs.Preferences;

import javax.swing.DefaultComboBoxModel;
import javax.swing.JComboBox;
import javax.swing.JPanel;

import org.jsoar.util.SwingTools;

/**
 * A panel with a command entry field and history.
 * 
 * @author ray
 */
public class CommandEntryPanel extends JPanel implements Disposable
{
    private static final long serialVersionUID = 667991263123343775L;
    
    private final JSoarDebugger debugger;
    private final DefaultComboBoxModel model = new DefaultComboBoxModel();
    private final JComboBox field = new JComboBox(model);
    
    /**
     * Construct the panel with the given debugger
     * 
     * @param debugger
     */
    public CommandEntryPanel(JSoarDebugger debugger)
    {
        super(new BorderLayout());
        
        this.debugger = debugger;
        
        this.add(field, BorderLayout.CENTER);
        
        field.setEditable(true);
        field.getEditor().addActionListener(new ActionListener() {

            @Override
            public void actionPerformed(ActionEvent e)
            {
                execute();
            }});
        SwingTools.addSelectAllOnFocus(field);
        
        final String rawhistory = getPrefs().get("history", "").replace((char)0, (char)0x1F); // in case a null string is in the history, replace it with a unit separator; this likely to come up for users upgrading from old versions of the debugger
        final String[] history = rawhistory.split(String.valueOf((char) 0x1F)); // split on "unit separator" character (used to use null, but that's no longer supported in preference values in Java 9+)
        for(String s : history)
        {
            final String trimmed = s.trim();
            if(trimmed.length() > 0)
            {
                model.addElement(trimmed);
            }
        }
    }
    
    public void giveFocus()
    {
        field.requestFocusInWindow();
    }
    
    /* (non-Javadoc)
     * @see org.jsoar.debugger.Disposable#dispose()
     */
    @Override
    public void dispose()
    {
        final StringBuilder b = new StringBuilder();
        boolean first = true;
        for(int i = 0; i < model.getSize() && i < 20; ++i)
        {
            if(!first)
            {
                b.append((char) 0x1F); // separate strings using the "unit separator" character (used to use null, but no longer supported in key values in Java 9+)
            }
            b.append(model.getElementAt(i));
            first = false;
        }
        
        try {
        	String history = b.toString();
        	history.replace((char)0, (char)0x1F); // in case a null string is in the history, replace it with a unit separator; this likely to come up for users upgrading from old versions of the debugger
        	getPrefs().put("history", b.toString());
        } catch (IllegalArgumentException e) {
        	// somehow the history is invalid, so don't save it
        	getPrefs().put("history", "");
        }
    }

    private Preferences getPrefs()
    {
        return JSoarDebugger.getPreferences().node("commands");
    }

    private void execute()
    {
        final String command = field.getEditor().getItem().toString().trim();
        if(command.length() > 0)
        {
            debugger.getAgent().execute(new CommandLineRunnable(debugger, command), null);
            addCommand(command);
        }
    }
    
    private void addCommand(String command)
    {
        field.removeItem(command);
        field.insertItemAt(command, 0);
        field.setSelectedIndex(0);
        field.getEditor().selectAll();
    }}
