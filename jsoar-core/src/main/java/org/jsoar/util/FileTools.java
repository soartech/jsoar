/*
 * Copyright (c) 2009  Dave Ray <daveray@gmail.com>
 *
 * Created on Mar 22, 2009
 */
package org.jsoar.util;

import java.io.File;
import java.net.MalformedURLException;
import java.net.URL;

/**
 * @author ray
 */
public class FileTools
{
    /**
     * Given a name, replaces all characters that are illegal in file names, e.g. ?, *, etc.
     * 
     * @param name the name to process
     * @param replacement the replacement string for illegal chars, e.g. "_"
     * @return the new string for illegal characters replaced.
     */
    public static String replaceIllegalCharacters(String name, String replacement)
    {
        return name.replaceAll("[\\?\\[\\]/\\\\=\\+\\<\\>\\:\\;\"\\,\\*\\|]", replacement);
    }
    
    /**
     * Convert the given string to a URL.
     * 
     * @param url the url string
     * @return a new URL object, or {@code null} if not a valid URL.
     */
    public static URL asUrl(String url)
    {
        try
        {
            return new URL(url);
        }
        catch (MalformedURLException e)
        {
            return null;
        }
    }
    
    /**
     * Get the extension of a file name, without the dot. If there's no dot in
     * the file name, {@code null} is returned.
     * 
     * @param name the file name
     * @return the extension of the file, without the dot, or {@code null} if
     *  there is no extension.
     */
    public static String getExtension(String name)
    {
        final int dot = name.lastIndexOf('.');
        final int slash = name.lastIndexOf('/');
        if(dot != -1)
        {
            return slash != -1 && slash > dot ? null : name.substring(dot+1);
        }
        else
        {
            return null;
        }
    }
    
    /**
     * Get the parent of a file. If {@link File#getParent()} returns {@code null}
     * returns ".." if the file is a directory, or "." if a file.
     * 
     * @param file the file
     * @return the parent of the file, never {@code null}
     */
    public static String getParent(File file)
    {
        final String p = file.getParent();
        
        return p != null ? p : (file.isDirectory() ? ".." : ".");
    }
    
}
