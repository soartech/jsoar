/*
 * Copyright (c) 2009 Dave Ray <daveray@gmail.com>
 */
package org.jsoar.tcl;

import java.io.BufferedInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.util.Stack;

import org.jsoar.kernel.SoarException;
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
    // This ain't pretty, but it's private and it works
    private static class Entry
    {
        File file;
        URL url;
        
        public Entry(File file) { this.file = file; }
        public Entry(URL url) { this.url = url; }
    }
    
    private Entry workingDirectory = new Entry(new File(System.getProperty("user.dir")));
    private Stack<Entry> directoryStack = new Stack<Entry>();
    private Stack<String> fileStack = new Stack<String>();
    
    public SourceCommand()
    {
        fileStack.push("");
    }
    
    public String getWorkingDirectory()
    {
        return workingDirectory.url != null ? workingDirectory.url.toExternalForm() : workingDirectory.file.getAbsolutePath();
    }
    
    public String getCurrentFile()
    {
        return fileStack.peek();
    }
    
    public void pushd(Interp interp, String dirString) throws TclException
    {
        File newDir = new File(dirString);
        URL url = FileTools.asUrl(dirString);
        if(url != null)
        {
            // A new URL. Just set that to be the working directory
            directoryStack.push(workingDirectory);
            workingDirectory = new Entry(url);
        }
        else if(workingDirectory.url != null && !newDir.isAbsolute())
        {
            // Relative path where current directory is a URL.
            directoryStack.push(workingDirectory);
            workingDirectory = new Entry(joinUrl(workingDirectory.url, dirString));
        }
        else 
        {
            if(!newDir.isAbsolute())
            {
                assert workingDirectory.url == null;
                newDir = new File(workingDirectory.file, dirString);
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
            workingDirectory = new Entry(newDir);
        }
    }
    
    public void popd(Interp interp) throws TclException
    {
        if(directoryStack.isEmpty())
        {
            throw new TclException(interp, "Directory stack is empty");
        }
        workingDirectory = directoryStack.pop();
    }

    public void source(Interp interp, String fileString) throws TclException, SoarException
    {
        final URL url = FileTools.asUrl(fileString);
        File file = new File(fileString);
        if(url != null)
        {
            pushd(interp, getParentUrl(url).toExternalForm());
            evalUrlAndPop(interp, url);
        }
        else if(file.isAbsolute())
        {
            pushd(interp, file.getParent());
            evalFileAndPop(interp, file);
        }
        else if(workingDirectory.url != null)
        {
            final URL childUrl = joinUrl(workingDirectory.url, fileString);
            pushd(interp, getParentUrl(childUrl).toExternalForm());
            evalUrlAndPop(interp, childUrl);
        }
        else 
        {
            file = new File(workingDirectory.file, file.getPath());
            pushd(interp, file.getParent());
            evalFileAndPop(interp, file);
        }
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
        
        try
        {
            source(interp, args[1].toString());
        }
        catch (SoarException e)
        {
            throw new TclException(interp, e.getMessage());
        }
    }
    
    private URL getParentUrl(URL url) throws SoarException
    {
        final String s = url.toExternalForm();
        final int i = s.lastIndexOf('/');
        if(i == -1)
        {
            throw new SoarException("Cannot determine parent of URL: " + url);
        }
        return FileTools.asUrl(s.substring(0, i));
    }
    
    private URL joinUrl(URL parent, String child)
    {
        final String s = parent.toExternalForm();
        return FileTools.asUrl(s.endsWith("/") ? s + child : s + "/" + child);
    }
    
    private void evalFileAndPop(Interp interp, File file) throws TclException
    {
        try
        {
            final String fileString = file.getAbsolutePath();
            fileStack.push(fileString);
            interp.evalFile(fileString);
            interp.cleanupTokens();
        }
        finally
        {
            fileStack.pop();
            popd(interp);
        }
    }
    
    private void evalUrlAndPop(Interp interp, URL url) throws TclException
    {
        try
        {
            fileStack.push(url.toExternalForm());
            final InputStream in = new BufferedInputStream(url.openStream());
            try
            {
                final ByteArrayOutputStream out = new ByteArrayOutputStream();
                FileTools.copy(in, out);
                interp.eval(out.toString());
            }
            finally
            {
                in.close();
            }
        }
        catch (IOException e)
        {
            throw new TclException(interp, e.getMessage());
        }
        finally
        {
            fileStack.pop();
            popd(interp);
        }
    }
}
