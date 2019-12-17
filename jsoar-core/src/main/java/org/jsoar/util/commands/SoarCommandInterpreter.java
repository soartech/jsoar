/*
 * Copyright (c) 2009 Dave Ray <daveray@gmail.com>
 *
 * Created on Jun 6, 2009
 */
package org.jsoar.util.commands;

import java.io.File;
import java.net.URL;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;

import org.jsoar.kernel.Agent;
import org.jsoar.kernel.SoarException;
import org.jsoar.util.SourceLocation;

import picocli.CommandLine;

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
     * Fetches a command associated with this interpreter.
     * 
     * @param name the name of a command added with {@link #addCommand(String, SoarCommand)};
     * @throws SoarException 
     */
    SoarCommand getCommand(String name, SourceLocation srcLoc) throws SoarException;
    
    /**
     * Gets the parsed command for a string, after alias resolution
     * @param name
     * @param srcLoc
     * @return
     */
    ParsedCommand getParsedCommand(String name, SourceLocation srcLoc);
    
    /**
     * Return a list of files that have been sourced by this object.
     * 
     * This is used by the Soar IDE.
     * 
     * @return an absolute path to each sourced file
     */
    Collection<String> getSourcedFiles();
    
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
     * @param file any the file to load a rete from
     * @throws SoarException
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
     */
    void loadRete(URL url) throws SoarException;
    
    /**
     * Saves the current rete network of an agent to the given file.
     */
    void saveRete(File file) throws SoarException;
    
    /**
     * Returns the current working directory of the interpreter (or null if not applicable).
     */
    String getWorkingDirectory();

    /**
     * Get the autocomplete list for an incomplete command or a command with no CommandLine
     * @param command
     * @return
     */
    String[] getCompletionList(String command, int cursorPosition);

    /**
     * Return the autocomplete list for a command with a command line
     * @param command
     * @return
     */
    default CommandLine findCommand(String command)
    {
        command = command.trim();
        if (!command.isEmpty()) {
            String[] parts = command.split(" ");
            ParsedCommand parsedCommand = null;
            // this will expand aliases if needed
            parsedCommand = getParsedCommand(parts[0], null);
            // then add the remaining arguments from the original string
            if(parts.length > 1) {
                parsedCommand.getArgs().addAll(Arrays.asList(parts).subList(1, parts.length));
            }
            
            SoarCommand cmd;
            try
            {
                cmd = getCommand(parsedCommand.getArgs().get(0), null);
            }
            catch (SoarException e)
            {
                // an exception here means the command isn't valid
                return null;
            }
            if (cmd != null && cmd.getCommand() != null) {
                CommandLine commandLine = new CommandLine(cmd.getCommand());
                int part = 0;
                List<String> args = parsedCommand.getArgs().subList(1, parsedCommand.getArgs().size());
                
                while (part < args.size() && commandLine.getSubcommands().containsKey(args.get(part))) {
                    commandLine = commandLine.getSubcommands().get(args.get(part));
                }
                return commandLine;
            }
        }
        return null;
    }
    
    /**
     * Returns a sorted list of the names of all the registered SoarCommands
     * This is not intended to report aliases or interpreter-specific commands (e.g., Tcl commands)
     * @return
     */
    public List<String> getCommandStrings() throws SoarException;

    /**
     * Returns the interpreter's SoarTclExceptionsManager
     * This is intended to be used by the language server to provide context/info
     * for "soft" exceptions that are caught and logged but not "reported" to IDEs
     * @return
     */
    public SoarExceptionsManager getExceptionsManager();

}
