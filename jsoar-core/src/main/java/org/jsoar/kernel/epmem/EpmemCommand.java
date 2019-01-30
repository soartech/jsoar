package org.jsoar.kernel.epmem;

import java.io.PrintWriter;
import java.io.StringWriter;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.Iterator;
import java.util.Set;

import org.jsoar.kernel.Agent;
import org.jsoar.kernel.SoarException;
import org.jsoar.kernel.commands.Utils;
import org.jsoar.kernel.epmem.DefaultEpisodicMemoryParams.AppendDatabaseChoices;
import org.jsoar.kernel.epmem.DefaultEpisodicMemoryParams.Force;
import org.jsoar.kernel.epmem.DefaultEpisodicMemoryParams.GmOrderingChoices;
import org.jsoar.kernel.epmem.DefaultEpisodicMemoryParams.GraphMatchChoices;
import org.jsoar.kernel.epmem.DefaultEpisodicMemoryParams.LazyCommitChoices;
import org.jsoar.kernel.epmem.DefaultEpisodicMemoryParams.Learning;
import org.jsoar.kernel.epmem.DefaultEpisodicMemoryParams.Optimization;
import org.jsoar.kernel.epmem.DefaultEpisodicMemoryParams.PageChoices;
import org.jsoar.kernel.epmem.DefaultEpisodicMemoryParams.Phase;
import org.jsoar.kernel.epmem.DefaultEpisodicMemoryParams.Trigger;
import org.jsoar.kernel.symbols.SymbolFactoryImpl;
import org.jsoar.kernel.symbols.SymbolImpl;
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

import picocli.CommandLine.Command;
import picocli.CommandLine.HelpCommand;
import picocli.CommandLine.Model.CommandSpec;
import picocli.CommandLine.Option;
import picocli.CommandLine.Parameters;
import picocli.CommandLine.ParameterException;
import picocli.CommandLine.ExecutionException;
import picocli.CommandLine.Spec;

/**
 * This is the implementation of the "epmem" command.
 * @author austin.brehob
 */
public class EpmemCommand implements SoarCommand
{
    private final Agent agent;
    
    public static class Provider implements SoarCommandProvider
    {
        @Override
        public void registerCommands(SoarCommandInterpreter interp, Adaptable context)
        {
            interp.addCommand("epmem", new EpmemCommand(context));
        }
    }

    public EpmemCommand(Adaptable context)
    {
        // TODO: There's probably a better way to get the agent from the context...
        this.agent = (Agent) context;
    }

    @Override
    public Object getCommand()
    {
        return new EpmemC(agent);
    }

    @Override
    public String execute(SoarCommandContext context, String[] args) throws SoarException
    {
        Utils.parseAndRun(agent, new EpmemC(agent), args);
        
        return "";
    }

    
    @Command(name="epmem", description="Controls the behavior of episodic memory",
            subcommands={HelpCommand.class})
    static public class EpmemC implements Runnable
    {
        private final Agent agent;
        private final DefaultEpisodicMemory epmem;
        private final SymbolFactoryImpl symbols;
        
        @Spec
        private CommandSpec spec; // injected by picocli
        
        public EpmemC(Agent agent)
        {
            this.agent = agent;
            this.epmem = Adaptables.require(getClass(), agent, DefaultEpisodicMemory.class);
            this.symbols = Adaptables.require(getClass(), agent, SymbolFactoryImpl.class);
        }
        
        @Option(names={"-s", "--set"}, description="Sets the given parameter value")
        String setParam = null;
        
        @Option(names={"-g", "--get"}, description="Prints the current setting of the given parameter")
        String getParam = null;
        
        @Option(names={"-S", "--stats"}, description="Prints statistic summary or specific statistic")
        boolean printStats = false;
        
        @Option(names={"-p", "--print"}, description="Prints episode in a user-readable format")
        String printEpisode = null;
        
        @Option(names={"-r", "--reinit"}, description="Re-initializes episodic memory")
        boolean reinit = false;
        
        @Option(names={"-b", "--backup"}, arity="1..*", description="Creates a backup of the episodic database on disk")
        String[] backupFileName = null;
        
        @Option(names={"-a", "--add"}, description="Adds knowledge to episodic memory")
        String knowledgeToAdd = null;
        
        @Parameters(arity="0..1", description="The new value of the parameter; "
                + "or specific statistic to print")
        String param = null;
        
