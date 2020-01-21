/*
 * Copyright (c) 2008  Dave Ray <daveray@gmail.com>
 *
 * Created on Sep 3, 2008
 */
package org.jsoar.kernel.rete;

/**
 * @author ray
 */
public enum ReteNodeType
{
    /* --- types and structure of beta nodes --- */  
    /*   key:  bit 0 --> hashed                  */
    /*         bit 1 --> memory                  */
    /*         bit 2 --> positive join           */
    /*         bit 3 --> negative join           */
    /*         bit 4 --> split from beta memory  */
    /*         bit 6 --> various special types   */

    /* Warning: If you change any of these or add ones, be sure to update the
       bit-twiddling macros just below */
    UNHASHED_MEMORY_BNODE(0x02),
    MEMORY_BNODE (0x03),
    UNHASHED_MP_BNODE(0x06),
    MP_BNODE(0x07),
    UNHASHED_POSITIVE_BNODE(0x14),
    POSITIVE_BNODE(0x15),
    UNHASHED_NEGATIVE_BNODE(0x08),
    NEGATIVE_BNODE(0x09),
    DUMMY_TOP_BNODE(0x40),
    DUMMY_MATCHES_BNODE(0x41),
    CN_BNODE(0x42),
    CN_PARTNER_BNODE(0x43),
    P_BNODE(0x44);

    private final int index;
    
    private ReteNodeType(int index)
    {
        this.index = index;
    }
    
    public boolean bnode_is_hashed() { return (index & 0x01) != 0; }
    public boolean bnode_is_memory() { return (index & 0x02) != 0; }
    public boolean bnode_is_positive() { return (index & 0x04) != 0; }
    public boolean bnode_is_negative() { return (index & 0x08) != 0; }
    public boolean bnode_is_posneg() { return (index & 0x0C) != 0; }
    public boolean bnode_is_bottom_of_split_mp() { return (index & 0x10) != 0; }

}
