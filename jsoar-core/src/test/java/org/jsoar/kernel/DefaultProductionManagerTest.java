/*
 * Copyright (c) 2009 Dave Ray <daveray@gmail.com>
 *
 * Created on May 22, 2009
 */
package org.jsoar.kernel;

import static org.junit.Assert.*;

import org.jsoar.JSoarTest;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

/**
 * @author ray
 */
public class DefaultProductionManagerTest extends JSoarTest
{
    private Agent agent;
    private ProductionManager pm;
    /**
     * @throws java.lang.Exception
     */
    @Before
    public void setUp() throws Exception
    {
        super.setUp();
        this.agent = new Agent();
        this.pm = this.agent.getProductions();
        this.agent.initialize();
    }

    /**
     * @throws java.lang.Exception
     */
    @After
    public void tearDown() throws Exception
    {
    }

    /**
     * Test method for {@link org.jsoar.kernel.DefaultProductionManager#getProduction(java.lang.String)}.
     */
    @Test
    public void testGetProduction() throws Exception
    {
        final Production p = pm.loadProduction("   testGetProduction (state <s> ^superstate nil) --> (<s> ^foo bar)");
        assertNotNull(p);
        assertSame(p, pm.getProduction("testGetProduction"));
        
    }

}
