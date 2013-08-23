package org.jsoar.performancetesting;

import java.io.FileInputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.yaml.snakeyaml.Yaml;

/**
 * 
 * @author ALT
 *
 */
public class Configuration
{
	/**
	 * Package private classes for configuration.
	 */
	
	// Package Private
    /**
     * A class for handling tests that we aren't running just yet.
     * This is basically just a container for the test information.
     * 
     * @author ALT
     *
     */
    class ConfigurationTest implements Comparable<ConfigurationTest>
    {
        private String testName;
        private String testFile;
        private TestSettings settings;
        
        public ConfigurationTest(String testName, String testFile, TestSettings settings)
        {
            this.testName = testName;
            this.testFile = testFile;
            this.settings = settings;
        }
        
        public String getTestName()
        {
            return testName;
        }
        
        public String getTestFile()
        {
            return testFile;
        }
        
        public TestSettings getTestSettings()
        {
            return settings;
        }
        
        @Override
        public int compareTo(ConfigurationTest o)
        {
            return this.testName.compareTo(o.testName);
        }
    }
    
    /**
     * Class variables
     */
    
    private final String file;
    
    public static final int PARSE_FAILURE = 201;
    public static final int PARSE_SUCCESS = 200;
    
    private final Yaml yaml;
    
    private Set<ConfigurationTest> configurationTests;

    private TestSettings defaultTestSettings = null;
        
    /**
     * Initializes the Configuration class
     * 
     * @param file
     */
    public Configuration(String file)
    {
        this.file = file;
        this.yaml = new Yaml();
                
        this.configurationTests = new LinkedHashSet<ConfigurationTest>();
    }
    
    /**
     * This parses the entire configuration file and places it into the class variables.
     * 
     * @return Whether the configuration file was parsed successfully or not
     * @throws IOException
     */
    public int parse() throws IOException
    {   
        FileInputStream fileStream = new FileInputStream(file);

        try
        {
            for (Object object : yaml.loadAll(fileStream))
            {
                assert(object.getClass() == LinkedHashMap.class);

                @SuppressWarnings("unchecked")
                LinkedHashMap<String, Object> yamlFile = (LinkedHashMap<String, Object>)object;

                boolean hasFoundDefaults = false;
                for (Map.Entry<String, Object> root : yamlFile.entrySet())
                {
                    if (root.getKey().equals("default"))
                    {
                        hasFoundDefaults = true;

                        defaultTestSettings = new TestSettings(false, false, 0, 0, new ArrayList<Integer>(), false, 1, null, null, null, null);

                        @SuppressWarnings("unchecked")
                        ArrayList<LinkedHashMap<String, Object>> defaultMap = (ArrayList<LinkedHashMap<String, Object>>)root.getValue();

                        parseTestSettings(defaultMap, defaultTestSettings);
                    }
                    else if (root.getKey().startsWith("test"))
                    {
                        if (!hasFoundDefaults)
                        {
                            throw new RuntimeException("You must place the defaults first in the yaml file!");
                        }

                        @SuppressWarnings("unchecked")
                        ArrayList<LinkedHashMap<String, Object>> testMap = (ArrayList<LinkedHashMap<String, Object>>)root.getValue();

                        TestSettings testSettings = new TestSettings(defaultTestSettings);

                        parseTestSettings(testMap, testSettings);

                        String name = null;
                        String path = null;

                        for (LinkedHashMap<String, Object> map : testMap)
                        {
                            for (Map.Entry<String, Object> keyValuePair : map.entrySet())
                            {
                                if (keyValuePair.getKey().equalsIgnoreCase("name"))
                                {
                                    name = (String) keyValuePair.getValue();
                                }
                                else if (keyValuePair.getKey().equalsIgnoreCase("path"))
                                {
                                    path = (String) keyValuePair.getValue();
                                }

                                if (name != null && path != null)
                                {
                                    break;
                                }
                            }

                            if (name != null && path != null)
                            {
                                break;
                            }
                        }

                        if (name == null || path == null)
                        {
                            throw new RuntimeException("Malformed test!");
                        }

                        ConfigurationTest test = new ConfigurationTest(name, path, testSettings);
                        configurationTests.add(test);
                    }
                }
            }
        }
        finally
        {
            fileStream.close();
        }

        return PARSE_SUCCESS;
    }
    
    private void parseTestSettings(ArrayList<LinkedHashMap<String, Object>> paramMap, TestSettings settings)
    {
        for (LinkedHashMap<String, Object> map : paramMap)
        {
            for (Map.Entry<String, Object> keyValuePair : map.entrySet())
            {
                if (keyValuePair.getKey().equalsIgnoreCase("JSoar Enabled"))
                {
                    settings.setJSoarEnabled((Boolean)keyValuePair.getValue());
                }
                else if (keyValuePair.getKey().equalsIgnoreCase("CSoar Enabled"))
                {
                    settings.setCSoarEnabled((Boolean)keyValuePair.getValue());
                }
                else if (keyValuePair.getKey().equalsIgnoreCase("CSoar Directories"))
                {
                    Object arrayObject = keyValuePair.getValue();

                    @SuppressWarnings("unchecked")
                    List<String> directoriesUnchecked = (ArrayList<String>)arrayObject;
                    
                    List<String> directoriesChecked = new ArrayList<String>();
                    
                    for (String directory : directoriesUnchecked)
                    {
                        directoriesChecked.add(directory.replace("\\", "/"));
                    }
                    
                    settings.setCSoarVersions(directoriesChecked);
                }
                else if (keyValuePair.getKey().equalsIgnoreCase("WarmUp Count"))
                {
                    settings.setWarmUpCount((Integer)keyValuePair.getValue());
                }
                else if (keyValuePair.getKey().equalsIgnoreCase("Run Count"))
                {
                    settings.setRunCount((Integer)keyValuePair.getValue());
                }
                else if (keyValuePair.getKey().equalsIgnoreCase("Decision Cycles"))
                {
                    Object arrayObject = keyValuePair.getValue();
                    
                    @SuppressWarnings("unchecked")
                    List<Integer> decisionCycles = (ArrayList<Integer>)arrayObject;
                    
                    settings.setDecisionCycles(decisionCycles);
                }
                else if (keyValuePair.getKey().equalsIgnoreCase("Use Seed"))
                {
                    settings.setUseSeed((Boolean)keyValuePair.getValue());
                }
                else if (keyValuePair.getKey().equalsIgnoreCase("Seed"))
                {
                    settings.setSeed(((Integer)keyValuePair.getValue()));
                }
                else if (keyValuePair.getKey().equalsIgnoreCase("CSV Directory"))
                {
                    settings.setCSVDirectory((String) keyValuePair.getValue());
                }
                else if (keyValuePair.getKey().equalsIgnoreCase("Summary File"))
                {
                    settings.setSummaryFile((String) keyValuePair.getValue());
                }
                else if (keyValuePair.getKey().equalsIgnoreCase("JVM Settings"))
                {
                    settings.setJVMSettings((String) keyValuePair.getValue());
                }
            }
        }
    }
    
    /**
     * 
     * @return All the ConfigurationTest holders
     */
    public Set<ConfigurationTest> getConfigurationTests()
    {
        return configurationTests;
    }
    
    public TestSettings getDefaultSettings()
    {
        return defaultTestSettings;
    }
}
