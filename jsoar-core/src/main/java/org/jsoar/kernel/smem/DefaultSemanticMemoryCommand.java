/*
 * Copyright (c) 2010 Dave Ray <daveray@gmail.com>
 *
 * Created on Jun 27, 2010
 */
package org.jsoar.kernel.smem;

import java.io.IOException;
import java.io.PrintWriter;
import java.io.StringReader;
import java.io.StringWriter;
import java.io.Writer;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.Arrays;

import org.jsoar.kernel.Agent;
import org.jsoar.kernel.Production;
import org.jsoar.kernel.SoarException;
import org.jsoar.kernel.parser.original.LexemeType;
import org.jsoar.kernel.parser.original.Lexer;
import org.jsoar.kernel.smem.DefaultSemanticMemoryParams.ActivationChoices;
import org.jsoar.kernel.smem.DefaultSemanticMemoryParams.BaseUpdateChoices;
import org.jsoar.kernel.smem.DefaultSemanticMemoryParams.Cache;
import org.jsoar.kernel.smem.DefaultSemanticMemoryParams.MergeChoices;
import org.jsoar.kernel.smem.DefaultSemanticMemoryParams.Optimization;
import org.jsoar.kernel.smem.DefaultSemanticMemoryParams.PageChoices;
import org.jsoar.kernel.smem.DefaultSemanticMemoryParams.SetWrapperLong;
import org.jsoar.kernel.tracing.Printer;
import org.jsoar.util.ByRef;
import org.jsoar.util.JdbcTools;
import org.jsoar.util.adaptables.Adaptable;
import org.jsoar.util.adaptables.Adaptables;
import org.jsoar.util.commands.SoarCommand;
import org.jsoar.util.commands.SoarCommandContext;
import org.jsoar.util.commands.SoarCommandInterpreter;
import org.jsoar.util.commands.SoarCommandProvider;
import org.jsoar.util.properties.PropertyKey;
import org.jsoar.util.properties.PropertyManager;

import com.google.common.base.Joiner;
import com.google.common.base.Strings;

/**
 * @author ray
 */
class DefaultSemanticMemoryCommand implements SoarCommand
{
    private final Adaptable context;
    private final DefaultSemanticMemory smem;
    private Lexer lexer;
    
    public static class Provider implements SoarCommandProvider
    {
        /* (non-Javadoc)
         * @see org.jsoar.util.commands.SoarCommandProvider#registerCommands(org.jsoar.util.commands.SoarCommandInterpreter)
         */
        @Override
        public void registerCommands(SoarCommandInterpreter interp, Adaptable context)
        {
            interp.addCommand("smem", new DefaultSemanticMemoryCommand(context));
        }
    }
    
    public DefaultSemanticMemoryCommand(Adaptable context)
    {
        this.context = context;
        this.smem = Adaptables.require(getClass(), context, DefaultSemanticMemory.class);
        try
        {
            this.lexer = new Lexer(new Printer(new PrintWriter(System.out)), new StringReader(""));
        }
        catch (IOException e)
        {
            System.out.print(e.getMessage());
            e.printStackTrace();
            this.lexer = null;
        }
    }

    /* (non-Javadoc)
     * @see org.jsoar.util.commands.SoarCommand#execute(java.lang.String[])
     */
    @Override
    public String execute(SoarCommandContext commandContext, String[] args) throws SoarException
    {
        if(args.length == 1)
        {
            return doSmem();
        }
        
        final String arg = args[1];
        if("-a".equals(arg) || "--add".equals(arg))
        {
            return doAdd(1, args);
        }
        else if("-g".equals(arg) || "--get".equals(arg))
        {
            return doGet(1, args);
        }
        else if("-i".equals(arg) || "--init".equals(arg))
        {
            return doInit(1, args);
        }
        else if("-s".equals(arg) || "--set".equals(arg))
        {
            return doSet(1, args);
        }
        else if("-S".equals(arg) || "--stats".equals(arg))
        {
            return doStats(1, args);
        }
        else if("-t".equals(arg) || "--timers".equals(arg))
        {
            return doTimers(1, args);
        }
        else if("-v".equals(arg) || "--viz".equals(arg))
        {
            return doViz(1, args);
        }
        else if("-c".equals(arg) || "--commit".equals(arg))
        {
            return doCommit(1, args);
        }
        else if("-q".equals(arg) || "--sql".equals(arg))
        {
            return doSql(1, args);
        }
        else if("-p".equals(arg) || "--print".equals(arg))
        {
            return doPrint(1, args);
        }
        else if(arg.startsWith("-"))
        {
            throw new SoarException("Unknown option " + arg);
        }
        else
        {
            throw new SoarException("Unknown argument " + arg);
        }
    }

