
package org.jsoar.kernel.io.quick;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.regex.Pattern;

import org.jsoar.kernel.Agent;
import org.jsoar.kernel.events.AfterInitSoarEvent;
import org.jsoar.kernel.events.InputEvent;
import org.jsoar.kernel.io.InputOutput;
import org.jsoar.kernel.symbols.Identifier;
import org.jsoar.kernel.symbols.Symbol;
import org.jsoar.util.Arguments;
import org.jsoar.util.events.SoarEvent;
import org.jsoar.util.events.SoarEventListener;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Adapts the contents of a {@link QMemory} structure to Soar working memory
 * at a particular location, usually the root of the input-link. This class
 * is thread-safe, i.e. it is safe to call any of its methods from any thread.
 * 
 * <p>Basic usage:
 * 
 * <pre>
 * {@code
 * // Create a memory to act as the source. Any values we set in this object
 * // will appear on the agent's input-link 
 * QMemory memory = DefaultQMemory.create();
 * SoarQMemoryAdapter adapter = SoarQMemoryAdapter.attach(agent, memory);
 * 
 * // ... start running agent in this, or another thread ...
 * 
 * // Now make modifications to the memory
 * memory.setString("agent.info.name", "Patrick");
 * }
 * </pre>
 * 
 * <p>Note that the <b>source</b> {@link QMemory} for the adapter can be changed
 * at any time, more than one agent can share a source, and that more than one
 * adapter can be attached to an agent.
 * 
 * <p>In the event of an init-soar, this object will automatically reconstruct
 * its last working memory state.
 * 
 * <p>When you're done with an adapter, call {@link #detach()} to remove it. Any
 * WMEs it has created will be removed from the agent during the next input phase.
 * 
 * @author ray
 */
public class SoarQMemoryAdapter implements SoarEventListener, QMemoryListener
{
    private static final Logger LOG = LoggerFactory.getLogger(SoarQMemoryAdapter.class);
    private static final Pattern INDEX_PATTERN = Pattern.compile("\\[[^]]+\\]$");
    
    static String getNameFromPath(String path)
    {
        final String[] pathElements = path.split("\\.");
        return INDEX_PATTERN.matcher(pathElements[pathElements.length - 1]).replaceFirst("");
    }
    
    private static String getParentPath(String path)
    {
        final int ix = path.lastIndexOf('.');
        return (ix < 0) ? "" : path.substring(0, ix);
    }
    
    private static final Comparator<String> INCREASING_LENGTH_COMPARATOR = new Comparator<String>()
    {
        public int compare(String s1, String s2)
        {
            return s1.length() - s2.length();
        }
    };
    
    private static final Comparator<String> DECREASING_LENGTH_COMPARATOR = new Comparator<String>()
    {
        public int compare(String s1, String s2)
        {
            return s2.length() - s1.length();
        }
    };
    
    private final Object lock = new Object();
    private QMemory source;
    private boolean sourceChanged = false;
    private InputOutput io;
    private Identifier rootId;
    private SoarMemoryNode rootNode;
    
    private Map<String, SoarMemoryNode> memory = new HashMap<String, SoarMemoryNode>();
    
    /**
     * Attach the given source to the given agent. Each input cycle, the current contents
     * of the memory source will be put on the input-link.
     * 
     * @param agent The agent
     * @param source The source, or <code>null</code> if there is no initial source
     * @return The adapter object
     * @throws IllegalArgumentException if agent is <code>null</code>
     */
    public static SoarQMemoryAdapter attach(Agent agent, QMemory source)
    {
        Arguments.checkNotNull(agent, "agent");
        return attach(agent.getInputOutput(), null, source);
    }
    
