/*
 * Copyright (c) 2009 Dave Ray <daveray@gmail.com>
 *
 * Created on Jun 2, 2009
 */
package org.jsoar.demos.robot;

import org.jsoar.debugger.JSoarDebugger;
import org.jsoar.kernel.Agent;
import org.jsoar.kernel.SoarException;
import org.jsoar.kernel.io.quick.DefaultQMemory;
import org.jsoar.kernel.io.quick.QMemory;
import org.jsoar.kernel.io.quick.SoarQMemoryAdapter;
import org.jsoar.runtime.ThreadedAgent;

/**
 * @author ray
 */
public class RobotAgent
{
    private final Robot robot;
    private final ThreadedAgent agent;
    
    private final QMemory memory = DefaultQMemory.create();
    
    /**
     * @param robot
     */
    public RobotAgent(Robot robot)
    {
        this.robot = robot;
        
        this.agent = ThreadedAgent.attach(new Agent()).initialize();
        this.agent.getAgent().setName(robot.name);
        this.agent.getAgent().setDebuggerProvider(JSoarDebugger.newDebuggerProvider());
        SoarQMemoryAdapter.attach(this.agent.getAgent(), memory);
        
        memory.setString("self.name", robot.name);
        memory.setDouble("self.radius", robot.radius);

        try
        {
            this.agent.getAgent().getDebuggerProvider().openDebugger(this.agent.getAgent());
        }
        catch (SoarException e)
        {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
    }
    
   
    public void update()
    {
        synchronized(memory)
        {
            memory.setDouble("self.pose.x", robot.shape.getCenterX());
            memory.setDouble("self.pose.y", robot.shape.getCenterY());
            memory.setDouble("self.pose.yaw", Math.toDegrees(robot.yaw));
            for(int i = 0; i < robot.ranges.length; ++i)
            {
                final RadarRange r = robot.ranges[i];
                final QMemory sub = memory.subMemory("ranges.range[" + i + "]");
                sub.setInteger("id", i - robot.ranges.length / 2);
                sub.setDouble("distance", r.range);
                sub.setDouble("angle", Math.toDegrees(r.angle));
            }
        }
    }
}
