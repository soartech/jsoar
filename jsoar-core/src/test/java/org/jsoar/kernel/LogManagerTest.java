package org.jsoar.kernel;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;
import org.jsoar.kernel.LogManager.LogLevel;
import org.jsoar.kernel.LogManager.LoggerException;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

public class LogManagerTest {

  private Agent agent;

  @Before
  public void setUp() throws Exception {
    agent = new Agent();
  }

  @After
  public void tearDown() throws Exception {
    if (agent != null) {
      agent.dispose();
      agent = null;
    }
  }

  @Test
  public void testLogManagerCreation() {
    LogManager logManager = agent.getLogManager();
    assertNotNull(logManager);
  }

  @Test
  public void testLogManagerInit() throws Exception {
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

  @Test(expected = LoggerException.class)
  public void testAddLoggerThrowsExceptionInStrictModeIfAddingExistingLogger() throws LoggerException {
    // Given a log manager
    LogManager logManager = agent.getLogManager();
    // And log manager is strict
    logManager.setStrict(true);
    // And existing logger "existing-logger";
    logManager.addLogger("existing-logger");

    // When adding logger "existing-logger"
    // Then LoggerException should occur
    logManager.addLogger("existing-logger");
  }

  @Test
  public void testLogAdd() throws Exception {
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
  public void testLogAddStrict() throws Exception {
    LogManager logManager = agent.getLogManager();

    logManager.setStrict(true);
    assertTrue(logManager.isStrict());

    Set<String> testSet = new HashSet<>();
    testSet.add("default");
    assertEquals(testSet, logManager.getLoggerNames());

    logManager.addLogger("test-logger");
    testSet.add("test-logger");
    assertEquals(testSet, logManager.getLoggerNames());

    boolean success = false;
    try {
      logManager.log("test-logger2", LogLevel.error, Collections.singletonList("test-string"), false);
    } catch (LoggerException e) {
      success = true;
    } finally {
      assertTrue(success);
    }

    logManager.addLogger("test-logger2");
    testSet.add("test-logger2");
    assertEquals(testSet, logManager.getLoggerNames());

    success = true;
    try {
      logManager.log("test-logger2", LogLevel.error, Collections.singletonList("test-string"), false);
    } catch (LoggerException e) {
      success = false;
    } finally {
      assertTrue(success);
    }

    success = false;
    try {
      logManager.addLogger("test-logger");
    } catch (LoggerException e) {
      success = true;
    } finally {
      assertTrue(success);
    }
  }

  @Test
  public void testLogEnableDisable() {
    LogManager logManager = agent.getLogManager();

    logManager.setActive(true);
    assertTrue(logManager.isActive());

    logManager.setActive(false);
    assertFalse(logManager.isActive());

    logManager.setActive(true);
    assertTrue(logManager.isActive());
  }

  @Test
  public void testLogStrictEnableDisable() {
    LogManager logManager = agent.getLogManager();

    logManager.setStrict(true);
    assertTrue(logManager.isStrict());

    logManager.setStrict(false);
    assertFalse(logManager.isStrict());

    logManager.setStrict(true);
    assertTrue(logManager.isStrict());
  }
}
