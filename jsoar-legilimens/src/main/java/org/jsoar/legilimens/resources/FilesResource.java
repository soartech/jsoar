/*
 * Copyright (c) 2009 Dave Ray <daveray@gmail.com>
 *
 * Created on Oct 9, 2009
 */
package org.jsoar.legilimens.resources;

import java.util.Comparator;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.TreeSet;

import org.jsoar.kernel.Production;
import org.jsoar.util.FileTools;

/**
 * @author ray
 */
public class FilesResource extends BaseAgentResource
{
    /*
     * (non-Javadoc)
     * 
     * @see org.jsoar.legilimens.resources.BaseAgentResource#setTemplateAttributes(java.util.Map)
     */
    @Override
    public void setTemplateAttributes(Map<String, Object> attrs)
    {
        super.setTemplateAttributes(attrs);
        
        final Set<File> files = new TreeSet<>(Comparator.comparing((File o1) -> o1.location));
        for(Production p : agent.getProductions().getProductions(null))
        {
            files.add(new File(p.getLocation().getFile()));
        }
        attrs.put("files", files);
    }
    
    public static class File
    {
        public final String location;
        
        public File(String location)
        {
            this.location = location;
        }
        
        /**
         * @return the location
         */
        public String getLocation()
        {
            return location;
        }
        
        public boolean isUrl()
        {
            return FileTools.asUrl(location) != null;
        }
        
        /*
         * (non-Javadoc)
         * 
         * @see java.lang.Object#hashCode()
         */
        @Override
        public int hashCode()
        {
            return Objects.hash(location);
        }
        
        /*
         * (non-Javadoc)
         * 
         * @see java.lang.Object#equals(java.lang.Object)
         */
        @Override
        public boolean equals(Object obj)
        {
            if(this == obj)
            {
                return true;
            }
            if(obj == null)
            {
                return false;
            }
            if(!(obj instanceof File))
            {
                return false;
            }
            File other = (File) obj;
            if(location == null)
            {
                if(other.location != null)
                {
                    return false;
                }
            }
            else if(!location.equals(other.location))
            {
                return false;
            }
            return true;
        }
        
    }
}
