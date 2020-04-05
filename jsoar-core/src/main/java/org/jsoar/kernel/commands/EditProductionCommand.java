package org.jsoar.kernel.commands;

import java.awt.Desktop;
import java.io.File;
import java.io.IOException;

import org.jsoar.kernel.Agent;
import org.jsoar.kernel.Production;
import org.jsoar.util.FileTools;
import org.jsoar.util.SourceLocation;
import org.jsoar.util.commands.PicocliSoarCommand;

import picocli.CommandLine.Command;
import picocli.CommandLine.HelpCommand;
import picocli.CommandLine.Parameters;

/**
 * This is the implementation of the "edit-production" command.
 * @author austin.brehob
 */
public class EditProductionCommand extends PicocliSoarCommand
{

    public EditProductionCommand(Agent agent)
    {
        super(agent, new EditProduction(agent));
    }

    @Command(name="edit-production", description="Opens the given production in a text editor",
            subcommands={HelpCommand.class})
    static public class EditProduction implements Runnable
    {
        private Agent agent;

        public EditProduction(Agent agent)
        {
            this.agent = agent;
        }

        @Parameters(index="0", description="The production to edit")
        private String prodName = null;

        @Override
        public void run()
        {
            if (prodName == null)
            {
                agent.getPrinter().startNewLine().print("Expected single production name argument");
                return;
            }

            final Production p = agent.getProductions().getProduction(prodName);
            if (p == null)
            {
                agent.getPrinter().startNewLine().print("No production named '" + prodName + "'");
                return;
            }

            // Search for the file's location
            final SourceLocation location = p.getLocation();
            final String file = location.getFile();
            if (file == null || file.length() == 0)
            {
                agent.getPrinter().startNewLine().print("Don't know source "
                        + "location of production '" + prodName + "'");
                return;
            }
            if (FileTools.asUrl(file) != null)
            {
                agent.getPrinter().startNewLine().print("Don't know how to "
                        + "edit productions loaded from URLs: " + file);
                return;
            }

            // Open the file in a text editor (if possible)
            try
            {
                Desktop.getDesktop().edit(new File(file));
            }
            catch (IOException e)
            {
                agent.getPrinter().startNewLine().print("Failed to edit '" + file + "': " + e);
            }
        }
    }
    
}
