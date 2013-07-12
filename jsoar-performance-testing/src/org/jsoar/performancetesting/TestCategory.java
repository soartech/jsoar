/**
 * 
 */
package org.jsoar.performancetesting;

import java.util.List;

/**
 * A Category for tests.  Holds a bunch of tests specified in the properties file.
 * @author ALT
 *
 */
public class TestCategory
{
	//Class variables
    private final String categoryName;
    private final List<Test> categoryTests;
    
    //Static methods
    
    /**
     * 
     * @param categoryName
     * @param testCategories
     * @return a test category out of a list of test categories with a given name or null if there wasn't one.
     */
    public static TestCategory getTestCategory(String categoryName, List<TestCategory> testCategories)
    {
        for (TestCategory category : testCategories)
        {
            if (category.getCategoryName() == categoryName)
            {
                return category;
            }
        }
        
        return null;
    }
    
    //Non-static methods
    
    /**
     * Initials a test category with a name and a list of category tests.
     * 
     * @param categoryName
     * @param categoryTests
     */
    public TestCategory(String categoryName, List<Test> categoryTests)
    {
        this.categoryName = categoryName;
        this.categoryTests = categoryTests;
    }
    
    /**
     * 
     * @return the category's name.
     */
    public String getCategoryName()
    {
        return categoryName;
    }
    
    /**
     * 
     * @return the tests in this category.
     */
    public List<Test> getCategoryTests()
    {
        return categoryTests;
    }
    
    /**
     * 
     * @param test
     * @return whether this category contains a given test.
     */
    public boolean containsTest(Test test)
    {
        return categoryTests.contains(test);
    }
    
    /**
     * Adds a test to this category.  If the test already it exists, it returns false.
     * 
     * @param test
     * @return whether the test was successfully added.
     */
    public boolean addTest(Test test)
    {
        if (containsTest(test))
        {
            return false;
        }
        
        return categoryTests.add(test);
    }
}
