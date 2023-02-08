/*
 * Copyright (c) 2010 Dave Ray <daveray@gmail.com>
 *
 * Created on Aug 21, 2010
 */
package org.jsoar.script;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;

import javax.script.Invocable;
import javax.script.ScriptEngine;
import javax.script.ScriptException;

import org.jsoar.kernel.SoarException;
import org.jsoar.kernel.rhs.functions.RhsFunctionManager;
import org.jsoar.util.adaptables.Adaptable;
import org.jsoar.util.adaptables.Adaptables;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.base.Charsets;

/**
 * Wrapper around a ScriptEngine.
 * 
 * @author ray
 */
public class ScriptEngineState
{
    private static final Logger logger = LoggerFactory.getLogger(ScriptEngineState.class);
    
    private final Adaptable context;
    private final String engineName;
    private final ScriptEngine engine;

    public ScriptEngineState(Adaptable context, String engineName, ScriptEngine engine) throws SoarException
    {
        this.context = context;
        this.engineName = engineName;
        this.engine = engine;
        
        initializeGlobalScope();
        
        installRhsFunction();
    }


    public ScriptEngine getEngine()
    {
        return engine;
    }
    
    /**
     * Evaluate the given string in the scripting engine.
     * 
     * @param script the script string
     * @return the result. Type depends on scripting engine and script.
     * @throws SoarException
     */
    public Object eval(String script) throws SoarException
    {
        try
        {
            return engine.eval(script);
        }
        catch (ScriptException e)
        {
            e.printStackTrace();
            throw new SoarException("Error executing script: " + e.getMessage(), e);
        }
    }

    /**
     * Dispose the engine, cleaning up any hooks that have been added to the
     * agent.
     */
    public void dispose() throws SoarException
    {
        uninstallRhsFunction();
        
        invokeDisposeMethod();
    }

    private void invokeDisposeMethod() throws SoarException
    {
        if(!(engine instanceof Invocable))
        {
            return;
        }
        
        final Invocable invocable = (Invocable) engine;
        try
        {
            invocable.invokeFunction("soar_dispose");
        }
        catch (ScriptException e)
        {
            logger.error(engineName + ": Error calling soar_dispose: " + e.getMessage(), e);
            throw new SoarException("Error executing script: " + e.getMessage(), e);
        }
        catch (NoSuchMethodException e)
        {
            // Fall back to just doing an eval...
            try
            {
                engine.eval("soar_dispose()");
            }
            catch (ScriptException die)
            {
                logger.error(engineName + ": soar_dispose method not defined. " + die.getMessage());
            }
        }
    }
    
    private void initializeGlobalScope() throws SoarException
    {
        final InputStream is = getClass().getResourceAsStream(engineName.toLowerCase());
        if(is != null)
        {
            engine.put("_soar", new ScriptContext(context));
            
            engine.put(ScriptEngine.FILENAME, "/org/jsoar/script/" + engineName);
            
            try(final BufferedReader reader = new BufferedReader(new InputStreamReader(is, Charsets.UTF_8)))
            {
                engine.eval(reader);
            }
            catch (ScriptException e)
            {
                throw new SoarException(e.getMessage(), e);
            }
            catch (IOException e)
            {
                throw new SoarException("While initializing '" + engineName + "' engine: " + e.getMessage(), e);
            }
            finally
            {
                engine.put(ScriptEngine.FILENAME, null);
            }
        }
        else
        {
            engine.put("soar", new ScriptContext(context));
        }
    }

    private void installRhsFunction()
    {
        final RhsFunctionManager rhsFunctions = Adaptables.adapt(context, RhsFunctionManager.class);
        if(rhsFunctions != null)
        {
            ScriptRhsFunction rhsFunction = new ScriptRhsFunction(engineName, this);
            rhsFunctions.registerHandler(rhsFunction);
        }
    }
    
    private void uninstallRhsFunction()
    {
        final RhsFunctionManager rhsFunctions = Adaptables.adapt(context, RhsFunctionManager.class);
        if(rhsFunctions != null)
        {
            rhsFunctions.unregisterHandler(engineName);
        }
    }
    
    /* (non-Javadoc)
     * @see java.lang.Object#toString()
     */
    public String toString()
    {
        return engineName;
    }
}
