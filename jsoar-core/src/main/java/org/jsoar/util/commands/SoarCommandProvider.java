/*
 * Copyright (c) 2010 Dave Ray <daveray@gmail.com>
 *
 * Created on Jul 24, 2010
 */
package org.jsoar.util.commands;

import org.jsoar.util.adaptables.Adaptable;


/**
 * Service loader interface for auto-registering custom commands. Implementers
 * of this interface can register it with the usual ServiceLoader mechanism, i.e.
 * a file in {@code META-INF/services/org.jsoar.util.commands.SoarCommandProvider} 
 *  
 * @author ray
 * @see SoarCommands
 */
public interface SoarCommandProvider
{
    /**
     * Register commands on the given interpreter using the given context.
     * 
     * @param interp the interpreter
     * @param context the context, e.g. Agent
     */
    void registerCommands(SoarCommandInterpreter interp, Adaptable context);
}
