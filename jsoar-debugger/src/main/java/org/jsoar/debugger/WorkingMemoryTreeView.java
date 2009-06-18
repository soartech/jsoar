/*
 * Copyright (c) 2008  Dave Ray <daveray@gmail.com>
 *
 * Created on Oct 23, 2008
 */
package org.jsoar.debugger;

import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Callable;

import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTextField;
import javax.swing.JToggleButton;
import javax.swing.JToolBar;
import javax.swing.JTree;
import javax.swing.SwingUtilities;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;
import javax.swing.tree.DefaultTreeCellRenderer;
import javax.swing.tree.TreePath;

import org.flexdock.docking.DockingConstants;
import org.jdesktop.swingx.JXTreeTable;
import org.jdesktop.swingx.action.AbstractActionExt;
import org.jdesktop.swingx.decorator.HighlighterFactory;
import org.jsoar.debugger.selection.SelectionManager;
import org.jsoar.debugger.selection.SelectionProvider;
import org.jsoar.kernel.Agent;
import org.jsoar.kernel.symbols.Identifier;
import org.jsoar.kernel.symbols.Symbol;
import org.jsoar.runtime.CompletionHandler;
import org.jsoar.runtime.SwingCompletionHandler;


/**
 * @author ray
 */
public class WorkingMemoryTreeView extends AbstractAdaptableView implements Refreshable, Disposable
{
    private static final long serialVersionUID = -6587008765716839376L;
    
    private final JSoarDebugger debugger;
    private WorkingMemoryTreeModel model;
    private JXTreeTable table;
    private Provider selectionProvider = new Provider();
    private JToggleButton synch = new JToggleButton(Images.SYNCH, getPreferences().getBoolean("synch", true));
    {
        synch.setToolTipText("Refresh tree when run ends");
    }
    private final JTextField idField = new JTextField("<s> <o> <ts> I2 I3", 40);
    private List<String> history = new ArrayList<String>();
    private int historyPosition = -1;

    private final AbstractActionExt forwardAction = new AbstractActionExt("Forward", Images.NEXT) {

        private static final long serialVersionUID = 4156756655913814386L;

        @Override
        public void actionPerformed(ActionEvent e)
        {
            forward();
        }};

    private final AbstractActionExt backAction = new AbstractActionExt("Back", Images.PREVIOUS) {
        private static final long serialVersionUID = 6420511008322250144L;

        @Override
        public void actionPerformed(ActionEvent arg0)
        {
            back();
        }};
    
