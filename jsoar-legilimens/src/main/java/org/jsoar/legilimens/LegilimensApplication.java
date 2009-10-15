/*
 * Copyright (c) 2009 Dave Ray <daveray@gmail.com>
 *
 * Created on Oct 9, 2009
 */
package org.jsoar.legilimens;

import java.util.List;

import org.antlr.stringtemplate.CommonGroupLoader;
import org.antlr.stringtemplate.StringTemplate;
import org.antlr.stringtemplate.StringTemplateGroup;
import org.antlr.stringtemplate.StringTemplateGroupLoader;
import org.antlr.stringtemplate.language.DefaultTemplateLexer;
import org.jsoar.legilimens.resources.AgentPropertiesResource;
import org.jsoar.legilimens.resources.AgentResource;
import org.jsoar.legilimens.resources.AgentsResource;
import org.jsoar.legilimens.resources.CommandsResource;
import org.jsoar.legilimens.resources.ProductionResource;
import org.jsoar.legilimens.resources.ProductionsResource;
import org.jsoar.legilimens.resources.TraceResource;
import org.jsoar.legilimens.resources.WmesResource;
import org.jsoar.legilimens.templates.HtmlFormatRenderer;
import org.jsoar.legilimens.templates.TemplateErrorListener;
import org.jsoar.runtime.ThreadedAgent;
import org.restlet.Application;
import org.restlet.Restlet;
import org.restlet.data.LocalReference;
import org.restlet.resource.Directory;
import org.restlet.routing.Router;

/**
 * @author ray
 */
public class LegilimensApplication extends Application
{
    private final StringTemplateGroupLoader loader = new CommonGroupLoader("org/jsoar/legilimens/templates", new TemplateErrorListener());
    
    public LegilimensApplication()
    {
        StringTemplateGroup.registerGroupLoader(loader);

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

    public StringTemplate template(String name)
    {
        // first load main language template
        final StringTemplateGroup templates = StringTemplateGroup.loadGroup(name, DefaultTemplateLexer.class, null);
        templates.registerRenderer(String.class, new HtmlFormatRenderer());
        templates.setRefreshInterval(0);  // no caching
        templates.setRefreshInterval(Integer.MAX_VALUE);  // no refreshing
        return templates.getInstanceOf("main");
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
        router.attach("/agents/{agentName}/properties", AgentPropertiesResource.class);
        router.attach("/agents/{agentName}/wmes", WmesResource.class);
        router.attach("/agents/{agentName}/productions", ProductionsResource.class);
        router.attach("/agents/{agentName}/productions/{productionName}", ProductionResource.class);
        
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
