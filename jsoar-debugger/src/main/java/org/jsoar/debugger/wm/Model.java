/*
 * Copyright (c) 2010 Dave Ray <daveray@gmail.com>
 *
 * Created on Sep 20, 2010
 */
package org.jsoar.debugger.wm;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.ListIterator;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.Callable;

import org.jsoar.debugger.wm.WmeRow.Value;
import org.jsoar.kernel.memory.ContextVariableInfo;
import org.jsoar.kernel.memory.Wme;
import org.jsoar.kernel.symbols.Identifier;
import org.jsoar.kernel.symbols.Symbol;
import org.jsoar.runtime.CompletionHandler;
import org.jsoar.runtime.ThreadedAgent;

import com.google.common.collect.HashMultimap;
import com.google.common.collect.Multimap;

/**
 * @author ray
 */
class Model
{
    final Object lock;
    final ThreadedAgent agent;
    final Map<Object, RootRow> roots = new HashMap<Object, RootRow>();
    final ArrayList<Row> rows = new ArrayList<Row>();
    final Multimap<Wme, WmeRow.Value> wmeToRowValues = HashMultimap.create();
    long ts = 0;
    
    public Model(ThreadedAgent agent, Object lock)
    {
        this.agent = agent;
        this.lock = lock;
    }
    
    public boolean isNew(WmeRow.Value v)
    {
        return v.ts == ts;
    }
    
    public boolean hasRoot(Object id)
    {
        synchronized(lock)
        {
            return roots.containsKey(id);
        }
    }
    
    public boolean isInputLink(Identifier id) { return agent.getInputOutput().getInputLink() == id; }
    public boolean isOutputLink(Identifier id) { return agent.getInputOutput().getOutputLink() == id; }
    
    public void addRoot(Object id, CompletionHandler<Void> finish)
    {
        synchronized(lock)
        {
            if(roots.containsKey(id))
            {
                
            }
            else
            {
                ts++;
                expandId(id, null, finish);
            }
        }
    }
    
    public boolean removeRoot(Object id, CompletionHandler<Void> finish)
    {
        synchronized(lock)
        {
            final RootRow root = roots.remove(id);
            if(root != null)
            {
                ts++;
                removeRootRow(root, false);
                if(finish != null)
                {
                    finish.finish(null);
                }
            }
            return root != null;
        }
    }

    private void removeRootRow(final RootRow root, boolean onlyChildren)
    {
        final ListIterator<Row> it = rows.listIterator(onlyChildren ? root.row + 1 : root.row);
        while(it.hasNext())
        {
            final Row row = it.next();
            if(row.root == root)
            {
                it.remove();
                cleanupRow(row);
            }
            else
            {
                row.row = it.previousIndex();
            }
        }
    }
    
    public void expandRow(WmeRow.Value v, CompletionHandler<Void> finish)
    {
        synchronized(lock)
        {
            if(v.expanded)
            {
                return;
            }
            final Identifier valueId = v.wme.getValue().asIdentifier();
            if(valueId == null)
            {
                return;
            }
            expandId(valueId, v, finish);
            v.expanded = true;
        }
    }
    
    private void expandIdInternalHelper(Identifier id, RootRow root, WmeRow.Value parent)
    {
        if(id == null)
        {
            return;
        }
        
        for(Iterator<Wme> it = id.getWmes(); it.hasNext();)
        {
            final Wme wme = it.next();
            final Identifier wmeId = wme.getIdentifier();
            final Symbol wmeAttr = wme.getAttribute();
            WmeRow newRow = parent != null ? parent.children.get(wmeAttr) : root.children.get(wmeAttr);
            
            if(newRow == null)
            {
                newRow = parent != null ? parent.addChild(root, wmeId, wmeAttr) : root.addChild(wmeId, wmeAttr);
                newRow.row = (parent != null ? parent.row.row : root.row) + 1;
                rows.add(newRow.row, newRow);
                
                for(int i = newRow.row + 1; i < rows.size(); i++)
                {
                    final Row followingRow = rows.get(i);
                    followingRow.row = i;
                }
            }

            final Value existingValue = newRow.getValue(wme);
            if(existingValue == null)
            {
                wmeToRowValues.put(wme, newRow.addValue(ts, wme));
            }
            else if(existingValue.expanded)
            {
                expandIdInternalHelper(wme.getValue().asIdentifier(), root, existingValue);
            }
        }
    }
    
