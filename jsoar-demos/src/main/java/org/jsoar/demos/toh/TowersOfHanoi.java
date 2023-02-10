/*
 * Copyright (c) 2008  Dave Ray <daveray@gmail.com>
 *
 * Created on Dec 4, 2008
 */
package org.jsoar.demos.toh;

import java.awt.BorderLayout;

import javax.swing.JFrame;
import javax.swing.JPanel;
import javax.swing.SwingUtilities;

import org.jsoar.debugger.JSoarDebugger;
import org.jsoar.debugger.JSoarDebuggerPlugin;
import org.jsoar.kernel.events.BeforeInitSoarEvent;
import org.jsoar.kernel.events.InputEvent;
import org.jsoar.kernel.events.OutputEvent;
import org.jsoar.util.events.SoarEventManager;

/**
 * @author ray
 */
public class TowersOfHanoi extends /* AbstractAdaptableView */ JPanel implements JSoarDebuggerPlugin
{
    private static final long serialVersionUID = -8069709839874209508L;
    
    private Game game;
    private TohPanel panel;
    
    public TowersOfHanoi()
    {
        // super("toh", "Towers of Hanoi");
    }
    
    /*
     * (non-Javadoc)
     * 
     * @see org.jsoar.debugger.JSoarDebuggerPlugin#initialize(org.jsoar.debugger.LittleDebugger, java.lang.String[])
     */
    @Override
    public void initialize(JSoarDebugger debugger, String[] args)
    {
        final JFrame f = new JFrame("JSoar - Towers of Hanoi");
        f.setDefaultCloseOperation(JFrame.DO_NOTHING_ON_CLOSE);
        f.add(this);
        f.setSize(640, 240);
        f.setVisible(true);
        
        SoarEventManager em = debugger.getAgent().getEvents();
        
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
        
        setLayout(new BorderLayout());
        this.add(panel, BorderLayout.CENTER);
        
        // update the input from the game each input cycle.
        em.addListener(InputEvent.class, event -> game.update(((InputEvent) event).getInputOutput()));
        
        // handle output commands from the agent
        em.addListener(OutputEvent.class, event ->
        {
            game.handleCommands((OutputEvent) event);
            synchDisplay();
        });
        
        // when the agent is reinitialized (init-soar), reset the game
        em.addListener(BeforeInitSoarEvent.class, event ->
        {
            game.reset();
            synchDisplay();
        });
    }
    
    private void synchDisplay()
    {
        if(SwingUtilities.isEventDispatchThread())
        {
            panel.repaint();
        }
        else
        {
            SwingUtilities.invokeLater(this::synchDisplay);
        }
    }
}
