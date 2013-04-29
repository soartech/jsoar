package org.jsoar.kernel.epmem;

import java.io.PrintWriter;
import java.io.StringWriter;

import org.jsoar.kernel.SoarException;
import org.jsoar.kernel.epmem.DefaultEpisodicMemoryParams.GmOrderingChoices;
import org.jsoar.kernel.epmem.DefaultEpisodicMemoryParams.Learning;
import org.jsoar.kernel.epmem.DefaultEpisodicMemoryParams.Trigger;
import org.jsoar.kernel.epmem.DefaultEpisodicMemoryParams.Phase;
import org.jsoar.util.adaptables.Adaptable;
import org.jsoar.util.adaptables.Adaptables;
import org.jsoar.util.commands.SoarCommand;
import org.jsoar.util.commands.SoarCommandContext;
import org.jsoar.util.commands.SoarCommandInterpreter;
import org.jsoar.util.commands.SoarCommandProvider;
import org.jsoar.util.properties.PropertyManager;

/**
 * Update settings in {@link DefaultEpisodicMemoryParams} when epmem options are
 * set through the JSoar command line.
 * 
 * @author chris.kawatsu
 * 
 */
public class DefaultEpisodicMemoryCommand implements SoarCommand
{
    private final Adaptable context;
    private final DefaultEpisodicMemory epmem;
    
    public static class Provider implements SoarCommandProvider
    {
        /* (non-Javadoc)
         * @see org.jsoar.util.commands.SoarCommandProvider#registerCommands(org.jsoar.util.commands.SoarCommandInterpreter)
         */
        @Override
        public void registerCommands(SoarCommandInterpreter interp, Adaptable context)
        {
            interp.addCommand("epmem", new DefaultEpisodicMemoryCommand(context));
        }
    }

    public DefaultEpisodicMemoryCommand(Adaptable context)
    {
        this.context = context;
        this.epmem = Adaptables.require(getClass(), context, DefaultEpisodicMemory.class);
    }

    /*
     * (non-Javadoc)
     * 
     * @see org.jsoar.util.commands.SoarCommand#execute(org.jsoar.util.commands.
     * SoarCommandContext, java.lang.String[])
     */
    @Override
    public String execute(SoarCommandContext context, String[] args) throws SoarException
    {
        if (args.length == 1)
        {
            return doEpmem();
        }

        final String arg = args[1];
        if ("-s".equals(arg) || "--set".equals(arg))
        {
            return doSet(1, args);
        }
        else if (arg.startsWith("-"))
        {
            throw new SoarException("Unknown option " + arg);
        }
        else
        {
            throw new SoarException("Unknown argument " + arg);
        }
    }

    private String doSet(int i, String[] args) throws SoarException
    {
        if (i + 2 >= args.length)
        {
            throw new SoarException("Invalid arguments for " + args[i] + " option");
        }
        final String name = args[i + 1];
        final String value = args[i + 2];
        final PropertyManager props = epmem.getParams().getProperties();
        if (name.equals("learning"))
        {
            props.set(DefaultEpisodicMemoryParams.LEARNING, Learning.valueOf(value));
        }
        else if (name.equals("trigger"))
        {
            props.set(DefaultEpisodicMemoryParams.TRIGGER, Trigger.valueOf(value));
            return "Set trigger to "+Trigger.valueOf(value).toString();
        }
        else if (name.equals("phase"))
        {
            props.set(DefaultEpisodicMemoryParams.PHASE, Phase.valueOf(value));
            return "Set phase to "+Phase.valueOf(value).toString();
        }
        else if (name.equals("graph-match-ordering"))
        {
            props.set(DefaultEpisodicMemoryParams.GM_ORDERING, GmOrderingChoices.valueOf(value));
            return "Set graph-match-ordering to "+GmOrderingChoices.valueOf(value).toString();
        }
        else if (name.equals("balance"))
        {
            props.set(DefaultEpisodicMemoryParams.BALANCE, Double.parseDouble(value));
            return "Set graph-match-ordering to "+Double.parseDouble(value);
        }
        else
        {
            throw new SoarException("Unknown epmem parameter '" + name + "'");
        }

        return "";
    }

    private String doEpmem()
    {
        final StringWriter sw = new StringWriter();
        final PrintWriter pw = new PrintWriter(sw);

        final DefaultEpisodicMemoryParams p = epmem.getParams();
        pw.printf("Epmem learning: %s%n", p.learning);
        pw.println();
        pw.println("Storage");
        pw.println("-------");
        pw.printf("driver: %s%n", p.driver);
        pw.printf("protocol: %s%n", p.protocol);
        pw.printf("path: %s%n", p.path);
        pw.printf("lazy-commit: %s%n", p.lazy_commit.get() ? "on" : "off");
        pw.println();
        pw.println("Performance");
        pw.println("-----------");
        pw.printf("cache: %s%n", p.cache);
        pw.printf("optimization: %s%n", p.optimization);
        // TODO other epmem params

        pw.flush();
        return sw.toString();
    }
}
