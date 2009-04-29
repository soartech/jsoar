/*
 * (c) 2009  Soar Technology, Inc.
 *
 * Created on Apr 28, 2009
 */
package org.jsoar.kernel.io.xml;

import static org.junit.Assert.*;

import org.jsoar.kernel.Agent;
import org.jsoar.kernel.RunType;
import org.jsoar.kernel.SoarProperties;
import org.jsoar.kernel.memory.Wme;
import org.jsoar.kernel.memory.Wmes;
import org.jsoar.kernel.memory.Wmes.MatcherBuilder;
import org.jsoar.kernel.symbols.Identifier;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

public class XmlTreeToWmeTest
{

    @Before
    public void setUp() throws Exception
    {
    }

    @After
    public void tearDown() throws Exception
    {
    }

    @Test
    public void testFromXml() throws Exception
    {
        final Agent agent = new Agent();
        
        agent.initialize();
        
        agent.getProperties().set(SoarProperties.WAITSNC, true);
        agent.getProductions().loadProduction(
                "testFromXml (state <s> ^superstate nil ^io.input-link <il>) -->" +
                "(<il> ^xml (from-xml |" +
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

}
