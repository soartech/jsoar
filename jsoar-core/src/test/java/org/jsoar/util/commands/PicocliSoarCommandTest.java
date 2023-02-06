package org.jsoar.util.commands;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.io.StringWriter;
import java.util.ArrayList;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeoutException;

import org.jsoar.kernel.Agent;
import org.jsoar.kernel.SoarException;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import picocli.CommandLine;

public class PicocliSoarCommandTest
{
    
    private Agent agent;
    private StringWriter outputWriter = new StringWriter();
    
    @BeforeEach
    public void setUp() throws Exception
    {
        this.agent = new Agent();
        this.agent.getPrinter().addPersistentWriter(this.outputWriter);
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
    
    private void clearBuffer()
    {
        outputWriter.getBuffer().setLength(0);
    }

    /**
     * There was an issue where sometimes the fields from a previous command execution were not cleared when the command was run again,
     * which would result in the new command being run with some old values. It appeared to be tied to autocompletion.
     * This test confirms that it is working now -- previously, the "qmemory --clear" command would output the same results
     * as the previous "qmemory --set a foo" command instead of outputting nothing (because it was rerunning the command with the previous
     * args). Note that this issue is not specific to the qmemory command; it's just easy to reproduce there.
     */
    @Test
    public void testFieldsReset() throws InterruptedException, ExecutionException, TimeoutException, SoarException
    {

        // this command should echo it's last argument to output
        this.autoComplete("qmemory --set a foo");
        this.agent.getInterpreter().eval("qmemory --set a foo");
        
        // check output is what's expected
        assertEquals("foo", this.outputWriter.toString().trim());
        clearBuffer();
        
        // this command should not echo anything to output
        this.agent.getInterpreter().eval("qmemory --clear");
        
        // confirm there is no output
        // as of this writing, the output is not empty -- it appears to remember its args from the previous execution of the command and does that one again
        assertEquals(0, this.outputWriter.toString().length());
    }
    
    /**
     * This is similar to how the jsoar-debugger performs autocompletion, but without all the UI parts
     * Something about this seems to cause the command to not get reset between executions
     */
    protected void autoComplete(String command) {
        CommandLine commandLine = this.agent.getInterpreter().findCommand(command);
        
        ArrayList<CharSequence> longResults = new ArrayList<>();
        String[] parts = command.split(" ");
        
        // if this line is commented out, the problem goes away (of course, then autocompletion doesn't work in the jsoar-debugger)
        picocli.AutoComplete.complete(commandLine.getCommandSpec(), parts, 1, parts[1].length(), command.length(), longResults);
    }
}
