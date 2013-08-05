package org.jsoar.kernel.epmem;

import java.io.PrintWriter;
import java.io.StringWriter;
import java.sql.SQLException;

import org.jsoar.kernel.SoarException;
import org.jsoar.kernel.epmem.DefaultEpisodicMemoryParams.AppendDatabaseChoices;
import org.jsoar.kernel.epmem.DefaultEpisodicMemoryParams.GmOrderingChoices;
import org.jsoar.kernel.epmem.DefaultEpisodicMemoryParams.GraphMatchChoices;
import org.jsoar.kernel.epmem.DefaultEpisodicMemoryParams.LazyCommitChoices;
import org.jsoar.kernel.epmem.DefaultEpisodicMemoryParams.Learning;
import org.jsoar.kernel.epmem.DefaultEpisodicMemoryParams.Optimization;
import org.jsoar.kernel.epmem.DefaultEpisodicMemoryParams.Trigger;
import org.jsoar.kernel.epmem.DefaultEpisodicMemoryParams.Phase;
import org.jsoar.util.ByRef;
import org.jsoar.util.PrintHelper;
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
        else if("-g".equals(arg) || "--get".equals(arg))
        {
            return doGet(1, args);
        }
        else if("-s".equals(arg) || "--stats".equals(arg))
        {
            return doStats(1, args);
        }
        else if("-p".equals(arg) || "--print".equals(arg))
        {
            return doPrintEpisode(1, args);
        }
        else if("-r".equals(arg) || "--reinit".equals(arg))
        {
            return doReinit();
        }
        else if("-b".equals(arg) || "--backup".equals(arg))
        {
            return doBackup(1, args);
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
        
        try
        {
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
                return "Set optimization to " + Optimization.valueOf(value);
            }
            else if (name.equals("path"))
            {
                props.set(DefaultEpisodicMemoryParams.PATH, value);
                return "Set path to " + value;
            }
            else if (name.equals("append-database"))
            {
                props.set(DefaultEpisodicMemoryParams.APPEND_DB, AppendDatabaseChoices.valueOf(value));
                return "Set append to " + AppendDatabaseChoices.valueOf(value);
            }
            else if (name.equals("lazy-commit"))
            {
                if(epmem.db != null){
                    return "Lazy commit is protected while the database is open.";
                }
                props.set(DefaultEpisodicMemoryParams.LAZY_COMMIT, LazyCommitChoices.valueOf(value));
                return "Set lazy-commit to " + LazyCommitChoices.valueOf(value);
            }
            else
            {
                throw new SoarException("Unknown epmem parameter '" + name + "'");
            }
        }
        catch(IllegalArgumentException e) // this is thrown by the enums if a bad value is passed in
        {
            throw new SoarException("Invalid value.");
        }

        return "";
    }
    
    private String doGet(int i, String[] args) throws SoarException
    {
        if(i + 1 >= args.length)
        {
            throw new SoarException("Invalid arguments for " + args[i] + " option");
        }
        final String name = args[i+1];
        final PropertyKey<?> key = DefaultEpisodicMemoryParams.getProperty(epmem.getParams().getProperties(), name);
        if(key == null)
        {
            throw new SoarException("Unknown parameter '" + name + "'");
        }
        return epmem.getParams().getProperties().get(key).toString();
    }

    private String doEpmem()
    {
        final StringWriter sw = new StringWriter();
        final PrintWriter pw = new PrintWriter(sw);

        final DefaultEpisodicMemoryParams p = epmem.getParams();
        pw.printf(PrintHelper.generateHeader("Episodic Memory Settings", 40));
        pw.printf(PrintHelper.generateItem("learning:", p.learning.get(), 40));
        pw.printf(PrintHelper.generateSection("Encoding", 40));
        pw.printf(PrintHelper.generateItem("phase:", p.phase.get(), 40));
        pw.printf(PrintHelper.generateItem("trigger:", p.trigger.get(), 40));
        pw.printf(PrintHelper.generateItem("force:", p.force.get(), 40));
        pw.printf(PrintHelper.generateItem("exclusions:", p.exclusions, 40));
        pw.printf(PrintHelper.generateSection("Storage", 40));
        pw.printf(PrintHelper.generateItem("driver:", p.driver, 40));
        pw.printf(PrintHelper.generateItem("append-database:", p.append_database.get(), 40));
        pw.printf(PrintHelper.generateItem("path:", p.path.get(), 40));
        pw.printf(PrintHelper.generateItem("lazy-commit:", p.lazy_commit.get(), 40));
        pw.printf(PrintHelper.generateSection("Retrieval", 40));
        pw.printf(PrintHelper.generateItem("balance:", p.balance.get(), 40));
        pw.printf(PrintHelper.generateItem("graph-match:", p.graph_match.get(), 40));
        pw.printf(PrintHelper.generateItem("graph-match-ordering:", p.gm_ordering.get(), 40));
        pw.printf(PrintHelper.generateSection("Performance", 40));
        pw.printf(PrintHelper.generateItem("page-size:", "N/A - Not Ported", 40));
        pw.printf(PrintHelper.generateItem("cache-size:", p.cache.get(), 40));
        pw.printf(PrintHelper.generateItem("optimization:", p.optimization.get(), 40));
        pw.printf(PrintHelper.generateItem("timers:", "off", 40));
        pw.printf(PrintHelper.generateSection("Experimental", 40));
        pw.printf(PrintHelper.generateItem("merge:", p.merge.get(), 40));
        
        pw.flush();
        return sw.toString();
    }

    private String doStats(int i, String[] args) throws SoarException
    {
        final StringWriter sw = new StringWriter();
        final PrintWriter pw = new PrintWriter(sw);
        
        if (epmem.getDatabase() == null)
        {
            epmem.epmem_init_db();
        }

        final DefaultEpisodicMemoryStats stats = epmem.stats;
        if (args.length == i + 1)
        {
            pw.printf(PrintHelper.generateHeader("Episodic Memory Statistics", 40));
            pw.printf(PrintHelper.generateItem("Time:", stats.time.get(), 40));
            try
            {
                String database = epmem.getDatabase().getConnection().getMetaData().getDatabaseProductName();
                String version = epmem.getDatabase().getConnection().getMetaData().getDatabaseProductVersion();
                pw.printf(PrintHelper.generateItem(database + " Version:", version, 40));
            }
            catch (SQLException e)
            {
                throw new SoarException(e);
            }
            pw.printf(PrintHelper.generateItem("Memory Usage:", stats.mem_usage.get(), 40));
            pw.printf(PrintHelper.generateItem("Memory Highwater:", stats.mem_high.get(), 40));
            pw.printf(PrintHelper.generateItem("Retrievals:", stats.ncbr.get(), 40));
            pw.printf(PrintHelper.generateItem("Queries:", stats.cbr.get(), 40));
            pw.printf(PrintHelper.generateItem("Nexts:", stats.nexts.get(), 40));
            pw.printf(PrintHelper.generateItem("Prevs:", stats.prevs.get(), 40));
            pw.printf(PrintHelper.generateItem("Last Retrieval WMEs:", stats.ncb_wmes.get(), 40));
            pw.printf(PrintHelper.generateItem("Last Query Positive:", stats.qry_pos.get(), 40));
            pw.printf(PrintHelper.generateItem("Last Query Negative:", stats.qry_neg.get(), 40));
            pw.printf(PrintHelper.generateItem("Last Query Retrieved:", stats.qry_ret.get(), 40));
            pw.printf(PrintHelper.generateItem("Last Query Cardinality:", stats.qry_card.get(), 40));
            pw.printf(PrintHelper.generateItem("Last Query Literals:", stats.qry_lits.get(), 40));
            pw.printf(PrintHelper.generateItem("Graph Match Attempts:", stats.graph_matches.get(), 40));
            pw.printf(PrintHelper.generateItem("Last Graph Match Attempts:", stats.last_graph_matches.get(), 40));
            pw.printf(PrintHelper.generateItem("Episodes Considered:", stats.considered.get(), 40));
            pw.printf(PrintHelper.generateItem("Last Episodes Considered:", stats.last_considered.get(), 40));

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
    
    private String doPrintEpisode(int i, String[] args)
    {
        //If there is no episode argument
        if(args.length <= i + 1){
            return "";
        }
        long episodeID;
        try{
            episodeID = Integer.parseInt(args[i + 1]);
        }catch(NumberFormatException e){
            return "";
        }
        return epmem.epmem_print_episode(episodeID);
    }
    
    private String doReinit(){
        epmem.epmem_reinit();
        return "EpMem| Episodic memory system re-initialized.";
    }
    
    private String doBackup(int i, String[] args) throws SoarException
    {
        if(args.length >= i + 2)
        {
            ByRef<String> err = new ByRef<String>("");
            boolean success = false;
            
            String dbFile = "";
            
            for (++i;i < args.length;i++)
            {
                dbFile += args[i] + " ";
            }
            
            dbFile = dbFile.trim();
            
            try
            {
                success = epmem.epmem_backup_db(dbFile, err);
            }
            catch (SQLException e)
            {
                throw new SoarException(e.getMessage(), e);
            }
            
            if (!success)
            {
                throw new SoarException(err.value);
            }
            
            return "EpMem| Database backed up to " + dbFile;
        }
        
        throw new SoarException("epmem --backup requires a path for an argument");
    }
}