    private String doSql(int i, String[] args) throws SoarException
    {
        if(i + 1 == args.length)
        {
            throw new SoarException("No argument for " + args[i] + " option");
        }
        final String sql = Joiner.on(' ').join(Arrays.copyOfRange(args, i+1, args.length)).trim();
        if(smem.getDatabase() == null)
        {
            throw new SoarException("Semantic memory database is not open.");
        }
        try
        {
            final Statement s = smem.getDatabase().getConnection().createStatement();
            try
            {
                final StringWriter out = new StringWriter();
                if(s.execute(sql))
                {
                    JdbcTools.printResultSet(s.getResultSet(), out);
                }
                return out.toString();
            }
            finally
            {
                s.close();
            }
        }
        catch (SQLException e)
        {
            throw new SoarException(e.getMessage(), e);
        }
    }

    private String doCommit(int i, String[] args) throws SoarException
    {
        if(smem.getDatabase() == null)
        {
            throw new SoarException("Semantic memory database is not open.");
        }
        if(!smem.getParams().lazy_commit.get())
        {
            return "Semantic memory database is not in lazy-commit mode.";
        }
        smem.commit();
        return "";
    }

    private String doAdd(int i, String[] args) throws SoarException
    {
        if(i + 1 == args.length)
        {
            throw new SoarException("No argument for " + args[i] + " option");
        }
        // Braces are stripped by the interpreter, so put them back
        smem.smem_parse_chunks("{" + args[i+1] + "}");
        return "SMem| Knowledge added to semantic memory.";
    }
    
    private String doGet(int i, String[] args) throws SoarException
    {
        if(i + 1 >= args.length)
        {
            throw new SoarException("Invalid arguments for " + args[i] + " option");
        }
        final String name = args[i+1];
        final PropertyKey<?> key = DefaultSemanticMemoryParams.getProperty(smem.getParams().getProperties(), name);
        if(key == null)
        {
            throw new SoarException("Unknown parameter '" + name + "'");
        }
        return smem.getParams().getProperties().get(key).toString();
    }

    private String doSet(int i, String[] args) throws SoarException
    {
        if(i + 2 >= args.length)
        {
            throw new SoarException("Invalid arguments for " + args[i] + " option");
        }
        final String name = args[i+1];
        final String value = args[i+2];
        final PropertyManager props = smem.getParams().getProperties();
        if(name.equals("learning"))
        {
            props.set(DefaultSemanticMemoryParams.LEARNING, "on".equals(value));
        }
        else if(smem.getDatabase() != null)
        {
            // TODO: This check should be done in the property system
            throw new SoarException("This parameter is protected while the semantic memory database is open");
        }
        else if(name.equals("driver"))
        {
            props.set(DefaultSemanticMemoryParams.DRIVER, value);
        }
        else if(name.equals("protocol"))
        {
            props.set(DefaultSemanticMemoryParams.PROTOCOL, value);
        }
        else if(name.equals("path"))
        {
            props.set(DefaultSemanticMemoryParams.PATH, value);
        }
        else if(name.equals("lazy-commit"))
        {
            props.set(DefaultSemanticMemoryParams.LAZY_COMMIT, "on".equals(value));
        }
        else if(name.equals("append-database"))
        {
            props.set(DefaultSemanticMemoryParams.APPEND_DB, "on".equals(value));
        }
        else if(name.equals("page-size"))
        {
            props.set(DefaultSemanticMemoryParams.PAGE_SIZE, PageChoices.valueOf(value));
        }
        else if(name.equals("cache-size"))
        {
            props.set(DefaultSemanticMemoryParams.CACHE_SIZE, Long.valueOf(value));
        }
        else if(name.equals("optimization"))
        {
            props.set(DefaultSemanticMemoryParams.OPTIMIZATION, Optimization.valueOf(value));
        }
        else if(name.equals("thresh"))
        {
            props.set(DefaultSemanticMemoryParams.THRESH, Long.valueOf(value));
        }
        else if(name.equals("merge"))
        {
            props.set(DefaultSemanticMemoryParams.MERGE, MergeChoices.valueOf(value));
        }
        else if(name.equals("activation-mode"))
        {
            props.set(DefaultSemanticMemoryParams.ACTIVATION_MODE, ActivationChoices.valueOf(value));
        }
        else if(name.equals("activate-on-query"))
        {
            props.set(DefaultSemanticMemoryParams.ACTIVATE_ON_QUERY, "on".equals(value));
        }
        else if(name.equals("base-decay"))
        {
            props.set(DefaultSemanticMemoryParams.BASE_DECAY, Double.valueOf(value));
        }
        else if(name.equals("base-update-policy"))
        {
            props.set(DefaultSemanticMemoryParams.BASE_UPDATE, BaseUpdateChoices.valueOf(value));
        }
        else if(name.equals("base-incremental-threshes"))
        {
            props.set(DefaultSemanticMemoryParams.BASE_INCREMENTAL_THRESHES, SetWrapperLong.toSetWrapper(value));
        }
        else if(name.equals("mirroring"))
        {
            props.set(DefaultSemanticMemoryParams.MIRRORING, "on".equals(value));
        }
        else
        {
            throw new SoarException("Unknown smem parameter '" + name + "'");
        }
        
        return "";
    }

