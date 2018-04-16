package org.jsoar.kernel.commands;

import android.test.AndroidTestCase;

import junit.framework.Assert;

import org.jsoar.kernel.Agent;
import org.jsoar.kernel.SoarException;
import org.jsoar.util.commands.DefaultSoarCommandContext;

import java.io.StringWriter;

public class PrintCommandTest extends AndroidTestCase
{
    private Agent agent;

    private PrintCommand command;

    private StringWriter outputWriter;

    private String prodTest = "test (state <s> ^superstate nil) -->";

    @Override
    public void setUp() throws Exception
    {
        this.agent = new Agent(false, getContext());
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

    @Override
    public void tearDown() throws Exception
    {
        if (this.agent != null)
        {
            this.agent.dispose();
            this.agent = null;
        }
    }

    public void testDefaultDepth() throws SoarException
    {
        assertEquals(command.getDefaultDepth(), 1);
        command.setDefaultDepth(5);
        assertEquals(command.getDefaultDepth(), 5);
    }

    public void testZeroDefaultDepth()
    {
        try {
            command.setDefaultDepth(0);
            Assert.fail("Should have thrown exception");
        } catch (SoarException e) {
            e.printStackTrace();
        }
    }

    public void testNegativeDefaultDepth()
    {
        try {
            command.setDefaultDepth(-1);
            Assert.fail("Should have thrown exception");
        } catch (SoarException e) {
            e.printStackTrace();
        }
    }

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

    public void testVarprintNotImplemented()
    {
        try {
            command.execute(DefaultSoarCommandContext.empty(), new String[] { "print", "--varprint" });
            Assert.fail("Should have thrown exception");
        } catch (SoarException e) {
            e.printStackTrace();
        }
    }

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
