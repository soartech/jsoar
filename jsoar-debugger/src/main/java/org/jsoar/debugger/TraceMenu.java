/*
 * Copyright (c) 2008  Dave Ray <daveray@gmail.com>
 *
 * Created on Oct 9, 2008
 */
package org.jsoar.debugger;

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

import javax.swing.AbstractAction;
import javax.swing.AbstractButton;
import javax.swing.JCheckBoxMenuItem;
import javax.swing.JMenu;
import javax.swing.event.MenuEvent;
import javax.swing.event.MenuListener;

import org.jsoar.kernel.tracing.Trace;
import org.jsoar.kernel.tracing.Trace.Category;

/**
 * @author ray
 */
public class TraceMenu extends JMenu
{
    private static final long serialVersionUID = 5643568728496938352L;
    
    private final Trace trace;
    private final Listener listener = new Listener();
    
    public TraceMenu(Trace trace)
    {
        super("Trace Options");
        
        this.trace = trace;
        
        this.addMenuListener(new MenuListener()
        {
            
            @Override
            public void menuCanceled(MenuEvent arg0)
            {
            }
            
            @Override
            public void menuDeselected(MenuEvent arg0)
            {
            }
            
            @Override
            public void menuSelected(MenuEvent arg0)
            {
                removeAll();
                populateMenu();
            }
        });
        
    }
    
    public void populateMenu()
    {
        removeAll();
        
        add(new TraceEverything("Trace everything"));
        
        add(new TraceNothingAction("Trace nothing"));
        
        final JMenu categories = new JMenu("Details");
        add(categories);
        
        for(Category c : Category.values())
        {
            JCheckBoxMenuItem item = new JCheckBoxMenuItem(c.toString(), trace.isEnabled(c));
            item.setActionCommand(c.toString());
            item.addActionListener(listener);
            categories.add(item);
        }
    }
    
    private final class TraceEverything extends AbstractAction
    {
        private static final long serialVersionUID = 1L;
        
        private TraceEverything(String name)
        {
            super(name);
        }
        
        @Override
        public void actionPerformed(ActionEvent arg0)
        {
            trace.enableAll();
        }
    }
    
    private final class TraceNothingAction extends AbstractAction
    {
        private static final long serialVersionUID = 1L;
        
        private TraceNothingAction(String name)
        {
            super(name);
        }
        
        @Override
        public void actionPerformed(ActionEvent e)
        {
            trace.disableAll();
        }
    }
    
    private class Listener implements ActionListener
    {
        /*
         * (non-Javadoc)
         * 
         * @see java.awt.event.ActionListener#actionPerformed(java.awt.event.ActionEvent)
         */
        @Override
        public void actionPerformed(ActionEvent e)
        {
            AbstractButton b = (AbstractButton) e.getSource();
            Category c = Category.valueOf(e.getActionCommand());
            trace.setEnabled(c, b.isSelected());
        }
    }
}
