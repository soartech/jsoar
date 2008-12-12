/*
 * Copyright (c) 2008  Dave Ray <daveray@gmail.com>
 *
 * Created on Nov 21, 2008
 */
package org.jsoar.kernel.io.quick;


import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.util.ArrayList;
import java.util.List;

import org.jsoar.JSoarTest;
import org.jsoar.kernel.Agent;
import org.jsoar.kernel.RunType;
import org.jsoar.kernel.rhs.functions.RhsFunctionException;
import org.jsoar.kernel.rhs.functions.StandaloneRhsFunctionHandler;
import org.jsoar.kernel.symbols.Symbol;
import org.jsoar.kernel.symbols.SymbolFactory;
import org.jsoar.tcl.SoarTclException;
import org.jsoar.tcl.SoarTclInterface;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import com.google.common.collect.Iterators;

/**
 * @author ray
 */
public class SoarQMemoryAdapterTest extends JSoarTest
{
    private Agent agent;
    private SoarTclInterface ifc;
    
    private static class MatchFunction extends StandaloneRhsFunctionHandler
    {
        boolean called = false;
        List<List<Symbol>> calls = new ArrayList<List<Symbol>>();
        
        public MatchFunction() { super("match"); }
        
        /* (non-Javadoc)
         * @see org.jsoar.kernel.rhs.functions.RhsFunctionHandler#execute(org.jsoar.kernel.symbols.SymbolFactory, java.util.List)
         */
        @Override
        public Symbol execute(SymbolFactory syms, List<Symbol> arguments) throws RhsFunctionException
        {
            called = true;
            calls.add(new ArrayList<Symbol>(arguments));
            return null;
        }
    }
    
    private MatchFunction match;
    
    private void sourceTestFile(String name) throws SoarTclException
    {
        ifc.sourceResource("/" + getClass().getName().replace('.', '/')  + "_" + name);
    }
    /**
     * @throws java.lang.Exception
     */
    @Before
    public void setUp() throws Exception
    {
        super.setUp();
        
        agent = new Agent();
        ifc = new SoarTclInterface(agent);
        agent.getRhsFunctions().registerHandler(match = new MatchFunction());
        agent.initialize();
    }

    /**
     * @throws java.lang.Exception
     */
    @After
    public void tearDown() throws Exception
    {
        ifc.dispose();
    }
    
    @Test public void testBasicInput() throws Exception
    {
        sourceTestFile("testBasicInput.soar");
        QMemory qmem = DefaultQMemory.create();
        SoarQMemoryAdapter adapter = SoarQMemoryAdapter.attach(agent, qmem);
        
        qmem.setInteger("cycles", 99);
        qmem.setDouble("location.x", 3.14159);
        qmem.setDouble("location.y", 10.0);
        qmem.setString("agent.info.name", "test");
        
        agent.runFor(2, RunType.DECISIONS);
        
        assertEquals("first", match.calls.get(0).get(0).toString());
        
        qmem.setInteger("cycles", 100);
        qmem.setDouble("location.x", 3.14159);
        qmem.setDouble("location.y", 35.0);
        qmem.remove("agent.info.name");
        
        agent.runFor(1, RunType.DECISIONS);
        
        assertEquals("second", match.calls.get(1).get(0).toString());
        
        adapter.detach();
    }
    
    @Test public void testBasicOutput() throws Exception
    {
        sourceTestFile("testBasicOutput.soar");
        
        agent.runFor(2, RunType.DECISIONS);

        QMemory qmem = DefaultQMemory.create(agent.getInputOutput().getOutputLink());
        
        assertEquals(99, qmem.getInteger("cycle"));
        assertEquals("99", qmem.getString("cycle"));
        assertEquals(12345, qmem.getInteger("a.very.deeply.nested.integer.as.string"));
        assertEquals("test", qmem.getString("agent.info.name"));
        assertEquals(3.14159, qmem.getDouble("location.x"), 0.001);
        assertEquals(31.0, qmem.getDouble("location.y"), 0.001);
        assertEquals("This is a test", qmem.getString("location.description"));
    }
    
    @Test public void testCircularReference() throws Exception
    {
        sourceTestFile("testCircularReference.soar");
        
        agent.runFor(2, RunType.DECISIONS);

        QMemory qmem = DefaultQMemory.create(agent.getInputOutput().getOutputLink());
        assertTrue(qmem.hasPath("root"));
        String path = "root.a.path.back.to.root";
        for(int i = 0; i < DefaultQMemory.MAX_DEPTH / 5 - 1; ++i)
        {
            path += ".a.path.back.to.root"; // five components contributing to depth
            assertTrue(qmem.hasPath(path));
        }
        assertEquals(DefaultQMemory.MAX_DEPTH + 1, qmem.getPaths().size());
    }
    
    @Test public void testDetach() throws Exception
    {
        ifc.eval("waitsnc --on");
        
        QMemory qmem = DefaultQMemory.create();
        SoarQMemoryAdapter adapter = SoarQMemoryAdapter.attach(agent, qmem);
        
        qmem.setInteger("cycles", 99);
        qmem.setDouble("location.x", 3.14159);
        qmem.setDouble("location.y", 10.0);
        qmem.setString("agent.info.name", "test");
        
        agent.runFor(2, RunType.DECISIONS);
        
        assertEquals(3, Iterators.size(agent.io.getInputLink().getWmes()));
        
        adapter.detach();
        
        assertEquals(3, Iterators.size(agent.io.getInputLink().getWmes()));
        
        agent.runFor(1, RunType.DECISIONS);
        
        // Verify that all WMEs were removed from input-link
        assertEquals(0, Iterators.size(agent.io.getInputLink().getWmes()));
        
    }

}
