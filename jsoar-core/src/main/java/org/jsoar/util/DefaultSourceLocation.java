/*
 * Copyright (c) 2009 Dave Ray <daveray@gmail.com>
 *
 * Created on Jul 15, 2009
 */
package org.jsoar.util;

/**
 * Default implementation of {@link SourceLocation} interface
 * 
 * @author ray
 */
public class DefaultSourceLocation implements SourceLocation
{
    public static final DefaultSourceLocation UNKNOWN = new DefaultSourceLocation(null, -1, -1);
    
    private final String file;
    private final int offset;
    private final int length;
    
    public DefaultSourceLocation(String file, int offset, int length)
    {
        this.file = file;
        this.offset = offset;
        this.length = length;
    }

    /* (non-Javadoc)
     * @see org.jsoar.util.SourceLocation#getFile()
     */
    @Override
    public String getFile()
    {
        return file;
    }

    /* (non-Javadoc)
     * @see org.jsoar.util.SourceLocation#getLength()
     */
    @Override
    public int getLength()
    {
        return length;
    }

    /* (non-Javadoc)
     * @see org.jsoar.util.SourceLocation#getOffset()
     */
    @Override
    public int getOffset()
    {
        return offset;
    }

}
