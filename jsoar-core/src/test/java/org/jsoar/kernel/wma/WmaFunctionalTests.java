/*
 * Copyright (c) 2010 Dave Ray <daveray@gmail.com>
 *
 * Created on Jul 4, 2010
 */
package org.jsoar.kernel.wma;

import static org.junit.Assert.assertEquals;

import java.io.StringWriter;

import org.jsoar.kernel.FunctionalTestHarness;
import org.junit.Test;

/**
 * @author ray
 */
public class WmaFunctionalTests extends FunctionalTestHarness
{
    @Test
    public void testSimpleActivation() throws Exception
    {
        runTest("testSimpleActivation", 2680);
        StringWriter sw = new StringWriter();
        agent.getPrinter().pushWriter(sw);
        agent.getInterpreter().eval("print s1 -i");
        final String result = sw.toString();
        
        assert(result.indexOf("S1 ^o-from-a true [-1.5]") >= 0);
        assert(result.indexOf("S1 ^o-from-o true [-1.9]") >= 0);
        assert(result.indexOf("S1 ^i-from-i true [1.0]") >= 0);
        assertEquals(result.indexOf("S1 ^o-from-i2"), -1);
    }
}
