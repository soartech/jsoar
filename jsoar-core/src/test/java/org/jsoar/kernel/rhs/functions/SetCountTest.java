package org.jsoar.kernel.rhs.functions;

import static org.junit.Assert.assertTrue;

import java.io.StringWriter;
import java.util.List;
import org.jsoar.kernel.Agent;
import org.jsoar.kernel.RunType;
import org.jsoar.kernel.SoarProperties;
import org.jsoar.kernel.symbols.Symbol;
import org.jsoar.util.ByRef;
import org.junit.Before;
import org.junit.Test;

public class SetCountTest {

  private Agent agent;
  ByRef<Boolean> matched;

  @Before
  public void setUp() throws Exception {
    this.agent = new Agent();
    this.matched = ByRef.create(Boolean.FALSE);
    agent
        .getRhsFunctions()
        .registerHandler(
            new StandaloneRhsFunctionHandler("match") {

              @Override
              public Symbol execute(RhsFunctionContext context, List<Symbol> arguments)
                  throws RhsFunctionException {
                matched.value = true;
                return null;
              }
            });

    // A production to create some set to count
    agent
        .getProductions()
        .loadProduction(
            ""
                + "createSet\n"
                + "(state <s> ^superstate nil)\n"
                + "-->\n"
                + "(<s> ^to-count <tc>)"
                + "(<tc> ^foo one ^bar one ^foo two)");
  }

  @Test
  public void TestNoArgs() throws Exception {
    // A production to call set-count with bad args
    agent
        .getProductions()
        .loadProduction(
            ""
                + "countSet\n"
                + "(state <s> ^superstate nil )\n"
                + "-->\n"
                + "(<s> ^count (set-count))");

    agent.getProperties().set(SoarProperties.WAITSNC, true);
    StringWriter sw = new StringWriter();
    agent.getPrinter().pushWriter(sw);
    agent.runFor(2, RunType.DECISIONS);

    assertTrue(sw.toString().contains("Error executing RHS function"));
  }

  @Test
  public void TestOneArg() throws Exception {
    // A production to call set-count with one arg, which will always return 0
    agent
        .getProductions()
        .loadProduction(
            ""
                + "countSet\n"
                + "(state <s> ^superstate nil ^to-count <tc>)\n"
                + "-->\n"
                + "(<s> ^count (set-count <tc>))");

    // Finally a production to validate that the count is correct
    agent
        .getProductions()
        .loadProduction("" + "testStructure\n" + "(state <s> ^count 0)\n" + "-->\n" + "(match)");

    agent.getProperties().set(SoarProperties.WAITSNC, true);
    agent.runFor(2, RunType.DECISIONS);

    assertTrue(matched.value);
  }

  @Test
  public void TestTwoArgs() throws Exception {
    // A production to call set-count with two args: <tc> and foo,
    // which means count how many WMEs have the identifier <tc> with the attribute "foo"
    agent
        .getProductions()
        .loadProduction(
            ""
                + "countSet\n"
                + "(state <s> ^superstate nil ^to-count <tc>)\n"
                + "-->\n"
                + "(<s> ^count (set-count <tc> foo))");

    // Finally a production to validate that the count is correct
    agent
        .getProductions()
        .loadProduction("" + "testStructure\n" + "(state <s> ^count 2)\n" + "-->\n" + "(match)");

    agent.getProperties().set(SoarProperties.WAITSNC, true);
    agent.runFor(2, RunType.DECISIONS);

    assertTrue(matched.value);
  }

  @Test
  public void TestThreeArgs() throws Exception {
    // A production to call set-count with bad args
    agent
        .getProductions()
        .loadProduction(
            ""
                + "countSet\n"
                + "(state <s> ^superstate nil )\n"
                + "-->\n"
                + "(<s> ^count (set-count <s> foo bar))");

    agent.getProperties().set(SoarProperties.WAITSNC, true);
    StringWriter sw = new StringWriter();
    agent.getPrinter().pushWriter(sw);
    agent.runFor(2, RunType.DECISIONS);

    assertTrue(sw.toString().contains("Error executing RHS function"));
  }
}
