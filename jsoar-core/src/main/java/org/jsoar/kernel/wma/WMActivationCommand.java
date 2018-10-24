package org.jsoar.kernel.wma;

import java.io.PrintWriter;
import java.io.StringWriter;

import org.jsoar.kernel.Agent;
import org.jsoar.kernel.SoarException;
import org.jsoar.kernel.commands.Utils;
import org.jsoar.kernel.memory.Wme;
import org.jsoar.kernel.rete.Rete;
import org.jsoar.kernel.wma.DefaultWorkingMemoryActivationParams.ActivationChoices;
import org.jsoar.kernel.wma.DefaultWorkingMemoryActivationParams.FakeForgettingChoices;
import org.jsoar.kernel.wma.DefaultWorkingMemoryActivationParams.ForgetWmeChoices;
import org.jsoar.kernel.wma.DefaultWorkingMemoryActivationParams.ForgettingChoices;
import org.jsoar.kernel.wma.DefaultWorkingMemoryActivationParams.PetrovApproxChoices;
import org.jsoar.kernel.wma.DefaultWorkingMemoryActivationParams.TimerLevels;
import org.jsoar.util.adaptables.Adaptables;
import org.jsoar.util.commands.SoarCommand;
import org.jsoar.util.commands.SoarCommandContext;
import org.jsoar.util.properties.PropertyKey;
import org.jsoar.util.properties.PropertyManager;
import org.jsoar.util.timing.ExecutionTimer;

import picocli.CommandLine.Command;
import picocli.CommandLine.HelpCommand;
import picocli.CommandLine.Option;
import picocli.CommandLine.Parameters;
import picocli.CommandLine.ParentCommand;

/**
 * This is the implementation of the "wm activation" command.
 * @author austin.brehob
 */
public class WMActivationCommand implements SoarCommand
{
    private final DefaultWorkingMemoryActivation wma;
    private final Rete rete;
    private Agent agent;
    
    public WMActivationCommand(Agent agent)
    {
        this.agent = agent;
        this.wma = Adaptables.require(getClass(), agent, DefaultWorkingMemoryActivation.class);
        this.rete = Adaptables.adapt(agent, Rete.class);
    }
    
    @Override
    public String execute(SoarCommandContext context, String[] args) throws SoarException
    {
        Utils.parseAndRun(agent, new WM(wma, rete, agent), args);
        
        return "";
    }

    
    @Command(name="wm", description="Commands and settings related to working memory",
            subcommands={HelpCommand.class,
                         WMActivationCommand.Activation.class})
    static public class WM implements Runnable
    {
        private final DefaultWorkingMemoryActivation wma;
        private final Rete rete;
        private Agent agent;
        
        public WM(DefaultWorkingMemoryActivation wma, Rete rete, Agent agent)
        {
            this.wma = wma;
            this.rete = rete;
            this.agent = agent;
        }
        
        @Override
        public void run()
        {
            agent.getPrinter().startNewLine().print("The 'wm' commands interact "
                    + "with working memory contents and settings.");
        }
    }
    
    
    @Command(name="activation", description="Changes the behavior of and displays "
            + "information about working memory activation", subcommands={HelpCommand.class})
    static public class Activation implements Runnable
    {
        @ParentCommand
        WM parent; // injected by picocli
        
        @Option(names={"-g", "--get"}, description="Print current parameter setting")
        String getParam = null;
        
        @Option(names={"-s", "--set"}, description="Set parameter value")
        String setParam = null;
        
        @Option(names={"-S", "--stats"}, description="Print statistic summary or specific statistic")
        boolean printStats = false;
        
        @Option(names={"-t", "--timers"}, description="Print timer summary or specific timer")
        boolean printTimer = false;
        
        @Option(names={"-h", "--history"}, description="Print reference history of a WME")
        boolean printHistory = false;
        
        @Parameters(arity="0..1", description="Set value, specific statistic, specific timer, or WME timetag")
        String param = null;
        
