/*
 * Copyright (c) 2008  Dave Ray <daveray@gmail.com>
 *
 * Created on Dec 12, 2008
 */
package org.jsoar.kernel.rhs.functions;

import java.util.List;

import javax.script.ScriptContext;
import javax.script.ScriptEngine;
import javax.script.ScriptEngineManager;
import javax.script.ScriptException;

import org.jsoar.kernel.symbols.JavaSymbol;
import org.jsoar.kernel.symbols.Symbol;
import org.jsoar.kernel.symbols.SymbolFactory;
import org.jsoar.kernel.symbols.Symbols;

/**
 * Implementation of a "java" RHS function that allows the full Java API to
 * be called from Soar rules.
 * 
 * <p>In support of this RHS function is {@link JavaSymbol} which is a symbol
 * whose value is an arbitrary Java object (except String, Integer, and Double
 * which are handled by their respective built-in Soar symbols). If this RHS
 * function returns a Java object, it will be stored in a {@link JavaSymbol}
 * and retrievable through normal matching.
 * 
 * <p>This function may be used either standalone, or as a RHS value.
 * 
 * <p>The first argument to the function is a mode flag that must be one of 
 * "null", "new", "static", "method", or "member". Each mode is described in
 * detail below.
 * 
 * <h3>null</h3>
 * <p>The <code>null</code> mode takes no additional arguments and simply
 * returns a {@link JavaSymbol} holding a <code>null</code> reference. This
 * function should be used whenever a <code>null</code> argument is required
 * by a method. For example,
 * 
 * <pre>
 * (java <o> setParent (java null))    # o.setParent(null);
 * </pre>
 * 
 * <h3>new</h3>
 * <p>The <code>new</code> mode is used to invoke Java constructors to create
 * new objects. The resulting object is returned by the function. It requires
 * a as arguments the class name of the object being created as well as any
 * constructor parameters. For example:
 * 
 * <pre>
 * (java new |java.io.File| (java static |java.lang.System.getProperty| |user.dir|))
 * </pre>
 * 
 * will construct a new File object initialized to the current working directory
 * of the application.
 * 
 * <h3>static</h3>
 * <p>The <code>static</code> mode allows static methods to be called. Its first
 * argument is the qualified name of the method, followed by any additional 
 * method parameters. For example:
 * 
 * <pre>{@code
 * (java static |java.lang.Math.max| <a> <b>)   # Math.max(a, b);
 * }</pre>
 * 
 * returns the max of {@code <a>} and {@code <b>}. <i>Note that all static methods
 * of java.lang.Math are already implemented as normal RHS functions.</i>
 * 
 * <h3>method</h3>
 * <p>The <code>method</code> mode is used to invoke non-static member functions
 * on Java objects. It requires at least two additional arguments. The first is
 * the target object. The second is the name of the method to invoke. This arguments
 * may be followed by any additional method parameters. For example, assuming 
 * {@code <o>} is bound to an instance of a Java PrintStream, e.g. System.out:
 * 
 * <pre>
 * (java method <o> println |Hello, World!|)    # o.println("Hello, World!");
 * </pre>
 * 
 * <h3>get</h3>
 * The <code>get</code> method allows access to static and non-state member
 * variables. For static members, the only additional argument is the qualified
 * name of the member. For example, to access System.out:
 * 
 * <pre>
 * (java get |java.lang.System.out|}   # System.out;
 * </pre>
 * 
 * <p>For non-static members, two additional arguments are required. The first
 * is the target object and the second is the name of the member. Assuming that
 * {@code <p>} is bound to a java.awt.Point object:
 * 
 * <pre>{@code
 * (java get <p> x)          # p.x;
 * }</pre>
 * 
 * <h3>set</h3>
 * The <code>set</code> method allows static and non-state member variables to
 * be set. For static members, the only additional arguments are the qualified
 * name of the member and the new value. For example,
 * 
 * <pre>
 * (java set |MyClass.myField| 33}  # MyClass.myField = 33;
 * </pre>
 * 
 * <p>For non-static members, three additional arguments are required. The first
 * is the target object, the second is the name of the member and the third is
 * the new value. Assuming that {@code <p>} is bound to a java.awt.Point object,
 * the <code>x</code> member can be set like this:
 * 
 * <pre>{@code
 * (java set <p> x 1234)           # p.x = 1234;
 * }</pre>
 *  
 * @author ray
 */
