/*
 * Copyright (c) 2008  Dave Ray <daveray@gmail.com>
 *
 * Created on Oct 30, 2008
 */
package org.jsoar.debugger;

import java.util.concurrent.Callable;

import javax.swing.JLabel;

import org.jdesktop.swingx.JXStatusBar;
import org.jsoar.kernel.Agent;
import org.jsoar.kernel.SoarProperties;
import org.jsoar.kernel.learning.rl.ReinforcementLearning;
import org.jsoar.runtime.CompletionHandler;
import org.jsoar.runtime.SwingCompletionHandler;
import org.jsoar.runtime.ThreadedAgent;
import org.jsoar.util.ByRef;
import org.jsoar.util.adaptables.Adaptables;

/**
 * @author ray
 */
public class StatusBar extends JXStatusBar implements Refreshable
{
    private static final long serialVersionUID = 1501760828755152573L;

    private final ThreadedAgent agent;
    private final JLabel runState = new JLabel("run state");
    private final JLabel phase = new JLabel("phase");
    private final JLabel decisions = new JLabel("decisions");
    private final JLabel settings = new JLabel("Status");
        
    public StatusBar(ThreadedAgent agent)
    {
        this.agent = agent;
        
        add(runState, fixed(100));
        add(phase, fixed(100));
        add(decisions, fixed(120));
        add(settings, fill());
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
                if(a.getProperties().get(SoarProperties.WAIT_INFO).waiting)
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
        final StringBuilder b = new StringBuilder("<html>");
        b.append(status("warnings", a.getPrinter().isPrintWarnings()) + ", ");
        b.append(status("waitsnc", a.getProperties().get(SoarProperties.WAITSNC)) + ", ");
        b.append(status("learn", a.getProperties().get(SoarProperties.LEARNING_ON)) + ", ");
        final ReinforcementLearning rl = Adaptables.adapt(a, ReinforcementLearning.class);
        b.append(status("rl", rl.rl_enabled()) + ", ");
        b.append(status("save-backtraces", a.getProperties().get(SoarProperties.EXPLAIN)));
        b.append("</html>");
        
        return b.toString();
    }
    
    private String status(String name, boolean value)
    {
        return "<b>" + name + ":</b>&nbsp;" + (value ? "on" : "off");
    }
    
    private JXStatusBar.Constraint fixed(int width)
    {
        return new JXStatusBar.Constraint(width);
    }
    
    private JXStatusBar.Constraint fill()
    {
        return new JXStatusBar.Constraint(JXStatusBar.Constraint.ResizeBehavior.FILL);
    }
    
}
