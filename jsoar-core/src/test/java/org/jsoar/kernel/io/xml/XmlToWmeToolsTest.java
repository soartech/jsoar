/*
 * Copyright (c) 2009 Dave Ray <daveray@gmail.com>
 *
 * Created on Jul 21, 2009
 */
package org.jsoar.kernel.io.xml;


import static org.junit.Assert.*;

import org.jsoar.kernel.Agent;
import org.jsoar.kernel.RunType;
import org.jsoar.kernel.memory.Wme;
import org.jsoar.kernel.memory.Wmes;
import org.jsoar.util.XmlTools;
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
        this.agent.initialize();
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
}
