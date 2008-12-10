/*
 * Copyright (c) 2008  Dave Ray <daveray@gmail.com>
 *
 * Created on Oct 27, 2008
 */
package org.jsoar.debugger;

import java.util.ArrayList;
import java.util.Enumeration;
import java.util.List;

import javax.swing.tree.TreeNode;

import org.jdesktop.swingx.treetable.TreeTableNode;
import org.jsoar.kernel.memory.Wme;
import org.jsoar.kernel.symbols.Identifier;

import com.google.common.collect.Iterators;

/**
 * @author ray
 */
public class WorkingMemoryTreeNode implements TreeTableNode
{
    private WorkingMemoryTreeModel model;
    private WorkingMemoryTreeNode parent;
    private Wme wme;
    private Identifier valueId;
    
    private List<WorkingMemoryTreeNode> kids;
    
    /**
     * Construct a node for a WME
     * 
     * @param model The owning model
     * @param parent The parent node
     * @param wme The wme held by this node
     */
    public WorkingMemoryTreeNode(WorkingMemoryTreeModel model, WorkingMemoryTreeNode parent, Wme wme)
    {
        this.model = model;
        this.parent = parent;
        this.wme = wme;
        this.valueId = this.wme.getValue().asIdentifier();
    }

    /**
     * Construct a node for a root identifier
     * 
     * @param model The owning model
     * @param parent The parent node
     * @param valueId The root identifier
     */
    public WorkingMemoryTreeNode(WorkingMemoryTreeModel model, WorkingMemoryTreeNode parent, Identifier valueId)
    {
        this.model = model;
        this.parent = parent;
        this.valueId = valueId;
    }
    
    /**
     * Construct the root node for the tree
     * 
     * @param model the owning model
     */
    public WorkingMemoryTreeNode(WorkingMemoryTreeModel model)
    {
        this.model = model;
        this.kids = new ArrayList<WorkingMemoryTreeNode>();
    }
    
    /**
     * @return the wme, or <code>null</code> if not a WME node
     */
    public Wme getWme()
    {
        return wme;
    }

    /**
     * @return the root identifier, or value of WME in this node.
     */
    public Identifier getValueId()
    {
        return valueId;
    }

    void addChild(WorkingMemoryTreeNode kid)
    {
        this.kids.add(kid);
    }

    private void updateChildren()
    {
        if(kids != null)
        {
            return;
        }
        
        kids = model.getChildWmes(this, valueId);
    }

    /* (non-Javadoc)
     * @see javax.swing.tree.TreeNode#children()
     */
    @SuppressWarnings("unchecked")
    @Override
    public Enumeration children()
    {
        updateChildren();
        return Iterators.asEnumeration(kids.iterator());
    }

    /* (non-Javadoc)
     * @see javax.swing.tree.TreeNode#getAllowsChildren()
     */
    @Override
    public boolean getAllowsChildren()
    {
        return !isLeaf();
    }

    /* (non-Javadoc)
     * @see javax.swing.tree.TreeNode#getChildAt(int)
     */
    @Override
    public TreeTableNode getChildAt(int n)
    {
        updateChildren();
        return kids.get(n);
    }

    /* (non-Javadoc)
     * @see javax.swing.tree.TreeNode#getChildCount()
     */
    @Override
    public int getChildCount()
    {
        updateChildren();
        return kids.size();
    }

    /* (non-Javadoc)
     * @see javax.swing.tree.TreeNode#getIndex(javax.swing.tree.TreeNode)
     */
    @Override
    public int getIndex(TreeNode node)
    {
        updateChildren();
        return kids.indexOf(node);
    }

    /* (non-Javadoc)
     * @see javax.swing.tree.TreeNode#getParent()
     */
    @Override
    public TreeTableNode getParent()
    {
        return parent;
    }

    /* (non-Javadoc)
     * @see javax.swing.tree.TreeNode#isLeaf()
     */
    @Override
    public boolean isLeaf()
    {
        return parent != null && valueId == null;
    }

    /* (non-Javadoc)
     * @see org.jdesktop.swingx.treetable.TreeTableNode#getColumnCount()
     */
    @Override
    public int getColumnCount()
    {
        return 3;
    }

    /* (non-Javadoc)
     * @see org.jdesktop.swingx.treetable.TreeTableNode#getUserObject()
     */
    @Override
    public Object getUserObject()
    {
        return wme != null ? wme : valueId;
    }

    /* (non-Javadoc)
     * @see org.jdesktop.swingx.treetable.TreeTableNode#getValueAt(int)
     */
    @Override
    public Object getValueAt(int column)
    {
        switch(column)
        {
        case 0: return wme != null ? wme : valueId;
        case 1: return wme != null ? wme.getValue() : "";
        case 2: return wme != null ? wme.getTimetag() : "";
        }
        return null;
    }

    /* (non-Javadoc)
     * @see org.jdesktop.swingx.treetable.TreeTableNode#isEditable(int)
     */
    @Override
    public boolean isEditable(int column)
    {
        return false;
    }

    /* (non-Javadoc)
     * @see org.jdesktop.swingx.treetable.TreeTableNode#setUserObject(java.lang.Object)
     */
    @Override
    public void setUserObject(Object arg0)
    {
    }

    /* (non-Javadoc)
     * @see org.jdesktop.swingx.treetable.TreeTableNode#setValueAt(java.lang.Object, int)
     */
    @Override
    public void setValueAt(Object arg0, int arg1)
    {
    }

}