        @Override
        public void run()
        {
            if (getParam != null)
            {
                doGet(getParam);
            }
            else if (setParam != null)
            {
                doSet(setParam, param);
            }
            else if (printStats)
            {
                doStats(param);
            }
            else if (printTimer)
            {
                doTimers(param);
            }
            else if (printHistory)
            {
                doHistory(param);
            }
            else
            {
                doWma();
            }
        }

        private void doGet(String param)
        {
            // Print the value of the given parameter if it is valid
            final PropertyKey<?> key = DefaultWorkingMemoryActivationParams.getProperty(
                    parent.wma.getParams().getProperties(), param);
            if (key == null)
            {
                parent.agent.getPrinter().startNewLine().print("Unknown parameter '" + param + "'");
                return;
            }
            parent.agent.getPrinter().startNewLine().print(
                    parent.wma.getParams().getProperties().get(key).toString());
        }

        private void doSet(String param, String value)
        {
            final PropertyManager props = parent.wma.getParams().getProperties();

            // Set the value of the given parameter if possible
            try
            {
                if (value == null)
                {
                    parent.agent.getPrinter().startNewLine().print("Set value not specified");
                }
                else if (param.equals("activation"))
                {
                    props.set(DefaultWorkingMemoryActivationParams.ACTIVATION,
                            ActivationChoices.valueOf(value));
                }
                else if (param.equals("timers"))
                {
                    props.set(DefaultWorkingMemoryActivationParams.TIMERS, TimerLevels.valueOf(value));
                }
                else if (props.get(DefaultWorkingMemoryActivationParams.ACTIVATION) == ActivationChoices.on)
                {
                    // TODO: This check should be done in the property system
                    // For now, protected parameters come after this point in the if-else chain,
                    // and unprotected parameters come earlier
                    // A protected parameter is one that can't be changed while wma is on, e.g. because
                    // it affects how wma is initialized, etc.
                    parent.agent.getPrinter().startNewLine().print(
                            "This parameter is protected while WMA is on.");
                }
                else if (param.equals("decay-rate"))
                {
                    props.set(DefaultWorkingMemoryActivationParams.DECAY_RATE, -Double.valueOf(value));
                }
                else if (param.equals("decay-thresh"))
                {
                    props.set(DefaultWorkingMemoryActivationParams.DECAY_THRESH, -Double.valueOf(value));
                }
                else if (param.equals("forgetting"))
                {
                    props.set(DefaultWorkingMemoryActivationParams.FORGETTING_CHOICES,
                            "on".equals(value) ? ForgettingChoices.approx : ForgettingChoices.valueOf(value));
                }
                else if (param.equals("forget-wme"))
                {
                    props.set(DefaultWorkingMemoryActivationParams.FORGET_WME_CHOICES,
                            ForgetWmeChoices.valueOf(value));
                }
                else if (param.equals("fake-forgetting"))
                {
                    props.set(DefaultWorkingMemoryActivationParams.FAKE_FORGETTING,
                            FakeForgettingChoices.valueOf(value));
                }
                else if (param.equals("max-pow-cache"))
                {
                    props.set(DefaultWorkingMemoryActivationParams.MAX_POW_CACHE, Integer.valueOf(value));
                }
                else if (param.equals("petrov-approx"))
                {
                    props.set(DefaultWorkingMemoryActivationParams.PETROV_APPROX,
                            PetrovApproxChoices.valueOf(value));
                }
                else
                {
                    parent.agent.getPrinter().startNewLine().print("Unknown parameter '" + param + "'");
                }
            }
            catch (IllegalArgumentException e)
            {
                parent.agent.getPrinter().startNewLine().print("Invalid value.");
            }
        }

