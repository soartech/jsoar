/*
 * Copyright (c) 2008  Dave Ray <daveray@gmail.com>
 *
 * Created on Oct 27, 2008
 */
package org.jsoar.debugger;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.Callable;

import org.jdesktop.swingx.treetable.DefaultTreeTableModel;
import org.jsoar.kernel.memory.Wme;
import org.jsoar.kernel.symbols.Identifier;
import org.jsoar.runtime.ThreadedAgentProxy;

import com.google.common.collect.Iterators;

/**
 * @author ray
 */
public class WorkingMemoryTreeModel extends DefaultTreeTableModel
{
    private final ThreadedAgentProxy proxy;

    /**
     * Construct a new tree model
     * 
     * @param proxy agenr proxy object
     * @param roots list of roots for the tree
     */
    public WorkingMemoryTreeModel(ThreadedAgentProxy proxy, List<Identifier> roots)
    {
        this.proxy = proxy;
        
        setColumnIdentifiers(Arrays.asList("Id/Attr", "Value", "Timetag"));
        
        WorkingMemoryTreeNode root = new WorkingMemoryTreeNode(this);
        for(Identifier id : roots)
        {
            root.addChild(new WorkingMemoryTreeNode(this, root, id));
        }
        
        this.setRoot(root);
    }

    /**
     * Get the child wmes for a particular identifier as a list of tree nodes
     * 
     * @param parent Parent tree node
     * @param valueId parent identifier
     * @return list of tree nodes, one for each child wme of the identifier
     */
    public List<WorkingMemoryTreeNode> getChildWmes(WorkingMemoryTreeNode parent, final Identifier valueId)
    {
        Callable<List<Wme>> callable = new Callable<List<Wme>>() {

            public List<Wme> call() throws Exception
            {
                List<Wme> wmes = new ArrayList<Wme>();
                Iterators.addAll(wmes, valueId.getWmes());
                return wmes;
            }};
            
        List<Wme> wmes = proxy.execute(callable);
        List<WorkingMemoryTreeNode> nodes = new ArrayList<WorkingMemoryTreeNode>(wmes.size());
        for(Wme w : wmes)
        {
            nodes.add(new WorkingMemoryTreeNode(this, parent, w));
        }
        return nodes;
    }

}
