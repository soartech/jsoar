/*
 * Copyright (c) 2008  Dave Ray <daveray@gmail.com>
 *
 * Created on Sep 18, 2008
 */
package org.jsoar.tcl;


import static org.junit.Assert.*;

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
        
        assertNotNull(ifc.getAgent().syms.find_sym_constant("top-state*propose*wait").production);
    }

}
