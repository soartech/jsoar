package org.jsoar.kernel.commands;

import java.util.concurrent.Callable;

import org.jsoar.kernel.SoarException;
import org.jsoar.util.commands.SoarCommand;
import org.jsoar.util.commands.SoarCommandContext;

import picocli.CommandLine.Command;
import picocli.CommandLine.HelpCommand;

/**
 * This is the implementation of the "pwd" command.
 * @author austin.brehob
 */
public class PwdCommand implements SoarCommand
{
    private final SourceCommand sourceCommand;
    
    public PwdCommand(SourceCommand sourceCommand)
    {
        this.sourceCommand = sourceCommand;
    }
    
    @Override
    public String execute(SoarCommandContext context, String[] args) throws SoarException
    {
        String result = Utils.parseAndRun(new Pwd(sourceCommand), args);
        return result;
    }
    @Override
    public Object getCommand() {

        return new Pwd(sourceCommand);
    }
    
    @Command(name="pwd", description="Prints the working directory to the screen", subcommands={HelpCommand.class})
    static public class Pwd implements Callable<String>
    {
        private final SourceCommand sourceCommand;
        
        public Pwd(SourceCommand sourceCommand)
        {
            this.sourceCommand = sourceCommand;
        }
        
        @Override
        public String call()
        {
            return sourceCommand.getWorkingDirectory().replace('\\', '/');
        }
    }
}
