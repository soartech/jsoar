/*
 * Copyright (c) 2009 Dave Ray <daveray@gmail.com>
 */
package org.jsoar.kernel.commands;

import java.io.File;
import java.net.URL;
import java.util.Stack;

import org.jsoar.kernel.SoarException;
import org.jsoar.util.FileTools;
import org.jsoar.util.commands.SoarCommand;

/**
 * @author ray
 */
public class SourceCommand implements SoarCommand
{
    // This ain't pretty, but it's private and it works
    private static class Entry
    {
        File file;
        URL url;
        
        public Entry(File file) { this.file = file; }
        public Entry(URL url) { this.url = url; }
    }
    
    private final SourceCommandAdapter interp;
    private Entry workingDirectory = new Entry(new File(System.getProperty("user.dir")));
    private Stack<Entry> directoryStack = new Stack<Entry>();
    private Stack<String> fileStack = new Stack<String>();
    
    public SourceCommand(SourceCommandAdapter interp)
    {
        this.interp = interp;
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
    
    public void pushd(String dirString) throws SoarException
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
                throw new SoarException("Directory '" + newDir  + "' does not exist");
            }
            if(!newDir.isDirectory())
            {
                throw new SoarException("'" + newDir + "' is not a directory");
            }
            directoryStack.push(workingDirectory);
            workingDirectory = new Entry(newDir);
        }
    }
    
    public void popd() throws SoarException
    {
        if(directoryStack.isEmpty())
        {
            throw new SoarException("Directory stack is empty");
        }
        workingDirectory = directoryStack.pop();
    }

    public void source(String fileString) throws SoarException
    {
        final URL url = FileTools.asUrl(fileString);
        File file = new File(fileString);
        if(url != null)
        {
            pushd(getParentUrl(url).toExternalForm());
            evalUrlAndPop(url);
        }
        else if(file.isAbsolute())
        {
            pushd(file.getParent());
            evalFileAndPop(file);
        }
        else if(workingDirectory.url != null)
        {
            final URL childUrl = joinUrl(workingDirectory.url, fileString);
            pushd(getParentUrl(childUrl).toExternalForm());
            evalUrlAndPop(childUrl);
        }
        else 
        {
            file = new File(workingDirectory.file, file.getPath());
            pushd(file.getParent());
            evalFileAndPop(file);
        }
    }
    
    /* (non-Javadoc)
     * @see org.jsoar.util.commands.SoarCommand#execute(java.lang.String[])
     */
    @Override
    public String execute(String[] args) throws SoarException
    {
        if(args.length != 2)
        {
            throw new SoarException("Expected fileName argument");
        }
        
        source(args[1].toString());
        return "";
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
    
    private void evalFileAndPop(File file) throws SoarException
    {
        try
        {
            fileStack.push(file.getAbsolutePath());
            interp.eval(file);
        }
        finally
        {
            fileStack.pop();
            popd();
        }
    }
    
    private void evalUrlAndPop(URL url) throws SoarException
    {
        try
        {
            fileStack.push(url.toExternalForm());
            interp.eval(url);
        }
        finally
        {
            fileStack.pop();
            popd();
        }
    }
}
