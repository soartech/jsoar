/*
 * Copyright (c) 2008  Dave Ray <daveray@gmail.com>
 *
 * Created on Oct 30, 2008
 */
package org.jsoar.kernel.commands;

import java.util.ArrayList;
import java.util.List;

import org.jsoar.kernel.Agent;
import org.jsoar.kernel.Production;
import org.jsoar.kernel.ProductionManager;
import org.jsoar.kernel.ProductionType;
import org.jsoar.kernel.SoarException;
import org.jsoar.kernel.memory.Wme;
import org.jsoar.kernel.memory.WorkingMemoryPrinter;
import org.jsoar.kernel.symbols.Symbol;
import org.jsoar.kernel.tracing.Printer;
import org.jsoar.util.commands.OptionProcessor;
import org.jsoar.util.commands.SoarCommand;

import com.google.common.base.Joiner;
import com.google.common.collect.Lists;

/**
 * http://winter.eecs.umich.edu/soarwiki/Print
 * 
 * @author ray
 * @author voigtjr
 */
public class PrintCommand implements SoarCommand
{
    private final Agent agent;
    private final WorkingMemoryPrinter wmp = new WorkingMemoryPrinter();
    private final OptionProcessor<Options> options = OptionProcessor.create();

    private int defaultDepth = 1;
    private int depth = 1;

    private enum Options
    {
        all, chunks, Defaults, depth, Filename, full, internal, justifications, 
        name, operators, rl, stack, States, Template, tree, user, varprint,
    }

    public PrintCommand(Agent agent)
    {
        this.agent = agent;

        options
        .newOption(Options.all)
        .newOption(Options.chunks)
        .newOption(Options.Defaults)
        .newOption(Options.depth).requiredArg()
        .newOption(Options.Filename)
        .newOption(Options.full)
        .newOption(Options.internal)
        .newOption(Options.justifications)
        .newOption(Options.name)
        .newOption(Options.operators)
        .newOption(Options.rl)
        .newOption(Options.stack)
        .newOption(Options.States)
        .newOption(Options.Template)
        .newOption(Options.tree)
        .newOption(Options.user)
        .newOption(Options.varprint)
        .done();
    }

    public void setDefaultDepth(int depth) throws SoarException
    {
        checkDepth(depth);
        this.defaultDepth = depth;
    }
    
    private void checkDepth(int depth) throws SoarException
    {
        if (depth <= 0)
            throw new SoarException("depth must be greater than 0");
    }

    public int getDefaultDepth()
    {
        return this.defaultDepth;
    }

    private List<Production> collectProductions()
    {
        final ProductionManager pm = agent.getProductions();
        final List<Production> result = new ArrayList<Production>();

        if (options.has(Options.chunks))
            result.addAll(pm.getProductions(ProductionType.CHUNK));
        if (options.has(Options.user))
            result.addAll(pm.getProductions(ProductionType.USER));
        if (options.has(Options.Defaults))
            result.addAll(pm.getProductions(ProductionType.DEFAULT));
        if (options.has(Options.Template))
            result.addAll(pm.getProductions(ProductionType.TEMPLATE));
        if (options.has(Options.justifications))
            result.addAll(pm.getProductions(ProductionType.JUSTIFICATION));

        return result;
    }

    @Override
    public String execute(String[] args) throws SoarException
    {
        agent.getPrinter().startNewLine();

        this.depth = defaultDepth;
        List<String> nonOpts = options.process(Lists.newArrayList(args));

        if (options.has(Options.depth))
        {
            int newDepth = options.getInteger(Options.depth);
            checkDepth(newDepth);
            depth = newDepth;
        }

        if (options.has(Options.varprint))
            throw new SoarException("--varprint not implemented yet");

        // New in Soar 8.6.3: if no args or options given, print all prods
        if (args.length == 1)
            options.set(Options.all);

        if (options.has(Options.stack))
        {
            agent.printStackTrace(options.has(Options.States), options.has(Options.operators));
            agent.getPrinter().print("\n").flush();
            return "";
        }

        if (!nonOpts.isEmpty())
        {
            String argString = Joiner.on(' ').join(nonOpts);

            // symbol? pattern?
            Symbol arg = agent.readIdentifierOrContextVariable(argString);
            if (arg != null || argString.charAt(0) == '(')
            {
                agent.getPrinter().startNewLine();
                wmp.setInternal(options.has(Options.internal));

                // these are ignored if pattern
                wmp.setDepth(depth);
                wmp.setTree(options.has(Options.tree));

                try
                {
                    wmp.print(agent, agent.getPrinter(), arg, argString);
                } catch (Exception e)
                {
                    throw new SoarException(e.toString());
                }

                agent.getPrinter().flush();
                return "";
            }

            // timetag?
            try
            {
                int tt = Integer.parseInt(argString);
                for (Wme wme : agent.getAllWmesInRete())
                {
                    if (wme.getTimetag() == tt)
                    {
                        agent.getPrinter().startNewLine();
                        agent.getPrinter().print(wme.toString());
                        agent.getPrinter().flush();
                        return "";
                    }
                }
                throw new SoarException("No wme " + tt + " in working memory.");
            } catch (NumberFormatException ignored)
            {
            }

            // production?
            // Default with arg is full print (productions)
            if (!options.has(Options.name))
                options.set(Options.full);

            agent.getPrinter().startNewLine();
            Production p = agent.getProductions().getProduction(argString);
            if (p != null)
                do_print_for_production(p);
            else
                agent.getPrinter().print("No production named " + argString);
            agent.getPrinter().flush();
            return "";
        }

        if (options.has(Options.all))
        {
            options.set(Options.chunks);
            options.set(Options.Defaults);
            options.set(Options.justifications);
            options.set(Options.user);
            options.set(Options.Template);
        }

        agent.getPrinter().startNewLine();
        for (Production p : collectProductions())
        {
            do_print_for_production(p);
        }

        if (options.has(Options.rl))
        {
            for (Production p : agent.getProductions().getProductions(null))
            {
                if (p.rl_rule)
                    do_print_for_production(p);
            }
        }
        agent.getPrinter().flush();
        return "";
    }

    private void do_print_for_production(Production prod)
    {
        final Printer p = agent.getPrinter();

        if (options.has(Options.Filename))
        {
            if (options.has(Options.full))
                p.print("# source file: ", prod.getLocation());

            p.print("%s", prod.getLocation());

            if (options.has(Options.full))
                p.print("\n");
            else
                p.print(": ");
        }

        if (options.has(Options.full))
            prod.print(p, options.has(Options.internal));
        else
        {
            p.print("%s ", prod.getName());

            if (prod.rl_rule)
            {
                p.print("%f  ", prod.rl_update_count);
                p.print("%s", prod.action_list.asMakeAction().referent);
            }
        }
        p.print("\n");
    }

}