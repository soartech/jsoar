/*
 * Copyright (c) 2010 Dave Ray <daveray@gmail.com>
 *
 * Created on Jun 11, 2010
 */
package org.jsoar.kernel.learning;


import static org.junit.Assert.*;

import java.util.List;

import org.jsoar.kernel.FunctionalTestHarness;
import org.jsoar.util.adaptables.Adaptables;
import org.junit.Test;

public class ExplainTest extends FunctionalTestHarness
{
    @Test(timeout=10000)
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
        assertTrue("Explanation should only have 2 conditions", c.conds.next.next == null);
        assertTrue("Explanation should only have 1 action", c.actions.next == null);
    }
}
