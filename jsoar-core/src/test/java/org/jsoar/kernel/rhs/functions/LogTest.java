package org.jsoar.kernel.rhs.functions;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.io.StringWriter;
import java.util.regex.Pattern;
import org.jsoar.kernel.Agent;
import org.jsoar.kernel.LogManager;
import org.jsoar.kernel.LogManager.EchoMode;
import org.jsoar.kernel.RunType;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

public class LogTest {
  private Agent agent;
  private StringWriter outputWriter = new StringWriter();

  @Before
  public void setUp() throws Exception {
    agent = new Agent();

    agent.getPrinter().addPersistentWriter(outputWriter);
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
  public void testBasicLog() throws Exception {
    LogManager logManager = agent.getLogManager();
    logManager.setEchoMode(EchoMode.simple);

    agent
        .getProductions()
        .loadProduction("test (state <s> ^superstate nil) --> (log info |Simple test|)");

    clearBuffer();

    agent.runFor(1, RunType.DECISIONS);

    Pattern regex = Pattern.compile("^Simple test$", Pattern.MULTILINE);
    assertTrue(regex.matcher(outputWriter.toString()).find());
  }

  @Test
  public void testDecoratedLog() throws Exception {
    LogManager logManager = agent.getLogManager();
    logManager.setEchoMode(EchoMode.on);

    agent
        .getProductions()
        .loadProduction("test (state <s> ^superstate nil) --> (log info |Simple test|)");

    clearBuffer();

    agent.runFor(1, RunType.DECISIONS);

    Pattern regex = Pattern.compile("^\\[INFO .+?\\] default: Simple test$", Pattern.MULTILINE);
    assertTrue(regex.matcher(outputWriter.toString()).find());
  }

  @Test
  public void testLogNoTrace() throws Exception {
    LogManager logManager = agent.getLogManager();
    logManager.setEchoMode(EchoMode.off);

    agent
        .getProductions()
        .loadProduction("test (state <s> ^superstate nil) --> (log info |Simple test|)");

    clearBuffer();

    agent.runFor(1, RunType.DECISIONS);

    Pattern regex = Pattern.compile("Simple test", Pattern.MULTILINE);
    assertFalse(regex.matcher(outputWriter.toString()).find());
  }

  @Test
  public void testLogReplacement() throws Exception {
    LogManager logManager = agent.getLogManager();
    logManager.setEchoMode(EchoMode.simple);

    agent
        .getProductions()
        .loadProduction(
            "test (state <s> ^superstate nil) --> (log info |{}, {}, {}| Friends Romans countrymen)");

    clearBuffer();

    agent.runFor(1, RunType.DECISIONS);

    Pattern regex = Pattern.compile("^Friends, Romans, countrymen$", Pattern.MULTILINE);
    assertTrue(regex.matcher(outputWriter.toString()).find());
  }

  @Test
  public void testLogConcatenation() throws Exception {
    LogManager logManager = agent.getLogManager();
    logManager.setEchoMode(EchoMode.simple);

    agent
        .getProductions()
        .loadProduction(
            "test (state <s> ^superstate nil) --> (log info Right now |I am testing | a simple state: | | <s>)");

    clearBuffer();

    agent.runFor(1, RunType.DECISIONS);

    Pattern regex = Pattern.compile("^RightnowI am testing asimplestate: S1$", Pattern.MULTILINE);
    assertTrue(regex.matcher(outputWriter.toString()).find());
  }
}
