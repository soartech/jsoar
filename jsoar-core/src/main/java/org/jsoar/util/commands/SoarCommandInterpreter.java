/*
 * Copyright (c) 2009 Dave Ray <daveray@gmail.com>
 *
 * Created on Jun 6, 2009
 */
package org.jsoar.util.commands;

/**
 * Interface for an object that knows how to execute Soar commands and
 * source Soar files. This interface only implements the command parser
 * and interpreter. The actual implementation of the commands is left
 * to instances of {@link SoarCommand}
 * 
 * @author ray
 */
public interface SoarCommandInterpreter
{
    /**
     * Add a new command to this interpreter
     * 
     * @param name the name of the command
     * @param handler the handler for the command
     */
    void addCommand(String name, SoarCommand handler);
    
}
