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
import org.jsoar.kernel.events.ProductionAddedEvent;
import org.jsoar.kernel.events.ProductionExcisedEvent;
import org.jsoar.runtime.CompletionHandler;
import org.jsoar.runtime.ThreadedAgent;
import org.jsoar.util.adaptables.Adaptable;
import org.jsoar.util.adaptables.Adaptables;
import org.jsoar.util.events.SoarEvent;
import org.jsoar.util.events.SoarEventListener;


/**
 * @author ray
 */
public class ProductionListView extends AbstractAdaptableView implements Refreshable, SelectionListener
{
    private final ThreadedAgent agent;
    private final ActionManager actionManager;
    private final ProductionTableModel model;
    private final JXTable table;
    private final JLabel stats = new JLabel();
    private final TableSelectionProvider selectionProvider;
    
    /**
     * @param debuggerIn The owning debugger
     */
    public ProductionListView(Adaptable debuggerIn)
    {
        super("productionList", "Productions");
        
        this.agent = Adaptables.adapt(debuggerIn, ThreadedAgent.class);
        this.actionManager = Adaptables.adapt(debuggerIn, ActionManager.class);
        
        final JPanel p = new JPanel(new BorderLayout());
        
        this.model = new ProductionTableModel(this.agent);
        this.model.initialize();
        this.table = new JXTable(this.model);
        this.table.setColumnControlVisible(true);
        // Order is important here!
        this.table.getColumnExt(2).setVisible(false);
        this.table.getColumnExt(1).setVisible(false);
        
        this.selectionProvider = new TableSelectionProvider(this.table);
        
        this.actionManager.getSelectionManager().addListener(this);
        
        this.table.setDefaultRenderer(Production.class, new NameCellRenderer());
        
        final DefaultTableCellRenderer fcRenderer = new DefaultTableCellRenderer();
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
        
        JPanel bottom = new JPanel(new BorderLayout());
        
        bottom.add(new TableFilterPanel(table, 0), BorderLayout.NORTH);
        bottom.add(stats, BorderLayout.CENTER);
        
        p.add(bottom, BorderLayout.SOUTH);
        
        this.getContentPane().add(p);
        
        this.agent.execute(() -> {
            this.agent.getAgent().getEvents().addListener(ProductionAddedEvent.class, eventListener);
            this.agent.getAgent().getEvents().addListener(ProductionExcisedEvent.class, eventListener);
        });
    }
    
    private final SoarEventListener eventListener = new SoarEventListener() {

        @Override
        public void onEvent(SoarEvent event)
        {
            // unregister for events, since we only want to refresh once
            agent.getAgent().getEvents().removeListener(ProductionAddedEvent.class, this);
            agent.getAgent().getEvents().removeListener(ProductionExcisedEvent.class, this);
            
            // on the swing thread, schedule another Soar thread execution
            // we do this through the swing thread so it ends up at the end of the Soar agent queue -- otherwise it will execute right away,
            // and we don't want it to execute until after the productions are loaded (e.g., the source command completes)
            SwingUtilities.invokeLater(() -> {
                agent.execute( () -> {
                    // by this time, the production sourcing should be complete, so schedule the refresh on the swing thread and re-register for the events
                    SwingUtilities.invokeLater(() -> refresh(false));
                    agent.getAgent().getEvents().addListener(ProductionAddedEvent.class, eventListener);
                    agent.getAgent().getEvents().addListener(ProductionExcisedEvent.class, eventListener);
                });
            });
            
        }
    };
    
    /* (non-Javadoc)
     * @see org.jsoar.debugger.Refreshable#refresh(boolean)
     */
    public void refresh(boolean afterInitSoar)
    {
        this.table.repaint();
        this.table.packAll();
        
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
            setIcon(column == 0 ? 
                        (p.isBreakpointEnabled() ? Images.PRODUCTION_BREAK : Images.PRODUCTION) 
                    : null);
            setText(p.getName());
            final long fc = p.getFiringCount();
            setToolTipText("<html>" +
                           "<b>" + p.getName() + "</b><br>" +
                            p.getType().getDisplayString() + " production<br>" +
                           "Fired " + fc + " time" + (fc != 1 ? "s" : "") +
                           (p.isBreakpointEnabled() ? "<br>:interrupt" : "") + "<p>" +
                            p.getDocumentation() + "</html>");
            return c;
        }
    }
}
