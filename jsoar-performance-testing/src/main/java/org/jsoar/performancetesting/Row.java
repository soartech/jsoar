/**
 * 
 */
package org.jsoar.performancetesting;

import java.util.ArrayList;
import java.util.List;

/**
 * @author ALT
 * 
 */
public class Row
{
    private List<Cell> cells;

    public Row()
    {
        this.cells = new ArrayList<Cell>();
    }

    public Row(List<Cell> cells)
    {
        this.cells = new ArrayList<Cell>(cells);
    }

    public void add(Cell cell)
    {
        cells.add(cell);
    }

    public void setCell(Cell cell, int location)
    {
        if (cells.size() < location)
        {
            while (cells.size() < location)
            {
                add(new Cell("", cells.size()));
            }

            cell.setRowLocation(location);
            add(cell);
        }
        else
        {
            cells.get(location).setValue(cell.getValue());

            cell.setRowLocation(location);
        }
    }

    public void removeCell(Cell cell)
    {
        if (cell.getRowLocation() < cells.size())
        {
            if (cells.get(cell.getRowLocation()).getValue() == cell.getValue())
            {
                cells.remove(cell.getRowLocation());
            }
            else
            {
                return;
            }
        }
        else
        {
            return;
        }

        for (int i = cell.getRowLocation(); i < cells.size(); i++)
        {
            Cell c = cells.get(i);

            c.setRowLocation(i);
        }
    }

    public void removeCell(int cell)
    {
        if (cell < cells.size())
        {
            cells.remove(cell);

            for (int i = cell; i < cells.size(); i++)
            {
                Cell c = cells.get(i);

                c.setRowLocation(i);
            }
        }
    }

    public Cell getCell(int cell)
    {
        return cells.get(cell);
    }

    public int getCellCount()
    {
        return cells.size();
    }

    public List<Cell> getCells()
    {
        return cells;
    }
}
