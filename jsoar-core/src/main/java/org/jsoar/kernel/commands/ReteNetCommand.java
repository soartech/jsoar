package org.jsoar.kernel.commands;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;

import org.jsoar.kernel.Agent;
import org.jsoar.kernel.SoarException;
import org.jsoar.kernel.rete.ReteSerializer;
import org.jsoar.util.FileTools;
import org.jsoar.util.StringTools;
import org.jsoar.util.commands.SoarCommand;
import org.jsoar.util.commands.SoarCommandContext;

/**
 * Implementation of the "rete-net" command.
 * 
 * The rete-net command allows an agent's productions to be serialized to a
 * binary file and later restored. Loading productions from a rete-net file
 * causes all prior productions in memory to be excised.
 * 
 * <p>Usage:
 * 
 * <pre>{@code
 * 
 * Save a rete network to a file:
 * > rete-net -s <file path>
 * 
 * Restore a rete from a file:
 * > rete-net -l <file path>
 * 
 * Restore a rete from a url:
 * > rete-net -l <url>
 * 
 * }</pre>
 * 
 * @see org.jsoar.kernel.ReteSerializer
 * @author charles.newton
 */
public class ReteNetCommand implements SoarCommand
{
    private final Agent agent;
    private final SourceCommand sourceCommand;

    public ReteNetCommand(SourceCommand sourceCommand, Agent context)
    {
        this.sourceCommand = sourceCommand;
        this.agent = context;
    }

    @Override
    public String execute(SoarCommandContext context, String[] args) throws SoarException
    {
        if(args.length < 2)
        {
            throw new SoarException("Expected one of --save or --load");
        }

        if(args.length < 3)
        {
            throw new SoarException("Expected file name argument");
        }

        final String arg = args[1];
        if("-s".equals(arg) || "--save".equals(arg))
        {
            save(args[2]);
            return "Rete saved";
        }
        else if("-l".equals(arg) || "--load".equals(arg) || "-r".equals(arg) || "--restore".equals(arg))
        {
            load(args[2]);
            return "Rete loaded into agent";
        }
        else
        {
            throw new SoarException("Unknown option '" + arg + "'");
        }
    }

    public void load(String source) throws SoarException
    {
        InputStream is = null;
        try
        {
            is = source(source); 
            ReteSerializer.replaceRete(agent, is);
        }
        catch (IOException e)
        {
            e.printStackTrace();
            final Throwable cause = e.getCause();
            agent.getPrinter().error("Unable to deserialize rete (I/O error): " + 
                    (cause != null ? cause.getMessage() : e.getMessage()) + "\n" +
                    StringTools.getStackTrace(e));
            throw new SoarException("IO error during rete load:\n" + e.toString());
        }
        finally 
        {
            if (is != null)
            {
                try
                {
                    is.close();
                }
                catch (IOException e)
                {
                    throw new SoarException("IO error while closing the input source.");
                }
            }
        }
    }
    
    /**
     * Construct an InputStream from a file or URL.
     */
    private InputStream source(String fileString) throws SoarException, IOException
    {
        final URL url = FileTools.asUrl(fileString);
        File file = new File(fileString);
        if(url != null)
        {
            return url.openStream();
        }
        else if(file.isAbsolute())
        {       
            if (!file.exists())
            {
                throw new SoarException("File not found: " + fileString);
            }
            return new FileInputStream(file);
 
        }
        else if(sourceCommand.getWorkingDirectoryRaw().url != null)
        {
            final URL childUrl = sourceCommand.joinUrl(sourceCommand.getWorkingDirectoryRaw().url, fileString);
            return childUrl.openStream();
        }
        else 
        {
            file = new File(sourceCommand.getWorkingDirectoryRaw().file, file.getPath());
            if (!file.exists())
            {
                throw new SoarException("File not found: " + fileString);
            }
            return new FileInputStream(file);
        } 
//        URL url = FileTools.asUrl(fileString);
//        if (!new File(fileString).isAbsolute())
//        {
//            fileString = sourceCommand.getWorkingDirectory() + "/" + fileString;
//        }
//        File file = new File(fileString);
//        if (url != null)
//        {
//            return url.openStream();
//        }
//        if (!file.exists())
//        {
//            throw new SoarException("File not found: " + fileString);
//        }
//        else
//        {
//            return new FileInputStream(file);
//        }
    }
 
    public void save(String filename) throws SoarException
    {
        FileOutputStream fos = null;
        if (!new File(filename).isAbsolute())
        {
            filename = sourceCommand.getWorkingDirectory() + "/" + filename;
        }
        try
        {
            fos = new FileOutputStream(filename);
            ReteSerializer.saveRete(agent, fos);
            fos.close();
        }
        catch (IOException e)
        {
            e.printStackTrace();
            final Throwable cause = e.getCause();
            agent.getPrinter().error("Unable to serialize rete (I/O error): " + 
                    (cause != null ? cause.getMessage() : e.getMessage()) + "\n" +
                    StringTools.getStackTrace(e));
            throw new SoarException("IO error during rete save:\n" + e.toString());
        }
        finally
        {
            if (fos != null)
            {
                try
                {
                    fos.close();
                }
                catch (IOException e)
                {
                    throw new SoarException("IO error while closing the file.");
                }
            }
        }
    }
}