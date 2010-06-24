/*
 * Copyright (c) 2010 Dave Ray <daveray@gmail.com>
 *
 * Created on Jun 23, 2010
 */
package org.jsoar.kernel.smem;

import com.sun.java_cup.internal.runtime.Symbol;

/**
 * @author ray
 */
public interface smem_chunk_value
{
    smem_chunk_lti asLti();
    Symbol asConstant();
}
