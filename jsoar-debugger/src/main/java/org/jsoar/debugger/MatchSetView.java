/*
 * Copyright (c) 2008  Dave Ray <daveray@gmail.com>
 *
 * Created on Oct 23, 2008
 */
package org.jsoar.debugger;

import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.Font;
import java.awt.event.ActionEvent;
import java.util.EnumSet;
import java.util.concurrent.Callable;

import javax.swing.DefaultListCellRenderer;
import javax.swing.DefaultListModel;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JSplitPane;
import javax.swing.JToolBar;
import javax.swing.ListSelectionModel;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;

import org.flexdock.docking.DockingConstants;
import org.jdesktop.swingx.JXList;
import org.jdesktop.swingx.JXTable;
import org.jdesktop.swingx.decorator.HighlighterFactory;
import org.jsoar.debugger.actions.AbstractDebuggerAction;
import org.jsoar.kernel.Agent;
import org.jsoar.kernel.MatchSet;
import org.jsoar.kernel.MatchSetEntry;
import org.jsoar.kernel.symbols.Identifier;
import org.jsoar.kernel.tracing.Trace.MatchSetTraceType;
import org.jsoar.kernel.tracing.Trace.WmeTraceType;
import org.jsoar.runtime.CompletionHandler;
import org.jsoar.runtime.SwingCompletionHandler;
import org.jsoar.runtime.ThreadedAgent;
import org.jsoar.util.SwingTools;
import org.jsoar.util.adaptables.Adaptables;

/**
 * @author ray
 */
public class MatchSetView extends AbstractAdaptableView implements Refreshable
{
    private static final long serialVersionUID = -5150761314645770374L;

    private final JSoarDebugger debugger;
    private final ThreadedAgent agent;
    private final JXList entryList = new JXList();
    private final JXTable wmeTable = new JXTable(new DefaultWmeTableModel());
    
    public MatchSetView(JSoarDebugger debugger)
    {
        super("matcheset", "Match Set");
        
        this.debugger = debugger;
        this.agent = debugger.getAgentProxy();
        
        addAction(DockingConstants.PIN_ACTION);
        
        final JPanel barPanel = new JPanel(new BorderLayout());
        final JToolBar bar = createToolbar();
        barPanel.add(bar, BorderLayout.EAST);
        barPanel.add(new JLabel("Pending Assertions/Retractions"), BorderLayout.WEST);
        
        final JPanel p = new JPanel(new BorderLayout());
        p.add(barPanel, BorderLayout.NORTH);
        
        this.entryList.setHighlighters(HighlighterFactory.createAlternateStriping());
        this.entryList.setCellRenderer(new CellRenderer());
        this.entryList.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        this.entryList.getSelectionModel().addListSelectionListener(new ListSelectionListener(){

            @Override
            public void valueChanged(ListSelectionEvent e)
            {
                if(!e.getValueIsAdjusting())
                {
                    tableSelectionChange();
                }
            }});
        
        this.wmeTable.setHighlighters(HighlighterFactory.createAlternateStriping());
        this.wmeTable.setShowGrid(true);
        this.wmeTable.setDefaultRenderer(Identifier.class, new DefaultWmeTableCellRenderer());
        
        
        final JSplitPane split = new JSplitPane(JSplitPane.VERTICAL_SPLIT, new JScrollPane(entryList), new JScrollPane(wmeTable));
        split.setDividerSize(5);
        SwingTools.setDividerLocation(split, 0.5);
        p.add(split, BorderLayout.CENTER);
        
        setContentPane(p);
    }
    
    private JToolBar createToolbar()
    {
        JToolBar bar = new JToolBar();
        bar.setFloatable(false);
        
        bar.add(new AbstractDebuggerAction("Print match set to trace", Images.COPY) {
            private static final long serialVersionUID = -3614573079885324027L;

            {
                setToolTip("Print match set to trace");
            }
            @Override
            public void update()
            {
            }

            @Override
            public void actionPerformed(ActionEvent arg0)
            {
                final Agent a = agent.getAgent();
                a.getPrinter().startNewLine();
                a.printMatchSet(a.getPrinter(), WmeTraceType.FULL, EnumSet.allOf(MatchSetTraceType.class));
            }});
        return bar;
    }

    /* (non-Javadoc)
     * @see org.jsoar.debugger.Refreshable#refresh(boolean)
     */
    @Override
    public void refresh(boolean afterInitSoar)
    {
        final Callable<MatchSet> matchCall = new Callable<MatchSet>() {

            @Override
            public MatchSet call() throws Exception
            {
                return agent.getAgent().getMatchSet();
            }};
        final CompletionHandler<MatchSet> finish = new CompletionHandler<MatchSet>() {
            @Override
            public void finish(MatchSet result)
            {
                entryList.setModel(SwingTools.addAll(new DefaultListModel(), result.getEntries()));
                wmeTable.setModel(new DefaultWmeTableModel());
                wmeTable.packAll();
            }
            
        };
        agent.execute(matchCall, SwingCompletionHandler.newInstance(finish));
    }
    
    private void tableSelectionChange()
    {
        final MatchSetEntry entry = (MatchSetEntry) entryList.getSelectedValue();
        wmeTable.setModel(entry != null ? new DefaultWmeTableModel(entry.getWmes()) : new DefaultWmeTableModel());
        
        if(entry != null)
        {
            final ProductionListView view = Adaptables.adapt(debugger, ProductionListView.class);
            if(view != null)
            {
                view.selectProduction(entry.getProduction());
            }
        }
    }
    
    
    private static class CellRenderer extends DefaultListCellRenderer
    {
        private static final long serialVersionUID = -2334648499852429083L;
        private Font normalFont;
        private Font boldFont;
        
        @Override
        public Component getListCellRendererComponent(JList list, Object value,
                int index, boolean isSelected, boolean cellHasFocus)
        {
            Component c = super.getListCellRendererComponent(list, value, index, isSelected, cellHasFocus);
            if(normalFont == null)
            {
                normalFont = getFont();
                boldFont = normalFont.deriveFont(Font.BOLD);
            }
            final MatchSetEntry entry = (MatchSetEntry) value;
            switch(entry.getType())
            {
            case I_ASSERTION: setIcon(Images.IASSERTION); break;
            case O_ASSERTION: setIcon(Images.OASSERTION); break;
            case RETRACTION: setIcon(Images.RETRACTION);  break;
            }
            setFont(boldFont);
            setText(entry.getProduction() != null ? entry.getProduction().getName().toString() : "[dummy]");
            return c;
        }
    }
}
