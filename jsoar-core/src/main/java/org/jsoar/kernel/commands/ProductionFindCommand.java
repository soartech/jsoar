/*
 * Copyright (c) 2009 Dave Ray <daveray@gmail.com>
 */
package org.jsoar.kernel.commands;

import java.util.Arrays;
import java.util.Collection;
import java.util.List;

import org.jsoar.kernel.Agent;
import org.jsoar.kernel.Production;
import org.jsoar.kernel.ProductionFinder;
import org.jsoar.kernel.ProductionType;
import org.jsoar.kernel.SoarException;
import org.jsoar.kernel.ProductionFinder.Options;
import org.jsoar.kernel.parser.ParserException;
import org.jsoar.kernel.tracing.Printer;
import org.jsoar.util.StringTools;
import org.jsoar.util.commands.SoarCommand;
import org.jsoar.util.commands.SoarCommandContext;

import com.google.common.base.Predicate;
import com.google.common.collect.Collections2;

/**
 * http://winter.eecs.umich.edu/soarwiki/Production-find
 * 
 * @author ray
 */
public final class ProductionFindCommand implements SoarCommand
{
    private final Agent agent;

    public ProductionFindCommand(Agent agent)
    {
        this.agent = agent;
    }

    @Override
    public String execute(SoarCommandContext commandContext, String[] args) throws SoarException
    {
        final ProductionFinder finder = new ProductionFinder(agent);
        finder.options().clear();
        
        boolean rhs = false;
        boolean chunks = false;
        boolean nochunks = false;
        int i = 1;
        for(; i < args.length; ++i)
        {
            final String arg = args[i];
            if("-l".equals(arg) || "--lhs".equals(arg))
            {
                finder.options().add(Options.LHS);
            }
            else if("-r".equals(arg) || "--rhs".equals(arg))
            {
                finder.options().add(Options.RHS);
                rhs = true;
            }
            else if("-c".equals(arg) || "--chunks".equals(arg))
            {
                chunks = true;
            }
            else if("-n".equals(arg) || "--nochunks".equals(arg))
            {
                nochunks = true;
            }
            else if("-s".equals(arg) || "--show-bindings".equals(arg))
            {
                agent.getPrinter().warn("%s option not yet supported", arg);
            }
            else if(arg.startsWith("-"))
            {
                throw new SoarException("Unknown option " + arg);
            }
            else
            {
                break;
            }
        }

        if(i >= args.length)
        {
            throw new SoarException("Missing pattern argument");
        }
        
        // LHS is default
        if(!rhs)
        {
            finder.options().add(Options.LHS);
        }
        
        final String pattern = StringTools.join(Arrays.asList(Arrays.copyOfRange(args, i, args.length)), " ");
        final Collection<Production> productions = collectProductions(chunks, nochunks);
        try
        {
            List<Production> result = finder.find(pattern, productions);
            printResults(pattern, result);
        }
        catch (ParserException e)
        {
            throw new SoarException(e.getMessage());
        }
        
        return "";
    }

    private void printResults(final String pattern, List<Production> result)
    {
        final Printer printer = agent.getPrinter();
        printer.startNewLine();
        if(result.isEmpty())
        {
            printer.print("No productions match '%s'\n", pattern);
        }
        else
        {
            for(Production p : result)
            {
                printer.print("%s\n", p.getName());
            }
        }
    }

    private Collection<Production> collectProductions(final boolean chunks, final boolean nochunks)
    {
        final Predicate<Production> filter = new Predicate<Production>() {

            @Override
            public boolean apply(Production p)
            {
                final ProductionType type = p.getType();
                return (type == ProductionType.CHUNK && chunks) ||
                       (type != ProductionType.CHUNK && nochunks) ||
                       (!chunks && !nochunks);
            }};
            
        final Collection<Production> productions = Collections2.filter(agent.getProductions().getProductions(null), filter);
        return productions;
    }
}