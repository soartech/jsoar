/*
 * Copyright (c) 2008  Dave Ray <daveray@gmail.com>
 *
 * Created on Oct 28, 2008
 */
package org.jsoar.debugger;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.Callable;

import javax.swing.SwingUtilities;
import javax.swing.table.AbstractTableModel;

import org.jsoar.kernel.Production;
import org.jsoar.kernel.events.ProductionAddedEvent;
import org.jsoar.kernel.events.ProductionExcisedEvent;
import org.jsoar.runtime.ThreadedAgentProxy;
import org.jsoar.util.events.SoarEvent;
import org.jsoar.util.events.SoarEventListener;
import org.jsoar.util.events.SoarEventManager;

/**
 * @author ray
 */
public class ProductionTableModel extends AbstractTableModel
{
    private static final long serialVersionUID = -6372714301859379317L;
    
    private final ThreadedAgentProxy agent;
    private final Listener listener = new Listener();
    private final List<Production> productions = Collections.synchronizedList(new ArrayList<Production>());
    
    /**
     * @param agent
     */
    public ProductionTableModel(ThreadedAgentProxy agent)
    {
        this.agent = agent;
    }
    
    public void initialize()
    {
        final SoarEventManager eventManager = this.agent.getAgent().getEventManager();
        eventManager.addListener(ProductionAddedEvent.class, listener);
        eventManager.addListener(ProductionExcisedEvent.class, listener);
        
        this.agent.execute(new Callable<Void>() {

            @Override
            public Void call() throws Exception
            {
                synchronized(productions)
                {
                    productions.addAll(agent.getAgent().getProductions().getProductions(null));
                }
                return null;
            }});
    }
    
    /**
     * @return The list of productions in the model.
     */
    public List<Production> getProductions()
    {
        return productions;
    }

    /* (non-Javadoc)
     * @see javax.swing.table.AbstractTableModel#getColumnClass(int)
     */
    @Override
    public Class<?> getColumnClass(int c)
    {
        switch(c)
        {
        case 0: return Production.class;
        case 1: return Integer.class;
        case 2: return String.class;
        }
        return super.getColumnClass(c);
    }

    /* (non-Javadoc)
     * @see javax.swing.table.AbstractTableModel#getColumnName(int)
     */
    @Override
    public String getColumnName(int c)
    {
        switch(c)
        {
        case 0: return "Name";
        case 1: return "FC";
        case 2: return "Type";
        }
        return super.getColumnName(c);
    }

    /* (non-Javadoc)
     * @see javax.swing.table.TableModel#getColumnCount()
     */
    @Override
    public int getColumnCount()
    {
        return 3;
    }

    /* (non-Javadoc)
     * @see javax.swing.table.TableModel#getRowCount()
     */
    @Override
    public int getRowCount()
    {
        return productions.size();
    }

    /* (non-Javadoc)
     * @see javax.swing.table.TableModel#getValueAt(int, int)
     */
    @Override
    public Object getValueAt(int row, int column)
    {
        synchronized(productions)
        {
            Production p = productions.get(row);
            switch(column)
            {
            case 0: return p;
            case 1: return p.firing_count;
            case 2: return p.getType().getDisplayString();
            }
        }
        return null;
    }
    
    private void handleProductionAdded(Production p)
    {
        int row = 0;
        synchronized (productions)
        {
            row = productions.size();
            productions.add(p);
        }
        fireTableRowsInserted(row, row);
    }
    
    private void handleProductionExcised(Production p)
    {
        int row = 0;
        synchronized (productions)
        {
            row = productions.indexOf(p);
            if(row == -1)
            {
                return;
            }
            productions.remove(row);
        }
        fireTableRowsDeleted(row, row);
    }

    private class Listener implements SoarEventListener
    {
        @Override
        public void onEvent(final SoarEvent event)
        {
            Runnable runnable = new Runnable() {
                public void run()
                {
                    if(event instanceof ProductionAddedEvent)
                    {
                        handleProductionAdded(((ProductionAddedEvent) event).getProduction());
                    }
                    else
                    {
                        handleProductionExcised(((ProductionExcisedEvent) event).getProduction());
                    }
                }
            };
            if(SwingUtilities.isEventDispatchThread())
            {
                runnable.run();
            }
            else
            {
                SwingUtilities.invokeLater(runnable);
            }
        }
        
    }
}
