/*
 * Copyright (c) 2008  Dave Ray <daveray@gmail.com>
 *
 * Created on Oct 15, 2008
 */
package org.jsoar.kernel.io;

import java.util.HashMap;
import java.util.Map;

import org.jsoar.kernel.memory.Wme;
import org.jsoar.kernel.symbols.Identifier;
import org.jsoar.kernel.symbols.Symbol;
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
 *   InputBuilder builder = InputBuilder.create(agent.io);
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
public class InputBuilder
{
    public final InputOutput io;
    public final Identifier id;
    private final InputBuilder parent;
    private final Map<String, Identifier> idMap;
    private final Map<String, Wme> wmeMap;
    
    public static Wme add(InputOutput io, Identifier id, Object attr, Object value)
    {
        final SymbolFactory syms = io.getSymbols();
        return io.addInputWme(id, Symbols.create(syms, attr), Symbols.create(syms, value));
    }
    
    public static void remove(InputOutput io, Wme wme)
    {
        io.removeInputWme(wme);
    }
    
    public static Wme update(InputOutput io, Wme wme, Object newValue)
    {
        return io.updateInputWme(wme, Symbols.create(io.getSymbols(), newValue));
    }
    
    /**
     * Construct a new builder object that starts building WMEs at the given
     * identifier.
     * 
     * @param io The I/O interface
     * @param root The root to start building at
     * @return a new builder object
     */
    public static InputBuilder create(InputOutput io, Identifier root)
    {
        return new InputBuilder(io, null, root, new HashMap<String, Identifier>(), new HashMap<String, Wme>());
    }
    
    /**
     * Construct a new builder object that starts building WMEs at the
     * input-link identifier, i.e. "I2".
     * 
     * @param io The I/O interface
     * @return a new builder object
     */
    public static InputBuilder create(InputOutput io)
    {
        return create(io, io.getInputLink());
    }
    
    private InputBuilder(InputOutput io, InputBuilder parent, Identifier id, 
                          Map<String, Identifier> idMap, Map<String, Wme> wmeMap)
    {
        this.io = io;
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
    public InputBuilder add(Object attr, Object value)
    {
        Symbol attrSym = Symbols.create(io.getSymbols(), attr);
        Symbol valueSym = Symbols.create(io.getSymbols(), value);
        wmeMap.put(null, io.addInputWme(id, attrSym, valueSym));
        return this;
    }
    
    /**
     * Construct a new object WME from the current id with the given attribute
     * and a newly generated identifier. The symbols for the attribute 
     * are constructed according to the rules of 
     * {@link Symbols#create(org.jsoar.kernel.symbols.SymbolFactory, Object)}.
     * 
     * <p>This method returns a new InputBuilder rooted at the newly created
     * identifier. Use {@link #pop()} to return to the current identifier.
     * 
     * @param attr the attribute
     * @return new builder rooted at a new identifier
     */
    public InputBuilder push(Object attr)
    {
        final Identifier newId = io.getSymbols().createIdentifier(Symbols.getFirstLetter(attr));
        Symbol attrSym = Symbols.create(io.getSymbols(), attr);
        wmeMap.put(null, io.addInputWme(id, attrSym, newId));
        return new InputBuilder(io, this, newId, idMap, wmeMap);
    }
    
    /**
     * Construct a link WME to an identifier previously marked with {@link #markId(String)}.
     * 
     * <p>This method does not change the current identifier
     * 
     * @param attr the attribute
     * @param target the name of the target identifier
     * @return this builder
     */
    public InputBuilder link(Object attr, String target)
    {
        Identifier targetId = idMap.get(target);
        if(targetId == null)
        {
            throw new IllegalArgumentException("Unknown target '" + target + "'");
        }
        Symbol attrSym = Symbols.create(io.getSymbols(), attr);
        wmeMap.put(null, io.addInputWme(id, attrSym, targetId));
        return this;
    }
    
    /**
     * Mark the current identifier with the given name
     * 
     * @param name The name of the identifier
     * @return this builder
     */
    public InputBuilder markId(String name)
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
    public InputBuilder markWme(String name)
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
    public Wme getWme(String name)
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
    public InputBuilder pop()
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
    public InputBuilder top()
    {
        if(parent == null)
        {
            return this;
        }
        InputBuilder p = parent;
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
    public InputBuilder jump(String idName)
    {
        Identifier newId = idMap.get(idName);
        if(newId == null)
        {
            throw new IllegalArgumentException("No id with name '" + idName + "'");
        }
        return new InputBuilder(io, this, newId, idMap, wmeMap);
    }
}
