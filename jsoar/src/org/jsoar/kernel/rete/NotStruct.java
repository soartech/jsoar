/*
 * (c) 2008  Dave Ray
 *
 * Created on Aug 22, 2008
 */
package org.jsoar.kernel.rete;

import org.jsoar.kernel.symbols.Symbol;

/**
 * instantiations.h:78
 * 
 * @author ray
 */
public class NotStruct
{
    NotStruct next;  /* next Not in the singly-linked list */
    Symbol s1;               /* the two identifiers constrained to be "<>" */
    Symbol s2;
}
