/*
 * Copyright (c) 2009 Dave Ray <daveray@gmail.com>
 *
 * Created on Jun 6, 2009
 */
package org.jsoar.util.commands;

import java.io.File;
import java.net.URL;

import org.jsoar.kernel.Agent;
import org.jsoar.kernel.SoarException;

/**
 * Interface for an object that knows how to execute Soar commands and
 * source Soar files. This interface only implements the command parser
 * and interpreter. The actual implementation of the commands is left
 * to instances of {@link SoarCommand}.
 * 
 * @author ray
 * @see SoarCommands
 */
public interface SoarCommandInterpreter
{
    /**
     * @return a name for this interpreter, e.g. "default", or "tcl"
     */
    String getName();
    
    /**
     * Dispose of this interpreter, detaching it from the agent. This method
     * is called when a new interpreter is added to the agent. It is also
     * called when {@link Agent#dispose()} is called.
     */
    void dispose();
    
    /**
     * Add a new command to this interpreter
     * 
     * @param name the name of the command
     * @param handler the handler for the command
     */
    void addCommand(String name, SoarCommand handler);
    
    /**
     * Evaluate a bit of code and return the result.
     * 
     * @param code the string of code to evaluate
     * @return the result
     * @throws SoarException if there was an error
     */
    String eval(String code) throws SoarException;
    
    /**
     * Source the Soar code at the given file. 
     * 
     * <p>If the file refers to a directory that is different from the current 
     * directory, then the current directory is first changed and then the file 
     * is sourced. If the file is relative it is assumed to be relative to the
     * interpreter's current directory. 
     * 
     * @param file the file to source
     * @throws SoarException
     * @see SoarCommands#source(SoarCommandInterpreter, Object)
     */
    void source(File file) throws SoarException;
    
    /**
     * Source the Soar code at the given URL.
     * 
     * <p>While the contents of the URL are being sourced, the interpreter's
     * current directory will be changed to that URL. If files are sourced
     * recursively, the interpreter will do its best to source them relative
     * to this URL, but this may not always make sense.
     * 
     * <p>Note that a URL for a Java resource may be obtained through
     * {@link Class#getResource(String)}.
     * 
     * @param url the URL.
     * @throws SoarException
     * @see SoarCommands#source(SoarCommandInterpreter, Object)
     */
    void source(URL url) throws SoarException;

    /**
     * Load the rete file at the given file.
     * 
     * <p>If the file refers to a directory that is different from the current 
     * directory, then the current directory is first changed and then the file 
     * is sourced. If the file is relative it is assumed to be relative to the
     * interpreter's current directory.
     * 
     * <p> Note that the working directory is not changed while loading the rete file
     * because additional files cannot be loaded during a rete deserialization.
     * 
     * @param any the file to load a rete from
     * @throws SoarException
     * @see SoarCommands#loadRete(SoarCommandInterpreter, Object)
     */
    void loadRete(File file) throws SoarException;
    
    /**
     * Load the rete file at the given URL.
     * 
     * <p> Note that the working directory is not changed while loading the rete file
     * because additional files cannot be loaded during a rete deserialization.
     * 
     * @param url the URL.
     * @throws SoarException
     * @see SoarCommands#loadRete(SoarCommandInterpreter, Object)
     */
    void loadRete(URL url) throws SoarException;
    
    /**
     * Saves the current rete network of an agent to the given file.
     */
    void saveRete(File file) throws SoarException;
}
