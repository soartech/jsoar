/*
 * Copyright (c) 2008  Dave Ray <daveray@gmail.com>
 *
 * Created on Oct 25, 2008
 */
package org.jsoar.kernel;

import java.io.IOException;
import java.io.InputStream;
import java.util.Properties;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * @author ray
 */
public final class JSoarVersion
{
    private static final Logger LOG = LoggerFactory.getLogger(JSoarVersion.class);
    
    private static final String PREFIX = "jsoar-core.buildinfo";
    
    private static JSoarVersion instance = new JSoarVersion();
    
    private Properties properties = new Properties();
    
    public static JSoarVersion getInstance()
    {
        return instance;
    }
    
    private JSoarVersion()
    {
        try(InputStream input = JSoarVersion.class.getResourceAsStream("/jsoar-core.buildinfo.properties"))
        {
            if(input != null)
            {
                properties.load(input);
            }
        }
        catch(IOException e)
        {
            LOG.error("Failed to load buildinfo properties", e);
        }
    }
    
    public String getVersion()
    {
        return properties.getProperty(PREFIX + ".version", "0.0.0").toString();
    }
    
    public String getBuildDate()
    {
        return properties.getProperty(PREFIX + ".date", "Unknown");
    }
    
    public String getBuiltBy()
    {
        return properties.getProperty(PREFIX + ".builtBy", "Unknown");
    }
    
    /*
     * (non-Javadoc)
     * 
     * @see java.lang.Object#toString()
     */
    @Override
    public String toString()
    {
        return getVersion();
    }
}
