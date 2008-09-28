/*
 * (c) 2008  Dave Ray
 *
 * Created on Aug 22, 2008
 */
package org.jsoar.kernel.rete;

import org.jsoar.kernel.symbols.Identifier;

/**
 * instantiations.h:78
 * 
 * @author ray
 */
public class NotStruct
{
    public NotStruct next;  /* next Not in the singly-linked list */
    public final Identifier s1;               /* the two identifiers constrained to be "<>" */
    public final Identifier s2;
    
    /**
     * @param s1
     * @param s2
     */
    public NotStruct(Identifier s1, Identifier s2)
    {
        this.s1 = s1;
        this.s2 = s2;
    }
    
    
}
