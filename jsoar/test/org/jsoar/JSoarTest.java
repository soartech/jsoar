/*
 * (c) 2008  Dave Ray
 *
 * Created on Aug 22, 2008
 */
package org.jsoar;

import org.apache.log4j.BasicConfigurator;
import org.junit.AfterClass;
import org.junit.BeforeClass;

/**
 * @author ray
 */
public class JSoarTest
{
    @BeforeClass
    public static void configureLogging()
    {
        BasicConfigurator.configure();
    }
    
    @AfterClass
    public static void unconfigureLogging()
    {
        BasicConfigurator.resetConfiguration();
    }
}
