/*
 * Copyright (c) 2008  Dave Ray <daveray@gmail.com>
 *
 * Created on Aug 24, 2008
 */
package org.jsoar.util;

/**
 * @author ray
 */
public interface HashFunction <T extends HashTableItem>
{
    int calculate(T item, int num_bits);
}
