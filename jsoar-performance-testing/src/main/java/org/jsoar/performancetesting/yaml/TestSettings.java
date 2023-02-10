/**
 * 
 */
package org.jsoar.performancetesting.yaml;

import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import java.util.stream.Collectors;

/**
 * A class for holding test settings. This is used all over the place for
 * getting individual test settings. It is usually constructed with a default
 * test setting object (another TestSettings object) and then any additional
 * assignments overwrite those values. This means there is less overhead for
 * trying to determine the settings for a test.
 * 
 * @author ALT
 *
 */
public class TestSettings
{
    private boolean jsoarEnabled;
    
    private boolean csoarEnabled;
    
    private int runCount;
    
    private int warmUpCount;
    
    private List<Integer> decisionCycles;
    
    private boolean useSeed;
    
    private long seed;
    
    private Path csvDirectory = Paths.get("");
    
    private Path summaryFile = Paths.get("");
    
    private List<Path> csoarDirectories;
    
    private List<Path> jsoarCoreJars;
    
    private String jvmSettings;
    
    // used by yaml reader
    public TestSettings()
    {
        this(Configuration.defaultSettings);
    }
    
    public TestSettings(TestSettings other)
    {
        if(other == null)
            return;
        
        jsoarEnabled = other.isJsoarEnabled();
        csoarEnabled = other.isCsoarEnabled();
        
        runCount = other.getRunCount();
        warmUpCount = other.getWarmUpCount();
        
        decisionCycles = other.getDecisionCycles();
        
        useSeed = other.isUsingSeed();
        seed = other.getSeed();
        
        csvDirectory = other.getCsvDirectory();
        summaryFile = other.getSummaryFile();
        
        csoarDirectories = other.getCsoarDirectories();
        jsoarCoreJars = other.getJsoarCoreJars();
        
        jvmSettings = other.getJvmSettings();
    }
    
    public TestSettings(boolean jsoarEnabled, boolean csoarEnabled,
            int runCount, int warmUpCount, List<Integer> decisionCycles,
            boolean useSeed, long seed, Path csvDirectory,
            Path summaryFile, List<Path> csoarDirectories,
            List<Path> jsoarDirectories, String jvmSettings)
    {
        this.jsoarEnabled = jsoarEnabled;
        this.csoarEnabled = csoarEnabled;
        
        this.runCount = runCount;
        this.warmUpCount = warmUpCount;
        
        this.decisionCycles = decisionCycles;
        
        this.useSeed = useSeed;
        this.seed = seed;
        
        this.csvDirectory = csvDirectory;
        this.summaryFile = summaryFile;
        
        this.csoarDirectories = csoarDirectories;
        this.jsoarCoreJars = jsoarDirectories;
        
        this.jvmSettings = jvmSettings;
        
        // Sanity check
        if(this.csoarEnabled && csoarDirectories.size() == 0)
        {
            throw new RuntimeException(
                    "Sanity Check Failed!  CSoar is enabled but there are no directories specified for it!");
        }
        
        if(this.jsoarEnabled && jsoarDirectories.size() == 0)
        {
            throw new RuntimeException(
                    "Sanity Check Failed!  JSoar is enabled but there are no directories specified for it!");
        }
    }
    
    public void setJsoarEnabled(boolean jsoarEnabled)
    {
        this.jsoarEnabled = jsoarEnabled;
    }
    
    public boolean isJsoarEnabled()
    {
        return jsoarEnabled;
    }
    
    public void setCsoarEnabled(boolean csoarEnabled)
    {
        this.csoarEnabled = csoarEnabled;
    }
    
    public boolean isCsoarEnabled()
    {
        return csoarEnabled;
    }
    
    public void setRunCount(int runCount)
    {
        this.runCount = runCount;
    }
    
    public int getRunCount()
    {
        return runCount;
    }
    
    public void setWarmUpCount(int warmUpCount)
    {
        this.warmUpCount = warmUpCount;
    }
    
    public int getWarmUpCount()
    {
        return warmUpCount;
    }
    
    public void setDecisionCycles(List<Integer> decisionCycles)
    {
        this.decisionCycles = decisionCycles;
    }
    
    public List<Integer> getDecisionCycles()
    {
        return decisionCycles;
    }
    
    public void setUseSeed(boolean useSeed)
    {
        this.useSeed = useSeed;
    }
    
    public boolean isUsingSeed()
    {
        return this.useSeed;
    }
    
    public void setSeed(long seed)
    {
        this.seed = seed;
    }
    
    public long getSeed()
    {
        return seed;
    }
    
    public void setCsvDirectory(Path csvDirectory)
    {
        this.csvDirectory = csvDirectory;
    }
    
    public Path getCsvDirectory()
    {
        return csvDirectory;
    }
    
    public void setSummaryFile(Path summaryFile)
    {
        this.summaryFile = summaryFile;
    }
    
    public Path getSummaryFile()
    {
        return summaryFile;
    }
    
    public void setCsoarDirectories(List<Path> csoarDirectories)
    {
        this.csoarDirectories = TestSettings.processUserHomeInPaths(csoarDirectories);
    }
    
    public List<Path> getCsoarDirectories()
    {
        return csoarDirectories;
    }
    
    public void setJsoarCoreJars(List<Path> jsoarCoreJars)
    {
        this.jsoarCoreJars = TestSettings.processUserHomeInPaths(jsoarCoreJars);
    }
    
    public List<Path> getJsoarCoreJars()
    {
        return jsoarCoreJars;
    }
    
    public void setJvmSettings(String jvmSettings)
    {
        this.jvmSettings = jvmSettings;
    }
    
    public String getJvmSettings()
    {
        return jvmSettings;
    }
    
    private static List<Path> processUserHomeInPaths(List<Path> paths)
    {
        // replace instances of %USER_HOME% with the user's home dir when setting
        return paths.stream()
                .map(Path::toString)
                .map(s -> s.replace("%USER_HOME%", System.getProperty("user.home")))
                .map(s -> Paths.get(s))
                .collect(Collectors.toList());
    }
}
