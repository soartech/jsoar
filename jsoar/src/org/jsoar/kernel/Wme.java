/*
 * (c) 2008  Dave Ray
 *
 * Created on Aug 15, 2008
 */
package org.jsoar.kernel;

import org.jsoar.kernel.rete.RightMemory;
import org.jsoar.kernel.rete.Token;
import org.jsoar.kernel.symbols.Identifier;
import org.jsoar.kernel.symbols.Symbol;
import org.jsoar.util.AsListItem;
import org.jsoar.util.ListHead;

/**
 * @author ray
 */
public class Wme
{
    public Identifier id;
    public Symbol attr;
    public Symbol value;
    public boolean acceptable;
    
//    unsigned long timetag;
//    unsigned long reference_count;
//    struct wme_struct *rete_next, *rete_prev; 
    public AsListItem<Wme> in_rete = new AsListItem<Wme>(this); // used for dll of wmes in rete
    public ListHead<RightMemory> right_mems = new ListHead<RightMemory>(); // used for dll of rm's it's in
    public ListHead<Token> tokens = new ListHead<Token>(); // dll of tokens in rete
//    struct wme_struct *next, *prev;           /* (see above) */
//    struct preference_struct *preference;     /* pref. supporting it, or NIL */
//    struct output_link_struct *output_link;   /* for top-state output commands */
//    tc_number grounds_tc;                     /* for chunker use only */
//    tc_number potentials_tc, locals_tc;
//    struct preference_struct *chunker_bt_pref;
//
//    /* REW: begin 09.15.96 */
//    struct gds_struct *gds;
//    struct wme_struct *gds_next, *gds_prev; /* used for dll of wmes in gds */
//    /* REW: end   09.15.96 */

}
