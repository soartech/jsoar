/*
 * Copyright (c) 2008  Dave Ray <daveray@gmail.com>
 *
 * Created on Oct 23, 2008
 */
package org.jsoar.debugger;

import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.concurrent.Callable;

import javax.swing.BorderFactory;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JSplitPane;
import javax.swing.JTable;
import javax.swing.JToggleButton;
import javax.swing.JToolBar;
import javax.swing.ListSelectionModel;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.DefaultTableModel;
import javax.swing.table.TableModel;

import org.flexdock.docking.DockingConstants;
import org.jdesktop.swingx.JXTable;
import org.jdesktop.swingx.decorator.HighlighterFactory;
import org.jsoar.debugger.selection.SelectionListener;
import org.jsoar.debugger.selection.SelectionManager;
import org.jsoar.kernel.commands.StructuredPreferencesCommand;
import org.jsoar.kernel.commands.StructuredPreferencesCommand.Result;
import org.jsoar.kernel.commands.StructuredPreferencesCommand.ResultEntry;
import org.jsoar.kernel.memory.PreferenceType;
import org.jsoar.kernel.memory.Wme;
import org.jsoar.kernel.symbols.Identifier;
import org.jsoar.util.adaptables.Adaptables;

/**
 * Simple view that shows preferences for selected id or wme.
 * 
 * @author ray
 */
public class PreferencesView extends AbstractAdaptableView implements SelectionListener
{
    private static final long serialVersionUID = -5150761314645770374L;

    private final LittleDebugger debugger;
    private final SelectionManager selectionManager;
    private final JLabel info = new JLabel("No id or wme selected");
    private final JXTable table = new JXTable();
    private final JToggleButton objectToggle = new JToggleButton(Images.ID, false);
    
    private final JLabel source = new JLabel("");
    private final JXTable sourceWmeTable = new JXTable();
    
    //private JTextArea textArea = new JTextArea();
    
    public PreferencesView(LittleDebugger debuggerIn)
    {
        super("preferences", "Preferences");
        
        this.debugger = debuggerIn;
        this.selectionManager = debugger.getSelectionManager();
        
        addAction(DockingConstants.PIN_ACTION);
        
        this.table.setShowGrid(false);
        this.table.setHighlighters(HighlighterFactory.createAlternateStriping());
        this.table.setColumnControlVisible(true);
        this.table.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        this.table.setDefaultRenderer(PreferenceType.class, new DefaultTableCellRenderer() {

            private static final long serialVersionUID = 7768070935030196160L;

            @Override
            public Component getTableCellRendererComponent(JTable table, Object value, boolean isSelected,
                    boolean hasFocus, int row, int column)
            {
                Component c = super.getTableCellRendererComponent(table, value, isSelected, hasFocus, row, column);
                setText(((PreferenceType) value).getDisplayName());
                return c;
            }});
        
        this.table.getSelectionModel().addListSelectionListener(new ListSelectionListener() {

            @Override
            public void valueChanged(ListSelectionEvent e)
            {
                updateSource();
            }});
        
        JPanel barPanel = new JPanel(new BorderLayout());
        barPanel.add(info, BorderLayout.WEST);
        JToolBar bar = new JToolBar();
        bar.setFloatable(false);
        bar.add(objectToggle);
        objectToggle.setToolTipText("Treat Identifier as object");
        objectToggle.addActionListener(new ActionListener() {

            @Override
            public void actionPerformed(ActionEvent e)
            {
                selectionChanged(debugger.getSelectionManager());
            }});
        
        barPanel.add(bar, BorderLayout.EAST);
        
        JPanel p = new JPanel(new BorderLayout());
        p.add(barPanel, BorderLayout.NORTH);
        //p.add(new JScrollPane(textArea), BorderLayout.SOUTH);
        
        this.sourceWmeTable.setShowGrid(false);
        this.sourceWmeTable.setHighlighters(HighlighterFactory.createAlternateStriping());
        this.sourceWmeTable.setColumnControlVisible(true);
        
        JPanel sourcePanel = new JPanel(new BorderLayout());
        sourcePanel.setBorder(BorderFactory.createTitledBorder("Preference Source"));
        sourcePanel.add(source, BorderLayout.NORTH);
        sourcePanel.add(new JScrollPane(sourceWmeTable));
        
        JSplitPane split = new JSplitPane(JSplitPane.VERTICAL_SPLIT, true, 
                                          new JScrollPane(table), sourcePanel);
        split.setDividerSize(5);
        split.setResizeWeight(0.5);
        
        p.add(split, BorderLayout.CENTER);
        
        setContentPane(p);

        this.selectionManager.addListener(this);
        
        updateSource();
    }

