/*
 * Copyright (c) 2009  Dave Ray <daveray@gmail.com>
 *
 * Created on Mar 22, 2009
 */
package org.jsoar.util;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.MalformedURLException;
import java.net.URL;

/**
 * @author ray
 */
public class FileTools
{
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
    
    public static String getExtension(String name)
    {
        int i = name.lastIndexOf('.');
        return i != -1 ? name.substring(i+1) : "";
    }
    
    /**
     * Copy an input stream to an output stream
     * 
     * @param from the input stream to copy from
     * @param to the output stream to copy to
     * @throws IOException
     */
    public static void copy(InputStream from, OutputStream to) throws IOException
    {
        final byte[] buffer = new byte[8092];
        int r = from.read(buffer);
        while(r != -1)
        {
            to.write(buffer, 0, r);
            r = from.read(buffer);
        }
    }
    
    /**
     * Copy one file to another
     * 
     * @param from the file to copy
     * @param to the file to create
     * @throws IOException
     */
    public static void copy(File from, File to) throws IOException
    {
        InputStream fromStream = null;
        OutputStream toStream = null;
        try
        {
            fromStream = new BufferedInputStream(new FileInputStream(from));
            toStream = new BufferedOutputStream(new FileOutputStream(from));
            copy(fromStream, toStream);
        }
        finally
        {
            try
            {
                if(fromStream != null)
                {
                    fromStream.close();
                }
            }
            finally
            {
                if(toStream != null)
                {
                    toStream.close();
                }
            }
        }
        
    }
}
