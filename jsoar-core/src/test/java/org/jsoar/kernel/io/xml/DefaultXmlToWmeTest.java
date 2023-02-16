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
import org.junit.jupiter.api.Test;

import com.google.common.collect.Iterators;

class DefaultXmlToWmeTest
{
    
    @Test
    void testFromXml() throws Exception
    {
        final Agent agent = setUpAndRunAgent(
                "testFromXml (state <s> ^superstate nil ^io.input-link <il>) -->" +
                        "(<il> ^xml (from-xml |" +
                        "<ignored>" +
                        "<location name='Ann Arbor' population='100000'>This is some text" +
                        "</location>" +
                        "<person>" +
                        "   <name>Bill</name>" +
                        "</person>" +
                        "</ignored>|))");
        
        final Identifier il = agent.getInputOutput().getInputLink();
        final MatcherBuilder m = Wmes.matcher(agent);
        final Identifier xml = m.attr("xml").find(il).getValue().asIdentifier();
        assertNotNull(xml);
        
        final Wme location = m.attr("location").find(xml);
        assertNotNull(location);
        final Wme locAttrs = m.attr(DefaultWmeToXml.ATTRS).find(location);
        assertNotNull(locAttrs);
        assertEquals("Ann Arbor", m.attr("name").find(locAttrs).getValue().asString().getValue());
        assertEquals("100000", m.attr("population").find(locAttrs).getValue().asString().getValue());
        
        assertEquals("This is some text", m.attr(DefaultWmeToXml.TEXT).find(location).getValue().asString().getValue());
        
        final Wme person = m.attr("person").find(xml);
        assertNotNull(person);
        
        assertSame(person.getValue(), m.attr(DefaultWmeToXml.NEXT).find(location).getValue());
        
        final Wme name = m.attr("name").find(person);
        assertEquals("Bill", m.attr(DefaultWmeToXml.TEXT).find(name).getValue().asString().getValue());
    }
    
    @Test
    void testFromXmlWithOnlyText() throws Exception
    {
        final Agent agent = setUpAndRunAgent(
                "testFromXml (state <s> ^superstate nil ^io.input-link <il>) -->" +
                        "(<il> ^xml (from-xml |<ignored>This is the only text in the document</ignored>|))");
        
        final Identifier il = agent.getInputOutput().getInputLink();
        final MatcherBuilder m = Wmes.matcher(agent);
        final Wme xml = m.attr("xml").find(il);
        assertNotNull(xml);
        assertEquals("This is the only text in the document", m.attr(DefaultWmeToXml.TEXT).find(xml).getValue().asString().getValue());
    }
    
    @Test
    void testFromXmlWithOnlyAttributes() throws Exception
    {
        final Agent agent = setUpAndRunAgent(
                "testFromXml (state <s> ^superstate nil ^io.input-link <il>) -->" +
                        "(<il> ^xml (from-xml |<ignored name='Boo' value='Radley'/>|))");
        
        final Identifier il = agent.getInputOutput().getInputLink();
        final MatcherBuilder m = Wmes.matcher(agent);
        final Identifier xml = m.attr("xml").find(il).getValue().asIdentifier();
        assertNotNull(xml);
        final Wme attrs = m.attr(DefaultWmeToXml.ATTRS).find(xml);
        assertNotNull(attrs);
        assertEquals(1, Iterators.size(xml.getWmes())); // Only /attrs
        
        assertEquals("Boo", m.attr("name").find(attrs).getValue().asString().getValue());
        assertEquals("Radley", m.attr("value").find(attrs).getValue().asString().getValue());
    }
    
    @Test
    void testFromXmlWithAttributesAndText() throws Exception
    {
        final Agent agent = setUpAndRunAgent(
                "testFromXml (state <s> ^superstate nil ^io.input-link <il>) -->" +
                        "(<il> ^xml (from-xml |<ignored name='Boo' value='Radley'>This is text</ignored>|))");
        
        final Identifier il = agent.getInputOutput().getInputLink();
        final MatcherBuilder m = Wmes.matcher(agent);
        final Identifier xml = m.attr("xml").find(il).getValue().asIdentifier();
        assertNotNull(xml);
        final Wme attrs = m.attr(DefaultWmeToXml.ATTRS).find(xml);
        assertNotNull(attrs);
        final Wme text = m.attr(DefaultWmeToXml.TEXT).find(xml);
        assertNotNull(text);
        assertEquals(2, Iterators.size(xml.getWmes())); // Only /attrs and /text
        
        assertEquals("Boo", m.attr("name").find(attrs).getValue().asString().getValue());
        assertEquals("Radley", m.attr("value").find(attrs).getValue().asString().getValue());
        assertEquals("This is text", text.getValue().asString().getValue());
    }
    
    @Test
    void testElementOrderIsPreservedThroughNextPointers() throws Exception
    {
        final Agent agent = setUpAndRunAgent(
                "testFromXml (state <s> ^superstate nil ^io.input-link <il>) -->" +
                        "(<il> ^xml (from-xml |<ignored><a/><b/><c/><d/></ignored>|))");
        
        final Identifier il = agent.getInputOutput().getInputLink();
        final MatcherBuilder m = Wmes.matcher(agent);
        final Identifier xml = m.attr("xml").find(il).getValue().asIdentifier();
        assertNotNull(xml);
        
        final Wme[] wmes = { m.attr("a").find(xml), m.attr("b").find(xml), m.attr("c").find(xml), m.attr("d").find(xml) };
        for(int i = 0; i < wmes.length; ++i)
        {
            Wme wme = wmes[i];
            assertNotNull(wme);
            if(i != 0)
            {
                Wme prev = wmes[i - 1];
                Wme next = m.attr(DefaultWmeToXml.NEXT).find(prev);
                assertNotNull(next);
                assertSame(prev.getValue(), next.getIdentifier());
                assertSame(next.getValue(), wme.getValue());
            }
        }
    }
    
    private Agent setUpAndRunAgent(String rule) throws Exception
    {
        final Agent agent = new Agent();
        
        agent.getTrace().disableAll();
        
        agent.getProperties().set(SoarProperties.WAITSNC, true);
        agent.getProductions().loadProduction(rule);
        agent.runFor(1, RunType.DECISIONS);
        return agent;
        
    }
}
