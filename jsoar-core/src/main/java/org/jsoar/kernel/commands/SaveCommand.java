package org.jsoar.kernel.commands;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.util.zip.GZIPOutputStream;

import org.jsoar.kernel.Agent;
import org.jsoar.kernel.SoarException;
import org.jsoar.kernel.rete.ReteSerializer;
import org.jsoar.util.commands.SoarCommand;
import org.jsoar.util.commands.SoarCommandContext;

import picocli.CommandLine.Command;
import picocli.CommandLine.HelpCommand;
import picocli.CommandLine.Option;
import picocli.CommandLine.Parameters;
import picocli.CommandLine.ParentCommand;

/**
 * This is the implementation of the "save rete-net" command.
 * @author austin.brehob
 */
public class SaveCommand implements SoarCommand
{
    private SourceCommand sourceCommand;
    private Agent agent;
    
    public SaveCommand(SourceCommand sourceCommand, Agent agent)
    {
        this.sourceCommand = sourceCommand;
        this.agent = agent;
    }
    
    @Override
    public String execute(SoarCommandContext context, String[] args) throws SoarException
    {
        Utils.parseAndRun(agent, new Save(sourceCommand, agent), args);
        
        return "";
    }
    
    
    @Command(name="save", description="Saves a rete-net",
            subcommands={HelpCommand.class,
                         ReteNet.class})
    static public class Save implements Runnable
    {
        private SourceCommand sourceCommand;
        private Agent agent;
        
        public Save(SourceCommand sourceCommand, Agent agent)
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
    
    
    @Command(name="rete-net", description="Serializes an agent's productions to a binary file",
            subcommands={HelpCommand.class})
    static public class ReteNet implements Runnable
    {
        @ParentCommand
        Save parent; // injected by picocli

        @Option(names={"-s", "--save"}, arity="1", description="File name to save rete-net to")
        String fileName = null;
        
        @Override
        public void run()
        {
            OutputStream os = null;
            if (!new File(fileName).isAbsolute())
            {
                fileName = parent.sourceCommand.getWorkingDirectory() + "/" + fileName;
            }
            try
            {
                os = compressIfNeeded(fileName, new FileOutputStream(fileName));
                ReteSerializer.saveRete(parent.agent, os);
                os.close();
            }
            catch (IOException e)
            {
                parent.agent.getPrinter().startNewLine().print("Save file failed.");
                return;
            }
            catch (SoarException e)
            {
                parent.agent.getPrinter().startNewLine().print(e.getMessage());
                return;
            }
            finally
            {
                if (os != null)
                {
                    try
                    {
                        os.close();
                    }
                    catch (IOException e)
                    {
                        parent.agent.getPrinter().startNewLine().print(
                                "IO error while closing the file.");
                    }
                }
            }

            parent.agent.getPrinter().startNewLine().print("Rete saved");
        }
        
        private OutputStream compressIfNeeded(String filename, OutputStream os) throws IOException
        {
            if (filename.endsWith(".Z")) 
            {
                return new GZIPOutputStream(os);
            }
            return os;
        }
    }
}
