package org.jsoar.kernel.io.xml;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import java.io.File;
import java.net.URISyntaxException;

import org.jsoar.kernel.Agent;
import org.jsoar.kernel.RunType;
import org.jsoar.kernel.memory.Wmes;
import org.jsoar.kernel.memory.Wmes.MatcherBuilder;
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
		assertEquals("", m.attr("TestEmpty2").find(msg).getValue().asString().getValue());
		assertEquals("", m.attr("TestEmpty3").find(msg).getValue().asString().getValue());
		assertEquals("", m.attr("TestEmpty4").find(msg).getValue().asString().getValue());

		assertEquals(0d, m.attr("TestFloat").find(msg).getValue().asDouble().getValue(), .000001);
		assertTrue(m.attr("TestFloat").find(msg).getValue().asInteger() == null);
		assertTrue(m.attr("TestFloat").find(msg).getValue().asString() == null);
		
		assertEquals(0, m.attr("TestInt").find(msg).getValue().asInteger().getValue());
		assertTrue(m.attr("TestInt").find(msg).getValue().asString() == null);
		assertTrue(m.attr("TestInt").find(msg).getValue().asDouble() == null);
	}
}
