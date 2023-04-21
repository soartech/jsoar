package org.jsoar.util.commands;

import org.jsoar.kernel.Agent;
import org.jsoar.kernel.SoarException;
import org.jsoar.kernel.commands.Utils;

import picocli.CommandLine;
import picocli.CommandLine.IExecutionExceptionHandler;
import picocli.CommandLine.ParseResult;

public abstract class PicocliSoarCommand implements SoarCommand
{
    protected static final String NULL = null;
    
    protected final Agent agent;
    protected final Object picocliCommand;
    protected final CommandLine commandLine;
    
    protected boolean autoFlush = true;
    
    /**
     * Creates a PicocliSoarCommand with an agent.
     * A command with an agent may print results to the agent's printer
     * and/or return a result directly.
     */
    protected PicocliSoarCommand(Agent agent, Object picocliCommand)
    {
        this.agent = agent;
        this.picocliCommand = picocliCommand;
        this.commandLine = new CommandLine(picocliCommand)
                .setExecutionExceptionHandler(new ExceptionHandler());
    }
    
    /**
     * Creates a PicocliSoarCommand with no agent.
     * A command with no agent is expected to return any result directly.
     */
    protected PicocliSoarCommand(Object picocliCommand)
    {
        this(null, picocliCommand);
    }
    
    @Override
    public String execute(SoarCommandContext context, String[] args) throws SoarException
    {
        if(agent != null)
        {
            String result = Utils.parseAndRun(this.agent, this.commandLine, args);
            if(autoFlush)
            {
                agent.getPrinter().flush();
            }
            return result;
        }
        else
        {
            return Utils.parseAndRun(this.commandLine, args);
        }
    }
    
    @Override
    public Object getCommand()
    {
        return this.picocliCommand;
    }
    
    /**
     * Turn auto flushing after command execution on/off
     * 
     * @param autoFlush
     */
    public void autoFlush(boolean autoFlush)
    {
        this.autoFlush = autoFlush;
    }
    
    /**
     * This throws all exceptions, so we can catch them above and re-throw as SoarExceptions
     * 
     * @author bob.marinier
     *
     */
    protected static class ExceptionHandler implements IExecutionExceptionHandler
    {
        
        /**
         * For execution exceptions, just rethrow without printing help
         * 
         * @throws SoarException
         * @throws InterruptedException
         */
        @Override
        public int handleExecutionException(Exception ex,
                CommandLine commandLine,
                ParseResult parseResult) throws SoarException, InterruptedException
        {
            if(ex instanceof InterruptedException)
            {
                throw new InterruptedException(ex.getMessage());
            }
            throw new SoarException(ex);
        }
    }
    
}
