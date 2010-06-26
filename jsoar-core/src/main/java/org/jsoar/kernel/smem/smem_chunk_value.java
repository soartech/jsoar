/*
 * Copyright (c) 2010 Dave Ray <daveray@gmail.com>
 *
 * Created on Jun 23, 2010
 */
package org.jsoar.kernel.smem;

import org.jsoar.kernel.symbols.SymbolImpl;

/**
 * @author ray
 */
public interface smem_chunk_value
{
    smem_chunk_lti asLti();
    SymbolImpl asConstant();
}
