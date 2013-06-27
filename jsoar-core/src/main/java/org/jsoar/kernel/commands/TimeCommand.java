package org.jsoar.kernel.commands;

import java.util.ArrayList;
import java.util.List;

import org.jsoar.kernel.Agent;
import org.jsoar.kernel.SoarException;
import org.jsoar.util.commands.SoarCommand;
import org.jsoar.util.commands.SoarCommandContext;
import org.jsoar.util.timing.DefaultExecutionTimer;
import org.jsoar.util.timing.ExecutionTimer;
import org.jsoar.util.timing.WallclockExecutionTimeSource;

/**
 * Implementation of the "timers" command.
 * 
 * @author ray
 */
public class TimeCommand implements SoarCommand 
{
    private  final Agent agent;
    
    public TimeCommand(Agent agent)
    {
        this.agent = agent;
    }

    @Override
    public String execute(SoarCommandContext context, String[] args) throws SoarException
    {
        List<String> arguments_list = new ArrayList<String>();
        
        for (String e : args)
        {
            arguments_list.add(e);
        }
        
        if (arguments_list.size() < 2)
            throw new SoarException("time command [arguments]");
        
        //JSoar can't easily have a Process Timer which does things exactly how
        // CSoar does things therefore I'm not including it in the output
        // - ALT
        
        WallclockExecutionTimeSource real_source = new WallclockExecutionTimeSource();
        
        ExecutionTimer real = DefaultExecutionTimer.newInstance(real_source);
        
        String combined = "";
        
        arguments_list.remove(0);
        
        for (String s : arguments_list)
        {
            combined += s + " ";
        }
        
        combined = combined.substring(0, combined.length()-1);
        
        real.start();
        
        String result = this.agent.getInterpreter().eval(combined);
        
        real.pause();
        
        double seconds = real.getTotalSeconds();
        
        if (result == null)
            result = new String();
        
        result += "\n";
        
        result += "(-1s) proc - Note JSoar does not support measuring CPU time at the moment.\n";
        result += "(" + seconds + "s) real\n";
        
        return result;
    }

}
