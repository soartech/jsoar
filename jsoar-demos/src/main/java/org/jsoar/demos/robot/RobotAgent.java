/*
 * Copyright (c) 2009 Dave Ray <daveray@gmail.com>
 *
 * Created on Jun 2, 2009
 */
package org.jsoar.demos.robot;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.jsoar.debugger.JSoarDebugger;
import org.jsoar.debugger.JSoarDebuggerConfiguration;
import org.jsoar.kernel.SoarException;
import org.jsoar.kernel.io.CycleCountInput;
import org.jsoar.kernel.io.quick.DefaultQMemory;
import org.jsoar.kernel.io.quick.QMemory;
import org.jsoar.kernel.io.quick.SoarQMemoryAdapter;
import org.jsoar.runtime.ThreadedAgent;

/**
 * @author ray
 */
public class RobotAgent
{
    private static final Log logger = LogFactory.getLog(RobotAgent.class);
    
    private Robot robot;
    private final ThreadedAgent agent;
    
    private final QMemory memory = DefaultQMemory.create();
    
    /**
     * @param robot
     */
    public RobotAgent()
    {
        logger.info("Creating robot agent " + this);
        this.agent = ThreadedAgent.create();
        this.agent.setDebuggerProvider(JSoarDebugger.newDebuggerProvider(new JSoarDebuggerConfiguration() {

            @Override
            public void exit()
            {
                // nothing
            }}));
        
        SoarQMemoryAdapter.attach(this.agent.getAgent(), memory);
        new CycleCountInput(this.agent.getInputOutput(), 
                            this.agent.getEvents());
        
        //debug();
    }

    private String getWaypointKey(Waypoint w)
    {
        return "self.waypoints.waypoint[" + w.name + "]";
    }
    
    public void setRobot(Robot robot)
    {
        logger.info("Attaching robot agent " + this + " to robot " + robot.name);
        this.robot = robot;
        this.agent.setName(robot.name);
        this.agent.initialize(); // Do an init-soar
    }
    
    public void start()
    {
        this.agent.runForever();
    }
    
    public void stop()
    {
        this.agent.stop();
    }
    
    public void debug()
    {
        try
        {
            this.agent.openDebugger();
        }
        catch (SoarException e)
        {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
    }
    /**
     * 
     */
    public void dispose()
    {
        logger.info("Disposing robot agent " + this);
        this.agent.detach();
    }
    
    
    public void update()
    {
        synchronized(memory)
        {
            memory.setString("self.name", robot.name);
            memory.setDouble("self.radius", robot.radius);

            final double x = robot.shape.getCenterX();
            final double y = robot.shape.getCenterY();
            memory.setDouble("self.pose.x", x);
            memory.setDouble("self.pose.y", y);
            memory.setDouble("self.pose.yaw", Math.toDegrees(robot.yaw));
            for(int i = 0; i < robot.ranges.length; ++i)
            {
                final RadarRange r = robot.ranges[i];
                final QMemory sub = memory.subMemory("ranges.range[" + i + "]");
                sub.setInteger("id", i - robot.ranges.length / 2);
                sub.setDouble("distance", r.range);
                sub.setDouble("angle", Math.toDegrees(r.angle));
            }
            
            for(Waypoint wp : robot.world.getWaypoints())
            {
                final double wpx = wp.point.getX();
                final double wpy = wp.point.getY();
                
                final QMemory sub = memory.subMemory(getWaypointKey(wp));
                sub.setDouble("x", wp.point.getX());
                sub.setDouble("y", wp.point.getY());
                sub.setDouble("distance", wp.point.distance(x, y));
                
                double bearing = Math.toDegrees(Math.atan2(y - wpy, x - wpx) - robot.yaw);
                while(bearing <= -180.0) bearing += 180.0;
                while(bearing >= 180.0) bearing -= 180.0;
                
                sub.setDouble("relative-bearing", bearing);
            }
        }
    }

}
