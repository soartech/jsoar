/**
 * 
 */
package org.jsoar.performancetesting.csoar;

import java.nio.file.Path;
import java.nio.file.Paths;

import org.jsoar.performancetesting.Test;
import org.jsoar.performancetesting.TestFactory;
import org.jsoar.performancetesting.yaml.TestSettings;

/**
 * This creates instantiations of CSoar tests.
 * 
 * @author ALT
 *
 */
public class CSoarTestFactory implements TestFactory
{
    private String label;

    private Path csoarDirectory;

    public CSoarTestFactory()
    {
        this.label = new String();
        this.csoarDirectory = Paths.get("");
    }

    public CSoarTestFactory(String label, Path csoarDirectory)
    {
        this.label = label;
        this.csoarDirectory = csoarDirectory;
    }

    public void setLabel(String label)
    {
        this.label = label;
    }

    public String getLabel()
    {
        return label;
    }

    public void setCSoarDirectory(Path csoarDirectory)
    {
        this.csoarDirectory = csoarDirectory;
    }

    /**
     * This creates a new and initialized CSoar test. It also, as by product of
     * creating the CSoar test, will load the CSoar sml classes for the first
     * time if necessary.
     * 
     * @param testName
     * @param testFile
     * @param decisionCycles
     * @return A new and initialized CSoar Test (but may be only assertion
     *         errors if it didn't load properly.)
     */
    @Override
    public Test createTest(String testName, Path testFile,
            TestSettings settings) throws Exception
    {
        CSoarTest csoarTest = new CSoarTest(label, csoarDirectory);

        csoarTest.initialize(testName, testFile, settings);

        return csoarTest;
    }

}
