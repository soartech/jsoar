/*
 * Copyright (c) 2008  Dave Ray <daveray@gmail.com>
 *
 * Created on Dec 4, 2008
 */
package org.jsoar.demos.toh;

import javax.swing.SwingUtilities;

import org.flexdock.docking.DockingConstants;
import org.jsoar.debugger.AbstractAdaptableView;
import org.jsoar.debugger.JSoarDebuggerPlugin;
import org.jsoar.debugger.JSoarDebugger;
import org.jsoar.debugger.TraceView;
import org.jsoar.kernel.events.BeforeInitSoarEvent;
import org.jsoar.kernel.events.InputCycleEvent;
import org.jsoar.kernel.events.OutputEvent;
import org.jsoar.util.adaptables.Adaptables;
import org.jsoar.util.events.SoarEvent;
import org.jsoar.util.events.SoarEventListener;
import org.jsoar.util.events.SoarEventManager;

/**
 * @author ray
 */
public class TowersOfHanoi extends AbstractAdaptableView implements JSoarDebuggerPlugin
{
    private static final long serialVersionUID = -8069709839874209508L;
    
    private Game game;
    private TohPanel panel;
    
    public TowersOfHanoi()
    {
        super("toh", "Towers of Hanoi");
    }
    
    /* (non-Javadoc)
     * @see org.jsoar.debugger.JSoarDebuggerPlugin#initialize(org.jsoar.debugger.LittleDebugger, java.lang.String[])
     */
    @Override
    public void initialize(JSoarDebugger debugger, String[] args)
    {
        
        TraceView trace = Adaptables.adapt(debugger, TraceView.class);
        trace.dock(this, DockingConstants.SOUTH_REGION);
        
        SoarEventManager em = debugger.getAgentProxy().getAgent().getEventManager();
        
        initialize(em, args);
    }

    /**
     * @param em
     * @param args
     */
    private void initialize(SoarEventManager em, String[] args)
    {
        int numPegs = 3, numDisks = 11;
        if(args.length == 2)
        {
            numPegs = Integer.valueOf(args[0]);
            numDisks = Integer.valueOf(args[1]);
        }
        this.game = new Game(numPegs, numDisks);
        this.panel = new TohPanel(game);

        this.setContentPane(panel);
        
        // update the input from the game each input cycle.
        em.addListener(InputCycleEvent.class, new SoarEventListener() {

            @Override
            public void onEvent(SoarEvent event)
            {
                game.update(((InputCycleEvent) event).getInputOutput());
            }});
        
        // handle output commands from the agent
        em.addListener(OutputEvent.class, new SoarEventListener() {

            @Override
            public void onEvent(SoarEvent event)
            {
                game.handleCommands((OutputEvent) event);
                synchDisplay();
            }});
        
        // when the agent is reinitialized (init-soar), reset the game
        em.addListener(BeforeInitSoarEvent.class, new SoarEventListener() {

            @Override
            public void onEvent(SoarEvent event)
            {
                game.reset();
                synchDisplay();
            }});
    }
    
    private void synchDisplay()
    {
        if(SwingUtilities.isEventDispatchThread())
        {
            panel.repaint();
        }
        else
        {
            SwingUtilities.invokeLater(new Runnable() { public void run() { synchDisplay(); }});
        }
    }
}