    /**
     * Attach the given source to an agent's input at the given identifier. This is
     * a lower-level version of {@link #attach(Agent, QMemory)} which allows working
     * memory to be manipulated at a point other than the agent's input-link.
     * 
     * @param io An I/O interface
     * @param rootId The root identifier to attach to, or <code>null</code> to
     *     attach to the input-link directly.
     * @param source The source, or <code>null</code> if there is no initial source
     * @return New adapter object
     * @throws IllegalArgumentException if events or io are <code>null</code>
     */
    public static SoarQMemoryAdapter attach(InputOutput io, Identifier rootId, QMemory source)
    {
        SoarQMemoryAdapter adapter = new SoarQMemoryAdapter();
        adapter.setSource(source);
        adapter.initialize(io, rootId);
        return adapter;
    }
    
    /**
     * Create an uninitialized memory adapter. This is for advanced use only.
     * The attach() factory methods should be preferred.
     */
    public SoarQMemoryAdapter()
    {
    }
    
    /**
     * Initialize this memory adapter. This should only be called when the default
     * constructor is used.
     * 
     * @param io An io interface
     * @param rootId Root identifier to construct WMEs from. If {@code null}
     *     then the root of the input-link is used.
     */
    public void initialize(InputOutput io, Identifier rootId)
    {
        synchronized (lock)
        {
            Arguments.checkNotNull(io, "io");
            
            if(this.io != null)
            {
                throw new IllegalStateException("already initialized");
            }
            
            this.io = io;
            this.io.getEvents().addListener(InputEvent.class, this);
            this.io.getEvents().addListener(AfterInitSoarEvent.class, this);
            if(rootId != null)
            {
                this.rootId = rootId;
                this.rootNode = new SoarMemoryNode(rootId);
            }
        }
    }
    
    /**
     * Detach this adapter from the agent. During the next input cycle all remaining
     * WMEs created by this adapter will be removed.
     */
    public void detach()
    {
        synchronized (lock)
        {
            if(this.io != null)
            {
                this.io.getEvents().removeListener(null, this);
                removeAllWmes(io, memory);
            }
            this.io = null;
            this.memory = null;
        }
    }
    
    /**
     * @return the current memory source of this adapter
     */
    public QMemory getSource()
    {
        synchronized (lock)
        {
            return source;
        }
    }
    
    /**
     * Set the source for this adapter. If a previous adapter was set, any WMEs
     * created for that source are removed at the next input cycle.
     * 
     * @param source the new memory source for this adapter
     */
    public void setSource(QMemory source)
    {
        synchronized (lock)
        {
            if(this.source != null)
            {
                this.source.removeListener(this);
            }
            this.sourceChanged = this.sourceChanged | (this.source != source);
            this.source = source;
            if(this.source != null)
            {
                this.source.addListener(this);
            }
            
            if(sourceChanged && io != null)
            {
                io.asynchronousInputReady();
            }
        }
    }
    
    /**
     * Retrieve the {@link Symbol} value for the given path, or {@code null} if
     * no value exists for that path.
     * 
     * <p>This method is thread-safe
     * 
     * @param path a memory path
     * @return the value, or {@code null} if not found
     */
    public Symbol getValue(String path)
    {
        synchronized (lock)
        {
            final SoarMemoryNode node = memory.get(path);
            
            return node != null ? node.getValue() : null;
        }
        
    }
    
