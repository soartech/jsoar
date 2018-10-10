package org.jsoar.kernel.commands;

import org.jsoar.kernel.Agent;
import org.jsoar.kernel.Phase;
import org.jsoar.kernel.SoarException;
import org.jsoar.kernel.SoarProperties;
import org.jsoar.runtime.ThreadedAgent;
import org.jsoar.util.commands.SoarCommand;
import org.jsoar.util.commands.SoarCommandContext;

import picocli.CommandLine.Command;
import picocli.CommandLine.HelpCommand;
import picocli.CommandLine.Option;
import picocli.CommandLine.Parameters;
import picocli.CommandLine.ParentCommand;

/**
 * This command is the implementation of the "soar" command.
 * It should be called "SoarCommand" to follow the naming convention, but that is already the name of an interface.
 * @author bob.marinier
 * @author austin.brehob
 */
@Command(name="soar", description="Commands and settings related to running Soar",
         subcommands={HelpCommand.class,
                      SoarSettingsCommand.Init.class,
                      SoarSettingsCommand.MaxElaborations.class,
                      SoarSettingsCommand.StopPhase.class,
                      SoarSettingsCommand.Stop.class,
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