        @Override
        public void run()
        {
            if (setParam != null)
            {
                if (param == null)
                {
                    throw new ParameterException(spec.commandLine(), "No parameter value provided");
                }
                agent.getPrinter().print(doSet(setParam, param));
            }
            else if (getParam != null)
            {
                agent.getPrinter().print(doGet(getParam));
            }
            else if (printStats)
            {
                agent.getPrinter().print(doStats(param));
            }
            else if (printEpisode != null)
            {
                agent.getPrinter().print(doPrintEpisode(printEpisode));
            }
            else if (reinit)
            {
                agent.getPrinter().print(doReinit());
            }
            else if (backupFileName != null)
            {
                agent.getPrinter().print(doBackup(backupFileName));
            }
            else if (knowledgeToAdd != null)
            {
                agent.getPrinter().print(doAdd(knowledgeToAdd));
            }
            else
            {
                agent.getPrinter().print(doEpmem());
            }
        }
        
        private String doSet(String paramToSet, String value)
        {
            final PropertyManager props = epmem.getParams().getProperties();
            
            try
            {
                if (paramToSet.equals("learning"))
                {
                    props.set(DefaultEpisodicMemoryParams.LEARNING, Learning.valueOf(value));
                    return "Set learning to " + Learning.valueOf(value).toString();
                }
                else if (paramToSet.equals("trigger"))
                {
                    props.set(DefaultEpisodicMemoryParams.TRIGGER, Trigger.valueOf(value));
                    return "Set trigger to " + Trigger.valueOf(value).toString();
                }
                else if (paramToSet.equals("phase"))
                {
                    props.set(DefaultEpisodicMemoryParams.PHASE, Phase.valueOf(value));
                    return "Set phase to " + Phase.valueOf(value).toString();
                }
                else if (paramToSet.equals("graph-match"))
                {
                    props.set(DefaultEpisodicMemoryParams.GRAPH_MATCH, GraphMatchChoices.valueOf(value));
                    return "Set graph-match to " + GraphMatchChoices.valueOf(value);
                }
                else if (paramToSet.equals("graph-match-ordering"))
                {
                    props.set(DefaultEpisodicMemoryParams.GM_ORDERING, GmOrderingChoices.valueOf(value));
                    return "Set graph-match-ordering to " + GmOrderingChoices.valueOf(value).toString();
                }
                else if (paramToSet.equals("balance"))
                {
                    props.set(DefaultEpisodicMemoryParams.BALANCE, Double.parseDouble(value));
                    return "Set balance to " + Double.parseDouble(value);
                }
                else if (paramToSet.equals("optimization"))
                {
                    props.set(DefaultEpisodicMemoryParams.OPTIMIZATION, Optimization.valueOf(value));
                    return "Set optimization to " + Optimization.valueOf(value);
                }
                else if (paramToSet.equals("path"))
                {
                    props.set(DefaultEpisodicMemoryParams.PATH, value);
                    return "Set path to " + value;
                }
                else if (paramToSet.equals("append-database"))
                {
                    props.set(DefaultEpisodicMemoryParams.APPEND_DB, AppendDatabaseChoices.valueOf(value));
                    return "Set append to " + AppendDatabaseChoices.valueOf(value);
                }
                else if (paramToSet.equals("page-size"))
                {
                    props.set(DefaultEpisodicMemoryParams.PAGE_SIZE, PageChoices.valueOf(value));
                    return "Set page size to " + PageChoices.valueOf(value);
                }
                else if (paramToSet.equals("cache-size"))
                {
                    props.set(DefaultEpisodicMemoryParams.CACHE_SIZE, Long.valueOf(value));
                    return "Set cache size to " + Long.valueOf(value);
                }
                else if (paramToSet.equals("lazy-commit"))
                {
                    if (epmem.db != null)
                    {
                        return "Lazy commit is protected while the database is open.";
                    }
                    props.set(DefaultEpisodicMemoryParams.LAZY_COMMIT, LazyCommitChoices.valueOf(value));
                    return "Set lazy-commit to " + LazyCommitChoices.valueOf(value);
                }
                else if (paramToSet.equals("exclusions"))
                {
                    DefaultEpisodicMemoryParams params = epmem.getParams();
                    
                    SymbolImpl sym = symbols.createString(value);
                    
                    // TODO Output something here?
                    if (params.exclusions.contains(sym))
                    {
                        params.exclusions.remove(sym);
                    }
                    else
                    {
                        params.exclusions.add(sym);
                    }
                }
                else if (paramToSet.equals("inclusions"))
                {
                    DefaultEpisodicMemoryParams params = epmem.getParams();
                    
                    SymbolImpl sym = symbols.createString(value);
                    
                    // TODO Output something here?
                    if (params.inclusions.contains(sym))
                    {
                        params.inclusions.remove(sym);
                    }
                    else
                    {
                        params.inclusions.add(sym);
                    }
                }
                else if (paramToSet.equals("force"))
                {
                    props.set(DefaultEpisodicMemoryParams.FORCE, Force.valueOf(value));
                    return "EpMem| force = " + value;
                }
                else if (paramToSet.equals("database"))
                {
                    if (value.equals("memory"))
                    {
                        props.set(DefaultEpisodicMemoryParams.PATH, EpisodicMemoryDatabase.IN_MEMORY_PATH);
                        return "EpMem| database = memory";
                    }
                    else if (value.equals("file"))
                    {
                        props.set(DefaultEpisodicMemoryParams.PATH, "");
                        return "EpMem| database = file";
                    }
                    else
                    {
                        throw new ParameterException(spec.commandLine(), "Invalid value for EpMem database parameter");
                    }
                }
                else
                {
                    agent.getPrinter().startNewLine().print("Unknown epmem parameter '" + paramToSet + "'");
                }
            }
            catch (IllegalArgumentException e) // this is thrown by the enums if a bad value is passed in
            {
                throw new ParameterException(spec.commandLine(), "Invalid value.", e);
            }

            return "";
        }
        
