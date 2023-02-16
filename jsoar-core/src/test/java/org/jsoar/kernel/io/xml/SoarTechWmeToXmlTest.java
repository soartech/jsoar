/*
 * Copyright (c) 2009  Dave Ray <daveray@gmail.com>
 *
 * Created on Apr 28, 2009
 */
package org.jsoar.kernel.io.xml;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

import java.util.List;
import java.util.concurrent.atomic.AtomicReference;

import org.jsoar.kernel.Agent;
import org.jsoar.kernel.RunType;
import org.jsoar.kernel.rhs.functions.RhsFunctionContext;
import org.jsoar.kernel.rhs.functions.RhsFunctionException;
import org.jsoar.kernel.rhs.functions.StandaloneRhsFunctionHandler;
import org.jsoar.kernel.symbols.Symbol;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

public class SoarTechWmeToXmlTest
{
    
    @BeforeEach
    public void setUp() throws Exception
    {
    }
    
    @AfterEach
    public void tearDown() throws Exception
    {
    }
    
    @Test
    public void testToXml() throws Exception
    {
        final Agent agent = new Agent();
        agent.getProductions().loadProduction(
                "testToXmlInit (state <s> ^superstate nil ^io.input-link <il>) -->" +
                        "(<il> ^location <loc> ^person <person>)" +
                        "(<loc> ^name |Ann Arbor| ^population 100000)" +
                        "(<person> ^name Bill ^where <loc>)");
        
        final AtomicReference<String> result = new AtomicReference<>();
        agent.getRhsFunctions().registerHandler(new StandaloneRhsFunctionHandler("finish")
        {
            
            @Override
            public Symbol execute(RhsFunctionContext context,
                    List<Symbol> arguments) throws RhsFunctionException
            {
                result.set(arguments.get(0).toString());
                return null;
            }
        });
        
        agent.getProductions().loadProduction(
                "testToXml (state <s> ^superstate nil ^io.input-link <il>) (<il> ^location <loc>) -->" +
                        "(finish (to-st-xml --root something <il>))");
        
        agent.runFor(1, RunType.DECISIONS);
        assertNotNull(result.get());
        
        assertEquals("<?xml version=\"1.0\" encoding=\"UTF-8\" standalone=\"no\"?>" +
                "<something>" +
                "<location link-id=\"L1\">" +
                "<name>Ann Arbor</name>" +
                "<population type=\"integer\">100000</population>" +
                "</location>" +
                "<person>" +
                "<name>Bill</name>" +
                "<where link=\"L1\"/>" +
                "</person>" +
                "</something>", result.get());
    }
    
}
