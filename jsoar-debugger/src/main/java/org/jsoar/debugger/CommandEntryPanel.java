/*
 * Copyright (c) 2008  Dave Ray <daveray@gmail.com>
 *
 * Created on Dec 17, 2008
 */
package org.jsoar.debugger;

import java.awt.BorderLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

import javax.swing.JComboBox;
import javax.swing.JPanel;

/**
 * A panel with a command entry field and history.
 * 
 * @author ray
 */
public class CommandEntryPanel extends JPanel
{
    private static final long serialVersionUID = 667991263123343775L;
    
    private final JSoarDebugger debugger;
    private final JComboBox field = new JComboBox();
    
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
    }
    
    private void execute()
    {
        final String command = field.getEditor().getItem().toString().trim();
        if(command.length() > 0)
        {
            debugger.getAgentProxy().execute(new CommandLineRunnable(debugger, command), null);
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
