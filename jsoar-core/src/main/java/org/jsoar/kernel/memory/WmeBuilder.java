/*
 * Copyright (c) 2012 Dave Ray <daveray@gmail.com>
 *
 * Created on July 10, 2012
 */
package org.jsoar.kernel.memory;

import java.util.HashMap;
import java.util.Map;

import org.jsoar.kernel.symbols.Identifier;
import org.jsoar.kernel.symbols.SymbolFactory;
import org.jsoar.kernel.symbols.Symbols;

/**
 * A builder object for constructing wme structures. Similar to
 * {@link org.jsoar.kernel.io.InputBuilder InputBuilder} but does not
 * add WMEs directly to the input-link. Instead a wme structure is built
 * and the root identifier is returned by {@link #topId()}.
 * 
 * @author chris.kawatsu
 */
public class WmeBuilder<T>
{
    private final WmeFactory<T> wmeFactory;
    private final Identifier id;
    private final WmeBuilder<T> parent;
    private final Map<String, Identifier> idMap;
    private final Map<String, T> wmeMap;
    
    /**
     * Construct a new builder object that starts building WMEs at the given
     * identifier name. Use {@link #topId()} to get a reference to the new 
     * identifier.<br><br>
     * 
     * <i>Note: WmeBuilder objects are immutable.</i>
     * 
     * @param wmeFactory The wmeFactory
     * @param rootName The name of the top wme for this builder
     * @return a new builder object
     */
    public static<U> WmeBuilder<U> create(WmeFactory<U> wmeFactory, String rootName)
    {
        return new WmeBuilder<U>(wmeFactory, null, rootName, new HashMap<String, Identifier>(), new HashMap<String, U>());
    }
    
    /**
     * Construct a new builder object that starts building WMEs at the given
     * identifier. <br><br>
     * 
     * <i>Note: WmeBuilder objects are immutable.</i>
     * 
     * @param wmeFactory The wmeFactory
     * @param root The root to start building at
     * @return a new builder object
     */
    public static<U> WmeBuilder<U> create(WmeFactory<U> wmeFactory, Identifier root)
    {
        return new WmeBuilder<U>(wmeFactory, null, root, new HashMap<String, Identifier>(), new HashMap<String, U>());
    }
    
    private WmeBuilder(WmeFactory<T> wmeFactory, WmeBuilder<T> parent, String rootAttr, 
    		Map<String, Identifier> idMap, Map<String, T> wmeMap)
    {
    	final Identifier newId = wmeFactory.getSymbols().createIdentifier(Symbols.getFirstLetter(rootAttr));
    	this.wmeFactory = wmeFactory;
    	this.parent = parent;
    	this.id = newId;
    	this.idMap = idMap;
    	this.wmeMap = wmeMap;
    }
    
    private WmeBuilder(WmeFactory<T> wmeFactory, WmeBuilder<T> parent, Identifier id, 
    		Map<String, Identifier> idMap, Map<String, T> wmeMap)
    {
    	this.wmeFactory = wmeFactory;
    	this.parent = parent;
    	this.id = id;
    	this.idMap = idMap;
    	this.wmeMap = wmeMap;
    }
    
    /**
     * Construct a new WME from the current id with the given attribute and
     * value. The symbols for the attribute and value are constructed 
     * according to the rules of {@link Symbols#create(org.jsoar.kernel.symbols.SymbolFactory, Object)}.
     * 
     * <p>This method does not change the current identifier.
     * 
     * @param attr the attribute
     * @param value the value
     * @return this builder
     */
    public WmeBuilder<T> add(Object attr, Object value)
    {
    	final SymbolFactory syms = wmeFactory.getSymbols();
        wmeMap.put(null, wmeFactory.addWme(id, Symbols.create(syms, attr), Symbols.create(syms, value)));
        return this;
    }
    
    /**
     * Construct a new object WME from the current id with the given attribute
     * and a newly generated identifier. The symbols for the attribute 
     * are constructed according to the rules of 
     * {@link Symbols#create(org.jsoar.kernel.symbols.SymbolFactory, Object)}.
     * 
     * <p>This method returns a new InputBuilder2 rooted at the newly created
     * identifier. Use {@link #pop()} to return to the current identifier.
     * 
     * @param attr the attribute
     * @return new builder rooted at a new identifier
     */
    public WmeBuilder<T> push(Object attr)
    {
        final Identifier newId = wmeFactory.getSymbols().createIdentifier(Symbols.getFirstLetter(attr));
        final SymbolFactory syms = wmeFactory.getSymbols();
        wmeMap.put(null, wmeFactory.addWme(id, Symbols.create(syms, attr), Symbols.create(syms, newId)));
        return new WmeBuilder<T>(wmeFactory, this, newId, idMap, wmeMap);
    }
    
    /**
     * Mark the current identifier with the given name
     * 
     * @param name The name of the identifier
     * @return this builder
     */
    public WmeBuilder<T> markId(String name)
    {
        idMap.put(name, id);
        return this;
    }
    
    /**
     * Find a identifier previously marked with {@link #markId(String)}.
     * 
     * @param name The name of the identifier
     * @return the identifier, or <code>null</code> if not found
     */
    public Identifier getId(String name)
    {
        return idMap.get(name);
    }
    
    /**
     * Mark the most recently created WME with the given name.
     * 
     * @param name The name of the WME
     * @return this builder
     */
    public WmeBuilder<T> markWme(String name)
    {
        wmeMap.put(name, wmeMap.get(null));
        return this;
    }
    
    /**
     * Find a WME previously marked with {@link #markWme(String)}
     * 
     * @param name The name of the WME, or <code>null</code> for the most
     *      recently created WME
     * @return the WME, or <code>null</code> if not found
     */
    public T getWme(String name)
    {
        return wmeMap.get(name);
    }
    
    /**
     * Pop up one level in the builder stack, i.e. return to the state of the
     * builder before the most recent call to {@link #push(Object)}.
     * 
     * @return parent builder
     * @throws IllegalStateException if the builder stack is empty
     */
    public WmeBuilder<T> pop()
    {
        if(parent == null)
        {
            throw new IllegalStateException("Can't pop");
        }
        return parent;
    }
    
    /**
     * Return back to the top of the builder stack.
     * 
     * @return builder at the top of the stack
     */
    public WmeBuilder<T> top()
    {
        if(parent == null)
        {
            return this;
        }
        WmeBuilder<T> p = parent;
        while(p.parent != null)
        {
            p = p.parent;
        }
        return p;
    }
    
    /**
     * Return the root identifier of the top of the builder stack.
     * 
     * @return identifier at the top of the stack
     */
    public Identifier topId() {
    	return top().id;
    }
    
    /**
     * Jump to start building WMEs at an identifier previously marked with
     * {@link #markId(String)}.
     * 
     * <p>This method, like {@link #push(Object)}, adds to the builder stack.
     * 
     * @param idName The name of the identifier
     * @return builder rooted at the named identifier
     * @throws IllegalArgumentException if no identifier with the given name
     *      exists.
     */
    public WmeBuilder<T>jump(String idName)
    {
        Identifier newId = idMap.get(idName);
        if(newId == null)
        {
            throw new IllegalArgumentException("No id with name '" + idName + "'");
        }
        return new WmeBuilder<T>(wmeFactory, this, newId, idMap, wmeMap);
    }
}
