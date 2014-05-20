package org.jsoar.kernel.commands;

import java.util.List;

import org.jsoar.kernel.LogManager;
import org.jsoar.kernel.LogManager.EchoMode;
import org.jsoar.kernel.LogManager.LogLevel;
import org.jsoar.kernel.LogManager.LoggerException;
import org.jsoar.kernel.SoarException;
import org.jsoar.util.commands.OptionProcessor;
import org.jsoar.util.commands.SoarCommand;
import org.jsoar.util.commands.SoarCommandContext;

import com.google.common.collect.Lists;

/**
 * Implementation of the "log" command.
 * @author adam.sypniewski
 */
public class LogCommand implements SoarCommand {
	
	private final LogManager logManager;
    private final OptionProcessor<Options> options = OptionProcessor.create();
    
    private enum Options
    {
        add,
        on, enable, yes,
        off, disable, no,
        strict,
        echo,
        init,
        collapse
    }
    
    public LogCommand(LogManager logManager)
    {
        this.logManager = logManager;
        
        options
        	.newOption(Options.add)
        	.newOption(Options.on)
        		.newOption(Options.enable).shortOption('b')
        		.newOption(Options.yes)
        	.newOption(Options.off).shortOption('z')
        		.newOption(Options.disable)
        		.newOption(Options.no)
        	.newOption(Options.strict)
        	.newOption(Options.echo)
        	.newOption(Options.init)
        	.newOption(Options.collapse)
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
		else if (options.has(Options.on) || options.has(Options.enable) || options.has(Options.yes))
		{
			if (logManager.isActive())
				return "Logger already enabled.";
			else
			{
				logManager.setActive(true);
				return "Logging enabled.";
			}
		}
		else if (options.has(Options.off) || options.has(Options.disable) || options.has(Options.no))
		{
			if (!logManager.isActive())
				return "Logger already disabled.";
			else
			{
				logManager.setActive(false);
				return "Logging disabled.";
			}
		}
		else if (options.has(Options.init))
		{
			logManager.init();
			return "Logger init.";
		}
		else if (options.has(Options.strict))
		{
			if (nonOpts.size() != 1)
				throw new SoarException("Expected one argument: yes | enable | on | no | disable | off");
			
			String mode = nonOpts.get(0);
			if (mode.toLowerCase().equalsIgnoreCase("yes") || mode.toLowerCase().equalsIgnoreCase("enable") || mode.toLowerCase().equalsIgnoreCase("on"))
			{
				if (logManager.isStrict())
					return "Logger already in strict mode.";
				else
				{
					logManager.setStrict(true);
					return "Logger set to strict mode.";
				}
			}
			else if (mode.toLowerCase().equalsIgnoreCase("no") || mode.toLowerCase().equalsIgnoreCase("disable") || mode.toLowerCase().equalsIgnoreCase("off"))
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
		else if (options.has(Options.echo))
		{
			if (nonOpts.size() != 1)
				throw new SoarException("Expected one argument: off | simple | on");
			
			EchoMode echoMode;
			try
			{
				echoMode = EchoMode.fromString(nonOpts.get(0));
			}
			catch (IllegalArgumentException e)
			{
				throw new SoarException("Unknown echo-mode value: " + nonOpts.get(0));
			}
			logManager.setEchoMode(echoMode);
			
			return "Logger echo mode set to: " + echoMode.toString();
		}
		else if (nonOpts.isEmpty())
		{
			return logManager.getLoggerStatus();
		}
		else
		{
			if (nonOpts.size() < 2)
				throw new SoarException("Too few argugments. Expected: log [LOGGER-NAME] {INFO | DEBUG | WARN | ERROR} MESSAGE...");
			
			boolean collapse = options.has(Options.collapse);				
			
			String loggerName = nonOpts.get(0);
			LogLevel logLevel;
			List<String> parameters;
			
			try
			{
				logLevel = LogManager.LogLevel.fromString(loggerName);
				loggerName = "default";
				parameters = nonOpts.subList(1, nonOpts.size());
			}
			catch (IllegalArgumentException e)
			{
				try
				{
					logLevel = LogManager.LogLevel.fromString(nonOpts.get(1));
				}
				catch (IllegalArgumentException ee)
				{
					throw new SoarException("Unknown log-level value: " + nonOpts.get(1));
				}
				
				parameters = nonOpts.subList(2, nonOpts.size());
			}

			try
			{
				logManager.log(loggerName, logLevel, parameters, collapse);
			}
			catch (LoggerException e)
			{
				throw new SoarException(e.getMessage(), e);
			}
			
			return "";
		}
	}
}