    private String doInit(int i, String[] args) throws SoarException
    {
        // Because of LTIs, re-initializing requires all other memories to be reinitialized.        
        
        // epmem - close before working/production memories to get re-init benefits
        // TODO EPMEM this->DoCommandInternal( "epmem --close" );
        
        // smem - close before working/production memories to prevent id counter mess-ups
        smem.smem_close();

        // production memory (automatic init-soar clears working memory as a result) 
        //this->DoCommandInternal( "excise --all" );
        
        // Excise all just removes all rules and does init-soar
        final Agent agent = Adaptables.require(getClass(), context, Agent.class);
        for(Production p : new ArrayList<Production>(agent.getProductions().getProductions(null)))
        {
            agent.getProductions().exciseProduction(p, false);
        }
        agent.initialize();
        
        return "";
    }

    private String doStats(int i, String[] args) throws SoarException
    {
        final StringWriter sw = new StringWriter();
        final PrintWriter pw = new PrintWriter(sw);
        
        smem.smem_attach();
        
        final DefaultSemanticMemoryStats p = smem.getStats();
        if(args.length == i + 1)
        {
            pw.printf(generateHeader("Semantic Memory", 40));
            try
            {
                String database = smem.getDatabase().getConnection().getMetaData().getDatabaseProductName();
                String version = smem.getDatabase().getConnection().getMetaData().getDatabaseProductVersion();
                pw.printf(generateItem(database + " Version:", version, 40));
            }
            catch (SQLException e)
            {
                throw new SoarException(e);
            }
            pw.printf(generateItem("Memory Usage:", p.mem_usage.get(), 40));
            pw.printf(generateItem("Memory Highwater:", p.mem_high.get(), 40));
            pw.printf(generateItem("Retrieves:", p.retrieves.get(), 40));
            pw.printf(generateItem("Queries:", p.queries.get(), 40));
            pw.printf(generateItem("Stores:", p.stores.get(), 40));
            pw.printf(generateItem("Activation Updates:", p.act_updates.get(), 40));
            pw.printf(generateItem("Mirrors:", p.mirrors.get(), 40));
            pw.printf(generateItem("Nodes:", p.nodes.get(), 40));
            pw.printf(generateItem("Edges:", p.edges.get(), 40));
        }
        else
        {
            final String name = args[i+1];
            final PropertyKey<?> key = DefaultSemanticMemoryStats.getProperty(smem.getParams().getProperties(), name);
            if(key == null)
            {
                throw new SoarException("Unknown stat '" + name + "'");
            }
            pw.printf(generateItem(key + ":", smem.getParams().getProperties().get(key).toString(), 40));
        }
        
        pw.flush();
        return sw.toString();
    }

    private String doTimers(int i, String[] args)
    {
        // TODO Auto-generated method stub
        return null;
    }

    private String doViz(int i, String[] args) throws SoarException
    {
        if(i + 1 == args.length)
        {
            final StringWriter sw = new StringWriter();
            final PrintWriter pw = new PrintWriter(sw);
            smem.smem_visualize_store(pw);
            pw.flush();
            return sw.toString();
        }
        // TODO SMEM Commands: --viz with args
        throw new SoarException("smem --viz with args not implemented yet");
    }

