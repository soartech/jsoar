/**
 * 
 */
package org.jsoar.performancetesting;

import java.nio.file.Path;

import org.jsoar.performancetesting.yaml.TestSettings;

/**
 * An interface for constructing tests.
 * 
 * @author ALT
 *
 */
public interface TestFactory
{
    /**
     * The main worker function of the factory. It creates new and initialized
     * tests from the passed parameters.
     * 
     * @param testName
     * @param testFile
     * @return A new instance of a class which has already been created and
     * initialized.
     */
    public Test createTest(String testName, Path testFile,
            TestSettings settings) throws Exception;
}
