package org.jsoar.kernel.commands;

import java.io.Writer;
import java.util.LinkedList;

import org.jsoar.kernel.Agent;
import org.jsoar.kernel.SoarException;
import org.jsoar.util.commands.SoarCommand;
import org.jsoar.util.commands.SoarCommandContext;

import picocli.CommandLine.Command;
import picocli.CommandLine.HelpCommand;
import picocli.CommandLine.Option;
import picocli.CommandLine.ParentCommand;

/**
 * This is the implementation of the "soar" command.
 * It should be called "SoarCommand" to follow the naming convention, but that is already the name of an interface.
 * @author bob.marinier
 * @author austin.brehob
 */
/*
@Command(name="soar", description="Commands and settings related to running Soar",
         subcommands={HelpCommand.class,
                      SoarSettingsCommand.Init.class,
                      SoarSettingsCommand.MaxElaborations.class,
                      SoarSettingsCommand.StopPhase.class,
                      SoarSettingsCommand.Stop.class,
                      SoarSettingsCommand.Timers.class,
                      SoarSettingsCommand.WaitSNC.class})
public class SoarSettingsCommand implements SoarCommand, Runnable
{
    private Agent agent;
    private ThreadedAgent tAgent;
    
    public SoarSettingsCommand(Agent agent)
    {
        this.agent = agent;
    }
    
    public SoarSettingsCommand(ThreadedAgent tAgent)
    {
        this.tAgent = tAgent;
    }
    
    @Override
    public String execute(SoarCommandContext context, String[] args) throws SoarException
    {
        // The agent is set here instead of in the constructor because the
        // Threaded Agent may not have an agent when this class is constructed
        if (tAgent != null)
        {
            this.agent = tAgent.getAgent();
        }
        Utils.parseAndRun(agent, this, args);
        
        return "";
    }

    // TODO Provide summary
    @Override
    public void run()
    {
        agent.getPrinter().startNewLine().print(
                "=======================================================\n" +
                "-                   Soar 9.6.0 Summary                -\n" +
                "=======================================================\n"
                );
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
    
    @Command(name="max-elaborations", description="Maximum elaboration in a decision cycle",
            subcommands={HelpCommand.class} )
    static public class MaxElaborations implements Runnable
    {
        @ParentCommand
        SoarSettingsCommand parent; // injected by picocli
        
        @Parameters(index="0", arity="0..1", description="The new number of maximum elaborations")
        private Integer numElaborations = null;

        @Override
        public void run()
        {
            if (numElaborations == null)
            {
                parent.agent.getPrinter().print("max-elaborations is " +
                        parent.agent.getProperties().get(SoarProperties.MAX_ELABORATIONS));
            }
            else
            {
                parent.agent.getProperties().set(SoarProperties.MAX_ELABORATIONS, numElaborations);
                parent.agent.getPrinter().print("The maximum number of elaborations in a phase is now " +
                        numElaborations + ".");
            }
        }
    }
    
    @Command(name="stop-phase", description="Phase before which Soar will stop",
            subcommands={HelpCommand.class} )
    static public class StopPhase implements Runnable
    {
        @ParentCommand
        SoarSettingsCommand parent; // injected by picocli
        
        // These are in the same order as the corresponding entries in the Phase class
        enum CommandPhase {input, proposal, decide, apply, output};
        
        @Parameters(index="0", arity="0..1",
                description="Valid phases are: ${COMPLETION-CANDIDATES}")
        private CommandPhase phase = null;

        @Override
        public void run()
        {
            if (phase == null)
            {
                CommandPhase currentPhase = CommandPhase.values()
                        [parent.agent.getProperties().get(SoarProperties.STOP_PHASE).ordinal()];
                parent.agent.getPrinter().print("stop-phase is " + currentPhase);
            }
            else
            {
                parent.agent.getProperties().set(SoarProperties.STOP_PHASE,
                        Phase.values()[phase.ordinal()]);
                parent.agent.getPrinter().print("Soar will now stop before the " +
                        phase + " phase.");
            }
        }
    }
    
    @Command(name="stop", description="Stop Soar execution",
            subcommands={HelpCommand.class} )
    static public class Stop implements Runnable
    {
        @ParentCommand
        SoarSettingsCommand parent; // injected by picocli

        @Override
        public void run()
        {
            if (parent.tAgent != null)
            {
                parent.tAgent.stop();
            }
            else
            {
                parent.agent.stop();
            }
        }
    }
    
    @Command(name="timers", description="Profile where Soar spends its time",
            subcommands={HelpCommand.class})
    static public class Timers implements Runnable
    {
        @ParentCommand
        SoarSettingsCommand parent; // injected by picocli
        
        @Option(names={"on", "-e", "--on", "--enable"},
                description="Enables timers")
        boolean enable = false;
        
        @Option(names={"off", "-d", "--off", "--disable"},
                description="Disables timers")
        boolean disable = false;

        @Override
        public void run()
        {
            if (!enable && !disable)
            {
                parent.agent.getPrinter().print("timers is " +
                        (ExecutionTimers.isEnabled() ? "on" : "off"));
            }
            else if (enable)
            {
                ExecutionTimers.setEnabled(true);
                parent.agent.getPrinter().print("Timers are now enabled.");
            }
            else
            {
                ExecutionTimers.setEnabled(false);
                parent.agent.getPrinter().print("Timers are now disabled.");
            }
        }
    }
    
    @Command(name="wait-snc", description="Wait instead of impasse after state-no-change",
            subcommands={HelpCommand.class})
    static public class WaitSNC implements Runnable
    {
        @ParentCommand
        SoarSettingsCommand parent; // injected by picocli
        
        @Option(names={"on", "-e", "--on", "--enable"},
                description="Enables wait-snc")
        boolean enable = false;
        
        @Option(names={"off", "-d", "--off", "--disable"},
                description="Disables wait-snc")
        boolean disable = false;

        @Override
        public void run()
        {
            if (!enable && !disable)
            {
                parent.agent.getPrinter().print("waitsnc is " +
                        (parent.agent.getProperties().get(SoarProperties.WAITSNC) ? "on" : "off"));
            }
            else if (enable)
            {
                parent.agent.getProperties().set(SoarProperties.WAITSNC, true);
                parent.agent.getPrinter().print("Soar will now wait instead of "
                        + "impassing when a state doesn't change.");
            }
            else
            {
                parent.agent.getProperties().set(SoarProperties.WAITSNC, false);
                parent.agent.getPrinter().print("Soar will now impasse "
                        + "when a state doesn't change.");
            }
        }
    }
}
*/
@Command(name="output", description="Commands related to handling output",
subcommands={HelpCommand.class,
             OutputCommand.Log.class})
