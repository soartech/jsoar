package org.jsoar.kernel.commands;

import java.io.FileWriter;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.io.Writer;
import java.util.LinkedList;

import org.jsoar.kernel.Agent;
import org.jsoar.kernel.SoarException;
import org.jsoar.util.commands.SoarCommand;
import org.jsoar.util.commands.SoarCommandContext;
import org.jsoar.util.TeeWriter;

import picocli.CommandLine.Command;
import picocli.CommandLine.HelpCommand;
import picocli.CommandLine.Option;
import picocli.CommandLine.Parameters;
import picocli.CommandLine.ParentCommand;

/**
 * This is the implementation of the "output" command.
 * @author austin.brehob
 */
@Command(name="output", description="Commands related to handling output",
subcommands={HelpCommand.class,
             OutputCommand.Log.class,
             OutputCommand.PrintDepth.class})
public final class OutputCommand implements SoarCommand, Runnable
{
    private Agent agent;
    private PrintCommand printCommand;
    private LinkedList<Writer> writerStack = new LinkedList<Writer>();
    
    public OutputCommand(Agent agent, PrintCommand printCommand)
    {
        this.agent = agent;
        this.printCommand = printCommand;
    }
    
    @Override
    public String execute(SoarCommandContext context, String[] args) throws SoarException
    {
        Utils.parseAndRun(agent, this, args);
        
        return "";
    }
    
    // TODO Provide summary
    @Override
    public void run()
    {
        agent.getPrinter().startNewLine().print(
                "=======================================================\n" +
                "-                    Output Status                    -\n" +
                "=======================================================\n"
        );
    }
    
    @Command(name="log", description="Changes output log settings",
            subcommands={HelpCommand.class} )
    static public class Log implements Runnable
    {
        @ParentCommand
        OutputCommand parent; // injected by picocli

        @Option(names={"-c", "--close"}, arity="0..1", description="Closes the log file")
        boolean close = false;
        
        @Parameters(index="0", arity="0..1", description="File name")
        String fileName = null;
        
        @Override
        public void run()
        {
            if (close)
            {
                if (parent.writerStack.isEmpty())
                {
                    parent.agent.getPrinter().startNewLine().print("Log is not open.");
                }
                else
                {
                    parent.writerStack.pop();
                    parent.agent.getPrinter().popWriter();
                    parent.agent.getPrinter().startNewLine().print("Log file closed.");
                }
            }
            else if (fileName != null)
            {
                if (fileName.equals("stdout"))
                {
                    Writer w = new OutputStreamWriter(System.out);
                    parent.writerStack.push(null);
                    parent.agent.getPrinter().pushWriter(new TeeWriter(
                            parent.agent.getPrinter().getWriter(), w));
                    parent.agent.getPrinter().startNewLine().print("Now writing to System.out");
                }
                else if (fileName.equals("stderr"))
                {
                    Writer w = new OutputStreamWriter(System.err);
                    parent.writerStack.push(null);
                    parent.agent.getPrinter().pushWriter(new TeeWriter(
                            parent.agent.getPrinter().getWriter(), w));
                    parent.agent.getPrinter().startNewLine().print("Now writing to System.err");
                }
                else
                {
                    try
                    {
                        Writer w = new FileWriter(fileName);
                        parent.writerStack.push(w);
                        parent.agent.getPrinter().pushWriter(new TeeWriter(
                                parent.agent.getPrinter().getWriter(), w));
                        parent.agent.getPrinter().startNewLine().print("Log file " + fileName + " open.");
                    }
                    catch (IOException e)
                    {
                        parent.agent.getPrinter().startNewLine().print(
                                "Failed to open file '" + fileName + "': " + e.getMessage());
                    }
                }
            }
            else
            {
                parent.agent.getPrinter().startNewLine().print(
                        "log is " + (!parent.writerStack.isEmpty() ? "on" : "off"));
            }
        }
    }
    
    @Command(name="print-depth", description="Adjusts or displays the print-depth",
            subcommands={HelpCommand.class} )
    static public class PrintDepth implements Runnable
    {
        @ParentCommand
        OutputCommand parent; // injected by picocli
        
        @Parameters(index="0", arity="0..1", description="New print depth")
        Integer printDepth = null;
        
        @Override
        public void run()
        {
            if (printDepth == null)
            {
                parent.agent.getPrinter().startNewLine().print("print-depth is " +
                        Integer.toString(parent.printCommand.getDefaultDepth()));
            }
            else
            {
                try
                {
                    int depth = Integer.valueOf(printDepth);
                    parent.printCommand.setDefaultDepth(depth);
                    parent.agent.getPrinter().startNewLine().print("print-depth is now " + depth);
                }
                catch (SoarException e)
                {
                    parent.agent.getPrinter().print(e.getMessage());
                }
            }
        }
    }
}