        // Print all wm activation statistics, or just the stats of the parameter provided
        private void doStats(String param)
        {
            final StringWriter sw = new StringWriter();
            final PrintWriter pw = new PrintWriter(sw);

            final DefaultWorkingMemoryActivationStats p = parent.wma.getStats();
            if (param == null)
            {
                pw.printf("Forgotten WMEs: %d%n", p.forgotten_wmes.get());
            }
            else
            {
                final PropertyKey<?> key = DefaultWorkingMemoryActivationStats.getProperty(
                        parent.wma.getParams().getProperties(), param);
                if (key == null)
                {
                    parent.agent.getPrinter().startNewLine().print("Unknown stat '" + param + "'");
                    return;
                }
                pw.printf("%s%n", parent.wma.getParams().getProperties().get(key).toString());
            }

            pw.flush();
            parent.agent.getPrinter().startNewLine().print(sw.toString());
        }

        // Print the values of all timers, or just the value of the timer provided
        private void doTimers(String param)
        {
            final StringWriter sw = new StringWriter();
            final PrintWriter pw = new PrintWriter(sw);

            final DefaultWorkingMemoryActivationTimers t = parent.wma.getTimers();
            if (param == null)
            {
                pw.printf("timers:\n");
                pw.printf("%s: %f\n", t.forgetting.getName(), t.forgetting.getTotalSeconds());
                pw.printf("%s: %f\n", t.history.getName(), t.history.getTotalSeconds());
            }
            else
            {
                ExecutionTimer timer = t.get(param);
                if (timer == null)
                {
                    parent.agent.getPrinter().startNewLine().print("Unknown timer '" + param + "'");
                    return;
                }
                pw.printf("%f", timer.getTotalSeconds());
            }

            pw.flush();
            parent.agent.getPrinter().startNewLine().print(sw.toString());
        }

        // Print the decay history of the wme with the given timetag
        private void doHistory(String param)
        {
            if (param == null)
            {
                parent.agent.getPrinter().startNewLine().print("Timetag argument required.");
                return;
            }

            long timetag;
            try
            {
                timetag = Long.valueOf(param);
            }
            catch (NumberFormatException ignored)
            {
                parent.agent.getPrinter().startNewLine().print("Timetag must be a valid integer.");
                return;
            }
            if (timetag == 0)
            {
                parent.agent.getPrinter().startNewLine().print("Invalid timetag.");
                return;
            }

            Wme wme = null;
            for (Wme tempwme : parent.rete.getAllWmes())
            {
                if (tempwme.getTimetag() == timetag)
                {
                    wme = tempwme;
                    break;
                }
            }

            if (wme != null)
            {
                parent.agent.getPrinter().startNewLine().print(parent.wma.wma_get_wme_history(wme));
            }

            parent.agent.getPrinter().startNewLine().print("WME has no decay history");
        }

        // Print wm activation settings
        private void doWma()
        {
            final StringWriter sw = new StringWriter();
            final PrintWriter pw = new PrintWriter(sw);

            final DefaultWorkingMemoryActivationParams p = parent.wma.getParams();
            pw.printf("WMA activation: %s%n", p.activation.get());
            pw.println();
            pw.println("Activation");
            pw.println("----------");
            pw.printf("decay-rate: %f%n", p.decay_rate.get());
            pw.printf("petrov-approx: %s%n", p.petrov_approx.get());
            pw.println();
            pw.println("Forgetting");
            pw.println("----------");
            pw.printf("decay-thresh: %f%n", p.decay_thresh.get());
            pw.printf("forgetting: %s%n", p.forgetting.get());
            pw.printf("forget-wme: %s%n", p.forget_wme.get());
            pw.printf("fake-forgetting: %s%n", p.fake_forgetting.get());
            pw.println();
            pw.println("Performance");
            pw.println("-----------");
            pw.printf("timers: %s%n", p.timers.get());
            pw.printf("max-pow-cache: %d%n", p.max_pow_cache.get());

            pw.flush();
            parent.agent.getPrinter().startNewLine().print(sw.toString());
        }
    }
}