public final class OutputCommand implements SoarCommand, Runnable
{
    private Agent agent;
    private LinkedList<Writer> writerStack = new LinkedList<Writer>();
    
    public OutputCommand(Agent agent)
    {
        this.agent = agent;
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
        
        @Override
        public void run()
        {
            if (close)
            {
                if (parent.writerStack.isEmpty())
                {
                    parent.agent.getPrinter().startNewLine().print("Log stack is empty");
                }
                else
                {
                    parent.writerStack.pop();
                    parent.agent.getPrinter().popWriter();
                }
            }
            else
            {
                parent.agent.getPrinter().startNewLine().print(
                        "log is " + (parent.writerStack.isEmpty() ? "on" : "off"));
            }
        }
        
        
    }
    /*
    private final OptionProcessor<Options> options = OptionProcessor.create();
    
    private enum Options
    {
        close, off, disable, query,
        // TODO: Implement -a, --add, -A, --append, -e, --existing
    }
    
    private LinkedList<Writer> writerStack = new LinkedList<Writer>();

    public OutputCommand(Agent agent)
    {
        this.agent = agent;
        
        options
        .newOption(Options.close)
        .newOption(Options.off)
        .newOption(Options.disable)
        .newOption(Options.query)
        .done();
    }

    @Override
    public String execute(SoarCommandContext context, String[] args) throws SoarException
    {
        List<String> nonOpts = options.process(Lists.newArrayList(args));
        
        if (options.has(Options.close) || options.has(Options.disable) || options.has(Options.off))
        {
            if(writerStack.isEmpty())
            {
                throw new SoarException("Log stack is empty");
            }
            final Writer w = writerStack.pop();
            agent.getPrinter().popWriter();
            if(w != null)
            {
                try
                {
                    w.close();
                }
                catch (IOException e)
                {
                    throw new SoarException("While closing writer: " + e.getMessage());
                }
            }
            return "";
        } 
        else if (options.has(Options.query))
        {
            return writerStack.isEmpty() ? "closed" : String.format("open (%d writers)", writerStack.size());
        } 
        else if (nonOpts.size() == 1)
        {
            String arg = nonOpts.get(0);
            
            if(arg.equals("stdout"))
            {
                Writer w = new OutputStreamWriter(System.out);
                writerStack.push(null);
                agent.getPrinter().pushWriter(new TeeWriter(agent.getPrinter().getWriter(), w));
                return "";
            }
            else if(arg.equals("stderr"))
            {
                Writer w = new OutputStreamWriter(System.err);
                writerStack.push(null);
                agent.getPrinter().pushWriter(new TeeWriter(agent.getPrinter().getWriter(), w));
                return "";
            }
            else
            {
                try
                {
                    Writer w = new FileWriter(arg);
                    writerStack.push(w);
                    agent.getPrinter().pushWriter(new TeeWriter(agent.getPrinter().getWriter(), w));
                    return "";
                }
                catch (IOException e)
                {
                    throw new SoarException("Failed to open file '" + arg + "': " + e.getMessage());
                }
            }
        }

        throw new SoarException("Expected 1 argument, got " + (args.length - 1));
    }*/    
}