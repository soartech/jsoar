/*
 * Copyright (c) 2009 Dave Ray <daveray@gmail.com>
 */
package org.jsoar.kernel.commands;

import android.content.res.AssetManager;

import com.google.common.io.ByteStreams;

import org.jsoar.kernel.Production;
import org.jsoar.kernel.SoarException;
import org.jsoar.kernel.events.ProductionAddedEvent;
import org.jsoar.kernel.events.ProductionExcisedEvent;
import org.jsoar.util.FileTools;
import org.jsoar.util.StringTools;
import org.jsoar.util.commands.SoarCommand;
import org.jsoar.util.commands.SoarCommandContext;
import org.jsoar.util.events.SoarEvent;
import org.jsoar.util.events.SoarEventListener;
import org.jsoar.util.events.SoarEventManager;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.net.MalformedURLException;
import java.net.URISyntaxException;
import java.net.URL;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.EnumSet;
import java.util.List;
import java.util.Stack;

/**
 * Implementation of the "source" command.
 *
 * <p> Manages the following:
 * <ul>
 * <li>The current working directory (pwd)
 * <li>The directory stack (pushd and popd)
 * <li>Stats about current top-level source command (last command, productions added, etc)
 * </ul>
 *
 * @author ray
 */
public class SourceCommand implements SoarCommand
{

    private static final Logger logger = LoggerFactory.getLogger(SourceCommand.class);

    private static enum Options { ALL, DISABLE, VERBOSE };

    private final SourceCommandAdapter interp;
    private DirStackEntry workingDirectory = new DirStackEntry(new File(System.getProperty("user.dir")));
    private Stack<String> fileStack = new Stack<String>();
    private Stack<DirStackEntry> directoryStack = new Stack<DirStackEntry>();

    /**
     * Save the path to each sourced file in this list.
     *
     * The Soar IDE uses this list to notify the user of un-sourced files.
     *
     * QUESTION: Should this also have urls added to it in evalUrlAndPop()?
     */
    private List<String> sourcedFiles = new ArrayList<String>();

    private final AssetManager assetManager;

    private TopLevelState topLevelState;
    private final SoarEventManager events;
    private final SoarEventListener eventListener = new SoarEventListener()
    {
        @Override
        public void onEvent(SoarEvent event)
        {
            if(event instanceof ProductionAddedEvent)
            {
                topLevelState.productionAdded(((ProductionAddedEvent) event).getProduction());
            }
            else if(event instanceof ProductionExcisedEvent)
            {
                topLevelState.productionExcised(((ProductionExcisedEvent) event).getProduction());
            }
        }
    };
    private String[] lastTopLevelCommand = null;
    //This method is for testing purposes
    public String[] getLastTopLevelCommand(){
        return Arrays.copyOf(lastTopLevelCommand, lastTopLevelCommand.length);
    }

    public SourceCommand(SourceCommandAdapter interp, SoarEventManager events, AssetManager assetManager)
    {
        this.interp = interp;
        this.events = events;
        fileStack.push("");
        this.assetManager = assetManager;
    }

    public String getWorkingDirectory()
    {
        return workingDirectory.url != null ? workingDirectory.url.toExternalForm() : workingDirectory.file.getAbsolutePath();
//        return getWorkingDirectoryPath();
    }

    /*package*/ DirStackEntry getWorkingDirectoryRaw()
    {
        return workingDirectory;
    }

    public String getCurrentFile()
    {
        return fileStack.peek();
    }

    public List<String> getSourcedFiles()
    {
        return sourcedFiles;
    }


