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

import javax.script.ScriptEngine;
import javax.script.ScriptException;

import org.jsoar.kernel.SoarException;
import org.jsoar.kernel.rhs.functions.RhsFunctionManager;
import org.jsoar.util.adaptables.Adaptable;
import org.jsoar.util.adaptables.Adaptables;

import com.google.common.base.Charsets;

/**
 * Wrapper around a ScriptEngine.
 * 
 * @author ray
 */
public class ScriptEngineState
{
    private final String engineName;
    private final ScriptEngine engine;

    public ScriptEngineState(Adaptable context, String engineName, ScriptEngine engine) throws SoarException
    {
        this.engineName = engineName;
        this.engine = engine;
        
        initializeGlobalScope(engineName, context, engine);
        
        installRhsFunction(engineName, context);
    }

    private void initializeGlobalScope(String engineName, Adaptable context, ScriptEngine engine) throws SoarException
    {
        final InputStream is = getClass().getResourceAsStream(engineName);
        if(is != null)
        {
            engine.put("_soar", new ScriptContext(context));
            
            engine.put(ScriptEngine.FILENAME, "/org/jsoar/script/" + engineName);
            final BufferedReader reader = new BufferedReader(new InputStreamReader(is, Charsets.UTF_8));
            try
            {
                try
                {
                    engine.eval(reader);
                }
                catch (ScriptException e)
                {
                    throw new SoarException(e.getMessage(), e);
                }
            }
            finally
            {
                engine.put(ScriptEngine.FILENAME, null);

                try
                {
                    reader.close();
                }
                catch (IOException e)
                {
                    throw new SoarException("While initializing '" + engineName + "' engine: " + e.getMessage(), e);
                }
            }
        }
        else
        {
            engine.put("soar", new ScriptContext(context));
        }
    }

    private void installRhsFunction(String name, Adaptable context)
    {
        final RhsFunctionManager rhsFunctions = Adaptables.adapt(context, RhsFunctionManager.class);
        if(rhsFunctions != null)
        {
            ScriptRhsFunction rhsFunction = new ScriptRhsFunction(name, this);
            rhsFunctions.registerHandler(rhsFunction);
        }
    }

    public ScriptEngine getEngine()
    {
        return engine;
    }
    
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
    
    public String toString()
    {
        return engineName;
    }
}
