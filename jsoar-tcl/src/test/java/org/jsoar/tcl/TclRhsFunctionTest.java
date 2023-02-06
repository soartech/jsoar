/*
 * Copyright (c) 2008  Dave Ray <daveray@gmail.com>
 *
 * Created on Dec 15, 2008
 */
package org.jsoar.tcl;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.jsoar.kernel.RunType;
import org.junit.jupiter.api.Test;

/**
 * @author ray
 */
public class TclRhsFunctionTest extends TclTestBase
{
    @Test
    public void testExecute() throws Exception
    {
        sourceTestFile(getClass(), "testExecute.soar");
        
        agent.runFor(1, RunType.DECISIONS);
        
        assertEquals("this is a \\ test", ifc.eval("set value"));
    }
}
