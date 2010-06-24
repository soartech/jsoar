/*
 * Copyright (c) 2010 Dave Ray <daveray@gmail.com>
 *
 * Created on Jun 23, 2010
 */
package org.jsoar.kernel.smem;

import com.sun.java_cup.internal.runtime.Symbol;

/**
 * <p>semantic_memory.h:334:smem_chunk_value_constant
 * 
 * @author ray
 */
public class smem_chunk_constant implements smem_chunk_value
{
    private final Symbol val_value; 

    public smem_chunk_constant(Symbol valValue)
    {
        val_value = valValue;
    }

    @Override
    public Symbol asConstant()
    {
        return val_value;
    }

    @Override
    public smem_chunk_lti asLti()
    {
        return null;
    }

}
