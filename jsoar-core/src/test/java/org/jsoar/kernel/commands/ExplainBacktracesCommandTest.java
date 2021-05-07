/*
 * Copyright (c) 2010 Dave Ray <daveray@gmail.com>
 *
 * Created on Jun 11, 2010
 */
package org.jsoar.kernel.commands;

import org.jsoar.kernel.Agent;
import org.jsoar.kernel.SoarException;
import org.jsoar.util.commands.DefaultSoarCommandContext;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.io.StringWriter;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;


class ExplainBacktracesCommandTest {

    private Agent agent;
    private StringWriter outputWriter = new StringWriter();
    private ExplainBacktracesCommand explainCommand;

    @BeforeEach
    void setUp() throws Exception {
        agent = new Agent();
        agent.getPrinter().addPersistentWriter(outputWriter);
        explainCommand = new ExplainBacktracesCommand(agent);
    }

    @AfterEach
    void tearDown() {
        if (agent != null) {
            agent.dispose();
            agent = null;
        }
    }

    @Test
    void testExplain() throws SoarException {
        explainCommand.execute(DefaultSoarCommandContext.empty(), new String[]{"explain-backtraces"});
        assertEquals("No chunks/justifications built yet!\n", outputWriter.toString());
    }

    @Test
    void testExplainPrintFull() throws SoarException {
        explainCommand.execute(DefaultSoarCommandContext.empty(), new String[]{"eb","-f","test"});
        assertEquals("""
                Could not find the chunk.  Maybe explain was not on when it was created.
                To turn on explain: save-backtraces --enable before the chunk is created.
                """, outputWriter.toString());
    }

    @Test
    void testExplainListChunks() throws SoarException {
        explainCommand.execute(DefaultSoarCommandContext.empty(), new String[]{"eb","test"});
        assertEquals("""
                Could not find the chunk.  Maybe explain was not on when it was created.
                To turn on explain: save-backtraces --enable before the chunk is created.
                """, outputWriter.toString());
    }

    @Test
    void testExplainSpecificChunk() throws SoarException {
        explainCommand.execute(DefaultSoarCommandContext.empty(), new String[]{"eb","-c","0","test"});
        assertEquals("""
                Could not find the chunk.  Maybe explain was not on when it was created.
                To turn on explain: save-backtraces --enable before the chunk is created.
                """, outputWriter.toString());
    }

    @Test
    void testThrowsExceptionForNonNumericCondition() {
        assertThrows(SoarException.class, () ->
                explainCommand.execute(DefaultSoarCommandContext.empty(), new String[]{"eb", "-c", "b"})
        );
    }

    @Test
    void testThrowsExceptionForMissingNumericCondition() {
        assertThrows(SoarException.class, () ->
                explainCommand.execute(DefaultSoarCommandContext.empty(), new String[]{"eb", "-c"})
        );
    }

}
