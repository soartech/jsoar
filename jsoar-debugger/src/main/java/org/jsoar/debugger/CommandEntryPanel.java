/*
 * Copyright (c) 2008  Dave Ray <daveray@gmail.com>
 *
 * Created on Dec 17, 2008
 */
package org.jsoar.debugger;

import java.awt.AWTKeyStroke;
import java.awt.BorderLayout;
import java.awt.KeyboardFocusManager;
import java.awt.Point;
import java.awt.Toolkit;
import java.awt.event.ActionEvent;
import java.awt.event.FocusAdapter;
import java.awt.event.FocusEvent;
import java.awt.event.KeyEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.Collections;
import java.util.prefs.Preferences;

import javax.swing.AbstractAction;
import javax.swing.DefaultComboBoxModel;
import javax.swing.JList;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTextField;
import javax.swing.JToolTip;
import javax.swing.JWindow;
import javax.swing.KeyStroke;
import javax.swing.Popup;
import javax.swing.PopupFactory;
import javax.swing.SwingUtilities;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import javax.swing.text.BadLocationException;

import org.jdesktop.swingx.JXComboBox;
import org.jsoar.util.commands.SoarCommandCompletion;

import picocli.CommandLine;

/**
 * A panel with a command entry field and history.
 *
 * @author ray
 */
public class CommandEntryPanel extends JPanel implements Disposable
{
    private static final long serialVersionUID = 667991263123343775L;
    private static final long COMPLETION_DELAY = 500;

    private final JSoarDebugger debugger;
    private final DefaultComboBoxModel<String> model = new DefaultComboBoxModel<>();
    private final JXComboBox field = new JXComboBox(model);
    private final JWindow completions;

    private final JList<String> completionsList = new JList<>();
    private boolean completionsShowing = false;
    @SuppressWarnings("serial")
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
    @SuppressWarnings("serial")
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
    @SuppressWarnings("serial")
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
    private long lastInputTimestamp = System.currentTimeMillis();

