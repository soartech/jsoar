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
    private Path csoarDirectory;

    public CSoarTestFactory()
    {
        this.csoarDirectory = Paths.get("");
    }

    public CSoarTestFactory(Path csoarDirectory)
    {
        this.csoarDirectory = csoarDirectory;
    }

    public Path getSoarPath()
    {
        return csoarDirectory;
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
        CSoarTest csoarTest = new CSoarTest(csoarDirectory);

        csoarTest.initialize(testName, testFile, settings);

        return csoarTest;
    }

}
