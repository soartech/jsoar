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
import java.util.List;
import java.util.ListIterator;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.Callable;

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
    final Object lock = new String("WorkingMemoryTreeLock");
    final ThreadedAgent agent;
    final Map<Identifier, RootRow> roots = new HashMap<Identifier, RootRow>();
    final ArrayList<Row> rows = new ArrayList<Row>();
    final Multimap<Wme, WmeRow.Value> wmeToRowValues = HashMultimap.create();
    
    public Model(ThreadedAgent agent)
    {
        this.agent = agent;
    }
    
    public boolean hasRoot(Identifier id)
    {
        synchronized(lock)
        {
            return roots.containsKey(id);
        }
    }
    
    public void addRoot(Identifier id, CompletionHandler<Void> finish)
    {
        synchronized(lock)
        {
            if(roots.containsKey(id))
            {
                
            }
            else
            {
                expandId(id, null, finish);
            }
        }
    }
    
    public boolean removeRoot(Identifier id, CompletionHandler<Void> finish)
    {
        synchronized(lock)
        {
            final RootRow root = roots.remove(id);
            if(root != null)
            {
                final ListIterator<Row> it = rows.listIterator(root.row);
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
                if(finish != null)
                {
                    finish.finish(null);
                }
            }
            return root != null;
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
    
    private void expandIdInternal(final Identifier id, final WmeRow.Value parent)
    {
        final RootRow root;
        final int insertAt;
        if(parent == null)
        {
            root = new RootRow(id);
            roots.put(id, root);
            rows.add(0, root);
            insertAt = 1;
        }
        else
        {
            root = parent.row.root;
            insertAt = parent.row.row + 1;
        }
        
        final List<WmeRow> newRows = new ArrayList<WmeRow>();
        final Map<Symbol, WmeRow> rowMap = new HashMap<Symbol, WmeRow>();
        for(Iterator<Wme> it = id.getWmes(); it.hasNext();)
        {
            final Wme wme = it.next();
            WmeRow newRow = rowMap.get(wme.getAttribute());
            if(newRow == null)
            {
                newRow = new WmeRow(root, parent, wme.getIdentifier(), wme.getAttribute());
                newRow.row = insertAt + newRows.size();
                newRows.add(newRow);
                rowMap.put(wme.getAttribute(), newRow);
            }
            final WmeRow.Value newRowValue = newRow.addValue(wme);
            wmeToRowValues.put(wme, newRowValue);
        }
        
        rows.addAll(insertAt, newRows);
        for(int i = insertAt + newRows.size(); i < rows.size(); i++)
        {
            rows.get(i).row += newRows.size() + (parent != null ? 0 : 1);
        }
        assert rowIndexesAreValid();
    }
    
    private void expandId(final Identifier id, final WmeRow.Value parent, CompletionHandler<Void> finish)
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
                collapseRow(rv, null);
                expandIdInternal(rv.wme.getValue().asIdentifier(), rv);
                rv.expanded = true;
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
    }
    
    public void update(CompletionHandler<Void> finish)
    {
        final Callable<Void> begin = new Callable<Void>(){

            @Override
            public Void call() throws Exception
            {
                synchronized(lock)
                {
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
}
