package org.jsoar.kernel;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.base.Joiner;

public class LogManager {
	
	private final Agent agent;
	private EchoMode echoMode = EchoMode.on;
	private boolean active = true;
	private boolean strict = false;
	private final Map<String, Logger> loggers = new HashMap<String, Logger>();
	
	static private final SimpleDateFormat timestampFormatter = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss.SSS");
	
	public class LoggerException extends Exception
	{
		private static final long serialVersionUID = 1L;
		public LoggerException(String message)
		{
			super(message);
		}
	}
	
	public enum LogLevel
	{
		info("INFO"),
		debug("DEBUG"),
		warn("WARN"),
		trace("TRACE"),
		error("ERROR");
		
		static private Map<String, LogLevel> logLevelStrings;
		static
		{
			logLevelStrings = new HashMap<String, LogLevel>();
			logLevelStrings.put("INFO", info);
			logLevelStrings.put("DEBUG", debug);
			logLevelStrings.put("WARN", warn);
			logLevelStrings.put("TRACE", trace);
			logLevelStrings.put("ERROR", error);
		}
		
		private String stringValue;
		
		private LogLevel(String stringValue)
		{
			this.stringValue = stringValue;
		}
		
		static public LogLevel fromString(String logLevel)
		{
			LogLevel val = logLevelStrings.get(logLevel.toUpperCase());
			if (val == null)
				throw new IllegalArgumentException();
			return val;
		}
		
		@Override
		public String toString()
		{
			return stringValue;
		}
	}
	
	public enum EchoMode
	{
		off("OFF"),
		simple("SIMPLE"),
		on("ON");
		
		static private Map<String, EchoMode> echoModeStrings;
		static
		{
			echoModeStrings = new HashMap<String, EchoMode>();
			echoModeStrings.put("OFF", off);
			echoModeStrings.put("SIMPLE", simple);
			echoModeStrings.put("ON", on);
		}
		
		private String stringValue;
		
		private EchoMode(String stringValue)
		{
			this.stringValue = stringValue;
		}
		
		static public EchoMode fromString(String echoMode)
		{
			EchoMode val = echoModeStrings.get(echoMode.toUpperCase());
			if (val == null)
				throw new IllegalArgumentException();
			return val;
		}
		
		@Override
		public String toString()
		{
			return stringValue;
		}
	}
	
	public LogManager(Agent agent)
	{
		this.agent = agent;
		init();
	}
	
	public void init()
	{
		loggers.clear();
		loggers.put("default", LoggerFactory.getLogger("default"));
	}
	
	public Logger getLogger(String loggerName) throws LoggerException
    {
    	Logger logger = loggers.get(loggerName);
    	if (logger == null)
    	{
    		if (strict)
    			throw new LoggerException("Logger [" + loggerName + "] does not exists (strict mode enabled).");
    		logger = LoggerFactory.getLogger(loggerName);
    		loggers.put(loggerName, logger);
    	}
		return logger;
	}
    
    public Set<String> getLoggerNames()
    {
    	return new HashSet<String>(loggers.keySet());
    }
    
    public int getLoggerCount()
    {
    	return loggers.size();
    }
    
    public Logger addLogger(String loggerName) throws LoggerException
    {
    	Logger logger = loggers.get(loggerName);
    	if (logger != null)
    	{
    		if (strict)
    			throw new LoggerException("Logger [" + loggerName + "] already exists (strict mode enabled).");
    	}
    	else
    	{
    		logger = LoggerFactory.getLogger(loggerName);
    		loggers.put(loggerName, logger);
    	}
    	return logger;
    }
    
    public boolean hasLogger(String loggerName)
    {
    	return loggers.containsKey(loggerName);
    }
    
    public String getLoggerStatus()
    {
    	String result
    	        = "      Log Settings     \n";
    	result += "=======================\n";
    	result += "logging:           " + (isActive() ? "on" : "off") + "\n";
    	result += "strict:            " + (isStrict() ? "on" : "off") + "\n";
    	result += "echo mode:         " + getEchoMode().toString().toLowerCase() + "\n";
    	result += "number of loggers: " + loggers.size() + "\n";
    	result += "------- Loggers -------\n";
    	
    	List<String> loggerList = new ArrayList<String>(getLoggerNames());
    	Collections.sort(loggerList);
    	for (String loggerName : loggerList)
    		result += loggerName + "\n";
    	
    	return result;
    }

    public void log(String loggerName, LogLevel logLevel, List<String> args, boolean collapse) throws LoggerException
    {
    	if (!active)
    		return;
    	
    	Logger logger = getLogger(loggerName);
    	
    	String result = formatArguments(args, collapse);
    	    	
    	if (logLevel == LogLevel.debug)
    		logger.debug(result);
    	else if (logLevel == LogLevel.info)
    		logger.info(result);
    	else if (logLevel == LogLevel.warn)
    		logger.warn(result);
    	else if (logLevel == LogLevel.trace)
    		logger.trace(result);
    	else
    		logger.error(result);
    	
    	if (echoMode != EchoMode.off)
    	{
    		agent.getPrinter().startNewLine();
    		
    		if (echoMode == EchoMode.simple)
    			agent.getPrinter().print(result);
    		else
    			agent.getPrinter().print("[" + logLevel.toString() + " " + getTimestamp() + "] " + loggerName + ": " + result);
    		
    		agent.getPrinter().flush();
    	}
    }
    
    private String formatArguments(List<String> args, boolean collapse)
    {
    	if (args.size() > 1)
    	{
    		String formatString = args.get(0);
    		if (formatString.contains("{}"))
    		{
    			int numFields = (formatString.length() - formatString.replace("{}", "").length()) / 2;
    			if (numFields == args.size() - 1)
    				return String.format(formatString.replace("{}", "%s"), args.subList(1, args.size()).toArray(new Object[args.size() - 1]));
    		}
    	}
    	
    	return Joiner.on(collapse ? "" : " ").join(args);
    }
    
    public static String getTimestamp()
    {
    	return timestampFormatter.format(new Date(System.currentTimeMillis()));
    }
    
    public boolean isActive()
    {
    	return active;
    }
    
    public void setActive(boolean active)
    {
    	this.active = active;
    }
    
    public boolean isStrict()
    {
    	return strict;
    }
    
    public void setStrict(boolean strict)
    {
    	this.strict = strict;
    }
    
    public EchoMode getEchoMode()
    {
    	return echoMode;
    }
    
    public void setEchoMode(EchoMode echoMode)
    {
    	this.echoMode = echoMode;
    }
}
