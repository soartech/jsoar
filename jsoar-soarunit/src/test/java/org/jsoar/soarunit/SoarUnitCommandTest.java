package org.jsoar.soarunit;

import static org.junit.Assert.assertTrue;

import java.io.StringWriter;
import org.jsoar.kernel.Agent;
import org.jsoar.kernel.SoarException;
import org.junit.Test;

public class SoarUnitCommandTest {

  @Test
  public void TestSoarUnitCommand() throws SoarException {
    Agent agent = new Agent();
    agent.getInterpreter().eval("pushd src/test/resources/example-unittest");
    agent.getPrinter().pushWriter(new StringWriter());
    agent.getInterpreter().eval("soarunit -R");
    String result = agent.getPrinter().popWriter().toString();
    assertTrue(result.contains("4 passed, 0 failed"));
  }
}
