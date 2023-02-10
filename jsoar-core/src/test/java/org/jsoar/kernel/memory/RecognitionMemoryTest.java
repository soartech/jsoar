/*
 * Copyright (c) 2008  Dave Ray <daveray@gmail.com>
 *
 * Created on Dec 14, 2008
 */
package org.jsoar.kernel.memory;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;

import java.util.List;

import org.jsoar.kernel.Agent;
import org.jsoar.kernel.Production;
import org.jsoar.kernel.ProductionType;
import org.jsoar.kernel.RunType;
import org.jsoar.kernel.SoarProperties;
import org.jsoar.kernel.io.CycleCountInput;
import org.jsoar.kernel.memory.Wmes.MatcherBuilder;
import org.jsoar.kernel.rhs.functions.AbstractRhsFunctionHandler;
import org.jsoar.kernel.rhs.functions.RhsFunctionContext;
import org.jsoar.kernel.rhs.functions.RhsFunctionException;
import org.jsoar.kernel.rhs.functions.RhsFunctionHandler;
import org.jsoar.kernel.symbols.Identifier;
import org.jsoar.kernel.symbols.Symbol;
import org.jsoar.kernel.symbols.SymbolFactory;
import org.jsoar.kernel.tracing.Trace.Category;
import org.jsoar.util.commands.SoarCommandInterpreter;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/**
 * @author ray
 */
public class RecognitionMemoryTest
{
    private Agent agent;
    
    /**
     * @throws java.lang.Exception
     */
    @BeforeEach
    public void setUp() throws Exception
    {
        this.agent = new Agent();
        agent.getTrace().disableAll();
    }
    
    @Test
    public void testRhsFunctionThatCreatesStructure() throws Exception
    {
        new CycleCountInput(agent.getInputOutput());
        
        // Add a RHS function that puts a structure in working memory
        RhsFunctionHandler func = new AbstractRhsFunctionHandler("rhs-structure", 0, 0)
        {
            
            @Override
            public Symbol execute(RhsFunctionContext context, List<Symbol> arguments) throws RhsFunctionException
            {
                Identifier r1 = context.getSymbols().createIdentifier('R');
                
                final SymbolFactory syms = context.getSymbols();
                context.addWme(r1, syms.createString("x"), syms.createInteger(1));
                context.addWme(r1, syms.createString("y"), syms.createInteger(2));
                context.addWme(r1, syms.createString("z"), syms.createInteger(3));
                context.addWme(r1, syms.createString("name"), syms.createString("hello"));
                
                return r1;
            }
        };
        
        agent.getRhsFunctions().registerHandler(func);
        agent.getTrace().setEnabled(Category.WM_CHANGES, true);
        agent.getProperties().set(SoarProperties.WAITSNC, true);
        agent.getProductions().loadProduction("" +
                "testRhsFunctionThatCreatesStructure\n" +
                "(state <s> ^superstate nil ^io.input-link.cycle-count 1)\n" +
                "-->" +
                "(<s> ^result (rhs-structure))");
        
        agent.runFor(1, RunType.DECISIONS);
        
        // Verify that the x, y, z, name structure is there
        final Identifier s1 = agent.getSymbols().findIdentifier('S', 1);
        final MatcherBuilder m = Wmes.matcher(agent);
        Wme result = m.attr("result").find(s1);
        assertNotNull(result);
        final Identifier r1 = result.getValue().asIdentifier();
        
        Wme x, y, z, name;
        assertNotNull(x = m.reset().attr("x").value(1).find(r1));
        assertNotNull(y = m.reset().attr("y").value(2).find(r1));
        assertNotNull(z = m.reset().attr("z").value(3).find(r1));
        assertNotNull(name = m.reset().attr("name").value("hello").find(r1));
        
        // Step again and verify that all the WMEs are retracted because of the test on
        // cycle-count
        agent.runFor(1, RunType.DECISIONS);
        
        assertNull(m.reset().attr("result").find(s1));
        assertFalse(agent.getAllWmesInRete().contains(x));
        assertFalse(agent.getAllWmesInRete().contains(y));
        assertFalse(agent.getAllWmesInRete().contains(z));
        assertFalse(agent.getAllWmesInRete().contains(name));
    }
    
    @Test
    public void testChunkedActionsAreCorrectlyConsideredAsNumericIndifferent() throws Exception
    {
        /*
         * Test for Joseph's fix in csoar r10460
         * 
         * From an email from Joseph explaining the bug: I didn't file a bug in
         * bugzilla about it, if that's what you mean. However, it does cause
         * unexpected behavior in that when you chunk over a result that was
         * created by a rule with an action of the form
         * 
         * (<s> ^operator <o> = <x>)
         * 
         * where <x> is bound to a numeric value, the chunk's action will be
         * considered a binary indifferent type, even though it's clearly a
         * numeric indifferent type. This became important because I was using
         * chunking to create RL rules, and for a rule to qualify as an RL rule,
         * it must have a numeric indifferent action on the RHS.
         * 
         * That explanation probably wasn't clear, so I've included a test case
         * where you can see the difference. If you run it 2 steps as is and do
         * a "print --rl", you'll see that the two learned chunks are considered
         * RL rules. If you go into the file and change the commented out line
         * and run it again, you'll see that the chunks are no longer considered
         * RL rules. The patch fixes this behavior.
         */
        final SoarCommandInterpreter ifc = agent.getInterpreter();
        ifc.source(RecognitionMemoryTest.class.getResource("/" + RecognitionMemoryTest.class.getName().replace('.', '/') + "_r10460.soar"));
        
        agent.runFor(2, RunType.DECISIONS);
        
        final List<Production> chunks = agent.getProductions().getProductions(ProductionType.CHUNK);
        assertEquals(2, chunks.size());
        for(Production p : chunks)
        {
            assertNotNull(p.rlRuleInfo, p.getName() + " should be an rl rule");
        }
    }
    
    @Test
    public void testInstiationDeallocationStackOverflow() throws Exception
    {
        /*
         * Test for jsoar issue 7 (port of recursion flattening from csoar)
         * The real test is whether this crashes with a stack overflow or not (it should not)
         */
        final SoarCommandInterpreter ifc = agent.getInterpreter();
        ifc.source(RecognitionMemoryTest.class.getResource("/" + RecognitionMemoryTest.class.getName().replace('.', '/') + "_count-and-die.soar"));
        
        agent.runFor(75006, RunType.DECISIONS);
        
        assertEquals(75005, agent.getProperties().get(SoarProperties.DECISION_PHASES_COUNT), "did not halt when expected");
    }
}
