/*
 * Copyright (c) 2009 Dave Ray <daveray@gmail.com>
 *
 * Created on Jul 18, 2009
 */
package org.jsoar.kernel;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.net.URL;
import java.util.List;

import org.jsoar.kernel.ProductionFinder.Options;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/**
 * @author ray
 */
class ProductionFinderTest
{
    private Agent agent;
    
    /**
     * @throws java.lang.Exception
     */
    @BeforeEach
    void setUp() throws Exception
    {
        this.agent = new Agent();
    }
    
    /**
     * @throws java.lang.Exception
     */
    @AfterEach
    void tearDown() throws Exception
    {
        this.agent.dispose();
    }
    
    private void loadFile(String testName) throws Exception
    {
        final String path = "/" + ProductionFinderTest.class.getCanonicalName().replace('.', '/') + "_" + testName + ".soar";
        final URL url = ProductionFinderTest.class.getResource(path);
        assertNotNull(url, "Could not location resource: " + path);
        agent.getInterpreter().source(url);
    }
    
    @Test
    void testLeftHandSide() throws Exception
    {
        loadFile("testLeftHandSide");
        
        final ProductionFinder finder = new ProductionFinder(agent);
        finder.options().remove(Options.RHS);
        final List<Production> result1 = finder.find("(<c> ^contact.name *)", agent.getProductions().getProductions(null));
        assertEquals(1, result1.size());
        assertEquals("test1", result1.get(0).getName());
        
        final List<Production> result2 = finder.find("(<c> ^contact.threat *yes*)", agent.getProductions().getProductions(null));
        assertEquals(1, result2.size());
        assertEquals("test2", result2.get(0).getName());
        
        assertEquals(2, finder.find("(<s> ^contacts <c>)(<c> ^contact)", agent.getProductions().getProductions(null)).size());
        assertTrue(finder.find("(<s> ^name foo)", agent.getProductions().getProductions(null)).isEmpty());
    }
    
    @Test
    void testRightHandSide() throws Exception
    {
        loadFile("testRightHandSide");
        
        final ProductionFinder finder = new ProductionFinder(agent);
        finder.options().remove(Options.LHS);
        final List<Production> result1 = finder.find("(<c> ^contact.name *)", agent.getProductions().getProductions(null));
        assertEquals(1, result1.size());
        assertEquals("test1", result1.get(0).getName());
        
        final List<Production> result2 = finder.find("(<c> ^contact.threat *yes*)", agent.getProductions().getProductions(null));
        assertEquals(1, result2.size());
        assertEquals("test2", result2.get(0).getName());
        
        assertEquals(2, finder.find("(<s> ^contacts <cs>)(<cs> ^contact <c>)", agent.getProductions().getProductions(null)).size());
        assertTrue(finder.find("(<s> ^name foo)", agent.getProductions().getProductions(null)).isEmpty());
    }
    
}
