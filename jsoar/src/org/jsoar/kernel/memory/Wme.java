/*
 * (c) 2008  Dave Ray
 *
 * Created on Aug 15, 2008
 */
package org.jsoar.kernel.memory;

import java.util.Formattable;
import java.util.FormattableFlags;
import java.util.Formatter;

import org.jsoar.kernel.GoalDependencySet;
import org.jsoar.kernel.io.OutputLink;
import org.jsoar.kernel.rete.RightMemory;
import org.jsoar.kernel.rete.Token;
import org.jsoar.kernel.symbols.Identifier;
import org.jsoar.kernel.symbols.Symbol;
import org.jsoar.util.AsListItem;
import org.jsoar.util.ListHead;

/**
   Fields in a WME:

      id, attr, value:  points to symbols for the wme fields

      acceptable:  TRUE iff this is an acceptable pref. wme

      timetag:  timetag of the wme

      reference count:  (see below)

      rete_next, rete_prev:  pointers in the doubly-linked list of all
         wmes currently known to the rete (header is all_wmes_in_rete)
         (this equals WM except while WM is being changed)

      right_mems:  header of a doubly-linked list of right memory entries
         (in one or more alpha memories containing the wme).  This is used
         only by the Rete, as part of list-based remove.

      tokens:  header of a doubly-linked list of tokens in the Rete.
         This is used only by the Rete, as part of list-based remove.

      next, prev:  pointers in a doubly-linked list of wmes.
         Depending on the wme type, the header of this DLL is:
           - slot.wmes (for ordinary wmes)
           - slot.acceptable_preference_wmes (for acceptable pref. wmes)
           - id.impasse_wmes (for architecture-created goal/impasse wmes)
           - id.input_wmes (for Soar I/O wmes)

      preference:  points to the preference supporting the wme.  For I/O
         wmes and (most) architecture-created wmes, this is NIL.

      output_link:  this is used only for top-state output links.
         It points to an output_link structure used by the I/O routines.

      grounds_tc, potentials_tc, locals_tc:  used by the chunker to indicate
         whether this wme is in the grounds, potentials, and/or locals sets

      chunker_bt_pref: used by the chunker; set to cond->bt.trace when
         a wme is added to either the potentials or locals set

      These are the additions to the WME structure that will be used
         to track dependencies for goals.  Each working memory element
     now includes a pointer  to a gds_struct (defined below) and
     pointers to other WMEs on the same GDS.

      gds: the goal dependency set the wme is in
      gds_next, gds_prev:  used for dll of all wmes in gds

      If a particular working memory element is not dependent for any goal,
     then the values for these pointers will all be NIL. If a WME is
     dependent for more than one goal, then it will point to the GDS
     of the highest goal.
    

      

   Reference counts on wmes:
      +1 if the wme is currently in WM
      +1 for each instantiation condition that points to it (bt.wme)
   We deallocate a wme when its reference count goes to 0.

 * 
 * wmem.h:125:wme
 * 
 * @author ray
 */
public class Wme implements Formattable
{
    public final Identifier id;
    public final Symbol attr;
    public final Symbol value;
    public final boolean acceptable;
    
    public final int timetag;
    private int reference_count;
    public final AsListItem<Wme> in_rete = new AsListItem<Wme>(this); // used for dll of wmes in rete
    public final ListHead<RightMemory> right_mems = new ListHead<RightMemory>(); // used for dll of rm's it's in
    public final ListHead<Token> tokens = new ListHead<Token>(); // dll of tokens in rete
    public final AsListItem<Wme> next_prev = new AsListItem<Wme>(this); // (see above)
    public Preference preference;     // pref. supporting it, or null
    public OutputLink output_link;   /* for top-state output commands */
    
    public int grounds_tc;                     /* for chunker use only */
    public int potentials_tc, locals_tc;
    public Preference chunker_bt_pref;
    
    public GoalDependencySet gds;
    public AsListItem<Wme> gds_next_prev = new AsListItem<Wme>(this); // part of dll of wmes in gds
    
    /**
     * @param id
     * @param attr
     * @param value
     * @param acceptable
     * @param timetag
     */
    public Wme(Identifier id, Symbol attr, Symbol value, boolean acceptable, int timetag)
    {
        this.id = id;
        this.attr = attr;
        this.value = value;
        this.acceptable = acceptable;
        this.timetag = timetag;
    }

    /**
     * wmem.h:160:wme_add_ref
     */
    public void wme_add_ref()
    {
        reference_count++;
    }
    public void wme_remove_ref(WorkingMemory wm)
    {
      /* There are occaisionally wme's with zero reference counts 
         created in the system. Make sure this function handles them 
         correctly. */
      if (reference_count != 0) { reference_count--; }
      if (reference_count == 0) { wm.deallocate_wme(this); }
    }    
    /* (non-Javadoc)
     * @see java.lang.Object#toString()
     */
    @Override
    public String toString()
    {
        return "<" + id + ", " + attr + ", " + value + ">:" + timetag;
    }

    /* (non-Javadoc)
     * @see java.util.Formattable#formatTo(java.util.Formatter, int, int, int)
     */
    @Override
    public void formatTo(Formatter fmt, int f, int width, int precision)
    {
        // print.cpp:981:print_wme
        // print.cpp:981:print_wme_without_timetag
        
        if((f & FormattableFlags.ALTERNATE) == 0)
        {
            // This is the normal print_wme case. It is specified with the 
            // usual %s format string
            fmt.format("(%d: %s ^%s %s%s)\n", timetag, id, attr, value, acceptable ? " +" : "");
        }
        else
        {
            // This is the print_wme_without_timetag case
            // It is specified with the %#s format string.
            fmt.format("(%s ^%s %s%s)\n", id, attr, value, acceptable ? " +" : "");
        }

        // <wme tag="123" id="s1" attr="foo" attrtype="string" val="123" valtype="string"></wme>
        // TODO xml_object( thisAgent, w );
    }
    
    
}
