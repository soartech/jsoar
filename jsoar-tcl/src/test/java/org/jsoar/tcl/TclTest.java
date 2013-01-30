/*
 * Copyright (c) 2008  Dave Ray <daveray@gmail.com>
 *
 * Created on Dec 15, 2008
 */
package org.jsoar.tcl;

import java.net.URL;

import org.jsoar.kernel.Agent;
import org.jsoar.kernel.SoarException;
import org.junit.After;
import org.junit.Before;

/**
 * @author ray
 */
public class TclTest
{
    protected Agent agent;
    protected SoarTclInterface ifc;

    protected void sourceTestFile(Class<? extends TclTest> childClass, String name) throws SoarException
    {
        ifc.source(getSourceTestFile(childClass, name));
    }
    
    protected URL getSourceTestFile(Class<? extends TclTest> childClass, String name)
    {
        return childClass.getResource("/" + childClass.getName().replace('.', '/')  + "_" + name);
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
