package org.jsoar.performancetesting;

import java.io.BufferedReader;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Properties;

public class Configuration
{
    public class ConfigurationTest
    {
        private String testName;
        private String testFile;
        private String testCategory;
        
        public ConfigurationTest(String testName, String testFile, String testCategory)
        {
            this.testName = testName;
            this.testFile = testFile;
            this.testCategory = testCategory;
        }
        
        public String getTestName()
        {
            return testName;
        }
        
        public String getTestFile()
        {
            return testFile;
        }
        
        public String getTestCategory()
        {
            return testCategory;
        }
        
        public void setTestCategory(String testCategory)
        {
            this.testCategory = testCategory;
        }
    }
    
    public class ConfigurationCategory
    {
        private final String categoryName;
        private final List<String> categoryTests;
        
        public ConfigurationCategory(String categoryName, List<String> categoryTests)
        {
            this.categoryName = categoryName;
            this.categoryTests = categoryTests;
        }
        
        public String getCategoryName()
        {
            return categoryName;
        }
        
        public List<String> getCategoryTests()
        {
            return categoryTests;
        }
        
        public boolean containsTest(String test)
        {
            return categoryTests.contains(test);
        }
        
        public boolean addTest(String test)
        {
            if (containsTest(test))
            {
                return false;
            }
            
            categoryTests.add(test);
            
            return true;
        }
    }
    
    public class UnknownPropertyException extends Exception
    {
        /**
         * 
         */
        private static final long serialVersionUID = 463144412019989054L;
        private final String property;
        
        public UnknownPropertyException(String property)
        {
            super("Unknown Property: " + property);
            
            this.property = property;
        }
        
        public String getProperty()
        {
            return property;
        }
    }
    
    public class InvalidTestNameException extends Exception
    {
        /**
         * 
         */
        private static final long serialVersionUID = -8450373113671237630L;
        private final String property;
        
        public InvalidTestNameException(String property)
        {
            super("Test Property is not a Soar File: " + property);
            
            this.property = property;
        }
        
        public String getProperty()
        {
            return property;
        }
    }
    
    public class MalformedTestCategory extends Exception
    {
        /**
         * 
         */
        private static final long serialVersionUID = -1914521968698486601L;
        private final String property;
        
        public MalformedTestCategory(String property)
        {
            super("Malformed Test Category: " + property);
            
            this.property = property;
        }
        
        public String getProperty()
        {
            return property;
        }
    }
    
    private final String file;
    
    public static final int PARSE_FAILURE = 201;
    public static final int PARSE_SUCCESS = 200;
    
    private final Properties propertiesFile;
    
    private List<ConfigurationCategory> configurationCategories;
    private List<ConfigurationTest> configurationTests;
    
    private int seed = 123456789;
    private int runCount = 0;
    private int warmUpCount = 0;
    
    private List<String> testsToRun;
    private List<String> categoriesToRun;
    
    private boolean jsoarEnabled = true;
    private boolean csoarEnabled = false;
    
    private String csoarDirectory = "";
    private String csoarLabel = "";
    
    private HashMap<String, Integer> testDecisionCycles;
    
    public Configuration(String file)
    {
        this.file = file;
        
        this.propertiesFile = new Properties();
        
        this.configurationCategories = new ArrayList<ConfigurationCategory>();
        this.configurationTests = new ArrayList<ConfigurationTest>();
        
        this.testsToRun = new ArrayList<String>();
        this.categoriesToRun = new ArrayList<String>();
        
        this.csoarDirectory = new String();
        this.csoarLabel = new String();
        
        this.testDecisionCycles = new HashMap<String, Integer>();
    }
    
    public void checkPropertiesFile(PrintWriter out) throws FileNotFoundException
    {
        BufferedReader reader = new BufferedReader(new FileReader(file));
        
        List<String> lines = new ArrayList<String>();
        
        try
        {
            try
            {
                String line = null;
                while ((line = reader.readLine()) != null)
                {
                    lines.add(line);
                }
            }
            finally
            {
                reader.close();
            }
        }
        catch (IOException e)
        {
            out.println("\nFailed to check properties file! " + e.getMessage());
        }
        
        HashMap<String, String> keysAndValues = new HashMap<String, String>();
        
        for (int i = 0;i < lines.size();i++)
        {
            String line = lines.get(i);
            
            int index = line.indexOf("=");
            
            if (index == -1)
            {
                if (line.startsWith("#") || line.replaceAll("\\s", "").length() == 0)
                    continue; //Ignore comments and lines of only whitespace
                
                out.println("\nWARNING: Invalid key-value pair '" + line + "' on line " + i+1 + "\n");
                continue;
            }
            
            String key = line.substring(0, index);
            String value = line.substring(index+1);
            
            if (keysAndValues.containsKey(key))
            {
                out.println("\nWARNING: Duplicate key of '" + key + "' at " + i+1 + "\n");
                continue;
            }
            
            keysAndValues.put(key, value);
        }
    }
    
