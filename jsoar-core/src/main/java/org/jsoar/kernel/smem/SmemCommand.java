package org.jsoar.kernel.smem;

import java.io.IOException;
import java.io.PrintWriter;
import java.io.StringReader;
import java.io.StringWriter;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.Arrays;

import org.jsoar.kernel.Agent;
import org.jsoar.kernel.Production;
import org.jsoar.kernel.SoarException;
import org.jsoar.kernel.epmem.EpisodicMemory;
import org.jsoar.kernel.parser.original.LexemeType;
import org.jsoar.kernel.parser.original.Lexer;
import org.jsoar.kernel.smem.DefaultSemanticMemory.BasicWeightedCue;
import org.jsoar.kernel.smem.DefaultSemanticMemoryParams.ActivateOnQueryChoices;
import org.jsoar.kernel.smem.DefaultSemanticMemoryParams.ActivationChoices;
import org.jsoar.kernel.smem.DefaultSemanticMemoryParams.AppendDatabaseChoices;
import org.jsoar.kernel.smem.DefaultSemanticMemoryParams.BaseUpdateChoices;
import org.jsoar.kernel.smem.DefaultSemanticMemoryParams.LazyCommitChoices;
import org.jsoar.kernel.smem.DefaultSemanticMemoryParams.LearningChoices;
import org.jsoar.kernel.smem.DefaultSemanticMemoryParams.MergeChoices;
import org.jsoar.kernel.smem.DefaultSemanticMemoryParams.MirroringChoices;
import org.jsoar.kernel.smem.DefaultSemanticMemoryParams.Optimization;
import org.jsoar.kernel.smem.DefaultSemanticMemoryParams.PageChoices;
import org.jsoar.kernel.tracing.Printer;
import org.jsoar.util.ByRef;
import org.jsoar.util.JdbcTools;
import org.jsoar.util.PrintHelper;
import org.jsoar.util.adaptables.Adaptable;
import org.jsoar.util.adaptables.Adaptables;
import org.jsoar.util.commands.PicocliSoarCommand;
import org.jsoar.util.commands.SoarCommandInterpreter;
import org.jsoar.util.commands.SoarCommandProvider;
import org.jsoar.util.properties.PropertyKey;
import org.jsoar.util.properties.PropertyManager;
import org.sqlite.SQLiteJDBCLoader;

import com.google.common.base.Joiner;

import picocli.CommandLine.Command;
import picocli.CommandLine.ExecutionException;
import picocli.CommandLine.HelpCommand;
import picocli.CommandLine.Model.CommandSpec;
import picocli.CommandLine.Option;
import picocli.CommandLine.ParameterException;
import picocli.CommandLine.Parameters;
import picocli.CommandLine.Spec;

/**
 * This is the implementation of the "smem" command.
 * 
 * @author austin.brehob
 */
public class SmemCommand extends PicocliSoarCommand
{
    public static class Provider implements SoarCommandProvider
    {
        @Override
        public void registerCommands(SoarCommandInterpreter interp, Adaptable context)
        {
            Agent agent = (Agent) context.getAdapter(Agent.class);
            DefaultSemanticMemory smem = Adaptables.require(getClass(), context, DefaultSemanticMemory.class);
            interp.addCommand("smem", new SmemCommand(agent, smem));
        }
    }
    
    public SmemCommand(Agent agent, DefaultSemanticMemory smem)
    {
        super(agent, new SmemC(agent, smem));
    }
    
    @Command(name = "smem", description = "Controls the behavior of "
            + "and displays information about semantic memory", subcommands = { HelpCommand.class })
    public static class SmemC implements Runnable
    {
        private final Agent agent;
        private final DefaultSemanticMemory smem;
        private Lexer lexer;
        
        @Spec
        private CommandSpec spec; // injected by picocli
        
        public SmemC(Agent agent, DefaultSemanticMemory smem)
        {
            this.agent = agent;
            this.smem = smem;
            try
            {
                this.lexer = new Lexer(new Printer(new PrintWriter(System.out)), new StringReader(""));
            }
            catch(IOException e)
            {
                throw new RuntimeException("SmemCommand failed to create Lexer", e);
            }
        }
        
        @Option(names = { "-e", "--on", "--enable" }, defaultValue = "false", description = "Enable semantic memory")
        boolean enable;
        
