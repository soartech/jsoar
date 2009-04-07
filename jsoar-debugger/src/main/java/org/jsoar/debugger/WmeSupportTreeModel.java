/*
 * Copyright (c) 2008  Dave Ray <daveray@gmail.com>
 *
 * Created on Dec 7, 2008
 */
package org.jsoar.debugger;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import org.jdesktop.swingx.treetable.DefaultTreeTableModel;
import org.jdesktop.swingx.treetable.TreeTableNode;
import org.jsoar.kernel.memory.Wme;
import org.jsoar.kernel.memory.WmeSupportInfo;
import org.jsoar.kernel.memory.WmeSupportInfo.Support;
import org.jsoar.util.AbstractTreeTableNode;

/**
 * @author ray
 */
public class WmeSupportTreeModel extends DefaultTreeTableModel
{
    private static enum Columns
    {
        Id {

            @Override
            public Object getValue(Wme wme)
            {
                return wme.getIdentifier();
            }}, 
        Attr {

            @Override
            public Object getValue(Wme wme)
            {
                return wme.getAttribute();
            }}, 
        Value {

            @Override
            public Object getValue(Wme wme)
            {
                return wme.getValue();
            }},
        Timetag {

            @Override
            public Object getValue(Wme wme)
            {
                return wme.getTimetag();
            }};
        
        public abstract Object getValue(Wme wme);
    }
    
    public WmeSupportTreeModel(WmeSupportInfo info)
    {
        setColumnIdentifiers(Arrays.asList("Id", "Attribute", "Value", "Timetag"));
        this.setRoot(new InfoNode(info));
    }
    
    private static class InfoNode extends AbstractTreeTableNode
    {
        private final WmeSupportInfo info;
        private final List<TreeTableNode> supports;
        
        public InfoNode(WmeSupportInfo info)
        {
            super(true);
            
            this.info = info;
            this.supports = new ArrayList<TreeTableNode>(info.getSupports().size());
            for(Support support : info.getSupports())
            {
                this.supports.add(new SourceNode(this, support));
            }
        }

        /* (non-Javadoc)
         * @see org.jsoar.util.AbstractTreeTableNode#getChildList()
         */
        @Override
        protected List<? extends TreeTableNode> getChildList()
        {
            return supports;
        }

        /* (non-Javadoc)
         * @see org.jdesktop.swingx.treetable.TreeTableNode#getColumnCount()
         */
        @Override
        public int getColumnCount()
        {
            return Columns.values().length;
        }

        /* (non-Javadoc)
         * @see org.jdesktop.swingx.treetable.TreeTableNode#getParent()
         */
        @Override
        public TreeTableNode getParent()
        {
            return null;
        }

        /* (non-Javadoc)
         * @see org.jdesktop.swingx.treetable.TreeTableNode#getUserObject()
         */
        @Override
        public Object getUserObject()
        {
            return info;
        }

        /* (non-Javadoc)
         * @see org.jdesktop.swingx.treetable.TreeTableNode#getValueAt(int)
         */
        @Override
        public Object getValueAt(int arg0)
        {
            return null;
        }

        /* (non-Javadoc)
         * @see javax.swing.tree.TreeNode#isLeaf()
         */
        @Override
        public boolean isLeaf()
        {
            return false;
        }
    }
    
    private static class SourceNode extends AbstractTreeTableNode
    {
        private TreeTableNode parent;
        private final Support support;
        private final List<TreeTableNode> wmes;
        
        public SourceNode(TreeTableNode parent, Support support)
        {
            super(true);
            
            this.parent = parent;
            this.support = support;
            this.wmes = new ArrayList<TreeTableNode>(this.support.getSourceWmes().size());
            for(Wme wme : this.support.getSourceWmes())
            {
                wmes.add(new WmeNode(this, wme));
            }
        }
        
        /* (non-Javadoc)
         * @see org.jsoar.util.AbstractTreeTableNode#getChildList()
         */
        @Override
        protected List<? extends TreeTableNode> getChildList()
        {
            return wmes;
        }

        /* (non-Javadoc)
         * @see org.jdesktop.swingx.treetable.TreeTableNode#getColumnCount()
         */
        @Override
        public int getColumnCount()
        {
            return Columns.values().length;
        }

        /* (non-Javadoc)
         * @see org.jdesktop.swingx.treetable.TreeTableNode#getParent()
         */
        @Override
        public TreeTableNode getParent()
        {
            return parent;
        }

        /* (non-Javadoc)
         * @see org.jdesktop.swingx.treetable.TreeTableNode#getUserObject()
         */
        @Override
        public Object getUserObject()
        {
            return support;
        }

        /* (non-Javadoc)
         * @see org.jdesktop.swingx.treetable.TreeTableNode#getValueAt(int)
         */
        @Override
        public Object getValueAt(int column)
        {
            return column == 0 ? support.getSource() : null;
        }

        /* (non-Javadoc)
         * @see javax.swing.tree.TreeNode#isLeaf()
         */
        @Override
        public boolean isLeaf()
        {
            return false;
        }
        
    }
    
    private static class WmeNode extends AbstractTreeTableNode
    {
        private final SourceNode parent;
        private final Wme wme;
        
        /**
         * @param sourceNode
         * @param wme
         */
        public WmeNode(SourceNode sourceNode, Wme wme)
        {
            super(false);
            
            this.parent = sourceNode;
            this.wme = wme;
        }

        
        /* (non-Javadoc)
         * @see org.jsoar.util.AbstractTreeTableNode#getChildList()
         */
        @Override
        protected List<? extends TreeTableNode> getChildList()
        {
            return Collections.emptyList();
        }

        /* (non-Javadoc)
         * @see javax.swing.tree.TreeNode#isLeaf()
         */
        @Override
        public boolean isLeaf()
        {
            return true;
        }

        /* (non-Javadoc)
         * @see org.jdesktop.swingx.treetable.TreeTableNode#getColumnCount()
         */
        @Override
        public int getColumnCount()
        {
            return Columns.values().length;
        }

        /* (non-Javadoc)
         * @see org.jdesktop.swingx.treetable.TreeTableNode#getParent()
         */
        @Override
        public TreeTableNode getParent()
        {
            return parent;
        }

        /* (non-Javadoc)
         * @see org.jdesktop.swingx.treetable.TreeTableNode#getUserObject()
         */
        @Override
        public Object getUserObject()
        {
            return wme;
        }

        /* (non-Javadoc)
         * @see org.jdesktop.swingx.treetable.TreeTableNode#getValueAt(int)
         */
        @Override
        public Object getValueAt(int column)
        {
            return Columns.values()[column].getValue(wme);
        }
    }
}
