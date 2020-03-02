/*
 * Copyright (c) 2010 Dave Ray <daveray@gmail.com>
 *
 * Created on Jul 21, 2010
 */
package org.jsoar.debugger;

import java.awt.BorderLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Callable;

import javax.swing.BorderFactory;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JTextField;

import org.jsoar.debugger.selection.SelectionProvider;
import org.jsoar.debugger.util.SwingTools;
import org.jsoar.debugger.wm.WorkingMemoryTree;
import org.jsoar.kernel.symbols.Identifier;
import org.jsoar.kernel.symbols.Symbols;

/**
 * @author ray
 */
public class WorkingMemoryView extends AbstractAdaptableView implements Refreshable
{
    private final JSoarDebugger debugger;
    private final JTextField roots = new JTextField("<s> <o>");
    private final WorkingMemoryTree tree;

    public WorkingMemoryView(JSoarDebugger debugger)
    {
        super("workingMemory", "Working Memory");
        
        this.debugger = debugger;
        this.tree = new WorkingMemoryTree(this.debugger.getAgent());
        tree.addRoot("<s>");
        tree.addRoot("<o>");
        
        final JPanel header = new JPanel(new BorderLayout());
        header.setBorder(BorderFactory.createEmptyBorder(4, 4, 4, 4));
        header.add(new JLabel("Roots:"), BorderLayout.WEST);
        header.add(roots);
        //PromptSupport.setPrompt("Enter ids and vars here, e.g. <s> <o> S1 I2 ...", roots);
        roots.addActionListener(new ActionListener(){
            @Override
            public void actionPerformed(ActionEvent e)
            {
                addRoots();
            }
        });
        SwingTools.addSelectAllOnFocus(roots);
        
        final JPanel treePanel = new JPanel(new BorderLayout());
        treePanel.setBorder(BorderFactory.createEmptyBorder(4, 4, 4, 4));
        treePanel.add(tree, BorderLayout.CENTER);
        
        final JPanel panel = new JPanel(new BorderLayout());
        panel.add(header, BorderLayout.SOUTH);
        tree.setBorder(BorderFactory.createLoweredBevelBorder());
        panel.add(treePanel, BorderLayout.CENTER);
        getContentPane().add(panel);
    }
    
    private void addRoots()
    {
        final String[] parts = roots.getText().trim().split("\\s+");
        final Callable<Void> call = new Callable<Void>() {
            @Override
            public Void call() throws Exception
            {
                final List<Object> newKeys = new ArrayList<Object>();
                for(String p : parts)
                {
                    if(p.startsWith("<") && p.endsWith(">"))
                    {
                        newKeys.add(p);
                    }
                    else if(!p.isEmpty())
                    {
                        final Identifier id = Symbols.parseIdentifier(debugger.getAgent().getSymbols(), p);
                        if(id != null)
                        {
                            newKeys.add(id);
                        }
                    }
                }
                
                for(Object key : newKeys)
                {
                    tree.addRoot(key);
                }
                return null;
            }
        };
        debugger.getAgent().execute(call, null);
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
