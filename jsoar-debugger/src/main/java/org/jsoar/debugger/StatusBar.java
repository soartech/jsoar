/*
 * Copyright (c) 2008  Dave Ray <daveray@gmail.com>
 *
 * Created on Oct 30, 2008
 */
package org.jsoar.debugger;

import java.awt.Color;
import java.awt.Font;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.concurrent.Callable;
import java.util.concurrent.atomic.AtomicReference;

import javax.swing.JLabel;
import javax.swing.Timer;

import org.jdesktop.swingx.JXStatusBar;
import org.jsoar.kernel.Agent;
import org.jsoar.kernel.SoarProperties;
import org.jsoar.kernel.learning.rl.ReinforcementLearning;
import org.jsoar.runtime.CompletionHandler;
import org.jsoar.runtime.SwingCompletionHandler;
import org.jsoar.runtime.ThreadedAgent;
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
        
        final Font boldStatusFont = runState.getFont().deriveFont(Font.BOLD);
        add(runState, fixed(100));
        runState.setFont(boldStatusFont);
        runState.setBackground(new Color(102, 242, 96));
        
        add(phase, fixed((int) (140*JSoarDebugger.getFontScale())));
        phase.setFont(boldStatusFont);
        
        add(decisions, fixed((int) (120*JSoarDebugger.getFontScale())));
        decisions.setFont(boldStatusFont);
        
        add(settings, fill());
        
        // periodically refresh. this is so we get some feedback when the agent 
        // is running.
        final Timer timer = new Timer(1000, new ActionListener()
        {
            @Override
            public void actionPerformed(ActionEvent e)
            {
                refresh(false);
            }
        });
        timer.start();
    }
    
    /* (non-Javadoc)
     * @see org.jsoar.debugger.Refreshable#refresh(boolean)
     */
    public void refresh(boolean afterInitSoar)
    {
        final AtomicReference<String> settingsString = new AtomicReference<String>();
        final Agent a = agent.getAgent();
        
        final Callable<Object> call = new Callable<Object>() {
            @Override
            public Object call() throws Exception
            {
                settingsString.set(getSettings(a));
                return null;
            }};
        final CompletionHandler<Object> finish = new CompletionHandler<Object>() {

            @Override
            public void finish(Object result)
            {
                final boolean running = agent.isRunning();
                String runStateString = running ? "Running" : "Idle";
                if(a.getProperties().get(SoarProperties.WAIT_INFO).waiting)
                {
                    runStateString += " (wait)";
                }
                runState.setText(runStateString);
                runState.setOpaque(running);
                
                phase.setText("Before " + a.getProperties().get(SoarProperties.CURRENT_PHASE).toString().toLowerCase() + " phase");
                
                final Long decisionCount = a.getProperties().get(SoarProperties.DECISION_PHASES_COUNT);
                final String decisionsString = decisionCount + " decision" + (decisionCount != 1 ? "s" : "");
                decisions.setText(decisionsString);
                
                settings.setText(settingsString.get());
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
