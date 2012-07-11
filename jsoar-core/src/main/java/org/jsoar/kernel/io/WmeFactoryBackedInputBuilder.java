/*
 * Copyright (c) 2012 Dave Ray <daveray@gmail.com>
 *
 * Created on July 10, 2012
 */
package org.jsoar.kernel.io;

import java.util.HashMap;
import java.util.Map;

import org.jsoar.kernel.memory.WmeFactory;
import org.jsoar.kernel.symbols.Identifier;
import org.jsoar.kernel.symbols.SymbolFactory;
import org.jsoar.kernel.symbols.Symbols;

/**
 * A builder object for constructing input link structures. 
 * 
 * <p>For example, to build the following input-link structure:
 * <pre>{@code
 * ^input-link
 *    ^location (L1)
 *       ^x 3
 *       ^y 4
 *       ^name hello
 *    ^location
 *       ^x 5
 *       ^y 6
 *       ^name goodbye
 *       ^|a link| <L1>
 *    ^99  |integer attribute|
 *    ^3.0 |double attribute|
 *    ^flag
 * }</pre>
 *
 * you would use the following builder code:
 * 
 * <pre>{@code                
 *   InputBuilder2 builder = InputBuilder2.create(agent.io);
 *   builder.push("location").markId("L1").
 *               add("x", 3).
 *               add("y", 4).
 *               add("name", "hello").
 *               pop().
 *           push("location").markId("L2").
 *               add("x", 5).
 *               add("y", 6).
 *               add("name", "goodbye").
 *               link("a link", "L1").
 *               pop().
 *           add(99, "integer attribute").
 *           add(3.0, "double attribute").
 *           add("flag", null);
 * }</pre>
 * 
 * @author ray
 */
public class WmeFactoryBackedInputBuilder<T>
{
    public final WmeFactory<T> wmeFactory;
    public final Identifier id;
    private final WmeFactoryBackedInputBuilder<T> parent;
    private final Map<String, Identifier> idMap;
    private final Map<String, T> wmeMap;
    
    /**
     * Construct a new builder object that starts building WMEs at the given
     * identifier.
     * 
     * @param wmeFactory The I/O interface
     * @param root The root to start building at
     * @return a new builder object
     */
    public static<U> WmeFactoryBackedInputBuilder<U> create(WmeFactory<U> wmeFactory, Identifier root)
    {
        return new WmeFactoryBackedInputBuilder<U>(wmeFactory, null, root, new HashMap<String, Identifier>(), new HashMap<String, U>());
    }
    
    /**
     * Construct a new builder object that starts building WMEs at the
     * input-link identifier, i.e. "I2".
     * 
     * @param wmeFactory The I/O interface
     * @return a new builder object
     */
    public static WmeFactoryBackedInputBuilder<?> create(WmeFactory<?> wmeFactory)
    {
        return create(wmeFactory, wmeFactory.getSymbols().findIdentifier('I', 2));
    }
    
    private WmeFactoryBackedInputBuilder(WmeFactory<T> wmeFactory, WmeFactoryBackedInputBuilder<T> parent, Identifier id, 
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
    public WmeFactoryBackedInputBuilder<T> add(Object attr, Object value)
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
    public WmeFactoryBackedInputBuilder<T> push(Object attr)
    {
        final Identifier newId = wmeFactory.getSymbols().createIdentifier(Symbols.getFirstLetter(attr));
        final SymbolFactory syms = wmeFactory.getSymbols();
        wmeMap.put(null, wmeFactory.addWme(id, Symbols.create(syms, attr), Symbols.create(syms, newId)));
        return new WmeFactoryBackedInputBuilder<T>(wmeFactory, this, newId, idMap, wmeMap);
    }
    
    /**
     * Mark the current identifier with the given name
     * 
     * @param name The name of the identifier
     * @return this builder
     */
    public WmeFactoryBackedInputBuilder<T> markId(String name)
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
    public WmeFactoryBackedInputBuilder<T> markWme(String name)
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
    public WmeFactoryBackedInputBuilder<T> pop()
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
    public WmeFactoryBackedInputBuilder<T> top()
    {
        if(parent == null)
        {
            return this;
        }
        WmeFactoryBackedInputBuilder<T> p = parent;
        while(p.parent != null)
        {
            p = p.parent;
        }
        return p;
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
    public WmeFactoryBackedInputBuilder<T>jump(String idName)
    {
        Identifier newId = idMap.get(idName);
        if(newId == null)
        {
            throw new IllegalArgumentException("No id with name '" + idName + "'");
        }
        return new WmeFactoryBackedInputBuilder<T>(wmeFactory, this, newId, idMap, wmeMap);
    }
}
