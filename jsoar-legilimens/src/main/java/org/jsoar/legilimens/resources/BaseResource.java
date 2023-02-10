/*
 * Copyright (c) 2009 Dave Ray <daveray@gmail.com>
 *
 * Created on Oct 9, 2009
 */
package org.jsoar.legilimens.resources;

import java.io.UnsupportedEncodingException;
import java.net.URLDecoder;
import java.util.Date;
import java.util.LinkedHashMap;
import java.util.Map;

import org.jsoar.legilimens.LegilimensApplication;
import org.jsoar.legilimens.templates.TemplateMethods;
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
    private String action;
    
    public String getAction()
    {
        return action;
    }
    
    /*
     * (non-Javadoc)
     * 
     * @see org.jsoar.legilimens.resources.BaseAgentResource#doInit()
     */
    @Override
    protected void doInit() throws ResourceException
    {
        super.doInit();
        
        action = getPathAttribute("action");
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
        attrs.put("now", new Date());
        attrs.put("resource", this);
        attrs.put("rootRef", getRootRef());
        attrs.put("resourceRef", getReference());
        
        TemplateMethods.installMethods(attrs);
    }
    
    public String getTemplateName(String input)
    {
        if(input != null)
        {
            return input;
        }
        final String simpleName = getClass().getName();
        final String RESOURCE = "Resource";
        final int resourceStart = simpleName.indexOf(RESOURCE);
        final int nameStart = simpleName.lastIndexOf('.');
        final String base = simpleName.substring(nameStart + 1, resourceStart).toLowerCase();
        final int subStart = simpleName.lastIndexOf('$');
        final String sub = subStart != -1 ? simpleName.substring(subStart) : "";
        
        return base + (sub.isEmpty() ? "" : "_" + sub) + (action != null ? "_" + action : "");
    }
    
    /**
     * Return the name of the resource. Called by templates!
     * 
     * @return the name of the resource
     */
    @Override
    public String getName()
    {
        return getTemplateName(null);
    }
    
    /**
     * Retrieve the value of a path attribute with proper decoding.
     * 
     * @param name the name of the attribute
     * @return the decoded value, or {@code null} if not found
     */
    public String getPathAttribute(String name)
    {
        final Object value = getRequest().getAttributes().get(name);
        if(value == null)
        {
            return null;
        }
        try
        {
            return URLDecoder.decode(value.toString(), "UTF-8");
        }
        catch(UnsupportedEncodingException e)
        {
            throw new IllegalArgumentException(e.getMessage());
        }
    }
}
