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

public class LogManager
{
    
    private final Agent agent;
    private EchoMode echoMode = EchoMode.on;
    private boolean active = true;
    private boolean strict = false;
    private boolean abbreviate = true;
    private SourceLocationMethod sourceLocationMethod = SourceLocationMethod.disk;
    private LogLevel currentLogLevel = LogLevel.info;
    private final Map<String, Logger> loggers = new HashMap<>();
    private final Set<String> disabledLoggers = new HashSet<>();
    
    private final SimpleDateFormat timestampFormatter = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss.SSS");
    
    public class LoggerException extends Exception
    {
        private static final long serialVersionUID = 1L;
        
        public LoggerException(String message)
        {
            super(message);
        }
    }
    
    public enum SourceLocationMethod
    {
        none("NONE"),
        disk("DISK"),
        stack("STACK");
        
        private static Map<String, SourceLocationMethod> sourceLocationMethodStrings;
        static
        {
            sourceLocationMethodStrings = new HashMap<String, SourceLocationMethod>();
            sourceLocationMethodStrings.put("NONE", none);
            sourceLocationMethodStrings.put("DISK", disk);
            sourceLocationMethodStrings.put("STACK", stack);
        }
        private String stringValue;
        
        private SourceLocationMethod(String stringValue)
        {
            this.stringValue = stringValue;
        }
        
        public static SourceLocationMethod fromString(String sourceLocationMethod)
        {
            SourceLocationMethod val = sourceLocationMethodStrings.get(sourceLocationMethod.toUpperCase());
            if(val == null)
            {
                throw new IllegalArgumentException();
            }
            return val;
        }
        
        @Override
        public String toString()
        {
            return stringValue;
        }
    }
    
    public enum LogLevel
    {
        trace("TRACE", 1),
        debug("DEBUG", 2),
        info("INFO", 3),
        warn("WARN", 4),
        error("ERROR", 5);
        
        private static Map<String, LogLevel> logLevelStrings;
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
        private int numericValue;
        
        private LogLevel(String stringValue, int numericValue)
        {
            this.stringValue = stringValue;
            this.numericValue = numericValue;
        }
        
        public static LogLevel fromString(String logLevel)
        {
            LogLevel val = logLevelStrings.get(logLevel.toUpperCase());
            if(val == null)
            {
                throw new IllegalArgumentException();
            }
            return val;
        }
        
        @Override
        public String toString()
        {
            return stringValue;
        }
        
        public boolean wouldAcceptLogLevel(LogLevel logLevel)
        {
            return logLevel.numericValue >= this.numericValue;
        }
    }
    
    public enum EchoMode
    {
        off("OFF"),
        simple("SIMPLE"),
        on("ON");
        
        private static Map<String, EchoMode> echoModeStrings;
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
        
        public static EchoMode fromString(String echoMode)
        {
            EchoMode val = echoModeStrings.get(echoMode.toUpperCase());
            if(val == null)
            {
                throw new IllegalArgumentException();
            }
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
        disabledLoggers.clear();
        loggers.put("default", LoggerFactory.getLogger("default"));
    }
    
    public Logger getLogger(String loggerName) throws LoggerException
    {
        Logger logger = loggers.get(loggerName);
        if(logger == null)
        {
            if(strict)
            {
                throw new LoggerException("Logger [" + loggerName + "] does not exist (strict mode enabled).");
            }
            logger = LoggerFactory.getLogger(loggerName);
            loggers.put(loggerName, logger);
        }
        return logger;
    }
    
    public Set<String> getLoggerNames()
    {
        return new HashSet<>(loggers.keySet());
    }
    
    public int getLoggerCount()
    {
        return loggers.size();
    }
    
