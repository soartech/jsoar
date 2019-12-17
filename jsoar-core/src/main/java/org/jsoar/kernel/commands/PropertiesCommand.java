package org.jsoar.kernel.commands;

import java.util.Collections;
import java.util.Comparator;
import java.util.List;

import org.jsoar.kernel.Agent;
import org.jsoar.kernel.SoarException;
import org.jsoar.kernel.tracing.Printer;
import org.jsoar.util.commands.SoarCommand;
import org.jsoar.util.commands.SoarCommandContext;
import org.jsoar.util.properties.PropertyKey;
import org.jsoar.util.properties.PropertyManager;

import picocli.CommandLine.Command;
import picocli.CommandLine.HelpCommand;

/**
 * This is the implementation of the "properties" command.
 * @author austin.brehob
 */
public class PropertiesCommand implements SoarCommand
{
    private final Agent agent;

    public PropertiesCommand(Agent agent)
    {
        this.agent = agent;
    }

    @Override
    public String execute(SoarCommandContext context, String[] args) throws SoarException
    {
        Utils.parseAndRun(agent, new Properties(agent), args);

        return "";
    }


    @Command(name="properties", description="Displays the agent's current properties",
            subcommands={HelpCommand.class})
    static public class Properties implements Runnable
    {
        private Agent agent;

        public Properties(Agent agent)
        {
            this.agent = agent;
        }

        @Override
        public void run()
        {
            final Printer p = agent.getPrinter();

            // Obtain all properties and sort them
            p.startNewLine();
            final PropertyManager properties = agent.getProperties();
            final List<PropertyKey<?>> keys = properties.getKeys();
            Collections.sort(keys, new Comparator<PropertyKey<?>>()
            {
                @Override
                public int compare(PropertyKey<?> a, PropertyKey<?> b)
                {
                    return a.getName().compareTo(b.getName());
                }
            });

            for (PropertyKey<?> key : keys)
            {
                p.print("%30s = %s%s\n", key.getName(), properties.get(key), key.isReadonly() ? " [RO]" : "");
            }
        }
    }
    @Override
    public Object getCommand() {
        return new Properties(agent);
    }
}
