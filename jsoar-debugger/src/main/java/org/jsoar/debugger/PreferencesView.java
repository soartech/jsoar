/*
 * Copyright (c) 2008  Dave Ray <daveray@gmail.com>
 *
 * Created on Oct 23, 2008
 */
package org.jsoar.debugger;

import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.event.ActionEvent;
import java.util.concurrent.Callable;

import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.JToolBar;
import javax.swing.ListSelectionModel;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.DefaultTableModel;
import javax.swing.table.TableModel;

import org.flexdock.docking.DockingConstants;
import org.jdesktop.swingx.JXTable;
import org.jdesktop.swingx.decorator.HighlighterFactory;
import org.jsoar.debugger.actions.AbstractDebuggerAction;
import org.jsoar.debugger.selection.SelectionListener;
import org.jsoar.debugger.selection.SelectionManager;
import org.jsoar.kernel.PredefinedSymbols;
import org.jsoar.kernel.commands.StructuredPreferencesCommand;
import org.jsoar.kernel.commands.StructuredPreferencesCommand.Result;
import org.jsoar.kernel.memory.PreferenceType;
import org.jsoar.kernel.memory.Wme;
import org.jsoar.kernel.symbols.Identifier;
import org.jsoar.runtime.CompletionHandler;
import org.jsoar.runtime.SwingCompletionHandler;
import org.jsoar.runtime.ThreadedAgent;
import org.jsoar.util.adaptables.Adaptables;

/**
 * Simple view that shows preferences for selected id or wme.
 * 
 * @author ray
 */
public class PreferencesView extends AbstractAdaptableView implements SelectionListener
{
    private static final long serialVersionUID = -5150761314645770374L;

    private final ThreadedAgent agent;
    private final SelectionManager selectionManager;
    private final JLabel info = new JLabel("No state selected");
    private final JXTable table = new JXTable();
    
    public PreferencesView(JSoarDebugger debuggerIn)
    {
        super("preferences", "Preferences");
        
        this.agent = debuggerIn.getAgentProxy();
        this.selectionManager = debuggerIn.getSelectionManager();
        
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
                setText(Character.toString(((PreferenceType) value).getIndicator()));
                return c;
            }});
        
        JPanel barPanel = new JPanel(new BorderLayout());
        barPanel.add(info, BorderLayout.WEST);
        JToolBar bar = createToolbar();
        
        barPanel.add(bar, BorderLayout.EAST);
        
        JPanel p = new JPanel(new BorderLayout());
        p.add(barPanel, BorderLayout.NORTH);
        p.add(new JScrollPane(table), BorderLayout.CENTER);
        
        setContentPane(p);

        this.selectionManager.addListener(this);
    }

    /**
     * @return
     */
    private JToolBar createToolbar()
    {
        JToolBar bar = new JToolBar();
        bar.setFloatable(false);
        
        bar.add(new AbstractDebuggerAction("Print to trace", Images.COPY) {
            private static final long serialVersionUID = -3614573079885324027L;

            {
                setToolTip("Print preferences to trace");
            }
            @Override
            public void update()
            {
            }

            @Override
            public void actionPerformed(ActionEvent arg0)
            {
                Result r = getLastResult();
                if(r != null)
                {
                    agent.getPrinter().startNewLine().print(r.getPrintResult()).flush();
                }
            }});
        return bar;
    }

    /* (non-Javadoc)
     * @see org.jsoar.debugger.selection.SelectionListener#selectionChanged(org.jsoar.debugger.selection.SelectionManager)
     */
    @Override
    public void selectionChanged(SelectionManager manager)
    {
        final Object selection = manager.getSelectedObject();
        Wme w = Adaptables.adapt(selection, Wme.class);
        Identifier id = Adaptables.adapt(selection, Identifier.class);
        if(w != null && id == null)
        {
            id = w.getIdentifier();
        }
        
        if(id != null && id.isGoal())
        {
            getPreferences(id);
        }
    }
    
    private Result getLastResult()
    {
        final TableModel prefModel = table.getModel();
        if(!(prefModel instanceof PreferencesTableModel))
        {
            return null;
        }
        
        return ((PreferencesTableModel) prefModel).getResult();
    }
    
    private void getPreferences(final Identifier id)
    {
        agent.execute(
            new Callable<Result>() {

                @Override
                public Result call() throws Exception
                {
                    return safeGetPreferences(id);
                }},
            SwingCompletionHandler.newInstance(new CompletionHandler<Result>() {

                @Override
                public void finish(Result result)
                {
                    finishGetPreferences(result);
                }}));
    }
    
    private void finishGetPreferences(Result result)
    {
        info.setText(String.format("<html><b>Operator preferences for <code>%s</code></b></html>", result.getQueryId()));
        if(result.getError() == null)
        {
            table.setModel(new PreferencesTableModel(result));
        }
        else
        {
            table.setModel(new DefaultTableModel(new Object[][] { new Object[] {result.getError()} }, new Object[] { "" }));
        }
        table.packAll();        
    }
    
    private Result safeGetPreferences(final Identifier id)
    {
        final PredefinedSymbols predefinedSyms = Adaptables.adapt(agent.getAgent(), PredefinedSymbols.class);
        final StructuredPreferencesCommand c = new StructuredPreferencesCommand();
        // Do (id ^operator *)
        return c.getPreferences(agent.getAgent(), id, predefinedSyms.operator_symbol);
    }
}
