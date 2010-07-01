/*
 * Copyright (c) 2008  Dave Ray <daveray@gmail.com>
 *
 * Created on Sep 19, 2008
 */
package org.jsoar.kernel;


import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import java.io.StringReader;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

import org.jsoar.kernel.memory.DummyWme;
import org.jsoar.kernel.memory.Instantiation;
import org.jsoar.kernel.memory.Wme;
import org.jsoar.kernel.memory.WmeImpl;
import org.jsoar.kernel.memory.Wmes;
import org.jsoar.kernel.parser.ParserContext;
import org.jsoar.kernel.parser.original.OriginalParser;
import org.jsoar.kernel.rete.ProductionAddResult;
import org.jsoar.kernel.rete.Rete;
import org.jsoar.kernel.rete.ReteListener;
import org.jsoar.kernel.rete.ReteNode;
import org.jsoar.kernel.rete.Token;
import org.jsoar.kernel.rhs.functions.RhsFunctionContext;
import org.jsoar.kernel.rhs.functions.RhsFunctionManager;
import org.jsoar.kernel.symbols.DoubleSymbol;
import org.jsoar.kernel.symbols.Identifier;
import org.jsoar.kernel.symbols.IdentifierImpl;
import org.jsoar.kernel.symbols.IntegerSymbol;
import org.jsoar.kernel.symbols.StringSymbol;
import org.jsoar.kernel.symbols.Symbol;
import org.jsoar.kernel.symbols.SymbolFactory;
import org.jsoar.kernel.symbols.SymbolFactoryImpl;
import org.jsoar.kernel.symbols.SymbolImpl;
import org.jsoar.kernel.tracing.Printer;
import org.jsoar.kernel.tracing.Trace;
import org.jsoar.util.adaptables.Adaptables;
import org.junit.Test;

import com.google.common.collect.Lists;

/**
 * @author ray
 */
public class GDSTests extends FunctionalTestHarness
{
    @Test(timeout=10000)
    public void testGDSBug1144() throws Exception
    {
        runTest("testGDSBug1144", 7); // should halt not crash
    }
    
    @Test(timeout=10000)
    public void testGDSBug1011() throws Exception
    {
        runTest("testGDSBug1011", 8);
        assertEquals(19, agent.getProperties().get(SoarProperties.E_CYCLE_COUNT).intValue());
    }
    
    @Test
    public void testSimple() throws Exception
    {
        runTest("testSimple", 5);
    }
    
    @Test
    public void testDoubleSupport() throws Exception
    {
        runTest("testDoubleSupport", 5);
        assertEquals(2, agent.getGoalStack().size());
        
        List<Identifier> goals = agent.getGoalStack();
        GoalDependencySet gds = Adaptables.adapt(goals.get(0), GoalDependencySet.class);
        assertTrue("Expected GDS for top state to be empty", gds == null);
        
        gds = Adaptables.adapt(goals.get(1), GoalDependencySet.class);
        assertTrue("Expected GDS for substate to be non-empty", gds != null);
    }
    
    @Test
    public void testMultilevel1() throws Exception
    {
        runTest("testMultilevel1", 5);
        
        testMultilevel();
    }

    @Test
    public void testMultilevel2() throws Exception
    {
        runTest("testMultilevel2", 5);
        
        testMultilevel();
    }
    
    // copied from ReteUnitTest.java
    private class Listener implements ReteListener
    {
        Set<Production> matching = new HashSet<Production>();
        
        /* (non-Javadoc)
         * @see org.jsoar.kernel.rete.ReteListener#finishRefraction(org.jsoar.kernel.rete.Rete, org.jsoar.kernel.Production, org.jsoar.kernel.rete.Instantiation, org.jsoar.kernel.rete.ReteNode)
         */
        @Override
        public boolean finishRefraction(Rete rete, Production p, Instantiation refracted_inst, ReteNode p_node)
        {
            return false;
        }

        /* (non-Javadoc)
         * @see org.jsoar.kernel.rete.ReteListener#p_node_left_addition(org.jsoar.kernel.rete.Rete, org.jsoar.kernel.rete.ReteNode, org.jsoar.kernel.rete.Token, org.jsoar.kernel.Wme)
         */
        @Override
        public void p_node_left_addition(Rete rete, ReteNode node, Token tok, WmeImpl w)
        {
            matching.add(node.b_p.prod);
        }

