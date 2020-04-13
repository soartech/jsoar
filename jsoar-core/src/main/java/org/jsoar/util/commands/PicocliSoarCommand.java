package org.jsoar.util.commands;

import org.jsoar.kernel.Agent;
import org.jsoar.kernel.SoarException;
import org.jsoar.kernel.commands.Utils;

import picocli.CommandLine;

public abstract class PicocliSoarCommand implements SoarCommand
{
    protected final static String NULL = null;
    
    final protected Agent agent;
    protected Object picocliCommand;
    final protected CommandLine commandLine;
    
    public PicocliSoarCommand(Agent agent, Object picocliCommand) {
        this.agent = agent;
        this.picocliCommand = picocliCommand;
        this.commandLine = new CommandLine(picocliCommand);
    }
    
    @Override
    public String execute(SoarCommandContext context, String[] args) throws SoarException {
        return Utils.parseAndRun(this.agent, this.commandLine, args);
    }

    @Override
    public Object getCommand() {
        return this.picocliCommand;
    }

}
