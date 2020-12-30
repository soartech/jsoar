/**
 * 
 */
package org.jsoar.performancetesting.jsoar;

import java.net.MalformedURLException;
import java.nio.file.Path;

import org.jsoar.performancetesting.Test;
import org.jsoar.performancetesting.TestFactory;
import org.jsoar.performancetesting.yaml.TestSettings;

/**
 * A class to create instantiations of JSoar tests.
 * 
 * @author ALT
 *
 */
public class JSoarTestFactory implements TestFactory
{
    private String label;

    private Path jsoarCoreJar;

    public JSoarTestFactory()
    {
        this.label = new String();
    }

    public void setLabel(String label)
    {
        this.label = label;
    }

    public String getLabel()
    {
        return label;
    }

    public void setJsoarCoreJar(Path jsoarCoreJar)
    {
        this.jsoarCoreJar = jsoarCoreJar;
    }

    /**
     * This creates JSoar Tests. It takes a test's name, file, and the number of
     * decision cycles to run and returns a new test which has been created and
     * initialized with all those values.
     * 
     * @param testName
     * @param testFile
     * @param settings
     * @return A new and initialized JSoar test.
     * @throws ClassNotFoundException 
     * @throws MalformedURLException 
     */
    @Override
    public Test createTest(String testName, Path testFile,
            TestSettings settings) throws MalformedURLException, ClassNotFoundException
    {
        JSoarTest jsoarTest = new JSoarTest(label, jsoarCoreJar);

        jsoarTest.initialize(testName, testFile, settings);

        return jsoarTest;
    }
}
