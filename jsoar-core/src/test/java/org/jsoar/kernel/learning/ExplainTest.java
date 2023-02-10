/*
 * Copyright (c) 2010 Dave Ray <daveray@gmail.com>
 *
 * Created on Jun 11, 2010
 */
package org.jsoar.kernel.learning;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;
import java.util.concurrent.TimeUnit;

import org.jsoar.kernel.FunctionalTestHarness;
import org.jsoar.util.adaptables.Adaptables;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Timeout;

public class ExplainTest extends FunctionalTestHarness
{
    @Test
    @Timeout(value = 10, unit = TimeUnit.SECONDS)
    public void testExplainBacktrace() throws Exception
    {
        // See ExplainTest_testExplainBacktrace.soar for details.
        runTest("testExplainBacktrace", 4);
        
        final Explain explain = Adaptables.adapt(agent, Explain.class);
        assertNotNull(explain);
        
        final List<ExplainChunk> chunks = explain.getChunkExplanations();
        assertEquals(1, chunks.size());
        final ExplainChunk c = chunks.get(0);
        assertEquals("chunk-1*d4*opnochange*1", c.name);
        assertTrue(c.conds.next.next == null, "Explanation should only have 2 conditions");
        assertTrue(c.actions.next == null, "Explanation should only have 1 action");
    }
}
