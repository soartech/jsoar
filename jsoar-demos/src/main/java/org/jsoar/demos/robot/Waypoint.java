/*
 * Copyright (c) 2009 Dave Ray <daveray@gmail.com>
 *
 * Created on May 31, 2009
 */
package org.jsoar.demos.robot;

import java.awt.geom.Point2D;

/**
 * @author ray
 */
public class Waypoint
{
    public final String name;
    public final Point2D point;
    
    /**
     * @param name
     * @param point
     */
    public Waypoint(String name, Point2D point)
    {
        this.name = name;
        this.point = point;
    }
    
}
