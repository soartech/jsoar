/*
 * Copyright (c) 2010 Dave Ray <daveray@gmail.com>
 *
 * Created on Jul 21, 2010
 */
package org.jsoar.debugger;

import java.awt.BorderLayout;

import javax.swing.JPanel;

import org.jsoar.debugger.selection.SelectionProvider;
import org.jsoar.debugger.wm.WorkingMemoryTree;

/**
 * @author ray
 */
public class WorkingMemoryView extends AbstractAdaptableView implements Refreshable
{
    private final JSoarDebugger debugger;
    private final WorkingMemoryTree tree;

    public WorkingMemoryView(JSoarDebugger debugger)
    {
        super("workingMemory", "Working Memory");
        
        this.debugger = debugger;
        this.tree = new WorkingMemoryTree(this.debugger.getAgent());
        tree.addRoot("<s>");
        tree.addRoot("<o>");
        
        final JPanel panel = new JPanel(new BorderLayout());
        
        panel.add(tree, BorderLayout.CENTER);
        getContentPane().add(panel);
        
    }
    
    /* (non-Javadoc)
     * @see org.jsoar.debugger.AbstractAdaptableView#getShortcutKey()
     */
    @Override
    public String getShortcutKey()
    {
        return "ctrl shift W";
    }

    /* (non-Javadoc)
     * @see org.jsoar.debugger.Refreshable#refresh(boolean)
     */
    @Override
    public void refresh(boolean afterInitSoar)
    {
        tree.updateModel();
    }
    
    /* (non-Javadoc)
     * @see org.jsoar.debugger.AbstractAdaptableView#getAdapter(java.lang.Class)
     */
    @Override
    public Object getAdapter(Class<?> klass)
    {
        if(SelectionProvider.class.equals(klass))
        {
            return tree.getSelectionProvider();
        }
        return super.getAdapter(klass);
    }
}
