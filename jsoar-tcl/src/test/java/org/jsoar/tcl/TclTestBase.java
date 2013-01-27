/*
 * Copyright (c) 2008  Dave Ray <daveray@gmail.com>
 *
 * Created on Dec 15, 2008
 */
package org.jsoar.tcl;

import org.jsoar.kernel.Agent;
import org.jsoar.kernel.SoarException;
import org.junit.After;
import org.junit.Before;

/**
 * Base class for TCL tests that use a common set up / tear down harness
 * and take <code>.soar</code> input via /resources.
 * 
 * @author ray
 */
public abstract class TclTestBase
{
    protected Agent agent;
    protected SoarTclInterface ifc;

    protected void sourceTestFile(Class<? extends TclTestBase> childClass, String name) throws SoarException
    {
        ifc.source(childClass.getResource("/" + childClass.getName().replace('.', '/')  + "_" + name));
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
}
