/*
 * Copyright (c) 2008  Dave Ray <daveray@gmail.com>
 *
 * Created on Oct 25, 2008
 */
package org.jsoar.kernel;

import java.io.IOException;
import java.io.InputStream;
import java.util.Properties;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

/**
 * @author ray
 */
public final class JSoarVersion
{
    private static final Log logger = LogFactory.getLog(JSoarVersion.class);
    
    private static JSoarVersion instance = new JSoarVersion();
        
    private Properties properties = new Properties();
    
    public static JSoarVersion getInstance()
    {
        return instance;
    }

    private JSoarVersion()
    {
        InputStream input = JSoarVersion.class.getResourceAsStream("/jsoar.buildinfo.properties");
        if(input != null)
        {
            try
            {
                properties.load(input);
            }
            catch (IOException e)
            {
                logger.error("Failed to load buildinfo properties: " + e.getMessage());
            }
            finally
            {
                try
                {
                    input.close();
                }
                catch (IOException e)
                {
                }
            }
        }
    }
    
    public String getVersion()
    {
        return properties.getProperty("jsoar.buildinfo.version", "0.0.0").toString();
    }
    
    public String getBuildDate()
    {
        return properties.getProperty("jsoar.buildinfo.date", "Unknown");
    }

    public String getBuiltBy()
    {
        return properties.getProperty("jsoar.buildinfo.builtBy", "Unknown");
    }
    
    public String getSvnRevision()
    {
        return properties.getProperty("jsoar.buildinfo.svn.revision", "Unknown");
    }
    
    public String getSvnUrl()
    {
        return properties.getProperty("jsoar.buildinfo.svn.url", "Unknown");
    }

    /* (non-Javadoc)
     * @see java.lang.Object#toString()
     */
    @Override
    public String toString()
    {
        return getVersion();
    }
}
