/*
 * Copyright (c) 2008  Dave Ray <daveray@gmail.com>
 *
 * Created on Oct 30, 2008
 */
package org.jsoar.kernel.rhs.functions;

import java.util.Arrays;
import java.util.List;

import org.jsoar.kernel.symbols.DoubleSymbol;
import org.jsoar.kernel.symbols.IntegerSymbol;
import org.jsoar.kernel.symbols.Symbol;

/**
 * Simple math RHS functions. This includes most of the functions from java.lang.Math.
 * 
 * @author ray
 */
public class MathFunctions
{
    private MathFunctions()
    {
    }
    
    /**
     * Base class for a RHS function that returns a double constant.
     */
    private static class Constant extends AbstractRhsFunctionHandler
    {
        
        private final double value;
        
        /**
         * @param name Name of the function
         * @param value Vaule to return
         */
        public Constant(String name, double value)
        {
            super(name, 0, 0);
            this.value = value;
        }
        
        @Override
        public Symbol execute(RhsFunctionContext context, List<Symbol> arguments) throws RhsFunctionException
        {
            RhsFunctions.checkArgumentCount(this, arguments);
            return context.getSymbols().createDouble(value);
        }
    }
    
    /**
     * RHS function that returns <i>pi</i>
     * 
     * @see java.lang.Math#PI
     */
    public static final RhsFunctionHandler PI = new Constant("pi", Math.PI);
    /**
     * RHS function that returns <i>e</i>
     * 
     * @see java.lang.Math#E
     */
    public static final RhsFunctionHandler E = new Constant("e", Math.E);
    
    /**
     * <p>rhsfun_math.cpp:443:abs_rhs_function_code
     */
    public static final RhsFunctionHandler ABS = new AbstractRhsFunctionHandler("abs", 1, 1)
    {
        
        /*
         * (non-Javadoc)
         * 
         * @see org.jsoar.kernel.rhs.functions.RhsFunctionHandler#execute(org.jsoar.kernel.symbols.SymbolFactory, java.util.List)
         */
        @Override
        public Symbol execute(RhsFunctionContext context, List<Symbol> arguments) throws RhsFunctionException
        {
            RhsFunctions.checkArgumentCount(this, arguments);
            IntegerSymbol i = arguments.get(0).asInteger();
            if(i != null)
            {
                return context.getSymbols().createInteger(Math.abs(i.getValue()));
            }
            DoubleSymbol d = arguments.get(0).asDouble();
            if(d != null)
            {
                return context.getSymbols().createDouble(Math.abs(d.getValue()));
            }
            throw new RhsFunctionException(getName() + " expected numeric argument, got " + arguments.get(0));
        }
    };
    
    /**
     * <p>rhsfun_math.cpp:390:atan2_rhs_function_code
     */
    public static final RhsFunctionHandler ATAN2 = new AbstractRhsFunctionHandler("atan2", 2, 2)
    {
        
        /*
         * (non-Javadoc)
         * 
         * @see org.jsoar.kernel.rhs.functions.RhsFunctionHandler#execute(org.jsoar.kernel.symbols.SymbolFactory, java.util.List)
         */
        @Override
        public Symbol execute(RhsFunctionContext context, List<Symbol> arguments) throws RhsFunctionException
        {
            RhsFunctions.checkArgumentCount(this, arguments);
            RhsFunctions.checkAllArgumentsAreNumeric(getName(), arguments);
            
            Double y = RhsFunctions.asDouble(arguments.get(0));
            Double x = RhsFunctions.asDouble(arguments.get(1));
            
            return context.getSymbols().createDouble(Math.atan2(y, x));
        }
    };
    
    private static abstract class OneArgMathFunction extends AbstractRhsFunctionHandler
    {
        public OneArgMathFunction(String name)
        {
            super(name, 1, 1);
        }
        
        /*
         * (non-Javadoc)
         * 
         * @see org.jsoar.kernel.rhs.functions.RhsFunctionHandler#execute(org.jsoar.kernel.symbols.SymbolFactory, java.util.List)
         */
        @Override
        public Symbol execute(RhsFunctionContext context, List<Symbol> arguments) throws RhsFunctionException
        {
            RhsFunctions.checkArgumentCount(this, arguments);
            RhsFunctions.checkAllArgumentsAreNumeric(getName(), arguments);
            
            Double v = RhsFunctions.asDouble(arguments.get(0));
            return context.getSymbols().createDouble(call(v));
        }
        
        protected abstract double call(double arg) throws RhsFunctionException;
    }
    
    /**
     * <p>rhsfun_math.cpp:361:sqrt_rhs_function_code
     */
    public static final RhsFunctionHandler SQRT = new OneArgMathFunction("sqrt")
    {
        @Override
        protected double call(double arg) throws RhsFunctionException
        {
            return Math.sqrt(arg);
        }
    };
    
    public static final RhsFunctionHandler SIN = new OneArgMathFunction("sin")
    {
        @Override
        protected double call(double arg) throws RhsFunctionException
        {
            return Math.sin(arg);
        }
    };
    
    public static final RhsFunctionHandler COS = new OneArgMathFunction("cos")
    {
        @Override
        protected double call(double arg) throws RhsFunctionException
        {
            return Math.cos(arg);
        }
    };
    
    public static final RhsFunctionHandler TAN = new OneArgMathFunction("tan")
    {
        @Override
        protected double call(double arg) throws RhsFunctionException
        {
            return Math.tan(arg);
        }
    };
    