    private void synchronize()
    {
        // If there was any change in source, clear memory and start over
        if(sourceChanged)
        {
            removeAllWmes(io, memory);
            
            sourceChanged = false;
        }
        
        if(source == null)
        {
            return;
        }
        
        // If there's no root node, just use the root of the input-link.
        if(rootNode == null)
        {
            rootNode = new SoarMemoryNode(io.getInputLink());
        }
        
        final Set<String> oldPaths = new HashSet<String>(memory.keySet());
        
        final Set<String> newPaths = new HashSet<String>();
        final Set<String> newInternalPaths = new HashSet<String>();
        
        for(String fullPath : source.getPaths())
        {
            final String[] pathElements = fullPath.split("\\.");
            String tmp = "";
            for(int ix = 0; ix < pathElements.length; ++ix)
            {
                if(ix > 0)
                {
                    tmp += ".";
                }
                tmp += pathElements[ix];
                newPaths.add(tmp);
                oldPaths.remove(tmp);
                if(ix < pathElements.length - 1)
                {
                    newInternalPaths.add(tmp);
                }
            }
        }
        
        // Destroy WMEs associated with paths that are
        // no longer present in the memory structure
        // ... walk through in order of decreasing length
        // so that child WMEs are destroyed before their parents
        final List<String> oldPathsByLength = new ArrayList<String>(oldPaths);
        Collections.sort(oldPathsByLength, DECREASING_LENGTH_COMPARATOR);
        
        for(String path : oldPathsByLength)
        {
            final SoarMemoryNode oldNode = memory.remove(path);
            oldNode.remove(io);
        }
        
        final List<String> newPathsByLength = new ArrayList<String>(newPaths);
        Collections.sort(newPathsByLength, INCREASING_LENGTH_COMPARATOR);
        
        // walk through paths in order of increasing length
        // (so prefixes are always hit first)
        //
        // if a path is present as both a node with a value
        // and as an internal node (within a longer path),
        // the value should be ignored... this will happen
        // automatically by walking shorter paths first.
        //
        // if a path is present in new memory but not in old memory,
        // create a corresponding WME.
        //
        // if a path is present in new memory with a different type
        // than in old memory, destroy the old WME (and its children
        // if any) and create a new one.
        //
        // if a path is present in new memory with the same type
        // but a different value than in old memory, update the
        // existing WME.
        
        for(String path : newPathsByLength)
        {
            SoarMemoryNode oldNode = memory.get(path);
            
            SoarMemoryNode parentNode = memory.get(getParentPath(path));
            if(parentNode == null)
            {
                parentNode = rootNode;
            }
            
            final String name = getNameFromPath(path);
            
            final MemoryNode newNode;
            
            if(!newInternalPaths.contains(path))
            {
                newNode = ((DefaultQMemory) source).getNode(path);
            }
            else
            {
                newNode = new MemoryNode();
                newNode.clearValue();
            }
            
            if(oldNode == null)
            {
                oldNode = new SoarMemoryNode(name);
                memory.put(path, oldNode);
            }
            
            oldNode.setParentNode(parentNode);
            oldNode.synchronizeToMemoryNode(io, newNode);
        }
    }
    
    /**
     * 
     */
    private static void removeAllWmes(InputOutput io, Map<String, SoarMemoryNode> memory)
    {
        for(SoarMemoryNode node : memory.values())
        {
            node.getWME().remove();
        }
        memory.clear();
    }
    
    /*
     * (non-Javadoc)
     * 
     * @see org.jsoar.util.events.SoarEventListener#onEvent(org.jsoar.util.events.SoarEvent)
     */
    @Override
    public void onEvent(SoarEvent event)
    {
        synchronized (lock) // Lock this object
        {
            if(event instanceof InputEvent)
            {
                synchronized (source) // Lock the source
                {
                    synchronize();
                }
            }
            else if(event instanceof AfterInitSoarEvent)
            {
                resetAfterInitSoar();
            }
        }
    }
    
    /*
     * (non-Javadoc)
     * 
     * @see org.jsoar.kernel.io.quick.QMemoryListener#onQMemoryChanged()
     */
    @Override
    public void onQMemoryChanged()
    {
        io.asynchronousInputReady();
    }
    
    private void resetAfterInitSoar()
    {
        LOG.info("Repopulating after init-soar");
        
        memory.clear();
        sourceChanged = true;
        
        if(rootId != null)
        {
            final Identifier oldRootId = rootId;
            rootId = io.getSymbols().findOrCreateIdentifier(oldRootId.getNameLetter(), oldRootId.getNameNumber());
            if(rootId == null)
            {
                rootId = io.getSymbols().findOrCreateIdentifier(oldRootId.getNameLetter(), oldRootId.getNameNumber());
                LOG.warn("After init-soar, could not find custom root identifier " + oldRootId + ". Using " + rootId + ".");
            }
            rootNode = new SoarMemoryNode(rootId);
        }
        else
        {
            rootNode = null;
        }
        
    }
}