        @Option(names = { "-d", "--off", "--disable" }, defaultValue = "false", description = "Disables semantic memory")
        boolean disable;
        
        @Option(names = { "-a", "--add" }, description = "Adds concepts to semantic memory")
        String conceptsToAdd = null;
        
        @Option(names = { "-b", "--backup" }, arity = "1..*", description = "Creates "
                + "a backup of the semantic database on disk")
        String[] backupFileName = null;
        
        @Option(names = { "-c", "--commit" }, description = "Commits data to semantic database")
        boolean commitData = false;
        
        @Option(names = { "-g", "--get" }, description = "Prints current parameter setting")
        String getParam = null;
        
        @Option(names = { "-i", "--init" }, description = "Deletes all memories if 'append' is off")
        boolean initialize = false;
        
        @Option(names = { "-l", "--lastcue" }, description = "Prints the cue from the last decision cycle")
        boolean getLastCue = false;
        
        @Option(names = { "-p", "--print" }, description = "Prints general or specific contents of semantic memory")
        boolean printContents = false;
        
        @Option(names = { "-q", "--sql" }, arity = "1..*", description = "Runs the "
                + "SQL statement on the semantic database")
        String[] sqlStatement = null;
        
        @Option(names = { "-s", "--set" }, description = "Sets parameter value")
        String setParam = null;
        
        @Option(names = { "-S", "--stats" }, description = "Prints statistic summary or specific statistic")
        boolean printStats = false;
        
        @Option(names = { "-t", "--timers" }, description = "Prints timer summary "
                + "or specific timer (not implemented)")
        boolean printTimers = false;
        
        @Option(names = { "-v", "--viz" }, description = "Prints semantic memory visualization")
        boolean printVisualization = false;
        
        @Parameters(index = "0", arity = "0..1", description = "The contents to print; or the new value "
                + "of the parameter; or the specific statistic to print")
        String param = null;
        
        @Parameters(index = "1", arity = "0..1", description = "The print depth")
        Integer printDepth = null;
        
        @Override
        public void run()
        {
            if(enable && !disable)
            {
                doSet("learning", "on");
            }
            else if(!enable && disable)
            {
                doSet("learning", "off");
            }
            else if(enable && disable)
            {
                agent.getPrinter().print("smem takes only one option at a time");
                return;
            }
            
            if(conceptsToAdd != null)
            {
                agent.getPrinter().print(doAdd(conceptsToAdd));
            }
            else if(backupFileName != null)
            {
                agent.getPrinter().print(doBackup(backupFileName));
            }
            else if(commitData)
            {
                agent.getPrinter().print(doCommit());
            }
            else if(getParam != null)
            {
                agent.getPrinter().print(doGet(getParam));
            }
            else if(initialize)
            {
                agent.getPrinter().print(doInit());
            }
            else if(getLastCue)
            {
                agent.getPrinter().print(doLastCue());
            }
            else if(printContents)
            {
                agent.getPrinter().print(doPrint(param, printDepth));
            }
            else if(sqlStatement != null)
            {
                agent.getPrinter().print(doSql(sqlStatement));
            }
            else if(setParam != null)
            {
                if(param == null)
                {
                    throw new ParameterException(spec.commandLine(), "No parameter value provided");
                }
                agent.getPrinter().print(doSet(setParam, param));
            }
            else if(printStats)
            {
                agent.getPrinter().print(doStats(param));
            }
            else if(printTimers)
            {
                agent.getPrinter().print(doTimers(param));
            }
            else if(printVisualization)
            {
                agent.getPrinter().print(doViz(param));
            }
            else
            {
                agent.getPrinter().print(doSmem());
            }
        }
        
        private String doAdd(String conceptsToAdd)
        {
            try
            {
                // Braces are stripped by the interpreter, so put them back
                smem.smem_parse_chunks("{" + conceptsToAdd + "}");
            }
            catch(SoarException e)
            {
                agent.getPrinter().startNewLine().print(e.getMessage());
                return "";
            }
            return "SMem| Knowledge added to semantic memory.";
        }
        
