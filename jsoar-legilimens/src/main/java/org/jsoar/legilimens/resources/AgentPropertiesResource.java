/*
 * Copyright (c) 2009 Dave Ray <daveray@gmail.com>
 *
 * Created on Oct 9, 2009
 */
package org.jsoar.legilimens.resources;

import org.jsoar.util.properties.PropertyKey;
import org.restlet.resource.Get;

/**
 * @author ray
 */
public class AgentPropertiesResource extends BaseAgentResource
{

    @Get
    public String toString()
    {
        final StringBuilder b = new StringBuilder();
        b.append(agent.getName() + " properties\n");
        for(PropertyKey<?> key : agent.getProperties().getKeys())
        {
            final Object value = agent.getProperties().get(key);
            b.append(String.format("%s = %s\n", key, value));
        }
        return b.toString();
    }
}
