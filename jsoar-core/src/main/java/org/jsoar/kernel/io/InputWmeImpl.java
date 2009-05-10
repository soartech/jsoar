/*
 * Copyright (c) 2009 Dave Ray <daveray@gmail.com>
 *
 * Created on May 8, 2009
 */
package org.jsoar.kernel.io;

import java.util.Formatter;
import java.util.Iterator;

import org.jsoar.kernel.memory.Preference;
import org.jsoar.kernel.memory.Wme;
import org.jsoar.kernel.memory.WmeImpl;
import org.jsoar.kernel.symbols.Identifier;
import org.jsoar.kernel.symbols.Symbol;
import org.jsoar.util.adaptables.AbstractAdaptable;

/**
 * @author ray
 */
class InputWmeImpl extends AbstractAdaptable implements InputWme
{
    private final InputOutputImpl io;
    private WmeImpl inner;
    
    InputWmeImpl(InputOutputImpl io, WmeImpl inner)
    {
        this.io = io;
        this.inner = inner;
        this.inner.setOuterInputWme(this);
    }
    
    WmeImpl getInner()
    {
        return inner;
    }
    
    void setInner(WmeImpl inner)
    {
        this.inner.setOuterInputWme(null);
        this.inner = inner;
        this.inner.setOuterInputWme(this);
    }
    
    /* (non-Javadoc)
     * @see org.jsoar.kernel.io.InputWme#getInputOutput()
     */
    @Override
    public InputOutput getInputOutput()
    {
        return io;
    }

    /* (non-Javadoc)
     * @see org.jsoar.kernel.io.InputWme#remove()
     */
    @Override
    public void remove()
    {
        io.removeInputWme(this);
    }

    /* (non-Javadoc)
     * @see org.jsoar.kernel.io.InputWme#update(org.jsoar.kernel.symbols.Symbol)
     */
    @Override
    public void update(Symbol newValue)
    {
        io.updateInputWme(this, newValue);
    }

    /* (non-Javadoc)
     * @see org.jsoar.kernel.memory.Wme#getAttribute()
     */
    @Override
    public Symbol getAttribute()
    {
        return inner.getAttribute();
    }

    /* (non-Javadoc)
     * @see org.jsoar.kernel.memory.Wme#getChildren()
     */
    @Override
    public Iterator<Wme> getChildren()
    {
        return inner.getChildren();
    }

    /* (non-Javadoc)
     * @see org.jsoar.kernel.memory.Wme#getIdentifier()
     */
    @Override
    public Identifier getIdentifier()
    {
        return inner.getIdentifier();
    }

    /* (non-Javadoc)
     * @see org.jsoar.kernel.memory.Wme#getPreferences()
     */
    @Override
    public Iterator<Preference> getPreferences()
    {
        return inner.getPreferences();
    }

    /* (non-Javadoc)
     * @see org.jsoar.kernel.memory.Wme#getTimetag()
     */
    @Override
    public int getTimetag()
    {
        return inner.getTimetag();
    }

    /* (non-Javadoc)
     * @see org.jsoar.kernel.memory.Wme#getValue()
     */
    @Override
    public Symbol getValue()
    {
        return inner.getValue();
    }

    /* (non-Javadoc)
     * @see org.jsoar.kernel.memory.Wme#isAcceptable()
     */
    @Override
    public boolean isAcceptable()
    {
        return inner.isAcceptable();
    }

    /* (non-Javadoc)
     * @see java.util.Formattable#formatTo(java.util.Formatter, int, int, int)
     */
    @Override
    public void formatTo(Formatter formatter, int flags, int width,
            int precision)
    {
        inner.formatTo(formatter, flags, width, precision);
    }

}
