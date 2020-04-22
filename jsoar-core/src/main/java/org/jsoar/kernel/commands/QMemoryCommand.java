package org.jsoar.kernel.commands;

import org.jsoar.kernel.Agent;
import org.jsoar.kernel.io.quick.DefaultQMemory;
import org.jsoar.kernel.io.quick.QMemory;
import org.jsoar.kernel.io.quick.SoarQMemoryAdapter;
import org.jsoar.util.commands.PicocliSoarCommand;

import picocli.CommandLine.Command;
import picocli.CommandLine.HelpCommand;
import picocli.CommandLine.Option;
import picocli.CommandLine.Parameters;

/**
 * This is the implementation of the "qmemory" command.
 * @author austin.brehob
 */
public class QMemoryCommand extends PicocliSoarCommand
{
    public QMemoryCommand(Agent agent)
    {
        super(agent, new QMemoryC(agent, SoarQMemoryAdapter.attach(agent, DefaultQMemory.create())));
    }
    
    @Command(name="qmemory", description="Stores and retrieves items from qmemory",
            subcommands={HelpCommand.class})
    static public class QMemoryC implements Runnable
    {
        private Agent agent;
        private final SoarQMemoryAdapter adapter;

        public QMemoryC(Agent agent, SoarQMemoryAdapter adapter)
        {
            this.agent = agent;
            this.adapter = adapter;
        }

        @Option(names={"-g", "--get"}, description="Item to retreive from qmemory")
        String getPath;

        @Option(names={"-s", "--set"}, description="Item to set in qmemory")
        String setPath;

        @Parameters(arity="0..1", description="New value of item")
        String value;

        @Option(names={"-r", "--remove"}, description="Item to remove from qmemory")
        String removePath;

        @Option(names={"-c", "--clear"}, defaultValue="false", description="Clears everything from qmemory")
        boolean clear;

        @Override
        public void run()
        {
            if (getPath != null)
            {
                String returnVal = adapter.getSource().getString(fixPath(getPath));
                agent.getPrinter().startNewLine().print(returnVal);
            }
            else if (setPath != null)
            {
                if (value != null)
                {
                    final QMemory qmemory = adapter.getSource();
                    final String path = fixPath(setPath);

                    // Parse the value's type and set it in qmemory
                    try
                    {
                        qmemory.setInteger(path, Integer.parseInt(value));
                    }
                    catch (NumberFormatException e)
                    {
                        try
                        {
                            qmemory.setDouble(path, Double.parseDouble(value));
                        }
                        catch (NumberFormatException e1)
                        {
                            // |'s can be used to ensure the value's type is a string
                            if (value.length() >= 2 && value.charAt(0) == '|' &&
                                    value.charAt(value.length() - 1) == '|')
                            {
                                qmemory.setString(path, value.substring(1, value.length() - 1));
                            }
                            else
                            {
                                qmemory.setString(path, value);
                            }
                        }
                    }

                    agent.getPrinter().startNewLine().print(value);
                }
                else
                {
                    agent.getPrinter().startNewLine().print("Error: new value not provided");
                }
            }
            else if (removePath != null)
            {
                adapter.getSource().remove(fixPath(removePath));
            }
            else if (clear)
            {
                this.adapter.setSource(DefaultQMemory.create());
            }
            // In case the user forgets to provide the --get option...
            else if (value != null)
            {
                String returnVal = adapter.getSource().getString(fixPath(value));
                agent.getPrinter().startNewLine().print(returnVal);
            }
            else
            {
                agent.getPrinter().startNewLine().print("Error: expected one of "
                        + "--get, --set, --remove, or --clear");
            }
        }

        private String fixPath(String path)
        {
            return path.replace('(', '[').replace(')', ']');
        }
    }
}
