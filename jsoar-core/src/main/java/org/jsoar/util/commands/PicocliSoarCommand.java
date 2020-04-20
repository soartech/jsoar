package org.jsoar.util.commands;

import org.jsoar.kernel.Agent;
import org.jsoar.kernel.SoarException;
import org.jsoar.kernel.commands.Utils;

import picocli.CommandLine;

public abstract class PicocliSoarCommand implements SoarCommand
{
    protected final static String NULL = null;
    
    final protected Agent agent;
    final protected Object picocliCommand;
    final protected CommandLine commandLine;
    
    /**
     * Creates a PicocliSoarCommand with an agent.
     * A command with an agent may print results to the agent's printer
     * and/or return a result directly.
     */
    public PicocliSoarCommand(Agent agent, Object picocliCommand) {
        this.agent = agent;
        this.picocliCommand = picocliCommand;
        this.commandLine = new CommandLine(picocliCommand);
    }
    
    /**
     * Creates a PicocliSoarCommand with no agent.
     * A command with no agent is expected to return any result directly.
     */
    public PicocliSoarCommand(Object picocliCommand) {
        this(null, picocliCommand);
    }
    
    @Override
    public String execute(SoarCommandContext context, String[] args) throws SoarException {
        if(agent != null) {
            return Utils.parseAndRun(this.agent, this.commandLine, args);
        } else {
            return Utils.parseAndRun(this.commandLine, args);
        }
    }

    @Override
    public Object getCommand() {
        return this.picocliCommand;
    }

}
