package org.jsoar.kernel.rhs.functions;

import org.jsoar.JSoarTest;
import org.jsoar.kernel.Agent;
import org.jsoar.kernel.RunType;
import org.jsoar.kernel.SoarProperties;
import org.jsoar.kernel.symbols.Symbol;
import org.jsoar.util.ByRef;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.StringWriter;
import java.util.List;

public class SizeTest extends JSoarTest 
{
    private Agent agent;
    ByRef<Boolean> matched;

    @BeforeEach
    void initAgent() throws Exception 
    {
        this.agent = new Agent();
        this.matched = ByRef.create(Boolean.FALSE);
        agent.getRhsFunctions().registerHandler(new StandaloneRhsFunctionHandler("match") {

            @Override
            public Symbol execute(RhsFunctionContext context, List<Symbol> arguments) throws RhsFunctionException {
                matched.value = true;
                return null;
            }
        });

    }

    @Test
    void TestNoArgs() throws Exception 
    {
        // A production to call set-count with bad args
        agent.getProductions().loadProduction("" +
                "countSize\n" +
                "(state <s> ^superstate nil )\n" +
                "-->\n" +
                "(<s> ^count-size (size))");

        agent.getProperties().set(SoarProperties.WAITSNC, true);
        StringWriter sw = new StringWriter();
        agent.getPrinter().pushWriter(sw);
        agent.runFor(2, RunType.DECISIONS);

        assertTrue(sw.toString().contains("Error executing RHS function"));
    }

    @Test
    void TestTwoArgs() throws Exception 
    {
        // A production to create ^to-count & ^to-count2
        agent.getProductions().loadProduction("" +
                "createSizeTest\n" +
                "(state <s> ^superstate nil)\n" +
                "-->\n" +
                "(<s> ^to-count <tc> ^to-count2 <tc2>) " +
                "(<tc> ^foo one)" +
                "(<tc2> ^foo2 one)");

        // A production to count ^to-count with size function
        agent.getProductions().loadProduction("" +
                "countSize\n" +
                "(state <s> ^superstate nil ^to-count <tc> ^to-count2 <tc2>)\n" +
                "-->\n" +
                "(<s> ^count (size <tc> <tc2>))");

        agent.getProperties().set(SoarProperties.WAITSNC, true);
        StringWriter sw = new StringWriter();
        agent.getPrinter().pushWriter(sw);
        agent.runFor(2, RunType.DECISIONS);

        assertTrue(sw.toString().contains("Error executing RHS function"));
    }

    @Test
    void TestOneArgsCountOne() throws Exception 
    {
        // A production to create ^to-count with one WME
        agent.getProductions().loadProduction("" +
                "createSizeTest\n" +
                "(state <s> ^superstate nil)\n" +
                "-->\n" +
                "(<s> ^to-count <tc>)" +
                "(<tc> ^foo one)");

        // A production to count ^to-count with size function
        agent.getProductions().loadProduction("" +
                "countSize\n" +
                "(state <s> ^superstate nil ^to-count <tc>)\n" +
                "-->\n" +
                "(<s> ^count (size <tc>))");

        // Finally a production to validate that the size count is correct
        agent.getProductions().loadProduction("" +
                "testStructure\n" +
                "(state <s> ^count 1)\n" +
                "-->\n" +
                "(match)");

        agent.getProperties().set(SoarProperties.WAITSNC, true);
        agent.runFor(2, RunType.DECISIONS);

        assertTrue(matched.value);
    }

    @Test
    void TestOneArgsCountTwo() throws Exception 
    {
        // A production to create ^to-count with two WMEs
        agent.getProductions().loadProduction("" +
                "createSizeTest\n" +
                "(state <s> ^superstate nil)\n" +
                "-->\n" +
                "(<s> ^to-count <tc>)" +
                "(<tc> ^foo one ^bar two)");

        // A production to count ^to-count with size function
        agent.getProductions().loadProduction("" +
                "countSize\n" +
                "(state <s> ^superstate nil ^to-count <tc>)\n" +
                "-->\n" +
                "(<s> ^count (size <tc>))");

        // Finally a production to validate that the size count is correct
        agent.getProductions().loadProduction("" +
                "testStructure\n" +
                "(state <s> ^count 2)\n" +
                "-->\n" +
                "(match)");

        agent.getProperties().set(SoarProperties.WAITSNC, true);
        agent.runFor(2, RunType.DECISIONS);

        assertTrue(matched.value);
    }

    @Test
    void TestOneArgsCountThree() throws Exception 
    {
        // A production to create ^to-count with three WMEs
        agent.getProductions().loadProduction("" +
                "createSizeTest\n" +
                "(state <s> ^superstate nil)\n" +
                "-->\n" +
                "(<s> ^to-count <tc>)" +
                "(<tc> ^foo one ^bar two ^foo three)");

        // A production to count ^to-count with size function
        agent.getProductions().loadProduction("" +
                "countSize\n" +
                "(state <s> ^superstate nil ^to-count <tc>)\n" +
                "-->\n" +
                "(<s> ^count (size <tc>))");

        // Finally a production to validate that the size count is correct
        agent.getProductions().loadProduction("" +
                "testStructure\n" +
                "(state <s> ^count 3)\n" +
                "-->\n" +
                "(match)");

        agent.getProperties().set(SoarProperties.WAITSNC, true);
        agent.runFor(2, RunType.DECISIONS);

        assertTrue(matched.value);
    }

}