    public void pushd(String dirString) throws SoarException
    {
        File newDir = new File(dirString);
        URL url = FileTools.asUrl(dirString);
        if(url != null)
        {
            // A new URL. Just set that to be the working directory
            directoryStack.push(workingDirectory);
            workingDirectory = new DirStackEntry(url);
        }
        else if(workingDirectory.url != null && !newDir.isAbsolute())
        {
            // Relative path where current directory is a URL.
            directoryStack.push(workingDirectory);
            workingDirectory = new DirStackEntry(joinUrl(workingDirectory.url, dirString));
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
            workingDirectory = new DirStackEntry(newDir);
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
        if(url != null)
        {
            pushd(getParentUrl(url).toExternalForm());
            evalUrlAndPop(url);
        }
        else if(workingDirectory.url != null)
        {
            final URL childUrl = joinUrl(workingDirectory.url, fileString);
            pushd(getParentUrl(childUrl).toExternalForm());
            evalUrlAndPop(childUrl);
        }
        else
        {
            File file = new File(fileString);
            file = new File(workingDirectory.file, file.getPath());
            pushd(file.getParent());
            evalFileAndPop(file.getAbsolutePath());
        }
    }

    /* (non-Javadoc)
     * @see org.jsoar.util.commands.SoarCommand#execute(java.lang.String[])
     */
    @Override
    public String execute(SoarCommandContext commandContext, String[] args) throws SoarException
    {
        if(args.length < 2)
        {
            throw new SoarException("Expected fileName argument");
        }

        final boolean topLevel = topLevelState == null;

        final boolean reload = "-r".equals(args[1]) || "--reload".equals(args[1]);
        if(topLevel && reload && lastTopLevelCommand != null)
        {
            return "Reloaded: " +
                   StringTools.join(Arrays.asList(lastTopLevelCommand), " ") + "\n" +
                   execute(commandContext, lastTopLevelCommand);
        }
        else if(!topLevel && reload)
        {
            throw new SoarException(args[1] + " option only valid at top level");
        }
        else if(lastTopLevelCommand == null && reload)
        {
            throw new SoarException("No previous file to reload");
        }

        // Process args to get list of files and options ...
        final List<String> files = new ArrayList<String>();
        final EnumSet<Options> opts = EnumSet.noneOf(Options.class);
        for(int i = 1; i < args.length; ++i)
        {
            final String arg = args[i];
            if(arg.equals("-d") || arg.equals("--disable")) opts.add(Options.DISABLE);
            else if(arg.equals("-a") || arg.equals("--all")) opts.add(Options.ALL);
            else if(arg.equals("-v") || arg.endsWith("--verbose")) opts.add(Options.VERBOSE);
            else if(arg.startsWith("-"))
            {
                throw new SoarException("Unknown option '" + arg + "'");
            }
            else
            {
                files.add(arg);
            }
        }

        // If this is the top source command (user-initiated), set up the 
        // state info and register for production events
        if(topLevel)
        {
            topLevelState = new TopLevelState();
            events.addListener(ProductionAddedEvent.class, eventListener);
            events.addListener(ProductionExcisedEvent.class, eventListener);
        }
        try
        {
            for(String file : files)
            {
                source(file);
            }
            if(topLevel)
            {
                lastTopLevelCommand = Arrays.copyOf(args, args.length);
            }
            return generateResult(topLevel, opts);
        }
        finally
        {
            // Clean up top-level state
            if(topLevel)
            {
                topLevelState = null;
                events.removeListener(null, eventListener);
            }
        }
    }

    private String generateResult(boolean topLevel, EnumSet<Options> opts)
    {
        final StringBuilder result = new StringBuilder();
        if(topLevel)
        {
            if(opts.contains(Options.ALL))
            {
                for(FileInfo file : topLevelState.files)
                {
                    result.append(String.format("%s: %d productions sourced.\n", file.name, file.productionsAdded.size()));
                    if(opts.contains(Options.VERBOSE) && !file.productionsExcised.isEmpty())
                    {
                        result.append("Excised productions:\n");
                        for(String p : file.productionsExcised)
                        {
                            result.append("        " + p + "\n");
                        }
                    }
                }
            }
            if(!opts.contains(Options.DISABLE))
            {
                result.append(String.format("Total: %d productions sourced. %d productions excised.\n",
                                topLevelState.totalProductionsAdded,
                                topLevelState.totalProductionsExcised));
            }
            if(opts.contains(Options.VERBOSE) && !opts.contains(Options.ALL) && topLevelState.totalProductionsExcised != 0)
            {
                result.append("Excised productions:\n");
                for(FileInfo file : topLevelState.files)
                {
                    for(String p : file.productionsExcised)
                    {
                        result.append("        " + p + "\n");
                    }
                }
            }
            result.append("Source finished.");
        }
        return result.toString();
    }

    private URL getParentUrl(URL url) throws SoarException
    {
        final String s = url.toExternalForm();
        final int i = s.lastIndexOf('/');
        if(i == -1)
        {
            throw new SoarException("Cannot determine parent of URL: " + url);
        }
        URL parent = FileTools.asUrl(s.substring(0, i));
        if (parent != null)
        {
            return parent;
        }
        return FileTools.asUrl(s.substring(0, i) + "/");
    }


    /*package*/ URL joinUrl(URL parent, String child)
    {
        final String s = parent.toExternalForm();
        return FileTools.asUrl(s.endsWith("/") ? s + child : s + "/" + child);
    }

    //In Android, this is really parsing an asset, not a file
    private void evalFileAndPop(String file) throws SoarException
    {
        try
        {
            //replace the system file separator to be a standard forward slash
            sourcedFiles.add(file.replace(File.separator, "/"));

            fileStack.push(file);
            if(topLevelState != null)
            {
                topLevelState.files.add(new FileInfo(file));
            }
            String code = "";
            try {
                InputStream is = assetManager.open(getWorkingDirectoryPath() + file);
                code = new String(ByteStreams.toByteArray(is));
            } catch (FileNotFoundException e){
                logger.warn("Sourced file not found: " + file, e);
            } catch (IOException e) {
                throw new SoarException("Error while sourcing file: " + file, e);
            }
            interp.eval(code);
        }
        finally
        {
            fileStack.pop();
            popd();
        }
    }

    private void evalUrlAndPop(URL urlIn) throws SoarException
    {
        final URL url = normalizeUrl(urlIn);

        try
        {
            fileStack.push(url.toExternalForm());
            if(topLevelState != null)
            {
                topLevelState.files.add(new FileInfo(url.toExternalForm()));
            }
            interp.eval(url);
        }
        finally
        {
            fileStack.pop();
            popd();
        }
    }

    /**
     * Make sure an URL is normalized, i.e. does not contain any .. or .
     * path components.
     *
     * @param url the url to normalize
     * @return normalized URL
     * @throws SoarException if there are any problems with the URL
     */
    private URL normalizeUrl(URL url) throws SoarException
    {
        try
        {
            return url.toURI().normalize().toURL();
        }
        catch (MalformedURLException e)
        {
            throw new SoarException(e.getMessage(), e);
        }
        catch (URISyntaxException e)
        {
            throw new SoarException(e.getMessage(), e);
        }
    }

    public String getWorkingDirectoryPath()
    {
        String path = "";

        for(DirStackEntry s : directoryStack)
        {
            path = path + s + "/";
        }

        return path;
    }

    public void initDirectoryStack(File directory)
    {
        directoryStack.push(new DirStackEntry(directory));
//        String[] dirs = splitPath(directory);
//        for(int i = 0; i < dirs.length; i++)
//        {
//            directoryStack.push(new DirStackEntry(dirs[i]);
//        }
    }

    public String[] splitPath(String path)
    {
        ArrayList<String> parts = new ArrayList<String>();

        int start = 0;
        int index = 0;
        while(index < path.length())
        {
            if(index == path.length()-1)
            {
                parts.add(path.substring(start, index+1));
            }
            else if(path.charAt(index) == '/')
            {
                parts.add(path.substring(start, index));
                start = index + 1;
            }

            index++;
        }

        return parts.toArray(new String[0]);
    }

    // This ain't pretty, but it's private and it works
    /*package*/ static class DirStackEntry
    {
        File file;
        URL url;

        public DirStackEntry(File file) { this.file = file; }
        public DirStackEntry(URL url) { this.url = url; }
    }

    private static class FileInfo
    {
        final String name;
        final List<String> productionsAdded = new ArrayList<String>();
        final List<String> productionsExcised = new ArrayList<String>();

        public FileInfo(String name)
        {
            this.name = name;
        }
    }

    private static class TopLevelState
    {
        final List<FileInfo> files = new ArrayList<FileInfo>();
        int totalProductionsAdded = 0;
        int totalProductionsExcised = 0;
        //int totalProductionsIgnored = 0; // TODO implement totalProductionsIgnored

        void productionAdded(Production p)
        {
            current().productionsAdded.add(p.getName());
            totalProductionsAdded++;
        }
        void productionExcised(Production p)
        {
            current().productionsExcised.add(p.getName());
            totalProductionsExcised++;
        }
        private FileInfo current()
        {
            return files.get(files.size() - 1);
        }
    }
}
