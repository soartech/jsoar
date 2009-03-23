/*
 * Copyright (c) 2009  Dave Ray <daveray@gmail.com>
 *
 * Created on Mar 22, 2009
 */
package org.jsoar.util;

/**
 * @author ray
 */
public class FileTools
{
    public static String getExtension(String name)
    {
        int i = name.lastIndexOf('.');
        return i != -1 ? name.substring(i+1) : "";
    }
}
