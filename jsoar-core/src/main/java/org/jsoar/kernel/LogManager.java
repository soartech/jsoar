package org.jsoar.kernel;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class LogManager {
	
	private final Agent agent;
	private boolean active = true;
	private boolean strict = false;
	private final Map<String, Logger> loggers = new HashMap<String, Logger>();
	
	public class LoggerException extends Exception
	{
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
		error("ERROR");
		
		static private Map<String, LogLevel> logLevelStrings;
		static
		{
			logLevelStrings = new HashMap<String, LogLevel>();
			logLevelStrings.put("INFO", info);
			logLevelStrings.put("DEBUG", debug);
			logLevelStrings.put("WARN", warn);
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
	
	public LogManager(Agent agent)
	{
		this.agent = agent;
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
    
    private Set<String> getLoggerNames()
    {
    	return new HashSet<String>(loggers.keySet());
    }
    
    public void addLogger(String loggerName) throws LoggerException
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
    }
    
    public boolean hasLogger(String loggerName)
    {
    	return loggers.containsKey(loggerName);
    }
    
    public String getLoggerStatus()
    {
    	String result
    	        = "    Log Settings   \n";
    	result += "===================\n";
    	result += "logging:       " + (isActive() ? "on" : "off") + "\n";
    	result += "strict:        " + (isStrict() ? "yes" : "no") + "\n";
    	result += "num-loggers:   " + loggers.size() + "\n";
    	result += "----- Loggers -----\n";
    	
    	List<String> loggerList = new ArrayList<String>(getLoggerNames());
    	Collections.sort(loggerList);
    	for (String loggerName : loggerList)
    		result += loggerName + "\n";
    	
    	return result;
    }
    
    public void log(String loggerName, LogLevel logLevel, String args) throws LoggerException
    {
    	if (!active)
    		return;
    	
    	Logger logger = getLogger(loggerName);
    	
    	if (logLevel == LogLevel.debug)
    		logger.debug(args);
    	else if (logLevel == LogLevel.info)
    		logger.info(args);
    	else if (logLevel == LogLevel.warn)
    		logger.warn(args);
    	else
    		logger.error(args);
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
}
