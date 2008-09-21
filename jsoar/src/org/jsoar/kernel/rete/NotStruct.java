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
    final Symbol s1;               /* the two identifiers constrained to be "<>" */
    final Symbol s2;
    
    /**
     * @param s1
     * @param s2
     */
    public NotStruct(Symbol s1, Symbol s2)
    {
        this.s1 = s1;
        this.s2 = s2;
    }
    
    
}