    public int parse() throws IOException, UnknownPropertyException, InvalidTestNameException, MalformedTestCategory
    {   
        FileInputStream fileStream = new FileInputStream(file);
        propertiesFile.load(fileStream);
        fileStream.close();
        
        configurationCategories.add(new ConfigurationCategory("Uncategorized Tests", new ArrayList<String>()));
        
        for (String key : propertiesFile.stringPropertyNames())
        {
            String value = propertiesFile.getProperty(key);
                        
            if (key.startsWith("Category_"))
            {
                //Is a category
                List<String> potentialCategories = Arrays.asList(value.split("\\s+"));
                List<String> actualCategories = new ArrayList<String>();
                
                String temporaryBuffer = "";
                
                for (String potential : potentialCategories)
                {
                    if (potential.startsWith("\"") && potential.endsWith("\""))
                    {
                        potential = potential.substring(1, potential.length()-1);
                    }
                    
                    if (potential.startsWith("\""))
                    {
                        if (temporaryBuffer.length() != 0)
                        {
                            throw new MalformedTestCategory(key);
                        }
                        
                        //Use the temporary buffer to calculate the name with spaces
                        temporaryBuffer += potential + " ";
                        continue;
                    }
                    else if (potential.endsWith("\""))
                    {
                        if (temporaryBuffer.length() == 0)
                        {
                            throw new MalformedTestCategory(key);
                        }
                        
                        temporaryBuffer += potential;
                        
                        temporaryBuffer = temporaryBuffer.substring(1, temporaryBuffer.length()-1);
                        
                        actualCategories.add(temporaryBuffer);
                        
                        temporaryBuffer = "";
                        continue;
                    }
                    else if (temporaryBuffer.length() != 0)
                    {
                        temporaryBuffer += potential + " ";
                        continue;
                    }
                    
                    actualCategories.add(potential);
                }
                
                if (temporaryBuffer.length() != 0)
                {
                    throw new MalformedTestCategory(key);
                }
                
                ConfigurationCategory category = new ConfigurationCategory(key.substring(9), actualCategories);
                configurationCategories.add(category);
            }
            else if (key.startsWith("Test_"))
            {
                //Is a test
                String test = key.substring(5);
                
                if (!value.endsWith(".soar"))
                {
                    throw new InvalidTestNameException(test);
                }
                
                String category = "Uncategorized Tests";
                
                for (ConfigurationCategory cc : configurationCategories)
                {
                    if (cc.containsTest(test))
                    {
                        category = cc.getCategoryName();
                        break;
                    }
                }
                
                configurationTests.add(new ConfigurationTest(test, value, category));
            }
            else if (key.startsWith("TestDecisionCycles_"))
            {
                //Is a test
                String test = key.substring(19);
                
                Integer decisionCycles = Integer.parseInt(value);
                
                testDecisionCycles.put(test, decisionCycles);
            }
            else if (key.equals("RunCount"))
            {
                runCount = Integer.parseInt(value);
            }
            else if (key.equals("WarmUpCount"))
            {
                warmUpCount = Integer.parseInt(value);
            }
            else if (key.equals("TestsToRun"))
            {   
                List<String> potentialTests = Arrays.asList(value.split("\\s+"));
                
                String temporaryBuffer = "";
                
                for (String potential : potentialTests)
                {
                    if (potential.startsWith("\"") && potential.endsWith("\""))
                    {
                        potential = potential.substring(1, potential.length()-1);
                    }
                    
                    if (potential.startsWith("\""))
                    {
                        if (temporaryBuffer.length() != 0)
                        {
                            throw new MalformedTestCategory(key);
                        }
                        
                        //Use the temporary buffer to calculate the name with spaces
                        temporaryBuffer += potential + " ";
                        continue;
                    }
                    else if (potential.endsWith("\""))
                    {
                        if (temporaryBuffer.length() == 0)
                        {
                            throw new MalformedTestCategory(key);
                        }
                        
                        temporaryBuffer += potential;
                        
                        temporaryBuffer = temporaryBuffer.substring(1, temporaryBuffer.length()-1);
                        
                        testsToRun.add(temporaryBuffer);
                        
                        temporaryBuffer = "";
                        continue;
                    }
                    else if (temporaryBuffer.length() != 0)
                    {
                        temporaryBuffer += potential + " ";
                        continue;
                    }
                    
                    testsToRun.add(potential);
                }
                
                if (temporaryBuffer.length() != 0)
                {
                    throw new MalformedTestCategory(key);
                }
            }
            else if (key.equals("CategoriesToRun"))
            {
                List<String> potentialCategories = Arrays.asList(value.split("\\s+"));
                
                String temporaryBuffer = "";
                
                for (String potential : potentialCategories)
                {
                    if (potential.startsWith("\"") && potential.endsWith("\""))
                    {
                        potential = potential.substring(1, potential.length()-1);
                    }
                    
                    if (potential.startsWith("\""))
                    {
                        if (temporaryBuffer.length() != 0)
                        {
                            throw new MalformedTestCategory(key);
                        }
                        
                        //Use the temporary buffer to calculate the name with spaces
                        temporaryBuffer += potential + " ";
                        continue;
                    }
                    else if (potential.endsWith("\""))
                    {
                        if (temporaryBuffer.length() == 0)
                        {
                            throw new MalformedTestCategory(key);
                        }
                        
                        temporaryBuffer += potential;
                        
                        temporaryBuffer = temporaryBuffer.substring(1, temporaryBuffer.length()-1);
                        
                        categoriesToRun.add(temporaryBuffer);
                        
                        temporaryBuffer = "";
                        continue;
                    }
                    else if (temporaryBuffer.length() != 0)
                    {
                        temporaryBuffer += potential + " ";
                        continue;
                    }
                    
                    categoriesToRun.add(potential);
                }
                
                if (temporaryBuffer.length() != 0)
                {
                    throw new MalformedTestCategory(key);
                }
            }
            else if (key.equals("JSoarEnabled"))
            {
                jsoarEnabled = Boolean.parseBoolean(value);
            }
            else if (key.equals("CSoarEnabled"))
            {
                csoarEnabled = Boolean.parseBoolean(value);
            }
            else if (key.startsWith("CSoarDirectory_"))
            {
                String path = value;
                String label = key.substring(15, key.length());
                
                csoarDirectory = path;
                csoarLabel = label;
            }
            else if (key.equals("Seed"))
            {
                seed = Integer.parseInt(value);
            }
            else
            {
                //Unknown
                throw new UnknownPropertyException(key);
            }
        }
        
        return PARSE_SUCCESS;
    }
    
