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
import java.util.SortedSet;
import java.util.TreeSet;

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
    class ConfigurationTest implements Comparable<ConfigurationTest>
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
        
        @Override
        public int compareTo(ConfigurationTest o)
        {
            return this.testName.compareTo(o.testName);
        }
    }
    
    // Package Private
    class ConfigurationCategory
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
    
    /**
     * Package private classes for exceptions of the configuration class.
     */
    
    // Package Private
    class UnknownPropertyException extends Exception
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
    
    // Package Private
    class InvalidTestNameException extends Exception
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
    
    // Package Private
    class MalformedTestCategory extends Exception
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
    
    /**
     * Class variables
     */
    
    private final String file;
    
    public static final int PARSE_FAILURE = 201;
    public static final int PARSE_SUCCESS = 200;
    
    private static final int OPTION_PARSE_SUCCESS = 1024;
    private static final int OPTION_PARSE_NON_APPLY = 1025;
    // If you wanted to have options be able to return errors you just
    // have to uncomment this.  This isn't there right now because
    // the tests throw errors instead of return errors but if you
    // wanted to do it the other way, you could.
    //private static final int OPTION_PARSE_FAILURE = 1026;
    
    private final Properties propertiesFile;
    
    private List<ConfigurationCategory> configurationCategories;
    private SortedSet<ConfigurationTest> configurationTests;

    private List<String> testsToRun;
    private List<String> categoriesToRun;
    
    private HashMap<String, Integer> testDecisionCycles;
    
    private Long seed = 123456789L;
    private int runCount = 0;
    private int warmUpCount = 0;
    
    private boolean jsoarEnabled = true;
    private boolean csoarEnabled = false;
    
    private String csoarDirectory = "";
    private String csoarLabel = "";
    private String csvDirectory = "";
    
    /**
     * Initializes the Configuration class
     * 
     * @param file
     */
    public Configuration(String file)
    {
        this.file = file;
        
        this.propertiesFile = new Properties();
        
        this.configurationCategories = new ArrayList<ConfigurationCategory>();
        this.configurationTests = new TreeSet<ConfigurationTest>();
        
        this.testsToRun = new ArrayList<String>();
        this.categoriesToRun = new ArrayList<String>();
        
        this.csoarDirectory = new String();
        this.csoarLabel = new String();
        
        this.testDecisionCycles = new HashMap<String, Integer>();
    }
    
    /**
     * This checks the properties file for duplicate keys and warns the user if any are found
     * 
     * @param out
     * @throws FileNotFoundException
     */
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
                    line = line.trim();
                    
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
    
    /**
     * Checks and parses a Category_ property.  If the property isn't actually
     * a Category_ property it returns a NON_APPLY result.
     * 
     * @param key
     * @param value
     * @return Either OPTION_PARSE_SUCCESS or OPTION_PARSE_NON_APPLY
     * @throws MalformedTestCategory
     */
    private int checkAndParseCategory(String key, String value) throws MalformedTestCategory
    {
    	if (key.startsWith("Category_")) // CODEREVIEW: all of these special strings should be constants
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
                
                potential = potential.trim();
                
                actualCategories.add(potential);
            }
            
            if (temporaryBuffer.length() != 0)
            {
                throw new MalformedTestCategory(key);
            }
            
            ConfigurationCategory category = new ConfigurationCategory(key.substring(9), actualCategories);
            configurationCategories.add(category);
            
            return OPTION_PARSE_SUCCESS;
        }
    	else
    	{
    		return OPTION_PARSE_NON_APPLY;
    	}
    }
    
    /**
     * Checks and parses a Test_ option in the properties file.  If the
     * option is not a Test_ option it returns a NON_APPLY.
     * 
     * @param key
     * @param value
     * @return Either OPTION_PARSE_SUCCESS or OPTION_PARSE_NON_APPLY
     * @throws InvalidTestNameException
     */
    private int checkAndParseTest(String key, String value) throws InvalidTestNameException
    {
    	if (key.startsWith("Test_"))
        {
            //Is a test
            String test = key.substring(5); // CODEREVIEW: magic number -- should be computed from constant
            
            if (!value.endsWith(".soar"))
            {
                throw new InvalidTestNameException(test);
            }
            
            String category = "Uncategorized Tests"; // CODEREVIEW: this should be a constant
            
            for (ConfigurationCategory cc : configurationCategories)
            {
                if (cc.containsTest(test))
                {
                    category = cc.getCategoryName();
                    break;
                }
            }
            
            String unixPath = value.replace("\\","/"); //Convert Windows style paths to unix
            
            unixPath = unixPath.trim();
            
            configurationTests.add(new ConfigurationTest(test, unixPath, category));
            
            return OPTION_PARSE_SUCCESS;
        }
    	else
    	{
    		return OPTION_PARSE_NON_APPLY;
    	}
    }
    
    /**
     * Checks and parses a TestDecisionCycles_ properties option.  If the option
     * isn't a TestDecisionCycles_ option then it returns NON_APPLY.
     * 
     * @param key
     * @param value
     * @return Either OPTION_PARSE_SUCCESS or OPTION_PARSE_NON_APPLY
     */
    private int checkAndParseTestDecisionCycles(String key, String value)
    {
    	if (key.startsWith("TestDecisionCycles_"))
        {
            //Is a test
            String test = key.substring(19);
            
            Integer decisionCycles = Integer.parseInt(value);
            
            testDecisionCycles.put(test, decisionCycles);
            
            return OPTION_PARSE_SUCCESS;
        }
    	else
    	{
    		return OPTION_PARSE_NON_APPLY;
    	}
    }
    
    /**
     * Checks and parses a TestsToRun option in the properties file.  If the option
     * isn't a TestsToRun option then it returns a NON_APPLY result.
     * 
     * @param key
     * @param value
     * @return Either OPTION_PARSE_SUCCESS or OPTION_PARSE_NON_APPLY
     * @throws MalformedTestCategory
     */
    private int checkAndParseTestsToRun(String key, String value) throws MalformedTestCategory
    {
    	if (key.equals("TestsToRun"))
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
                
                potential = potential.trim();
                
                testsToRun.add(potential);
            }
            
            if (temporaryBuffer.length() != 0)
            {
                throw new MalformedTestCategory(key);
            }
            
            return OPTION_PARSE_SUCCESS;
        }
    	else
    	{
    		return OPTION_PARSE_NON_APPLY;
    	}
    }
    
    /**
     * Checks and parses a CategoriesToRun option.  If it isn't,
     * then it returns a NON_APPLY result.
     * 
     * @param key
     * @param value
     * @return Either OPTION_PARSE_SUCCESS or OPTION_PARSE_NON_APPLY
     * @throws MalformedTestCategory
     */
    private int checkAndParseCategoriesToRun(String key, String value) throws MalformedTestCategory
    {
    	if (key.equals("CategoriesToRun"))
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
                
                potential = potential.trim();
                
                categoriesToRun.add(potential);
            }
            
            if (temporaryBuffer.length() != 0)
            {
                throw new MalformedTestCategory(key);
            }
            
            return OPTION_PARSE_SUCCESS;
        }
    	else
    	{
    		return OPTION_PARSE_NON_APPLY;
    	}
    }
    
    /**
     * Checks and parses a CSoarDirectory_ option.  If it isn't, it returns
     * a NON_APPLY.
     * 
     * @param key
     * @param value
     * @return Either OPTION_PARSE_SUCCESS or OPTION_PARSE_NON_APPLY
     */
    private int checkAndParseCSoarDirectory(String key, String value)
    {
    	if (key.startsWith("CSoarDirectory_"))
        {
            String path = value;
            String label = key.substring(15, key.length());
            
            String unixPath = path.replace("\\","/"); //Convert Windows style paths to unix style.
            
            unixPath = unixPath.trim();
            
            csoarDirectory = unixPath;
            csoarLabel = label;
            
            return OPTION_PARSE_SUCCESS;
        }
    	else
    	{
    		return OPTION_PARSE_NON_APPLY;
    	}
    }
    
    /**
     * This parses the entire configuration file and places it into the class variables.
     * 
     * @return Whether the configuration file was parsed successfully or not
     * @throws IOException
     * @throws UnknownPropertyException
     * @throws InvalidTestNameException
     * @throws MalformedTestCategory
     */
    public int parse() throws IOException, UnknownPropertyException, InvalidTestNameException, MalformedTestCategory
    {   
        FileInputStream fileStream = new FileInputStream(file);
        propertiesFile.load(fileStream);
        fileStream.close();
        
        configurationCategories.add(new ConfigurationCategory("Uncategorized Tests", new ArrayList<String>()));
        
        for (String key : propertiesFile.stringPropertyNames())
        {
            String value = propertiesFile.getProperty(key);
            key = key.trim();
            value = value.trim();
            
            if (checkAndParseCategory(key, value) == OPTION_PARSE_SUCCESS)
            	continue;
            
            if (checkAndParseTest(key, value) == OPTION_PARSE_SUCCESS)
            	continue;
            
            if (checkAndParseTestDecisionCycles(key, value) == OPTION_PARSE_SUCCESS)
            	continue;
            
            if (key.equals("RunCount"))
            {
                runCount = Integer.parseInt(value);
                continue;
            }
            
            if (key.equals("WarmUpCount"))
            {
                warmUpCount = Integer.parseInt(value);
                continue;
            }
           
            if (checkAndParseTestsToRun(key, value) == OPTION_PARSE_SUCCESS)
            	continue;
            
            if (checkAndParseCategoriesToRun(key, value) == OPTION_PARSE_SUCCESS)
            	continue;
            
            if (key.equals("JSoarEnabled"))
            {
                jsoarEnabled = Boolean.parseBoolean(value);
                continue;
            }
            
            if (key.equals("CSoarEnabled"))
            {
                csoarEnabled = Boolean.parseBoolean(value);
                continue;
            }
            
            if (checkAndParseCSoarDirectory(key, value) == OPTION_PARSE_SUCCESS)
            	continue;
            
            if (key.equals("Seed"))
            {
                seed = Long.parseLong(value);
                continue;
            }
            
            if (key.equals("CSVDirectory"))
            {
                csvDirectory = value;
                continue;
            }

            {
                //Unknown
                throw new UnknownPropertyException(key);
            }
        }
        
        return PARSE_SUCCESS;
    }
    
    /**
     * 
     * @return All the ConfigurationTest holders
     */
    public SortedSet<ConfigurationTest> getConfigurationTests()
    {
        // Do one final check to make sure everything is right.
    	// "right" being everything is hooked up correctly.  This check is to
    	// ensure that the tests are all connected to the right categories
    	// because previously there was a bug related to this because of the
    	// potential for different ordering of files.
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
    
    /**
     * 
     * @return All the ConfigurationCategory holders
     */
    public List<ConfigurationCategory> getConfigurationCategories()
    {
    	// Do one final check to make sure everything is right.
    	// "right" being everything is hooked up correctly.  This check is to
    	// ensure that the tests are all connected to the right categories
    	// because previously there was a bug related to this because of the
    	// potential for different ordering of files.
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
    
    /**
     * 
     * @return The seed set in the configuration file.  Will return a default value of 123456789L if none was set.
     */
    public Long getSeed()
    {
        return seed;
    }
    
    /**
     * 
     * @return The number of runs to run each test
     */
    public int getRunCount()
    {
        return runCount;
    }
    
    /**
     * 
     * @return The number of warm up runs to run each JSoar test.
     */
    public int getWarmUpCount()
    {
        return warmUpCount;
    }
    
    /**
     * 
     * @return A list of the tests to run.  If the test does not appear in this list, and the list is not empty, that test will not be run.
     */
    public List<String> getTestsToRun()
    {
        return testsToRun;
    }
    
    /**
     * 
     * @return A list of the categories to run.  If the category does not appear in this list, and the list is not empty, then any test not in one of these categories will not be run.
     */
    public List<String> getCategoriesToRun()
    {
        return categoriesToRun;
    }
    
    /**
     * 
     * @return Whether to run JSoar Tests.
     */
    public boolean getJSoarEnabled()
    {
        return jsoarEnabled;
    }
    
    /**
     * 
     * @return Whether to run CSoar Tests.
     */
    public boolean getCSoarEnabled()
    {
        return csoarEnabled;
    }
    
    /**
     * 
     * @return The root CSoar directory.  That is, the directory containing Soar.dll and the java folder.
     */
    public String getCSoarDirectory()
    {
        return csoarDirectory;
    }
    
    /**
     * 
     * @return The CSoar label, which is usually the version of CSoar running.
     */
    public String getCSoarLabel()
    {
        return csoarLabel;
    }
    
    /**
     * 
     * @param testName
     * @return The number of decision cycles to run run a given test.
     */
    public Integer getDecisionCyclesToRunTest(String testName)
    {
        if (testDecisionCycles.containsKey(testName) == false)
            return 0;
        else
            return testDecisionCycles.get(testName);
    }
    
    /**
     * 
     * @return The CSV Directory to use.
     */
    public String getCSVDirectory()
    {
        return csvDirectory;
    }
}
