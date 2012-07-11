/*
 * Copyright (c) 2012 Dave Ray <daveray@gmail.com>
 *
 * Created on July 10, 2012
 */
package org.jsoar.kernel.io.xml;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import java.io.File;
import java.net.URISyntaxException;

import org.jsoar.kernel.Agent;
import org.jsoar.kernel.RunType;
import org.jsoar.kernel.SoarProperties;
import org.jsoar.kernel.memory.Wme;
import org.jsoar.kernel.memory.Wmes;
import org.jsoar.kernel.memory.Wmes.MatcherBuilder;
import org.jsoar.kernel.parser.ParserException;
import org.jsoar.kernel.rhs.ReordererException;
import org.jsoar.kernel.symbols.Identifier;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

public class AutoTypeXmlToWmeTest {
	private Agent agent;
    
    @Before
    public void setUp() throws Exception
    {
        this.agent = new Agent();
        this.agent.getTrace().disableAll();
        this.agent.initialize();
    }

    @After
    public void tearDown() throws Exception
    {
        this.agent.dispose();
    }
    
	@Test
	public void testXmlToWme() throws URISyntaxException {
		File testMsg = new File(this.getClass().getResource("testMessage.xml").toURI());
		assertTrue(testMsg.canRead());

		AutoTypeXmlToWme pat = new AutoTypeXmlToWme(agent.getInputOutput());
		pat.xmlToWme(testMsg);
		agent.runFor(1, RunType.DECISIONS);

		final MatcherBuilder m = Wmes.matcher(agent);
		final Identifier il = agent.getInputOutput().getInputLink();
		final Identifier msg = m.attr("Message").find(il).getValue().asIdentifier();
		assertNotNull(msg);

		assertEquals("", m.attr("TestEmpty").find(msg).getValue().asString().getValue());
		assertEquals("", m.attr("TestEmpty1").find(msg).getValue().asString().getValue());
		assertEquals("", m.attr("TestEmpty2").find(msg).getValue().asString().getValue());
		assertEquals("", m.attr("TestEmpty3").find(msg).getValue().asString().getValue());
		assertEquals("", m.attr("TestEmpty4").find(msg).getValue().asString().getValue());

		assertEquals(0d, m.attr("TestFloat").find(msg).getValue().asDouble().getValue(), .000001);
		assertTrue(m.attr("TestFloat").find(msg).getValue().asInteger() == null);
		assertTrue(m.attr("TestFloat").find(msg).getValue().asString() == null);
		
		assertEquals(0l, m.attr("TestInt").find(msg).getValue().asInteger().getValue());
		assertTrue(m.attr("TestInt").find(msg).getValue().asString() == null);
		assertTrue(m.attr("TestInt").find(msg).getValue().asDouble() == null);
		
		assertTrue(m.attr("Attribute").find(msg).getValue().asString() == null);
		final Identifier att = m.attr("Attribute").find(msg).getValue().asIdentifier();
		assertEquals("test", m.attr("myString").find(att).getValue().asString().getValue());
		assertEquals(1l, m.attr("myInt").find(att).getValue().asInteger().getValue());
		assertEquals(1d, m.attr("myFloat").find(att).getValue().asDouble().getValue(), .000001);
	}
	
	@Test
	public void testXmlToWmeSp() throws URISyntaxException, ReordererException, ParserException {
		agent.getProperties().set(SoarProperties.WAITSNC, true);
        agent.getProductions().loadProduction(
                "testFromXml (state <s> ^superstate nil ^io.input-link <il>) -->" +
                "(<il> ^xml (from-at-xml |" +
                "<ignored>" +
                "<location>" +
                "   <name>Ann Arbor</name>" +
                "   <population>100000</population>" +
                "</location>" +
                "<person>" +
                "   <name>Bill</name>" +
                "</person>" +
                "</ignored>|))");
        agent.runFor(1, RunType.DECISIONS);
        
        final Identifier il = agent.getInputOutput().getInputLink();
        final MatcherBuilder m = Wmes.matcher(agent);
        final Identifier xml = m.attr("xml").find(il).getValue().asIdentifier();
        assertNotNull(xml);
        final Identifier ignored = m.attr("ignored").find(xml).getValue().asIdentifier();
        assertNotNull(ignored);
        
        final Wme location = m.attr("location").find(ignored);
        assertNotNull(location);
        assertEquals("Ann Arbor", m.attr("name").find(location).getValue().asString().getValue());
        assertEquals(100000, m.attr("population").find(location).getValue().asInteger().getValue());
        
        final Wme person = m.attr("person").find(ignored);
        assertNotNull(person);
        assertEquals("Bill", m.attr("name").find(person).getValue().asString().getValue());
	}
}
