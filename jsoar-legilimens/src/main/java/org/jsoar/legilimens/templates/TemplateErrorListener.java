package org.jsoar.legilimens.templates;

import org.antlr.stringtemplate.StringTemplateErrorListener;

/**
 * @author ray
 */
public class TemplateErrorListener implements
        StringTemplateErrorListener
{
    @Override
    public void warning(String arg0)
    {
        
    }

    @Override
    public void error(String arg0, Throwable arg1)
    {
        throw new RuntimeException(arg0, arg1);
    }
}