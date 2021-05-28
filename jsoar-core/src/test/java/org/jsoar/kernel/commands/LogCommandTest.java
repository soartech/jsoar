package org.jsoar.kernel.commands;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.io.StringWriter;
import java.util.HashSet;
import java.util.Set;
import java.util.regex.Pattern;
import org.jsoar.kernel.Agent;
import org.jsoar.kernel.LogManager;
import org.jsoar.kernel.LogManager.EchoMode;
import org.jsoar.kernel.LogManager.LogLevel;
import org.jsoar.kernel.LogManager.SourceLocationMethod;
import org.jsoar.kernel.SoarException;
import org.jsoar.util.commands.DefaultInterpreter;
import org.jsoar.util.commands.DefaultSoarCommandContext;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

public class LogCommandTest {

  private Agent agent;
  private StringWriter outputWriter = new StringWriter();
  private LogManager logManager;
  private LogCommand logCommand;

  @Before
  public void setUp() throws Exception {
    agent = new Agent();
    agent.getPrinter().addPersistentWriter(outputWriter);
    logManager = agent.getLogManager();
    logCommand = new LogCommand(agent, new DefaultInterpreter(agent));
  }

  @After
  public void tearDown() throws Exception {
    if (agent != null) {
      agent.dispose();
      agent = null;
    }
  }

  private void clearBuffer() {
    outputWriter.getBuffer().setLength(0);
  }

  @Test
  public void testLogInit() throws Exception {
    Set<String> testSet = new HashSet<String>();
    testSet.add("default");
    assertEquals(testSet, logManager.getLoggerNames());

    logCommand.execute(
        DefaultSoarCommandContext.empty(), new String[] {"log", "--add", "test-logger"});
    testSet.add("test-logger");
    assertEquals(testSet, logManager.getLoggerNames());

    logCommand.execute(DefaultSoarCommandContext.empty(), new String[] {"log", "--init"});
    testSet.clear();
    testSet.add("default");
    assertEquals(testSet, logManager.getLoggerNames());
  }

  @Test
  public void testLogAdd() throws Exception {
    logCommand.execute(
        DefaultSoarCommandContext.empty(), new String[] {"log", "--strict", "disable"});
    assertFalse(logManager.isStrict());

    Set<String> testSet = new HashSet<String>();
    testSet.add("default");
    assertEquals(testSet, logManager.getLoggerNames());

    logCommand.execute(
        DefaultSoarCommandContext.empty(), new String[] {"log", "--add", "test-logger"});
    testSet.add("test-logger");
    assertEquals(testSet, logManager.getLoggerNames());

    logCommand.execute(
        DefaultSoarCommandContext.empty(), new String[] {"log", "--add", "test-logger2"});
    testSet.add("test-logger2");
    assertEquals(testSet, logManager.getLoggerNames());

    logCommand.execute(
        DefaultSoarCommandContext.empty(), new String[] {"log", "--add", "test-logger3"});
    testSet.add("test-logger3");
    assertEquals(testSet, logManager.getLoggerNames());

    logCommand.execute(DefaultSoarCommandContext.empty(), new String[] {"log", "--init"});
    testSet.clear();
    testSet.add("default");
    assertEquals(testSet, logManager.getLoggerNames());

    logCommand.execute(
        DefaultSoarCommandContext.empty(), new String[] {"log", "--add", "test-logger4"});
    testSet.add("test-logger4");
    assertEquals(testSet, logManager.getLoggerNames());
  }

  @Test
  public void testEnableStrictMode() throws Exception {
    logCommand.execute(
        DefaultSoarCommandContext.empty(), new String[] {"log", "--strict", "enable"});
    assertTrue(logManager.isStrict());
  }

  @Test
  public void testDisableStrictMode() throws Exception {
    logCommand.execute(DefaultSoarCommandContext.empty(), new String[] {"log", "--strict", "off"});
    assertFalse(logManager.isStrict());
  }

  @Test
  public void testEnableAbbreviate() throws Exception {
    logCommand.execute(
        DefaultSoarCommandContext.empty(), new String[] {"log", "--abbreviate", "enable"});
    assertTrue(logManager.getAbbreviate());
  }

  @Test
  public void testDisableAbbreviate() throws Exception {
    logCommand.execute(
        DefaultSoarCommandContext.empty(), new String[] {"log", "--abbreviate", "off"});
    assertFalse(logManager.getAbbreviate());
  }

