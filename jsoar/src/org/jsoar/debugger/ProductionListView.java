/*
 * Copyright (c) 2008  Dave Ray <daveray@gmail.com>
 *
 * Created on Oct 23, 2008
 */
package org.jsoar.debugger;

import java.awt.BorderLayout;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.List;
import java.util.concurrent.Callable;

import javax.swing.DefaultListModel;
import javax.swing.JList;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.SwingUtilities;

import org.flexdock.docking.DockingConstants;
import org.jsoar.debugger.selection.ListSelectionProvider;
import org.jsoar.debugger.selection.SelectionProvider;
import org.jsoar.kernel.Agent;
import org.jsoar.kernel.Production;


/**
 * @author ray
 */
public class ProductionListView extends AbstractAdaptableView
{
    private static final long serialVersionUID = -5724361674833156058L;
    
    private final LittleDebugger debugger;
    private DefaultListModel prodListModel = new DefaultListModel();
    private JList prodList = new JList(prodListModel);
    private ListSelectionProvider selectionProdiver = new ListSelectionProvider(prodList);
    
    /**
     * @param persistentId
     * @param title
     * @param tabText
     */
    public ProductionListView(LittleDebugger debugger)
    {
        super("productionList", "Productions");
        
        this.debugger = debugger;
        
        this.addAction(DockingConstants.PIN_ACTION);
        
        JPanel p = new JPanel(new BorderLayout());
        p.add(new JScrollPane(prodList), BorderLayout.CENTER);
        
        this.setContentPane(p);
        
        prodList.addMouseListener(new MouseAdapter() {

            /* (non-Javadoc)
             * @see java.awt.event.MouseAdapter#mouseClicked(java.awt.event.MouseEvent)
             */
            @Override
            public void mouseClicked(MouseEvent e)
            {
                if(e.getClickCount() != 2)
                {
                    return;
                }
                Production p = (Production) prodList.getSelectedValue();
                if(p != null)
                {
                    printProduction(p, SwingUtilities.isLeftMouseButton(e));
                }
            }});
    }

    private void printProduction(final Production p, final boolean internal)
    {
        final Agent agent = debugger.getAgentProxy().getAgent();
        
        debugger.getAgentProxy().execute(new Runnable() {

            public void run()
            {
                p.print_production(agent.rete, agent.getPrinter(), internal);
            }});
    }
    
    public void refresh()
    {
        prodListModel.clear();
        
        final Agent agent = debugger.getAgentProxy().getAgent();
        
        Callable<List<Production>> callable = new Callable<List<Production>>() {
            public List<Production> call() throws Exception
            {
                return agent.getProductions(null);
            }};
            
        List<Production> prods = debugger.getAgentProxy().execute(callable);
        for(Production p : prods)
        {
            prodListModel.addElement(p);
        }
    }

    /* (non-Javadoc)
     * @see org.jsoar.debugger.AbstractAdaptableView#getAdapter(java.lang.Class)
     */
    @Override
    public Object getAdapter(Class<?> klass)
    {
        if(SelectionProvider.class.equals(klass))
        {
            return selectionProdiver;
        }
        return super.getAdapter(klass);
    }

}
