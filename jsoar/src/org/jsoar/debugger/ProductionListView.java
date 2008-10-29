/*
 * Copyright (c) 2008  Dave Ray <daveray@gmail.com>
 *
 * Created on Oct 23, 2008
 */
package org.jsoar.debugger;

import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Callable;

import javax.swing.BorderFactory;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;
import javax.swing.table.DefaultTableCellRenderer;

import org.flexdock.docking.DockingConstants;
import org.jdesktop.swingx.JXTable;
import org.jdesktop.swingx.decorator.HighlighterFactory;
import org.jsoar.debugger.actions.ObjectPopupMenu;
import org.jsoar.debugger.selection.SelectionManager;
import org.jsoar.debugger.selection.SelectionProvider;
import org.jsoar.kernel.Production;
import org.jsoar.kernel.ProductionType;


/**
 * @author ray
 */
public class ProductionListView extends AbstractAdaptableView
{
    private static final long serialVersionUID = -5724361674833156058L;
    
    private final LittleDebugger debugger;
    private final ProductionTableModel model;
    private final JXTable table;
    private final JLabel stats = new JLabel();
    private final Provider selectionProvider = new Provider();
    
    /**
     * @param persistentId
     * @param title
     * @param tabText
     */
    public ProductionListView(LittleDebugger debuggerIn)
    {
        super("productionList", "Productions");
        
        this.debugger = debuggerIn;
        
        this.addAction(DockingConstants.PIN_ACTION);
        
        JPanel p = new JPanel(new BorderLayout());
        
        this.model = new ProductionTableModel(this.debugger.getAgentProxy());
        this.model.initialize();
        this.table = new JXTable(this.model);
        this.table.getSelectionModel().addListSelectionListener(selectionProvider);
        this.table.setDefaultRenderer(Production.class, new NameCellRenderer());
        
        DefaultTableCellRenderer fcRenderer = new DefaultTableCellRenderer();
        fcRenderer.setHorizontalAlignment(DefaultTableCellRenderer.LEFT);
        this.table.setDefaultRenderer(Integer.class, fcRenderer);
        
        this.table.setShowGrid(false);
        this.table.setHighlighters(HighlighterFactory.createAlternateStriping());
        this.table.setColumnControlVisible(true);
        this.table.addMouseListener(new MouseAdapter() {

            @Override
            public void mousePressed(MouseEvent e)
            {
                ObjectPopupMenu.show(e, debugger.getActionManager());
            }

            @Override
            public void mouseReleased(MouseEvent e)
            {
                ObjectPopupMenu.show(e, debugger.getActionManager());
            }});
        
        p.add(new JScrollPane(table), BorderLayout.CENTER);
        
        stats.setBorder(BorderFactory.createTitledBorder("Production Statistics"));
        
        JPanel bottom = new JPanel(new BorderLayout());
        
        bottom.add(new TableFilterPanel(table, 0), BorderLayout.NORTH);
        bottom.add(stats, BorderLayout.CENTER);
        
        p.add(bottom, BorderLayout.SOUTH);
        
        this.setContentPane(p);
    }
    
    public void refresh()
    {
        this.table.repaint();
        this.table.packAll();
        
        Map<ProductionType, Integer> counts = 
        debugger.getAgentProxy().execute(new Callable<Map<ProductionType, Integer>>() {

            @Override
            public Map<ProductionType, Integer> call() throws Exception
            {
                return debugger.getAgentProxy().getAgent().getProductions().getProductionCounts();
            }});
        
        final String spaces = "&nbsp;&nbsp;&nbsp;";
        final StringBuilder b = new StringBuilder("<html>");
        b.append("<b>Total:</b>&nbsp;" + this.model.getRowCount() + spaces);
        for(Map.Entry<ProductionType, Integer> e : counts.entrySet())
        {
            b.append(" <b>" + e.getKey().getDisplayString() + ":</b>&nbsp;" + e.getValue() + spaces);
        }
        b.append("</html>");
            
        this.stats.setText(b.toString());
        
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

    private static class NameCellRenderer extends DefaultTableCellRenderer
    {
        private static final long serialVersionUID = -7488455622043120329L;

        /* (non-Javadoc)
         * @see javax.swing.table.DefaultTableCellRenderer#getTableCellRendererComponent(javax.swing.JTable, java.lang.Object, boolean, boolean, int, int)
         */
        @Override
        public Component getTableCellRendererComponent(JTable table, Object value, boolean isSelected,
                boolean hasFocus, int row, int column)
        {
            Component c = super.getTableCellRendererComponent(table, value, isSelected, hasFocus, row, column);
            final Production p = (Production) value;
            setIcon(column == 0 ? Images.PRODUCTION : null);
            setText(p.getName().toString());
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
            List<Object> s = getSelection();
            return !s.isEmpty() ? s.get(0) : null;
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
                final Object value = table.getValueAt(row, 0);
                result.add(value);
            }
            return result;
        }

        /* (non-Javadoc)
         * @see javax.swing.event.ListSelectionListener#valueChanged(javax.swing.event.ListSelectionEvent)
         */
        @Override
        public void valueChanged(ListSelectionEvent e)
        {
            if(manager != null)
            {
                manager.fireSelectionChanged();
            }
        }
        
    }
}
