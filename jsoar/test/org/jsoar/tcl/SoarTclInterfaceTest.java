/*
 * Copyright (c) 2008  Dave Ray <daveray@gmail.com>
 *
 * Created on Sep 18, 2008
 */
package org.jsoar.tcl;


import static org.junit.Assert.*;

import java.io.Reader;
import java.util.ArrayList;
import java.util.List;

import org.jsoar.kernel.Agent;
import org.jsoar.kernel.Production;
import org.jsoar.kernel.parser.Parser;
import org.jsoar.kernel.parser.ParserContext;
import org.jsoar.kernel.parser.ParserException;
import org.jsoar.util.adaptables.AbstractAdaptable;
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
        ifc = SoarTclInterface.findOrCreate(agent);
        agent.initialize();
    }

    /**
     * @throws java.lang.Exception
     */
    @After
    public void tearDown() throws Exception
    {
        SoarTclInterface.dispose(ifc);
        ifc = null;
    }
    
    @Test
    public void testSourceResource() throws SoarTclException
    {
        ifc.sourceResource("/" + SoarTclInterfaceTest.class.getCanonicalName().replace('.', '/') + "_sourceResource.soar");
        
        assertNotNull(ifc.getAgent().getProductions().getProduction("top-state*propose*wait"));
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
}
