package org.jsoar.kernel.commands;

import java.io.File;
import java.io.IOException;
import java.util.List;

import org.jsoar.kernel.LogManager;
import org.jsoar.kernel.LogManager.EchoMode;
import org.jsoar.kernel.LogManager.LogLevel;
import org.jsoar.kernel.LogManager.LoggerException;
import org.jsoar.kernel.SoarException;
import org.jsoar.util.DefaultSourceLocation;
import org.jsoar.util.SourceLocation;
import org.jsoar.util.commands.OptionProcessor;
import org.jsoar.util.commands.SoarCommand;
import org.jsoar.util.commands.SoarCommandContext;
import org.jsoar.util.commands.SoarCommandInterpreter;

import com.google.common.collect.Lists;

/**
 * Implementation of the "log" command.
 * @author adam.sypniewski
 */
public class LogCommand implements SoarCommand {
	
	private final LogManager logManager;
	private final SoarCommandInterpreter soarCommandInterpreter;
    private final OptionProcessor<Options> options = OptionProcessor.create();
    
    private enum Options
    {
        add,
        on, enable, yes,
        off, disable, no,
        strict,
        echo,
        init,
        collapse,
        level
    }
    
    public LogCommand(LogManager logManager, SoarCommandInterpreter soarCommandInterpreter)
    {
        this.logManager = logManager;
        this.soarCommandInterpreter = soarCommandInterpreter;
        
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
        	.newOption(Options.level)
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
			if (nonOpts.isEmpty())
			{
				if (logManager.isActive())
					return "Logger already enabled.";
				else
				{
					logManager.setActive(true);
					return "Logging enabled.";
				}
			}
			else if (nonOpts.size() == 1)
			{
				try
				{
					logManager.enableLogger(nonOpts.get(0));
				}
				catch (LoggerException e)
				{
					throw new SoarException(e.getMessage(), e);
				}
				return "Logger [" + nonOpts.get(0) + "] enabled.";
			}
			else
				throw new SoarException("Too many arguments. Expected: a log to enable or no argument (to enable logging altogether).");
		}
		else if (options.has(Options.off) || options.has(Options.disable) || options.has(Options.no))
		{
			if (nonOpts.isEmpty())
			{
				if (!logManager.isActive())
					return "Logger already disabled.";
				else
				{
					logManager.setActive(false);
					return "Logging disabled.";
				}
			}
			else if (nonOpts.size() == 1)
			{
				try
				{
					logManager.disableLogger(nonOpts.get(0));
				}
				catch (LoggerException e)
				{
					throw new SoarException(e.getMessage(), e);
				}
				return "Logger [" + nonOpts.get(0) + "] disabled.";
			}
			else
				throw new SoarException("Too many arguments. Expected: a log to disable or no argument (to disable logging altogether).");
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
		else if (options.has(Options.level))
		{
			if (nonOpts.size() != 1)
				throw new SoarException("Expected one argument: trace | debug | info | warn | error");
			
			LogLevel logLevel;
			try
			{
				logLevel = LogLevel.fromString(nonOpts.get(0));
			}
			catch (IllegalArgumentException e)
			{
				throw new SoarException("Unknown echo-mode value: " + nonOpts.get(0));
			}
			logManager.setLogLevel(logLevel);
			
			return "Logger level set to: " + logLevel.toString();
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
				
				loggerName = null;
				
				SourceLocation sourceLocation = context.getSourceLocation();
		        if (sourceLocation != DefaultSourceLocation.UNKNOWN)
		        {
		            String fileName = sourceLocation.getFile();
		            if (fileName != null && !fileName.isEmpty())
		            {
		                loggerName = collapseFileName(fileName, soarCommandInterpreter.getWorkingDirectory());
		                if (loggerName != null)
		                {
		                    // Prevent strict mode from biting us.
		                    if (!logManager.hasLogger(loggerName))
		                    {
                                try {
                                    logManager.addLogger(loggerName);
                                } catch (LoggerException e) {
                                    loggerName = null;
                                }
		                    }
		                }
		            }
		        }
		        
		        if (loggerName == null)
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
	
	public static String collapseFileName(String file, String cwd)
	{
	    String pathSeparator = System.getProperty("file.separator");
	    
	    String[] cwdParts;
	    String[] fileParts;
        try {
            cwdParts = new File(cwd).getCanonicalPath().split(pathSeparator);
            fileParts = new File(file).getCanonicalPath().split(pathSeparator);
        } catch (IOException e) {
            return null;
        }
        
        int minLength = Math.min(cwdParts.length, fileParts.length);
	    
	    int marker;
	    for (marker = 0; marker < minLength; ++marker)
	    {
	        if (!cwdParts[marker].equals(fileParts[marker]))
	            break;
	    }
	    
	    String result = "";
	    for (int i = 0; i < cwdParts.length - marker; ++i)
	        result += "^";
	    for (int i = marker; i < fileParts.length - 1; ++i)
	        result += fileParts[i].charAt(0) + ".";
	    result += fileParts[fileParts.length-1];
	    	    
	    return result;
	}
}