        private String doGet(String paramToGet)
        {
            final PropertyKey<?> key = DefaultEpisodicMemoryParams.getProperty(
                    epmem.getParams().getProperties(), paramToGet);
            if (key == null)
            {
                if (paramToGet.equals("database"))
                {
                    PropertyKey<?> pathProperty = DefaultEpisodicMemoryParams.getProperty(
                            epmem.getParams().getProperties(), "path");
                    if (pathProperty == null)
                    {
                        agent.getPrinter().startNewLine().print("Path is null.");
                        return "";
                    }
                    
                    String path = epmem.getParams().getProperties().get(pathProperty).toString();
                    if (path.equals(EpisodicMemoryDatabase.IN_MEMORY_PATH))
                    {
                        return "memory";
                    }
                    else
                    {
                        return "file";
                    }
                }
                else if (paramToGet.equals("exclusions"))
                {
                    String exclusionStringList = "";
                    
                    Set<SymbolImpl> exclusions = epmem.getParams().exclusions;
                    Iterator<SymbolImpl> it = exclusions.iterator();
                    while (it.hasNext())
                    {
                        exclusionStringList += it.next().toString();
                        if (it.hasNext())
                        {
                            exclusionStringList += ", ";
                        }
                    }
                    
                    return exclusionStringList;
                }
                else if (paramToGet.equals("inclusions"))
                {
                    String inclusionStringList = "";
                    
                    Set<SymbolImpl> inclusions = epmem.getParams().inclusions;
                    Iterator<SymbolImpl> it = inclusions.iterator();
                    while (it.hasNext())
                    {
                        inclusionStringList += it.next().toString();
                        if (it.hasNext())
                        {
                            inclusionStringList += ", ";
                        }
                    }
                    
                    return inclusionStringList;
                }
                else
                {
                    throw new ParameterException(spec.commandLine(), "Unknown parameter '" + paramToGet + "'");
                }
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
            pw.printf(PrintHelper.generateItem("inclusions:", p.inclusions, 40));
            pw.printf(PrintHelper.generateSection("Storage", 40));
            pw.printf(PrintHelper.generateItem("driver:", p.driver, 40));
            
            String nativeOrPure = null;
            try
            {
                EpisodicMemoryDatabase db = epmem.getDatabase();
                if (db != null)
                {
                    nativeOrPure = db.getConnection().getMetaData().getDriverVersion();
                }
                else
                {
                    nativeOrPure = "Not connected to database";
                }
            }
            catch (SQLException e)
            {
                agent.getPrinter().startNewLine().print(e.getMessage());
                return "";
            }
            
            pw.printf(PrintHelper.generateItem("driver-type:", nativeOrPure, 40));
            pw.printf(PrintHelper.generateItem("protocol:", p.protocol.get(), 40));
            pw.printf(PrintHelper.generateItem("append-database:", p.append_database.get(), 40));
            
            String database = "memory";
            String path = "";
            if (!p.path.get().equals(EpisodicMemoryDatabase.IN_MEMORY_PATH))
            {
                database = "file";
                path = p.path.get();
            }
            
            pw.printf(PrintHelper.generateItem("database:", database, 40));        
            pw.printf(PrintHelper.generateItem("path:", path, 40));
            pw.printf(PrintHelper.generateItem("lazy-commit:", p.lazy_commit.get(), 40));
            pw.printf(PrintHelper.generateSection("Retrieval", 40));
            pw.printf(PrintHelper.generateItem("balance:", p.balance.get(), 40));
            pw.printf(PrintHelper.generateItem("graph-match:", p.graph_match.get(), 40));
            pw.printf(PrintHelper.generateItem("graph-match-ordering:", p.gm_ordering.get(), 40));
            pw.printf(PrintHelper.generateSection("Performance", 40));
            pw.printf(PrintHelper.generateItem("page-size:", p.page_size.get(), 40));
            pw.printf(PrintHelper.generateItem("cache-size:", p.cache_size.get(), 40));
            pw.printf(PrintHelper.generateItem("optimization:", p.optimization.get(), 40));
            pw.printf(PrintHelper.generateItem("timers:", "off", 40));
            pw.printf(PrintHelper.generateSection("Experimental", 40));
            pw.printf(PrintHelper.generateItem("merge:", p.merge.get(), 40));
            
            pw.flush();
            return sw.toString();
        }

        private String doStats(String statToPrint)
        {
            final StringWriter sw = new StringWriter();
            final PrintWriter pw = new PrintWriter(sw);
            
            if (epmem.getDatabase() == null)
            {
                try
                {
                    epmem.epmem_init_db();
                }
                catch (SoarException e)
                {
                    agent.getPrinter().startNewLine().print(e.getMessage());
                    return "";
                }
            }

            final DefaultEpisodicMemoryStats stats = epmem.stats;
            if (statToPrint == null)
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
                    agent.getPrinter().startNewLine().print(e.getMessage());
                    return "";
                }
                
                Statement s = null;
                long pageCount = 0;
                long pageSize = 0;
                try
                {
                    s = epmem.getDatabase().getConnection().createStatement();
                    
                    ResultSet rs = null;
                    try
                    {
                        rs = s.executeQuery("PRAGMA page_count");
                        pageCount = rs.getLong(0 + 1);
                    }
                    finally
                    {
                        rs.close();
                    }
                    
                    try
                    {
                        rs = s.executeQuery("PRAGMA page_size");
                        pageSize = rs.getLong(0 + 1);
                    }
                    finally
                    {
                        rs.close();
                    }
                }
                catch (SQLException e)
                {
                    agent.getPrinter().startNewLine().print(e.getMessage());
                    return "";
                }
                finally
                {
                    try
                    {
                        s.close();
                    }
                    catch (SQLException e)
                    {
                        agent.getPrinter().startNewLine().print(e.getMessage());
                        return "";
                    }
                }
                
                stats.mem_usage.set(pageCount * pageSize);
                            
                pw.printf(PrintHelper.generateItem("Memory Usage:",
                        new Double(stats.mem_usage.get()) / 1024.0 + " KB", 40));
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
                pw.printf(PrintHelper.generateItem("Last Graph Match Attempts:",
                        stats.last_graph_matches.get(), 40));
                pw.printf(PrintHelper.generateItem("Episodes Considered:", stats.considered.get(), 40));
                pw.printf(PrintHelper.generateItem("Last Episodes Considered:",
                        stats.last_considered.get(), 40));
            }
            else
            {
                final PropertyKey<?> key = DefaultEpisodicMemoryStats.getProperty(
                        epmem.getParams().getProperties(), statToPrint);
                if (key == null)
                {
                    agent.getPrinter().startNewLine().print("Unknown stat '" + statToPrint + "'");
                    return "";
                }
                pw.printf("%s%n", epmem.getParams().getProperties().get(key).toString());
            }

