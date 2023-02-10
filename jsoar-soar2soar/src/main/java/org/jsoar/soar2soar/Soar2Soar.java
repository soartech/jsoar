package org.jsoar.soar2soar;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeoutException;

import org.jsoar.kernel.AgentRunController;
import org.jsoar.kernel.SoarException;
import org.jsoar.kernel.commands.RunCommand;
import org.jsoar.runtime.ThreadedAgent;
import org.jsoar.util.commands.DefaultSoarCommandContext;

public class Soar2Soar
{
    private final EnvironmentAgent ea;
    
    public Soar2Soar(String[] args) throws SoarException, InterruptedException, ExecutionException, TimeoutException, IOException
    {
        if(args.length < 1)
        {
            usage();
            System.exit(1);
        }
        
        final int numberOfAgents = Integer.parseInt(args[0]);
        ea = new EnvironmentAgent(args[1]);
        
        final List<ThreadedAgent> clientAgents = new ArrayList<ThreadedAgent>();
        for(int i = 0; i < numberOfAgents; ++i)
        {
            final ClientAgent ca = ea.createClient(args.length > 3 ? args[2 + i] : args[2]);
            clientAgents.add(ca.getAgent());
        }
        
        final Soar2SoarRunController controller = new Soar2SoarRunController(ea.getAgent(), clientAgents);
        
        ThreadedAgent currentAgent = null;
        while(true)
        {
            final String prompt = currentAgent == null ? "root" : currentAgent.getName();
            System.out.print(prompt + "> ");
            System.out.flush();
            
            BufferedReader br = new BufferedReader(new InputStreamReader(System.in));
            
            String line = br.readLine();
            if(line == null)
            {
                break;
            }
            
            line = line.trim();
            if(line.equals("quit") || line.equals("exit"))
            {
                break;
            }
            
            // switch agent
            ThreadedAgent temp = ea.getThreadedAgent(line);
            if(temp != null)
            {
                currentAgent = temp;
            }
            else
            {
                if(line.equals("root"))
                {
                    currentAgent = null;
                }
                
                if(currentAgent != null)
                {
                    currentAgent.getInterpreter().eval(line);
                }
                // root-level commands
                else if(line.equals("ls"))
                {
                    System.out.println(ea.getAgent().getName());
                    for(ThreadedAgent client : clientAgents)
                    {
                        System.out.println(client.getName());
                    }
                }
                else if(line.startsWith("run"))
                {
                    doRunCommand(controller, line);
                }
                else if(line.startsWith("stop"))
                {
                    controller.stop();
                }
                else
                {
                    System.out.println(
                            "Soar2Soar command interface\n" +
                                    "       root               - return to root prompt\n" +
                                    "       <agent-name> - switch to agent prompt\n" +
                                    "       ls           - list names of all agents\n" +
                                    "       run          - Apply a Soar run command to all agents\n" +
                                    "       stop         - Stop all agents\n" +
                                    "agent> debugger    - open debugger for agent\n" +
                                    "agent> Other Soar commands ...");
                }
            }
        }
        
        // cleanup
        System.exit(0);
    }
    
    private void doRunCommand(AgentRunController controller, String line) throws SoarException
    {
        final String args[] = line.split("\\s+", 0);
        final RunCommand runCommand = new RunCommand(controller);
        runCommand.execute(/* TODO SoarCommandContext */ DefaultSoarCommandContext.empty(), args);
    }
    
    private void usage()
    {
        System.out
                .println("usage: "
                        + Soar2Soar.class.toString()
                        + " <# agents> [environment source] [a1 source] [a2 source] ...");
    }
    
    public static void main(String[] args) throws Exception
    {
        new Soar2Soar(args);
    }
}