    /**
     * Construct the panel with the given debugger
     *
     * @param debugger
     */
    @SuppressWarnings("serial")
    public CommandEntryPanel(JSoarDebugger debugger)
    {
        super(new BorderLayout());

        this.debugger = debugger;

        this.add(field, BorderLayout.CENTER);

        field.setEditable(true);
        field.getEditor().addActionListener(e -> execute());
        field.setFocusTraversalKeys(KeyboardFocusManager.FORWARD_TRAVERSAL_KEYS, Collections.<AWTKeyStroke>emptySet());

        completions = new JWindow(debugger.frame);
        //completions.setOpacity(0.8f); // some environments, like Linux Manjaro with KDE, do not support transparency, and it's not necessary here
        completions.setVisible(false);
        completions.setFocusable(true);
        completions.setAutoRequestFocus(false);
        completions.setFocusableWindowState(true);
        completionsScrollPane.setViewportView(completionsList);
        completions.add(completionsScrollPane);
        completions.pack();

        final String rawhistory = getPrefs().get("history", "").replace((char) 0, (char) 0x1F); // in case a null string is in the history, replace it with a unit separator; this likely to come up for users upgrading from old versions of the debugger
        final String[] history = rawhistory.split(String.valueOf((char) 0x1F)); // split on "unit separator" character (used to use null, but that's no longer supported in preference values in Java 9+)
        for (String s : history) {
            final String trimmed = s.trim();
            if (trimmed.length() > 0) {
                model.addElement(trimmed);
            }
        }

        final JTextField editorComponent = (JTextField) field.getEditor().getEditorComponent();
        editorComponent.getDocument().addDocumentListener(new DocumentListener()
        {
            @Override
            public void insertUpdate(DocumentEvent e)
            {

                int position = editorComponent.getCaretPosition();
                updateCompletionsDelay(field.getEditor().getItem().toString(), position);

            }

            @Override
            public void removeUpdate(DocumentEvent e)
            {
                try {
                    String text = e.getDocument().getText(0, e.getDocument().getLength());
                    updateCompletions(text,text.length());
                } catch (BadLocationException ignored) {
                }
            }

            @Override
            public void changedUpdate(DocumentEvent e)
            {
                int position = editorComponent.getCaretPosition();
                updateCompletionsDelay(field.getEditor().getItem().toString(),position);
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
        editorComponent.getActionMap().put("hide_completions", new AbstractAction()
        {
            @Override
            public void actionPerformed(ActionEvent e)
            {
                if (completionsShowing) {
                    hideCompletions();
                }
            }
        });

        editorComponent.getInputMap().put(KeyStroke.getKeyStroke(KeyEvent.VK_UP,Toolkit.getDefaultToolkit().getMenuShortcutKeyMask()),"up_complete");
        editorComponent.getInputMap().put(KeyStroke.getKeyStroke(KeyEvent.VK_DOWN,Toolkit.getDefaultToolkit().getMenuShortcutKeyMask()),"down_complete");
        editorComponent.getInputMap().put(KeyStroke.getKeyStroke('\t'),"complete_selected");
        editorComponent.getInputMap().put(KeyStroke.getKeyStroke(KeyEvent.VK_SPACE,Toolkit.getDefaultToolkit().getMenuShortcutKeyMask()),"show_completions");
        editorComponent.getInputMap().put(KeyStroke.getKeyStroke(KeyEvent.VK_ESCAPE,0),"hide_completions");
        completionsList.getInputMap().put(KeyStroke.getKeyStroke(KeyEvent.VK_ENTER,0), "complete_selected");

        completionsList.addMouseListener(new MouseAdapter()
        {
            @Override
            public void mouseClicked(MouseEvent e)
            {
                JList<?> list = (JList<?>)e.getSource();
                if (e.getClickCount() == 2) {
                    int selectedIndex = list.locationToIndex(e.getPoint());
                    useCompletion(selectedIndex);
                }
            }
        });
        
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
        debugger.getAgent().execute(() -> {
            return debugger.getAgent().getInterpreter().findCommand(command);
        }, commandLine -> {
            String help = getHelp(commandLine);
            SwingUtilities.invokeLater(() -> {
                if (commandLine == null){
                    if (tooltipPopup != null){
                        tooltipPopup.hide();
                        tooltipPopup = null;
                    }
                } else {
                    showHelpTooltip(help);
                }
            });
        });
        
    }

    private void updateCompletionsDelay(String command, int cursorPosition){
        if (!completionsShowing) {
            new Thread(() ->
            {
                lastInputTimestamp = System.currentTimeMillis();
                try {
                    Thread.sleep(COMPLETION_DELAY);
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                }
                long time = System.currentTimeMillis();
                if (lastInputTimestamp + COMPLETION_DELAY <= time) {
                    SwingUtilities.invokeLater(() ->
                            updateCompletions(command, cursorPosition));
                }
            }).start();
        } else {
            updateCompletions(command, cursorPosition);
        }
    }

    private void updateCompletions(String command, int cursorPosition)
    {
        String trimmedCommand = command.trim();
        if (!trimmedCommand.isEmpty())
        {
            return;
        }
        
        this.debugger.getAgent().execute(() -> {
            String[] commands = null;
            
            CommandLine commandLine = debugger.getAgent().getInterpreter().findCommand(trimmedCommand);
            if (commandLine == null) {
                commands = debugger.getAgent().getInterpreter().getCompletionList(trimmedCommand, cursorPosition);
            } else {
                commands = SoarCommandCompletion.complete(commandLine, trimmedCommand, cursorPosition);
            }
            
            final String[] finalCommands = commands;
            if (finalCommands != null && finalCommands.length > 0)
            {
                String help = getHelp(commandLine);
            
                SwingUtilities.invokeLater( () -> {
                    try {
                            completions.setVisible(true);
                            completionsList.setListData(finalCommands);
                            completionsScrollPane.doLayout();
                            Point location = field.getLocationOnScreen();
                            int yLoc = location.y + field.getHeight();
                            completions.setBounds(location.x, yLoc, 200, 100);
                            completions.toFront();
                            completionsList.setToolTipText("");
                            completionsShowing = true;
    
                            showHelpTooltip(help);
                        } catch (RuntimeException e) {
                            // ignore
                        }
                });
            } else {
                SwingUtilities.invokeLater( () -> {
                    completions.setVisible(false);
                    if (tooltipPopup != null) {
                        tooltipPopup.hide();
                        tooltipPopup = null;
                    }
                });
            }
            });
    }

    private void showHelpTooltip(String help)
    {
        int yLoc = completions.getY();
        int xLoc = completions.getX() + completions.getWidth();

        if (tooltipPopup != null) {
            tooltipPopup.hide();
            tooltipPopup = null;
        }

        if (help != null && !help.isEmpty()) {
            JToolTip toolTip = new JToolTip();
            toolTip.setTipText(help);
            PopupFactory popupFactory = PopupFactory.getSharedInstance();
            tooltipPopup = popupFactory.getPopup(field, toolTip, xLoc, yLoc);
            tooltipPopup.show();
        }
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
            debugger.getAgent().execute(() -> {
                debugger.getAgent().execute(new CommandLineRunnable(debugger, command), null);
                return null;
            }, null);
            
            addCommand(command);
        }
    }

    @SuppressWarnings("unchecked") // unfortunately, can't parameterize JXComboBox, even though it extends a generic type
    private void addCommand(String command)
    {
        field.removeItem(command);
        field.insertItemAt(command, 0);
        field.setSelectedIndex(0);
        field.getEditor().selectAll();
    }

    /**
     * NOTE: this must be executed on the Soar thread!
     */
    private String getHelp(CommandLine commandLine)
    {
        if (commandLine != null)
        {
            StringBuilder helpBuilder = new StringBuilder();
                
            CommandLine.Help help = new CommandLine.Help(commandLine.getCommandSpec(), commandLine.getColorScheme());
            helpBuilder
                    .append("<html>")
                    .append("<b>Usage:</b>")
                    .append("<br>")
                    .append(help.abbreviatedSynopsis())
                    .append("<br>")
                    .append("<b>Description</b>")
                    .append("<br>")
                    .append(help.description())
                    .append("<br>");
            if (!help.parameterList().isEmpty())
            {
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
}
