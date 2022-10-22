package org.jsoar.kernel.commands;

import static org.junit.Assert.assertEquals;

import java.io.StringWriter;
import java.util.ArrayList;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeoutException;

import org.jsoar.kernel.Agent;
import org.jsoar.kernel.SoarException;
import org.jsoar.runtime.ThreadedAgent;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import picocli.CommandLine;
import picocli.CommandLine.Model.CommandSpec;

public class QMemoryCommandTest
{
    
    private Agent agent;
    private StringWriter outputWriter = new StringWriter();
    
    @Before
    public void setUp() throws Exception
    {
        this.agent = new Agent();
        this.agent.getPrinter().addPersistentWriter(this.outputWriter);
    }

    @After
    public void tearDown() throws Exception
    {
        if(this.agent != null)
        {
            this.agent.dispose();
            this.agent = null;
        }
    }
    
    private void clearBuffer()
    {
        outputWriter.getBuffer().setLength(0);
    }

    @Test
    public void testClear() throws InterruptedException, ExecutionException, TimeoutException, SoarException
    {

        // this command should echo it's last argument to output
        executeCommand("qmemory --set a foo");
        
        // check output is what's expected
        assertEquals("foo", this.outputWriter.toString().trim());
        clearBuffer();
        
        // this command should not echo anything to output
        executeCommand("qmemory --clear");
        
        // confirm there is no output
        // as of this writing, the output is not empty -- it appears to remember its args from the previous execution of the command and does that one again
        assertEquals(0, this.outputWriter.toString().length());
    }
    
    protected void executeCommand(String command) throws SoarException {
        this.autoComplete(command);
        this.agent.getInterpreter().eval(command);
    }
    
    /**
     * This is similar to how the jsoar-debugger performs autocompletion, but without all the UI parts
     * Something about this seems to cause the command to not get reset between executions
     */
    protected void autoComplete(String command) {
        CommandLine commandLine = this.agent.getInterpreter().findCommand(command);
        
        // we need to get a "fresh" command spec each time to avoid accidentally reusing one that may already be in use
        CommandSpec commandSpec = CommandSpec.forAnnotatedObject(commandLine.getCommandSpec().userObject());
        
        ArrayList<CharSequence> longResults = new ArrayList<>();
        String[] parts = command.split(" ");
        
        // if this line is commented out, the problem goes away (of course, then autocompletion doesn't work in the jsoar-debugger)
        picocli.AutoComplete.complete(commandSpec, parts, 1, parts[1].length(), command.length(), longResults);
    }
}
