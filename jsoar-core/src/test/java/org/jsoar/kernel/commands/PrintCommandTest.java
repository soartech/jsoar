package org.jsoar.kernel.commands;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.StringWriter;

import org.jsoar.kernel.Agent;
import org.jsoar.kernel.SoarException;
import org.jsoar.util.commands.DefaultSoarCommandContext;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

public class PrintCommandTest
{
    private Agent agent;
    
    private PrintCommand command;
    
    private StringWriter outputWriter;
    
    private String prodTest = "test (state <s> ^superstate nil) -->";
    
    @BeforeEach
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
    
    @AfterEach
    public void tearDown() throws Exception
    {
        if(this.agent != null)
        {
            this.agent.dispose();
            this.agent = null;
        }
    }
    
    @Test
    public void testDefaultDepth() throws SoarException
    {
        assertEquals(command.getCommand().getDefaultDepth(), 1);
        command.getCommand().setDefaultDepth(5);
        assertEquals(command.getCommand().getDefaultDepth(), 5);
    }
    
    @Test
    public void testZeroDefaultDepth() throws SoarException
    {
        assertThrows(SoarException.class, () -> command.getCommand().setDefaultDepth(0));
    }
    
    @Test
    public void testNegativeDefaultDepth()
    {
        assertThrows(SoarException.class, () -> command.getCommand().setDefaultDepth(-1));
    }
    
    @Test
    public void testPrintS1() throws SoarException
    {
        command.execute(DefaultSoarCommandContext.empty(), new String[] { "print", "s1" });
        System.out.println("'" + outputWriter.toString() + "'");
        assertTrue(outputWriter
                .toString()
                .startsWith(
                        // "(S1 ^io I1 ^reward-link R1 ^smem S2 ^superstate nil ^type state)"));
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
        
        command.getCommand().setDefaultDepth(2);
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
    public void testVarprintNotImplemented()
    {
        assertThrows(SoarException.class, () -> command.execute(DefaultSoarCommandContext.empty(), new String[] { "print", "--varprint" }));
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
