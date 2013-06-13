package org.jsoar.kernel.epmem;

import java.io.PrintWriter;
import java.io.StringWriter;

import org.jsoar.kernel.SoarException;
import org.jsoar.kernel.epmem.DefaultEpisodicMemoryParams.GmOrderingChoices;
import org.jsoar.kernel.epmem.DefaultEpisodicMemoryParams.GraphMatchChoices;
import org.jsoar.kernel.epmem.DefaultEpisodicMemoryParams.Learning;
import org.jsoar.kernel.epmem.DefaultEpisodicMemoryParams.Optimization;
import org.jsoar.kernel.epmem.DefaultEpisodicMemoryParams.Trigger;
import org.jsoar.kernel.epmem.DefaultEpisodicMemoryParams.Phase;
import org.jsoar.util.adaptables.Adaptable;
import org.jsoar.util.adaptables.Adaptables;
import org.jsoar.util.commands.SoarCommand;
import org.jsoar.util.commands.SoarCommandContext;
import org.jsoar.util.commands.SoarCommandInterpreter;
import org.jsoar.util.commands.SoarCommandProvider;
import org.jsoar.util.properties.PropertyKey;
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
        /*
         * (non-Javadoc)
         * 
         * @see
         * org.jsoar.util.commands.SoarCommandProvider#registerCommands(org.
         * jsoar.util.commands.SoarCommandInterpreter)
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
        else if("--stats".equals(arg))
        {
            return doStats(1, args);
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
            return "Set trigger to " + Trigger.valueOf(value).toString();
        }
        else if (name.equals("phase"))
        {
            props.set(DefaultEpisodicMemoryParams.PHASE, Phase.valueOf(value));
            return "Set phase to " + Phase.valueOf(value).toString();
        }
        else if (name.equals("graph-match"))
        {
            props.set(DefaultEpisodicMemoryParams.GRAPH_MATCH, GraphMatchChoices.valueOf(value));
            return "Set graph-match to " + GraphMatchChoices.valueOf(value);
        }
        else if (name.equals("graph-match-ordering"))
        {
            props.set(DefaultEpisodicMemoryParams.GM_ORDERING, GmOrderingChoices.valueOf(value));
            return "Set graph-match-ordering to " + GmOrderingChoices.valueOf(value).toString();
        }
        else if (name.equals("balance"))
        {
            props.set(DefaultEpisodicMemoryParams.BALANCE, Double.parseDouble(value));
            return "Set graph-match-ordering to " + Double.parseDouble(value);
        }
        else if (name.equals("optimization"))
        {
            props.set(DefaultEpisodicMemoryParams.OPTIMIZATION, Optimization.valueOf(value));
            return "Set optimization to " + Optimization.valueOf(value).toString();
        }
        else if (name.equals("path"))
        {
            props.set(DefaultEpisodicMemoryParams.PATH, value);
            return "Set path to " + value;
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

    private String doStats(int i, String[] args) throws SoarException
    {
        final StringWriter sw = new StringWriter();
        final PrintWriter pw = new PrintWriter(sw);

        final DefaultEpisodicMemoryStats stats = epmem.getStats();
        if (args.length == i + 1)
        {
            pw.printf("Time: %d%n", stats.time.get());
            pw.printf("SQLite Version: %s%n", stats.db_version.get());
            pw.printf("Memory Usage: %d%n", stats.mem_usage.get());
            pw.printf("Memory Highwater: %d%n", stats.mem_high.get());
            pw.printf("Retrievals: %d%n", stats.ncbr.get());
            pw.printf("Queries: %d%n", stats.cbr.get());
            pw.printf("Nexts: %d%n", stats.nexts.get());
            pw.printf("Prevs: %d%n", stats.prevs.get());
            pw.printf("Last Retrieval WMEs: %d%n", stats.ncb_wmes.get());
            pw.printf("Last Query Positive: %d%n", stats.qry_pos.get());
            pw.printf("Last Query Negative: %d%n", stats.qry_neg.get());
            pw.printf("Last Query Retrieved: %d%n", stats.qry_ret.get());
            pw.printf("Last Query Cardinality: %d%n", stats.qry_card.get());
            pw.printf("Last Query Literals: %d%n", stats.qry_lits.get());
            pw.printf("Graph Match Attempts: %d%n", stats.graph_matches.get());
            pw.printf("Last Graph Match Attempts: %d%n", stats.last_graph_matches.get());
            pw.printf("Episodes Considered: %d%n", stats.considered.get());
            pw.printf("Last Episodes Considered: %d%n", stats.last_considered.get());

        }
        else
        {
            final String name = args[i + 1];
            final PropertyKey<?> key = DefaultEpisodicMemoryStats.getProperty(epmem.getParams().getProperties(), name);
            if (key == null)
            {
                throw new SoarException("Unknown stat '" + name + "'");
            }
            pw.printf("%s%n", epmem.getParams().getProperties().get(key).toString());
        }

        pw.flush();
        return sw.toString();
    }
}
