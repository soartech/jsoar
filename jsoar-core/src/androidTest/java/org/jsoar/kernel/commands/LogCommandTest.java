package org.jsoar.kernel.commands;

import android.test.AndroidTestCase;

import org.jsoar.kernel.Agent;
import org.jsoar.kernel.LogManager;
import org.jsoar.kernel.LogManager.EchoMode;
import org.jsoar.kernel.SoarException;
import org.jsoar.util.commands.DefaultInterpreter;
import org.jsoar.util.commands.DefaultSoarCommandContext;

import java.io.StringWriter;
import java.util.HashSet;
import java.util.Set;
import java.util.regex.Pattern;

public class LogCommandTest extends AndroidTestCase
{
	private Agent agent;
	private StringWriter outputWriter = new StringWriter();
	
	@Override
	public void setUp() throws Exception
	{
		agent = new Agent(getContext());
		
		agent.getPrinter().addPersistentWriter(outputWriter);
	}
	
	@Override
	public void tearDown() throws Exception
	{
		if (agent != null)
		{
			agent.dispose();
			agent = null;
		}
	}
	
	private void clearBuffer()
    {
        outputWriter.getBuffer().setLength(0);
    }
	
	public void testLogInit() throws Exception
	{
		LogManager logManager = agent.getLogManager();
		LogCommand logCommand = new LogCommand(agent, new DefaultInterpreter(agent, getContext().getAssets()));
		
		Set<String> testSet = new HashSet<String>();
		testSet.add("default");
		assertTrue(logManager.getLoggerNames().equals(testSet));
		
		logCommand.execute(DefaultSoarCommandContext.empty(), new String[]{"log", "--add", "test-logger"});
		testSet.add("test-logger");
		assertTrue(logManager.getLoggerNames().equals(testSet));
		
		logCommand.execute(DefaultSoarCommandContext.empty(), new String[]{"log", "--init"});
		testSet.clear();
		testSet.add("default");
		assertTrue(logManager.getLoggerNames().equals(testSet));
	}
	
	public void testLogAdd() throws Exception
	{
		LogManager logManager = agent.getLogManager();
		LogCommand logCommand = new LogCommand(agent, new DefaultInterpreter(agent, getContext().getAssets()));
		
		logCommand.execute(DefaultSoarCommandContext.empty(), new String[]{"log", "--strict", "disable"});
		assertFalse(logManager.isStrict());
		
		Set<String> testSet = new HashSet<String>();
		testSet.add("default");
		assertTrue(logManager.getLoggerNames().equals(testSet));
		
		logCommand.execute(DefaultSoarCommandContext.empty(), new String[]{"log", "--add", "test-logger"});
		testSet.add("test-logger");
		assertTrue(logManager.getLoggerNames().equals(testSet));
		
		logCommand.execute(DefaultSoarCommandContext.empty(), new String[]{"log", "--add", "test-logger2"});
		testSet.add("test-logger2");
		assertTrue(logManager.getLoggerNames().equals(testSet));
		
		logCommand.execute(DefaultSoarCommandContext.empty(), new String[]{"log", "--add", "test-logger3"});
		testSet.add("test-logger3");
		assertTrue(logManager.getLoggerNames().equals(testSet));
		
		logCommand.execute(DefaultSoarCommandContext.empty(), new String[]{"log", "--init"});
		testSet.clear();
		testSet.add("default");
		assertTrue(logManager.getLoggerNames().equals(testSet));
		
		logCommand.execute(DefaultSoarCommandContext.empty(), new String[]{"log", "--add", "test-logger4"});
		testSet.add("test-logger4");
		assertTrue(logManager.getLoggerNames().equals(testSet));
	}
	
	public void testLogAddStrict() throws Exception
	{
		LogManager logManager = agent.getLogManager();
		LogCommand logCommand = new LogCommand(agent, new DefaultInterpreter(agent, getContext().getAssets()));
		
		logCommand.execute(DefaultSoarCommandContext.empty(), new String[]{"log", "--strict", "enable"});
		assertTrue(logManager.isStrict());
		
		Set<String> testSet = new HashSet<String>();
		testSet.add("default");
		assertTrue(logManager.getLoggerNames().equals(testSet));
		
		logCommand.execute(DefaultSoarCommandContext.empty(), new String[]{"log", "--add", "test-logger"});
		testSet.add("test-logger");
		assertTrue(logManager.getLoggerNames().equals(testSet));
		
		boolean success = false;
		try
		{
			logCommand.execute(DefaultSoarCommandContext.empty(), new String[]{"log", "test-logger2", "error", "test-string"});
		}
		catch (SoarException e)
		{
			success = true;
		}
		finally
		{
			assertTrue(success);
		}
		
		logCommand.execute(DefaultSoarCommandContext.empty(), new String[]{"log", "--add", "test-logger2"});
		testSet.add("test-logger2");
		assertTrue(logManager.getLoggerNames().equals(testSet));
		
		success = true;
		try
		{
			logCommand.execute(DefaultSoarCommandContext.empty(), new String[]{"log", "test-logger2", "error", "test-string"});
		}
		catch (SoarException e)
		{
			success = false;
		}
		finally
		{
			assertTrue(success);
		}
		
		success = false;
		try
		{
			logCommand.execute(DefaultSoarCommandContext.empty(), new String[]{"log", "--add", "test-logger"});
		}
		catch (SoarException e)
		{
			success = true;
		}
		finally
		{
			assertTrue(success);
		}
	}
	
