package org.jsoar.kernel;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

import org.jsoar.kernel.LogManager.LogLevel;
import org.jsoar.kernel.LogManager.LoggerException;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

public class LogManagerTest
{
    
    private Agent agent;
    
    @BeforeEach
    public void setUp() throws Exception
    {
        agent = new Agent();
    }
    
    @AfterEach
    public void tearDown() throws Exception
    {
        if(agent != null)
        {
            agent.dispose();
            agent = null;
        }
    }
    
    @Test
    public void testLogManagerCreation() throws Exception
    {
        LogManager logManager = agent.getLogManager();
        assertNotNull(logManager);
    }
    
    @Test
    public void testLogManagerInit() throws Exception
    {
        LogManager logManager = agent.getLogManager();
        
        Set<String> testSet = new HashSet<>();
        testSet.add("default");
        assertEquals(testSet, logManager.getLoggerNames());
        
        logManager.addLogger("test-logger");
        testSet.add("test-logger");
        assertEquals(testSet, logManager.getLoggerNames());
        
        logManager.init();
        testSet.clear();
        testSet.add("default");
        assertEquals(testSet, logManager.getLoggerNames());
    }
    
    @Test
    public void testLogAdd() throws Exception
    {
        LogManager logManager = agent.getLogManager();
        
        logManager.setStrict(false);
        assertFalse(logManager.isStrict());
        
        Set<String> testSet = new HashSet<>();
        testSet.add("default");
        assertEquals(testSet, logManager.getLoggerNames());
        
        logManager.addLogger("test-logger");
        testSet.add("test-logger");
        assertEquals(testSet, logManager.getLoggerNames());
        
        logManager.addLogger("test-logger2");
        testSet.add("test-logger2");
        assertEquals(testSet, logManager.getLoggerNames());
        
        logManager.addLogger("test-logger3");
        testSet.add("test-logger3");
        assertEquals(testSet, logManager.getLoggerNames());
        
        logManager.init();
        testSet.clear();
        testSet.add("default");
        assertEquals(testSet, logManager.getLoggerNames());
        
        logManager.addLogger("test-logger4");
        testSet.add("test-logger4");
        assertEquals(testSet, logManager.getLoggerNames());
    }
    
    @Test
    public void testLogAddStrict() throws Exception
    {
        LogManager logManager = agent.getLogManager();
        
        logManager.setStrict(true);
        assertTrue(logManager.isStrict());
        
        Set<String> testSet = new HashSet<>();
        testSet.add("default");
        assertEquals(testSet, logManager.getLoggerNames());
        
        logManager.addLogger("test-logger");
        testSet.add("test-logger");
        assertEquals(testSet, logManager.getLoggerNames());
        
        assertThrows(LoggerException.class, () -> logManager.log("test-logger2", LogLevel.error, Arrays.asList("test-string"), false));
        
        logManager.addLogger("test-logger2");
        testSet.add("test-logger2");
        assertEquals(testSet, logManager.getLoggerNames());
        
        // shouldn't throw an exceptin
        logManager.log("test-logger2", LogLevel.error, Arrays.asList("test-string"), false);
        
        assertThrows(LoggerException.class, () -> logManager.addLogger("test-logger"));
    }
    
    @Test
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
    
    @Test
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
