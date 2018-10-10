package org.jsoar.kernel.commands;

import java.io.OutputStream;
import java.io.PrintStream;
import java.nio.charset.Charset;
import java.util.Arrays;

import org.apache.commons.io.output.WriterOutputStream;
import org.jsoar.kernel.Agent;
import org.jsoar.kernel.SoarException;
import org.jsoar.util.commands.SoarCommand;
import org.jsoar.util.commands.SoarCommandContext;

import picocli.CommandLine;
import picocli.CommandLine.Command;
import picocli.CommandLine.RunLast;

import picocli.CommandLine.HelpCommand;
import picocli.CommandLine.ParentCommand;

/**
 * This command is the implementation of the "soar" command.
 * It should be called "SoarCommand" to follow the naming convention, but that is already the name of an interface.
 * @author bob.marinier
 *
 */
@Command(name="soar", description="Commands and settings related to running Soar",
         subcommands={HelpCommand.class,
                      SoarSettingsCommand.Init.class})
public class SoarSettingsCommand implements SoarCommand, Runnable
{
    private Agent agent;
    
    public SoarSettingsCommand(Agent agent)
    {
        this.agent = agent;
    }
    
    @Override
    public String execute(SoarCommandContext context, String[] args) throws SoarException
    {
        
        OutputStream os = new WriterOutputStream(agent.getPrinter().getWriter(), Charset.defaultCharset(), 1024, true);
        PrintStream ps = new PrintStream(os);
        
        CommandLine commandLine = new CommandLine(this);
        commandLine.parseWithHandlers(
                new RunLast().useOut(ps),
                CommandLine.defaultExceptionHandler().useErr(ps),
                Arrays.copyOfRange(args, 1, args.length));
        
        return "";
    }

    @Override
    public void run()
    {
        // TODO print soar summary information
        
    }
    
    @Command(name="init", description="Re-initializes Soar", subcommands={HelpCommand.class} )
    static public class Init implements Runnable
    {
        @ParentCommand
        SoarSettingsCommand parent; // injected by picocli

        @Override
        public void run()
        {
            parent.agent.initialize();
            parent.agent.getPrinter().startNewLine().print("Agent reinitialized\n").flush();
            
        }
    }

}
