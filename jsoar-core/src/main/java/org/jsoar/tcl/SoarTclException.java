/*
 * Copyright (c) 2008  Dave Ray <daveray@gmail.com>
 *
 * Created on Sep 26, 2008
 */
package org.jsoar.tcl;

import org.jsoar.kernel.SoarException;

import tcl.lang.Interp;

/**
 * @author ray
 */
public class SoarTclException extends SoarException
{
    private static final long serialVersionUID = -8338120035464058863L;

    public SoarTclException(Interp interp)
    {
        super(interp.getResult().toString());
    }
}
