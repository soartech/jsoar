/*
 * Copyright (c) 2008  Dave Ray <daveray@gmail.com>
 *
 * Created on Dec 16, 2008
 */
package org.jsoar.kernel.memory;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.jsoar.kernel.Agent;
import org.jsoar.kernel.symbols.IdentifierImpl;
import org.jsoar.kernel.symbols.Symbol;
import org.jsoar.kernel.symbols.SymbolImpl;
import org.jsoar.kernel.tracing.Printer;
import org.jsoar.util.Arguments;
import org.jsoar.util.markers.DefaultMarker;
import org.jsoar.util.markers.Marker;


/**
 * @author ray
 */
public class WorkingMemoryPrinter
{
    private Printer printer;
    private int depth = 1;
    private boolean internal = false;
    private boolean tree = false;
    private boolean exact = false;

    /**
     * <p>sml_KernelHelpers.cpp::do_print_for_identifier
     * 
     * @param agent
     * @param printer
     * @param idIn
     * @param pattern
     */
    public void print(Agent agent, Printer printer, Symbol idIn, String pattern)
            throws Exception
    {
        Arguments.checkNotNull(agent, "agent");
        Arguments.checkNotNull(printer, "printer");

        this.printer = printer;

        if (exact || idIn == null)
        {
            // for wme patterns
            if (pattern.length() > 2 && pattern.charAt(0) == '(' && pattern.charAt(pattern.length() - 1) == ')')
                pattern = pattern.substring(1, pattern.length() - 1);
            
            List<Wme> wmes = Wmes.filter(agent.getAllWmesInRete().iterator(),
                    WorkingMemoryPatternReader.getPredicate(agent, pattern));
            if (internal)
            {
                for (Wme w : wmes)
                    printer.print("%s", w);
            }
            else
            {
                // create a map of id -> wme so we can print out wmes with the
                // same id together
                Map<Symbol, List<Wme>> objects = new HashMap<Symbol, List<Wme>>();
                for (Wme w : wmes)
                {
                    if (!objects.containsKey(w.getIdentifier()))
                    {
                        objects.put(w.getIdentifier(), new ArrayList<Wme>());
                    }
                    List<Wme> l = objects.get(w.getIdentifier());
                    l.add(w);
                }

                // for each id, print the id and the attr/value pairs of the
                // wmes with that id
                for (Symbol id : objects.keySet())
                {
                    printer.print("(%s", id);
                    for (Wme w : objects.get(id))
                    {
                        printer.print(" ^%s %s", w.getAttribute(), w.getValue());
                        if (w.isAcceptable())
                            printer.print(" +");
                    }
                    printer.print(")\n");
                }
            }
        }
        else
        {
            IdentifierImpl id = (IdentifierImpl) idIn;

            // RPM 4/07: first mark the nodes with their shallowest depth
            // then print them at their shallowest depth
            Marker tc = DefaultMarker.create();
            mark_depths_augs_of_id(id, depth, tc);
            tc = DefaultMarker.create();
            print_augs_of_id(id, depth, depth, internal, tree, tc);
        }
        this.printer = null;
    }
    
    public WorkingMemoryPrinter setDefaults()
    {
        this.depth = 1;
        this.internal = false;
        this.tree = false;
        return this;
    }
    
    /**
     * @return the depth
     */
    public int getDepth()
    {
        return depth;
    }

    /**
     * @param depth the depth to set
     */
    public WorkingMemoryPrinter setDepth(int depth)
    {
        this.depth = depth;
        return this;
    }

    /**
     * @return the internal
     */
    public boolean isInternal()
    {
        return internal;
    }

    /**
     * @param internal the internal to set
     */
    public WorkingMemoryPrinter setInternal(boolean internal)
    {
        this.internal = internal;
        return this;
    }

    /**
     * @return the tree
     */
    public boolean isTree()
    {
        return tree;
    }

