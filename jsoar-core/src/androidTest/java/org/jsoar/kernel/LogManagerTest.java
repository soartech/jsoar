package org.jsoar.kernel;

import android.test.AndroidTestCase;

import org.jsoar.kernel.LogManager.LogLevel;
import org.jsoar.kernel.LogManager.LoggerException;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

public class LogManagerTest extends AndroidTestCase{
	
	private Agent agent;
	
	@Override
	public void setUp() throws Exception
	{
		agent = new Agent(getContext());
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
	
	public void testLogManagerCreation() throws Exception
	{
		LogManager logManager = agent.getLogManager();
		assertNotNull(logManager);
	}
	
	public void testLogManagerInit() throws Exception
	{
		LogManager logManager = agent.getLogManager();

		Set<String> testSet = new HashSet<String>();
		testSet.add("default");
		assertTrue(logManager.getLoggerNames().equals(testSet));
		
		logManager.addLogger("test-logger");
		testSet.add("test-logger");
		assertTrue(logManager.getLoggerNames().equals(testSet));
		
		logManager.init();
		testSet.clear();
		testSet.add("default");
		assertTrue(logManager.getLoggerNames().equals(testSet));
	}
	
	public void testLogAdd() throws Exception
	{
		LogManager logManager = agent.getLogManager();
		
		logManager.setStrict(false);
		assertFalse(logManager.isStrict());
		
		Set<String> testSet = new HashSet<String>();
		testSet.add("default");
		assertTrue(logManager.getLoggerNames().equals(testSet));
		
		logManager.addLogger("test-logger");
		testSet.add("test-logger");
		assertTrue(logManager.getLoggerNames().equals(testSet));
		
		logManager.addLogger("test-logger2");
		testSet.add("test-logger2");
		assertTrue(logManager.getLoggerNames().equals(testSet));
		
		logManager.addLogger("test-logger3");
		testSet.add("test-logger3");
		assertTrue(logManager.getLoggerNames().equals(testSet));
		
		logManager.init();
		testSet.clear();
		testSet.add("default");
		assertTrue(logManager.getLoggerNames().equals(testSet));
		
		logManager.addLogger("test-logger4");
		testSet.add("test-logger4");
		assertTrue(logManager.getLoggerNames().equals(testSet));
	}
	
	public void testLogAddStrict() throws Exception
	{
		LogManager logManager = agent.getLogManager();
		
		logManager.setStrict(true);
		assertTrue(logManager.isStrict());
		
		Set<String> testSet = new HashSet<String>();
		testSet.add("default");
		assertTrue(logManager.getLoggerNames().equals(testSet));
		
		logManager.addLogger("test-logger");
		testSet.add("test-logger");
		assertTrue(logManager.getLoggerNames().equals(testSet));
		
		boolean success = false;
		try
		{
			logManager.log("test-logger2", LogLevel.error, Arrays.asList("test-string"), false);
		}
		catch (LoggerException e)
		{
			success = true;
		}
		finally
		{
			assertTrue(success);
		}
		
		logManager.addLogger("test-logger2");
		testSet.add("test-logger2");
		assertTrue(logManager.getLoggerNames().equals(testSet));
		
		success = true;
		try
		{
			logManager.log("test-logger2", LogLevel.error, Arrays.asList("test-string"), false);
		}
		catch (LoggerException e)
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
			logManager.addLogger("test-logger");
		}
		catch (LoggerException e)
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
		
		logManager.setActive(true);
		assertTrue(logManager.isActive());
		
		logManager.setActive(false);
		assertFalse(logManager.isActive());
		
		logManager.setActive(true);
		assertTrue(logManager.isActive());
	}
	
	public void testLogStrictEnableDisable() throws Exception
	{
		LogManager logManager = agent.getLogManager();
		
		logManager.setStrict(true);
		assertTrue(logManager.isStrict());
		
		logManager.setStrict(false);
		assertFalse(logManager.isStrict());
		
		logManager.setStrict(true);
		assertTrue(logManager.isStrict());
	}
}
