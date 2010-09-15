/*
 * Copyright (c) 2009 Dave Ray <daveray@gmail.com>
 */
package org.jsoar.kernel.commands;

import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

import org.jsoar.kernel.Agent;
import org.jsoar.kernel.Production;
import org.jsoar.kernel.ProductionManager;
import org.jsoar.kernel.ProductionType;
import org.jsoar.kernel.SoarException;
import org.jsoar.kernel.learning.rl.ReinforcementLearning;
import org.jsoar.util.adaptables.Adaptables;
import org.jsoar.util.commands.OptionProcessor;
import org.jsoar.util.commands.SoarCommand;
import org.jsoar.util.commands.SoarCommandContext;

import com.google.common.collect.Lists;

/**
 * Implementation of the "excise" command.
 * 
 * @author ray
 */
public final class ExciseCommand implements SoarCommand
{
    private final OptionProcessor<Options> options = OptionProcessor.create();
    
    private enum Options
    {
        all, chunks, Default, rl, task, Templates, user,
    }
    
    private final Agent agent;

    public ExciseCommand(Agent agent)
    {
        this.agent = agent;
        
        options
        .newOption(Options.all)
        .newOption(Options.chunks)
        .newOption(Options.Default).shortOption('d')
        .newOption(Options.rl)
        .newOption(Options.task)
        .newOption(Options.Templates)
        .newOption(Options.user)
        .done();
    }

    @Override
    public String execute(SoarCommandContext commandContext, String[] args) throws SoarException
    {
        List<String> nonOpts = options.process(Lists.newArrayList(args));

        final Set<Production> toExcise = new LinkedHashSet<Production>();
        final ProductionManager pm = agent.getProductions();
        boolean doInit = false;
        boolean rl = false;

        if (options.has(Options.all))
        {
            toExcise.addAll(pm.getProductions(null));
            doInit = true;
        }
        
        if (options.has(Options.chunks))
        {
            toExcise.addAll(pm.getProductions(ProductionType.CHUNK));
        }
        
        if (options.has(Options.Default))
        {
            toExcise.addAll(pm.getProductions(ProductionType.DEFAULT));
        }
        
        if (options.has(Options.rl))
        {
            // cli_excise.cpp:DoExcise
            toExcise.addAll(pm.getProductions(ProductionType.DEFAULT));
            toExcise.addAll(pm.getProductions(ProductionType.USER));
            toExcise.addAll(pm.getProductions(ProductionType.CHUNK));
            rl = true;
        }
        
        if (options.has(Options.task))
        {
            toExcise.addAll(pm.getProductions(ProductionType.CHUNK));
            toExcise.addAll(pm.getProductions(ProductionType.JUSTIFICATION));
            toExcise.addAll(pm.getProductions(ProductionType.USER));
        }
            
        if (options.has(Options.Templates))
        {
            toExcise.addAll(pm.getProductions(ProductionType.TEMPLATE));
        }
        
        if (options.has(Options.user))
        {
            toExcise.addAll(pm.getProductions(ProductionType.USER));
        }
        
        for (String arg : nonOpts)
        {
            final Production p = pm.getProduction(arg);
            if(p == null)
            {
                throw new SoarException("No production named '" + arg + "'");
            }
            toExcise.add(p);
        }
            
        for(Production p : toExcise)
        {
            if(!rl ||  (rl && p.rlRuleInfo != null))
            {
                pm.exciseProduction(p, false);
            }
        }
        
        if(rl)
        {
            // cli_excise.cpp:DoExcise
            Adaptables.adapt(agent, ReinforcementLearning.class).rl_initialize_template_tracking();
        }
        
        // If -a is given, we do an init-soar as well
        if(doInit)
        {
            agent.initialize();
        }
        
        return String.format("\n%d production%s excised.", toExcise.size(), toExcise.size() == 1 ? "" : "s");
    }
}
