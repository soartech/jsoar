package org.jsoar.debugger.syntax;

import javax.swing.text.AttributeSet;

public class StyleOffset implements Comparable<StyleOffset>
{
    public int start = 0, end = 0;
    public AttributeSet style;
    
    public StyleOffset(int start, int end, AttributeSet attributeSet)
    {
        this.start = start;
        this.end = end;
        this.style = attributeSet;
    }
    
    public int length()
    {
        return end - start;
    }
    
    @Override
    public int compareTo(StyleOffset o)
    {
        if(o != null)
        {
            if(start == o.start)
            {
                return length() - o.length();
            }
            else
            {
                return start - o.start;
            }
        }
        else
        {
            return -1;
        }
    }
}
