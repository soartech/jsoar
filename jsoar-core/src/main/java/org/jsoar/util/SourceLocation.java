/*
 * Copyright (c) 2009 Dave Ray <daveray@gmail.com>
 *
 * Created on Jul 15, 2009
 */
package org.jsoar.util;

/**
 * Represents a location in a source file.
 * 
 * @author ray
 */
public interface SourceLocation
{
    /**
     * @return the absolute file or URL, or {@code null} if unknown
     */
    String getFile();
    
    /**
     * @return the character offset in the file, or {@code -1} if unknown
     */
    int getOffset();
    
    /**
     * @return the length, characters of the source, or {@code -1} if unknown
     */
    int getLength();
    
    /**
     * @return the line (starting at 0), or {@code -1} if unknown
     */
    int getLine();
}
