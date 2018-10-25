/*
 * Copyright (c) 2009 Dave Ray <daveray@gmail.com>
 *
 * Created on Jul 4, 2009
 */
package org.jsoar.debugger.plugins;

import java.io.IOException;

import org.jsoar.debugger.JSoarDebugger;
import org.jsoar.debugger.JSoarDebuggerPlugin;
import org.jsoar.kernel.SoarException;
import org.jsoar.kernel.io.xml.XmlMessageQueue;
import org.jsoar.runtime.ThreadedAgent;
import org.jsoar.util.XmlTools;
import org.jsoar.util.commands.SoarCommand;
import org.jsoar.util.commands.SoarCommandContext;
import org.w3c.dom.Document;
import org.xml.sax.SAXException;

/**
 * Simple plugin with an interface for testing {@link XmlMessageQueuePlugin}.
 * Installs a default message queue on the agent along with a command called
 * 'xmq-add' that enqueues arbitrary XML onto the queue.
 * 
 * @author ray
 */
public class XmlMessageQueuePlugin implements JSoarDebuggerPlugin
{
    private XmlMessageQueue queue;

    /* (non-Javadoc)
     * @see org.jsoar.debugger.JSoarDebuggerPlugin#initialize(org.jsoar.debugger.JSoarDebugger, java.lang.String[])
     */
    @Override
    public void initialize(JSoarDebugger debugger, String[] args)
    {
        final ThreadedAgent agent = debugger.getAgent();
        
        this.queue = XmlMessageQueue.newBuilder(agent.getInputOutput()).create();
        
        agent.getInterpreter().addCommand("xmq-add", new SoarCommand() {
            @Override
            public Object getCommand() {
                //todo - when implementing picocli, return the runnable
                return null;
            }
            @Override
            public String execute(SoarCommandContext commandContext, String[] args) throws SoarException
            {
                if(args.length != 2)
                {
                    throw new SoarException("Expected 1 argument, got " + (args.length - 1));
                }
                final String message = args[1];
                try
                {
                    final Document doc = XmlTools.parse(message);
                    queue.add(doc.getDocumentElement());
                    agent.getPrinter().print("Added message to message queue").flush();
                }
                catch (SAXException e)
                {
                    throw new SoarException(e.getMessage(), e);
                }
                catch (IOException e)
                {
                    throw new SoarException(e.getMessage(), e);
                }
                return "";
            }});
        
        agent.getPrinter().print("Registered command 'xmq-add'. Usage: xmq-add <xml>").flush();
    }

    /* (non-Javadoc)
     * @see org.jsoar.debugger.JSoarDebuggerPlugin#shutdown()
     */
    @Override
    public void shutdown()
    {
        if(this.queue != null)
        {
            this.queue.dispose();
            this.queue = null;
        }
        
        // TODO cleanup command
    }

}
