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

import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JToolBar;
import javax.swing.JTree;
import javax.swing.ListSelectionModel;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;
import javax.swing.tree.DefaultTreeCellRenderer;

import org.flexdock.docking.DockingConstants;
import org.jdesktop.swingx.JXTreeTable;
import org.jdesktop.swingx.decorator.HighlighterFactory;
import org.jdesktop.swingx.treetable.TreeTableNode;
import org.jsoar.debugger.actions.AbstractDebuggerAction;
import org.jsoar.debugger.selection.SelectionListener;
import org.jsoar.debugger.selection.SelectionManager;
import org.jsoar.kernel.Agent;
import org.jsoar.kernel.Production;
import org.jsoar.kernel.memory.Wme;
import org.jsoar.kernel.memory.WmeSupportInfo;
import org.jsoar.kernel.memory.WmeSupportInfo.Support;
import org.jsoar.runtime.Completer;
import org.jsoar.runtime.SwingCompletion;
import org.jsoar.util.adaptables.Adaptables;

/**
 * Simple view that shows preferences for selected id or wme.
 * 
 * @author ray
 */
public class WmeSupportView extends AbstractAdaptableView implements SelectionListener
{
    private static final long serialVersionUID = -5150761314645770374L;

    private final JSoarDebugger debugger;
    private final SelectionManager selectionManager;
    private final JLabel source = new JLabel("");
    private final JXTreeTable sourceWmeTable = new JXTreeTable();
    private WmeSupportInfo sourceInfo;
    
    public WmeSupportView(JSoarDebugger debuggerIn)
    {
        super("wmeSupport", "WME Support");
        this.debugger = debuggerIn;
        this.selectionManager = debugger.getSelectionManager();
        
        addAction(DockingConstants.PIN_ACTION);
                
        JPanel barPanel = new JPanel(new BorderLayout());
        barPanel.add(source, BorderLayout.WEST);
        JToolBar bar = createToolbar();
        
        barPanel.add(bar, BorderLayout.EAST);
        
        JPanel p = new JPanel(new BorderLayout());
        p.add(barPanel, BorderLayout.NORTH);
        
        this.sourceWmeTable.setRootVisible(false);
        this.sourceWmeTable.setShowGrid(false);
        this.sourceWmeTable.setHighlighters(HighlighterFactory.createAlternateStriping());
        this.sourceWmeTable.setColumnControlVisible(true);
        this.sourceWmeTable.setTreeCellRenderer(new CellRenderer());
        this.sourceWmeTable.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        this.sourceWmeTable.getSelectionModel().addListSelectionListener(new ListSelectionListener() {

            /* (non-Javadoc)
             * @see javax.swing.event.ListSelectionListener#valueChanged(javax.swing.event.ListSelectionEvent)
             */
            @Override
            public void valueChanged(ListSelectionEvent e)
            {
                if(!e.getValueIsAdjusting())
                {
                    tableSelectionChange();
                }
            }});
        
        p.add(new JScrollPane(sourceWmeTable), BorderLayout.CENTER);
        
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
                if(sourceInfo != null)
                {
                    debugger.getAgentProxy().getAgent().getPrinter().startNewLine().print(sourceInfo.toString()).flush();
                }
            }});
        return bar;
    }

    private void tableSelectionChange()
    {
        int r = sourceWmeTable.getSelectedRow();
        Object o = sourceWmeTable.getModel().getValueAt(r, 0);
        if(o instanceof Production)
        {
            ProductionListView view = Adaptables.adapt(debugger, ProductionListView.class);
            if(view != null)
            {
                view.selectProduction((Production) o);
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
                final Agent agent = debugger.getAgentProxy().getAgent();
                return WmeSupportInfo.get(agent, w);
            }};
        final Completer<WmeSupportInfo> finish = new Completer<WmeSupportInfo>() {

            @Override
            public void finish(WmeSupportInfo sourceInfo)
            {
                source.setText(String.format("<html><b><code>%#s</code></b> is supported by the following productions:</html>", w));
                sourceWmeTable.setTreeTableModel(new WmeSupportTreeModel(sourceInfo));
                sourceWmeTable.expandAll();
                sourceWmeTable.packAll();
            }
        };
        debugger.getAgentProxy().execute(call, SwingCompletion.newInstance(finish));
    }
    
    private static class CellRenderer extends DefaultTreeCellRenderer
    {
        private static final long serialVersionUID = -2334648499852429083L;
        private Font normalFont;
        private Font boldFont;
        
        /* (non-Javadoc)
         * @see javax.swing.tree.DefaultTreeCellRenderer#getTreeCellRendererComponent(javax.swing.JTree, java.lang.Object, boolean, boolean, boolean, int, boolean)
         */
        @Override
        public Component getTreeCellRendererComponent(JTree tree, Object object, boolean arg2, boolean arg3,
                boolean arg4, int arg5, boolean arg6)
        {
            Component c = super.getTreeCellRendererComponent(tree, object, arg2, arg3, arg4, arg5, arg6);
            if(normalFont == null)
            {
                normalFont = getFont();
                boldFont = normalFont.deriveFont(Font.BOLD);
            }
            setIcon(null);
            
            Object user = ((TreeTableNode) object).getUserObject();
            if(user instanceof Support)
            {
                Support support = (Support) user;
                setIcon(Images.PRODUCTION);
                setFont(boldFont);
                setText(String.format("%s (:%c)", 
                                      support.getSource() != null ? support.getSource().getName().toString() : "[dummy]",
                                      support.isOSupported() ? 'O' : 'I'));
            } 
            else if(user instanceof Wme)
            {
                Wme wme = (Wme) user;
                setIcon(Images.WME);
                setFont(normalFont);
                setText(wme.getIdentifier().toString());
            }
            return c;
        }
    }

}
