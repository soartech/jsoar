/*
 * Copyright (c) 2009  Dave Ray <daveray@gmail.com>
 *
 * Created on Apr 28, 2009
 */
package org.jsoar.kernel.io.xml;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertSame;

import org.jsoar.kernel.Agent;
import org.jsoar.kernel.RunType;
import org.jsoar.kernel.SoarProperties;
import org.jsoar.kernel.memory.Wme;
import org.jsoar.kernel.memory.Wmes;
import org.jsoar.kernel.memory.Wmes.MatcherBuilder;
import org.jsoar.kernel.symbols.Identifier;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

public class SoarTechXmlToWmeTest
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
    public void testFromXml() throws Exception
    {
        final Agent agent = new Agent();
                
        agent.getProperties().set(SoarProperties.WAITSNC, true);
        agent.getProductions().loadProduction(
                "testFromXml (state <s> ^superstate nil ^io.input-link <il>) -->" +
                "(<il> ^xml (from-st-xml |" +
                "<ignored>" +
                "<location link-id='L1'>" +
                "   <name>Ann Arbor</name>" +
                "   <population type='integer'>100000</population>" +
                "</location>" +
                "<person>" +
                "   <name>Bill</name>" +
                "   <where link='L1'/>" +
                "</person>" +
                "</ignored>|))");
        agent.runFor(1, RunType.DECISIONS);
        
        final Identifier il = agent.getInputOutput().getInputLink();
        final MatcherBuilder m = Wmes.matcher(agent);
        final Identifier xml = m.attr("xml").find(il).getValue().asIdentifier();
        assertNotNull(xml);
        
        final Wme location = m.attr("location").find(xml);
        assertNotNull(location);
        assertEquals("Ann Arbor", m.attr("name").find(location).getValue().asString().getValue());
        assertEquals(100000, m.attr("population").find(location).getValue().asInteger().getValue());
        
        final Wme person = m.attr("person").find(xml);
        assertNotNull(person);
        assertEquals("Bill", m.attr("name").find(person).getValue().asString().getValue());
        assertSame(location.getValue(), m.attr("where").find(person).getValue());
    }

    @Test
    public void testValueAttributeIsHandled() throws Exception
    {
        final Agent agent = new Agent();
                
        agent.getProperties().set(SoarProperties.WAITSNC, true);
        agent.getProductions().loadProduction(
                "testValueAttributeIsHandled (state <s> ^superstate nil ^io.input-link <il>) -->" +
                "(<il> ^xml (from-st-xml |" +
                "<ignored>" +
                "<name value='hello'/>" +
                "<age type='integer' value='33'/>" +
                "<weight type='double' value='180.5'/>" +
                "</ignored>|))");
        agent.runFor(1, RunType.DECISIONS);
        
        final Identifier il = agent.getInputOutput().getInputLink();
        final MatcherBuilder m = Wmes.matcher(agent);
        final Identifier xml = m.attr("xml").find(il).getValue().asIdentifier();
        assertNotNull(xml);
        
        assertEquals("hello", m.attr("name").find(xml).getValue().asString().getValue());
        assertEquals(33, m.attr("age").find(xml).getValue().asInteger().getValue());
        assertEquals(180.5, m.attr("weight").find(xml).getValue().asDouble().getValue(), 0.0001);
    }
}
