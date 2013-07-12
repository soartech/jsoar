/**
 * 
 */
package org.jsoar.performancetesting;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.List;

/**
 * A class for representing a Table and outputing the Table.
 * 
 * @author ALT
 *
 */
public class Table
{
    List<Row> rows;
    
    public Table()
    {
        rows = new ArrayList<Row>();
    }
    
    public void addRow(Row row)
    {
        rows.add(row);
    }
    
    public Row getRow(int row)
    {
        if (row >= rows.size())
            return null;
        else
            return rows.get(row);
    }
    
    public void addCellAtRow(Cell cell, int row)
    {
        if (row >= rows.size())
        {
            return;
        }
        
        rows.get(row).add(cell);
    }
    
    public void setValueAtLocation(String value, int row, int cell)
    {
        if (row >= rows.size())
            return;
        
        if (cell >= rows.get(row).getCellCount())
            return;
        
        rows.get(row).getCell(cell).setValue(value);
    }
    
    public void setOrAddValueAtLocation(String value, int row, int cell)
    {
        while (rows.size() <= row)
        {
            rows.add(new Row());
        }
        
        Row rowToSetAt = rows.get(row);
        
        while (rowToSetAt.getCellCount() <= cell)
        {
            rowToSetAt.add(new Cell(""));
        }
        
        rowToSetAt.getCell(cell).setValue(value);
    }
    
    public void writeToWriter(PrintWriter out)
    {
        out.print("\n");
        
        int maxCellSize = 0;
        int maxCellCount = 0;
        
        for (Row row : rows)
        {
            for (Cell cell : row.getCells())
            {
                int size = cell.getValue().length();
                
                if (size > maxCellSize)
                {
                    maxCellSize = size;
                }
            }
            
            int size = row.getCellCount();
            
            if (size > maxCellCount)
            {
                maxCellCount = size;
            }
        }
        
        if (maxCellCount == 0)
        {
            return;
        }
        
        //Add two space padding
        maxCellSize += 2;
        
        for (int i = 0;i < rows.size();i++)
        {
            Row row = rows.get(i);
            
            while (row.getCellCount() < maxCellCount)
            {
                row.add(new Cell(""));
            }
            
            for (Cell cell : row.getCells())
            {
                out.print("| " + cell.getValue());
                
                for (int j = 1 + cell.getValue().length();j < maxCellSize;j++)
                {
                    out.print(" ");
                }
            }
            
            out.print("|\n");
            
            // Output the divider between rows

            // This also assumes a fixed-width font (which you should be using
            // anyways
            // for the console
            // - ALT
            int dividers = maxCellSize * maxCellCount + maxCellCount * 1 + 1;
            for (int j = 0; j < dividers; j++)
            {
                out.print("-");
            }

            out.print("\n");
        }
        
        out.flush();
    }
    
    public void writeToCSV(String file)
    {
        writeToCSV(file, '\t', false);
    }
    
    public void writeToCSV(String file, boolean append)
    {
        writeToCSV(file, '\t', append);
    }
    
    public void writeToCSV(String file, Character delimiter, boolean append)
    {
        File outFile = new File(file);
        if (outFile.exists() && !append)
        {
            outFile.delete();
        }
        
        PrintWriter out = null;
        
        try
        {
            out = new PrintWriter(new BufferedWriter(new FileWriter(outFile, append)));
        }
        catch (IOException e)
        {
            // TODO Auto-generated catch block
            e.printStackTrace();
            throw new AssertionError();
        }
        
        for (int i = 0;i < rows.size();i++)
        {
            Row row = rows.get(i);
            
            String line = "";
            
            for (Cell cell : row.getCells())
            {
                line += cell.getValue() + delimiter;
            }

            out.print(line.trim() + "\n");
        }
        
        out.println(delimiter); // if multiple tests are appended to the same file, this will separate them by a line
        
        out.flush();
        out.close();
    }
    
}
