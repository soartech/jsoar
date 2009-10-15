/*
 * Copyright (c) 2009 Dave Ray <daveray@gmail.com>
 *
 * Created on Oct 9, 2009
 */
package org.jsoar.legilimens.resources;

import java.nio.charset.Charset;
import java.util.LinkedHashMap;
import java.util.Map;

import org.antlr.stringtemplate.StringTemplate;
import org.jsoar.legilimens.LegilimensApplication;
import org.restlet.data.MediaType;
import org.restlet.representation.Representation;
import org.restlet.representation.StringRepresentation;
import org.restlet.resource.Get;
import org.restlet.resource.ServerResource;

/**
 * @author ray
 */
public class BaseResource extends ServerResource
{
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
        final String simpleName = getClass().getSimpleName();
        System.err.println("Simple name = " + simpleName);
        final int i = simpleName.indexOf("Resource");
        return simpleName.substring(0, i).toLowerCase();
    }
}