        private String doBackup(String[] backupFileName)
        {
            ByRef<String> err = new ByRef<>("");
            boolean success = false;
            
            String dbFile = "";
            
            for(String namePiece : backupFileName)
            {
                dbFile += namePiece + " ";
            }
            
            dbFile = dbFile.trim();
            
            try
            {
                success = smem.smem_backup_db(dbFile, err);
            }
            catch(SQLException e)
            {
                throw new ExecutionException(spec.commandLine(), e.getMessage(), e);
            }
            
            if(!success)
            {
                throw new ExecutionException(spec.commandLine(), err.value);
            }
            
            return "SMem| Database backed up to " + dbFile;
        }
        
        private String doCommit()
        {
            if(smem.getDatabase() == null)
            {
                agent.getPrinter().startNewLine().print("Semantic memory database is not open.");
                return "";
            }
            if(smem.getParams().lazy_commit.get() == LazyCommitChoices.off)
            {
                return "Semantic memory database is not in lazy-commit mode.";
            }
            
            try
            {
                smem.commit();
            }
            catch(SoarException e)
            {
                agent.getPrinter().startNewLine().print(e.getMessage());
            }
            return "";
        }
        
        private String doGet(String getParam)
        {
            final PropertyKey<?> key = DefaultSemanticMemoryParams.getProperty(
                    smem.getParams().getProperties(), getParam);
            if(key == null)
            {
                if(getParam.equals("database"))
                {
                    PropertyKey<?> pathProperty = DefaultSemanticMemoryParams.getProperty(
                            smem.getParams().getProperties(), "path");
                    if(pathProperty == null)
                    {
                        agent.getPrinter().startNewLine().print("Path is null.");
                        return "";
                    }
                    
                    String path = smem.getParams().getProperties().get(pathProperty).toString();
                    if(path.equals(SemanticMemoryDatabase.IN_MEMORY_PATH))
                    {
                        return "memory";
                    }
                    else
                    {
                        return "file";
                    }
                }
                else
                {
                    throw new ParameterException(spec.commandLine(), "Unknown parameter '" + getParam + "'");
                }
            }
            return smem.getParams().getProperties().get(key).toString();
        }
        
        private String doInit()
        {
            // Because of LTIs, re-initializing requires all other memories to be reinitialized.
            // epmem - close before working/production memories to get re-init benefits
            // smem - close before working/production memories to prevent id counter mess-ups
            // production memory (automatic init-soar clears working memory as a result)
            
            final EpisodicMemory epmem = Adaptables.require(getClass(), agent, EpisodicMemory.class);
            try
            {
                epmem.epmem_close();
                smem.smem_close();
            }
            catch(SoarException e)
            {
                agent.getPrinter().startNewLine().print(e.getMessage());
                return "";
            }
            
            int count = 0;
            for(Production p : new ArrayList<Production>(agent.getProductions().getProductions(null)))
            {
                agent.getProductions().exciseProduction(p, false);
                count++;
            }
            agent.initialize();
            
            return "Agent reinitialized.\n" +
                    count + " productions excised.\n" +
                    "SMem| Semantic memory system re-initialized.\n";
        }
        
        private String doLastCue()
        {
            BasicWeightedCue lastCue = smem.getLastCue();
            if(lastCue == null)
            {
                return "Either the last decision cycle did not contain a query, or the query was bad.";
            }
            return lastCue.cue.toString() + " Weight: " + lastCue.weight;
        }
        
        private String doPrint(String param, Integer printDepth)
        {
            final StringWriter sw = new StringWriter();
            final PrintWriter pw = new PrintWriter(sw);
            
            long /* smem_lti_id */ lti_id = 0 /* NIL */;
            int depth = 1;
            
            try
            {
                smem.smem_attach();
            }
            catch(SoarException e)
            {
                agent.getPrinter().startNewLine().print(e.getMessage());
                return "";
            }
            
            StringBuilder viz = new StringBuilder("");
            
            try
            {
                if(param != null)
                {
                    // not sure if we have to do this, but better safe than sorry --
                    // store old value and restore it after we're done
                    boolean allowIdsOld = lexer.isAllowIds();
                    
                    lexer.setAllowIds(true);
                    lexer.get_lexeme_from_string(param);
                    if(lexer.getCurrentLexeme().type == LexemeType.IDENTIFIER)
                    {
                        final char name_letter = lexer.getCurrentLexeme().id_letter;
                        final long name_number = lexer.getCurrentLexeme().id_number;
                        
                        if(smem.getDatabase() != null)
                        {
                            lti_id = smem.smem_lti_get_id(name_letter, name_number);
                            
                            if(lti_id == 0)
                            {
                                agent.getPrinter().startNewLine().print("'" +
                                        lexer.getCurrentLexeme() + "' is not an LTI");
                                return "";
                            }
                            
                            if((lti_id != 0 /* NIL */) && printDepth != null)
                            {
                                depth = printDepth;
                            }
                        }
                        
                        smem.smem_print_lti(lti_id, depth, viz);
                    }
                    else
                    {
                        throw new ParameterException(spec.commandLine(), "Expected identifier, got '" + lexer.getCurrentLexeme() + "'");
                    }
                    
                    // restore original value
                    lexer.setAllowIds(allowIdsOld);
                }
                else
                {
                    smem.smem_print_store(viz);
                }
            }
            catch(SoarException e)
            {
                throw new ExecutionException(spec.commandLine(), e.getMessage(), e);
            }
            
            if(viz.length() == 0)
            {
                return "SMem| Semantic memory is empty.";
            }
            
            pw.printf(viz.toString());
            
            pw.flush();
            return sw.toString();
        }
        
