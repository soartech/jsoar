/*
 * Copyright (c) 2010 Dave Ray <daveray@gmail.com>
 *
 * Created on Sep 2, 2010
 */
package org.jsoar.kernel.commands;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import org.jsoar.kernel.Production;
import org.jsoar.kernel.ProductionManager;
import org.jsoar.kernel.SoarException;
import org.jsoar.util.commands.OptionProcessor;
import org.jsoar.util.commands.SoarCommand;
import org.jsoar.util.commands.SoarCommandContext;

import com.google.common.base.Joiner;

/**
 * Implementation of the "pwatch" command.
 * 
 * <p>See http://code.google.com/p/soar/wiki/CommandLineInterface#pwatch
 *  
 * @author ray
 */
public class PWatchCommand implements SoarCommand
{
    private enum Options {
        on,
        off,
        enable,
        disable;
    };
    
    private final ProductionManager pm;
    private final OptionProcessor<Options> options = new OptionProcessor<Options>();
    
    public PWatchCommand(ProductionManager pm)
    {
        this.pm = pm;
        options.newOption(Options.on).
                newOption(Options.off).shortOption('f').
                newOption(Options.enable).
                newOption(Options.disable).
                done();
    }

    /* (non-Javadoc)
     * @see org.jsoar.util.commands.SoarCommand#execute(org.jsoar.util.commands.SoarCommandContext, java.lang.String[])
     */
    @Override
    public String execute(SoarCommandContext context, String[] args)
            throws SoarException
    {
        final List<String> rest = options.process(Arrays.asList(args));
        if(rest.isEmpty())
        {
            return Joiner.on('\n').join(collectAndSortTracedRuleNames());
        }
        else
        {
            final boolean on = !(options.has(Options.off) || options.has(Options.disable));
            for(String name : rest)
            {
                final Production p = pm.getProduction(name);
                if(p != null)
                {
                    p.setTraceFirings(on);
                }
                else
                {
                    throw new SoarException("No production named '" + name + "'");
                }
            }
            return "";
        }
    }

    private List<String> collectAndSortTracedRuleNames()
    {
        final List<String> result = new ArrayList<String>();
        for(Production p : pm.getProductions(null))
        {
            if(p.isTraceFirings())
            {
                result.add(p.getName());
            }
        }
        Collections.sort(result);
        return result;
    }
}
