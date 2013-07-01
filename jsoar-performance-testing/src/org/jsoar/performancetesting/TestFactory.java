/**
 * 
 */
package org.jsoar.performancetesting;

/**
 * @author Alex
 *
 */
public interface TestFactory
{
    public Test createTest(String testName, String testFile);
}
