/*
 * Copyright (c) 2008  Dave Ray <daveray@gmail.com>
 *
 * Created on Oct 23, 2008
 */
package org.jsoar.debugger;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.event.ActionEvent;
import java.util.concurrent.Callable;

import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.JToggleButton;
import javax.swing.JToolBar;
import javax.swing.ListSelectionModel;
import javax.swing.SwingConstants;
import javax.swing.table.DefaultTableCellRenderer;

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
import org.jsoar.kernel.symbols.Symbol;
import org.jsoar.runtime.CompletionHandler;
import org.jsoar.runtime.SwingCompletionHandler;
import org.jsoar.runtime.ThreadedAgent;
import org.jsoar.util.SwingTools;
import org.jsoar.util.adaptables.Adaptables;

/**
 * Simple view that shows preferences for selected id or wme.
 * 
 * @author ray
 */
public class PreferencesView extends AbstractAdaptableView implements SelectionListener, Refreshable, Disposable
{
    private static final long serialVersionUID = -5150761314645770374L;

    private final ThreadedAgent agent;
    private final SelectionManager selectionManager;
    private final JPanel panel = new JPanel(new BorderLayout());
    private final JLabel info = new JLabel(" No state selected");
    private final PreferencesTableModel tableModel = new PreferencesTableModel();
    private final JXTable table = new JXTable(tableModel);
    private final JScrollPane tableScrollPane = new JScrollPane(table);
    private final JLabel errorLabel = new JLabel();
    private JToggleButton synch = new JToggleButton(Images.SYNCH, getPreferences().getBoolean("synch", true));
    {
        synch.setToolTipText("Refresh when run ends");
    }
    private Identifier lastGoal = null;
    
    public PreferencesView(JSoarDebugger debuggerIn)
    {
        super("preferences", "Preferences");
        
        this.agent = debuggerIn.getAgent();
        this.selectionManager = debuggerIn.getSelectionManager();
        
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
        this.table.getColumnExt(5).setVisible(false);
        this.table.getColumnExt(3).setVisible(false);
        this.table.getColumnExt(2).setVisible(false);
        
        table.getColumn(0).setResizable(false);
        table.getColumnExt(0).setMaxWidth(40);
        table.getColumn(1).setResizable(false);
        table.getColumn(1).setMaxWidth(60);
        
        errorLabel.setVerticalAlignment(SwingConstants.TOP);
        errorLabel.setForeground(Color.RED);
        
        JPanel barPanel = new JPanel(new BorderLayout());
        barPanel.add(info, BorderLayout.WEST);
        JToolBar bar = createToolbar();
        
        barPanel.add(bar, BorderLayout.EAST);
        
        panel.add(barPanel, BorderLayout.NORTH);
        panel.add(tableScrollPane, BorderLayout.CENTER);
        
        getContentPane().add(panel);

        this.selectionManager.addListener(this);
    }

    private JToolBar createToolbar()
    {
        JToolBar bar = new JToolBar();
        bar.setFloatable(false);
        
        bar.add(synch);
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
     * @see org.jsoar.debugger.Disposable#dispose()
     */
    @Override
    public void dispose()
    {
        getPreferences().putBoolean("synch", synch.isSelected());
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
        
        if(id == null || id.isGoal())
        {
            lastGoal = id;
            getPreferences(id);
        }
    }
    
    /* (non-Javadoc)
     * @see org.jsoar.debugger.Refreshable#refresh(boolean)
     */
    @Override
    public void refresh(boolean afterInitSoar)
    {
        if(synch.isSelected())
        {
            // Force an update
            getPreferences(lastGoal);
        }
    }

    private Result getLastResult()
    {
        return tableModel.getResult();
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
        if(result == null)
        {
            return;
        }
        
        info.setText(String.format("<html><b>&nbsp;Operator preferences for <code>%s</code></b></html>", result.getQueryId()));
        if(result.getError() == null)
        {
            tableModel.setResult(result);
            switchPanelComponent(errorLabel, tableScrollPane);
        }
        else
        {
            errorLabel.setText("Hello" /*result.getError()*/);
            switchPanelComponent(tableScrollPane, errorLabel);
        }
    }
    
    private void switchPanelComponent(JComponent oldChild, JComponent newChild)
    {
        if(!SwingTools.hasChild(panel, newChild))
        {
            panel.remove(oldChild);
            panel.add(newChild, BorderLayout.CENTER);
            panel.invalidate();
            panel.revalidate();
            panel.repaint();
        }
    }
    
    private Result safeGetPreferences(Identifier id)
    {
        if(id == null)
        {
            // If a state hasn't been selected in the UI, try to just show the current state
            final Symbol s = agent.getAgent().readIdentifierOrContextVariable("<s>");
            if(s != null && s.asIdentifier() != null)
            {
                id = s.asIdentifier();
            }
            else
            {
                return null;
            }
        }
        final PredefinedSymbols predefinedSyms = Adaptables.adapt(agent.getAgent(), PredefinedSymbols.class);
        final StructuredPreferencesCommand c = new StructuredPreferencesCommand();
        // Do (id ^operator *)
        return c.getPreferences(agent.getAgent(), id, predefinedSyms.operator_symbol);
    }
}
