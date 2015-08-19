/*
 * Copyright (c) 2010 Dave Ray <daveray@gmail.com>
 *
 * Created on Jul 4, 2010
 */
package org.jsoar.kernel.wma;

import org.jsoar.kernel.FunctionalTestHarness;

import java.io.StringWriter;

/**
 * @author ray
 */
public class WmaFunctionalTests extends FunctionalTestHarness
{
    public void testSimpleActivation() throws Exception
    {
        runTest("testSimpleActivation", 2679);
        final StringWriter sw = new StringWriter();
        agent.getPrinter().pushWriter(sw);
        agent.getInterpreter().eval("print s1 -i");
        final String result = sw.toString();
        
        assert(result.contains("S1 ^o-from-a true [-1.5]"));
        assert(result.contains("S1 ^o-from-o true [-1.9]"));
        assert(result.contains("S1 ^i-from-i true [1.0]"));
        assert(!result.contains("S1 ^o-from-i2"));
    }
}
