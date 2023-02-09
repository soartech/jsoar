/*
 * Copyright (c) 2008  Dave Ray <daveray@gmail.com>
 *
 * Created on Sep 18, 2008
 */
package org.jsoar.tcl;


import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNotSame;

import java.io.Reader;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;

import org.jsoar.kernel.Agent;
import org.jsoar.kernel.Production;
import org.jsoar.kernel.SoarException;
import org.jsoar.kernel.parser.Parser;
import org.jsoar.kernel.parser.ParserContext;
import org.jsoar.kernel.parser.ParserException;
import org.jsoar.util.adaptables.AbstractAdaptable;
import org.jsoar.util.commands.SoarCommands;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/**
 * @author ray
 */
public class SoarTclInterfaceTest
{
    private SoarTclInterface ifc;
    
    /**
     * @throws java.lang.Exception
     */
    @BeforeEach
    public void setUp() throws Exception
    {
        final Agent agent = new Agent();
        ifc = SoarTclInterface.findOrCreate(agent);
    }

    /**
     * @throws java.lang.Exception
     */
    @AfterEach
    public void tearDown() throws Exception
    {
        SoarTclInterface.dispose(ifc);
        ifc = null;
    }
    
    @Test
    public void testSourceResource() throws SoarException
    {
        ifc.source(getClass().getResource("/" + SoarTclInterfaceTest.class.getCanonicalName().replace('.', '/') + "_sourceResource.soar"));
        
        assertNotNull(ifc.getAgent().getProductions().getProduction("top-state*propose*wait"));
    }

    @Test
    public void testSrandCommand() throws Exception
    {
        ifc.eval("decide srand 98765");
        List<Integer> firstInts = new ArrayList<Integer>();
        for(int i = 0; i < 1000; ++i)
        {
            firstInts.add(ifc.getAgent().getRandom().nextInt());
        }
        ifc.eval("decide srand 98765");
        List<Integer> secondInts = new ArrayList<Integer>();
        for(int i = 0; i < 1000; ++i)
        {
            secondInts.add(ifc.getAgent().getRandom().nextInt());
        }
        assertEquals(firstInts, secondInts);
    }
    
    @Test
    public void testEnvironmentVariablesAreAvailable() throws Exception
    {
        final String path = ifc.eval("global env; set env(PATH)");
        assertEquals(System.getenv("PATH"), path);
    }
    
    public static class TestParser extends AbstractAdaptable implements Parser
    {
        /* (non-Javadoc)
         * @see org.jsoar.kernel.parser.Parser#parseProduction(org.jsoar.kernel.parser.ParserContext, java.io.Reader)
         */
        @Override
        public Production parseProduction(ParserContext context, Reader reader) throws ParserException
        {
            return null;
        }
    }
    
    @Test
    public void testSetParserCommand() throws Exception
    {
        final Parser oldParser = ifc.getAgent().getProductions().getParser();
        assertNotNull(oldParser);
        
        ifc.eval("set-parser " + SoarTclInterfaceTest.class.getCanonicalName() + "\\$TestParser");
        
        final Parser newParser = ifc.getAgent().getProductions().getParser();
        
        assertNotNull(newParser);
        assertNotSame(oldParser, newParser);
        assertEquals(TestParser.class, newParser.getClass());
    }
    
    @Test
    public void testCanLoadRelativePathInJar() throws Exception
    {
        // it turns out that loading a file from a jar whose path contains a "." causes problems
        // this problem can arise via a sequence like this (which NGS used to do):
        //
        // pushd .
        // source myfile.soar
        //
        // jsoar was modified (both default and Tcl interps) to normalize these paths
        // this test ensures it stays fixed: it uses a jar with a file that does the above
        // specifically, test.soar contains:
        //
        // pushd .
        // source test2.soar
        //
        // test2.soar contains a rule. So we simply test that the rule gets sourced.
        // When it failed before the fix, it did so by throwing an exception.
        
        // trying to construct "jar:file:test.jar!/test.soar" with the proper path to the jar
        // probably there is a better way, but this is what worked first
        
        String path = getClass().getResource("test.jar").toExternalForm();
        String inputFile = "jar:" + path + "!/test.soar";
        
        URL url = new URL(inputFile);
        SoarCommands.source(ifc, url );
        
        assertEquals(1, ifc.getAgent().getProductions().getProductionCount(), "Expected a rule to be loaded");
    }
}
