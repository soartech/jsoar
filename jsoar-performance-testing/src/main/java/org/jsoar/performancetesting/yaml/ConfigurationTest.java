package org.jsoar.performancetesting.yaml;

import java.nio.file.Path;

// Package Private
/**
 * A class for handling tests that we aren't running just yet. This is
 * basically just a container for the test information.
 * 
 * @author ALT
 *
 */
public class ConfigurationTest implements Comparable<ConfigurationTest>
{
    private String name;
    
    private Path file;
    
    private TestSettings settings = Configuration.defaultSettings;
    
    public TestSettings getSettings()
    {
        return settings;
    }
    
    public void setSettings(TestSettings settings)
    {
        this.settings = settings;
    }
    
    public void setName(String name)
    {
        this.name = name;
    }
    
    public void setFile(Path file)
    {
        this.file = file;
    }
    
    public String getName()
    {
        return name;
    }
    
    public Path getFile()
    {
        return file;
    }
    
    public TestSettings getTestSettings()
    {
        return settings;
    }
    
    @Override
    public int compareTo(ConfigurationTest o)
    {
        return this.name.compareTo(o.name);
    }
}
