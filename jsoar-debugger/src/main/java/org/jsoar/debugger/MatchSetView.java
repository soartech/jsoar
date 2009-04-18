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

import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JToolBar;
import javax.swing.JTree;
import javax.swing.tree.DefaultTreeCellRenderer;

import org.flexdock.docking.DockingConstants;
import org.jdesktop.swingx.JXTreeTable;
import org.jdesktop.swingx.decorator.HighlighterFactory;
import org.jdesktop.swingx.treetable.TreeTableNode;
import org.jsoar.debugger.actions.AbstractDebuggerAction;
import org.jsoar.kernel.Agent;
import org.jsoar.kernel.MatchSet;
import org.jsoar.kernel.MatchSetEntry;
import org.jsoar.kernel.memory.Wme;
import org.jsoar.kernel.tracing.Trace.MatchSetTraceType;
import org.jsoar.kernel.tracing.Trace.WmeTraceType;
import org.jsoar.runtime.CompletionHandler;
import org.jsoar.runtime.SwingCompletionHandler;
import org.jsoar.runtime.ThreadedAgent;

/**
 * @author ray
 */
public class MatchSetView extends AbstractAdaptableView implements Refreshable
{
    private static final long serialVersionUID = -5150761314645770374L;

    private final ThreadedAgent agent;
    private final JXTreeTable entryTable = new JXTreeTable();
    
    public MatchSetView(ThreadedAgent agent)
    {
        super("matcheset", "Match Set");
        
        this.agent = agent;
        
        addAction(DockingConstants.PIN_ACTION);
        
        JPanel barPanel = new JPanel(new BorderLayout());
        JToolBar bar = createToolbar();
        barPanel.add(bar, BorderLayout.EAST);
        
        JPanel p = new JPanel(new BorderLayout());
        p.add(barPanel, BorderLayout.NORTH);
        
        this.entryTable.setRootVisible(false);
        this.entryTable.setShowGrid(false);
        this.entryTable.setHighlighters(HighlighterFactory.createAlternateStriping());
        this.entryTable.setColumnControlVisible(true);
        this.entryTable.setTreeCellRenderer(new CellRenderer());
        
        p.add(new JScrollPane(entryTable), BorderLayout.CENTER);
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
                entryTable.setTreeTableModel(new MatchSetTreeModel(result));
                //sourceWmeTable.expandAll();
                entryTable.packAll();
            }
            
        };
        agent.execute(matchCall, SwingCompletionHandler.newInstance(finish));
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
            if(user instanceof MatchSetEntry)
            {
                final MatchSetEntry entry = (MatchSetEntry) user;
                switch(entry.getType())
                {
                case I_ASSERTION: setIcon(Images.IASSERTION); break;
                case O_ASSERTION: setIcon(Images.OASSERTION); break;
                case RETRACTION: setIcon(Images.RETRACTION);  break;
                }
                setFont(boldFont);
                setText(entry.getProduction() != null ? entry.getProduction().getName().toString() : "[dummy]");
            } 
            else if(user instanceof Wme)
            {
                final Wme wme = (Wme) user;
                setIcon(Images.WME);
                setFont(normalFont);
                setText(wme.getIdentifier().toString());
            }
            return c;
        }
    }
}
