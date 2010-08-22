/*
 * Copyright (c) 2010 Dave Ray <daveray@gmail.com>
 *
 * Created on Aug 21, 2010
 */
package org.jsoar.script;

import org.jsoar.util.adaptables.Adaptable;

/**
 * A bean object, registered as a global, "soar", in the script engine.
 * Providers access to the agent, current working directory, etc.
 * 
 * @author ray
 */
public class ScriptContext
{
    private final Adaptable agent;
    
    public ScriptContext(Adaptable agent)
    {
        this.agent = agent;
    }

    public Adaptable getAgent()
    {
        return agent;
    }
}
