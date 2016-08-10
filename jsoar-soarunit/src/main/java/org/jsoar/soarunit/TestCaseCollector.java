/*
 * Copyright (c) 2010 Dave Ray <daveray@gmail.com>
 *
 * Created on Jul 30, 2010
 */
package org.jsoar.soarunit;

import java.io.File;
import java.io.IOException;
import java.io.PrintWriter;
import java.net.MalformedURLException;
import java.net.URI;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;

import org.jsoar.kernel.SoarException;
import org.jsoar.util.UrlTools;

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
        try
        {
            URL url = file.toURI().toURL();
            entries.add(new Entry(url, recursive));
        } catch (MalformedURLException e) {
        }
    }

    public void addEntry(URL url, boolean recursive)
    {
        entries.add(new Entry(url, recursive));
    }
    
    public List<TestCase> collect() throws SoarException, IOException
    {
        final List<TestCase> all = new ArrayList<TestCase>();
        for(Entry entry : entries)
        {
            final URL input = entry.url;
            if (UrlTools.isFileURL(input))
            {
                File file = UrlTools.toFile2(input);
                if(file.isFile())
                {
                    all.add(TestCase.fromFile(file));
                }
                else if(file.isDirectory() && entry.recursive)
                {
                    all.addAll(collectTestCasesInDirectory(file));
                }
            }
            else
            {
                all.add(TestCase.fromURL(input));
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
        final URL url;
        final boolean recursive;
        
        public Entry(URL url, boolean recursive)
        {
            this.url = url;
            this.recursive = recursive;
        }
    }
}
