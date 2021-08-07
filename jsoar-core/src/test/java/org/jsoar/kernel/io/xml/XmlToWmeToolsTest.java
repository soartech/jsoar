/*
 * Copyright (c) 2009 Dave Ray <daveray@gmail.com>
 *
 * Created on Jul 21, 2009
 */
package org.jsoar.kernel.io.xml;


import static org.junit.Assert.*;

import org.jsoar.kernel.Agent;
import org.jsoar.kernel.RunType;
import org.jsoar.kernel.events.InputEvent;
import org.jsoar.kernel.io.InputWme;
import org.jsoar.kernel.io.InputWmes;
import org.jsoar.kernel.memory.Wme;
import org.jsoar.kernel.memory.Wmes;
import org.jsoar.kernel.symbols.Symbols;
import org.jsoar.util.ByRef;
import org.jsoar.util.XmlTools;
import org.jsoar.util.events.SoarEvents;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

/**
 * @author ray
 */
public class XmlToWmeToolsTest
{
    private Agent agent;
    
    @Before
    public void setUp() throws Exception
    {
        this.agent = new Agent();
        this.agent.getTrace().disableAll();
    }

    @After
    public void tearDown() throws Exception
    {
        this.agent.dispose();
    }

    @Test
    public void testAddXmlInputWithDefaultAttribute() throws Exception
    {
        final XmlToWme converter = SoarTechXmlToWme.forInput(agent.getInputOutput());
        XmlToWmeTools.addXmlInput(agent.getInputOutput(), XmlTools.parse("<xml-input><name value='hi'/></xml-input>"), converter);
        
        agent.runFor(1, RunType.DECISIONS);
        
        final Wme root = Wmes.matcher(agent).attr("xml-input").find(agent.getInputOutput().getInputLink());
        assertNotNull(root);
        assertNotNull(Wmes.matcher(agent).attr("name").value("hi").find(root));
    }
    
    @Test
    public void testAddXmlInputWithExplicitAttribute() throws Exception
    {
        final XmlToWme converter = SoarTechXmlToWme.forInput(agent.getInputOutput());
        XmlToWmeTools.addXmlInput(agent.getInputOutput(), XmlTools.parse("<xml-input><name value='hi'/></xml-input>"), converter, "another-attr");
        
        agent.runFor(1, RunType.DECISIONS);
        
        final Wme root = Wmes.matcher(agent).attr("another-attr").find(agent.getInputOutput().getInputLink());
        assertNotNull(root);
        assertNotNull(Wmes.matcher(agent).attr("name").value("hi").find(root));
    }
    
    @Test
    public void testAddXmlInputToInputWmeWithExplicitAttribute() throws Exception
    {
        final ByRef<InputWme> loc = new ByRef<InputWme>();
        SoarEvents.listenForSingleEvent(agent.getEvents(), InputEvent.class,
                event -> loc.value = InputWmes.add(agent.getInputOutput(), "xml-data", Symbols.NEW_ID));
        
        agent.runFor(1, RunType.DECISIONS);
        
        final XmlToWme converter = SoarTechXmlToWme.forInput(agent.getInputOutput());
        XmlToWmeTools.addXmlInput(loc.value, XmlTools.parse("<xml-input><name value='hi'/></xml-input>"), converter, "another-attr");
        
        // Creates:
        // ^io.input-link.xml-data.another-attr.name |hi|
        agent.runFor(1, RunType.DECISIONS);
        
        final Wme root = Wmes.matcher(agent).attr("xml-data").find(agent.getInputOutput().getInputLink());
        assertNotNull(root);
        final Wme xmlData = Wmes.matcher(agent).attr("another-attr").find(root.getValue().asIdentifier());
        assertNotNull(xmlData);
        assertNotNull(Wmes.matcher(agent).attr("name").value("hi").find(xmlData.getValue().asIdentifier()));
    }
    
    @Test
    public void testAddXmlInputToInputWmeWithDefaultAttribute() throws Exception
    {
        final ByRef<InputWme> loc = new ByRef<InputWme>();
        SoarEvents.listenForSingleEvent(agent.getEvents(), InputEvent.class,
                event -> loc.value = InputWmes.add(agent.getInputOutput(), "xml-data", Symbols.NEW_ID));
        
        // Creates:
        // ^io.input-link.xml-data.xml-input.name |hi|
        agent.runFor(1, RunType.DECISIONS);
        
        final XmlToWme converter = SoarTechXmlToWme.forInput(agent.getInputOutput());
        XmlToWmeTools.addXmlInput(loc.value, XmlTools.parse("<xml-input><name value='hi'/></xml-input>"), converter);
        
        agent.runFor(1, RunType.DECISIONS);
        
        final Wme root = Wmes.matcher(agent).attr("xml-data").find(agent.getInputOutput().getInputLink());
        assertNotNull(root);
        final Wme xmlData = Wmes.matcher(agent).attr("xml-input").find(root.getValue().asIdentifier());
        assertNotNull(xmlData);
        assertNotNull(Wmes.matcher(agent).attr("name").value("hi").find(xmlData.getValue().asIdentifier()));
    }
}
