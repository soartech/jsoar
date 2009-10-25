/*
 * Copyright (c) 2009 Dave Ray <daveray@gmail.com>
 *
 * Created on Oct 9, 2009
 */
package org.jsoar.legilimens;

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
import org.jsoar.runtime.ThreadedAgent;
import org.restlet.Application;
import org.restlet.Restlet;
import org.restlet.data.LocalReference;
import org.restlet.resource.Directory;
import org.restlet.routing.Router;

import freemarker.template.Configuration;

/**
 * @author ray
 */
public class LegilimensApplication extends Application
{
    private final Configuration fmc = new Configuration();
    
    public LegilimensApplication()
    {
        fmc.setURLEscapingCharset("UTF-8");
        fmc.setClassForTemplateLoading(getClass(), "/org/jsoar/legilimens/templates");
        
        getTunnelService().setEnabled(true);
        getTunnelService().setExtensionsTunnel(true);
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

    /* (non-Javadoc)
     * @see org.restlet.Application#createInboundRoot()
     */
    @Override
    public Restlet createInboundRoot()
    {
        final Router router = new Router(getContext());
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

}
