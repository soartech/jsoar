/*
 * Copyright (c) 2010 Dave Ray <daveray@gmail.com>
 *
 * Created on Jun 11, 2010
 */
package org.jsoar.kernel.learning;


import junit.framework.Assert;

import org.jsoar.kernel.FunctionalTestHarness;
import org.jsoar.util.adaptables.Adaptables;

import java.util.List;

public class ExplainTest extends FunctionalTestHarness
{
    public void testExplainBacktrace() throws Exception
    {
        // See ExplainTest_testExplainBacktrace.soar for details.
        runTest("testExplainBacktrace", 4);
        
        final Explain explain = Adaptables.adapt(agent, Explain.class);
        Assert.assertNotNull(explain);
        
        final List<ExplainChunk> chunks = explain.getChunkExplanations();
        Assert.assertEquals(1, chunks.size());
        final ExplainChunk c = chunks.get(0);
        Assert.assertEquals("chunk-1*d4*opnochange*1", c.name);
        Assert.assertTrue("Explanation should only have 2 conditions", c.conds.next.next == null);
        Assert.assertTrue("Explanation should only have 1 action", c.actions.next == null);
    }
}