    /* (non-Javadoc)
     * @see org.jsoar.debugger.selection.SelectionListener#selectionChanged(org.jsoar.debugger.selection.SelectionManager)
     */
    @Override
    public void selectionChanged(SelectionManager manager)
    {
        //textArea.setText(getPreferencesOutput(manager.getSelection()));
        //textArea.setCaretPosition(0);
        
        final Object selection = manager.getSelectedObject();
        Wme w = Adaptables.adapt(selection, Wme.class);
        Identifier id = Adaptables.adapt(selection, Identifier.class);
        if(w != null || id != null)
        {
            objectToggle.setEnabled(id != null);
            final boolean object = objectToggle.isSelected();
            updateInfo(w, id, object);
            
            final Result result = getPreferences(w, id, object);
            if(result.getError() == null)
            {
                table.setModel(new PreferencesTableModel(result));
            }
            else
            {
                table.setModel(new DefaultTableModel(new Object[][] { new Object[] {result.getError()} }, new Object[] { "" }));
            }
        }
        else
        {
            objectToggle.setEnabled(false);
            info.setText("No wme or id selected");
            table.setModel(new DefaultTableModel());
        }
        table.packAll();
    }
    
    private void updateInfo(final Wme w, final Identifier id, final boolean object)
    {
        if(w != null)
        {
            info.setText(String.format("<html><b>Preferences for <code>(%s  ^%s  *)</code></b></html>",
                    w.getIdentifier(), w.getAttribute()));
        }
        else if(id != null && object)
        {
            // Do (id ^* *)
            info.setText(String.format("<html><b>Preferences for <code>(%s  ^*  *)</code></b></html>", id));
        }
        else if(id != null && !object)
        {
            // Do (* ^* id)
            info.setText(String.format("<html><b>Preferences for <code>(*  ^*  %s)</code></b></html>", id));
        }
        else
        {
            throw new IllegalStateException("Unreachable code");
        }
    }
    
    private void updateSource()
    {
        final int r = table.getSelectedRow();
        final TableModel prefModel = table.getModel();
        if(r == -1 || !(prefModel instanceof PreferencesTableModel))
        {
            source.setText("No preference selected");
            sourceWmeTable.setModel(new DefaultTableModel());
            return;
        }
        ResultEntry e = ((PreferencesTableModel) prefModel).getResultEntry(r);
        source.setText("<html><b>Production:&nbsp;&nbsp;" + e.getSource() + "</b></html>");
        sourceWmeTable.setModel(new DefaultWmeTableModel(e.getSourceWmes()));
        sourceWmeTable.packAll();
    }
    
    private Result getPreferences(final Wme w, final Identifier id, final boolean object)
    {
        Callable<Result> callable = new Callable<Result>() {

            @Override
            public Result call() throws Exception
            {
                return safeGetPreferences(w, id, object);
            }};
        return debugger.getAgentProxy().execute(callable);
    }
    
    private Result safeGetPreferences(final Wme w, final Identifier id, boolean object)
    {
        
        StructuredPreferencesCommand c = new StructuredPreferencesCommand();
        if(w != null)
        {
            // Do (id ^attr *)
            return c.getPreferences(w.getIdentifier(), w.getAttribute());
        }
        else if(id != null && object)
        {
            // Do (id ^* *)
            return c.getPreferences(id, null);
        }
        else if(id != null && !object)
        {
            // Do (* ^* id)
            return c.getPreferencesForValue(debugger.getAgentProxy().getAgent(), id);
        }
        else
        {
            throw new IllegalStateException("Unreachable code");
        }
    }
    
//    private String getPreferencesOutput(final List<Object> selection)
//    {
//        Callable<String> matchCall = new Callable<String>() {
//
//            @Override
//            public String call() throws Exception
//            {
//                return safeGetPreferencesOutput(selection);
//            }};
//        return debugger.getAgentProxy().execute(matchCall);
//    }
//        
//    private String safeGetPreferencesOutput(List<Object> selection)
//    {
//        final Agent agent = debugger.getAgentProxy().getAgent();
//        final StringWriter writer = new StringWriter();
//        final Printer printer = new Printer(writer, true);
//        for(Object o : selection)
//        {
//            Wme w = Adaptables.adapt(o, Wme.class);
//            Identifier id = Adaptables.adapt(o, Identifier.class);
//            PrintPreferencesCommand command = new PrintPreferencesCommand();
//            command.setPrintProduction(true);
//            command.setWmeTraceType(WmeTraceType.FULL);
//            if(w != null)
//            {
//                command.setId(w.getIdentifier());
//                command.setAttr(w.getAttribute());
//            }
//            else if(id != null)
//            {
//                command.setId(id);
//                command.setObject(true);
//            }
//            else
//            {
//                continue;
//            }
//            
//            try
//            {
//                command.print(agent, printer);
//            }
//            catch (IOException e)
//            {
//                printer.error(e.getMessage());
//                e.printStackTrace();
//            }
//            printer.print("\n");
//            break;
//        }
//        
////        printer.print("*** preferences\n");
////        agent.soarReteListener.print_match_set(printer, 
////                WmeTraceType.FULL, 
////                EnumSet.of(MatchSetTraceType.MS_ASSERT, MatchSetTraceType.MS_RETRACT));
//        
//        return writer.toString();
//    }
}
