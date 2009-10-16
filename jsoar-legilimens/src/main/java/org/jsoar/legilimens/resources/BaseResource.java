/*
 * Copyright (c) 2009 Dave Ray <daveray@gmail.com>
 *
 * Created on Oct 9, 2009
 */
package org.jsoar.legilimens.resources;

import java.util.LinkedHashMap;
import java.util.Map;

import org.antlr.stringtemplate.StringTemplate;
import org.jsoar.legilimens.LegilimensApplication;
import org.restlet.data.MediaType;
import org.restlet.representation.Representation;
import org.restlet.representation.StringRepresentation;
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
        final StringTemplate t = template(templateName);
        return new StringRepresentation(t.toString(), MediaType.TEXT_HTML);
    }
    
    public final StringTemplate template(String name)
    {
        name = getTemplateName(name);
        final Map<String, Object> attrs = new LinkedHashMap<String, Object>();
        setTemplateAttributes(attrs);
        final StringTemplate t = getLegilimens().template(name);
        t.setAttribute("env", attrs);
        return t;
    }
    
    public void setTemplateAttributes(Map<String, Object> attrs)
    {
        attrs.put("resource", this);
        attrs.put("rootRef", getRootRef());
        attrs.put("resourceRef", getReference());
    }
    
    private String getTemplateName(String input)
    {
        if(input != null)
        {
            return input;
        }
        
        final String simpleName = getClass().getName();
        System.err.println("Name = " + simpleName);
        final String RESOURCE = "Resource";
        final int resourceStart = simpleName.indexOf(RESOURCE);
        final int nameStart = simpleName.lastIndexOf('.');
        final String base = simpleName.substring(nameStart + 1, resourceStart).toLowerCase();
        final int subStart = simpleName.lastIndexOf('$');
        final String sub = subStart != -1 ? simpleName.substring(subStart) : "";
        
        return base + (sub.isEmpty() ? "" : "_" + sub);
    }
}
