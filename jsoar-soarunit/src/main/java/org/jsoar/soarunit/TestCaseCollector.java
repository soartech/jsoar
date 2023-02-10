/*
 * Copyright (c) 2010 Dave Ray <daveray@gmail.com>
 *
 * Created on Jul 30, 2010
 */
package org.jsoar.soarunit;

import java.io.File;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;

import org.jsoar.kernel.SoarException;
import org.jsoar.soarunit.SoarUnit.PrintWriterProxy;
import org.jsoar.util.UrlTools;

/**
 * @author ray
 */
public class TestCaseCollector
{
    private final PrintWriterProxy out;
    private final List<Entry> entries = new ArrayList<Entry>();
    
    public TestCaseCollector(PrintWriterProxy out)
    {
        this.out = out;
    }
    
    public void addEntry(File file, boolean recursive)
    {
        try
        {
            URL url = file.toURI().toURL();
            entries.add(new Entry(url, recursive));
        }
        catch(MalformedURLException e)
        {
        }
    }
    
    public void addEntry(URL url, boolean recursive)
    {
        entries.add(new Entry(url, recursive));
    }
    
    public List<TestCase> collect() throws SoarException, IOException
    {
        final int prefixIndex = greatestCommonPrefixIndex();
        
        final List<TestCase> all = new ArrayList<TestCase>();
        for(Entry entry : entries)
        {
            final URL input = entry.url;
            if(UrlTools.isFileURL(input))
            {
                File file = UrlTools.toFile2(input);
                if(file.isFile())
                {
                    all.add(TestCase.fromFile(file, prefixIndex));
                }
                else if(file.isDirectory() && entry.recursive)
                {
                    all.addAll(collectTestCasesInDirectory(file, prefixIndex));
                }
            }
            else
            {
                all.add(TestCase.fromURL(input, prefixIndex));
            }
        }
        out.printf("Found %d test case%s%n", all.size(), all.size() != 1 ? "s" : "");
        return all;
        
    }
    
    private List<TestCase> collectTestCasesInDirectory(File dir, int prefixIndex) throws SoarException, IOException
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
                    result.addAll(collectTestCasesInDirectory(file, prefixIndex));
                }
                else if(isTestFile(file))
                {
                    out.println("Collecting tests in file '" + file + "'");
                    result.add(TestCase.fromFile(file, prefixIndex));
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
    
    /**
     * Find the index of the end of the greatest common prefix in the entries list
     * This will be used to truncate that prefix off for the test names
     * Note if there is only one entry, it will be the entire entry
     */
    private int greatestCommonPrefixIndex()
    {
        // TODO: this has never been tested with multiple entries, as we never actually run with more than one entry
        int commonPrefixIndex = this.entries.get(0).url.getPath().length();
        for(int i = 0; i < this.entries.size(); i++)
        {
            for(int j = i + 1; j < this.entries.size(); j++)
            {
                final int index = greatestCommonPrefixIndex(this.entries.get(i).url.getPath(), this.entries.get(j).url.getPath());
                commonPrefixIndex = Math.min(commonPrefixIndex, index);
            }
        }
        return commonPrefixIndex;
    }
    
    private int greatestCommonPrefixIndex(String a, String b)
    {
        
        int minLength = Math.min(a.length(), b.length());
        for(int i = 0; i < minLength; i++)
        {
            if(a.charAt(i) != b.charAt(i))
            {
                return i;
            }
        }
        return minLength;
    }
}
