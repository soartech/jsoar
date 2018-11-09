/*
 * Copyright (c) 2008  Dave Ray <daveray@gmail.com>
 *
 * Created on Dec 17, 2008
 */
package org.jsoar.debugger;

import java.awt.*;
import java.awt.event.*;
import java.util.*;
import java.util.List;
import java.util.prefs.Preferences;

import javax.swing.*;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;

import org.jdesktop.swingx.*;
import org.jsoar.kernel.SoarException;
import org.jsoar.tcl.SoarTclException;
import org.jsoar.tcl.SoarTclInterface;
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
    private final JWindow completions;
    private final JXPanel help = new JXPanel();

    private final JList<String> completionsList = new JList<>();
    private boolean completionsShowing = false;
    private final AbstractAction selectUpAction = new AbstractAction()
    {
        @Override
        public void actionPerformed(ActionEvent e)
        {
            if (completionsShowing) {
                int selectedIndex = completionsList.getSelectedIndex();
                if (selectedIndex < 0 || selectedIndex >= completionsList.getModel().getSize() - 1) {
                    selectedIndex = 0;
                } else {
                    selectedIndex = selectedIndex - 1;
                }
                selectCompletion(selectedIndex);
            }
        }
    };
    private final AbstractAction selectDownAction = new AbstractAction()
    {
        @Override
        public void actionPerformed(ActionEvent e)
        {
            if (completionsShowing) {
                int selectedIndex = completionsList.getSelectedIndex();
                if (selectedIndex < 0 || selectedIndex >= completionsList.getModel().getSize() - 1) {
                    selectedIndex = 0;
                } else {
                    selectedIndex = selectedIndex + 1;
                }
                selectCompletion(selectedIndex);
            }
        }
    };
    private final AbstractAction completeSelectedAction = new AbstractAction()
    {
        @Override
        public void actionPerformed(ActionEvent e)
        {
            if (completionsShowing) {
                int selectedIndex = completionsList.getSelectedIndex();
                if (selectedIndex == -1 && completionsList.getModel().getSize() > 1) {
                    completionsList.setSelectedIndex(0);
                    completionsList.requestFocus();
                } else {
                    if (selectedIndex < 0 || selectedIndex > completionsList.getModel().getSize()) {
                        selectedIndex = 0;
                    }
                    useCompletion(selectedIndex);
                }
            }
        }
    };
    private Popup tooltipPopup;
    private final JScrollPane completionsScrollPane = new JScrollPane();

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
        /*completionsList.setCellRenderer(new ListCellRenderer<String>()
        {
            @Override
            public Component getListCellRendererComponent(JList<? extends String> list, String value, int index, boolean isSelected, boolean cellHasFocus)
            {
                return new JLabel(value);
            }
        });*/
//        completions.setUndecorated(true);
        completions = new JWindow(debugger.frame);
        completions.setOpacity(0.8f);
        completions.setVisible(false);
        completions.setFocusable(true);
        completions.setAutoRequestFocus(false);
        completions.setFocusableWindowState(true);
        completionsScrollPane.setViewportView(completionsList);
        completions.add(completionsScrollPane);
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

        //close the completions if we click out of the editor or the list
        editorComponent.addFocusListener(new FocusAdapter()
        {
            @Override
            public void focusLost(FocusEvent e)
            {
                super.focusLost(e);
                if (e.getOppositeComponent() != completions && e.getOppositeComponent() != completionsList) {
                    hideCompletions();
                }
            }
        });
        completions.addFocusListener(new FocusAdapter()
        {
            @Override
            public void focusLost(FocusEvent e)
            {
                super.focusLost(e);
                if (e.getOppositeComponent() != editorComponent) {
                    hideCompletions();
                }
            }
        });

        editorComponent.getActionMap().put("complete_selected", completeSelectedAction);
        completionsList.getActionMap().put("complete_selected",completeSelectedAction);
        editorComponent.getActionMap().put("down_complete", selectDownAction);
        editorComponent.getActionMap().put("up_complete", selectUpAction);
        editorComponent.getActionMap().put("show_completions", new AbstractAction()
        {
            @Override
            public void actionPerformed(ActionEvent e)
            {
                if (!completionsShowing) {
                    int position = editorComponent.getCaretPosition();
                    updateCompletions(field.getEditor().getItem().toString(), position);
                }
            }
        });


        editorComponent.getInputMap().put(KeyStroke.getKeyStroke(KeyEvent.VK_UP,Toolkit.getDefaultToolkit().getMenuShortcutKeyMask()),"up_complete");
        editorComponent.getInputMap().put(KeyStroke.getKeyStroke(KeyEvent.VK_DOWN,Toolkit.getDefaultToolkit().getMenuShortcutKeyMask()),"down_complete");
        editorComponent.getInputMap().put(KeyStroke.getKeyStroke('\t'),"complete_selected");
        editorComponent.getInputMap().put(KeyStroke.getKeyStroke(KeyEvent.VK_SPACE,Toolkit.getDefaultToolkit().getMenuShortcutKeyMask()),"show_completions");
        completionsList.getInputMap().put(KeyStroke.getKeyStroke(KeyEvent.VK_ENTER,0), "complete_selected");

        completionsList.addMouseListener(new MouseAdapter()
        {
            @Override
            public void mouseClicked(MouseEvent e)
            {
                JList list = (JList)e.getSource();
                if (e.getClickCount() == 2) {
                    int selectedIndex = list.locationToIndex(e.getPoint());
                    useCompletion(selectedIndex);
                }
            }
        });

