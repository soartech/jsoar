/*
 * Copyright (c) 2009 Dave Ray <daveray@gmail.com>
 *
 * Created on Oct 9, 2009
 */
package org.jsoar.legilimens;

import java.io.IOException;
import java.util.List;

import org.jsoar.legilimens.resources.AgentResource;
import org.jsoar.legilimens.resources.AgentsResource;
import org.jsoar.legilimens.resources.CommandsResource;
import org.jsoar.legilimens.resources.FilesResource;
import org.jsoar.legilimens.resources.ProductionResource;
import org.jsoar.legilimens.resources.ProductionsResource;
import org.jsoar.legilimens.resources.PropertiesResource;
import org.jsoar.legilimens.resources.TraceResource;
import org.jsoar.legilimens.resources.WmesResource;
import org.jsoar.legilimens.trace.AgentTraceBuffer;
import org.jsoar.runtime.ThreadedAgent;
import org.jsoar.runtime.ThreadedAgentAttachedEvent;
import org.jsoar.runtime.ThreadedAgentDetachedEvent;
import org.jsoar.util.events.SoarEventListener;
import org.restlet.Application;
import org.restlet.Restlet;
import org.restlet.data.LocalReference;
import org.restlet.resource.Directory;
import org.restlet.routing.Router;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import freemarker.template.Configuration;

/**
 * @author ray
 */
public class LegilimensApplication extends Application
{
    private static final Logger LOG = LoggerFactory.getLogger(LegilimensApplication.class);
    
    private final Configuration fmc = new Configuration(Configuration.VERSION_2_3_28);
    private final SoarEventListener attachListener = event -> agentAttached(((ThreadedAgentAttachedEvent) event).getAgent());
    private final SoarEventListener detachListener = event -> agentDetached(((ThreadedAgentDetachedEvent) event).getAgent());
    
    public LegilimensApplication()
    {
        LOG.info("Legilimens application constructed");
        
        fmc.setURLEscapingCharset("UTF-8");
        fmc.setClassForTemplateLoading(getClass(), "/org/jsoar/legilimens/templates");
        
        getTunnelService().setEnabled(true);
        getTunnelService().setExtensionsTunnel(true);
        
        for(ThreadedAgent agent : ThreadedAgent.getAll())
        {
            agentAttached(agent);
        }
        ThreadedAgent.getEventManager().addListener(ThreadedAgentAttachedEvent.class, attachListener);
        ThreadedAgent.getEventManager().addListener(ThreadedAgentDetachedEvent.class, detachListener);
    }
    
    public List<ThreadedAgent> getAgents()
    {
        return ThreadedAgent.getAll();
    }
    
    public ThreadedAgent getAgent(String name)
    {
        for(ThreadedAgent agent : getAgents())
        {
            if(agent.getName().equals(name))
            {
                return agent;
            }
        }
        return null;
    }
    
    public Configuration getFreeMarker()
    {
        return fmc;
    }
    
    /*
     * (non-Javadoc)
     * 
     * @see org.restlet.Application#createInboundRoot()
     */
    @Override
    public Restlet createInboundRoot()
    {
        final Router router = new Router(getContext());
        router.attach("/", AgentsResource.class);
        router.attach("/agents", AgentsResource.class);
        router.attach("/agents/{agentName}", AgentResource.class);
        router.attach("/agents/{agentName}/trace", TraceResource.class);
        router.attach("/agents/{agentName}/commands", CommandsResource.class);
        router.attach("/agents/{agentName}/properties", PropertiesResource.class);
        router.attach("/agents/{agentName}/files", FilesResource.class);
        router.attach("/agents/{agentName}/wmes", WmesResource.class);
        router.attach("/agents/{agentName}/productions", ProductionsResource.class);
        router.attach("/agents/{agentName}/productions/{productionName}", ProductionResource.class);
        router.attach("/agents/{agentName}/productions/{productionName}/{action}", ProductionResource.class);
        
        attachPublicResource(router, "/images");
        attachPublicResource(router, "/javascripts");
        attachPublicResource(router, "/stylesheets");
        
        return router;
    }
    
    private static void attachPublicResource(Router router, String name)
    {
        router.attach(name, new Directory(router.getContext(),
                LocalReference.createClapReference(LocalReference.CLAP_THREAD, "/org/jsoar/legilimens/public" + name)));
    }
    
    protected void agentAttached(ThreadedAgent agent)
    {
        LOG.info("Attaching to agent '{}' (agent's name may not be accurate if it isn't set yet)", agent);
        try
        {
            AgentTraceBuffer.attach(agent.getAgent());
        }
        catch(IOException e)
        {
            LOG.error("Failed to attach trace buffer to agent '{}'", agent, e);
        }
    }
    
    protected void agentDetached(ThreadedAgent agent)
    {
        LOG.info("Detaching from agent '{}'", agent);
        final AgentTraceBuffer traceBuffer = agent.getProperties().get(AgentTraceBuffer.KEY);
        try
        {
            traceBuffer.detach();
        }
        catch(IOException e)
        {
            LOG.error("Failed to detach trace buffer from agent '{}'", agent, e);
        }
    }
    
}
