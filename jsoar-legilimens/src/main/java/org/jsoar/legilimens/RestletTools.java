/*
 * Copyright (c) 2009 Dave Ray <daveray@gmail.com>
 *
 * Created on Oct 14, 2009
 */
package org.jsoar.legilimens;

import org.restlet.Response;
import org.restlet.data.Header;
import org.restlet.engine.header.HeaderConstants;
import org.restlet.util.Series;

/**
 * @author ray
 */
public class RestletTools
{
    
    @SuppressWarnings({ "rawtypes", "unchecked" })
    public static void setResponseHeader(Response response, String name, Object value)
    {
        Series<Header> responseHeaders = (Series<Header>) response.getAttributes().get(HeaderConstants.ATTRIBUTE_HEADERS);
        if(responseHeaders == null)
        {
            responseHeaders = new Series(Header.class);
            response.getAttributes().put(HeaderConstants.ATTRIBUTE_HEADERS, responseHeaders);
        }
        responseHeaders.add(new Header(name, value.toString()));
    }
}
