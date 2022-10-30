package org.jsoar.kernel.commands;

import org.jsoar.kernel.Agent;
import org.jsoar.kernel.io.quick.DefaultQMemory;
import org.jsoar.kernel.io.quick.QMemory;
import org.jsoar.kernel.io.quick.SoarQMemoryAdapter;
import org.jsoar.util.commands.PicocliSoarCommand;

import picocli.CommandLine.Command;
import picocli.CommandLine.HelpCommand;
import picocli.CommandLine.Option;

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

        @Option(names={"-g", "--get"}, description="Retreives item from qmemory at specified path")
        String getPath = "";

        @Option(names={"-s", "--set"}, arity="2", description="Path and item to set in qmemory (2 parameters required)")
        String[] setPathAndValue = new String[] {};

        @Option(names={"-r", "--remove"}, description="Removes item from qmemory at specified path")
        String removePath = "";

        @Option(names={"-c", "--clear"}, defaultValue="false", description="Clears everything from qmemory")
        boolean clear = false;

        @Override
        public void run()
        {
            if (getPath != null && !getPath.isEmpty())
            {
                String returnVal = adapter.getSource().getString(fixPath(getPath));
                agent.getPrinter().startNewLine().print(returnVal);
            }
            else if (setPathAndValue != null && setPathAndValue.length != 0)
            {
                
                String setPath = setPathAndValue[0];
                String value = setPathAndValue[1];
                
                
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
            else if (removePath != null)
            {
                adapter.getSource().remove(fixPath(removePath));
            }
            else if (clear)
            {
                this.adapter.setSource(DefaultQMemory.create());
            }
        }

        private String fixPath(String path)
        {
            return path.replace('(', '[').replace(')', ']');
        }
    }
}