    public List<ConfigurationTest> getConfigurationTests()
    {
        //Do one final check to make sure everything is right
        for (ConfigurationCategory category : configurationCategories)
        {
            for (ConfigurationTest test : configurationTests)
            {
                if (category.containsTest(test.getTestName()))
                    test.setTestCategory(category.getCategoryName());
            }
        }
        
        return configurationTests;
    }
    
    public List<ConfigurationCategory> getConfigurationCategories()
    {
        // Do one final check to make sure everything is right
        for (ConfigurationCategory category : configurationCategories)
        {
            for (ConfigurationTest test : configurationTests)
            {
                if (category.containsTest(test.getTestName()))
                    test.setTestCategory(category.getCategoryName());
            }
        }

        return configurationCategories;
    }
    
    public int getSeed()
    {
        return seed;
    }
    
    public int getRunCount()
    {
        return runCount;
    }
    
    public int getWarmUpCount()
    {
        return warmUpCount;
    }
    
    public List<String> getTestsToRun()
    {
        return testsToRun;
    }
    
    public List<String> getCategoriesToRun()
    {
        return categoriesToRun;
    }
    
    public boolean getJSoarEnabled()
    {
        return jsoarEnabled;
    }
    
    public boolean getCSoarEnabled()
    {
        return csoarEnabled;
    }
    
    public String getCSoarDirectory()
    {
        return csoarDirectory;
    }
    
    public String getCSoarLabel()
    {
        return csoarLabel;
    }
    
    public Integer getDecisionCyclesToRunTest(String testName)
    {
        if (testDecisionCycles.containsKey(testName) == false)
            return 0;
        else
            return testDecisionCycles.get(testName);
    }
}
