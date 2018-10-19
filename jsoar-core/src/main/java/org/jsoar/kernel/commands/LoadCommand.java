package org.jsoar.kernel.commands;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.util.zip.GZIPInputStream;

import org.jsoar.kernel.Agent;
import org.jsoar.kernel.SoarException;
import org.jsoar.kernel.rete.ReteSerializer;
import org.jsoar.util.FileTools;
import org.jsoar.util.StringTools;
import org.jsoar.util.commands.SoarCommand;
import org.jsoar.util.commands.SoarCommandContext;

import picocli.CommandLine.Command;
import picocli.CommandLine.HelpCommand;
import picocli.CommandLine.Option;
import picocli.CommandLine.Parameters;
import picocli.CommandLine.ParentCommand;

/**
 * This is the implementation of the "load" command.
 * @author austin.brehob
 */
public class LoadCommand implements SoarCommand
{
    private SourceCommand sourceCommand;
    private Agent agent;
    
    public LoadCommand(SourceCommand sourceCommand, Agent agent)
    {
        this.sourceCommand = sourceCommand;
        this.agent = agent;
    }
    
    @Override
    public String execute(SoarCommandContext context, String[] args) throws SoarException
    {
        Utils.parseAndRun(agent, new Load(sourceCommand, agent), args);
        
        return "";
    }
    
    @Command(name="load", description="Loads a file or rete-net",
            subcommands={HelpCommand.class,
                         ReteNet.class})
    static public class Load implements Runnable
    {
        private SourceCommand sourceCommand;
        private Agent agent;
        
        public Load(SourceCommand sourceCommand, Agent agent)
        {
            this.sourceCommand = sourceCommand;
            this.agent = agent;
        }
        
        @Override
        public void run()
        {
            agent.getPrinter().startNewLine().print("File type is required.");
        }
    }
    
    @Command(name="rete-net", description="Resotres an agent's productions from a binary file. " + 
            "Loading productions from a rete-net file causes all prior productions in memory to be excised.",
            subcommands={HelpCommand.class})
    static public class ReteNet implements Runnable
    {
        @ParentCommand
        Load parent; // injected by picocli

        @Option(names={"-l", "--load, -r, --restore"}, description="Required to load the file")
        boolean close = false;
        
        @Parameters(description="File name")
        String fileName = null;
        
        @Override
        public void run()
        {
            InputStream is = null;
            
            try
            {
                is = uncompressIfNeeded(fileName, findFile(fileName));
                ReteSerializer.replaceRete(parent.agent, is);
            }
            catch (IOException e)
            {
                parent.agent.getPrinter().startNewLine().print("Unable to deserialize rete (I/O error): " + 
                        (e.getCause() != null ? e.getCause().getMessage() : e.getMessage()) + "\n" +
                        StringTools.getStackTrace(e));
                return;
            }
            catch (SoarException e)
            {
                parent.agent.getPrinter().startNewLine().print(e.getMessage());
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
                        parent.agent.getPrinter().startNewLine().print("IO error while closing the input source.");
                    }
                }
            }
            
            parent.agent.getPrinter().startNewLine().print("Rete loaded into agent");
        }
        
        /**
         * Construct an InputStream from a file or URL.
         */
        private InputStream findFile(String fileString) throws SoarException, IOException
        {
            final URL url = FileTools.asUrl(fileString);
            File file = new File(fileString);
            if (url != null)
            {
                return url.openStream();
            }
            else if (file.isAbsolute())
            {       
                if (!file.exists())
                {
                    parent.agent.getPrinter().startNewLine().print("File not found: " + fileString);
                }
                return new FileInputStream(file);
            }
            else if (parent.sourceCommand.getWorkingDirectoryRaw().url != null)
            {
                final URL childUrl = parent.sourceCommand.joinUrl(
                        parent.sourceCommand.getWorkingDirectoryRaw().url, fileString);
                return childUrl.openStream();
            }
            else 
            {
                file = new File(parent.sourceCommand.getWorkingDirectoryRaw().file, file.getPath());
                if (!file.exists())
                {
                    parent.agent.getPrinter().startNewLine().print("File not found: " + fileString);
                }
                return new FileInputStream(file);
            } 
        }
        
        private InputStream uncompressIfNeeded(String filename, InputStream is) throws IOException
        {
            if (filename.endsWith(".Z")) 
            {
                return new GZIPInputStream(is);
            }
            return is;
        }
    }
}
