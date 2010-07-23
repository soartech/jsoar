/*
 * Copyright (c) 2010 Dave Ray <daveray@gmail.com>
 *
 * Created on Jul 22, 2010
 */
package org.jsoar.soarunit;

import java.io.File;
import java.io.FilenameFilter;
import java.util.ArrayList;
import java.util.List;

import org.jsoar.util.commands.OptionProcessor;

import com.google.common.collect.Lists;

/**
 * @author ray
 */
public class SoarUnit
{
    private static enum Options {};
    
    /**
     * @param args
     */
    public static void main(String[] args) throws Exception
    {
        final OptionProcessor<Options> options = new OptionProcessor<Options>();
        final List<String> rest = options.process(Lists.asList("SoarUnit", args));
        final List<File> inputs = new ArrayList<File>();
        if(rest.isEmpty())
        {
            inputs.add(new File("."));
        }
        else
        {
            for(String arg : rest)
            {
                final File file = new File(arg);
                if(!file.exists())
                {
                    System.err.println("'" + arg + "' does not exist.");
                    System.exit(1);
                }
                inputs.add(file);
            }
        }
        
        for(File input : inputs)
        {
            if(input.isFile())
            {
                if(!runTestSuite(input))
                {
                    System.exit(1);
                }
            }
            else
            {
                if(!runTestSuitesInDirectory(input))
                {
                    System.exit(1);
                }
            }
        }
        
    }

    private static boolean runTestSuitesInDirectory(File dir) throws Exception
    {
        System.out.println("Sourcing tests in directory '" + dir + "'");
        
        final String[] suiteFiles = dir.list(new FilenameFilter()
        {
            @Override
            public boolean accept(File dir, String name)
            {
                return name.startsWith("test") && name.endsWith(".soar");
            }
        });
        
        for(String suiteFile : suiteFiles)
        {
            if(!runTestSuite(new File(dir, suiteFile)))
            {
                return false;
            }
        }
        
        return true;
    }

    private static boolean runTestSuite(File suiteFile) throws Exception
    {
        System.out.println("Sourcing tests in file '" + suiteFile + "'");
        final TestSuite suite = TestSuite.fromFile(suiteFile);
        
        return suite.run();
    }

}
