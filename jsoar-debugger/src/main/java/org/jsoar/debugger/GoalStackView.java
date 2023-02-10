/*
 * Copyright (c) 2010 Dave Ray <daveray@gmail.com>
 *
 * Created on Jul 21, 2010
 */
package org.jsoar.debugger;

import java.awt.Color;
import java.awt.Component;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Callable;

import javax.swing.BorderFactory;
import javax.swing.DefaultListCellRenderer;
import javax.swing.DefaultListModel;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JScrollPane;
import javax.swing.SwingUtilities;

import org.jsoar.debugger.selection.ListSelectionProvider;
import org.jsoar.debugger.selection.SelectionProvider;
import org.jsoar.kernel.Goal;
import org.jsoar.kernel.symbols.Identifier;
import org.jsoar.kernel.symbols.Symbol;
import org.jsoar.runtime.CompletionHandler;

/**
 * @author ray
 */
public class GoalStackView extends AbstractAdaptableView implements Refreshable
{
    private final JSoarDebugger debugger;
    private final DefaultListModel<Entry> model = new DefaultListModel<>();
    private final JList<Entry> list = new JList<Entry>(model)
    {
        private static final long serialVersionUID = -1363240384388636598L;
        
        public String getToolTipText(MouseEvent event)
        {
            final int index = locationToIndex(event.getPoint());
            return index >= 0 ? getModel().getElementAt(index).toString() : null;
        }
    };
    
    private final ListSelectionProvider<Entry> selectionProvider = new ListSelectionProvider<>(list);
    
    public GoalStackView(JSoarDebugger debugger)
    {
        super("goalStack", "State Stack");
        
        this.debugger = debugger;
        
        list.setCellRenderer(new Renderer());
        list.addMouseListener(new MouseAdapter()
        {
            /*
             * (non-Javadoc)
             * 
             * @see java.awt.event.MouseAdapter#mouseClicked(java.awt.event.MouseEvent)
             */
            @Override
            public void mouseClicked(MouseEvent e)
            {
                if(SwingUtilities.isLeftMouseButton(e) && e.getClickCount() == 2)
                {
                    printOnDoubleClick();
                }
            }
            
        });
        getContentPane().add(new JScrollPane(list));
    }
    
    private void printOnDoubleClick()
    {
        final Entry e = (Entry) list.getSelectedValue();
        if(e == null)
        {
            return;
        }
        
        debugger.getAgent().execute(new CommandLineRunnable(debugger, "print " + e.object), null);
    }
    
    /*
     * (non-Javadoc)
     * 
     * @see org.jsoar.debugger.AbstractAdaptableView#getShortcutKey()
     */
    @Override
    public String getShortcutKey()
    {
        return "ctrl G";
    }
    
    /*
     * (non-Javadoc)
     * 
     * @see org.jsoar.debugger.Refreshable#refresh(boolean)
     */
    @Override
    public void refresh(boolean afterInitSoar)
    {
        final Callable<List<Entry>> start = () -> {
            final List<Goal> goals = debugger.getAgent().getAgent().getGoalStack();
            final List<Entry> result = new ArrayList<Entry>();
            for(Goal g : goals)
            {
                result.add(createGoalEntry(g));
                final Entry op = createOperatorEntryForGoal(g);
                if(op != null)
                {
                    result.add(op);
                }
            }
            return result;
        };
        
        final CompletionHandler<List<Entry>> finish = result -> updateModel(result);
        debugger.getAgent().execute(start, finish);
    }
    
    private Entry createGoalEntry(Goal g)
    {
        return new Entry(g.getIdentifier(), g.getIdentifier().toString());
    }
    
    private Entry createOperatorEntryForGoal(Goal g)
    {
        final Identifier op = g.getOperator();
        if(op == null)
        {
            return null;
        }
        
        final Symbol name = g.getOperatorName();
        
        String label = op.toString();
        
        if(name != null)
        {
            label += " (" + name.toString() + ")";
        }
        
        return new Entry(op, label);
    }
    
    private void updateModel(List<Entry> newStack)
    {
        model.clear();
        for(Entry g : newStack)
        {
            model.addElement(g);
        }
    }
    
    /*
     * (non-Javadoc)
     * 
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
    
    private class Renderer extends DefaultListCellRenderer
    {
        private static final long serialVersionUID = 8598881348069202700L;
        
        /*
         * (non-Javadoc)
         * 
         * @see javax.swing.DefaultListCellRenderer#getListCellRendererComponent(javax.swing.JList, java.lang.Object, int, boolean, boolean)
         */
        @Override
        public Component getListCellRendererComponent(JList<?> list, Object value,
                int index, boolean isSelected, boolean cellHasFocus)
        {
            final JLabel label = (JLabel) super.getListCellRendererComponent(list, value, index, isSelected,
                    cellHasFocus);
            
            final Entry entry = (Entry) value;
            if(entry.object.isGoal())
            {
                label.setIcon(Images.GOAL);
                label.setBorder(BorderFactory.createMatteBorder(1, 0, 0, 0, Color.LIGHT_GRAY));
            }
            else
            {
                label.setIcon(Images.OPERATOR);
                label.setIconTextGap(16);
            }
            return label;
        }
    }
    
    private static class Entry
    {
        final Identifier object;
        final String label;
        
        public Entry(Identifier object, String label)
        {
            this.object = object;
            this.label = label;
        }
        
        public String toString()
        {
            return label;
        }
    }
}
