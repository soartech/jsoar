
package org.jsoar.kernel.io.quick;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.CopyOnWriteArrayList;

import org.jsoar.kernel.memory.Wme;
import org.jsoar.kernel.symbols.Identifier;
import org.jsoar.util.Arguments;

/**
 * Default implementation of {@link QMemory} interface. This class contains
 * factory methods for constructing new {@link QMemory} objects.
 * 
 * @author ray
 */
public class DefaultQMemory implements QMemory
{
    /**
     * A fairly arbitrary limit to the depth that we'll search when building up
     * a QMemory for a particular id.  Simple way to avoid going around loops
     * and causing a stack overflow without a bunch of "visited" book-keeping.
     */
    static final int MAX_DEPTH = 20;
    
    private Map<String, MemoryNode> memory = new HashMap<String, MemoryNode>();
    private List<QMemoryListener> listeners = new CopyOnWriteArrayList<QMemoryListener>();
    
    /**
     * @return A new empty QMemory
     */
    public static QMemory create()
    {
        return new DefaultQMemory();
    }
    
    /**
     * Create a new QMemory initialized with the contents under the given identifier
     * 
     * @param id root identifier
     * @return new QMemory object
     */
    public static QMemory create(Identifier id)
    {
        return create(id, "", new DefaultQMemory(), 0);
    }
    
    private static QMemory create(Identifier id, String path, DefaultQMemory struct, int depth)
    {
        Arguments.checkNotNull(id, "id");
        
        if(depth > MAX_DEPTH)
        {
            return struct;
        }
        
        Iterator<Wme> it = id.getWmes();
        while(it.hasNext())
        {
            final Wme e = it.next();
            
            String childPath = e.getAttribute().toString();
            
            if (path != null && path.length() > 0)
            {
                childPath = path + "." + childPath;
            }
            
            if (struct.hasPath(childPath))
            {
                int ix = 0;
                while (struct.hasPath(childPath + "[" + ix + "]"))
                {
                    ++ix;
                }
                childPath += "[" + ix + "]";
            }
            
            MemoryNode childNode = MemoryNode.create(e.getValue());
            
            struct.setNode(childPath, childNode);
            
            if (!childNode.isLeaf())
            {
                Identifier childId = e.getValue().asIdentifier();
                assert (childId != null);
                create(childId, childPath, struct, depth + 1);
            }
        }
        
        return struct;
    }
    
    private DefaultQMemory()
    {
    }
    
    /* (non-Javadoc)
     * @see org.jsoar.kernel.io.quick.QMemory#hasPath(java.lang.String)
     */
    public synchronized boolean hasPath(String path)
    {
        return memory.containsKey(path);
    }

    /* (non-Javadoc)
     * @see org.jsoar.kernel.io.quick.QMemory#getDouble(java.lang.String)
     */
    public synchronized double getDouble(String path)
    {
        MemoryNode node = memory.get(path);
        return node != null ? node.getDoubleValue() : 0.0;
    }
    
    /* (non-Javadoc)
     * @see org.jsoar.kernel.io.quick.QMemory#getInteger(java.lang.String)
     */
    public synchronized long getInteger(String path)
    {
        MemoryNode node = memory.get(path);
        return node != null ? node.getIntValue() : 0;
    }
    
    /* (non-Javadoc)
     * @see org.jsoar.kernel.io.quick.QMemory#getString(java.lang.String)
     */
    public synchronized String getString(String path)
    {
        MemoryNode node = memory.get(path);
        return node != null ? node.getStringValue() : "";
    }
    
    /* (non-Javadoc)
     * @see org.jsoar.kernel.io.quick.QMemory#getPaths()
     */
    public synchronized Set<String> getPaths()
    {
        return new HashSet<String>(memory.keySet());
    }
    
    synchronized Set<String> getPaths(String prefix, boolean strip)
    {
        Set<String> paths = new HashSet<String>();
        for(String path : memory.keySet())
        {
            if(path.startsWith(prefix))
            {
                if(strip)
                {
                    paths.add(path.substring(prefix.length()));
                }
                else
                {
                    paths.add(path);
                }
            }
        }
        return paths;
    }
    
    /* (non-Javadoc)
     * @see org.jsoar.kernel.io.quick.QMemory#subMemory(java.lang.String)
     */
    @Override
    public QMemory subMemory(String prefix)
    {
        return new SubQMemory(this, prefix);
    }
    
    /* (non-Javadoc)
     * @see org.jsoar.kernel.io.quick.QMemory#setDouble(java.lang.String, double)
     */
    public synchronized void setDouble(String path, double doubleVal)
    {
        if(getNode(path).setDoubleValue(doubleVal))
        {
            fireChangeEvent();
        }
    }

    /* (non-Javadoc)
     * @see org.jsoar.kernel.io.quick.QMemory#setInteger(java.lang.String, int)
     */
    public synchronized void setInteger(String path, int intVal)
    {
        setInteger(path, (long)intVal);
    }

    /* (non-Javadoc)
     * @see org.jsoar.kernel.io.quick.QMemory#setInteger(java.lang.String, int)
     */
    public synchronized void setInteger(String path, long longVal)
    {
        if(getNode(path).setIntValue(longVal))
        {
            fireChangeEvent();
        }
    }

    /* (non-Javadoc)
     * @see org.jsoar.kernel.io.quick.QMemory#setString(java.lang.String, java.lang.String)
     */
    public synchronized void setString(String path, String strVal)
    {
        if(getNode(path).setStringValue(strVal))
        {
            fireChangeEvent();
        }
    }

    /* (non-Javadoc)
     * @see org.jsoar.kernel.io.quick.QMemory#clear(java.lang.String)
     */
    public synchronized void clear(String path)
    {
        getNode(path).clearValue();
        fireChangeEvent();
    }

    /* (non-Javadoc)
     * @see org.jsoar.kernel.io.quick.QMemory#remove(java.lang.String)
     */
    public synchronized void remove(String path)
    {
        removeNode(path);
        fireChangeEvent();
    }

    /* (non-Javadoc)
     * @see org.jsoar.kernel.io.quick.QMemory#addListener(org.jsoar.kernel.io.quick.QMemoryListener)
     */
    @Override
    public void addListener(QMemoryListener listener)
    {
        listeners.add(listener);
    }

    /* (non-Javadoc)
     * @see org.jsoar.kernel.io.quick.QMemory#removeListener(org.jsoar.kernel.io.quick.QMemoryListener)
     */
    @Override
    public void removeListener(QMemoryListener listener)
    {
        listeners.remove(listener);
    }

    private void fireChangeEvent()
    {
        for(QMemoryListener listener : listeners)
        {
            listener.onQMemoryChanged();
        }
    }
    
    private void setNode(String path, MemoryNode node)
    {
        memory.put(path, node);
    }
    
    MemoryNode getNode(String path)
    {
        MemoryNode node = memory.get(path);
        if (node == null)
        {
            node = new MemoryNode();
            memory.put(path, node);
        }
        return node;
    }
    
    private void removeNode(String path)
    {
        memory.remove(path);
        
        // Now remove any nodes with "path." as a prefix
        Iterator<String> it = memory.keySet().iterator();
        while(it.hasNext())
        {
            String fullPath = it.next();
            if(fullPath.startsWith(path))
            {
                if(fullPath.length() == path.length() || 
                   (fullPath.length() > path.length() && fullPath.charAt(path.length()) == '.'))
                {
                    it.remove();
                }
            }
        }
    }
}

