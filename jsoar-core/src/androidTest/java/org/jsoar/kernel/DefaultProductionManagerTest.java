/*
 * Copyright (c) 2009 Dave Ray <daveray@gmail.com>
 *
 * Created on May 22, 2009
 */
package org.jsoar.kernel;

import org.jsoar.JSoarTest;

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
    @Override
    public void setUp() throws Exception
    {
        super.setUp();
        this.agent = new Agent(getContext());
        this.pm = this.agent.getProductions();
    }

    /**
     * @throws java.lang.Exception
     */
    @Override
    public void tearDown() throws Exception
    {
    }

    /**
     * Test method for {@link org.jsoar.kernel.DefaultProductionManager#getProduction(java.lang.String)}.
     */
    public void testGetProduction() throws Exception
    {
        final Production p = pm.loadProduction("   testGetProduction (state <s> ^superstate nil) --> (<s> ^foo bar)");
        assertNotNull(p);
        assertSame(p, pm.getProduction("testGetProduction"));
        
    }

}
