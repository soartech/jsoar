package org.jsoar.kernel.rete;

import android.test.AndroidTestCase;

import org.jsoar.kernel.Agent;
import org.jsoar.kernel.Production;
import org.jsoar.kernel.RunType;
import org.jsoar.kernel.SoarProperties;
import org.jsoar.kernel.rhs.functions.RhsFunctionContext;
import org.jsoar.kernel.rhs.functions.RhsFunctionException;
import org.jsoar.kernel.rhs.functions.StandaloneRhsFunctionHandler;
import org.jsoar.kernel.symbols.Symbol;
import org.jsoar.util.ByRef;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.util.HashSet;
import java.util.List;

/**
 * Tests for {@link org.jsoar.kernel.rete.ReteSerializer}
 * 
 * @author charles.newton
 */
public class ReteSerializeTest extends AndroidTestCase
{
    private Agent agent;
    
    @Override
    public void setUp() throws Exception
    {
        agent = new Agent(getContext());
    }

    @Override
    public void tearDown() throws Exception
    {
        agent.dispose();
    }

    public void serializationTest() throws Exception
    {
        serializationTestBuilder(false, false);
        serializationTestBuilder(false, true);
        serializationTestBuilder(true, false);
        serializationTestBuilder(true, true);
    }
    
    private void serializationTestBuilder(boolean initializeBeforeSerialization, boolean initializeBeforeDeSerialization) throws Exception
    {
        if (initializeBeforeSerialization)
        {
            agent.initialize();
        }
        final ByRef<Boolean> matched = ByRef.create(Boolean.FALSE);
        StandaloneRhsFunctionHandler match = new StandaloneRhsFunctionHandler("match") {
            @Override
            public Symbol execute(RhsFunctionContext context, List<Symbol> arguments) throws RhsFunctionException
            {
                matched.value = true;
                return null;
            }
        };
        StandaloneRhsFunctionHandler rhsFailure = new StandaloneRhsFunctionHandler("rhs-failure") {
            @Override
            public Symbol execute(RhsFunctionContext context, List<Symbol> arguments) throws RhsFunctionException
            {
                throw new RhsFunctionException("rhs-failure should not be called");
            }
        };
        agent.getRhsFunctions().registerHandler(match);
        agent.getRhsFunctions().registerHandler(rhsFailure);
        
        // First a production to create some structure to copy
        agent.getProductions().loadProduction("" +
                "createStructure\n" +
                "(state <s> ^superstate nil)\n" +
                "-->\n" +
                "(<s> ^to-copy <tc>)" +
                "(<tc> ^value 1 ^location <loc> ^another <a>)" +
                "(<loc> ^x 123 ^y 3.14 ^name |hello| ^loop <tc> ^sub <sub>)" +
                "(<a> ^foo bar ^link <sub>)");
        
        // Next a production to copy the structure
        agent.getProductions().loadProduction("" +
                "copyStructure\n" +
                "(state <s> ^superstate nil ^to-copy <tc>)\n" +
                "-->\n" +
                "(<s> ^copy (deep-copy <tc>))");
        
        // Finally a production to validate that the structure is there
        agent.getProductions().loadProduction("" +
                "testStructure\n" +
                "(state <s> ^superstate nil ^to-copy <tc-old> ^copy <c>)\n" +
                "(<c> ^value 1 ^location <loc> ^another <a>)" +
                "(<loc> ^x 123 ^y 3.14 ^name |hello| ^loop <tc> ^sub <sub>)" +
                "(<a> ^foo bar ^link <sub>)\n" +
                "-->\n" +
                "(match)");
        
        Agent newAgent;
        if (initializeBeforeDeSerialization)
        {
            newAgent = new Agent(getContext());
            newAgent.getProductions().loadProduction("" +
                    "copyStructure\n" +
                    "(state <s> ^superstate nil)\n" +
                    "-->\n" +
                    "(rhs-failure)");
            newAgent.getRhsFunctions().registerHandler(rhsFailure);
            serializeInto(agent, newAgent);
        }
        else
        {
            newAgent = serialize(agent);
        }
        
        if (!initializeBeforeSerialization)
        {
            agent.initialize();
        }
        agent.getProperties().set(SoarProperties.WAITSNC, true);
        agent.runFor(2, RunType.DECISIONS);
        assertTrue(matched.value);

        matched.value = false;
        newAgent.getRhsFunctions().registerHandler(match);
        newAgent.getRhsFunctions().registerHandler(rhsFailure);
        newAgent.getProperties().set(SoarProperties.WAITSNC, true);
        newAgent.runFor(2, RunType.DECISIONS);
        assertTrue(matched.value);
        newAgent.dispose();
    }
    
    public void serializationStressTest() throws Exception
    {
        // Verify we don't get a stack overflow for reasonably sized agents.
        final int SIZE = 5000;
        final ByRef<HashSet<Long>> recordKeeper = ByRef.create(new HashSet<Long>());
        StandaloneRhsFunctionHandler record = new StandaloneRhsFunctionHandler("record"){
            @Override
            public Symbol execute(RhsFunctionContext context, List<Symbol> arguments) throws RhsFunctionException
            {
                recordKeeper.value.add(arguments.get(0).asInteger().getValue());
                return null;
            }
        };
        agent.initialize();
        agent.getRhsFunctions().registerHandler(record);
        
        for(int i = 0; i < SIZE; i++)
        {
            String name = "production-" + i;
            agent.getProductions().loadProduction("" +
                    name + "\n" +
                    "(state <s> ^superstate nil)\n" +
                    "-->\n" +
                    "(<s> ^value " + i + ")\n" +
                    "(record " + i + ")\n");
        }
        
        Agent newAgent = serialize(agent);
        for(Production p : newAgent.getProductions().getProductions(null))
        {
            assertNotNull(agent.getProductions().getProduction(p.getName()));
        }
        
        for(Production p : agent.getProductions().getProductions(null))
        {
            assertNotNull(newAgent.getProductions().getProduction(p.getName()));
        }
        
        agent.getProperties().set(SoarProperties.WAITSNC, true);
        agent.runFor(1, RunType.DECISIONS);
        
        for(long i = 0; i < SIZE; i++)
        {
            assertTrue(recordKeeper.value.contains(i));
        }
        
        recordKeeper.value = new HashSet<Long>();
        for(long i = 0; i < SIZE; i++)
        {
            assertFalse(recordKeeper.value.contains(i));
        }
        newAgent.getRhsFunctions().registerHandler(record);
        newAgent.getProperties().set(SoarProperties.WAITSNC, true);
        newAgent.runFor(1, RunType.DECISIONS);
        
        for(long i = 0; i < SIZE; i++)
        {
            assertTrue(recordKeeper.value.contains(i));
        }
        
        newAgent.dispose();
    }
    
    private Agent serialize(Agent agent) throws Exception
    {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        ReteSerializer.saveRete(agent, baos);
        baos.close();
        ByteArrayInputStream bais = new ByteArrayInputStream(baos.toByteArray());
        Agent newAgent = ReteSerializer.createAgent(bais, getContext());
        newAgent.initialize();
        return newAgent;
    }
    
    private void serializeInto(Agent fromAgent, Agent toAgent) throws Exception
    {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        ReteSerializer.saveRete(fromAgent, baos);
        baos.close();
        ByteArrayInputStream bais = new ByteArrayInputStream(baos.toByteArray());
        ReteSerializer.replaceRete(toAgent, bais);
    }
}
