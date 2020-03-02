/*
 * Copyright (c) 2008  Dave Ray <daveray@gmail.com>
 *
 * Created on Dec 7, 2008
 */
package org.jsoar.debugger.util;

import java.util.Enumeration;
import java.util.List;

import javax.swing.tree.TreeNode;

import org.jdesktop.swingx.treetable.TreeTableNode;

import com.google.common.collect.Iterators;

/**
 * @author ray
 */
public abstract class AbstractTreeTableNode implements TreeTableNode
{
    private final boolean allowsChildren;
    
    
    /**
     * @param allowsChildren
     */
    public AbstractTreeTableNode(boolean allowsChildren)
    {
        this.allowsChildren = allowsChildren;
    }

    protected abstract List<? extends TreeTableNode> getChildList();
    
    /* (non-Javadoc)
     * @see org.jdesktop.swingx.treetable.TreeTableNode#children()
     */
    @Override
    public Enumeration<? extends TreeTableNode> children()
    {
        return Iterators.asEnumeration(getChildList().iterator());
    }

    /* (non-Javadoc)
     * @see org.jdesktop.swingx.treetable.TreeTableNode#getChildAt(int)
     */
    @Override
    public TreeTableNode getChildAt(int index)
    {
        return getChildList().get(index);
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

    /* (non-Javadoc)
     * @see javax.swing.tree.TreeNode#getAllowsChildren()
     */
    @Override
    public boolean getAllowsChildren()
    {
        return allowsChildren;
    }

    /* (non-Javadoc)
     * @see javax.swing.tree.TreeNode#getChildCount()
     */
    @Override
    public int getChildCount()
    {
        return getChildList().size();
    }

    /* (non-Javadoc)
     * @see javax.swing.tree.TreeNode#getIndex(javax.swing.tree.TreeNode)
     */
    @Override
    public int getIndex(TreeNode node)
    {
        return getChildList().indexOf(node);
    }
}
