package org.jsoar.kernel.commands;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.io.StringWriter;

import org.jsoar.kernel.Agent;
import org.jsoar.kernel.SoarException;
import org.jsoar.util.commands.DefaultSoarCommandContext;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

public class PrintCommandTest
{
    private Agent agent;

    private PrintCommand command;

    private StringWriter outputWriter;

    private String prodTest = "test (state <s> ^superstate nil) -->";

    @Before
    public void setUp() throws Exception
    {
        this.agent = new Agent(false);
        this.agent.getPrinter().addPersistentWriter(
                outputWriter = new StringWriter());
        this.agent.getTrace().disableAll();
        
        // Normally this shouldn't be called after and should just be initialized
        // with everything else but because .initialize() adds a \n to the output,
        // this screws up the output.
        // - ALT
        
        this.agent.initialize();

        this.agent.getProductions().loadProduction(prodTest);

        command = new PrintCommand(agent);
    }

    @After
    public void tearDown() throws Exception
    {
        if (this.agent != null)
        {
            this.agent.dispose();
            this.agent = null;
        }
    }

    @Test
    public void testDefaultDepth() throws SoarException
    {
        assertEquals(command.getDefaultDepth(), 1);
        command.setDefaultDepth(5);
        assertEquals(command.getDefaultDepth(), 5);
    }

    @Test(expected = SoarException.class)
    public void testZeroDefaultDepth() throws SoarException
    {
        command.setDefaultDepth(0);
    }

    @Test(expected = SoarException.class)
    public void testNegativeDefaultDepth() throws SoarException
    {
        command.setDefaultDepth(-1);
    }

    @Test
    public void testPrintS1() throws SoarException
    {
        command.execute(DefaultSoarCommandContext.empty(), new String[] { "print", "s1" });
        System.out.println("'" + outputWriter.toString() + "'");
        assertTrue(outputWriter
                .toString()
                .startsWith(
                        //"(S1 ^io I1 ^reward-link R1 ^smem S2 ^superstate nil ^type state)"));
                        "(S1 ^epmem E1 ^io I1 ^reward-link R1 ^smem S2 ^superstate nil ^type state)"));
        
    }

    @Test
    public void testPrintS1Depth2() throws SoarException
    {
        clearBuffer();
        
        command.execute(DefaultSoarCommandContext.empty(), new String[] { "print", "s1", "--depth", "2" });
        String a = outputWriter.toString();
        System.out.println("'" + a + "'");

        clearBuffer();

        command.setDefaultDepth(2);
        command.execute(DefaultSoarCommandContext.empty(), new String[] { "print", "s1" });
        String b = outputWriter.toString();
        System.out.println("'" + b + "'");

        assertEquals(a, b);
    }

    private void clearBuffer()
    {
        outputWriter.getBuffer().setLength(0);
    }

    @Test
    public void testVarprintNotImplemented() throws SoarException
    {
        StringWriter sw = new StringWriter();
        agent.getPrinter().pushWriter(sw);
        command.execute(DefaultSoarCommandContext.empty(), new String[] { "print", "--varprint" });
        agent.getPrinter().popWriter();
        assertTrue(sw.toString().contains("not implemented"));
    }

    @Test
    public void testPrintAll() throws SoarException
    {
        command.execute(DefaultSoarCommandContext.empty(), new String[] { "print" });
        String a = outputWriter.toString();
        System.out.println("'" + a + "'");

        clearBuffer();

        command.execute(DefaultSoarCommandContext.empty(), new String[] { "print", "--all" });
        String b = outputWriter.toString();
        System.out.println("'" + b + "'");

        assertEquals(a, b);
        assertTrue(a.startsWith("test"));
    }

}
