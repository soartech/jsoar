/*
 * Copyright (c) 2009 Dave Ray <daveray@gmail.com>
 *
 * Created on May 25, 2009
 */
package org.jsoar.demos.robot;

import java.awt.geom.Ellipse2D;

/**
 * @author ray
 */
public class Robot
{
    public final World world;
    public final String name;
    public final Ellipse2D shape = new Ellipse2D.Double(-0.5, -0.5, 1.0, 1.0);
    public double yaw;
    public double speed;
    public double turnRate;
    public final double radius = 0.5;
    public final RadarRange[] ranges = new RadarRange[5];
    {
        int slot = -(ranges.length / 2);
        for(int i = 0; i < ranges.length; ++i)
        {
            ranges[i] = new RadarRange(slot * (Math.PI / ranges.length));
            ranges[i].range = i;
            slot++;
        }
    }
    
    /**
     * @param game
     */
    public Robot(World game, String name)
    {
        this.world = game;
        this.name = name;
    }
    
    public void move(double newX, double newY)
    {
        shape.setFrameFromCenter(newX, newY, newX + radius, newY + radius);
    }
    
    public void update(double dt)
    {
        yaw += dt * turnRate;
        while(yaw < 0.0)
            yaw += 2.0 * Math.PI;
        while(yaw > 2.0 * Math.PI)
            yaw -= 2.0 * Math.PI;
        
        final double dx = Math.cos(yaw) * speed;
        final double dy = Math.sin(yaw) * speed;
        
        final double newX = shape.getCenterX() + dx;
        final double newY = shape.getCenterY() + dy;
        if(!world.willCollide(this, newX, newY))
        {
            move(newX, newY);
        }
        
        for(RadarRange range : ranges)
        {
            range.range = world.getCollisionRange(this, range.angle + yaw);
        }
    }
    
}