        private String doSql(String[] sqlStatement)
        {
            final String sql = Joiner.on(' ').join(Arrays.copyOfRange(
                    sqlStatement, 0, sqlStatement.length)).trim();
            if(smem.getDatabase() == null)
            {
                agent.getPrinter().startNewLine().print("Semantic memory database is not open.");
                return "";
            }
            
            try(Statement s = smem.getDatabase().getConnection().createStatement())
            {
                final StringWriter out = new StringWriter();
                if(s.execute(sql))
                {
                    JdbcTools.printResultSet(s.getResultSet(), out);
                }
                return out.toString();
            }
            catch(SQLException e)
            {
                agent.getPrinter().startNewLine().print(e.getMessage());
            }
            
            return "";
        }
        
        private String doSet(String setParam, String value)
        {
            final PropertyManager props = smem.getParams().getProperties();
            
            try
            {
                if(setParam.equals("learning"))
                {
                    props.set(DefaultSemanticMemoryParams.LEARNING, LearningChoices.valueOf(value));
                }
                else if(setParam.equals("driver"))
                {
                    props.set(DefaultSemanticMemoryParams.DRIVER, value);
                }
                else if(setParam.equals("protocol"))
                {
                    props.set(DefaultSemanticMemoryParams.PROTOCOL, value);
                }
                else if(setParam.equals("path"))
                {
                    props.set(DefaultSemanticMemoryParams.PATH, value);
                }
                else if(setParam.equals("lazy-commit"))
                {
                    props.set(DefaultSemanticMemoryParams.LAZY_COMMIT,
                            LazyCommitChoices.valueOf(value));
                }
                else if(setParam.equals("append-database"))
                {
                    props.set(DefaultSemanticMemoryParams.APPEND_DB,
                            AppendDatabaseChoices.valueOf(value));
                }
                else if(setParam.equals("page-size"))
                {
                    props.set(DefaultSemanticMemoryParams.PAGE_SIZE, PageChoices.valueOf(value));
                }
                else if(setParam.equals("cache-size"))
                {
                    props.set(DefaultSemanticMemoryParams.CACHE_SIZE, Long.valueOf(value));
                }
                else if(setParam.equals("optimization"))
                {
                    props.set(DefaultSemanticMemoryParams.OPTIMIZATION, Optimization.valueOf(value));
                }
                else if(setParam.equals("thresh"))
                {
                    props.set(DefaultSemanticMemoryParams.THRESH, Long.valueOf(value));
                }
                else if(setParam.equals("merge"))
                {
                    props.set(DefaultSemanticMemoryParams.MERGE, MergeChoices.valueOf(value));
                }
                else if(setParam.equals("activation-mode"))
                {
                    // note this uses our custom getEnum method instead
                    // of valueOf to support the dash in "base-level"
                    props.set(DefaultSemanticMemoryParams.ACTIVATION_MODE,
                            ActivationChoices.getEnum(value));
                }
                else if(setParam.equals("activate-on-query"))
                {
                    props.set(DefaultSemanticMemoryParams.ACTIVATE_ON_QUERY,
                            ActivateOnQueryChoices.valueOf(value));
                }
                else if(setParam.equals("base-decay"))
                {
                    props.set(DefaultSemanticMemoryParams.BASE_DECAY, Double.valueOf(value));
                }
                else if(setParam.equals("base-update-policy"))
                {
                    props.set(DefaultSemanticMemoryParams.BASE_UPDATE,
                            BaseUpdateChoices.valueOf(value));
                }
                else if(setParam.equals("base-incremental-threshes"))
                {
                    props.set(DefaultSemanticMemoryParams.BASE_INCREMENTAL_THRESHES,
                            smem.getParams().base_incremental_threshes.get().toSetWrapper(value));
                }
                else if(setParam.equals("mirroring"))
                {
                    props.set(DefaultSemanticMemoryParams.MIRRORING,
                            MirroringChoices.valueOf(value));
                }
                else if(setParam.equals("database"))
                {
                    if(value.equals("memory"))
                    {
                        props.set(DefaultSemanticMemoryParams.PATH,
                                SemanticMemoryDatabase.IN_MEMORY_PATH);
                        return "SMem| database = memory";
                    }
                    else if(value.equals("file"))
                    {
                        props.set(DefaultSemanticMemoryParams.PATH, "");
                        return "SMem| database = file";
                    }
                    else
                    {
                        throw new ParameterException(spec.commandLine(), "Invalid value for SMem database parameter");
                    }
                }
                else
                {
                    throw new ParameterException(spec.commandLine(), "Unknown smem parameter '" + setParam + "'");
                }
            }
            // this is thrown by the enums if a bad value is passed in
            catch(IllegalArgumentException e)
            {
                throw new ParameterException(spec.commandLine(), "Invalid value.", e);
            }
            
            return "";
        }
        
