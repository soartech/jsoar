/*
 * (c) 2010  The JSoar Project
 *
 * Created on June 8, 2010
 */
package org.jsoar.kernel.memory;

import java.util.Collections;
import java.util.FormattableFlags;
import java.util.Formatter;
import java.util.Iterator;
import java.util.LinkedHashSet;
import java.util.Set;

import org.jsoar.kernel.symbols.Identifier;
import org.jsoar.kernel.symbols.Symbol;
import org.jsoar.kernel.symbols.SymbolFactory;
import org.jsoar.kernel.symbols.Symbols;
import org.jsoar.util.adaptables.AbstractAdaptable;

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

    public static DummyWme create(SymbolFactory syms, Object id, Object attr, Object value)
    {
        return new DummyWme((Identifier) Symbols.create(syms, id), 
                            Symbols.create(syms, attr),
                            Symbols.create(syms, value));
    }
    
    public static Set<Wme> create(SymbolFactory syms, Object... wmes)
    {
        if(wmes.length % 3 != 0)
        {
            throw new IllegalArgumentException("wmes must be a multiple of 3");
        }
                
        final Set<Wme> result = new LinkedHashSet<Wme>();
        for(int i = 0; i < wmes.length; i += 3)
        {
            result.add(DummyWme.create(syms, wmes[i], wmes[i+1], wmes[i+2]));
        }
        return result;
    }
    
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
        return Collections.<Preference>emptyIterator();
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
