/*
 * Copyright (c) 2009 Dave Ray <daveray@gmail.com>
 *
 * Created on Oct 14, 2009
 */
package org.jsoar.legilimens;

import org.restlet.Response;
import org.restlet.data.Form;

/**
 * @author ray
 */
public class RestletTools
{

    public static void setResponseHeader(Response response, String name, Object value)
    {
        Form responseHeaders = (Form) response.getAttributes().get("org.restlet.http.headers");  
        if (responseHeaders == null)  
        {  
            responseHeaders = new Form();  
            response.getAttributes().put("org.restlet.http.headers", responseHeaders);  
        }  
        responseHeaders.add(name, value.toString());  
    }
}
