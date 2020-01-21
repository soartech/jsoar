/*
 * Copyright (c) 2009 Dave Ray <daveray@gmail.com>
 *
 * Created on May 25, 2009
 */
package org.jsoar.demos.robot;

import java.awt.Shape;
import java.awt.geom.Point2D;
import java.awt.geom.Rectangle2D;
import java.util.ArrayList;
import java.util.List;

import org.jsoar.demos.robot.events.WaypointAdded;
import org.jsoar.demos.robot.events.WaypointRemoved;
import org.jsoar.util.events.SoarEventManager;

/**
 * @author ray
 */
public class World
{
    public final SoarEventManager events = new SoarEventManager();
    public final Rectangle2D extents = new Rectangle2D.Double(-5.0, -5.0, 10.0, 10.0);
    private final List<Robot> robots = new ArrayList<Robot>();
    private final List<Shape> obstacles = new ArrayList<Shape>();
    private final List<Waypoint> waypoints = new ArrayList<Waypoint>();
    
    public World()
    {
        /*
        Robot robot = new Robot(this, "a");
        robot.move(3, 2);
        robot.yaw = Math.toRadians(180.0);
        robot.speed = 0.1;
        robot.turnRate = Math.toRadians(12.0);
        
        addRobot(robot);
        
        Robot robot2 = new Robot(this, "b");
        robot2.move(3, -2);
        robot2.yaw = Math.toRadians(90.0);
        robot2.speed = 0.1;
        robot2.turnRate = Math.toRadians(-15.0);
        
        addRobot(robot2);
        */
        //addObstacle(new RoundRectangle2D.Double(-3.0, -3.0, 3, 3, .25, .25));
        //addObstacle(new Ellipse2D.Double(0, 0, 2, 2));
        
        //addWaypoint(new Waypoint("w", new Point2D.Double(0.5, 0.5)));
    }
    
    public void addRobot(Robot robot)
    {
        this.robots.add(robot);
    }
    
    public void removeRobot(Robot robot)
    {
        this.robots.remove(robot);
    }
    
    public List<Robot> getRobots()
    {
        return robots;
    }
    
    public void addObstacle(Shape shape)
    {
        this.obstacles.add(shape);
    }
    
    public void removeObstacle(Shape shape)
    {
        this.obstacles.remove(shape);
    }
    
    /**
     * @return the obstacles
     */
    public List<Shape> getObstacles()
    {
        return obstacles;
    }

    public void addWaypoint(Waypoint waypoint)
    {
        this.waypoints.add(waypoint);
        events.fireEvent(new WaypointAdded(this, waypoint));
    }
    
    public void removeWaypoint(Waypoint waypoint)
    {
        this.waypoints.remove(waypoint);
        events.fireEvent(new WaypointRemoved(this, waypoint));
    }
    
    public List<Waypoint> getWaypoints()
    {
        return waypoints;
    }
    
    public void update(double dt)
    {
        for(Robot robot : robots)
        {
            robot.update(dt);
        }
    }
    
    public boolean willCollide(Robot r, double newX, double newY)
    {
        final double radius = r.radius;
        if(!extents.contains(newX + radius, newY + radius) ||
                !extents.contains(newX + radius, newY - radius) ||
                !extents.contains(newX - radius, newY - radius) ||
                !extents.contains(newX - radius, newY + radius))
        {
            return true;
        }
        
        for(Shape s : obstacles)
        {
            if(s.contains(newX + radius, newY + radius) ||
                    s.contains(newX + radius, newY - radius) ||
                    s.contains(newX - radius, newY - radius) ||
                    s.contains(newX - radius, newY + radius))
            {
                return true;
            }
        }
        final Point2D newPoint = new Point2D.Double(newX, newY);
        for(Robot other : robots)
        {
            if(r != other)
            {
                if(newPoint.distance(other.shape.getCenterX(), other.shape.getCenterY()) < radius + other.radius)
                {
                    return true;
                }
            }
        }
        return false;
    }
    
    
    public double getCollisionRange(Robot source, double angle)
    {
        final double delta = source.radius / 2.0;
        final double dx = delta * Math.cos(angle);
        final double dy = delta * Math.sin(angle);
        double range = 2 * delta;
        double x = source.shape.getCenterX() + 2 * dx;
        double y = source.shape.getCenterY() + 2 * dy;
        if(collides(source.shape, x, y))
        {
            return 0.0;
        }
        while(!collides(source.shape, x, y))
        {
            x += dx;
            y += dy;
            range += delta;
        }
        return range - delta;
    }
    
    private boolean collides(Shape ignore, double x, double y)
    {
        if(!extents.contains(x, y))
        {
            return true;
        }
        
        for(Shape s : obstacles)
        {
            if(ignore != s && s.contains(x, y))
            {
                return true;
            }
        }
        for(Robot r : robots)
        {
            if(ignore != r.shape && r.shape.contains(x, y))
            {
                return true;
            }
        }
        return false;
    }
}