        /* (non-Javadoc)
         * @see org.jsoar.kernel.rete.ReteListener#p_node_left_removal(org.jsoar.kernel.rete.Rete, org.jsoar.kernel.rete.ReteNode, org.jsoar.kernel.rete.Token, org.jsoar.kernel.Wme)
         */
        @Override
        public void p_node_left_removal(Rete rete, ReteNode node, Token tok, WmeImpl w)
        {
            matching.remove(node.b_p.prod);
        }

        /* (non-Javadoc)
         * @see org.jsoar.kernel.rete.ReteListener#startRefraction(org.jsoar.kernel.rete.Rete, org.jsoar.kernel.Production, org.jsoar.kernel.rete.Instantiation, org.jsoar.kernel.rete.ReteNode)
         */
        @Override
        public void startRefraction(Rete rete, Production p, Instantiation refracted_inst, ReteNode p_node)
        {
        }

        /* (non-Javadoc)
         * @see org.jsoar.kernel.rete.ReteListener#removingProductionNode(org.jsoar.kernel.rete.Rete, org.jsoar.kernel.rete.ReteNode)
         */
        @Override
        public void removingProductionNode(Rete rete, ReteNode p_node)
        {
        }
        
    }
    
    // copied from ReteUnitTest.java, JSoarUnitTest.java
    SymbolFactoryImpl syms;
    Listener listener;
    Rete rete;
    
