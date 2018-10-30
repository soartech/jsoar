/*
 * Copyright (c) 2008  Dave Ray <daveray@gmail.com>
 *
 * Created on Dec 17, 2008
 */
package org.jsoar.debugger;

import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.*;
import java.util.List;
import java.util.prefs.Preferences;

import javax.swing.*;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import javax.swing.text.Element;

import com.sun.xml.internal.fastinfoset.stax.events.CharactersEvent;
import org.jdesktop.swingx.*;
import org.jsoar.kernel.SoarException;
import org.jsoar.tcl.SoarTclInterface;
import org.jsoar.util.SwingTools;
import org.jsoar.util.commands.DefaultInterpreter;
import org.jsoar.util.commands.SoarCommand;
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
    private final JWindow completions = new JWindow();
    private final JXPanel help = new JXPanel();

    private final JList<String> completionsList = new JList<>();
    private boolean completionsShowing = false;
    private Popup tooltipPopup;

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
        field.getEditor().addActionListener(new ActionListener()
        {

            @Override
            public void actionPerformed(ActionEvent e)
            {
                execute();
            }
        });
        field.setFocusTraversalKeys(KeyboardFocusManager.FORWARD_TRAVERSAL_KEYS, Collections.<AWTKeyStroke>emptySet());
//        AutoCompleteDecorator.decorate(field);
        completionsList.setCellRenderer(new ListCellRenderer<String>()
        {
            @Override
            public Component getListCellRendererComponent(JList<? extends String> list, String value, int index, boolean isSelected, boolean cellHasFocus)
            {
                return new JLabel(value);
            }
        });
