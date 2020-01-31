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

import org.jdesktop.swingx.JXList;
import org.jdesktop.swingx.JXTable;
import org.jdesktop.swingx.decorator.HighlighterFactory;
import org.jsoar.debugger.actions.AbstractDebuggerAction;
import org.jsoar.debugger.selection.SelectionListener;
import org.jsoar.debugger.selection.SelectionManager;
import org.jsoar.debugger.util.SwingTools;
import org.jsoar.kernel.Agent;
import org.jsoar.kernel.memory.Wme;
import org.jsoar.kernel.memory.WmeSupportInfo;
import org.jsoar.kernel.memory.WmeSupportInfo.Support;
import org.jsoar.kernel.symbols.Identifier;
import org.jsoar.runtime.CompletionHandler;
import org.jsoar.runtime.SwingCompletionHandler;
import org.jsoar.util.adaptables.Adaptables;

/**
 * Simple view that shows preferences for selected id or wme.
 * 
 * @author ray
 */
public class WmeSupportView extends AbstractAdaptableView implements SelectionListener
{
    private final JSoarDebugger debugger;
    private final SelectionManager selectionManager;
    private final JLabel source = new JLabel("");
    private WmeSupportInfo sourceInfo;
    private final JXList entryList = new JXList();
    private final DefaultWmeTableModel wmeModel = new DefaultWmeTableModel();
    private final JXTable wmeTable = new JXTable(wmeModel);
    
    public WmeSupportView(JSoarDebugger debuggerIn)
    {
        super("wmeSupport", "WME Support");
        this.debugger = debuggerIn;
        this.selectionManager = debugger.getSelectionManager();
        
        JPanel barPanel = new JPanel(new BorderLayout());
        barPanel.add(source, BorderLayout.WEST);
        JToolBar bar = createToolbar();
        
        barPanel.add(bar, BorderLayout.EAST);
        
        JPanel p = new JPanel(new BorderLayout());
        p.add(barPanel, BorderLayout.NORTH);
        this.entryList.setHighlighters(HighlighterFactory.createAlternateStriping());
        this.entryList.setCellRenderer(new CellRenderer());
        this.entryList.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        this.entryList.getSelectionModel().addListSelectionListener(new ListSelectionListener(){

            @Override
            public void valueChanged(ListSelectionEvent e)
            {
                if(e.getValueIsAdjusting()) return;
                tableSelectionChange();
            }});
        
        this.wmeTable.setHighlighters(HighlighterFactory.createAlternateStriping());
        this.wmeTable.setShowGrid(false);
        this.wmeTable.setDefaultRenderer(Identifier.class, new DefaultWmeTableCellRenderer());
        
        
        final JSplitPane split = new JSplitPane(JSplitPane.VERTICAL_SPLIT, new JScrollPane(entryList), new JScrollPane(wmeTable));
        split.setDividerSize(5);
        SwingTools.setDividerLocation(split, 0.5);
        p.add(split, BorderLayout.CENTER);
        
        getContentPane().add(p);
        
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
                if(sourceInfo != null)
                {
                    debugger.getAgent().getPrinter().startNewLine().print(sourceInfo.toString()).flush();
                }
            }});
        return bar;
    }

    private void tableSelectionChange()
    {
        final Support support = (Support) entryList.getSelectedValue();
        wmeModel.setWmes(support != null ? support.getSourceWmes() : null);
        
        if(support != null)
        {
            ProductionListView view = Adaptables.adapt(debugger, ProductionListView.class);
            if(view != null)
            {
                view.selectProduction(support.getSource());
            }
        }
    }
    
    /* (non-Javadoc)
     * @see org.jsoar.debugger.selection.SelectionListener#selectionChanged(org.jsoar.debugger.selection.SelectionManager)
     */
    @Override
    public void selectionChanged(SelectionManager manager)
    {
        final Object selection = manager.getSelectedObject();
        final Wme w = Adaptables.adapt(selection, Wme.class);
        if(w == null)
        {
            return;
        }
        final Callable<WmeSupportInfo> call = new Callable<WmeSupportInfo>() {

            @Override
            public WmeSupportInfo call() throws Exception
            {
                final Agent agent = debugger.getAgent().getAgent();
                return WmeSupportInfo.get(agent, w);
            }};
        final CompletionHandler<WmeSupportInfo> finish = new CompletionHandler<WmeSupportInfo>() {

            @Override
            public void finish(WmeSupportInfo sourceInfo)
            {
                source.setText(String.format("<html><b>&nbsp;<code> %#s</code></b> is supported by:</html>", w));
                entryList.setModel(SwingTools.addAll(new DefaultListModel<Support>(), sourceInfo.getSupports()));
                wmeModel.setWmes(null);
                wmeTable.packAll();
                entryList.setSelectedIndex(0);
            }
        };
        debugger.getAgent().execute(call, SwingCompletionHandler.newInstance(finish));
    }
    
    private static class CellRenderer extends DefaultListCellRenderer
    {
        private static final long serialVersionUID = -2334648499852429083L;
        private Font normalFont;
        private Font boldFont;
        
        @Override
        public Component getListCellRendererComponent(JList<?> list, Object value,
                int index, boolean isSelected, boolean cellHasFocus)
        {
            Component c = super.getListCellRendererComponent(list, value, index, isSelected, cellHasFocus);
            if(normalFont == null)
            {
                normalFont = getFont();
                boldFont = normalFont.deriveFont(Font.BOLD);
            }
            final Support support = (Support) value;
            setIcon(Images.PRODUCTION);
            setFont(boldFont);
            setText(String.format("%s (:%c)", 
                                  support.getSource() != null ? support.getSource().getName().toString() : "[dummy]",
                                  support.isOSupported() ? 'O' : 'I'));
            return c;
        }
    }

}
