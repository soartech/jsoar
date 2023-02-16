package org.jsoar.kernel.commands;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.StringWriter;
import java.util.HashSet;
import java.util.Set;
import java.util.regex.Pattern;

import org.jsoar.kernel.Agent;
import org.jsoar.kernel.LogManager;
import org.jsoar.kernel.LogManager.EchoMode;
import org.jsoar.kernel.LogManager.LogLevel;
import org.jsoar.kernel.SoarException;
import org.jsoar.util.commands.DefaultInterpreter;
import org.jsoar.util.commands.DefaultSoarCommandContext;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;

public class LogCommandTest
{
    private Agent agent;
    private StringWriter outputWriter = new StringWriter();
    private LogManager logManager;
    private LogCommand logCommand;
    
    @BeforeEach
    public void setUp() throws Exception
    {
        agent = new Agent();
        agent.getPrinter().addPersistentWriter(outputWriter);
        logManager = agent.getLogManager();
        logCommand = new LogCommand(agent, new DefaultInterpreter(agent));
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
    
    private void clearBuffer()
    {
        outputWriter.getBuffer().setLength(0);
    }
    
    @Test
    public void testLogInit() throws Exception
    {
        Set<String> testSet = new HashSet<>();
        testSet.add("default");
        assertEquals(testSet, logManager.getLoggerNames());
        
        logCommand.execute(DefaultSoarCommandContext.empty(), new String[] { "log", "--add", "test-logger" });
        testSet.add("test-logger");
        assertEquals(testSet, logManager.getLoggerNames());
        
        logCommand.execute(DefaultSoarCommandContext.empty(), new String[] { "log", "--init" });
        testSet.clear();
        testSet.add("default");
        assertEquals(testSet, logManager.getLoggerNames());
    }
    
    @Test
    public void testLogAdd() throws Exception
    {
        logCommand.execute(DefaultSoarCommandContext.empty(), new String[] { "log", "--strict", "disable" });
        assertFalse(logManager.isStrict());
        
        Set<String> testSet = new HashSet<>();
        testSet.add("default");
        assertEquals(testSet, logManager.getLoggerNames());
        
        logCommand.execute(DefaultSoarCommandContext.empty(), new String[] { "log", "--add", "test-logger" });
        testSet.add("test-logger");
        assertEquals(testSet, logManager.getLoggerNames());
        
        logCommand.execute(DefaultSoarCommandContext.empty(), new String[] { "log", "--add", "test-logger2" });
        testSet.add("test-logger2");
        assertEquals(testSet, logManager.getLoggerNames());
        
        logCommand.execute(DefaultSoarCommandContext.empty(), new String[] { "log", "--add", "test-logger3" });
        testSet.add("test-logger3");
        assertEquals(testSet, logManager.getLoggerNames());
        
        logCommand.execute(DefaultSoarCommandContext.empty(), new String[] { "log", "--init" });
        testSet.clear();
        testSet.add("default");
        assertEquals(testSet, logManager.getLoggerNames());
        
        logCommand.execute(DefaultSoarCommandContext.empty(), new String[] { "log", "--add", "test-logger4" });
        testSet.add("test-logger4");
        assertEquals(testSet, logManager.getLoggerNames());
    }
    
    @Test
    public void testLogAddStrict() throws Exception
    {
        logCommand.execute(DefaultSoarCommandContext.empty(), new String[] { "log", "--strict", "enable" });
        assertTrue(logManager.isStrict());
        
        Set<String> testSet = new HashSet<>();
        testSet.add("default");
        assertEquals(testSet, logManager.getLoggerNames());
        
        logCommand.execute(DefaultSoarCommandContext.empty(), new String[] { "log", "--add", "test-logger" });
        testSet.add("test-logger");
        assertEquals(testSet, logManager.getLoggerNames());
        
        assertThrows(SoarException.class, () -> logCommand.execute(DefaultSoarCommandContext.empty(), new String[] { "log", "test-logger2", "error", "test-string" }));
        
        logCommand.execute(DefaultSoarCommandContext.empty(), new String[] { "log", "--add", "test-logger2" });
        testSet.add("test-logger2");
        assertEquals(testSet, logManager.getLoggerNames());
        
        // should not throw an exception
        logCommand.execute(DefaultSoarCommandContext.empty(), new String[] { "log", "test-logger2", "error", "test-string" });
        
        assertThrows(SoarException.class, () -> logCommand.execute(DefaultSoarCommandContext.empty(), new String[] { "log", "--add", "test-logger" }));
    }
    
    @Test
    public void testLogEnableDisable() throws Exception
    {
        logManager.setActive(true);
        assertTrue(logManager.isActive());
        
        logCommand.execute(DefaultSoarCommandContext.empty(), new String[] { "log", "--disable" });
        assertTrue(!logManager.isActive());
        
        logCommand.execute(DefaultSoarCommandContext.empty(), new String[] { "log", "--enable" });
        assertTrue(logManager.isActive());
        
        logCommand.execute(DefaultSoarCommandContext.empty(), new String[] { "log", "--no" });
        assertTrue(!logManager.isActive());
        
        logCommand.execute(DefaultSoarCommandContext.empty(), new String[] { "log", "--yes" });
        assertTrue(logManager.isActive());
        
        logCommand.execute(DefaultSoarCommandContext.empty(), new String[] { "log", "--off" });
        assertTrue(!logManager.isActive());
        
        logCommand.execute(DefaultSoarCommandContext.empty(), new String[] { "log", "--on" });
        assertTrue(logManager.isActive());
    }
    
    @Test
    public void testLogLevel() throws Exception
    {
        logManager.setStrict(false);
        
        logCommand.execute(DefaultSoarCommandContext.empty(), new String[] { "log", "test-logger", "info", "test-string" });
        logCommand.execute(DefaultSoarCommandContext.empty(), new String[] { "log", "test-logger", "debug", "test-string" });
        logCommand.execute(DefaultSoarCommandContext.empty(), new String[] { "log", "test-logger", "warn", "test-string" });
        logCommand.execute(DefaultSoarCommandContext.empty(), new String[] { "log", "test-logger", "error", "test-string" });
        logCommand.execute(DefaultSoarCommandContext.empty(), new String[] { "log", "test-logger", "trace", "test-string" });
        
        assertThrows(SoarException.class, () -> logCommand.execute(DefaultSoarCommandContext.empty(), new String[] { "log", "test-logger", "unknown", "test-string" }));
    }
    
    @Test
    public void testLogDefault() throws Exception
    {
        logManager.setStrict(false);
        
        logCommand.execute(DefaultSoarCommandContext.empty(), new String[] { "log", "info", "test-string" });
        
        assertThrows(SoarException.class, () -> logCommand.execute(DefaultSoarCommandContext.empty(), new String[] { "log", "unknown", "test-string" }));
    }
    
    @Test
    public void testEchoBasic() throws Exception
    {
        logCommand.execute(DefaultSoarCommandContext.empty(), new String[] { "log", "--echo", "simple" });
        assertEquals(logManager.getEchoMode(), EchoMode.simple);
        
        clearBuffer();
        
        logCommand.execute(DefaultSoarCommandContext.empty(), new String[] { "log", "info", "This", "is", "a", "simple", "test", "case." });
        assertEquals("\nThis is a simple test case.", outputWriter.toString());
    }
    
    @Test
    public void testEchoOff() throws Exception
    {
        logCommand.execute(DefaultSoarCommandContext.empty(), new String[] { "log", "--echo", "off" });
        assertEquals(logManager.getEchoMode(), EchoMode.off);
        
        clearBuffer();
        
        logCommand.execute(DefaultSoarCommandContext.empty(), new String[] { "log", "info", "This", "is", "a", "simple", "test", "case." });
        assertEquals("", outputWriter.toString());
    }
    
    @Test
    public void testEchoOn() throws Exception
    {
        logCommand.execute(DefaultSoarCommandContext.empty(), new String[] { "log", "--echo", "on" });
        assertEquals(logManager.getEchoMode(), EchoMode.on);
        
        clearBuffer();
        
        logCommand.execute(DefaultSoarCommandContext.empty(), new String[] { "log", "info", "This", "is", "a", "simple", "test", "case." });
        assertTrue(Pattern.matches("^\\n\\[INFO .+?\\] default: This is a simple test case\\.$", outputWriter.toString()));
    }
    
    /**
     * This is just a performance test for when nothing should be logged. It shouldn't fail unless other tests here also fail.
     * 
     * @throws SoarException
     */
    @Test
    @Disabled
    public void testPerformance() throws SoarException
    {
        logManager.setLogLevel(LogLevel.warn);
        
        // warm up the jvm so we get more stable times
        for(int i = 0; i < 1000; i++)
        {
            logCommand.execute(DefaultSoarCommandContext.empty(), new String[] { "log", "trace", "This", "is", "a", "simple", "test", "case." });
        }
        
        long start = System.currentTimeMillis();
        for(int i = 0; i < 100000; i++)
        {
            logCommand.execute(DefaultSoarCommandContext.empty(), new String[] { "log", "trace", "This", "is", "a", "simple", "test", "case." });
        }
        long end = System.currentTimeMillis();
        
        System.out.println("Total log time: " + (end - start) / 1000.0);
        
    }
}