  @Test
  public void testLogAddStrict() throws Exception {
    logCommand.execute(
        DefaultSoarCommandContext.empty(), new String[] {"log", "--strict", "enable"});
    assertTrue(logManager.isStrict());

    Set<String> testSet = new HashSet<String>();
    testSet.add("default");
    assertEquals(testSet, logManager.getLoggerNames());

    logCommand.execute(
        DefaultSoarCommandContext.empty(), new String[] {"log", "--add", "test-logger"});
    testSet.add("test-logger");
    assertEquals(testSet, logManager.getLoggerNames());

    boolean success = false;
    try {
      logCommand.execute(
          DefaultSoarCommandContext.empty(),
          new String[] {"log", "test-logger2", "error", "test-string"});
    } catch (SoarException e) {
      success = true;
    } finally {
      assertTrue(success);
    }

    logCommand.execute(
        DefaultSoarCommandContext.empty(), new String[] {"log", "--add", "test-logger2"});
    testSet.add("test-logger2");
    assertEquals(testSet, logManager.getLoggerNames());

    success = true;
    try {
      logCommand.execute(
          DefaultSoarCommandContext.empty(),
          new String[] {"log", "test-logger2", "error", "test-string"});
    } catch (SoarException e) {
      success = false;
    } finally {
      assertTrue(success);
    }

    success = false;
    try {
      logCommand.execute(
          DefaultSoarCommandContext.empty(), new String[] {"log", "--add", "test-logger"});
    } catch (SoarException e) {
      success = true;
    } finally {
      assertTrue(success);
    }
  }

  @Test
  public void testLogEnableDisableLogger() throws Exception {
    enableLogger("--disable", false);
    enableLogger("--enable", true);
    enableLogger("--no", false);
    enableLogger("--yes", true);
    enableLogger("--off", false);
    enableLogger("--on", true);
  }

  private void enableLogger(String argument, boolean loggerEnabled) throws Exception {
    final String loggerName = "LOGGER-TEST";
    logCommand.execute(
        DefaultSoarCommandContext.empty(), new String[] {"log", argument, loggerName});
    assertEquals(loggerEnabled, !logManager.isDisabledLogger(loggerName));
  }

  @Test
  public void testLogEnableDisable() throws Exception {
    logManager.setActive(true);
    assertTrue(logManager.isActive());

    activateLog("--disable", false);
    activateLog("--enable", true);
    activateLog("--no", false);
    activateLog("--yes", true);
    activateLog("--off", false);
    activateLog("--on", true);
  }

  private void activateLog(String argument, boolean logActive) throws Exception {
    logCommand.execute(DefaultSoarCommandContext.empty(), new String[] {"log", argument});
    assertEquals(logActive, logManager.isActive());
  }

  @Test
  public void testLogLevel() throws Exception {
    logManager.setStrict(false);

    logCommand.execute(
        DefaultSoarCommandContext.empty(),
        new String[] {"log", "test-logger", "info", "test-string"});
    logCommand.execute(
        DefaultSoarCommandContext.empty(),
        new String[] {"log", "test-logger", "debug", "test-string"});
    logCommand.execute(
        DefaultSoarCommandContext.empty(),
        new String[] {"log", "test-logger", "warn", "test-string"});
    logCommand.execute(
        DefaultSoarCommandContext.empty(),
        new String[] {"log", "test-logger", "error", "test-string"});
    logCommand.execute(
        DefaultSoarCommandContext.empty(),
        new String[] {"log", "test-logger", "trace", "test-string"});

    boolean success = false;
    try {
      logCommand.execute(
          DefaultSoarCommandContext.empty(),
          new String[] {"log", "test-logger", "unknown", "test-string"});
    } catch (SoarException e) {
      success = true;
    } finally {
      assertTrue(success);
    }
  }

  @Test
  public void testLogDefault() throws Exception {
    logManager.setStrict(false);

    logCommand.execute(
        DefaultSoarCommandContext.empty(), new String[] {"log", "info", "test-string"});

    boolean success = false;
    try {
      logCommand.execute(
          DefaultSoarCommandContext.empty(), new String[] {"log", "unknown", "test-string"});
    } catch (SoarException e) {
      success = true;
    } finally {
      assertTrue(success);
    }
  }

  @Test
  public void testSetLogLevelToTrace() throws SoarException {
    setLogLevel("trace", LogLevel.trace);
  }

  @Test
  public void testSetLogLevelToDebug() throws SoarException {
    setLogLevel("debug", LogLevel.debug);
  }

