/*
 * Copyright (c) 2009 Dave Ray <daveray@gmail.com>
 *
 * Created on Jun 7, 2009
 */
package org.jsoar.util.commands;

import java.io.File;
import java.net.URL;

import org.jsoar.kernel.SoarException;
import org.jsoar.util.FileTools;

/**
 * Utility methods for Soar commands and the Soar interpreter
 * 
 * @author ray
 */
public class SoarCommands
{
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
            interp.source((File) any);
        }
        else if(any instanceof URL)
        {
            interp.source((URL) any);
        }
        else
        {
            final String s = any.toString();
            final URL url = FileTools.asUrl(s);
            if(url != null)
            {
                interp.source(url);
            }
            else
            {
                interp.source(new File(s));
            }
        }
    }

}
