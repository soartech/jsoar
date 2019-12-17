/*
 * Copyright (c) 2009 Dave Ray <daveray@gmail.com>
 *
 * Created on Jun 6, 2009
 */
package org.jsoar.util.commands;

import org.jsoar.kernel.SoarException;

/**
 * Interface for implementing a Soar command.
 * 
 * @author ray
 * @see SoarCommandInterpreter
 * @see SoarCommands
 */
public interface SoarCommand
{
    /**
     * Execute the command with the given arguments. Note that the name of the
     * command is the first argument, so {@code args.length} will always be
     * at least 1.
     * 
     * @param context the command execution context
     * @param args commands arguments. {@code args[0]} is the name of the command.
     *  {@code args.length} will always be at least 1.
     * @return the result of the command
     * @throws SoarException
     */
    String execute(SoarCommandContext context, String[] args) throws SoarException;

    /**
     * For the new picocli commands, return the annotated command Object, which will be used for autocomplete processing
     * @return picocli annotated Object
     */
    Object getCommand();
}