    public Logger addLogger(String loggerName) throws LoggerException
    {
        Logger logger = loggers.get(loggerName);
        if(logger != null)
        {
            if(strict)
            {
                throw new LoggerException("Logger [" + loggerName + "] already exists (strict mode enabled).");
            }
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
        String result = "      Log Settings     \n";
        result += "=======================\n";
        result += "logging:           " + (isActive() ? "on" : "off") + "\n";
        result += "strict:            " + (isStrict() ? "on" : "off") + "\n";
        result += "echo mode:         " + getEchoMode().toString().toLowerCase() + "\n";
        result += "log level:         " + getLogLevel().toString().toLowerCase() + "\n";
        result += "source location:   " + getSourceLocationMethod().toString().toLowerCase() + "\n";
        result += "abbreviate:        " + (getAbbreviate() ? "yes" : "no") + "\n";
        result += "number of loggers: " + loggers.size() + "\n";
        result += "------- Loggers -------\n";
        
        List<String> loggerList = new ArrayList<>(getLoggerNames());
        Collections.sort(loggerList);
        for(String loggerName : loggerList)
        {
            result += (disabledLoggers.contains(loggerName) ? "*" : " ") + " " + loggerName + "\n";
        }
        
        return result;
    }
    
    public void log(String loggerName, LogLevel logLevel, List<String> args, boolean collapse) throws LoggerException
    {
        if(!isActive())
        {
            return;
        }
        
        Logger logger = getLogger(loggerName);
        
        String result = formatArguments(args, collapse);
        
        if(logLevel == LogLevel.debug)
        {
            logger.debug(result);
        }
        else if(logLevel == LogLevel.info)
        {
            logger.info(result);
        }
        else if(logLevel == LogLevel.warn)
        {
            logger.warn(result);
        }
        else if(logLevel == LogLevel.trace)
        {
            logger.trace(result);
        }
        else
        {
            logger.error(result);
        }
        
        if(echoMode != EchoMode.off && currentLogLevel.wouldAcceptLogLevel(logLevel) && !disabledLoggers.contains(loggerName))
        {
            agent.getPrinter().startNewLine();
            
            if(echoMode == EchoMode.simple)
            {
                agent.getPrinter().print(result);
            }
            else
            {
                agent.getPrinter().print("[" + logLevel.toString() + " " + getTimestamp() + "] " + loggerName + ": " + result);
            }
            
            agent.getPrinter().flush();
        }
    }
    
    private String formatArguments(List<String> args, boolean collapse)
    {
        if(args.size() > 1)
        {
            String formatString = args.get(0);
            if(formatString.contains("{}"))
            {
                int numFields = (formatString.length() - formatString.replace("{}", "").length()) / 2;
                if(numFields == args.size() - 1)
                {
                    return String.format(formatString.replace("{}", "%s"), args.subList(1, args.size()).toArray(new Object[args.size() - 1]));
                }
            }
        }
        
        return Joiner.on(collapse ? "" : " ").join(args);
    }
    
    public String getTimestamp()
    {
        return this.timestampFormatter.format(new Date(System.currentTimeMillis()));
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
    
    public void setLogLevel(LogLevel logLevel)
    {
        currentLogLevel = logLevel;
    }
    
    public LogLevel getLogLevel()
    {
        return currentLogLevel;
    }
    
    public void setSourceLocationMethod(SourceLocationMethod sourceLocationMethod)
    {
        this.sourceLocationMethod = sourceLocationMethod;
    }
    
    public SourceLocationMethod getSourceLocationMethod()
    {
        return sourceLocationMethod;
    }
    
    public void enableLogger(String name) throws LoggerException
    {
        getLogger(name);
        if(isStrict() && !disabledLoggers.contains(name))
        {
            throw new LoggerException("Logger is not currently disabled (strict mode enabled).");
        }
        disabledLoggers.remove(name);
    }
    
    public void disableLogger(String name) throws LoggerException
    {
        getLogger(name);
        if(isStrict() && disabledLoggers.contains(name))
        {
            throw new LoggerException("Logger is already disabled (strict mode enabled).");
        }
        disabledLoggers.add(name);
    }
    
    public void setAbbreviate(boolean abbreviate)
    {
        this.abbreviate = abbreviate;
    }
    
    public boolean getAbbreviate()
    {
        return abbreviate;
    }
}
