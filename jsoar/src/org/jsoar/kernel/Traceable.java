/*
 * Copyright (c) 2008  Dave Ray <daveray@gmail.com>
 *
 * Created on Sep 20, 2008
 */
package org.jsoar.kernel;

import java.util.Formatter;

/**
 * @author ray
 */
public interface Traceable
{
    void trace(Trace trace, Formatter formatter, int flags, int width, int precision);
}
