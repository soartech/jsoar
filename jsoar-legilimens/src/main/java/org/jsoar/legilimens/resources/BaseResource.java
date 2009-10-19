/*
 * Copyright (c) 2009 Dave Ray <daveray@gmail.com>
 *
 * Created on Oct 9, 2009
 */
package org.jsoar.legilimens.resources;

import java.util.LinkedHashMap;
import java.util.Map;

import org.jsoar.legilimens.LegilimensApplication;
import org.restlet.data.MediaType;
import org.restlet.ext.freemarker.TemplateRepresentation;
import org.restlet.representation.Representation;
import org.restlet.resource.Get;
import org.restlet.resource.ResourceException;
import org.restlet.resource.ServerResource;

/**
 * @author ray
 */
public class BaseResource extends ServerResource
{
    private boolean edit;
    
    public boolean isEdit()
    {
        return edit;
    }
    
    /* (non-Javadoc)
     * @see org.jsoar.legilimens.resources.BaseAgentResource#doInit()
     */
    @Override
    protected void doInit() throws ResourceException
    {
        super.doInit();
        
        edit = "edit".equals(getRequest().getAttributes().get("action"));
    }
    
    public LegilimensApplication getLegilimens()
    {
        return (LegilimensApplication) getApplication();
    }
    
    @Get("html")
    public Representation getHtmlRepresentation()
    {
        return html(null);
    }
    
    public Representation html(String templateName)
    {
        return template(getTemplateName(templateName) + ".html.fmt", MediaType.TEXT_HTML);
    }
    
    public final TemplateRepresentation template(String name, MediaType type)
    {
        final Map<String, Object> attrs = new LinkedHashMap<String, Object>();
        setTemplateAttributes(attrs);
        return new TemplateRepresentation(name, getLegilimens().getFreeMarker(), attrs, type);
    }
    
    public void setTemplateAttributes(Map<String, Object> attrs)
    {
        attrs.put("resource", this);
        attrs.put("rootRef", getRootRef());
        attrs.put("resourceRef", getReference());
    }
    
    public String getTemplateName(String input)
    {
        if(input != null)
        {
            return input;
        }
        return getName();
    }
    
    public String getName()
    {
        final String simpleName = getClass().getName();
        final String RESOURCE = "Resource";
        final int resourceStart = simpleName.indexOf(RESOURCE);
        final int nameStart = simpleName.lastIndexOf('.');
        final String base = simpleName.substring(nameStart + 1, resourceStart).toLowerCase();
        final int subStart = simpleName.lastIndexOf('$');
        final String sub = subStart != -1 ? simpleName.substring(subStart) : "";
        
        return base + (sub.isEmpty() ? "" : "_" + sub);
    }
}
