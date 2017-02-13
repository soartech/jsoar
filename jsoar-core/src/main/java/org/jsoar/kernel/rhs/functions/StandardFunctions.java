/*
 * Copyright (c) 2008  Dave Ray <daveray@gmail.com>
 *
 * Created on Sep 18, 2008
 */
package org.jsoar.kernel.rhs.functions;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import org.jsoar.kernel.Agent;
import org.jsoar.kernel.DecisionCycle;
import org.jsoar.kernel.symbols.Symbol;
import org.jsoar.util.StringTools;
import org.jsoar.util.adaptables.Adaptables;

/**
 * @author ray
 */
public class StandardFunctions
{
    private final Agent context;
    
    /**
     * Takes any number of arguments, and prints each one.
     *  
     * rhsfun.cpp:162:write_rhs_function_code
     */
    public final RhsFunctionHandler write = new StandaloneRhsFunctionHandler("write") {

        @Override
        public Symbol execute(RhsFunctionContext rhsContext, List<Symbol> arguments) throws RhsFunctionException
        {
            for(Symbol arg : arguments)
            {
                context.getPrinter().print(arg.toString());
            }
            context.getPrinter().flush();
            return null;
        }
    };
    
    /**
     * Just returns a sym_constant whose print name is a line feed.
     *  
     * rhsfun.cpp:189:crlf_rhs_function_code
     */
    public final RhsFunctionHandler crlf = new AbstractRhsFunctionHandler("crlf", 0, 0) {

        @Override
        public Symbol execute(RhsFunctionContext rhsContext, List<Symbol> arguments) throws RhsFunctionException
        {
            RhsFunctions.checkArgumentCount(this, arguments);
            return rhsContext.getSymbols().createString("\n");
        }
    };
    
    /**
     * RHS function that prints a failure message and halts the agent.
     */
    public final RhsFunctionHandler failed = new StandaloneRhsFunctionHandler("failed") {

        @Override
        public Symbol execute(RhsFunctionContext rhsContext, List<Symbol> arguments) throws RhsFunctionException
        {
            context.getPrinter().error("Failed: %s: %s", 
                    rhsContext.getProductionBeingFired().getName(), 
                    StringTools.join(arguments, ", "));
            return context.getRhsFunctions().getHandler("halt").execute(rhsContext, arguments);
        }
        
    };
    
    /**
     * RHS function that prints a success message and halts the agent.
     */
    public final RhsFunctionHandler succeeded = new StandaloneRhsFunctionHandler("succeeded") {

        @Override
        public Symbol execute(RhsFunctionContext rhsContext, List<Symbol> arguments) throws RhsFunctionException
        {
            context.getPrinter().print("Succeeded: %s: %s", 
                    rhsContext.getProductionBeingFired().getName(), 
                    StringTools.join(arguments, ", "));
            return context.getRhsFunctions().getHandler("halt").execute(rhsContext, arguments);
        }
        
    };
    
    private final List<RhsFunctionHandler> allInternal = 
        new ArrayList<RhsFunctionHandler>(Arrays.asList(write, crlf, failed, succeeded,
                new Concat(), new IfEq(), new MakeConstantSymbol(), new MakeIntegerSymbol(), new StrLen(), new Split(),
                new DeepCopy(),
                new StringRhsFunction(), new IntRhsFunction(), new FloatRhsFunction(),
                new FromXml(), 
                new ToXml(),
                new FromSoarTechXml(),
                new FromAutoTypeXml(),
                new FromManualTypeXml(),
                new ToSoarTechXml(),
                new GetUrl(), 
                new AcceptRhsFunction(),
                new ListRhsFunction(),
                new FormatRhsFunction()));
    {
        allInternal.addAll(MathFunctions.all);
    }
    
    /**
     * Unmodifiable list of all standard RHS function handlers, including those 
     * defined in {@link MathFunctions}
     */
    public final List<RhsFunctionHandler> all = Collections.unmodifiableList(allInternal);

    /**
     * Construct an instance of this class and install standard RHS functions
     * 
     * @param context The agent
     */
    public StandardFunctions(Agent context)
    {
        this.context = context;
        
        final DecisionCycle decisionCycle = Adaptables.adapt(context, DecisionCycle.class);
        allInternal.add(new Interrupt(decisionCycle));
        allInternal.add(new Debug(context));
        allInternal.add(new ExecRhsFunction(context.getRhsFunctions()));
        allInternal.add(new CmdRhsFunction(context.getInterpreter(), context));
        
        allInternal.add(new RandomInt(context.getRandom()));
        allInternal.add(new RandomFloat(context.getRandom()));
        
        allInternal.add(new LogRhsFunction(context.getInterpreter()));
        
        for(RhsFunctionHandler handler : all)
        {
            context.getRhsFunctions().registerHandler(handler);
        }
    }
    
    
}
