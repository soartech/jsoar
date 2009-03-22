/*
 * Copyright (c) 2009  Dave Ray <daveray@gmail.com>
 *
 * Created on Mar 21, 2009
 */
package org.jsoar.runtime;

/**
 * @author ray
 */
public interface Completer<T>
{
    void finish(T result);
}