	public void testLogEnableDisable() throws Exception
	{
		LogManager logManager = agent.getLogManager();
		LogCommand logCommand = new LogCommand(agent, new DefaultInterpreter(agent, getContext().getAssets()));
		
		logManager.setActive(true);
		assertTrue(logManager.isActive());
		
		logCommand.execute(DefaultSoarCommandContext.empty(), new String[]{"log", "--disable"});
		assertTrue(!logManager.isActive());
		
		logCommand.execute(DefaultSoarCommandContext.empty(), new String[]{"log", "--enable"});
		assertTrue(logManager.isActive());
		
		logCommand.execute(DefaultSoarCommandContext.empty(), new String[]{"log", "--no"});
		assertTrue(!logManager.isActive());
		
		logCommand.execute(DefaultSoarCommandContext.empty(), new String[]{"log", "--yes"});
		assertTrue(logManager.isActive());
		
		logCommand.execute(DefaultSoarCommandContext.empty(), new String[]{"log", "--off"});
		assertTrue(!logManager.isActive());
		
		logCommand.execute(DefaultSoarCommandContext.empty(), new String[]{"log", "--on"});
		assertTrue(logManager.isActive());
	}
	
	public void testLogLevel() throws Exception
	{
		LogManager logManager = agent.getLogManager();
		LogCommand logCommand = new LogCommand(agent, new DefaultInterpreter(agent, getContext().getAssets()));
		
		logManager.setStrict(false);
		
		logCommand.execute(DefaultSoarCommandContext.empty(), new String[]{"log", "test-logger", "info", "test-string"});
		logCommand.execute(DefaultSoarCommandContext.empty(), new String[]{"log", "test-logger", "debug", "test-string"});
		logCommand.execute(DefaultSoarCommandContext.empty(), new String[]{"log", "test-logger", "warn", "test-string"});
		logCommand.execute(DefaultSoarCommandContext.empty(), new String[]{"log", "test-logger", "error", "test-string"});
		logCommand.execute(DefaultSoarCommandContext.empty(), new String[]{"log", "test-logger", "trace", "test-string"});
		
		boolean success = false;
		try
		{
			logCommand.execute(DefaultSoarCommandContext.empty(), new String[]{"log", "test-logger", "unknown", "test-string"});
		}
		catch (SoarException e)
		{
			success = true;
		}
		finally
		{
			assertTrue(success);
		}
	}
	
	public void testLogDefault() throws Exception
	{
		LogManager logManager = agent.getLogManager();
		LogCommand logCommand = new LogCommand(agent, new DefaultInterpreter(agent, getContext().getAssets()));
		
		logManager.setStrict(false);
		
		logCommand.execute(DefaultSoarCommandContext.empty(), new String[]{"log", "info", "test-string"});
		
		boolean success = false;
		try
		{
			logCommand.execute(DefaultSoarCommandContext.empty(), new String[]{"log", "unknown", "test-string"});
		}
		catch (SoarException e)
		{
			success = true;
		}
		finally
		{
			assertTrue(success);
		}
	}
	
	public void testEchoBasic() throws Exception
	{
		LogManager logManager = agent.getLogManager();
		LogCommand logCommand = new LogCommand(agent, new DefaultInterpreter(agent, getContext().getAssets()));
		
		logCommand.execute(DefaultSoarCommandContext.empty(), new String[]{"log", "--echo", "simple"});
		assertEquals(logManager.getEchoMode(), EchoMode.simple);
		
		clearBuffer();
		
		logCommand.execute(DefaultSoarCommandContext.empty(), new String[]{"log", "info", "This", "is", "a", "simple", "test", "case."});
		assertTrue(outputWriter.toString().equals("\nThis is a simple test case."));
	}
	
	public void testEchoOff() throws Exception
	{
		LogManager logManager = agent.getLogManager();
		LogCommand logCommand = new LogCommand(agent, new DefaultInterpreter(agent, getContext().getAssets()));
		
		logCommand.execute(DefaultSoarCommandContext.empty(), new String[]{"log", "--echo", "off"});
		assertEquals(logManager.getEchoMode(), EchoMode.off);
		
		clearBuffer();
		
		logCommand.execute(DefaultSoarCommandContext.empty(), new String[]{"log", "info", "This", "is", "a", "simple", "test", "case."});
		assertTrue(outputWriter.toString().equals(""));
	}
	
	public void testEchoOn() throws Exception
	{
		LogManager logManager = agent.getLogManager();
		LogCommand logCommand = new LogCommand(agent, new DefaultInterpreter(agent, getContext().getAssets()));
		
		logCommand.execute(DefaultSoarCommandContext.empty(), new String[]{"log", "--echo", "on"});
		assertEquals(logManager.getEchoMode(), EchoMode.on);
		
		clearBuffer();
		
		logCommand.execute(DefaultSoarCommandContext.empty(), new String[]{"log", "info", "This", "is", "a", "simple", "test", "case."});
		assertTrue(Pattern.matches("^\\n\\[INFO .+?\\] default: This is a simple test case\\.$", outputWriter.toString()));
	}
}
