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
    public static final DefaultSourceLocation UNKNOWN = new DefaultSourceLocation(null, -1, -1, -1);
    
    public static Builder newBuilder() { return new Builder(); }
    
    public static class Builder
    {
        private String file;
        private int offset = -1;
        private int length = -1;
        private int line = -1;
        
        private Builder() {}
        
        public Builder file(String v) { file = v; return this; }
        public Builder offset(int v)  { offset = v; return this; }
        public Builder length(int v) { length = v; return this; }
        public Builder line(int v) { line = v; return this; }
        
        public DefaultSourceLocation build()
        {
            return new DefaultSourceLocation(file, offset, length, line);
        }
    }
    
    private final String file;
    private final int offset;
    private final int length;
    private final int line;
    
    private DefaultSourceLocation(String file, int offset, int length, int line)
    {
        this.file = file;
        this.offset = offset;
        this.length = length;
        this.line = line;
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

    /* (non-Javadoc)
     * @see org.jsoar.util.SourceLocation#getLine()
     */
    @Override
    public int getLine()
    {
        return line;
    }

    /* (non-Javadoc)
     * @see java.lang.Object#toString()
     */
    @Override
    public String toString()
    {
        return  (file != null && file.length() > 0 ? file : "*unknown*") +
                (line != -1 ? (":" + (line + 1)) : "");
    }

    
}
