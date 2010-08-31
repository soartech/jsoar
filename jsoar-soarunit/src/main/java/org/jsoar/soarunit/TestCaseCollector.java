/*
 * Copyright (c) 2010 Dave Ray <daveray@gmail.com>
 *
 * Created on Jul 30, 2010
 */
package org.jsoar.soarunit;

import java.io.File;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.List;

import org.jsoar.kernel.SoarException;

/**
 * @author ray
 */
public class TestCaseCollector
{
    private final PrintWriter out;
    private final List<Entry> entries = new ArrayList<Entry>();
    
    public TestCaseCollector(PrintWriter out)
    {
        this.out = out;
    }

    public void addEntry(File file, boolean recursive)
    {
        entries.add(new Entry(file, recursive));
    }
    
    public List<TestCase> collect() throws SoarException, IOException
    {
        final List<TestCase> all = new ArrayList<TestCase>();
        for(Entry entry : entries)
        {
            final File input = entry.file;
            if(input.isFile())
            {
                all.add(TestCase.fromFile(input));
            }
            else if(input.isDirectory() && entry.recursive)
            {
                all.addAll(collectTestCasesInDirectory(input));
            }
        }
        out.printf("Found %d test case%s%n", all.size(), all.size() != 1 ? "s" : "");
        return all;
        
    }
    
    private List<TestCase> collectTestCasesInDirectory(File dir) throws SoarException, IOException
    {
        out.println("Collecting tests in directory '" + dir + "'");
        
        final List<TestCase> result = new ArrayList<TestCase>();
        final File[] children = dir.listFiles();
        if(children != null)
        {
            for(File file : children)
            {
                if(file.isDirectory() && !file.getName().startsWith("."))
                {
                    result.addAll(collectTestCasesInDirectory(file));
                }
                else if(isTestFile(file))
                {
                    out.println("Collecting tests in file '" + file + "'");
                    result.add(TestCase.fromFile(file));
                }
            }
        }
        
        return result;
    }
    
    private boolean isTestFile(File file)
    {
        return file.isFile() &&
              (file.getName().endsWith(".soarunit") ||
               (file.getName().startsWith("test") && file.getName().endsWith(".soar")));
    }
    
    private static class Entry
    {
        final File file;
        final boolean recursive;
        
        public Entry(File file, boolean recursive)
        {
            this.file = file;
            this.recursive = recursive;
        }
    }
}