    /**
     * @param debugger the owning debugger
     */
    public WorkingMemoryTreeView(JSoarDebugger debugger)
    {
        super("workingMemory.tree", "Working Memory Tree");

        addAction(DockingConstants.PIN_ACTION);
        
        this.debugger = debugger;
        this.model = new WorkingMemoryTreeModel(debugger.getAgent(), new ArrayList<Identifier>());
        this.table = new JXTreeTable(this.model);
        this.table.setRootVisible(false);
        this.table.setHighlighters(HighlighterFactory.createAlternateStriping());
        this.table.getSelectionModel().addListSelectionListener(selectionProvider);
        this.table.setRolloverEnabled(true);
        this.table.setTreeCellRenderer(new CellRenderer());
        this.table.setColumnControlVisible(true);
        this.table.addMouseListener(new MouseAdapter() {

            @Override
            public void mouseClicked(MouseEvent e)
            {
                if(SwingUtilities.isLeftMouseButton(e) && e.getClickCount() == 2)
                {
                    TreePath path = table.getPathForLocation(e.getX(), e.getY());
                    if(path != null)
                    {
                        WorkingMemoryTreeNode node = (WorkingMemoryTreeNode) path.getLastPathComponent();
                        Identifier id = node.getValueId();
                        if(id != null)
                        {
                            jump(id.toString(), true);
                        }
                    }
                }
            }});
        
        this.addAction(DockingConstants.PIN_ACTION);
        
        JPanel p = new JPanel(new BorderLayout());
        p.add(new JScrollPane(table), BorderLayout.CENTER);
        
        JToolBar bar = new JToolBar();
        bar.setFloatable(false);
        bar.add(new JLabel(" Roots: "));
        idField.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e)
            {
                jump(idField.getText(), true);
            }});
        idField.setMaximumSize(new Dimension(10000, 20));
        bar.add(idField);
        bar.add(backAction);
        bar.add(forwardAction);
        bar.add(new AbstractActionExt("Refresh", Images.REFRESH) {

            private static final long serialVersionUID = -6030691756683552616L;

            {
                setToolTipText("Refresh tree");
            }
            @Override
            public void actionPerformed(ActionEvent arg0)
            {
                refresh(idField.getText());
            }});
        bar.add(synch);
        
        p.add(bar, BorderLayout.NORTH);
        
        this.setContentPane(p);
        
        jump(idField.getText(), true);
    }

    /* (non-Javadoc)
     * @see org.jsoar.debugger.Refreshable#refresh(boolean)
     */
    public void refresh(boolean afterInitSoar)
    {
        if(synch.isSelected())
        {
            refresh(idField.getText());
        }
    }
    
    /* (non-Javadoc)
     * @see org.jsoar.debugger.Disposable#dispose()
     */
    @Override
    public void dispose()
    {
        getPreferences().putBoolean("synch", synch.isSelected());
    }

    private void jump(String idList, boolean addToHistory)
    {
        idField.setText(idList);
        
        if(addToHistory)
        {
            history = history.subList(0, historyPosition + 1);
            history.add(idList);
            historyPosition = history.size() - 1;
        }
        
        refresh(idField.getText());
    }
    
    private void back()
    {
        if(historyPosition == 0)
        {
            return;
        }
        --historyPosition;
        jump(history.get(historyPosition), false);
    }
    
    private void forward()
    {
        if(historyPosition + 1 == history.size())
        {
            return;
        }
        ++historyPosition;
        jump(history.get(historyPosition), false);
    }
    
    private void refresh(String idString)
    {
        backAction.setEnabled(historyPosition > 0);
        forwardAction.setEnabled(historyPosition < history.size() - 1);
        
        final String[] tokens = idString.split("\\s+");
        final Agent agent = debugger.getAgent().getAgent();
        final Callable<List<Identifier>> callable = new Callable<List<Identifier>>() {

            public List<Identifier> call() throws Exception
            {
                final List<Identifier> result = new ArrayList<Identifier>();
                for(String t : tokens)
                {
                    t = t.trim();
                    
                    final Symbol s = agent.readIdentifierOrContextVariable(t);
                    if(s != null)
                    {
                        Identifier id = s.asIdentifier();
                        if(id != null && !result.contains(id))
                        {
                            result.add(id);
                        }
                    }
                }
                return result;
            }};
        final CompletionHandler<List<Identifier>> finish = new CompletionHandler<List<Identifier>>() {

            @Override
            public void finish(List<Identifier> ids)
            {

                // Reset the tree model
                model = new WorkingMemoryTreeModel(debugger.getAgent(), ids);
                table.setTreeTableModel(model);
                
                // Expand the rows
                for(int row = table.getRowCount() - 1; row >= 0; row--)
                {
                    table.expandRow(row);
                }
            }
            
        };
        
        debugger.getAgent().execute(callable, SwingCompletionHandler.newInstance(finish));
    }

    /* (non-Javadoc)
     * @see org.jsoar.debugger.AbstractAdaptableView#getAdapter(java.lang.Class)
     */
    @Override
    public Object getAdapter(Class<?> klass)
    {
        if(SelectionProvider.class.equals(klass))
        {
            return selectionProvider;
        }
        return super.getAdapter(klass);
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
            WorkingMemoryTreeNode node = (WorkingMemoryTreeNode) object;
            if(node.getWme() != null)
            {
                setIcon(Images.WME);
                setFont(normalFont);
                setText(node.getWme().getAttribute().toString());
            }
            else if(node.getValueId() != null)
            {
                setIcon(Images.ID);
                setFont(boldFont);
                setText(node.getValueId().toString());
            }
            return c;
        }
    }
    
    private class Provider implements SelectionProvider, ListSelectionListener
    {   
        private SelectionManager manager;

        /* (non-Javadoc)
         * @see org.jsoar.debugger.selection.SelectionProvider#activate(org.jsoar.debugger.selection.SelectionManager)
         */
        @Override
        public void activate(SelectionManager manager)
        {
            this.manager = manager;
        }

        /* (non-Javadoc)
         * @see org.jsoar.debugger.selection.SelectionProvider#deactivate()
         */
        @Override
        public void deactivate()
        {
            this.manager = null;
        }

        /* (non-Javadoc)
         * @see org.jsoar.debugger.selection.SelectionProvider#getSelectedObject()
         */
        @Override
        public Object getSelectedObject()
        {
            List<Object> selection = getSelection();
            return !selection.isEmpty() ? selection.get(0) : null;
        }

        /* (non-Javadoc)
         * @see org.jsoar.debugger.selection.SelectionProvider#getSelection()
         */
        @Override
        public List<Object> getSelection()
        {
            List<Object> result = new ArrayList<Object>();
            for(int row : table.getSelectedRows())
            {
                result.add(table.getValueAt(row, 0));
            }
            return result;
        }

        /* (non-Javadoc)
         * @see javax.swing.event.ListSelectionListener#valueChanged(javax.swing.event.ListSelectionEvent)
         */
        @Override
        public void valueChanged(ListSelectionEvent arg0)
        {
            if(manager == null)
            {
                return;
            }
            manager.fireSelectionChanged();
        }
    }
}