    private void expandIdInternal(final Object id, final WmeRow.Value parent)
    {
        final RootRow root;
        if(parent == null)
        {
            if(!roots.containsKey(id))
            {
                root = new RootRow(ts, id, rootIdGetter(id));
                
                roots.put(id, root);
                rows.add(0, root);
                if(parent != null)
                {
                    parent.row.row++;
                }
            }
            else
            {
                root = roots.get(id);
            }
        }
        else
        {
            root = parent.row.root;
        }
        
        expandIdInternalHelper(id instanceof Identifier ? (Identifier) id : root.getId(), root, parent);
        
        for(int i = 0; i < rows.size(); i++)
        {
            rows.get(i).row = i;
        }
        assert rowIndexesAreValid();
    }
    
    private void expandId(final Object id, final WmeRow.Value parent, CompletionHandler<Void> finish)
    {
        final Callable<Void> start = new Callable<Void>()
        {
            @Override
            public Void call() throws Exception
            {
                synchronized(lock)
                {
                    expandIdInternal(id, parent);
                }
                return null;
            }
        };
        agent.execute(start, finish);
    }

    
    private void removeRowAndChildren(WmeRow row)
    {
        synchronized(lock)
        {
            final ListIterator<Row> it = rows.listIterator(row.row);
            cleanupRow(row);
            it.next();
            it.remove();
            while(it.hasNext())
            {
                final Row subRow = it.next();
                if(subRow.level > row.level)
                {
                    cleanupRow(subRow);
                    it.remove();
                }
                else
                {
                    subRow.row = it.previousIndex();
                    break;
                }
            }
            
            fixTrailingRowIndexes(it);
        }
    }
    
    public void collapseRow(WmeRow.Value value, CompletionHandler<Void> finish)
    {
        synchronized(lock)
        {
            if(!value.expanded)
            {
                return;
            }
            
            final ListIterator<Row> it = rows.listIterator(value.row.row + 1);
            boolean inSubRow = false;
            while(it.hasNext())
            {
                final Row subRow = it.next();
                final WmeRow asWme = subRow.asWme();
                if(asWme == null)
                {
                    subRow.row = it.previousIndex();
                    break;
                }
                else if(asWme.id == value.wme.getValue() && subRow.level == value.row.level + 1)
                {
                    it.remove();
                    cleanupRow(subRow);
                    inSubRow = true;
                }
                else if(inSubRow && subRow.level > value.row.level + 1)
                {
                    cleanupRow(subRow);
                    it.remove();
                }
                else if(inSubRow && subRow.level == value.row.level + 1)
                {
                    inSubRow = false;
                    subRow.row = it.previousIndex();
                }
                else if(subRow.level <= value.row.level)
                {
                    subRow.row = it.previousIndex();
                    break;
                }
                else
                {
                    subRow.row = it.previousIndex();
                }
            }
            
            fixTrailingRowIndexes(it);
            value.expanded = false;
            if(finish != null)
            {
                finish.finish(null);
            }
        }
    }
    
    public void expandOrCollapseRow(WmeRow row, CompletionHandler<Void> finish)
    {
        synchronized(lock)
        {
            boolean expanded = false;
            for(WmeRow.Value v : row.values)
            {
                if(v.expanded)
                {
                    expanded = true;
                    break;
                }
            }
            
            if(expanded)
            {
                for(WmeRow.Value v : row.values)
                {
                    collapseRow(v, finish);
                }
            }
            else
            {
                for(WmeRow.Value v : row.values)
                {
                    expandRow(v, finish);
                }
            }
        }
    }
    