//        completions.setUndecorated(true);
        completions.setOpacity(0.8f);
        completions.setVisible(false);
        completions.add(new ScrollPane().add(completionsList));
        completions.pack();


        final JTextField editorComponent = (JTextField) field.getEditor().getEditorComponent();
        editorComponent.getDocument().addDocumentListener(new DocumentListener()
        {
            @Override
            public void insertUpdate(DocumentEvent e)
            {
                int position = editorComponent.getCaretPosition();
                updateCompletions(field.getEditor().getItem().toString(),position);
            }

            @Override
            public void removeUpdate(DocumentEvent e)
            {
                //fixme - backspace seems to be inconsistent on what position and offset are
                /*
                int position = editorComponent.getCaretPosition();
                //fix position with editor removals
                if ( position > e.getOffset()){
                    position -= e.getLength();
                }
                updateCompletions(field.getEditor().getItem().toString(),position);
                */
            }

            @Override
            public void changedUpdate(DocumentEvent e)
            {
                int position = editorComponent.getCaretPosition();
                updateCompletions(field.getEditor().getItem().toString(),position);
            }
        });

        SwingTools.addSelectAllOnFocus(field);

        final String rawhistory = getPrefs().get("history", "").replace((char) 0, (char) 0x1F); // in case a null string is in the history, replace it with a unit separator; this likely to come up for users upgrading from old versions of the debugger
        final String[] history = rawhistory.split(String.valueOf((char) 0x1F)); // split on "unit separator" character (used to use null, but that's no longer supported in preference values in Java 9+)
        for (String s : history) {
            final String trimmed = s.trim();
            if (trimmed.length() > 0) {
                model.addElement(trimmed);
            }
        }
    }

    private void updateCompletions(String command, int cursorPosition)
    {
        if (!command.isEmpty()) {
//            updateCompletions(command);
//                    field.getEditor().getEditorComponent().
            CommandLine commandLine = findCommand(command);
            String[] commands = null;

            //if we don't have a full command, we can't use the picocli completion functionality
            if (commandLine == null){
                if (debugger.getAgent().getInterpreter() instanceof DefaultInterpreter){
                    DefaultInterpreter interp = ((DefaultInterpreter) debugger.getAgent().getInterpreter());
                    List<String> commandsList = new ArrayList<>();
                    for(String s: interp.getCommandStrings()){
                        if (s.startsWith(command)){
                            commandsList.add(s);
                        }
                    }
                    commands = new String[commandsList.size()];
                    commands = commandsList.toArray(commands);
                } else if (debugger.getAgent().getInterpreter() instanceof SoarTclInterface) {
                    SoarTclInterface interp = ((SoarTclInterface) debugger.getAgent().getInterpreter());
                    try {
                        String commandsStr = interp.eval("info commands") + " " + interp.eval("info procs");
                        List<String> commandsList = new ArrayList<>();
                        for (String s: commandsStr.split(" ")){
                            if (s.startsWith(command)){
                                commandsList.add(s);
                            }
                        }
                        commands = new String[commandsList.size()];
                        commands = commandsList.toArray(commands);
                    } catch (SoarException e) {
                        e.printStackTrace();
                    }
                }
            } else {
                //if we do have a full command, this will work
                commands = complete(commandLine, command, cursorPosition);

            }

            if (commands != null && commands.length > 0) {
                try {
                    completions.setVisible(true);
                    completionsList.setListData(commands);
                    Point location = field.getLocationOnScreen();
                    int yLoc = location.y+field.getHeight();
                    completions.setBounds(location.x, yLoc, 200, 100);
                    completions.toFront();
                    completionsList.setToolTipText("");
                    completionsShowing = true;
                    JToolTip toolTip = new JToolTip();
                    String help = getHelp(commandLine);

                    if (tooltipPopup != null) {
                        tooltipPopup.hide();
                        tooltipPopup = null;
                    }

                    if (help != null && !help.isEmpty()) {
                        toolTip.setTipText(help);
                        PopupFactory popupFactory = PopupFactory.getSharedInstance();
                        int xLoc = completions.getX() + completions.getWidth();

                        tooltipPopup = popupFactory.getPopup(field, toolTip, xLoc, yLoc);
                        tooltipPopup.show();
                    }
                } catch (Exception e) {

                }
            } else {
                completions.setVisible(false);
                if (tooltipPopup != null) {
                    tooltipPopup.hide();
                    tooltipPopup = null;
                }
            }
        }
    }

    private CommandLine findCommand(String substring)
    {
        if (debugger.getAgent().getInterpreter() instanceof DefaultInterpreter) {

            DefaultInterpreter interpreter = ((DefaultInterpreter) debugger.getAgent().getInterpreter());
            String[] parts = substring.split(" ");
            SoarCommand cmd = interpreter.getCommand(parts[0]);
            if (cmd != null && cmd.getCommand() != null) {
                CommandLine command = new CommandLine(cmd.getCommand());

                int part = 1;
                while (part < parts.length && command.getSubcommands().containsKey(parts[part])) {
                    command = command.getSubcommands().get(parts[part]);
                }
                return command;

            }
        }
        return null;
    }




    private void hideCompletions()
    {
        if (completionsShowing) {
            completions.setVisible(false);
            completionsShowing = false;
        }
        if (tooltipPopup != null) {
            tooltipPopup.hide();
            tooltipPopup = null;
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
        for (int i = 0; i < model.getSize() && i < 20; ++i) {
            if (!first) {
                b.append((char) 0x1F); // separate strings using the "unit separator" character (used to use null, but no longer supported in key values in Java 9+)
            }
            b.append(model.getElementAt(i));
            first = false;
        }

        try {
            String history = b.toString();
            history.replace((char) 0, (char) 0x1F); // in case a null string is in the history, replace it with a unit separator; this likely to come up for users upgrading from old versions of the debugger
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
        if (command.length() > 0) {
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


    private String getHelp(CommandLine commandLine)
    {
        if (commandLine != null) {
            CommandLine.Help help = new CommandLine.Help(commandLine.getCommandSpec(), new CommandLine.Help.ColorScheme());
            StringBuilder helpBuilder = new StringBuilder()
                    .append("<html>")
                    .append("<b>Usage:</b>")
                    .append("<br>")
                    .append(help.abbreviatedSynopsis())
                    .append("<br>")
                    .append("<b>Description</b>")
                    .append("<br>")
                    .append(help.description())
                    .append("<br>");
            if (!help.parameterList().isEmpty()) {
                helpBuilder.append("<b>Parameters:</b>")
                           .append("<br>")
                           .append(help.parameterList().replaceAll("\n", "<br>"))
                           .append("<br>");
            }
            helpBuilder.append("<b>Options:</b>")
                       .append("<br>")
                       .append(help.optionList().replaceAll("\n", "<br>"))
                       .append("<b>Commands:</b>")
                       .append("<br>")
                       .append(help.commandList().replaceAll("\n", "<br>"))
                       .append("</html>");
            return helpBuilder.toString();

        }
        return "";
    }

    private String[] complete(CommandLine commandLine, String input, int cursorPosition)
    {
        if (debugger.getAgent().getInterpreter() instanceof DefaultInterpreter && commandLine != null) {

            String[] parts = input.split(" ");
            int argIndex = 0;
            int positionInArg = cursorPosition;
            //figure out argIndex
            for (String part: parts){

                if (positionInArg < part.length()) {
                    break;
                } else {
                    argIndex++;
                    positionInArg-=part.length()+1;
                }
            }
            positionInArg+=1;//offset by 1 because we want the cursor after the character

            ArrayList<CharSequence> longResults = new ArrayList<>();
            System.out.println("argIndex: "+argIndex+", position: "+positionInArg+", command: "+input);
            picocli.AutoComplete.complete(commandLine.getCommandSpec(), parts, argIndex, positionInArg, input.length(), longResults);

            for (int i = 0; i < longResults.size(); i++) {
                longResults.set(i, input + longResults.get(i));
            }

            if (longResults.isEmpty()){
                longResults.add(input);
            }

            return longResults.toArray(new String[0]);
        }

        return null;
    }
}