        private String doStats(String statToPrint)
        {
            final StringWriter sw = new StringWriter();
            final PrintWriter pw = new PrintWriter(sw);
            
            try
            {
                smem.smem_attach();
            }
            catch(SoarException e)
            {
                agent.getPrinter().startNewLine().print(e.getMessage());
                return "";
            }
            
            final DefaultSemanticMemoryStats p = smem.getStats();
            if(statToPrint == null)
            {
                pw.printf(PrintHelper.generateHeader("Semantic Memory Statistics", 40));
                
                try
                {
                    String database = smem.getDatabase().getConnection().getMetaData().getDatabaseProductName();
                    String version = smem.getDatabase().getConnection().getMetaData().getDatabaseProductVersion();
                    pw.printf(PrintHelper.generateItem(database + " Version:", version, 40));
                }
                catch(SQLException e)
                {
                    agent.getPrinter().startNewLine().print(e.getMessage());
                    return "";
                }
                
                long pageCount = 0;
                long pageSize = 0;
                try(Statement s = smem.getDatabase().getConnection().createStatement())
                {
                    
                    try(ResultSet rs = s.executeQuery("PRAGMA page_count"))
                    {
                        pageCount = rs.getLong(0 + 1);
                    }
                    
                    try(ResultSet rs = s.executeQuery("PRAGMA page_size"))
                    {
                        pageSize = rs.getLong(0 + 1);
                    }
                }
                catch(SQLException e)
                {
                    throw new ExecutionException(spec.commandLine(), e.getMessage(), e);
                }
                
                p.mem_usage.set(pageCount * pageSize);
                
                pw.printf(PrintHelper.generateItem("Memory Usage:", p.mem_usage.get() / 1024.0 + " KB", 40));
                pw.printf(PrintHelper.generateItem("Memory Highwater:", p.mem_high.get(), 40));
                pw.printf(PrintHelper.generateItem("Retrieves:", p.retrieves.get(), 40));
                pw.printf(PrintHelper.generateItem("Queries:", p.queries.get(), 40));
                pw.printf(PrintHelper.generateItem("Stores:", p.stores.get(), 40));
                pw.printf(PrintHelper.generateItem("Activation Updates:", p.act_updates.get(), 40));
                pw.printf(PrintHelper.generateItem("Mirrors:", p.mirrors.get(), 40));
                pw.printf(PrintHelper.generateItem("Nodes:", p.nodes.get(), 40));
                pw.printf(PrintHelper.generateItem("Edges:", p.edges.get(), 40));
            }
            else
            {
                final PropertyKey<?> key = DefaultSemanticMemoryStats.getProperty(
                        smem.getParams().getProperties(), statToPrint);
                if(key == null)
                {
                    throw new ParameterException(spec.commandLine(), "Unknown stat '" + statToPrint + "'");
                }
                pw.printf(PrintHelper.generateItem(key + ":",
                        smem.getParams().getProperties().get(key).toString(), 40));
            }
            
            pw.flush();
            return sw.toString();
        }
        
