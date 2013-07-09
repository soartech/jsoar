/*
 * Copyright (c) 2009 Dave Ray <daveray@gmail.com>
 *
 * Created on Jul 3, 2009
 */
package org.jsoar.kernel.io.xml;


import static org.junit.Assert.*;

import java.util.List;

import org.jsoar.kernel.Agent;
import org.jsoar.kernel.RunType;
import org.jsoar.kernel.SoarProperties;
import org.jsoar.kernel.io.CycleCountInput;
import org.jsoar.kernel.rhs.functions.RhsFunctionContext;
import org.jsoar.kernel.rhs.functions.RhsFunctionException;
import org.jsoar.kernel.rhs.functions.StandaloneRhsFunctionHandler;
import org.jsoar.kernel.symbols.Symbol;
import org.jsoar.util.XmlTools;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

/**
 * @author ray
 */
public class XmlMessageQueueTest
{
    private static class MatchFunction extends StandaloneRhsFunctionHandler
    {
        int count = 0;
        public MatchFunction()
        {
            super("match");
        }

        @Override
        public Symbol execute(RhsFunctionContext context, List<Symbol> arguments)
                throws RhsFunctionException
        {
            ++count;
            return null;
        }
    }
    
    private Agent agent;
    private MatchFunction match;
    
    @Before
    public void setUp() throws Exception
    {
        this.agent = new Agent(false);
        agent.getTrace().disableAll();
        this.match = new MatchFunction();
        this.agent.getRhsFunctions().registerHandler(match);
        new CycleCountInput(this.agent.getInputOutput());
        this.agent.initialize();
    }

    /**
     * @throws java.lang.Exception
     */
    @After
    public void tearDown() throws Exception
    {
        this.agent.dispose();
        this.agent = null;
    }
    
    @Test public void testMessageQueueRootIsCreated() throws Exception
    {
        XmlMessageQueue.newBuilder(agent.getInputOutput()).queueName("test-queue-root").create();
        
        agent.getProductions().loadProduction("testMessageQueueRootIsCreated (state <s> ^superstate nil ^io.input-link.test-queue-root) --> (match)");
        
        agent.runFor(1, RunType.DECISIONS);
        assertEquals(1, match.count);
    }
    
    @Test public void testMessagesAreAddedToQueue() throws Exception
    {
        final XmlMessageQueue queue = XmlMessageQueue.newBuilder(agent.getInputOutput()).queueName("test-messages").create();
        
        agent.getProductions().loadProduction("checkForMessages" +
        		"(state <s> ^superstate nil ^io.input-link.test-messages <r>)" +
        		"(<r> ^a <a> ^b <b> ^c <c>)" +
        		"(<a> ^/text |message a| ^/next <b> -^/previous)" +
        		"(<b> ^/text |message b| ^/previous <a> ^/next <c>)" +
        		"(<c> ^/text |message c| ^/previous <b> -^/next)" +
        		"--> (match)");
        agent.runFor(1, RunType.DECISIONS);
        assertEquals(0, match.count);
        
        queue.add(XmlTools.parse("<a>message a</a>").getDocumentElement());
        queue.add(XmlTools.parse("<b>message b</b>").getDocumentElement());
        queue.add(XmlTools.parse("<c>message c</c>").getDocumentElement());
        agent.runFor(1, RunType.DECISIONS);
        assertEquals(1, match.count);
    }
    
    @Test public void testMessagesAreRemovedFromQueueAfterTimeToLiveExpires() throws Exception
    {
        final XmlMessageQueue queue = XmlMessageQueue.newBuilder(agent.getInputOutput()).timeToLive(20).queueName("test-messages").create();
        
        agent.getProperties().set(SoarProperties.WAITSNC, true);
        agent.getProductions().loadProduction("checkForMessages" +
                "(state <s> ^superstate nil ^io.input-link.test-messages <r>)" +
                "(<r> ^a <a> ^b <b> ^c <c>)" +
                "(<a> ^/text |message a| ^/next <b> -^/previous)" +
                "(<b> ^/text |message b| ^/previous <a> ^/next <c>)" +
                "(<c> ^/text |message c| ^/previous <b> -^/next)" +
                "--> (match)");
        agent.getProductions().loadProduction("checkForRemoval" +
                "(state <s> ^superstate nil ^io.input-link <il>)" +
                "(<il> ^cycle-count > 2 < 24 ^test-messages <r>)" +
                "(<r> -^a -^b -^c)" +
                "-->" +
                "(match)");
        
        agent.runFor(1, RunType.DECISIONS);
        assertEquals(0, match.count);
        
        queue.add(XmlTools.parse("<a>message a</a>").getDocumentElement());
        queue.add(XmlTools.parse("<b>message b</b>").getDocumentElement());
        queue.add(XmlTools.parse("<c>message c</c>").getDocumentElement());
        agent.runFor(1, RunType.DECISIONS);
        assertEquals(1, match.count);
        agent.runFor(20, RunType.DECISIONS);
        assertEquals(2, match.count);
    }
}