    /** @see java.lang.Math#acos(double) */
    public static final RhsFunctionHandler ACOS = new OneArgMathFunction("acos")
    {
        @Override
        protected double call(double arg) throws RhsFunctionException
        {
            return Math.acos(arg);
        }
    };
    
    /** @see java.lang.Math#asin(double) */
    public static final RhsFunctionHandler ASIN = new OneArgMathFunction("asin")
    {
        @Override
        protected double call(double arg) throws RhsFunctionException
        {
            return Math.asin(arg);
        }
    };
    
    /** @see java.lang.Math#atan(double) */
    public static final RhsFunctionHandler ATAN = new OneArgMathFunction("atan")
    {
        @Override
        protected double call(double arg) throws RhsFunctionException
        {
            return Math.atan(arg);
        }
    };
    
    /** @see java.lang.Math#cbrt(double) */
    public static final RhsFunctionHandler CBRT = new OneArgMathFunction("cbrt")
    {
        @Override
        protected double call(double arg) throws RhsFunctionException
        {
            return Math.cbrt(arg);
        }
    };
    
    /** @see java.lang.Math#ceil(double) */
    public static final RhsFunctionHandler CEIL = new OneArgMathFunction("ceil")
    {
        @Override
        protected double call(double arg) throws RhsFunctionException
        {
            return Math.ceil(arg);
        }
    };
    
    /** @see java.lang.Math#cosh(double) */
    public static final RhsFunctionHandler COSH = new OneArgMathFunction("cosh")
    {
        @Override
        protected double call(double arg) throws RhsFunctionException
        {
            return Math.cosh(arg);
        }
    };
    
    /** @see java.lang.Math#exp(double) */
    public static final RhsFunctionHandler EXP = new OneArgMathFunction("exp")
    {
        @Override
        protected double call(double arg) throws RhsFunctionException
        {
            return Math.exp(arg);
        }
    };
    
    /** @see java.lang.Math#expm1(double) */
    public static final RhsFunctionHandler EXPM1 = new OneArgMathFunction("expm1")
    {
        @Override
        protected double call(double arg) throws RhsFunctionException
        {
            return Math.expm1(arg);
        }
    };
    
    /** @see java.lang.Math#floor(double) */
    public static final RhsFunctionHandler FLOOR = new OneArgMathFunction("floor")
    {
        @Override
        protected double call(double arg) throws RhsFunctionException
        {
            return Math.floor(arg);
        }
    };
    
    /** @see java.lang.Math#log(double) */
    public static final RhsFunctionHandler LOG = new OneArgMathFunction("log")
    {
        @Override
        protected double call(double arg) throws RhsFunctionException
        {
            return Math.log(arg);
        }
    };
    
    /** @see java.lang.Math#log10(double) */
    public static final RhsFunctionHandler LOG10 = new OneArgMathFunction("log10")
    {
        @Override
        protected double call(double arg) throws RhsFunctionException
        {
            return Math.log10(arg);
        }
    };
    
    /** @see java.lang.Math#log1p(double) */
    public static final RhsFunctionHandler LOG1P = new OneArgMathFunction("log1p")
    {
        @Override
        protected double call(double arg) throws RhsFunctionException
        {
            return Math.log1p(arg);
        }
    };
    
    /** @see java.lang.Math#sinh(double) */
    public static final RhsFunctionHandler SINH = new OneArgMathFunction("sinh")
    {
        @Override
        protected double call(double arg) throws RhsFunctionException
        {
            return Math.sinh(arg);
        }
    };
    
    /** @see java.lang.Math#tanh(double) */
    public static final RhsFunctionHandler TANH = new OneArgMathFunction("tanh")
    {
        @Override
        protected double call(double arg) throws RhsFunctionException
        {
            return Math.tanh(arg);
        }
    };
    
    /** @see java.lang.Math#signum(double) */
    public static final RhsFunctionHandler SIGNUM = new OneArgMathFunction("signum")
    {
        @Override
        protected double call(double arg) throws RhsFunctionException
        {
            return Math.signum(arg);
        }
    };
    
    /** @see java.lang.Math#toDegrees(double) */
    public static final RhsFunctionHandler DEGREES = new OneArgMathFunction("degrees")
    {
        @Override
        protected double call(double arg) throws RhsFunctionException
        {
            return Math.toDegrees(arg);
        }
    };
    
    /** @see java.lang.Math#toRadians(double) */
    public static final RhsFunctionHandler RADIANS = new OneArgMathFunction("radians")
    {
        @Override
        protected double call(double arg) throws RhsFunctionException
        {
            return Math.toRadians(arg);
        }
    };
    
    /** @see java.lang.Math#ulp(double) */
    public static final RhsFunctionHandler ULP = new OneArgMathFunction("ulp")
    {
        @Override
        protected double call(double arg) throws RhsFunctionException
        {
            return Math.ulp(arg);
        }
    };
    
    public static final List<RhsFunctionHandler> ALL = Arrays.asList(
            new Plus(), new Multiply(), new Minus(), new FloatingPointDivide(), new Mod(),
            new Min(), new Max(), new CumulativeNormalDistribution(), new Div(),
            PI, E,
            ABS, ATAN2, COS, SIN, TAN, SQRT, ACOS, ASIN, ATAN, CBRT, CEIL, COSH, EXP, EXPM1, FLOOR,
            LOG, LOG10, LOG1P, SINH, TANH, DEGREES, RADIANS, ULP);
    
}
