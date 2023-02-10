package org.jsoar.performancetesting.yaml;

import java.util.List;

/**
 * This is a configuration class for parsing the YAML files used by the
 * performance testing framework.
 * 
 * @author ALT
 *
 */
public class Configuration
{
    public static TestSettings defaultSettings;
    private List<ConfigurationTest> tests;
    
    public List<ConfigurationTest> getTests()
    {
        return tests;
    }
    
    public void setConfigurationTests(List<ConfigurationTest> tests)
    {
        this.tests = tests;
    }
    
    public TestSettings getDefaultSettings()
    {
        return Configuration.defaultSettings;
    }
    
    public void setDefaultSettings(TestSettings defaultSettings)
    {
        Configuration.defaultSettings = defaultSettings;
    }
}
