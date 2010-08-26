/*
 * Copyright (c) 2010 Dave Ray <daveray@gmail.com>
 *
 * Created on Aug 21, 2010
 */
package org.jsoar.script;

import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.script.ScriptEngine;
import javax.script.ScriptEngineFactory;
import javax.script.ScriptEngineManager;

import org.jsoar.kernel.SoarException;
import org.jsoar.util.adaptables.Adaptable;
import org.jsoar.util.commands.OptionProcessor;
import org.jsoar.util.commands.SoarCommand;
import org.jsoar.util.commands.SoarCommandContext;
import org.jsoar.util.commands.SoarCommandInterpreter;
import org.jsoar.util.commands.SoarCommandProvider;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.base.Joiner;

/**
 * @author ray
 */
public class ScriptCommand implements SoarCommand
{
    private static final Logger logger = LoggerFactory.getLogger(ScriptCommand.class);
    
    private static enum GlobalOptions { reset, dispose; }
    private static enum EngineOptions {  }
    
    private final OptionProcessor<GlobalOptions> globalOptions = OptionProcessor.create();
    private final OptionProcessor<EngineOptions> engineOptions = OptionProcessor.create();
    private final Adaptable context;
    private ScriptEngineManager manager;
    private final Map<String, ScriptEngineState> engines = new HashMap<String, ScriptEngineState>();
    
    /**
     * Command provider for this command. Register in META-INF/services/org.jsoar.util.commands.SoarCommandProvider
     * 
     * @author ray
     */
    public static class Provider implements SoarCommandProvider
    {
        /* (non-Javadoc)
         * @see org.jsoar.util.commands.SoarCommandProvider#registerCommands(org.jsoar.util.commands.SoarCommandInterpreter)
         */
        @Override
        public void registerCommands(SoarCommandInterpreter interp, Adaptable context)
        {
            interp.addCommand("script", new ScriptCommand(context));
        }
    }
    
    /**
     * Constructed by command provider.
     * 
     * @param context the context, e.g. agent
     */
    ScriptCommand(Adaptable context)
    {
        this.context = context;
        
        globalOptions.newOption(GlobalOptions.dispose)
                     .newOption(GlobalOptions.reset)
                     .done();
                    
    }
    
    /* (non-Javadoc)
     * @see org.jsoar.util.commands.SoarCommand#execute(java.lang.String[])
     */
    @Override
    public String execute(SoarCommandContext commandContext, String[] args) throws SoarException
    {
        if(args.length == 1)
        {
            return engines.toString();
        }
        
        // Strip off script and treat the engine type as the command name...
        // i.e. "script javascript ..." becomes "javascript ..."
        final List<String> engineArgs = globalOptions.process(Arrays.asList(args));
        final String engineName = engineArgs.get(0);
        if(globalOptions.has(GlobalOptions.dispose))
        {
            return disposeEngine(engineName);
        }
        
        final List<String> trailing = engineOptions.process(engineArgs);
        final ScriptEngineState state = getEngineByName(engineName, globalOptions.has(GlobalOptions.reset));
        if(!trailing.isEmpty())
        {
            final Object result = state.eval(Joiner.on(' ').join(trailing));
            return result != null ? result.toString() : "";
        }
        else
        {
            return state.toString();
        }
    }
    
    private synchronized ScriptEngineManager getEngineManager()
    {
        if(manager == null)
        {
            manager = new ScriptEngineManager();
        }
        return manager;
    }
    
    private synchronized String disposeEngine(String name) throws SoarException
    {
        final ScriptEngineState state = engines.remove(name);
        if(state != null)
        {
            state.dispose();
            return "Disposed '" + name + "'";
        }
        else
        {
            return "No engine '" + name + "'";
        }
    }


    private synchronized ScriptEngineState getEngineByName(String name, boolean reset) throws SoarException
    {
        ScriptEngineState state = engines.get(name);
        if(state != null && reset)
        {
            state.dispose();
            engines.remove(name);
            state = null;
        }
        
        if(state == null)
        {
            final ScriptEngine engine = getEngineManager().getEngineByName(name);
            if(engine == null)
            {
                throw new SoarException("Unsupported script engine '" + name + "'");
            }
            
            final ScriptEngineFactory f = engine.getFactory();
            logger.info(String.format("Loaded '%s' script engine for %s: %s version %s, %s version %s", 
                    name,
                    context, 
                    f.getEngineName(), f.getEngineVersion(), 
                    f.getLanguageName(), f.getLanguageVersion()));
            
            engines.put(name, state = new ScriptEngineState(context, name, engine));
        }
        return state;
    }
}
