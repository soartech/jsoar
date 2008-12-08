/*
 * Copyright (c) 2008  Dave Ray <daveray@gmail.com>
 *
 * Created on Dec 7, 2008
 */
package org.jsoar.demos.toh;

import java.awt.Color;
import java.awt.Graphics;
import java.util.List;

import javax.swing.JPanel;

/**
 * @author ray
 */
public class TohPanel extends JPanel
{
    private static final long serialVersionUID = 7962774839498516994L;
    
    private final Game game;
    
    /**
     * @param game
     */
    public TohPanel(Game game)
    {
        this.game = game;
    }

    /* (non-Javadoc)
     * @see javax.swing.JComponent#paintComponent(java.awt.Graphics)
     */
    protected void paintComponent(Graphics g)
    {
        super.paintComponent(g);
        
        synchronized(game)
        {
            List<Peg> pegs = game.getPegs();
            final int towerSpacing = getWidth() / pegs.size();
            final int towerWidth = 20;
            int i = 0;
            for(Peg peg : pegs)
            {
                final int center = i * towerSpacing + (towerSpacing / 2);
                g.setColor(Color.GRAY);
                g.fillRect(center - towerWidth / 2, 25, towerWidth, getHeight() - 25);
                
                int y = getHeight();
                for(Disk disk : peg.getDisks())
                {
                    int width = 20 + disk.getSize() * 10;
                    int height = 8;
                    g.setColor(Color.BLUE);
                    g.fillRoundRect(center - width / 2, y - height, width, height, 5, 5);
                    y -= height + 2;
                }
                
                ++i;
            }
        }
    }

}
