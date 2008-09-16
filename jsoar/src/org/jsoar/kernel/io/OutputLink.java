/*
 * Copyright (c) 2008  Dave Ray <daveray@gmail.com>
 *
 * Created on Sep 15, 2008
 */
package org.jsoar.kernel.io;

import java.util.LinkedList;

import org.jsoar.kernel.memory.Wme;
import org.jsoar.kernel.symbols.Identifier;
import org.jsoar.util.AsListItem;

/**
 * io.h:184:output_link
 * 
 * @author ray
 */
public class OutputLink
{
    final AsListItem<OutputLink> next_prev = new AsListItem<OutputLink>(this); /* dll of all existing links */
    OutputLinkStatus status = OutputLinkStatus.NEW_OL_STATUS;                             /* current xxx_OL_STATUS */
    Wme link_wme;                           /* points to the output link wme */
    LinkedList<Identifier> ids_in_tc = new LinkedList<Identifier>(); /* ids in TC(link) */
    
    // TODO soar_callback *cb;                       /* corresponding output function */

    public OutputLink(Wme link_wme)
    {
        this.link_wme = link_wme;
        this.link_wme.wme_add_ref();
        this.link_wme.output_link = this;
    }
}
