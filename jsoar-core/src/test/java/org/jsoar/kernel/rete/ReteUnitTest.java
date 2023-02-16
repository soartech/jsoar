/*
 * Copyright (c) 2008  Dave Ray <daveray@gmail.com>
 *
 * Created on Sep 2, 2008
 */
package org.jsoar.kernel.rete;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.StringReader;
import java.util.HashSet;
import java.util.Set;

import org.jsoar.JSoarTest;
import org.jsoar.kernel.Agent;
import org.jsoar.kernel.Production;
import org.jsoar.kernel.epmem.DefaultEpisodicMemory;
import org.jsoar.kernel.learning.rl.ReinforcementLearningParams;
import org.jsoar.kernel.memory.Instantiation;
import org.jsoar.kernel.memory.WmeImpl;
import org.jsoar.kernel.parser.ParserContext;
import org.jsoar.kernel.parser.original.OriginalParser;
import org.jsoar.kernel.rhs.functions.RhsFunctionManager;
import org.jsoar.kernel.smem.DefaultSemanticMemory;
import org.jsoar.kernel.symbols.IdentifierImpl;
import org.jsoar.kernel.symbols.SymbolFactoryImpl;
import org.jsoar.kernel.tracing.Printer;
import org.jsoar.kernel.tracing.Trace;
import org.jsoar.util.adaptables.AdaptableContainer;
import org.jsoar.util.properties.PropertyManager;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/**
 * @author ray
 */
public class ReteUnitTest extends JSoarTest
{
    private Rete rete;
    private Listener listener;
    private DefaultEpisodicMemory episodicMemory;
    private DefaultSemanticMemory semanticMemory;
    
    private class Listener implements ReteListener
    {
        Set<Production> matching = new HashSet<>();
        
        /*
         * (non-Javadoc)
         * 
         * @see org.jsoar.kernel.rete.ReteListener#finishRefraction(org.jsoar.kernel.rete.Rete, org.jsoar.kernel.Production, org.jsoar.kernel.rete.Instantiation, org.jsoar.kernel.rete.ReteNode)
         */
        @Override
        public boolean finishRefraction(Rete rete, Production p, Instantiation refracted_inst, ReteNode p_node)
        {
            return false;
        }
        
        /*
         * (non-Javadoc)
         * 
         * @see org.jsoar.kernel.rete.ReteListener#p_node_left_addition(org.jsoar.kernel.rete.Rete, org.jsoar.kernel.rete.ReteNode, org.jsoar.kernel.rete.Token, org.jsoar.kernel.Wme)
         */
        @Override
        public void p_node_left_addition(Rete rete, ReteNode node, Token tok, WmeImpl w)
        {
            matching.add(node.b_p().prod);
        }
        
        /*
         * (non-Javadoc)
         * 
         * @see org.jsoar.kernel.rete.ReteListener#p_node_left_removal(org.jsoar.kernel.rete.Rete, org.jsoar.kernel.rete.ReteNode, org.jsoar.kernel.rete.Token, org.jsoar.kernel.Wme)
         */
        @Override
        public void p_node_left_removal(Rete rete, ReteNode node, Token tok, WmeImpl w)
        {
            matching.remove(node.b_p().prod);
        }
        
        /*
         * (non-Javadoc)
         * 
         * @see org.jsoar.kernel.rete.ReteListener#startRefraction(org.jsoar.kernel.rete.Rete, org.jsoar.kernel.Production, org.jsoar.kernel.rete.Instantiation, org.jsoar.kernel.rete.ReteNode)
         */
        @Override
        public void startRefraction(Rete rete, Production p, Instantiation refracted_inst, ReteNode p_node)
        {
        }
        
        /*
         * (non-Javadoc)
         * 
         * @see org.jsoar.kernel.rete.ReteListener#removingProductionNode(org.jsoar.kernel.rete.Rete, org.jsoar.kernel.rete.ReteNode)
         */
        @Override
        public void removingProductionNode(Rete rete, ReteNode p_node)
        {
        }
        
    }
    
    /*
     * (non-Javadoc)
     * 
     * @see org.jsoar.JSoarTest#setUp()
     */
    @Override
    @BeforeEach
    public void setUp() throws Exception
    {
        super.setUp();
        
        this.listener = new Listener();
        Agent agent = new Agent();
        this.episodicMemory = new DefaultEpisodicMemory(AdaptableContainer.from(syms, agent));
        this.episodicMemory.initialize();
        this.semanticMemory = new DefaultSemanticMemory(AdaptableContainer.from(syms, agent));
        this.rete = new Rete(Trace.createStdOutTrace().enableAll(), syms, episodicMemory, semanticMemory,
                new ReinforcementLearningParams(new PropertyManager(), syms));
        this.rete.setReteListener(listener);
    }
    
