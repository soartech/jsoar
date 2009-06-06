/*
 * Copyright (c) 2009 Dave Ray <daveray@gmail.com>
 *
 * Created on Jun 3, 2009
 */
package org.jsoar.demos.robot.events;

import org.jsoar.demos.robot.Waypoint;
import org.jsoar.demos.robot.World;

/**
 * @author ray
 */
public class AbstractWaypointEvent extends AbstractWorldEvent
{
    public final Waypoint waypoint;

    public AbstractWaypointEvent(World world, Waypoint waypoint)
    {
        super(world);
        this.waypoint = waypoint;
    }

}
