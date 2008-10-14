/*
 * Copyright (c) 2008  Dave Ray <daveray@gmail.com>
 *
 * Created on Sep 18, 2008
 */
package org.jsoar.tcl;


import static org.junit.Assert.*;

import java.util.ArrayList;
import java.util.List;

import org.jsoar.kernel.Agent;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

/**
 * @author ray
 */
public class SoarTclInterfaceTest
{
    private SoarTclInterface ifc;
    
    /**
     * @throws java.lang.Exception
     */
    @Before
    public void setUp() throws Exception
    {
        final Agent agent = new Agent();
        ifc = new SoarTclInterface(agent);
        agent.initialize();
    }

    /**
     * @throws java.lang.Exception
     */
    @After
    public void tearDown() throws Exception
    {
        ifc.dispose();
        ifc = null;
    }
    
    @Test
    public void testSourceResource() throws SoarTclException
    {
        ifc.sourceResource("/" + SoarTclInterfaceTest.class.getCanonicalName().replace('.', '/') + "_sourceResource.soar");
        
        assertNotNull(ifc.getAgent().getProduction("top-state*propose*wait"));
    }

    @Test
    public void testSrandCommand() throws SoarTclException
    {
        ifc.eval("srand 98765");
        List<Integer> firstInts = new ArrayList<Integer>();
        for(int i = 0; i < 1000; ++i)
        {
            firstInts.add(ifc.getAgent().getRandom().nextInt());
        }
        ifc.eval("srand 98765");
        List<Integer> secondInts = new ArrayList<Integer>();
        for(int i = 0; i < 1000; ++i)
        {
            secondInts.add(ifc.getAgent().getRandom().nextInt());
        }
        assertEquals(firstInts, secondInts);
    }
}
