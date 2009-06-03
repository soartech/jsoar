/*
 * Copyright (c) 2009 Dave Ray <daveray@gmail.com>
 *
 * Created on May 10, 2009
 */
package org.jsoar.demos.robot;

import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Font;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Point;
import java.awt.Shape;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.event.MouseWheelEvent;
import java.awt.geom.AffineTransform;
import java.awt.geom.Arc2D;
import java.awt.geom.Ellipse2D;
import java.awt.geom.Line2D;
import java.awt.geom.Point2D;

import javax.swing.JFrame;
import javax.swing.JPanel;
import javax.swing.Timer;

import org.jsoar.util.SwingTools;

/**
 * @author ray
 */
public class WorldPanel extends JPanel 
{
    private double pixelsPerMeter = 55;
    private double panX = 0.0;
    private double panY = 0.0;
    
    private Point lastDrag = null;
    
    private World game;
    
    public WorldPanel(World world)
    {
        this.game = world;
        setBackground(Color.BLACK);
                
        addMouseListener(new MouseAdapter() {

            /* (non-Javadoc)
             * @see java.awt.event.MouseAdapter#mouseReleased(java.awt.event.MouseEvent)
             */
            @Override
            public void mouseReleased(MouseEvent e)
            {
                lastDrag = null;
            }
        });
        
        addMouseWheelListener(new MouseAdapter() {
            /* (non-Javadoc)
             * @see java.awt.event.MouseAdapter#mouseWheelMoved(java.awt.event.MouseWheelEvent)
             */
            @Override
            public void mouseWheelMoved(MouseWheelEvent e)
            {
                if(e.getScrollType() == MouseWheelEvent.WHEEL_UNIT_SCROLL)
                {
                    pixelsPerMeter += e.getWheelRotation() * 0.5;
                    if(pixelsPerMeter <= 0)
                    {
                        pixelsPerMeter = 1;
                    }
                    repaint();
                }
            }
        });
        addMouseMotionListener(new MouseAdapter() {

            /* (non-Javadoc)
             * @see java.awt.event.MouseAdapter#mouseDragged(java.awt.event.MouseEvent)
             */
            @Override
            public void mouseDragged(MouseEvent e)
            {
                if(lastDrag != null)
                {
                    int dx = e.getPoint().x - lastDrag.x;
                    int dy = e.getPoint().y - lastDrag.y;
                    
                    panX += dx / pixelsPerMeter;
                    panY += -dy / pixelsPerMeter; 
                    
                    lastDrag.setLocation(e.getPoint());
                    repaint();
                }
                else
                {
                    lastDrag = new Point(e.getPoint());
                }
            }});
    }
    
    private void drawShape(Graphics2D g2d, Shape shape, Color fill, Color stroke)
    {
        g2d.setColor(fill);
        g2d.fill(shape);
        g2d.setColor(stroke);
        g2d.draw(shape);
    }
    
    /* (non-Javadoc)
     * @see javax.swing.JComponent#paintComponent(java.awt.Graphics)
     */
    @Override
    protected void paintComponent(Graphics g)
    {
        SwingTools.enableAntiAliasing(g);
        super.paintComponent(g);
        
        final Graphics2D g2d = (Graphics2D) g;
        
        final AffineTransform transform = new AffineTransform();
        transform.translate(getWidth() / 2.0, getHeight() / 2.0);
        transform.scale(pixelsPerMeter, -pixelsPerMeter); // 
        transform.translate(panX, panY);
        g2d.transform(transform);

        g2d.setStroke(new BasicStroke(2 * (1.0f / (float)pixelsPerMeter)));
        
        drawShape(g2d, game.extents, Color.LIGHT_GRAY, Color.BLUE);
        
        for(Robot robot : game.getRobots())
        {
            drawRobot(g2d, robot);
        }
       
        for(Shape s : game.getObstacles())
        {
            drawObstacle(g2d, s);
        }
        
        for(Waypoint w : game.getWaypoints())
        {
            drawWaypoint(g2d, w);
        }
        
        final double fontHeight = 12 * (1.0f / (float) pixelsPerMeter);
        final AffineTransform fontTransform = AffineTransform.getScaleInstance(1.0, -1.0);
        final Font font = g2d.getFont().deriveFont((float) fontHeight).deriveFont(fontTransform);
        g2d.setFont(font);
        for(double y = game.extents.getMinY(); y <= game.extents.getMaxY() ; y += 1.0)
        {
            g2d.drawString(String.format("%8.1f", y), 0.0f, (float) y);
        }
        for(double x = game.extents.getMinX(); x <= game.extents.getMaxX() ; x += 1.0)
        {
            g2d.drawString(String.format("%8.1f", x), (float) x, 0.0f);
        }
    }
    
    private void drawRobot(Graphics2D g2dIn, Robot robot)
    {
        final Graphics2D g2d = (Graphics2D) g2dIn.create();
        final AffineTransform transform = new AffineTransform();
        transform.translate(robot.shape.getCenterX(), robot.shape.getCenterY());
        transform.rotate(robot.yaw);
        g2d.transform(transform);
        
        drawRanges(g2d, robot);
        
        final double r = robot.radius;
        final Ellipse2D body = new Ellipse2D.Double(-r, -r, r * 2.0, r * 2.0);
        drawShape(g2d, body, Color.WHITE, Color.BLACK);
        
        final double dirR = r / 5.0;
        final Ellipse2D dir = new Ellipse2D.Double(r - dirR, -dirR, dirR * 2.0, dirR * 2.0);
        drawShape(g2d, dir, Color.RED, Color.BLACK);
        
        g2d.dispose();
    }
    
    private void drawObstacle(Graphics2D g2dIn, Shape shape)
    {
        final Graphics2D g2d = (Graphics2D) g2dIn.create();

        drawShape(g2d, shape, Color.GRAY, Color.BLACK);
        g2d.dispose();
    }
    
    private void drawRanges(Graphics2D g2dIn, Robot robot)
    {
        for(RadarRange range : robot.ranges)
        {
            final Arc2D arc = new Arc2D.Double(-range.range, -range.range, 2 * range.range, 2 * range.range, 
                                Math.toDegrees(-range.angle) - 1.0, 2.0, Arc2D.PIE);
            drawShape(g2dIn, arc, Color.GREEN, Color.GREEN);
        }
    }
    
    private void drawWaypoint(Graphics2D g2dIn, Waypoint waypoint)
    {
        final Point2D p = waypoint.point;
        final double r = 0.2;
        final Ellipse2D circle = new Ellipse2D.Double(p.getX() - r, p.getY() - r, 2 * r, 2 * r);
        
        drawShape(g2dIn, circle, Color.ORANGE, Color.BLUE);
        
        g2dIn.draw(new Line2D.Double(p.getX(), p.getY() - 1.5 * r, p.getX(), p.getY() + 1.5 * r));
        g2dIn.draw(new Line2D.Double(p.getX() - 1.5 * r, p.getY(), p.getX() + 1.5 * r, p.getY()));
    }    
}
