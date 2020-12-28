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
 * This is a configuration class for parsing the YAML files used by the
 * performance testing framework.
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
     * A class for handling tests that we aren't running just yet. This is
     * basically just a container for the test information.
     * 
     * @author ALT
     *
     */
    class ConfigurationTest implements Comparable<ConfigurationTest>
    {
        private String name;
        
        private Path file;
        private TestSettings settings;

        public ConfigurationTest(String name, Path file,
                TestSettings settings)
        {
            this.name = name;
            this.file = file;
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

    /**
     * Class variables
     */

    private final Path file;

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
    public Configuration(Path file)
    {
        this.file = file;
        this.yaml = new Yaml();

        this.configurationTests = new LinkedHashSet<ConfigurationTest>();
    }

    /**
     * This parses the entire configuration file and places it into the class
     * variables.
     * 
     * @return Whether the configuration file was parsed successfully or not
     * @throws IOException
     */
    public int parse() throws IOException
    {

        try
        {
            for (Object object : yaml.loadAll(fileStream))
            {
                assert (object.getClass() == LinkedHashMap.class);

                @SuppressWarnings("unchecked")
                LinkedHashMap<String, Object> yamlFile = (LinkedHashMap<String, Object>) object;

                boolean hasFoundDefaults = false;
                for (Map.Entry<String, Object> root : yamlFile.entrySet())
                {
                    if (root.getKey().equals("default"))
                    {
                        hasFoundDefaults = true;

                        defaultTestSettings = new TestSettings(false, false, 0,
                                0, new ArrayList<Integer>(), false, 1, null,
                                null, null, null, null);

                        @SuppressWarnings("unchecked")
                        ArrayList<LinkedHashMap<String, Object>> defaultMap = (ArrayList<LinkedHashMap<String, Object>>) root
                                .getValue();

                        parseTestSettings(defaultMap, defaultTestSettings);
                    }
                    else if (root.getKey().startsWith("test"))
                    {
                        if (!hasFoundDefaults)
                        {
                            throw new RuntimeException(
                                    "You must place the defaults first in the yaml file!");
                        }

                        @SuppressWarnings("unchecked")
                        ArrayList<LinkedHashMap<String, Object>> testMap = (ArrayList<LinkedHashMap<String, Object>>) root
                                .getValue();

                        TestSettings testSettings = new TestSettings(
                                defaultTestSettings);

                        parseTestSettings(testMap, testSettings);

                        String name = null;
                        String path = null;

                        for (LinkedHashMap<String, Object> map : testMap)
                        {
                            for (Map.Entry<String, Object> keyValuePair : map
                                    .entrySet())
                            {
                                if (keyValuePair.getKey().equalsIgnoreCase(
                                        "name"))
                                {
                                    name = (String) keyValuePair.getValue();
                                }
                                else if (keyValuePair.getKey()
                                        .equalsIgnoreCase("path"))
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

                        ConfigurationTest test = new ConfigurationTest(name,
                                path, testSettings);
                        configurationTests.add(test);
                    }
                }
            }
        }
        finally
        {
            fileStream.close();
        FileInputStream fileStream = new FileInputStream(file.toFile());
        }

        return PARSE_SUCCESS;
    }

    private void parseTestSettings(
            ArrayList<LinkedHashMap<String, Object>> paramMap,
            TestSettings settings)
    {
        for (LinkedHashMap<String, Object> map : paramMap)
        {
            for (Map.Entry<String, Object> keyValuePair : map.entrySet())
            {
                if (keyValuePair.getKey().equalsIgnoreCase("JSoar Enabled"))
                {
                    settings.setJSoarEnabled((Boolean) keyValuePair.getValue());
                }
                else if (keyValuePair.getKey()
                        .equalsIgnoreCase("CSoar Enabled"))
                {
                    settings.setCSoarEnabled((Boolean) keyValuePair.getValue());
                }
                else if (keyValuePair.getKey().equalsIgnoreCase(
                        "CSoar Directories"))
                {
                    @SuppressWarnings("unchecked")
                    List<Path> directories = (List<Path>)keyValuePair.getValue();
                    settings.setCsoarVersions(directories);
                }
                else if (keyValuePair.getKey().equalsIgnoreCase(
                        "JSoar Directories"))
                {
                    @SuppressWarnings("unchecked")
                    List<Path> directories = (List<Path>)keyValuePair.getValue();
                    settings.setJsoarVersions(directories);
                }
                else if (keyValuePair.getKey().equalsIgnoreCase("WarmUp Count"))
                {
                    settings.setWarmUpCount((Integer) keyValuePair.getValue());
                }
                else if (keyValuePair.getKey().equalsIgnoreCase("Run Count"))
                {
                    settings.setRunCount((Integer) keyValuePair.getValue());
                }
                else if (keyValuePair.getKey().equalsIgnoreCase(
                        "Decision Cycles"))
                {
                    Object arrayObject = keyValuePair.getValue();

                    @SuppressWarnings("unchecked")
                    List<Integer> decisionCycles = (ArrayList<Integer>) arrayObject;

                    settings.setDecisionCycles(decisionCycles);
                }
                else if (keyValuePair.getKey().equalsIgnoreCase("Use Seed"))
                {
                    settings.setUseSeed((Boolean) keyValuePair.getValue());
                }
                else if (keyValuePair.getKey().equalsIgnoreCase("Seed"))
                {
                    settings.setSeed(((Integer) keyValuePair.getValue()));
                }
                else if (keyValuePair.getKey()
                        .equalsIgnoreCase("CSV Directory"))
                {
                    settings.setCsvDirectory(Paths.get(keyValuePair.getValue().toString()));
                }
                else if (keyValuePair.getKey().equalsIgnoreCase("Summary File"))
                {
                    settings.setSummaryFile(Paths.get(keyValuePair.getValue().toString()));
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