    private String doSmem()
    {
        final StringWriter sw = new StringWriter();
        final PrintWriter pw = new PrintWriter(sw);
        
        final DefaultSemanticMemoryParams p = smem.getParams();
        pw.printf(generateHeader("Semantic Memory Settings", 40));
        
        pw.printf(generateItem("learning:", p.learning.get() ? "on" : "off", 40));
        
        pw.printf(generateSection("Storage", 40));
        
        pw.printf(generateItem("driver:", p.driver.get(), 40));
        pw.printf(generateItem("protocol:", p.protocol.get(), 40));
        pw.printf(generateItem("append-database:", p.append_db.get() ? "on" : "off", 40));
        pw.printf(generateItem("path:", p.path.get(), 40));
        pw.printf(generateItem("lazy-commit:", p.lazy_commit.get() ? "on" : "off", 40));
        
        pw.printf(generateSection("Activation", 40));
        
        pw.printf(generateItem("activation-mode:", p.activation_mode.get(), 40));
        pw.printf(generateItem("activate-on-query:", p.activate_on_query.get() ? "on" : "off", 40));
        pw.printf(generateItem("base-decay:", p.base_decay.get(), 40));
        pw.printf(generateItem("base-update-policy", p.base_update.get(), 40));
        pw.printf(generateItem("base-incremental-threshes", p.base_incremental_threshes.get(), 40));
        pw.printf(generateItem("thresh", p.thresh.get(), 40));
        
        pw.printf(generateSection("Performance", 40));
        
        pw.printf(generateItem("page-size:", p.page_size.get(), 40));
        pw.printf(generateItem("cache-size:", p.cache_size.get(), 40));
        pw.printf(generateItem("optimization:", p.optimization.get(), 40));
        pw.printf(generateItem("timers:", "off - Not Implemented", 40));
        
        pw.printf(generateSection("Experimental", 40));
        
        pw.printf(generateItem("merge:", p.merge.get(), 40));
        pw.printf(generateItem("mirroring:", p.mirroring.get() ? "on" : "off", 40));
        
        pw.flush();
        return sw.toString();
    }
    
    private String generateItem(String prefixString, Object printObject, int column_width)
    {
        int left_width, right_width, middle_width;
        String sep_string = null;
        String printString = printObject.toString();
        
        left_width = prefixString.length();
        right_width = printString.length();
        middle_width = column_width - left_width - right_width;
        
        if (middle_width < 0)
            middle_width = 1;
        
        sep_string = Strings.repeat(" ", middle_width);
        
        return prefixString + sep_string + printString + "\n";
    }

    private String generateHeader(String headerString, int column_width)
    {
        int left_width, right_width, header_width;
        String left_string, right_string, sep_string = null;
        
        header_width = headerString.length() + 2;
        left_width = (column_width - header_width) / 2;
        right_width = column_width - left_width - header_width;
        left_string = Strings.repeat(" ", left_width);
        right_string = Strings.repeat(" ", right_width);
        sep_string = Strings.repeat("=", column_width);
        
        String temp_string = sep_string + "\n" +
                             left_string + " " + headerString + " " + right_string + "\n" +
                             sep_string + "\n";
        
        return temp_string;
    }
    
    private String generateSection(String headerString, int column_width)
    {
        int left_width, right_width, header_width;
        String left_string, right_string = null;
        
        header_width = headerString.length() + 2;
        left_width = (column_width - header_width) / 2;
        right_width = column_width - left_width - header_width;
        left_string = Strings.repeat("-", left_width);
        right_string = Strings.repeat("-", right_width);
        
        String temp_string = left_string + " " + headerString + " " + right_string + "\n";
        
        return temp_string;
    }
    
    private String doPrint(int i, String[] args) throws SoarException
    {
        final StringWriter sw = new StringWriter();
        final PrintWriter pw = new PrintWriter(sw);
        
        long /*smem_lti_id*/ lti_id = 0 /*NIL*/;
        int depth = 1;
        
        if (args.length > i && args.length <= i + 3)
        {
            char name_letter = 0;
            long name_number = 0;
            
            if (args.length == i + 2)
            {
                lexer.get_lexeme_from_string(args[i+1]);
                if (lexer.getCurrentLexeme().type == LexemeType.IDENTIFIER)
                {
                    if (smem.getDatabase() != null)
                    {
                        lti_id = smem.smem_lti_get_id(name_letter, name_number);
                        
                        if ( ( lti_id != 0 /*NIL*/ ) && args.length == i + 3)
                        {
                            depth = Integer.parseInt(args[i+2]);
                        }
                    }
                }
            }
            
            ByRef<String> viz = new ByRef<String>(new String());
            
            if (lti_id == 0)
            {
                smem.smem_print_store(viz);
            }
            else
            {
                smem.smem_print_lti(lti_id, depth, viz);
            }
            
            if (viz.value.length() == 0)
            {
                throw new SoarException("SMem| Semantic memory is empty.");
            }
            
            pw.printf(generateHeader("Semantic Memory", 40));
            pw.printf(viz.value);
        }
        else
        {
            throw new SoarException( "Invalid attribute." );
        }
        
        pw.flush();
        return sw.toString();
    }
}
