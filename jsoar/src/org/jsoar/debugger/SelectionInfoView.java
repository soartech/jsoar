/*
 * Copyright (c) 2008  Dave Ray <daveray@gmail.com>
 *
 * Created on Oct 23, 2008
 */
package org.jsoar.debugger;

import java.awt.BorderLayout;
import java.io.StringWriter;
import java.util.concurrent.Callable;

import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTextArea;

import org.flexdock.docking.DockingConstants;
import org.jsoar.debugger.selection.SelectionListener;
import org.jsoar.debugger.selection.SelectionManager;
import org.jsoar.kernel.Production;
import org.jsoar.kernel.tracing.Printer;
import org.jsoar.util.adaptables.Adaptables;

/**
 * @author ray
 */
public class SelectionInfoView extends AbstractAdaptableView implements SelectionListener
{
    private static final long serialVersionUID = -5150761314645770374L;

    private final LittleDebugger debugger;
    private final SelectionManager selectionManager;
    private JTextArea textArea = new JTextArea();
    
    public SelectionInfoView(LittleDebugger debugger)
    {
        super("info", "Selection Info");
        
        this.debugger = debugger;
        this.selectionManager = debugger.getSelectionManager();
        
        addAction(DockingConstants.PIN_ACTION);
        
        JPanel p = new JPanel(new BorderLayout());
        p.add(new JScrollPane(textArea), BorderLayout.CENTER);
        
        setContentPane(p);

        this.selectionManager.addListener(this);
    }

    /* (non-Javadoc)
     * @see org.jsoar.debugger.selection.SelectionListener#selectionChanged(org.jsoar.debugger.selection.SelectionManager)
     */
    @Override
    public void selectionChanged(SelectionManager manager)
    {
        textArea.setText("");
        
        if(manager.getSelectedObject() == null)
        {
            return;
        }
        StringBuilder result = new StringBuilder();
        for(Object o : manager.getSelection())
        {
            result.append(getObjectText(o));
            result.append('\n');
        }
        textArea.setText(result.toString());
        textArea.setCaretPosition(0);
    }
    
    private String getObjectText(Object o)
    {
        final Production p = Adaptables.adapt(o, Production.class);
        if(p != null)
        {
            return debugger.getAgentProxy().execute(new Callable<String>() {

                public String call() throws Exception
                {
                    StringWriter s = new StringWriter();
                    p.print_production(new Printer(s, true), true);
                    return s.toString();
                }});
        }
        else
        {
            return o.toString();
        }
    }
}
