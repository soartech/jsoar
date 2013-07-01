/**
 * 
 */
package org.jsoar.performancetesting;

import java.util.List;

/**
 * @author ALT
 *
 */
public class TestCategory
{
    private final String categoryName;
    private final List<Test> categoryTests;
    
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
    
    public TestCategory(String categoryName, List<Test> categoryTests)
    {
        this.categoryName = categoryName;
        this.categoryTests = categoryTests;
    }
    
    public String getCategoryName()
    {
        return categoryName;
    }
    
    public List<Test> getCategoryTests()
    {
        return categoryTests;
    }
    
    public boolean containsTest(Test test)
    {
        return categoryTests.contains(test);
    }
    
    public boolean addTest(Test test)
    {
        if (containsTest(test))
        {
            return false;
        }
        
        categoryTests.add(test);
        
        return true;
    }
}
