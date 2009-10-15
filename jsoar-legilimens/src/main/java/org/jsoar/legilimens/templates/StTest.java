/*
 * Copyright (c) 2009 Dave Ray <daveray@gmail.com>
 *
 * Created on Oct 11, 2009
 */
package org.jsoar.legilimens.templates;

import org.antlr.stringtemplate.CommonGroupLoader;
import org.antlr.stringtemplate.StringTemplate;
import org.antlr.stringtemplate.StringTemplateGroup;
import org.antlr.stringtemplate.StringTemplateGroupLoader;
import org.antlr.stringtemplate.language.DefaultTemplateLexer;

/**
 * @author ray
 */
public class StTest
{

    /**
     * @param args
     */
    public static void main(String[] args)
    {
        StringTemplateGroupLoader loader =
            new CommonGroupLoader("org/jsoar/legilimens/templates", new TemplateErrorListener());
        StringTemplateGroup.registerGroupLoader(loader);

        // first load main language template
        StringTemplateGroup coreTemplates = StringTemplateGroup.loadGroup("agents", DefaultTemplateLexer.class, null);
        coreTemplates.registerRenderer(String.class, new HtmlFormatRenderer());
        StringTemplate t = coreTemplates.getInstanceOf("layout_main");
        t.setAttribute("env", "This & that");
        System.out.println(t.toString());
    }

}