public class JavaRhsFunction extends AbstractRhsFunctionHandler
{
    private ScriptEngine engine = null;
    
    /**
     * Construct a new java RHS function
     */
    public JavaRhsFunction()
    {
        super("java", 1, Integer.MAX_VALUE);
    }

    /* (non-Javadoc)
     * @see org.jsoar.kernel.rhs.functions.AbstractRhsFunctionHandler#mayBeStandalone()
     */
    @Override
    public boolean mayBeStandalone()
    {
        return true;
    }

    /* (non-Javadoc)
     * @see org.jsoar.kernel.rhs.functions.AbstractRhsFunctionHandler#mayBeValue()
     */
    @Override
    public boolean mayBeValue()
    {
        return true;
    }

    private void initialize() throws RhsFunctionException
    {
        if(engine == null)
        {
            engine = new ScriptEngineManager().getEngineByName("JavaScript");
            if(engine == null)
            {
                throw new RhsFunctionException("Could not locate 'JavaScript' script engine. " +
                        "java RHS function will not function correctly.");
            }
        }
    }
    
    /* (non-Javadoc)
     * @see org.jsoar.kernel.rhs.functions.RhsFunctionHandler#execute(org.jsoar.kernel.symbols.SymbolFactory, java.util.List)
     */
    @Override
    public Symbol execute(RhsFunctionContext rhsContext, List<Symbol> arguments) throws RhsFunctionException
    {
        RhsFunctions.checkArgumentCount(this, arguments);
        
        initialize();
        
        final SymbolFactory syms = rhsContext.getSymbols();
        try
        {
            String mode = arguments.get(0).toString();
            if(mode.equals("null"))
            {
                RhsFunctions.checkArgumentCount(getName(), arguments, 1, 1);
                // (java null)
                return syms.createJavaSymbol(null);
            }
            else if(mode.equals("static"))
            {
                RhsFunctions.checkArgumentCount(getName(), arguments, 2, Integer.MAX_VALUE);
                // e.g. (java static java.lang.Math.min <a> <b>)
                final String methodName = arguments.get(1).toString();
                return executeStatic(syms, methodName, arguments.subList(2, arguments.size()));
            }
            else if(mode.equals("new"))
            {
                // e.g. (java new java.io.File |path|)
                RhsFunctions.checkArgumentCount(getName(), arguments, 2, Integer.MAX_VALUE);
                final String typeName = arguments.get(1).toString();
                return executeConstructor(syms, typeName, arguments.subList(2, arguments.size()));
            }
            else if(mode.equals("method"))
            {
                RhsFunctions.checkArgumentCount(getName(), arguments, 3, Integer.MAX_VALUE);
                // e.g. (java method <o> setValue <value>)
                final Symbol object = arguments.get(1);
                final String methodName = arguments.get(2).toString();
                return executeMethod(syms, object, methodName, arguments.subList(3, arguments.size()));
            }
            else if(mode.equals("get"))
            {
                if(arguments.size() == 2) // Static member
                {
                    // e.g. (java get java.lang.Math.PI)
                    return getStaticMember(syms, arguments.get(1).toString());
                }
                else if(arguments.size() == 3) // Non-static member
                {
                    // e.g. (java get <o> value)
                    return getMember(syms, arguments.get(1), arguments.get(2).toString());
                }
                else
                {
                    RhsFunctions.checkArgumentCount(getName(), arguments, 2, 3);
                }
            }
            else if(mode.equals("set"))
            {
                if(arguments.size() == 3) // Static member
                {
                    // e.g. (java set java.lang.Math.PI 99)
                    return setStaticMember(syms, arguments.get(1).toString(), arguments.get(2));
                }
                else if(arguments.size() == 4) // Non-static member
                {
                    // e.g. (java set <o> value |hello|)
                    return setMember(syms, arguments.get(1), arguments.get(2).toString(), arguments.get(3));
                }
                else
                {
                    RhsFunctions.checkArgumentCount(getName(), arguments, 3, 4);
                }
            }
            else
            {
                throw new RhsFunctionException("Unexpected java RHS function mode '" + mode + "'");
            }
        }
        finally
        {
            // Clear out any bindings we set to avoid memory leaks.
            engine.getBindings(ScriptContext.ENGINE_SCOPE).clear();
        }
        return null;
    }
    
