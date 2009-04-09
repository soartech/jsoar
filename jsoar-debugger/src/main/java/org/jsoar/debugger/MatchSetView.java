/*
 * Copyright (c) 2008  Dave Ray <daveray@gmail.com>
 *
 * Created on Oct 23, 2008
 */
package org.jsoar.debugger;

import java.awt.BorderLayout;
import java.io.StringWriter;
import java.util.EnumSet;
import java.util.concurrent.Callable;

import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTextArea;

import org.flexdock.docking.DockingConstants;
import org.jsoar.kernel.tracing.Printer;
import org.jsoar.kernel.tracing.Trace.MatchSetTraceType;
import org.jsoar.kernel.tracing.Trace.WmeTraceType;
import org.jsoar.runtime.CompletionHandler;
import org.jsoar.runtime.SwingCompletionHandler;
import org.jsoar.runtime.ThreadedAgent;

/**
 * @author ray
 */
public class MatchSetView extends AbstractAdaptableView implements Refreshable
{
    private static final long serialVersionUID = -5150761314645770374L;

    private final ThreadedAgent agent;
    private JTextArea textArea = new JTextArea();
    
    public MatchSetView(ThreadedAgent agent)
    {
        super("matcheset", "Match Set");
        
        this.agent = agent;
        
        addAction(DockingConstants.PIN_ACTION);
        
        JPanel p = new JPanel(new BorderLayout());
        p.add(new JScrollPane(textArea), BorderLayout.CENTER);
        
        setContentPane(p);
    }

    /* (non-Javadoc)
     * @see org.jsoar.debugger.Refreshable#refresh(boolean)
     */
    @Override
    public void refresh(boolean afterInitSoar)
    {
        Callable<String> matchCall = new Callable<String>() {

            @Override
            public String call() throws Exception
            {
                return safeGetMatchOutput();
            }};
        CompletionHandler<String> finish = new CompletionHandler<String>() {
            @Override
            public void finish(String result)
            {
                textArea.setText(result);
                textArea.setCaretPosition(0);
            }
            
        };
        agent.execute(matchCall, SwingCompletionHandler.newInstance(finish));
    }
    
    private String safeGetMatchOutput()
    {
        final StringWriter writer = new StringWriter();
        final Printer printer = new Printer(writer, true);

        agent.getAgent().printMatchSet(printer, 
                WmeTraceType.FULL, 
                EnumSet.of(MatchSetTraceType.MS_ASSERT, MatchSetTraceType.MS_RETRACT));
        
        return writer.toString();
    }
}
