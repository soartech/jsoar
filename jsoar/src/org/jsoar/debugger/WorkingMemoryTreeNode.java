/*
 * Copyright (c) 2008  Dave Ray <daveray@gmail.com>
 *
 * Created on Oct 27, 2008
 */
package org.jsoar.debugger;

import org.jdesktop.swingx.treetable.DefaultMutableTreeTableNode;
import org.jsoar.kernel.memory.Wme;
import org.jsoar.kernel.symbols.Identifier;

/**
 * @author ray
 */
public class WorkingMemoryTreeNode extends DefaultMutableTreeTableNode
{
    private WorkingMemoryTreeModel model;
    private Wme wme;
    private Identifier valueId;
    
    private boolean updated = false;
    
    /**
     * Construct a node for a WME
     * 
     * @param model The owning model
     * @param parent The parent node
     * @param wme The wme held by this node
     */
    public WorkingMemoryTreeNode(WorkingMemoryTreeModel model, WorkingMemoryTreeNode parent, Wme wme)
    {
        super(wme);
        setParent(parent);
        this.model = model;
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
        super(valueId);
        setParent(parent);
        this.model = model;
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

    private void updateChildren()
    {
        if(updated || valueId == null)
        {
            return;
        }
        updated = true;
        
        model.getChildWmes(this, valueId);
    }


    /* (non-Javadoc)
     * @see javax.swing.tree.TreeNode#getChildCount()
     */
    @Override
    public int getChildCount()
    {
        updateChildren();
        return super.getChildCount();
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
