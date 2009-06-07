/*
 * Copyright (c) 2009 Dave Ray <daveray@gmail.com>
 */
package org.jsoar.tcl;

import java.io.BufferedInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.Stack;

import org.jsoar.util.FileTools;

import tcl.lang.Command;
import tcl.lang.Interp;
import tcl.lang.TclException;
import tcl.lang.TclNumArgsException;
import tcl.lang.TclObject;

/**
 * @author ray
 */
public class SourceCommand implements Command
{
    private File workingDirectory = new File(System.getProperty("user.dir"));
    private Stack<File> directoryStack = new Stack<File>();
    
    /**
     * @param interp
     */
    public SourceCommand()
    {
    }
    
    public File getWorkingDirectory()
    {
        return workingDirectory;
    }
    
    public void pushd(Interp interp, String dirString) throws TclException
    {
        File newDir = new File(dirString);
        if(!newDir.isAbsolute())
        {
            newDir = new File(workingDirectory, dirString);
        }
        
        if(!newDir.exists())
        {
            throw new TclException(interp, "Directory '" + newDir  + "' does not exist");
        }
        if(!newDir.isDirectory())
        {
            throw new TclException(interp, "'" + newDir + "' is not a directory");
        }
        directoryStack.push(workingDirectory);
        workingDirectory = newDir;
    }
    
    public void popd(Interp interp) throws TclException
    {
        if(directoryStack.isEmpty())
        {
            throw new TclException(interp, "Directory stack is empty");
        }
        workingDirectory = directoryStack.pop();
    }

    /* (non-Javadoc)
     * @see tcl.lang.Command#cmdProc(tcl.lang.Interp, tcl.lang.TclObject[])
     */
    public void cmdProc(Interp interp, TclObject[] args) throws TclException
    {
        if(args.length != 2)
        {
            throw new TclNumArgsException(interp, 0, args, "fileName");
        }
        
        final String fileName = args[1].toString();
        
        // First see if it's a URL
        if(tryUrl(interp, fileName))
        {
            return;
        }
        
        // Fallback to file system
        source(interp, fileName);
    }

    /**
     * @param interp
     * @param fileName
     * @throws TclException
     */
    public void source(Interp interp, String fileName) throws TclException
    {
        File file = new File(fileName);
        if(!file.isAbsolute())
        {
            file = new File(workingDirectory, fileName);
        }
        final String parent = file.getParent();
        pushd(interp, parent);
        try
        {
            interp.evalFile(file.getAbsolutePath());
        }
        finally
        {
            popd(interp);
        }
    }
    
    private boolean tryUrl(Interp interp, String fileName) throws TclException 
    {
        try
        {
            final URL url = new URL(fileName);
            
            // TODO: This should also handle relative URL paths as well as proxies, authentication, etc
            final InputStream in = new BufferedInputStream(url.openStream());
            try
            {
                final ByteArrayOutputStream out = new ByteArrayOutputStream();
                FileTools.copy(in, out);
                interp.eval(out.toString());
                return true;
            }
            finally
            {
                in.close();
            }
        }
        catch (MalformedURLException e)
        {
            return false; // Not a URL
        }
        catch (IOException e)
        {
            throw new TclException(interp, e.getMessage());
        }
    }
}
