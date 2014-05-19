package org.jsoar.kernel.commands;

import java.util.List;

import org.jsoar.kernel.Agent;
import org.jsoar.kernel.LogManager;
import org.jsoar.kernel.LogManager.LogLevel;
import org.jsoar.kernel.SoarException;
import org.jsoar.kernel.LogManager.LoggerException;
import org.jsoar.util.commands.OptionProcessor;
import org.jsoar.util.commands.SoarCommand;
import org.jsoar.util.commands.SoarCommandContext;

import com.google.common.collect.Lists;

/**
 * Implementation of the "log" command.
 * @author adam.sypniewski
 */
public class LogCommand implements SoarCommand {
	
	private final Agent agent;
	private final LogManager logManager;
    private final OptionProcessor<Options> options = OptionProcessor.create();
    
    private enum Options
    {
        add,
        on, off,
        strict
    }
    
    public LogCommand(Agent agent)
    {
        this.agent = agent;
        logManager = agent.getLogManager();
        
        options
        	.newOption(Options.add)
        	.newOption(Options.on)
        	.newOption(Options.off).shortOption('f')
        	.newOption(Options.strict)
        	.done();
    }

	@Override
	public String execute(SoarCommandContext context, String[] args) throws SoarException
	{
		List<String> nonOpts = options.process(Lists.newArrayList(args));
		
		if (options.has(Options.add))
		{
			if (nonOpts.size() == 1)
			{
				String loggerName = nonOpts.get(0);
				try
				{
					logManager.addLogger(loggerName);
				}
				catch (LoggerException e)
				{
					throw new SoarException(e.getMessage(), e);
				}
				return "Added logger: " + loggerName;
			}
			else if (nonOpts.isEmpty())
				throw new SoarException("Too few arguments. Expected a logger name.");
			else
				throw new SoarException("Too many arguments. Expected only a logger name.");
		}
		else if (options.has(Options.on))
		{
			if (logManager.isActive())
				return "Logger already enabled.";
			else
			{
				logManager.setActive(true);
				return "Logging enabled.";
			}
		}
		else if (options.has(Options.off))
		{
			if (!logManager.isActive())
				return "Logger already disabled.";
			else
			{
				logManager.setActive(false);
				return "Logging disabled.";
			}
		}
		else if (options.has(Options.strict))
		{
			if (nonOpts.size() != 1)
				throw new SoarException("Expected one argument: yes | no");
			
			String mode = nonOpts.get(0);
			if (mode.toLowerCase().equalsIgnoreCase("yes"))
			{
				if (logManager.isStrict())
					return "Logger already in strict mode.";
				else
				{
					logManager.setStrict(true);
					return "Logger set to strict mode.";
				}
			}
			else if (mode.toLowerCase().equalsIgnoreCase("no"))
			{
				if (!logManager.isStrict())
					return "Logger already in non-strict mode.";
				else
				{
					logManager.setStrict(false);
					return "Logger set to non-strict mode.";
				}
			}
			else
				throw new SoarException("Expected one argument: yes | no");
		}
		else if (nonOpts.isEmpty())
		{
			return logManager.getLoggerStatus();
		}
		else
		{
			if (nonOpts.size() < 3)
				throw new SoarException("Too few argugments. Expected: log LOGGER-NAME [INFO | DEBUG | WARN | ERROR] MESSAGE...");
			
			String loggerName = nonOpts.get(0);
			LogLevel logLevel;
			try
			{
				logLevel = LogManager.LogLevel.fromString(nonOpts.get(1));
			}
			catch (IllegalArgumentException e)
			{
				throw new SoarException("Unknown log-level value: " + nonOpts.get(1));
			}
			String logMessage = nonOpts.get(2);
			try
			{
				logManager.log(loggerName, logLevel, logMessage);
			}
			catch (LoggerException e)
			{
				throw new SoarException(e.getMessage(), e);
			}
			
			return "";
		}
	}

}
