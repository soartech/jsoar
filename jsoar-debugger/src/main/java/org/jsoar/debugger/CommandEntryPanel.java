/*
 * Copyright (c) 2008  Dave Ray <daveray@gmail.com>
 *
 * Created on Dec 17, 2008
 */
package org.jsoar.debugger;

import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Set;
import java.util.prefs.Preferences;

import javax.swing.*;

import org.jdesktop.swingx.JXComboBox;
import org.jdesktop.swingx.autocomplete.AutoCompleteDecorator;
import org.jsoar.util.SwingTools;
import org.jsoar.util.commands.DefaultInterpreter;
import org.jsoar.util.commands.SoarCommand;
import org.jsoar.util.commands.SoarCommandInterpreter;
import picocli.CommandLine;

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
    private final JXComboBox field = new JXComboBox(model);
    
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
        field.setFocusTraversalKeys(KeyboardFocusManager.FORWARD_TRAVERSAL_KEYS, Collections.<AWTKeyStroke>emptySet());
//        AutoCompleteDecorator.decorate(field);

        field.getEditor().getEditorComponent().addKeyListener(new KeyAdapter() {
            @Override
            public void keyPressed(KeyEvent e) {

                if (e.getKeyChar() == KeyEvent.VK_TAB) {
                    final String command = field.getEditor().getItem().toString();
                    if (!command.isEmpty()) {
//                    field.getEditor().getEditorComponent().
                        String[] commands = complete(command.trim());
                        model.removeAllElements();
                        field.getEditor().setItem(command);
                        for (String c : commands) {
                            model.addElement(c);
                        }
                        AutoCompleteDecorator.decorate(field);

                    }


                } else {
                    super.keyTyped(e);
                }
            }
        });


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
    }

    private String[] complete(String input){
        if (debugger.getAgent().getInterpreter() instanceof DefaultInterpreter){
            ArrayList<CharSequence> results = new ArrayList<>();

            DefaultInterpreter interpreter = ((DefaultInterpreter) debugger.getAgent().getInterpreter());
            String[] parts = input.split(" ");

            Set<String> commandStrings = interpreter.getCommandStrings();
            for (String cmd: commandStrings){
                if (cmd.startsWith(parts[0])){
                    results.add(cmd);
                }
            }
            if (results.size() == 1 && results.get(0).length() == input.trim().length()){
                SoarCommand soarCmd = interpreter.getCommand(results.get(0).toString());
                ArrayList<CharSequence> longResults = new ArrayList<>();
                if (soarCmd.getCommand() != null){
                    CommandLine commandLine = new CommandLine(soarCmd.getCommand());
                    picocli.AutoComplete.complete(commandLine.getCommandSpec(),parts,0,0,input.length(),longResults);
                }
                for (int i = 0; i < longResults.size(); i++){
                    longResults.set(i,results.get(0)+" "+longResults.get(i));
                }
                results = longResults;
            }
            return results.toArray(new String[results.size()]);
        }

        return null;
    }
}
