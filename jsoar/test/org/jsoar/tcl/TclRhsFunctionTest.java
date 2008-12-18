/*
 * Copyright (c) 2008  Dave Ray <daveray@gmail.com>
 *
 * Created on Dec 15, 2008
 */
package org.jsoar.tcl;

import static org.junit.Assert.assertEquals;

import org.jsoar.kernel.Agent;
import org.jsoar.kernel.RunType;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

/**
 * @author ray
 */
public class TclRhsFunctionTest
{
    private Agent agent;
    private SoarTclInterface ifc;

    private void sourceTestFile(String name) throws SoarTclException
    {
        ifc.sourceResource("/" + TclRhsFunctionTest.class.getName().replace('.', '/')  + "_" + name);
    }
    
    /**
     * @throws java.lang.Exception
     */
    @Before
    public void setUp() throws Exception
    {
        this.agent = new Agent();
        this.ifc = SoarTclInterface.findOrCreate(agent);
        this.agent.initialize();
    }

    /**
     * @throws java.lang.Exception
     */
    @After
    public void tearDown() throws Exception
    {
        SoarTclInterface.dispose(ifc);
    }

    @Test
    public void testExecute() throws Exception
    {
        sourceTestFile("testExecute.soar");
        
        agent.runFor(1, RunType.DECISIONS);
        
        assertEquals("this is a \\ test", ifc.eval("set value"));
    }

}