    private void cleanupRow(Row row)
    {
        synchronized(lock)
        {
            final WmeRow wmeRow = row.asWme();
            if(wmeRow != null)
            {
                if(wmeRow.parent != null)
                {
                    wmeRow.parent.removeChild(wmeRow);
                }
                else
                {
                    wmeRow.root.removeChild(wmeRow);
                }
                
                for(WmeRow.Value value : wmeRow.values)
                {
                    wmeToRowValues.remove(value.wme, value);
                }
            }
        }
    }
    
    private void fixTrailingRowIndexes(final ListIterator<Row> it)
    {
        while(it.hasNext())
        {
            final Row trailingRow = it.next();
            trailingRow.row = it.previousIndex();
        }
        
        assert rowIndexesAreValid();
    }
    
    private void updateRemovedWmes()
    {
        // For all removed WMEs, remove corresponding rows and sub-rows
        final Set<Wme> removedWmes = new HashSet<Wme>();
        for(Wme wme : wmeToRowValues.keys())
        {
            if(!agent.getAgent().isWmeInRete(wme))
            {
                removedWmes.add(wme);
            }
        }
        
        for(Wme wme : removedWmes)
        {
            final Collection<WmeRow.Value> values = wmeToRowValues.removeAll(wme);
            for(WmeRow.Value value : values)
            {
                final WmeRow containingRow = value.row;
                if(containingRow.values.size() == 1)
                {
                    removeRowAndChildren(containingRow);
                }
                else
                {
                    collapseRow(value, null);
                    value.row.values.remove(value);
                }
            }
        }
    }
    
    private void updateWmeChildren(Wme wme)
    {
        for(WmeRow.Value rv : new ArrayList<WmeRow.Value>(wmeToRowValues.get(wme)))
        {
            if(rv.expanded)
            {
                expandIdInternal(rv.wme.getValue().asIdentifier(), rv);
            }
        }
    }
    
    private void updateAddedWmes()
    {
        // For each remaining WME if it's value is an id and it's marked
        // as expanded, re-request the sub-wmes and add any new ones that
        // are missing from the tree.
        final Set<Wme> currentWmes = new HashSet<Wme>(wmeToRowValues.keys());
        for(Wme wme : currentWmes)
        {
            final Identifier valueId = wme.getValue().asIdentifier();
            if(valueId != null)
            {
                updateWmeChildren(wme);
            }
        }
        for(RootRow root : roots.values())
        {
            if(root.update(ts))
            {
                removeRootRow(root, true);
            }
            expandIdInternal(root.key, null);
        }
    }
    
    public void update(CompletionHandler<Void> finish)
    {
        final Callable<Void> begin = new Callable<Void>(){

            @Override
            public Void call() throws Exception
            {
                synchronized(lock)
                {
                    ts++;
                    updateRemovedWmes();
                    updateAddedWmes();
                }
                return null;
            }
        };
        agent.execute(begin, finish);
    }    
    
    private boolean rowIndexesAreValid()
    {
        for(int i = 0; i < rows.size(); ++i)
        {
            if(rows.get(i).row != i)
            {
                return false;
            }
        }
        return true;
    }
    
    private Callable<Identifier> rootIdGetter(final Object var)
    {
        if(var instanceof Identifier)
        {
            return new Callable<Identifier>() {
                @Override
                public Identifier call() throws Exception
                {
                    return (Identifier) var;
                }
            };
        }
        else
        {
            return new Callable<Identifier>() {
    
                @Override
                public Identifier call() throws Exception
                {
                    final ContextVariableInfo info = agent.getAgent().getContextVariableInfo(var.toString());
                    final Symbol value = info.getValue();
                    return value != null ? value.asIdentifier() : null;
                }
            };
        }
    }
}
