/*
 * Copyright (c) 2009 Dave Ray <daveray@gmail.com>
 *
 * Created on Oct 9, 2009
 */
package org.jsoar.legilimens.resources;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Map;

import org.jsoar.kernel.Production;

/**
 * @author ray
 */
public class ProductionsResource extends BaseAgentResource
{
    /* (non-Javadoc)
     * @see org.jsoar.legilimens.resources.BaseAgentResource#setTemplateAttributes(java.util.Map)
     */
    @Override
    public void setTemplateAttributes(Map<String, Object> attrs)
    {
        super.setTemplateAttributes(attrs);

        final List<Production> rules = new ArrayList<Production>();
        for(Production p : agent.getProductions().getProductions(null))
        {
            rules.add(p);
        }
        Collections.sort(rules, new Comparator<Production>()
        {
            @Override
            public int compare(Production o1, Production o2)
            {
                return o1.getName().toString().compareToIgnoreCase(o2.getName().toString());
            }
        });
        attrs.put("productions", rules);
    }
    
}