    private Production parseProduction(String s) throws Exception
    {
        final OriginalParser parser = new OriginalParser();
        final StringReader reader = new StringReader(s);
        final ParserContext context = new ParserContext()
        {
            
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
    
    @Test
    public void testInitDummyTopNode() throws Exception
    {
        assertNotNull(rete.dummy_top_node);
        assertEquals(ReteNodeType.DUMMY_TOP_BNODE, rete.dummy_top_node.node_type);
        RightToken dummyTopToken = (RightToken) rete.dummy_top_node.a_np().tokens;
        assertNotNull(dummyTopToken);
        assertNull(dummyTopToken.parent);
        assertSame(rete.dummy_top_node, dummyTopToken.node);
    }
    
    @Test
    public void testAddProductionToRete() throws Exception
    {
        Production p = parseProduction(
                "testAddProductionToRete \n" +
                        "(<root> ^integer 1 \n" +
                        "        ^float 3.14 \n" +
                        "        ^string |S| \n" +
                        "        ^id <id>) \n" +
                        "--> \n" +
                        "(write <root>)");
        
        assertNotNull(p);
        
        ProductionAddResult result = rete.add_production_to_rete(p);
        assertNotNull(result);
        assertEquals(ProductionAddResult.NO_REFRACTED_INST, result);
        
        // TODO: Test structure of built rete
    }
    
    @Test
    public void testSimpleAddWmeToRete() throws Exception
    {
        Production p = parseProduction(
                "testAddProductionToRete \n" +
                        "(<root> ^integer 1 ^float 3.14 ^string |S| ^id <id>)" +
                        "--> \n" +
                        "(write <root>)");
        
        assertNotNull(p);
        
        ProductionAddResult result = rete.add_production_to_rete(p);
        assertNotNull(result);
        assertEquals(ProductionAddResult.NO_REFRACTED_INST, result);
        
        // Add WMEs one at a time and make sure there isn't a match until the last WME is added
        IdentifierImpl root = syms.make_new_identifier('R', (short) 0);
        WmeImpl intWme = new WmeImpl(root, syms.createString("integer"), syms.createInteger(1), false, 0);
        rete.add_wme_to_rete(intWme);
        assertFalse(listener.matching.contains(p));
        
        WmeImpl floatWme = new WmeImpl(root, syms.createString("float"), syms.createDouble(3.14), false, 0);
        rete.add_wme_to_rete(floatWme);
        assertFalse(listener.matching.contains(p));
        
        WmeImpl stringWme = new WmeImpl(root, syms.createString("string"), syms.createString("S"), false, 0);
        rete.add_wme_to_rete(stringWme);
        assertFalse(listener.matching.contains(p));
        
        WmeImpl idWme = new WmeImpl(root, syms.createString("id"), syms.make_new_identifier('i', (short) 0), false, 0);
        rete.add_wme_to_rete(idWme);
        assertTrue(listener.matching.contains(p));
        
        // Remove int WME to verify the production unmatches
        rete.remove_wme_from_rete(intWme);
        assertFalse(listener.matching.contains(p));
        
        // Re-add it to verify it re-matches
        rete.add_wme_to_rete(intWme);
        assertTrue(listener.matching.contains(p));
        
        // Remove rest of WMEs to verify the production doesn't match again.
        rete.remove_wme_from_rete(floatWme);
        assertFalse(listener.matching.contains(p));
        rete.remove_wme_from_rete(idWme);
        assertFalse(listener.matching.contains(p));
        rete.remove_wme_from_rete(stringWme);
        assertFalse(listener.matching.contains(p));
        
        rete.remove_wme_from_rete(intWme);
        assertFalse(listener.matching.contains(p));
    }
    
    @Test
    public void testReteWithNegatedConjunctiveCondition() throws Exception
    {
        Production p = parseProduction(
                "testReteWithNegatedConjunctiveCondition \n" +
                        "(<root> ^integer 1 ^float 3.14)\n" +
                        "-{ (<root> ^string |S|) " +
                        "   (<root> ^id <id>)}\n" +
                        "--> \n" +
                        "(write <root>)");
        
        assertNotNull(p);
        
        ProductionAddResult result = rete.add_production_to_rete(p);
        assertNotNull(result);
        assertEquals(ProductionAddResult.NO_REFRACTED_INST, result);
        
        IdentifierImpl root = syms.make_new_identifier('R', (short) 0);
        WmeImpl intWme = new WmeImpl(root, syms.createString("integer"), syms.createInteger(1), false, 0);
        rete.add_wme_to_rete(intWme);
        assertFalse(listener.matching.contains(p));
        
        WmeImpl floatWme = new WmeImpl(root, syms.createString("float"), syms.createDouble(3.14), false, 0);
        rete.add_wme_to_rete(floatWme);
        
        // At this point, the production should match because the negated condition is false
        assertTrue(listener.matching.contains(p));
        
        WmeImpl stringWme = new WmeImpl(root, syms.createString("string"), syms.createString("S"), false, 0);
        rete.add_wme_to_rete(stringWme);
        
        // NCC stays false when the string is added
        assertTrue(listener.matching.contains(p));
        
        WmeImpl idWme = new WmeImpl(root, syms.createString("id"), syms.make_new_identifier('i', (short) 0), false, 0);
        rete.add_wme_to_rete(idWme);
        
        // Addint this WME makes the NCC true, so the production unmatches
        assertFalse(listener.matching.contains(p));
        
        // Remove int WME to verify the production re-matches
        rete.remove_wme_from_rete(stringWme);
        assertTrue(listener.matching.contains(p));
        
        // Re-add it to verify it unmatches
        rete.add_wme_to_rete(stringWme);
        assertFalse(listener.matching.contains(p));
    }
    
    @Test
    public void testSimpleReteTests() throws Exception
    {
        Production p = parseProduction(
                "testAddProductionToRete \n" +
                        "(<root> ^integer  < 2 ^float >= 3.14 ^string << T UV S >> ^id <id>)" +
                        "--> \n" +
                        "(write <root>)");
        
        assertNotNull(p);
        
        ProductionAddResult result = rete.add_production_to_rete(p);
        assertNotNull(result);
        assertEquals(ProductionAddResult.NO_REFRACTED_INST, result);
        
        // Add WMEs one at a time and make sure there isn't a match until the last WME is added
        IdentifierImpl root = syms.make_new_identifier('R', (short) 0);
        WmeImpl intWme = new WmeImpl(root, syms.createString("integer"), syms.createInteger(1), false, 0);
        rete.add_wme_to_rete(intWme);
        assertFalse(listener.matching.contains(p));
        
        WmeImpl floatWme = new WmeImpl(root, syms.createString("float"), syms.createDouble(3.14), false, 0);
        rete.add_wme_to_rete(floatWme);
        assertFalse(listener.matching.contains(p));
        
        WmeImpl stringWme = new WmeImpl(root, syms.createString("string"), syms.createString("S"), false, 0);
        rete.add_wme_to_rete(stringWme);
        assertFalse(listener.matching.contains(p));
        
        WmeImpl idWme = new WmeImpl(root, syms.createString("id"), syms.make_new_identifier('i', (short) 0), false, 0);
        rete.add_wme_to_rete(idWme);
        assertTrue(listener.matching.contains(p));
        
        // Remove int WME to verify the production unmatches
        rete.remove_wme_from_rete(intWme);
        assertFalse(listener.matching.contains(p));
        
        // Re-add it to verify it re-matches
        rete.add_wme_to_rete(intWme);
        assertTrue(listener.matching.contains(p));
        
        // Remove rest of WMEs to verify the production doesn't match again.
        rete.remove_wme_from_rete(floatWme);
        assertFalse(listener.matching.contains(p));
        rete.remove_wme_from_rete(idWme);
        assertFalse(listener.matching.contains(p));
        rete.remove_wme_from_rete(stringWme);
        assertFalse(listener.matching.contains(p));
        
        rete.remove_wme_from_rete(intWme);
        assertFalse(listener.matching.contains(p));
    }
    
    @Test
    public void testAddProductionSimpleMakeAction() throws Exception
    {
        Production p = parseProduction(
                "testAddProductionSimpleMakeAction \n" +
                        "(state <s> ^superstate nil)" +
                        "--> \n" +
                        "(<s> ^value 1)");
        
        assertNotNull(p);
        
        ProductionAddResult result = rete.add_production_to_rete(p);
        assertNotNull(result);
        assertEquals(ProductionAddResult.NO_REFRACTED_INST, result);
    }
    
    @Test
    public void testAddTwoProductionsToRete() throws Exception
    {
        Production p = parseProduction(
                "testAddTwoProductionsToRete1 \n" +
                        "(state <s> ^superstate nil)" +
                        "--> \n" +
                        "(<s> ^value 1)");
        
        assertNotNull(p);
        
        ProductionAddResult result = rete.add_production_to_rete(p);
        assertNotNull(result);
        assertEquals(ProductionAddResult.NO_REFRACTED_INST, result);
        
        Production p2 = parseProduction(
                "testAddTwoProductionsToRete2 \n" +
                        "(state <s> ^superstate nil ^value 1)" +
                        "--> \n" +
                        "(<s> ^value 2)");
        
        assertNotNull(p2);
        
        result = rete.add_production_to_rete(p2);
        assertNotNull(result);
        assertEquals(ProductionAddResult.NO_REFRACTED_INST, result);
    }
    
    @Test
    public void testAddProblematicTowersOfHanoiProduction() throws Exception
    {
        Production p = parseProduction("towers-of-hanoi*propose*initialize\n" +
                "   (state <s> ^superstate nil\n" +
                "             -^name)\n" +
                "-->\n" +
                "   (<s> ^operator <o> +)\n" +
                "   (<o> ^name initialize-toh)");
        
        assertNotNull(p);
        
        ProductionAddResult result = rete.add_production_to_rete(p);
        assertNotNull(result);
        assertEquals(ProductionAddResult.NO_REFRACTED_INST, result);
    }
}