  @Test
  public void testSetLogLevelToInfo() throws SoarException {
    setLogLevel("info", LogLevel.info);
  }

  @Test
  public void testSetLogLevelToWarn() throws SoarException {
    setLogLevel("warn", LogLevel.warn);
  }

  @Test
  public void testSetLogLevelToError() throws SoarException {
    setLogLevel("error", LogLevel.error);
  }

  private void setLogLevel(String argument, LogLevel expectedLogLevel) throws SoarException {
    logCommand.execute(
        DefaultSoarCommandContext.empty(), new String[] {"log", "--level", argument});
    assertEquals(expectedLogLevel, logManager.getLogLevel());
  }

  @Test
  public void testSetEchoToBasic() throws SoarException {
    setEchoMode("simple", EchoMode.simple);
  }

  @Test
  public void testLogWithEchoBasic() throws Exception {
    logCommand.execute(DefaultSoarCommandContext.empty(), new String[] {"log", "--echo", "simple"});

    clearBuffer();

    logCommand.execute(
        DefaultSoarCommandContext.empty(),
        new String[] {"log", "info", "This", "is", "a", "simple", "test", "case."});
    assertEquals("\nThis is a simple test case.", outputWriter.toString());
  }

  @Test
  public void testSetSourceLocationMethodToDisk() throws Exception {
    setSourceLocationMethod("disk", SourceLocationMethod.disk);
  }

  @Test
  public void testSetSourceLocationMethodToStack() throws Exception {
    setSourceLocationMethod("stack", SourceLocationMethod.stack);
  }

  @Test
  public void testSetSourceLocationMethodToNone() throws Exception {
    setSourceLocationMethod("none", SourceLocationMethod.none);
  }

  private void setSourceLocationMethod(
      String argument, SourceLocationMethod expectedSourceLocationMethod) throws SoarException {
    logCommand.execute(
        DefaultSoarCommandContext.empty(), new String[] {"log", "--source", argument});
    assertEquals(expectedSourceLocationMethod, logManager.getSourceLocationMethod());
  }

  @Test
  public void testSetEchoToOff() throws Exception {
    setEchoMode("off", EchoMode.off);
  }

  private void setEchoMode(String argument, EchoMode expectedEchoMode) throws SoarException {
    logCommand.execute(DefaultSoarCommandContext.empty(), new String[] {"log", "--echo", argument});
    assertEquals(expectedEchoMode, logManager.getEchoMode());
  }

  @Test
  public void testLogWithEchoOff() throws Exception {
    logCommand.execute(DefaultSoarCommandContext.empty(), new String[] {"log", "--echo", "off"});

    clearBuffer();

    logCommand.execute(
        DefaultSoarCommandContext.empty(),
        new String[] {"log", "info", "This", "is", "a", "simple", "test", "case."});
    assertEquals("", outputWriter.toString());
  }

  @Test
  public void testSetEchoToOn() throws Exception {
    setEchoMode("on", EchoMode.on);
  }

  @Test
  public void testLogWithEchoOn() throws Exception {
    logCommand.execute(DefaultSoarCommandContext.empty(), new String[] {"log", "--echo", "on"});

    clearBuffer();

    logCommand.execute(
        DefaultSoarCommandContext.empty(),
        new String[] {"log", "info", "This", "is", "a", "simple", "test", "case."});
    assertTrue(
        Pattern.matches(
            "^\\n\\[INFO .+?\\] default: This is a simple test case\\.$", outputWriter.toString()));
  }

  /**
   * This is just a performance test for when nothing should be logged. It shouldn't fail unless
   * other tests here also fail.
   *
   * @throws SoarException
   */
  @Test
  public void testPerformance() throws SoarException {
    logManager.setLogLevel(LogLevel.warn);

    // warm up the jvm so we get more stable times
    for (int i = 0; i < 1000; i++) {
      logCommand.execute(
          DefaultSoarCommandContext.empty(),
          new String[] {"log", "trace", "This", "is", "a", "simple", "test", "case."});
    }

    long start = System.currentTimeMillis();
    for (int i = 0; i < 100000; i++) {
      logCommand.execute(
          DefaultSoarCommandContext.empty(),
          new String[] {"log", "trace", "This", "is", "a", "simple", "test", "case."});
    }
    long end = System.currentTimeMillis();

    System.out.println("Total log time: " + (end - start) / 1000.0);
  }
}
