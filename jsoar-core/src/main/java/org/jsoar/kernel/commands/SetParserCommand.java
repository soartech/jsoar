package org.jsoar.kernel.commands;

import java.lang.reflect.InvocationTargetException;

import org.jsoar.kernel.Agent;
import org.jsoar.kernel.parser.Parser;
import org.jsoar.util.commands.PicocliSoarCommand;

import picocli.CommandLine.Command;
import picocli.CommandLine.HelpCommand;
import picocli.CommandLine.Parameters;

/**
 * This is the implementation of the "set-parser" command.
 * 
 * @author austin.brehob
 */
public class SetParserCommand extends PicocliSoarCommand
{
    public SetParserCommand(Agent agent)
    {
        super(agent, new SetParser(agent));
    }
    
    @Command(name = "set-parser", description = "Sets the current parser", subcommands = { HelpCommand.class })
    static public class SetParser implements Runnable
    {
        private Agent agent;
        
        public SetParser(Agent agent)
        {
            this.agent = agent;
        }
        
        @Parameters(description = "The new parser")
        String parser;
        
        @Override
        public void run()
        {
            if(parser == null)
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
            catch(ClassNotFoundException | InstantiationException | IllegalAccessException | ClassCastException | IllegalArgumentException | InvocationTargetException | NoSuchMethodException
                    | SecurityException e)
            {
                agent.getPrinter().startNewLine().print(e.getClass() + " error: " + e.getMessage());
            }
        }
    }
}