    /**
     * @param tree the tree to set
     */
    public WorkingMemoryPrinter setTree(boolean tree)
    {
        this.tree = tree;
        return this;
    }
    
    /**
     * @return the exact
     */
    public boolean isExact()
    {
        return exact;
    }

    /**
     * @param exact the exact to set
     */
    public WorkingMemoryPrinter setExact(boolean exact)
    {
        this.exact = exact;
        return this;
    }
    

    /**
     * sml_KernelHelpers.cpp:246:compare_attr
     */
    private static final Comparator<WmeImpl> ATTRIBUTE_COMPARATOR = new Comparator<WmeImpl>() {

        @Override
        public int compare(WmeImpl o1, WmeImpl o2)
        {
            return o1.attr.toString().compareTo(o2.attr.toString());
        }
    };
    

    /**
     * <p>sml_KernelHelpers.cpp:264:neatly_print_wme_augmentation_of_id
     * 
     * @param w
     * @param indentation
     */
    private void neatly_print_wme_augmentation_of_id(Wme w, int indentation)
    {
        final String buf = String.format(" ^%s %s%s", w.getAttribute(), w.getValue(), w.isAcceptable() ? " +" : "");

        printer.print(buf);
    }

/**
 * RPM 4/07 bug 988
 * This function traverses the ids we are going to print and marks each with its
 * shallowest depth That is, if an id can be reached by multiple paths, this
 * will find the shortest one and save the depth of that path on the id. Thus,
 * when we print, the wmes will be indented properly, making it much easier to
 * read, and avoiding bugs (see bug 988).
 * 
 * <p>
 * sml_KernelHelpers.cpp:290:mark_depths_augs_of_id
 * 
 * @param id
 * @param depth
 * @param tc
 */
private void mark_depths_augs_of_id (SymbolImpl idIn, int depth, Marker tc) 
{
    /* AGR 652  The plan is to go through the list of WMEs and find out how
    many there are.  Then we malloc an array of that many pointers.
    Then we go through the list again and copy all the pointers to that array.
    Then we qsort the array and print it out.  94.12.13 */

    IdentifierImpl id = idIn.asIdentifier();
    if (id == null) return;
    if (id.tc_number==tc && id.depth >= depth) return;  // this has already been printed at an equal-or-lower depth, RPM 4/07 bug 988
    
    id.depth = depth; // set the depth of this id
    id.tc_number = tc;

    /* --- if depth<=1, we're done --- */
    if (depth<=1) return;

    /* --- call this routine recursively --- */
    for (WmeImpl w=id.getInputWmes(); w!=null; w=w.next) {
        mark_depths_augs_of_id (w.attr, depth-1, tc);
        mark_depths_augs_of_id (w.value, depth-1, tc);
    }
    for (WmeImpl w=id.goalInfo != null ? id.goalInfo.getImpasseWmes() : null; w!=null; w=w.next) {
        mark_depths_augs_of_id (w.attr, depth-1, tc);
        mark_depths_augs_of_id (w.value, depth-1, tc);
    }
    for (Slot s = id.slots; s != null; s = s.next) {
        for (WmeImpl w=s.getWmes(); w!=null; w=w.next) {
            mark_depths_augs_of_id (w.attr, depth-1, tc);
            mark_depths_augs_of_id (w.value, depth-1, tc);
        }
        for (WmeImpl w=s.getAcceptablePreferenceWmes(); w!=null; w=w.next) {
            mark_depths_augs_of_id (w.attr, depth-1, tc);
            mark_depths_augs_of_id (w.value, depth-1, tc);
        }
    }
}

// 
/**
 * RPM 4/07: Note, mark_depths_augs_of_id must be called before the root call to
 * print_augs_of_id Thus, this should probably only be called from
 * do_print_for_identifier
 * 
 * <p>sml_KernelHelpers.cpp:335:print_augs_of_id
 * 
 * @param idIn
 * @param depth
 * @param maxdepth
 * @param internal
 * @param tree
 * @param tc
 */
private void print_augs_of_id (SymbolImpl idIn, 
    int depth,
    int maxdepth,
    boolean internal,
    boolean tree,
    Marker tc) 
{
    /* AGR 652  The plan is to go through the list of WMEs and find out how
    many there are.  Then we malloc an array of that many pointers.
    Then we go through the list again and copy all the pointers to that array.
    Then we qsort the array and print it out.  94.12.13 */

    IdentifierImpl id = idIn.asIdentifier();
    if (id == null) return;
    if (id.tc_number==tc) return;  // this has already been printed, so return RPM 4/07 bug 988
    if (id.depth > depth) return;  // this can be reached via an equal or shorter path, so return without printing RPM 4/07 bug 988

    // if we're here, then we haven't printed this id yet, so print it

    depth = id.depth; // set the depth to the depth via the shallowest path, RPM 4/07 bug 988
    int indent = (maxdepth-depth)*2; // set the indent based on how deep we are, RPM 4/07 bug 988

    id.tc_number = tc;  // mark id as printed

    /* --- first, count all direct augmentations of this id --- */
    /* --- next, construct the array of wme pointers and sort them --- */
    List<WmeImpl> list = new ArrayList<WmeImpl>();
    for (WmeImpl w=id.goalInfo != null ? id.goalInfo.getImpasseWmes() : null; w!=null; w=w.next)
        list.add(w);
    for (WmeImpl w=id.getInputWmes(); w!=null; w=w.next)
        list.add(w);
    for (Slot s = id.slots; s != null; s = s.next) {
        for (WmeImpl w=s.getWmes(); w!=null; w=w.next)
            list.add(w);
        for (WmeImpl w=s.getAcceptablePreferenceWmes(); w!=null; w=w.next)
            list.add(w);
    }
    Collections.sort(list, ATTRIBUTE_COMPARATOR);

    /* --- finally, print the sorted wmes and deallocate the array --- */

    // RPM 4/07 If this is a tree print, then for each wme in the list, print it and its children
    if(tree) {
        for (WmeImpl w : list) {
            printer.spaces(indent);
            if (internal) printer.print ("%s", w);
            else printer.print("%#s", w);

            if (depth>1) { // we're not done yet
                /* --- call this routine recursively --- */
                print_augs_of_id (w.attr, depth-1, maxdepth, internal, tree, tc);
                print_augs_of_id (w.value, depth-1, maxdepth, internal, tree, tc);
            }
        }
    // RPM 4/07 This is not a tree print, so for each wme in the list, print it
    // Then, after all wmes have been printed, print the children
    } else {
        int attr;
        final int num_attr = list.size();
        for (attr=0; attr < num_attr; attr++) {
            WmeImpl w = list.get(attr);
            printer.spaces(indent);

            if(internal) {
                printer.print("%s", w);
            } else {
                printer.print("(%s", id);

                // XML format of an <id> followed by a series of <wmes> each of which shares the original ID.
                // <id id="s1"><wme tag="123" attr="foo" attrtype="string" val="123" valtype="string"></wme><wme attr="bar" ...></wme></id>
                //xml_begin_tag(agnt, kWME_Id);
                //xml_att_val(agnt, kWME_Id, id);

                for (attr=0; attr < num_attr; attr++) {
                    w = list.get(attr);
                    neatly_print_wme_augmentation_of_id (w, indent);
                }
                
                //xml_end_tag(agnt, kWME_Id);

                printer.print (")\n");
            }
        }

        // If there is still depth left, recurse
        if (depth > 1) {
            for (attr=0; attr < num_attr; attr++) {
                WmeImpl w = list.get(attr);
                /* --- call this routine recursively --- */
                print_augs_of_id (w.attr, depth-1, maxdepth, internal, tree, tc);
                print_augs_of_id (w.value, depth-1, maxdepth, internal, tree, tc);
            }
        }
    }
}


}
