/**
 * 
 */
package org.jsoar.performancetesting.jsoar;

import org.jsoar.performancetesting.Test;
import org.jsoar.performancetesting.TestFactory;

/**
 * @author ALT
 *
 */
public class JSoarTestFactory implements TestFactory
{
    @Override
    public Test createTest(String testName, String testFile, Integer decisionCycles)
    {
        JSoarTest jsoarTest = new JSoarTest();
        
        jsoarTest.initialize(testName, testFile, decisionCycles);
        
        return jsoarTest;
    }
}
