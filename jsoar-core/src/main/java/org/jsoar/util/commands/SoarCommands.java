/*
 * Copyright (c) 2009 Dave Ray <daveray@gmail.com>
 *
 * Created on Jun 7, 2009
 */
package org.jsoar.util.commands;

import java.io.File;
import java.net.URL;
import java.util.Iterator;
import java.util.ServiceLoader;

import org.jsoar.kernel.SoarException;
import org.jsoar.util.FileTools;
import org.jsoar.util.adaptables.Adaptable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Utility methods for Soar commands and the Soar interpreter
 * 
 * @author ray
 */
public class SoarCommands
{
    private static final Logger logger = LoggerFactory.getLogger(SoarCommands.class);
    
    /**
     * A more general form of the source method.
     * 
     * <p>The {@code any} parameter may be any of the following:
     * <ul>
     * <li>java.lang.String - first treated as a URL, then a File if that fails
     * <li>java.io.File
     * <li>java.net.URL
     * <li>Any other object - uses result of {@code Object.toString()}
     * </ul>
     * 
     * @param interp the command interpreter
     * @param any a reference to a location as described above
     * @throws SoarException
     */
    public static void source(SoarCommandInterpreter interp, Object any) throws SoarException
    {
        if(any instanceof File)
        {
            logger.debug("Loading as File: {}", any);
            interp.source((File) any);
        }
        else if(any instanceof URL)
        {
            logger.debug("Loading as URL: {}", any);
            interp.source((URL) any);
        }
        else
        {
            final String s = any.toString();
            final URL url = FileTools.asUrl(s);
            if(url != null)
            {
                logger.debug("Attempting load as URL: {}", url);
                interp.source(url);
            }
            else
            {
                logger.debug("Attempting load as File: {}", s);
                interp.source(new File(s));
            }
        }
    }
    
    public static void loadRete(SoarCommandInterpreter interp, Object any) throws SoarException
    {
        if(any instanceof File)
        {
            logger.debug("Loading as File: {}", any);
            interp.loadRete((File) any);
        }
        else if(any instanceof URL)
        {
            logger.debug("Loading as URL: {}", any);
            interp.loadRete((URL) any);
        }
        else
        {
            final String s = any.toString();
            final URL url = FileTools.asUrl(s);
            if(url != null)
            {
                logger.debug("Attempting load as URL: {}", url);
                interp.loadRete(url);
            }
            else
            {
                File file = new File(s);
                logger.debug("Attempting to load {} as File: {}, exists?: {}", s, file, file.exists());
                interp.loadRete(file);
            }
        }
    }
    
    /**
     * Register custom commands on the given interpreter by finding SoarCommandProvider
     * through the usual ServiceLoader mechanism.
     * 
     * @param interp the interpreter
     * @param context the context, e.g. Agent
     */
    public static void registerCustomCommands(SoarCommandInterpreter interp, Adaptable context)
    {
        final ServiceLoader<SoarCommandProvider> loader = ServiceLoader.load(SoarCommandProvider.class);
        for(Iterator<SoarCommandProvider> it = loader.iterator(); it.hasNext();)
        {
            final SoarCommandProvider provider = it.next();
            logger.info("Registering custom commands from " + provider.getClass());
            provider.registerCommands(interp, context);
        }
    }
    
}
