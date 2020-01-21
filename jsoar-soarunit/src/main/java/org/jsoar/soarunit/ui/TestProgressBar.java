/*
 * Copyright (c) 2010 Dave Ray <daveray@gmail.com>
 *
 * Created on Jul 25, 2010
 */
package org.jsoar.soarunit.ui;

import java.awt.Color;
import java.awt.Dimension;
import java.awt.Graphics;

import javax.swing.JComponent;

/**
 * @author ray
 */
public class TestProgressBar extends JComponent
{
    private static final long serialVersionUID = -87446388667398864L;
    private static final Color FAIL_COLOR = new Color(242, 102, 96);
    private static final Color PASS_COLOR = new Color(102, 242, 96);
    
    private int total;
    private int passed;
    private int failed;
    
    public TestProgressBar()
    {
        setOpaque(true);
        setPreferredSize(new Dimension(16, 16));
    }
    
    public void update(int total, int passed, int failed)
    {
        this.total = total;
        this.passed = passed;
        this.failed = failed;
        repaint();
    }

    /* (non-Javadoc)
     * @see javax.swing.JComponent#paintComponent(java.awt.Graphics)
     */
    @Override
    protected void paintComponent(Graphics g)
    {
        super.paintComponent(g);
        
        final int width = getWidth();
        final int height = getHeight();
        
        g.setColor(getBackground());
        g.fillRect(0, 0, width, height);
        
        if(total == 0)
        {
            return;
        }
        
        final int fillWidth = (int) (((passed + failed) / (double) total) * width);
        
        g.setColor(failed == 0 ? PASS_COLOR : FAIL_COLOR);
        g.fillRect(0, 0, fillWidth, height);
    }
}
