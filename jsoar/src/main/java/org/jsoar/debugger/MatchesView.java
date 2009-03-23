/*
 * Copyright (c) 2008  Dave Ray <daveray@gmail.com>
 *
 * Created on Oct 23, 2008
 */
package org.jsoar.debugger;

import java.awt.BorderLayout;
import java.io.StringWriter;
import java.util.ArrayList;
import java.util.EnumSet;
import java.util.List;
import java.util.concurrent.Callable;

import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTextArea;

import org.flexdock.docking.DockingConstants;
import org.jsoar.debugger.selection.SelectionListener;
import org.jsoar.debugger.selection.SelectionManager;
import org.jsoar.kernel.Agent;
import org.jsoar.kernel.Production;
import org.jsoar.kernel.tracing.Printer;
import org.jsoar.kernel.tracing.Trace.MatchSetTraceType;
import org.jsoar.kernel.tracing.Trace.WmeTraceType;
import org.jsoar.runtime.CompletionHandler;
import org.jsoar.runtime.SwingCompletionHandler;
import org.jsoar.util.adaptables.Adaptables;

/**
 * @author ray
 */
public class MatchesView extends AbstractAdaptableView implements SelectionListener
{
    private static final long serialVersionUID = -5150761314645770374L;

    private final JSoarDebugger debugger;
    private final SelectionManager selectionManager;
    private JTextArea textArea = new JTextArea();
    
    public MatchesView(JSoarDebugger debugger)
    {
        super("matches", "Matches");
        
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
        getMatchOutput(new ArrayList<Object>(manager.getSelection()));
    }
    
    private void getMatchOutput(final List<Object> selection)
    {
        Callable<String> matchCall = new Callable<String>() {

            @Override
            public String call() throws Exception
            {
                return safeGetMatchOutput(selection);
            }};
        CompletionHandler<String> finish = new CompletionHandler<String>() {
            @Override
            public void finish(String result)
            {
                textArea.setText(result);
                textArea.setCaretPosition(0);
            }
            
        };
        debugger.getAgentProxy().execute(matchCall, SwingCompletionHandler.newInstance(finish));
    }
    
    private Production getProduction(Agent agent, Object o)
    {
        Production p = Adaptables.adapt(o, Production.class);
        if(p != null)
        {
            return p;
        }
        
        return agent.getProductions().getProduction(o.toString());
    }
    
    private String safeGetMatchOutput(List<Object> selection)
    {
        final Agent agent = debugger.getAgentProxy().getAgent();
        final StringWriter writer = new StringWriter();
        final Printer printer = new Printer(writer, true);
        for(Object o : selection)
        {
            Production p = getProduction(agent, o);
            if(p != null)
            {
                printer.print("*************************************************\n");
                printer.print("*** matches %s\n", p.getName());
                p.printPartialMatches(printer, WmeTraceType.FULL);
                printer.print("\n\n");
            }
        }
        
        printer.print("*** matches\n");
        agent.printMatchSet(printer, 
                WmeTraceType.FULL, 
                EnumSet.of(MatchSetTraceType.MS_ASSERT, MatchSetTraceType.MS_RETRACT));
        
        return writer.toString();
    }
}
