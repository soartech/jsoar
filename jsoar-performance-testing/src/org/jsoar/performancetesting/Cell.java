/**
 * 
 */
package org.jsoar.performancetesting;

/**
 * @author ALT
 *
 */
public class Cell
{
    private String value;
    private int rowLocation;
    
    public Cell(String value)
    {
        this.value = value;
        rowLocation = -1;
    }

    public Cell(String value, int rowLocation)
    {
        this.value = value;
        this.rowLocation = rowLocation;
    }
    
    public void setValue(String value)
    {
        this.value = value;
    }
    
    public String getValue()
    {
        return value;
    }
    
    public void setRowLocation(int rowLocation)
    {
        this.rowLocation = rowLocation;
    }
    
    public int getRowLocation()
    {
        return rowLocation;
    }
}
