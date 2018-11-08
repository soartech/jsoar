package org.jsoar.kernel.commands;

import java.lang.reflect.InvocationTargetException;

import org.jsoar.kernel.Agent;
import org.jsoar.kernel.SoarException;
import org.jsoar.kernel.parser.Parser;
import org.jsoar.util.commands.SoarCommand;
import org.jsoar.util.commands.SoarCommandContext;

import picocli.CommandLine.Command;
import picocli.CommandLine.HelpCommand;
import picocli.CommandLine.Parameters;

/**
 * This is the implementation of the "set-parser" command.
 * @author austin.brehob
 */
public class SetParserCommand implements SoarCommand
{
    private final Agent agent;
    
    public SetParserCommand(Agent agent)
    {
        this.agent = agent;
    }

    @Override
    public String execute(SoarCommandContext context, String[] args) throws SoarException
    {
        Utils.parseAndRun(agent, new SetParser(agent), args);

        return "";
    }


    @Command(name="set-parser", description="Sets the current parser",
            subcommands={HelpCommand.class})
    static public class SetParser implements Runnable
    {
        private Agent agent;

        public SetParser(Agent agent)
        {
            this.agent = agent;
        }

        @Parameters(description="The new parser")
        String parser = null;

        @Override
        public void run()
        {
            if (parser == null)
            {
                agent.getPrinter().startNewLine().print("No parser provided");
                return;
            }

            try
            {
                Class<?> klass = Class.forName(parser);
                Parser parser = (Parser) klass.getConstructor().newInstance();
                agent.getProductions().setParser(parser);
            }
            catch (ClassNotFoundException | InstantiationException | IllegalAccessException |
                    ClassCastException | IllegalArgumentException | InvocationTargetException |
                    NoSuchMethodException | SecurityException e)
            {
                agent.getPrinter().startNewLine().print(e.getClass() + " error: " + e.getMessage());
            }
        }
    }
    @Override
    public Object getCommand() {
        return new SetParser(agent);
    }
}