        private String doTimers(String timerToPrint)
        {
            throw new ExecutionException(spec.commandLine(), "This command has not been implemented in JSoar.");
        }
        
        private String doViz(String arg)
        {
            if(arg == null)
            {
                final StringWriter sw = new StringWriter();
                final PrintWriter pw = new PrintWriter(sw);
                try
                {
                    smem.smem_visualize_store(pw);
                }
                catch(SoarException e)
                {
                    throw new ExecutionException(spec.commandLine(), e.getMessage(), e);
                }
                pw.flush();
                return sw.toString();
            }
            // TODO SMEM Commands: --viz with args
            throw new ExecutionException(spec.commandLine(), "smem --viz with args has not been implemented in JSoar.");
        }
        
        private String doSmem()
        {
            final StringWriter sw = new StringWriter();
            final PrintWriter pw = new PrintWriter(sw);
            
            final DefaultSemanticMemoryParams p = smem.getParams();
            pw.printf(PrintHelper.generateHeader("Semantic Memory Settings", 40));
            
            pw.printf(PrintHelper.generateItem("learning:", p.learning.get(), 40));
            pw.printf(PrintHelper.generateSection("Storage", 40));
            
            pw.printf(PrintHelper.generateItem("driver:", p.driver.get(), 40));
            
            String nativeOrPure = null;
            try
            {
                SemanticMemoryDatabase db = smem.getDatabase();
                if(db != null)
                {
                    nativeOrPure = ((SQLiteJDBCLoader.isNativeMode()) ? "Native" : "Pure Java") +
                            " - " +
                            db.getConnection().getMetaData().getDriverVersion();
                }
                else
                {
                    nativeOrPure = "Not connected to database";
                }
            }
            catch(Exception e) // SQLiteJDBCLoader.isNativeMode() throws Exception, but nothing throws InterruptedException so this should be ok
            {
                agent.getPrinter().startNewLine().print(e.getMessage());
                return "";
            }
            
            pw.printf(PrintHelper.generateItem("driver-type:", nativeOrPure, 40));
            pw.printf(PrintHelper.generateItem("protocol:", p.protocol.get(), 40));
            pw.printf(PrintHelper.generateItem("append-database:", p.append_db.get(), 40));
            
            String database = "memory";
            String path = "";
            if(!p.path.get().equals(SemanticMemoryDatabase.IN_MEMORY_PATH))
            {
                database = "file";
                path = p.path.get();
            }
            
            pw.printf(PrintHelper.generateItem("database:", database, 40));
            pw.printf(PrintHelper.generateItem("path:", path, 40));
            pw.printf(PrintHelper.generateItem("lazy-commit:", p.lazy_commit.get(), 40));
            
            pw.printf(PrintHelper.generateSection("Activation", 40));
            
            pw.printf(PrintHelper.generateItem("activation-mode:", p.activation_mode.get(), 40));
            pw.printf(PrintHelper.generateItem("activate-on-query:", p.activate_on_query.get(), 40));
            pw.printf(PrintHelper.generateItem("base-decay:", p.base_decay.get(), 40));
            pw.printf(PrintHelper.generateItem("base-update-policy", p.base_update.get(), 40));
            pw.printf(PrintHelper.generateItem("base-incremental-threshes", p.base_incremental_threshes.get(), 40));
            pw.printf(PrintHelper.generateItem("thresh", p.thresh.get(), 40));
            
            pw.printf(PrintHelper.generateSection("Performance", 40));
            
            pw.printf(PrintHelper.generateItem("page-size:", p.page_size.get(), 40));
            pw.printf(PrintHelper.generateItem("cache-size:", p.cache_size.get(), 40));
            pw.printf(PrintHelper.generateItem("optimization:", p.optimization.get(), 40));
            pw.printf(PrintHelper.generateItem("timers:", "off - Not Implemented", 40));
            
            pw.printf(PrintHelper.generateSection("Experimental", 40));
            
            pw.printf(PrintHelper.generateItem("merge:", p.merge.get(), 40));
            pw.printf(PrintHelper.generateItem("mirroring:", p.mirroring.get(), 40));
            
            pw.flush();
            return sw.toString();
        }
    }
}