    protected RhsFunctionContext rhsFuncContext = new RhsFunctionContext() {

        @Override
        public SymbolFactory getSymbols()
        {
            return syms;
        }

        /* (non-Javadoc)
         * @see org.jsoar.kernel.rhs.functions.RhsFunctionContext#addWme(org.jsoar.kernel.symbols.Identifier, org.jsoar.kernel.symbols.Symbol, org.jsoar.kernel.symbols.Symbol)
         */
        @Override
        public Void addWme(Identifier id, Symbol attr, Symbol value)
        {
            throw new UnsupportedOperationException("This test implementation of RhsFunctionContext doesn't support addWme");
        }

        /* (non-Javadoc)
         * @see org.jsoar.kernel.rhs.functions.RhsFunctionContext#getProductionBeingFired()
         */
        @Override
        public Production getProductionBeingFired()
        {
            return null;
        }
        
    };
    
    
    private Production parseProduction(String s) throws Exception
    {
        final OriginalParser parser = new OriginalParser();
        final StringReader reader = new StringReader(s);
        final ParserContext context = new ParserContext() {

            @Override
            public Object getAdapter(Class<?> klass)
            {
                if(klass.equals(SymbolFactoryImpl.class))
                {
                    return syms;
                }
                if(klass.equals(RhsFunctionManager.class))
                {
                    return new RhsFunctionManager(rhsFuncContext);
                }
                else if(klass.equals(Printer.class))
                {
                    return Printer.createStdOutPrinter();
                }
                return null;
            }
            
        };
        return parser.parseProduction(context, reader);
    }
    
    
    /**
     * 
     */
    private void testMultilevel() throws Exception
    {
        List<Identifier> goals = agent.getGoalStack();
        assertTrue("Unexpected number of states", goals.size() == 3);
        
        // top state
        GoalDependencySet gds = Adaptables.adapt(goals.get(0), GoalDependencySet.class);
        assertTrue("Expected first goal to have empty GDS", gds == null);

        // first substate
        gds = Adaptables.adapt(goals.get(1), GoalDependencySet.class);
        assertTrue("Expected second goal have non-empty GDS", gds != null);

        SymbolFactory sf = agent.getSymbols();

        final Set<Wme> actual = new LinkedHashSet<Wme>(Lists.newArrayList(gds.getWmes()));
        
//      (14: P1 ^name top)
//      (13: S1 ^problem-space P1)
//      (31: S3 ^attribute operator)
//      (32: S3 ^impasse no-change)
//      (16: S1 ^a a)
//      (17: S1 ^b b)
//      (18: S1 ^c c)
//      (23: S3 ^superstate S1)
        
//        Set<Wme> expected = DummyWme.create(sf, 
//                Symbols.NEW_ID, "name", "top",
//                Symbols.NEW_ID, "problem-space", Symbols.NEW_ID,
//                Symbols.NEW_ID, "attribute", "operator",
//                Symbols.NEW_ID, "impasse", "no-change",
//                Symbols.NEW_ID, "a", "a",
//                Symbols.NEW_ID, "b", "b",
//                Symbols.NEW_ID, "c", "c",
//                Symbols.NEW_ID, "superstate", Symbols.NEW_ID);
//        

//        
//        // TODO: would be nice if this actually reported which wme didn't match
//        assertTrue("Actual wmes don't match expected wmes", Wmes.equal(actual, expected, true));
        
        this.syms = new SymbolFactoryImpl();
        this.listener = new Listener();
        this.rete = new Rete(Trace.createStdOutTrace().enableAll(), syms);
        rete.setReteListener(listener);
        
        Production p = parseProduction(
                "expectedGDS \n" +
                "(<s3> ^attribute operator ^impasse no-change ^superstate <s1>) \n" +
                "(<s1> ^problem-space <p1> ^a a ^b b ^c c) \n" +
                "(<p1> ^name top) \n" +
                "--> \n" +
                "(write <s3>)");
        
        assertNotNull(p);
        
        ProductionAddResult result = rete.add_production_to_rete(p);
        assertNotNull(result);
        assertEquals(ProductionAddResult.NO_REFRACTED_INST, result);
        
        for(Wme actualWme : actual)
        {
            final IdentifierImpl id = (IdentifierImpl) syms.findOrCreateIdentifier(actualWme.getIdentifier().getNameLetter(), actualWme.getIdentifier().getNameNumber());
            final SymbolImpl attr = copySymbol(syms, actualWme.getAttribute());
            final SymbolImpl value = copySymbol(syms, actualWme.getValue());;
            final WmeImpl wme = new WmeImpl(id, attr, value, false, 0);
            rete.add_wme_to_rete(wme);
        }
        
        assertTrue(listener.matching.contains(p));
        
        // second substate
        gds = Adaptables.adapt(goals.get(2), GoalDependencySet.class);
        assertTrue("Expected third goal have non-empty GDS", gds != null);
        
//        (36: P2 ^name second)
//        (35: S3 ^problem-space P2)
//        (40: O2 ^name operator-2)
//        (42: S3 ^operator O2)
//        (53: S5 ^impasse no-change)
//        (38: S3 ^d d)
//        (39: S3 ^e e)
//        (44: S5 ^superstate S3)
        
        Set<Wme> expected2 = new LinkedHashSet<Wme>();
        expected2.add(new DummyWme(sf.createIdentifier('P'), sf.createString("name"), sf.createString("second")));
        expected2.add(new DummyWme(sf.createIdentifier('S'), sf.createString("problem-space"), sf.createIdentifier('P')));
        expected2.add(new DummyWme(sf.createIdentifier('O'), sf.createString("name"), sf.createString("operator-2")));
        expected2.add(new DummyWme(sf.createIdentifier('S'), sf.createString("operator"), sf.createIdentifier('O')));
        expected2.add(new DummyWme(sf.createIdentifier('S'), sf.createString("impasse"), sf.createString("no-change")));
        expected2.add(new DummyWme(sf.createIdentifier('S'), sf.createString("d"), sf.createString("d")));
        expected2.add(new DummyWme(sf.createIdentifier('S'), sf.createString("e"), sf.createString("e")));
        expected2.add(new DummyWme(sf.createIdentifier('S'), sf.createString("superstate"), sf.createIdentifier('S')));
        
        final Set<Wme> actual2 = new LinkedHashSet<Wme>(Lists.newArrayList(gds.getWmes()));
        
        // TODO: would be nice if this actually reported which wme didn't match
        assertTrue("Actual wmes don't match expected wmes", Wmes.equal(actual2, expected2, true));
    }
    
    private SymbolImpl copySymbol(final SymbolFactoryImpl syms, final Symbol origSym)
    {
        final SymbolImpl copySym;
        final Identifier sId = origSym.asIdentifier();
        final IntegerSymbol sInt = origSym.asInteger();
        final DoubleSymbol sDouble = origSym.asDouble();
        final StringSymbol sStr = origSym.asString();
        
        if(sId != null)
        {
            copySym = (SymbolImpl) syms.findOrCreateIdentifier(sId.getNameLetter(), sId.getNameNumber());
        }
        else if(sInt !=  null)
        {
            copySym = syms.createInteger(sInt.getValue());
        }
        else if(sDouble !=  null)
        {
            copySym = syms.createDouble(sDouble.getValue());
        }
        else if(sStr !=  null)
        {
            copySym = syms.createString(sStr.getValue());
        }
        else
        {
            copySym = null;
        }
        
        // TODO java sym
        
        return copySym;
    }
}