    private Symbol getStaticMember(SymbolFactory syms, String name) throws RhsFunctionException
    {
        try
        {
            Object result = engine.eval(name);
            return Symbols.create(syms, result);
        }
        catch (ScriptException e)
        {
            throw new RhsFunctionException(e.getMessage(), e);
        }
    }
    
    private Symbol setStaticMember(SymbolFactory syms, String name, Symbol value) throws RhsFunctionException
    {
        try
        {
            engine.put("arg", Symbols.valueOf(value));
            Object result = engine.eval(name + "=arg");
            return Symbols.create(syms, result);
        }
        catch (ScriptException e)
        {
            throw new RhsFunctionException(e.getMessage(), e);
        }
    }
    
    private Symbol getMember(SymbolFactory syms, Symbol object, String name) throws RhsFunctionException
    {
        try
        {
            engine.put("o", Symbols.valueOf(object));
            Object result = engine.eval("o." + name);
            return Symbols.create(syms, result);
        }
        catch (ScriptException e)
        {
            throw new RhsFunctionException(e.getMessage(), e);
        }
    }
    
    private Symbol setMember(SymbolFactory syms, Symbol object, String name, Symbol value) throws RhsFunctionException
    {
        try
        {
            engine.put("o", Symbols.valueOf(object));
            engine.put("arg", Symbols.valueOf(value));
            Object result = engine.eval("o." + name + "=arg");
            return Symbols.create(syms, result);
        }
        catch (ScriptException e)
        {
            throw new RhsFunctionException(e.getMessage(), e);
        }
    }
    
    private Symbol executeStatic(SymbolFactory syms, String name, List<Symbol> arguments) throws RhsFunctionException
    {
        StringBuilder exp = new StringBuilder(name);
        appendArguments(arguments, exp);
        
        try
        {
            Object result = engine.eval(exp.toString());
            return Symbols.create(syms, result);
        }
        catch (ScriptException e)
        {
            throw new RhsFunctionException(e.getMessage(), e);
        }
    }
    
    private Symbol executeConstructor(SymbolFactory syms, String typeName, List<Symbol> arguments) throws RhsFunctionException
    {
        StringBuilder exp = new StringBuilder("new " + typeName);
        appendArguments(arguments, exp);
        
        try
        {
            Object result = engine.eval(exp.toString());
            return Symbols.create(syms, result);
        }
        catch (ScriptException e)
        {
            throw new RhsFunctionException(e.getMessage(), e);
        }
    }
    
    private Symbol executeMethod(SymbolFactory syms, Symbol object, String name, List<Symbol> arguments) throws RhsFunctionException
    {
        StringBuilder exp = new StringBuilder("o." + name);
        engine.put("o", Symbols.valueOf(object));
        appendArguments(arguments, exp);
        
        try
        {
            Object result = engine.eval(exp.toString());
            return Symbols.create(syms, result);
        }
        catch (ScriptException e)
        {
            throw new RhsFunctionException(e.getMessage(), e);
        }
    }

    private void appendArguments(List<Symbol> arguments, StringBuilder exp)
    {
        exp.append('(');
        int i = 0;
        for(Symbol arg : arguments)
        {
            String argName = "arg" + i;
            engine.put(argName, Symbols.valueOf(arg));
            if(i != 0)
            {
                exp.append(',');
            }
            exp.append(argName);
            ++i;
        }
        exp.append(')');
    }
}
