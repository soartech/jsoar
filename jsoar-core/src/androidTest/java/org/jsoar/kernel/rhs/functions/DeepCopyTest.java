/*
 * Copyright (c) 2008  Dave Ray <daveray@gmail.com>
 *
 * Created on Dec 15, 2008
 */
package org.jsoar.kernel.rhs.functions;

import android.test.AndroidTestCase;

import org.jsoar.kernel.Agent;
import org.jsoar.kernel.RunType;
import org.jsoar.kernel.SoarProperties;
import org.jsoar.kernel.symbols.Symbol;
import org.jsoar.util.ByRef;

import java.util.List;

/**
 * @author ray
 */
public class DeepCopyTest extends AndroidTestCase
{
    private Agent agent;

    /**
     * @throws java.lang.Exception
     */
    @Override
    public void setUp() throws Exception
    {
        this.agent = new Agent(getContext());
    }

    public void testExecute() throws Exception
    {
        final ByRef<Boolean> matched = ByRef.create(Boolean.FALSE);
        agent.getRhsFunctions().registerHandler(new StandaloneRhsFunctionHandler("match"){

            @Override
            public Symbol execute(RhsFunctionContext context, List<Symbol> arguments) throws RhsFunctionException
            {
                matched.value = true;
                return null;
            }});
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
        
        agent.getProperties().set(SoarProperties.WAITSNC, true);
        agent.runFor(2, RunType.DECISIONS);
        
        assertTrue(matched.value);
    }

}