//        SwingTools.addSelectAllOnFocus(field);

        final String rawhistory = getPrefs().get("history", "").replace((char) 0, (char) 0x1F); // in case a null string is in the history, replace it with a unit separator; this likely to come up for users upgrading from old versions of the debugger
        final String[] history = rawhistory.split(String.valueOf((char) 0x1F)); // split on "unit separator" character (used to use null, but that's no longer supported in preference values in Java 9+)
        for (String s : history) {
            final String trimmed = s.trim();
            if (trimmed.length() > 0) {
                model.addElement(trimmed);
            }
        }
    }

    public void useCompletion(int selectedIndex)
    {
        if (selectedIndex >= 0 && selectedIndex < completionsList.getModel().getSize()) {
            String completion = completionsList.getModel().getElementAt(selectedIndex);
            field.getEditor().setItem(completion);
            field.getEditor().getEditorComponent().requestFocus();
        }
    }

    public void selectCompletion(int selectedIndex)
    {
        completionsList.setSelectedIndex(selectedIndex);
        completionsList.ensureIndexIsVisible(selectedIndex);
        String command = completionsList.getSelectedValue();
        CommandLine commandLine = findCommand(command);
        if (commandLine == null){
            if (tooltipPopup != null){
                tooltipPopup.hide();
                tooltipPopup = null;
            }
        } else {
            showHelpTooltip(commandLine);
        }
    }

    private void updateCompletions(String command, int cursorPosition)
    {
        command = command.trim();
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
                    completionsScrollPane.doLayout();
                    Point location = field.getLocationOnScreen();
                    int yLoc = location.y+field.getHeight();
                    completions.setBounds(location.x, yLoc, 200, 100);
                    completions.toFront();
                    completionsList.setToolTipText("");
                    completionsShowing = true;

                    showHelpTooltip(commandLine);
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

    private void showHelpTooltip(CommandLine commandLine)
    {
        int yLoc = completions.getY();
        int xLoc = completions.getX() + completions.getWidth();

        JToolTip toolTip = new JToolTip();
        String help = getHelp(commandLine);

        if (tooltipPopup != null) {
            tooltipPopup.hide();
            tooltipPopup = null;
        }

        if (help != null && !help.isEmpty()) {
            toolTip.setTipText(help);
            PopupFactory popupFactory = PopupFactory.getSharedInstance();
            tooltipPopup = popupFactory.getPopup(field, toolTip, xLoc, yLoc);
            tooltipPopup.show();
        }
    }

    private CommandLine findCommand(String substring)
    {
        substring = substring.trim();
        if (!substring.isEmpty() ) {
            SoarCommand cmd = null;
            String[] parts = substring.split(" ");
            if (debugger.getAgent().getInterpreter() instanceof DefaultInterpreter) {
                DefaultInterpreter interpreter = ((DefaultInterpreter) debugger.getAgent().getInterpreter());
                cmd = interpreter.getCommand(parts[0]);
            } else if (debugger.getAgent().getInterpreter() instanceof SoarTclInterface) {
                SoarTclInterface interpreter = (SoarTclInterface) debugger.getAgent().getInterpreter();
                try {
                    cmd = interpreter.getCommand(parts[0], null);
                } catch (SoarException ignored){}
            }
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
        if (commandLine != null) {

            String[] parts = input.split(" ");
            int argIndex = 0;
            int positionInArg = 0;
            //figure out argIndex
            for (int i = 1; i < input.length(); i++){
                char c = input.charAt(i);
                char prev = input.charAt(i-1);
                if (c == ' ' &&  prev != ' '){
                    argIndex++;
                    positionInArg = 0;
                } else if (c != ' '){
                    positionInArg++;
                }
            }

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
