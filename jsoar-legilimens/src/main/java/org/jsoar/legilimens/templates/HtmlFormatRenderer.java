/*
 * Copyright (c) 2009 Dave Ray <daveray@gmail.com>
 *
 * Created on Oct 12, 2009
 */
package org.jsoar.legilimens.templates;

import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;

import org.antlr.stringtemplate.AttributeRenderer;
import org.apache.commons.lang.StringEscapeUtils;

/**
 * @author ray
 */
public class HtmlFormatRenderer implements AttributeRenderer
{

    /* (non-Javadoc)
     * @see org.antlr.stringtemplate.AttributeRenderer#toString(java.lang.Object)
     */
    @Override
    public String toString(Object o)
    {
        return o.toString();
    }

    /* (non-Javadoc)
     * @see org.antlr.stringtemplate.AttributeRenderer#toString(java.lang.Object, java.lang.String)
     */
    @Override
    public String toString(Object o, String format)
    {
        if("h".equals(format))
        {
            return StringEscapeUtils.escapeHtml(o.toString());
        }
        else if("a".equals(format))
        {
            try
            {
                return URLEncoder.encode(o.toString(), "UTF-8");
            }
            catch (UnsupportedEncodingException e)
            {
                throw new RuntimeException(e.getMessage(), e);
            }
        }
        return o.toString();
    }

}
