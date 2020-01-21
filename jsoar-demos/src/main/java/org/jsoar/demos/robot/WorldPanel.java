/*
 * Copyright (c) 2009 Dave Ray <daveray@gmail.com>
 *
 * Created on May 10, 2009
 */
package org.jsoar.demos.robot;

import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Point;
import java.awt.Rectangle;
import java.awt.Shape;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.event.MouseWheelEvent;
import java.awt.geom.AffineTransform;
import java.awt.geom.Arc2D;
import java.awt.geom.Ellipse2D;
import java.awt.geom.Point2D;
import java.awt.geom.Rectangle2D;

import javax.swing.JPanel;

import org.jsoar.util.SwingTools;

/**
 * @author ray
 */
public class WorldPanel extends JPanel 
{
	private static final long serialVersionUID = -8738777236222717325L;

	private double pixelsPerMeter = 55;
    private double panX = 0.0;
    private double panY = 0.0;
    private Robot follow = null;
    
    private Point lastDrag = null;
    
    private World world = new World();
    private Robot selection = null;
    
    public WorldPanel()
    {
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

            /* (non-Javadoc)
             * @see java.awt.event.MouseAdapter#mouseClicked(java.awt.event.MouseEvent)
             */
            @Override
            public void mouseClicked(MouseEvent e)
            {
                handleMouseClick(e);
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
                    
                    if(selection != null)
                    {
                        double cx = selection.shape.getCenterX();
                        double cy = selection.shape.getCenterY();
                        selection.move(cx + dx / pixelsPerMeter, cy - dy / pixelsPerMeter);
                    }
                    else
                    {
                        panX += dx / pixelsPerMeter;
                        panY += -dy / pixelsPerMeter; 
                    }
                    
                    lastDrag.setLocation(e.getPoint());
                    repaint();
                }
                else
                {
                    lastDrag = new Point(e.getPoint());
                }
            }});
    }
    
    /**
     * @param e
     */
    private void handleMouseClick(MouseEvent e)
    {
        Graphics2D g2d = (Graphics2D) this.getGraphics().create();

        try 
        {
            setupWorldTransform(g2d);
            final Rectangle rect = new Rectangle(e.getPoint(), new Dimension(1, 1));
            selection = null;
            for(Robot robot : world.getRobots())
            {
                if(g2d.hit(rect, robot.shape, false))
                {
                    selection = robot;
                }
            }
        }
        finally
        {
            g2d.dispose();
        }
        
        repaint();
    }

    public void setWorld(World world)
    {
        this.world = world;
    }
    
    public void fit()
    {
        pixelsPerMeter = (getWidth() / world.extents.getWidth()) * 0.8;
        panX = -world.extents.getCenterX();
        panY = -world.extents.getCenterY();
        repaint();
    }
    
    public void setFollow(Robot robot)
    {
        this.follow = robot;
        repaint();
    }
    
    public Robot getSelection()
    {
        return selection;
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
        
        final Graphics2D g2d = setupWorldTransform((Graphics2D) g.create());

        g2d.setStroke(new BasicStroke(2 * (1.0f / (float)pixelsPerMeter)));
        
        drawShape(g2d, world.extents, Color.LIGHT_GRAY, Color.BLUE);
        
        for(Robot robot : world.getRobots())
        {
            drawRobot(g2d, robot);
        }
       
        for(Waypoint w : world.getWaypoints())
        {
            drawWaypoint(g2d, w);
        }
        
        for(Shape s : world.getObstacles())
        {
            drawObstacle(g2d, s);
        }
        g2d.dispose();
        /*
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
        */
    }

    private Graphics2D setupWorldTransform(final Graphics2D g2d)
    {
        final AffineTransform transform = new AffineTransform();
        transform.translate(getWidth() / 2.0, getHeight() / 2.0);
        transform.scale(pixelsPerMeter, -pixelsPerMeter);
        if(follow != null)
        {
            panX = -follow.shape.getCenterX();
            panY = -follow.shape.getCenterY();
            transform.rotate(-(follow.yaw - Math.toRadians(90)));
        }
        transform.translate(panX, panY);
        
        g2d.transform(transform);
        return g2d;
    }
    
    private void drawRobot(Graphics2D g2dIn, Robot robot)
    {
        final Graphics2D g2d = (Graphics2D) g2dIn.create();
        final AffineTransform transform = new AffineTransform();
        transform.translate(robot.shape.getCenterX(), robot.shape.getCenterY());
        transform.rotate(robot.yaw);
        g2d.transform(transform);
        
        drawRanges(g2d, robot);
        
        if(robot == selection)
        {
            final double selR = robot.radius * 1.4;
            final Ellipse2D sel = new Ellipse2D.Double(-selR, -selR, selR * 2.0, selR * 2.0);
            g2d.setColor(Color.BLUE);
            g2d.fill(sel);
        }
        
        final double r = robot.radius;
        final Ellipse2D body = new Ellipse2D.Double(-r, -r, r * 2.0, r * 2.0);
        drawShape(g2d, body, Color.WHITE, Color.BLACK);
        
        final double dirR = r / 5.0;
        final Ellipse2D dir = new Ellipse2D.Double(r - dirR, -dirR, dirR * 2.0, dirR * 2.0);
        drawShape(g2d, dir, Color.RED, Color.BLACK);
        
        g2d.rotate(follow == null ? -robot.yaw : Math.toRadians(-90.0));
        
        final double fontHeight = robot.radius * 1.5;
        prepareFont(g2d, fontHeight);
        final Rectangle2D bounds = g2d.getFont().getStringBounds(robot.name, g2d.getFontRenderContext());
        g2d.setColor(Color.BLACK);
        g2d.drawString(robot.name, (float)(-bounds.getWidth() / 2.0), (float)(-(fontHeight / 3.0)));
        
        g2d.dispose();
    }
    
    private Font prepareFont(Graphics2D g2d, double fontHeight)
    {
        final AffineTransform fontTransform = AffineTransform.getScaleInstance(1.0, -1.0);
        final Font font = g2d.getFont().deriveFont(Font.BOLD, (float) fontHeight).deriveFont(fontTransform);
        g2d.setFont(font);
        return font;
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
        final Graphics2D g2d = (Graphics2D) g2dIn.create();
        final Point2D p = waypoint.point;
        final double r = 0.2;
        final Ellipse2D circle1 = new Ellipse2D.Double(p.getX() - r, p.getY() - r, 2 * r, 2 * r);
        
        drawShape(g2d, circle1, Color.ORANGE, Color.BLACK);
        
        final double r2 = r * 1.4;
        final Ellipse2D circle2 = new Ellipse2D.Double(p.getX() - r2, p.getY() - r2, 2 * r2, 2 * r2);
        g2d.draw(circle2);
        
        final double fontHeight = r * 1.5;
        prepareFont(g2d, fontHeight);
        final Rectangle2D bounds = g2d.getFont().getStringBounds(waypoint.name, g2d.getFontRenderContext());
        g2d.setColor(Color.BLACK);
        g2d.drawString(waypoint.name, (float) (p.getX() - bounds.getWidth() / 2.0), (float) (p.getY() - fontHeight / 3.0));
        
        g2d.dispose();
        //g2d.draw(new Line2D.Double(p.getX(), p.getY() - 1.5 * r, p.getX(), p.getY() + 1.5 * r));
        //g2d.draw(new Line2D.Double(p.getX() - 1.5 * r, p.getY(), p.getX() + 1.5 * r, p.getY()));
    }    
}
