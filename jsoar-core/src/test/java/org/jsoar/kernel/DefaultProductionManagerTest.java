/*
 * Copyright (c) 2009 Dave Ray <daveray@gmail.com>
 *
 * Created on May 22, 2009
 */
package org.jsoar.kernel;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertSame;

import org.jsoar.JSoarTest;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

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
    @BeforeEach
    public void setUp() throws Exception
    {
        super.setUp();
        this.agent = new Agent();
        this.pm = this.agent.getProductions();
    }
    
    /**
     * @throws java.lang.Exception
     */
    @AfterEach
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
