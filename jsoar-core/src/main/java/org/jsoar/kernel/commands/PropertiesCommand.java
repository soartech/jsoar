package org.jsoar.kernel.commands;

import java.util.Collections;
import java.util.Comparator;
import java.util.List;

import org.jsoar.kernel.Agent;
import org.jsoar.kernel.tracing.Printer;
import org.jsoar.util.commands.PicocliSoarCommand;
import org.jsoar.util.properties.PropertyKey;
import org.jsoar.util.properties.PropertyManager;

import picocli.CommandLine.Command;
import picocli.CommandLine.HelpCommand;

/**
 * This is the implementation of the "properties" command.
 * @author austin.brehob
 */
public class PropertiesCommand extends PicocliSoarCommand
{

    public PropertiesCommand(Agent agent)
    {
        super(agent, new Properties(agent));
    }
    
    @Override
    public Properties getCommand() {
        return (Properties)super.getCommand();
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
}
