/*
 * Copyright (c) 2010 Dave Ray <daveray@gmail.com>
 *
 * Created on Jul 22, 2010
 */
package org.jsoar.soarunit;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

import org.jsoar.kernel.SoarException;
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
        
        final List<TestSuite> all = collectAllTestSuites(inputs);
        final List<TestSuiteResult> results = runAllTestSuites(all);
        
        printAllTestSuiteResults(results);
    }

    private static void printAllTestSuiteResults(final List<TestSuiteResult> results)
    {
        int totalPassed = 0;
        int totalFailed = 0;
        int totalTests = 0;
        for(TestSuiteResult result : results)
        {
            final TestSuite suite = result.getSuite();
            totalTests += suite.getTests().size();
            System.out.println("-------------------------------------------------------------");
            System.out.printf("Test Suite: %s (%s)%n", suite.getName(), suite.getFile());
            System.out.printf("%d passed, %d failed%n", result.getPassed(), result.getFailed());
            for(TestResult testResult : result.getTestResults())
            {
                final Test test = testResult.getTest();
                if(testResult.isPassed())
                {
                    System.out.printf("PASSED: %s, %s%n", test.getName(), testResult.getMessage());
                    totalPassed++;
                }
                else
                {
                    System.out.printf("FAILED: %s, %s%n", test.getName(), testResult.getMessage());
                    System.out.println(testResult.getOutput());
                    totalFailed++;
                }
            }
        }
        System.out.println("-------------------------------------------------------------");
        System.out.printf("%d/%d tests run. %d passed, %d failed%n", totalPassed + totalFailed, totalTests, totalPassed, totalFailed);
    }

    private static List<TestSuiteResult> runAllTestSuites(
            final List<TestSuite> all) throws SoarException
    {
        int index = 0;
        final List<TestSuiteResult> results = new ArrayList<TestSuiteResult>();
        for(TestSuite suite : all)
        {
            final TestSuiteResult result = suite.run(index++, all.size());
            results.add(result);
            if(result.getFailed() > 0)
            {
                break;
            }
        }
        return results;
    }

    private static List<TestSuite> collectAllTestSuites(final List<File> inputs)
            throws SoarException, Exception
    {
        final List<TestSuite> all = new ArrayList<TestSuite>();
        for(File input : inputs)
        {
            if(input.isFile())
            {
                all.add(TestSuite.fromFile(input));
            }
            else if(input.isDirectory())
            {
                all.addAll(collectTestSuitesInDirectory(input));
            }
        }
        System.out.printf("Found %d test suite%s%n", all.size(), all.size() != 1 ? "s" : "");
        return all;
    }
    
    private static List<TestSuite> collectTestSuitesInDirectory(File dir) throws Exception
    {
        System.out.println("Collecting tests in directory '" + dir + "'");
        
        final List<TestSuite> result = new ArrayList<TestSuite>();
        final File[] children = dir.listFiles();
        if(children != null)
        {
            for(File file : children)
            {
                if(file.isDirectory() && !file.getName().startsWith("."))
                {
                    result.addAll(collectTestSuitesInDirectory(file));
                }
                else if(file.isFile() && file.getName().startsWith("test") && file.getName().endsWith(".soar"))
                {
                    System.out.println("Collecting tests in file '" + file + "'");
                    result.add(TestSuite.fromFile(file));
                }
            }
        }
        
        return result;
    }
}
