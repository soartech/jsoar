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

public class TrimTest extends JSoarTest {
    private Agent agent;
    ByRef<Boolean> matched;

    @BeforeEach
    void initAgent() throws Exception {
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
    void testNoArgs() throws Exception {
        // A production to call trim with bad args
        agent.getProductions().loadProduction("" +
                "trimMessage\n" +
                "(state <s> ^superstate nil )\n" +
                "-->\n" +
                "(<s> ^trimmed (trim))");

        agent.getProperties().set(SoarProperties.WAITSNC, true);
        StringWriter sw = new StringWriter();
        agent.getPrinter().pushWriter(sw);
        agent.runFor(2, RunType.DECISIONS);

        assertTrue(sw.toString().contains("Error executing RHS function"));
    }

    @Test
    void testTwoArgs() throws Exception {
        // A production to create ^message & ^message2
        agent.getProductions().loadProduction("" +
                "createMessages\n" +
                "(state <s> ^superstate nil)\n" +
                "-->\n" +
                "(<s> ^message |message| ^message2 |message2|)");

        // A production to trim with two args which leads to an error
        agent.getProductions().loadProduction("" +
                "trimMessage\n" +
                "(state <s> ^superstate nil ^message <m1> ^message2 <m2>)\n" +
                "-->\n" +
                "(<s> ^trimmed (trim <m1> <m2>))");

        agent.getProperties().set(SoarProperties.WAITSNC, true);
        StringWriter sw = new StringWriter();
        agent.getPrinter().pushWriter(sw);
        agent.runFor(2, RunType.DECISIONS);

        assertTrue(sw.toString().contains("Error executing RHS function"));
    }

    @Test
    void testWithLeadingWhiteSpace() throws Exception {
        // A production to create ^message with leading white space
        agent.getProductions().loadProduction("" +
                "createMessages\n" +
                "(state <s> ^superstate nil)\n" +
                "-->\n" +
                "(<s> ^message | this is a test message| )");

        // A production to trim the ^message
        agent.getProductions().loadProduction("" +
                "trimMessage\n" +
                "(state <s> ^superstate nil ^message <m1> )\n" +
                "-->\n" +
                "(<s> ^trimmed (trim <m1> ))");

        // Finally a production to validate that the trim function is correct
        agent.getProductions().loadProduction("" +
                "testStructure\n" +
                "(state <s> ^trimmed |this is a test message| )\n" +
                "-->\n" +
                "(match)");

        agent.getProperties().set(SoarProperties.WAITSNC, true);
        agent.runFor(2, RunType.DECISIONS);

        assertTrue(matched.value);
    }

    @Test
    void testWithMultipleLeadingWhiteSpaces() throws Exception {
        // A production to create ^message with multiple leading white spaces
        agent.getProductions().loadProduction("" +
                "createMessages\n" +
                "(state <s> ^superstate nil)\n" +
                "-->\n" +
                "(<s> ^message |     this is a test message| )");

        // A production to trim the ^message
        agent.getProductions().loadProduction("" +
                "trimMessage\n" +
                "(state <s> ^superstate nil ^message <m1> )\n" +
                "-->\n" +
                "(<s> ^trimmed (trim <m1> ))");

        // Finally a production to validate that the trim function is correct
        agent.getProductions().loadProduction("" +
                "testStructure\n" +
                "(state <s> ^trimmed |this is a test message| )\n" +
                "-->\n" +
                "(match)");

        agent.getProperties().set(SoarProperties.WAITSNC, true);
        agent.runFor(2, RunType.DECISIONS);

        assertTrue(matched.value);
    }

    @Test
    void testWithTrailingWhiteSpace() throws Exception {
        // A production to create ^message with trailing white space
        agent.getProductions().loadProduction("" +
                "createMessages\n" +
                "(state <s> ^superstate nil)\n" +
                "-->\n" +
                "(<s> ^message |this is a test message | )");

        // A production to trim the ^message
        agent.getProductions().loadProduction("" +
                "trimMessage\n" +
                "(state <s> ^superstate nil ^message <m1> )\n" +
                "-->\n" +
                "(<s> ^trimmed (trim <m1> ))");

        // Finally a production to validate that the trim function is correct
        agent.getProductions().loadProduction("" +
                "testStructure\n" +
                "(state <s> ^trimmed |this is a test message| )\n" +
                "-->\n" +
                "(match)");

        agent.getProperties().set(SoarProperties.WAITSNC, true);
        agent.runFor(2, RunType.DECISIONS);

        assertTrue(matched.value);
    }

    @Test
    void testWithMultipleTrailingWhiteSpaces() throws Exception {
        // A production to create ^message with multiple trailing white spaces
        agent.getProductions().loadProduction("" +
                "createMessages\n" +
                "(state <s> ^superstate nil)\n" +
                "-->\n" +
                "(<s> ^message |this is a test message    | )");

        // A production to trim the ^message
        agent.getProductions().loadProduction("" +
                "trimMessage\n" +
                "(state <s> ^superstate nil ^message <m1> )\n" +
                "-->\n" +
                "(<s> ^trimmed (trim <m1> ))");

        // Finally a production to validate that the trim function is correct
        agent.getProductions().loadProduction("" +
                "testStructure\n" +
                "(state <s> ^trimmed |this is a test message| )\n" +
                "-->\n" +
                "(match)");

        agent.getProperties().set(SoarProperties.WAITSNC, true);
        agent.runFor(2, RunType.DECISIONS);

        assertTrue(matched.value);
    }

    @Test
    void testWithNoLeadingAndTrailingWhiteSpaces() throws Exception {
        // A production to create ^message with no leading or trailing white spaces
        agent.getProductions().loadProduction("" +
                "createMessages\n" +
                "(state <s> ^superstate nil)\n" +
                "-->\n" +
                "(<s> ^message |this is a test message| )");

        // A production to trim the ^message
        agent.getProductions().loadProduction("" +
                "trimMessage\n" +
                "(state <s> ^superstate nil ^message <m1> )\n" +
                "-->\n" +
                "(<s> ^trimmed (trim <m1> ))");

        // Finally a production to validate that the trim function is correct
        agent.getProductions().loadProduction("" +
                "testStructure\n" +
                "(state <s> ^trimmed |this is a test message| )\n" +
                "-->\n" +
                "(match)");

        agent.getProperties().set(SoarProperties.WAITSNC, true);
        agent.runFor(2, RunType.DECISIONS);

        assertTrue(matched.value);
    }

    @Test
    void testWithNonStringSymbol() throws Exception {

        // A production to create a non string symbol
        agent.getProductions().loadProduction("" +
                "createNonString\n" +
                "(state <s> ^superstate nil)\n" +
                "-->\n" +
                "(<s> ^foobars <fbs>)" +
                "(<fbs> ^foo one ^bar two)");

        // A production to trim the ^foobars
        agent.getProductions().loadProduction("" +
                "trimMessage\n" +
                "(state <s> ^superstate nil ^foobars <fbs> )\n" +
                "-->\n" +
                "(<s> ^trimmed (trim <fbs> ))");

        agent.getProperties().set(SoarProperties.WAITSNC, true);
        StringWriter sw = new StringWriter();
        agent.getPrinter().pushWriter(sw);
        agent.runFor(2, RunType.DECISIONS);

        assertTrue(sw.toString().contains("Error executing RHS function"));
    }

}
