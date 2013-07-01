/**
 * 
 */
package org.jsoar.performancetesting.csoar;

import org.jsoar.performancetesting.Test;
import org.jsoar.performancetesting.TestFactory;

/**
 * @author Alex
 *
 */
public class CSoarTestFactory implements TestFactory
{
    private String label;
    private String csoarDirectory;
    
    public CSoarTestFactory(String label, String csoarDirectory)
    {
        this.label = label;
        this.csoarDirectory = csoarDirectory;
    }
    
    @Override
    public Test createTest(String testName, String testFile)
    {
        CSoarTest csoarTest = new CSoarTest(label, csoarDirectory);
        
        csoarTest.initialize(testName, testFile);
        
        return csoarTest;
    }

}
