/*
 * Copyright (c) 2008  Dave Ray <daveray@gmail.com>
 *
 * Created on Oct 30, 2008
 */
package org.jsoar.debugger;

import java.awt.BorderLayout;
import java.awt.Dimension;
import java.util.concurrent.Callable;

import javax.swing.BorderFactory;
import javax.swing.JLabel;
import javax.swing.JPanel;

import org.jsoar.kernel.Agent;
import org.jsoar.kernel.SoarProperties;
import org.jsoar.runtime.CompletionHandler;
import org.jsoar.runtime.SwingCompletionHandler;
import org.jsoar.runtime.ThreadedAgent;
import org.jsoar.util.ByRef;

/**
 * @author ray
 */
public class StatusBar extends JPanel implements Refreshable
{
    private static final long serialVersionUID = 1501760828755152573L;

    private final ThreadedAgent agent;
    private final JLabel runState = new JLabel("run state");
    private final JLabel phase = new JLabel("phase");
    private final JLabel decisions = new JLabel("decisions");
    private final JLabel settings = new JLabel("Status");
    
    public StatusBar(ThreadedAgent agent)
    {
        super(new BorderLayout());
        
        this.agent = agent;
        
        runState.setBorder(BorderFactory.createEtchedBorder());
        runState.setPreferredSize(new Dimension(100, 25));
        phase.setBorder(BorderFactory.createEtchedBorder());
        phase.setPreferredSize(new Dimension(100, 25));
        phase.setMaximumSize(new Dimension(100, 25));
        decisions.setBorder(BorderFactory.createEtchedBorder());
        decisions.setPreferredSize(new Dimension(100, 25));
        decisions.setMaximumSize(new Dimension(100, 25));
        
        settings.setBorder(BorderFactory.createEtchedBorder());
        
        JPanel left = new JPanel(new BorderLayout());
        left.add(runState, BorderLayout.WEST);
        left.add(phase, BorderLayout.CENTER);
        left.add(decisions, BorderLayout.EAST);
        
        add(left, BorderLayout.WEST);
        add(settings, BorderLayout.CENTER);
    }
    
    public void refresh(boolean afterInitSoar)
    {
        final ByRef<String> runStateString = ByRef.create(null);
        final ByRef<String> phaseString = ByRef.create(null);
        final ByRef<String> decisionsString = ByRef.create(null);
        final ByRef<String> settingsString = ByRef.create(null);
        final Agent a = agent.getAgent();
        
        final Callable<Object> call = new Callable<Object>() {
            @Override
            public Object call() throws Exception
            {
                runStateString.value = agent.isRunning() ? "Running" : "Idle";
                if(a.getProperties().get(SoarProperties.WAITING))
                {
                    runStateString.value += " (wait)";
                }
                phaseString.value = a.getCurrentPhase().toString().toLowerCase();
                decisionsString.value = a.getProperties().get(SoarProperties.DECISION_PHASES_COUNT) + " decisions";
                settingsString.value = getSettings(a);
                return null;
            }};
        final CompletionHandler<Object> finish = new CompletionHandler<Object>() {

            @Override
            public void finish(Object result)
            {
                
                runState.setText("<html><b>" + runStateString.value + "</b></html>");
                phase.setText("<html><b>before " + phaseString.value + "</b></html>");
                decisions.setText("<html><b>" + decisionsString.value + "</b></html>");
                settings.setText(settingsString.value);
            }
            
        };
        agent.execute(call, SwingCompletionHandler.newInstance(finish));
    }
    
    private String getSettings(Agent a)
    {
        StringBuilder b = new StringBuilder("<html>");
        b.append(status("warnings", a.getPrinter().isPrintWarnings()) + ", ");
        b.append(status("waitsnc", a.getProperties().get(SoarProperties.WAITSNC)) + ", ");
        b.append(status("learn", a.getProperties().get(SoarProperties.LEARNING_ON)) + ", ");
        b.append(status("rl", a.rl.rl_enabled()) + ", ");
        b.append(status("save-backtraces", a.getProperties().get(SoarProperties.EXPLAIN)));
        b.append("</html>");
        
        return b.toString();
    }
    
    private String status(String name, boolean value)
    {
        return "<b>" + name + ":</b>&nbsp;" + (value ? "on" : "off");
    }
}
