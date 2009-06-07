/*
 * Copyright (c) 2009 Dave Ray <daveray@gmail.com>
 *
 * Created on Jun 6, 2009
 */
package org.jsoar.util.commands;

import org.jsoar.kernel.SoarException;

/**
 * Interface for implementing Soar commands.
 * 
 * @author ray
 */
public interface SoarCommand
{
    /**
     * Execute the command with the given arguments. Note that the name of the
     * command is the first argument, so {@code args.length} will always be
     * at least 1.
     * 
     * @param args commands arguments. {@code args[0]} is the name of the command.
     *  {@code args.length} will always be at least 1.
     * @return the result of the command
     * @throws SoarException
     */
    String execute(String[] args) throws SoarException;
}