            pw.flush();
            return sw.toString();
        }
        
        private String doPrintEpisode(String episodeNum)
        {
            long episodeID;
            try
            {
                episodeID = Integer.parseInt(episodeNum);
            }
            catch (NumberFormatException e)
            {
                throw new ParameterException(spec.commandLine(), "Parameter provided is not an integer", e);
            }
            return epmem.epmem_print_episode(episodeID);
        }
        
        private String doReinit()
        {
            epmem.epmem_reinit();
            return "EpMem| Episodic memory system re-initialized.";
        }

        private String doBackup(String[] fileName)
        {
            ByRef<String> err = new ByRef<String>("");
            boolean success = false;

            String dbFile = "";

            for (int i = 0; i < fileName.length; i++)
            {
                dbFile += fileName[i] + " ";
            }

            dbFile = dbFile.trim();

            try
            {
                success = epmem.epmem_backup_db(dbFile, err);
            }
            catch (SQLException e)
            {
                throw new ExecutionException(spec.commandLine(), e.getMessage(), e);
            }

            if (!success)
            {
                throw new ExecutionException(spec.commandLine(), err.value);
            }

            return "EpMem| Database backed up to " + dbFile;
        }
        
        private String doAdd(String knowledge)
        {
            try
            {
                epmem.epmem_parse_and_add(knowledge);
            }
            catch (SoarException e)
            {
                agent.getPrinter().startNewLine().print(e.getMessage());
                return "";
            }
            return "EpMem| Knowledge added to episodic memory.";
        }
    }
}
