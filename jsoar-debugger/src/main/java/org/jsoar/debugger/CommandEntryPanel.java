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
        
        final String[] history = getPrefs().get("history", "").split("\\00"); // split on null character
        for(String s : history)
        {
            final String trimmed = s.trim();
            if(trimmed.length() > 0)
            {
                model.addElement(trimmed);
            }
        }
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
                b.append((char) 0); // null-separated strings
            }
            b.append(model.getElementAt(i));
            first = false;
        }
        
        getPrefs().put("history", b.toString());
    }

    private Preferences getPrefs()
    {
        return JSoarDebugger.PREFERENCES.node("commands");
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
