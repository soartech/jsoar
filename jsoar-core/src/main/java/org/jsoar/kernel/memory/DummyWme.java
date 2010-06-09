/*
 * (c) 2010  The JSoar Project
 *
 * Created on June 8, 2010
 */
package org.jsoar.kernel.memory;

import java.util.FormattableFlags;
import java.util.Formatter;
import java.util.Iterator;

import org.jsoar.kernel.symbols.Identifier;
import org.jsoar.kernel.symbols.Symbol;
import org.jsoar.util.adaptables.AbstractAdaptable;

import com.google.common.collect.Iterators;

/**
 * A dummy implementation of the {@link Wme} interface. Basically a container
 * for a triple.
 * 
 * <p>In this implementation {@link #getTimetag()} always returns {@code -1}
 * and {@link #getPreferences()} always returns an empty iterator.
 * 
 * @author ray
 */
public class DummyWme extends AbstractAdaptable implements Wme
{
    final private Identifier id;
    final private Symbol attr;
    final private Symbol value;

    public DummyWme(Identifier id, Symbol attr, Symbol value)
    {
        this.id = id;
        this.attr = attr;
        this.value = value;
    }

    /* (non-Javadoc)
     * @see org.jsoar.kernel.memory.Wme#getAttribute()
     */
    @Override
    public Symbol getAttribute()
    {
        return attr;
    }

    /* (non-Javadoc)
     * @see org.jsoar.kernel.memory.Wme#getChildren()
     */
    @Override
    public Iterator<Wme> getChildren()
    {
        return null;
    }

    /* (non-Javadoc)
     * @see org.jsoar.kernel.memory.Wme#getIdentifier()
     */
    @Override
    public Identifier getIdentifier()
    {
        return id;
    }

    /* (non-Javadoc)
     * @see org.jsoar.kernel.memory.Wme#getPreferences()
     */
    @Override
    public Iterator<Preference> getPreferences()
    {
        return Iterators.emptyIterator();
    }

    /* (non-Javadoc)
     * @see org.jsoar.kernel.memory.Wme#getTimetag()
     */
    @Override
    public int getTimetag()
    {
        return -1;
    }

    /* (non-Javadoc)
     * @see org.jsoar.kernel.memory.Wme#getValue()
     */
    @Override
    public Symbol getValue()
    {
        return value;
    }

    /* (non-Javadoc)
     * @see org.jsoar.kernel.memory.Wme#isAcceptable()
     */
    @Override
    public boolean isAcceptable()
    {
        return false;
    }

    @Override
    public void formatTo(Formatter fmt, int f, int width, int precision)
    {
        // See WmeImpl.formatTo
        // print.cpp:981:print_wme
        // print.cpp:981:print_wme_without_timetag
        
        // TODO: I don't think that this should automatically insert a newline!
        if((f & FormattableFlags.ALTERNATE) == 0)
        {
            // This is the normal print_wme case. It is specified with the 
            // usual %s format string
            fmt.format("(%d: %s ^%s %s%s)\n", getTimetag(), id, attr, value, isAcceptable() ? " +" : "");
        }
        else
        {
            // This is the print_wme_without_timetag case
            // It is specified with the %#s format string.
            fmt.format("(%s ^%s %s%s)\n", id, attr, value, isAcceptable() ? " +" : "");
        }
    }
}
