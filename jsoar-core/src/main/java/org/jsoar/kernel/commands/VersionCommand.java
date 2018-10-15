package org.jsoar.kernel.commands;

import org.jsoar.kernel.Agent;
import org.jsoar.kernel.JSoarVersion;
import org.jsoar.kernel.SoarException;
import org.jsoar.util.commands.SoarCommand;
import org.jsoar.util.commands.SoarCommandContext;

import picocli.CommandLine.Command;
import picocli.CommandLine.HelpCommand;

/**
 * This is the implementation of the "version" command.
 * @author austin.brehob
 */
public class VersionCommand implements SoarCommand
{
    private Agent agent;
    
    public VersionCommand(Agent agent)
    {
        this.agent = agent;
    }
    
    @Override
    public String execute(SoarCommandContext context, String[] args) throws SoarException
    {
        Utils.parseAndRun(agent, new Version(agent), args);
        
        return "";
    }

    @Command(name="version", description="Prints the version of Soar to the screen",
            subcommands={HelpCommand.class})
    static public class Version implements Runnable
    {
        private Agent agent;
        
        public Version(Agent agent)
        {
            this.agent = agent;
        }
        
        @Override
        public void run()
        {
            final JSoarVersion v = JSoarVersion.getInstance();
            agent.getPrinter().startNewLine().print(String.format("%s%nBuilt on: %s%nBuilt by: %s",
                    v.getVersion(), v.getBuildDate(), v.getBuiltBy()));
        }
    }
}
