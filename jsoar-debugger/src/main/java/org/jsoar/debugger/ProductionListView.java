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
import java.util.Map;
import java.util.concurrent.Callable;

import javax.swing.BorderFactory;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.SwingUtilities;
import javax.swing.event.ListSelectionEvent;
import javax.swing.table.DefaultTableCellRenderer;

import org.jdesktop.swingx.JXTable;
import org.jdesktop.swingx.decorator.HighlighterFactory;
import org.jsoar.debugger.actions.ActionManager;
import org.jsoar.debugger.actions.EditProductionAction;
import org.jsoar.debugger.actions.ObjectPopupMenu;
import org.jsoar.debugger.selection.SelectionListener;
import org.jsoar.debugger.selection.SelectionManager;
import org.jsoar.debugger.selection.SelectionProvider;
import org.jsoar.debugger.selection.TableSelectionProvider;
import org.jsoar.kernel.Production;
import org.jsoar.kernel.ProductionType;
import org.jsoar.runtime.CompletionHandler;
import org.jsoar.runtime.SwingCompletionHandler;
import org.jsoar.runtime.ThreadedAgent;
import org.jsoar.util.adaptables.Adaptable;
import org.jsoar.util.adaptables.Adaptables;


/**
 * @author ray
 */
public class ProductionListView extends AbstractAdaptableView implements Refreshable, SelectionListener
{
    private static final long serialVersionUID = -5724361674833156058L;
    
    private final ThreadedAgent agent;
    private final ActionManager actionManager;
    private final ProductionTableModel model;
    private final JXTable table;
    private final JLabel stats = new JLabel();
    private final JLabel info = new JLabel();
    private final TableSelectionProvider selectionProvider;
    
    /**
     * @param debuggerIn The owning debugger
     */
    public ProductionListView(Adaptable debuggerIn)
    {
        super("productionList", "Productions");
        
        this.agent = Adaptables.adapt(debuggerIn, ThreadedAgent.class);
        this.actionManager = Adaptables.adapt(debuggerIn, ActionManager.class);
        
        JPanel p = new JPanel(new BorderLayout());
        
        this.model = new ProductionTableModel(this.agent);
        this.model.initialize();
        this.table = new JXTable(this.model);
        this.selectionProvider = new TableSelectionProvider(this.table) {

            @Override
            public void valueChanged(ListSelectionEvent e)
            {
                super.valueChanged(e);
                updateInfo();
            }};
        
        this.actionManager.getSelectionManager().addListener(this);
        
        this.table.setDefaultRenderer(Production.class, new NameCellRenderer());
        
        DefaultTableCellRenderer fcRenderer = new DefaultTableCellRenderer();
        fcRenderer.setHorizontalAlignment(DefaultTableCellRenderer.LEFT);
        this.table.setDefaultRenderer(Integer.class, fcRenderer);
        
        this.table.setShowGrid(false);
        this.table.setHighlighters(HighlighterFactory.createAlternateStriping());
        this.table.setColumnControlVisible(true);
        this.table.addMouseListener(new MouseAdapter() {

            @Override
            public void mouseClicked(MouseEvent e)
            {
                if(SwingUtilities.isLeftMouseButton(e) && e.getClickCount() == 2)
                {
                    handleDoubleClick();
                }
            }

            @Override
            public void mousePressed(MouseEvent e)
            {
                ObjectPopupMenu.show(e, actionManager, true);
            }

            @Override
            public void mouseReleased(MouseEvent e)
            {
                ObjectPopupMenu.show(e, actionManager, true);
            }});
        
        p.add(new JScrollPane(table), BorderLayout.CENTER);
        
        stats.setBorder(BorderFactory.createTitledBorder("All Production Statistics"));
        info.setBorder(BorderFactory.createTitledBorder("Info"));
        
        JPanel bottom = new JPanel(new BorderLayout());
        
        bottom.add(new TableFilterPanel(table, 0), BorderLayout.NORTH);
        bottom.add(info, BorderLayout.CENTER);
        bottom.add(stats, BorderLayout.SOUTH);
        
        p.add(bottom, BorderLayout.SOUTH);
        
        this.getContentPane().add(p);
    }
    
    /* (non-Javadoc)
     * @see org.jsoar.debugger.Refreshable#refresh(boolean)
     */
    public void refresh(boolean afterInitSoar)
    {
        this.table.repaint();
        this.table.packAll();
        
        updateInfo();
        updateStats();
    }

    /* (non-Javadoc)
     * @see org.jsoar.debugger.selection.SelectionListener#selectionChanged(org.jsoar.debugger.selection.SelectionManager)
     */
    public void selectionChanged(SelectionManager manager)
    {
        if(selectionProvider.isActive()) // ignore our own selection
        {
            return;
        }
        
        final Production p = Adaptables.adapt(manager.getSelectedObject(), Production.class);
        if(p != null)
        {
            selectProduction(p);
        }
    }
    
    public void selectProduction(Production production)
    {
        int index = model.getProductions().indexOf(production);
        if(index != -1)
        {
            index = table.convertRowIndexToView(index);
            if(index != -1)
            {
                table.getSelectionModel().setSelectionInterval(index, index);
                table.scrollRowToVisible(index);
            }
        }
    }
    
    private void handleDoubleClick()
    {
        actionManager.executeAction(EditProductionAction.class.getCanonicalName());
    }
    
    private void updateInfo()
    {
        final Production p =  Adaptables.adapt(selectionProvider.getSelectedObject(), Production.class);
        
        final Callable<Integer> call = new Callable<Integer>() {

            public Integer call() throws Exception
            {
                return p != null ? p.getReteTokenCount() : 0;
            }};
        final CompletionHandler<Integer> finish = new CompletionHandler<Integer>() {

            @Override
            public void finish(Integer count)
            {
                final StringBuilder b = new StringBuilder("<html>");
                if(p != null)
                {
                    b.append("<b>Name:</b>&nbsp;" + p.getName() + "<br>");
                    b.append("<b>Comment:</b>&nbsp;" + p.getDocumentation() + "<br>");
                    b.append("<b>Memories:</b>&nbsp;" + count);
                }
                else
                {
                    b.append("No selection");
                }
                b.append("</html>");
                
                info.setText(b.toString());
            }
        };
        agent.execute(call, SwingCompletionHandler.newInstance(finish));
    }

    private void updateStats()
    {
        final Callable<Map<ProductionType, Integer>> call = new Callable<Map<ProductionType, Integer>>() {

            @Override
            public Map<ProductionType, Integer> call() throws Exception
            {
                return agent.getProductions().getProductionCounts();
            }};
            
        final CompletionHandler<Map<ProductionType, Integer>> finish = new CompletionHandler<Map<ProductionType,Integer>>() {

            @Override
            public void finish(Map<ProductionType, Integer> counts)
            {
                final String spaces = "&nbsp;&nbsp;&nbsp;";
                final StringBuilder b = new StringBuilder("<html>");
                b.append("<b>Total:</b>&nbsp;" + model.getRowCount() + spaces);
                for(Map.Entry<ProductionType, Integer> e : counts.entrySet())
                {
                    b.append(" <b>" + e.getKey().getDisplayString() + ":</b>&nbsp;" + e.getValue() + spaces);
                }
                b.append("</html>");
                    
                stats.setText(b.toString());
            }
        };
        agent.execute(call, SwingCompletionHandler.newInstance(finish));
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
        else if(ProductionTableModel.class.equals(klass))
        {
            return model;
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
}
