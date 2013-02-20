/*
 * Copyright (c) 2013 Soar Technology, Inc.
 *
 * Created on Feb 12, 2013
 */
package org.jsoar.kernel.wma;

import java.io.PrintWriter;
import java.io.StringWriter;

import org.jsoar.kernel.SoarException;
import org.jsoar.kernel.memory.Wme;
import org.jsoar.kernel.rete.Rete;
import org.jsoar.kernel.wma.DefaultWorkingMemoryActivationParams.ForgetWmeChoices;
import org.jsoar.kernel.wma.DefaultWorkingMemoryActivationParams.ForgettingChoices;
import org.jsoar.kernel.wma.DefaultWorkingMemoryActivationParams.TimerLevels;
import org.jsoar.util.adaptables.Adaptable;
import org.jsoar.util.adaptables.Adaptables;
import org.jsoar.util.commands.SoarCommand;
import org.jsoar.util.commands.SoarCommandContext;
import org.jsoar.util.commands.SoarCommandInterpreter;
import org.jsoar.util.commands.SoarCommandProvider;
import org.jsoar.util.properties.PropertyKey;
import org.jsoar.util.properties.PropertyManager;

/**
 * @author bob.marinier
 */
class DefaultWorkingMemoryActivationCommand implements SoarCommand
{
    private final DefaultWorkingMemoryActivation wma;
    private final Rete rete;
    
    public static class Provider implements SoarCommandProvider
    {
        /* (non-Javadoc)
         * @see org.jsoar.util.commands.SoarCommandProvider#registerCommands(org.jsoar.util.commands.SoarCommandInterpreter)
         */
        @Override
        public void registerCommands(SoarCommandInterpreter interp, Adaptable context)
        {
            interp.addCommand("wma", new DefaultWorkingMemoryActivationCommand(context));
        }
    }
    
    public DefaultWorkingMemoryActivationCommand(Adaptable context)
    {
        this.wma = Adaptables.require(getClass(), context, DefaultWorkingMemoryActivation.class);
        this.rete = Adaptables.adapt(context, Rete.class);
    }

    /* (non-Javadoc)
     * @see org.jsoar.util.commands.SoarCommand#execute(java.lang.String[])
     */
    @Override
    public String execute(SoarCommandContext commandContext, String[] args) throws SoarException
    {
        if(args.length == 1)
        {
            return doWma();
        }
        
        final String arg = args[1];
        if("-g".equals(arg) || "--get".equals(arg))
        {
            return doGet(1, args);
        }
        else if("-s".equals(arg) || "--set".equals(arg))
        {
            return doSet(1, args);
        }
        else if("-S".equals(arg) || "--stats".equals(arg))
        {
            return doStats(1, args);
        }
        else if("-t".equals(arg) || "--timers".equals(arg))
        {
            return doTimers(1, args);
        }
        else if("-h".equals(arg) || "--history".equals(arg))
        {
            return doHistory(1, args);
        }
        else if(arg.startsWith("-"))
        {
            throw new SoarException("Unknown option " + arg);
        }
        else
        {
            throw new SoarException("Unknown argument " + arg);
        }
    }

    private String doGet(int i, String[] args) throws SoarException
    {
        if(i + 1 >= args.length)
        {
            throw new SoarException("Invalid arguments for " + args[i] + " option");
        }
        final String name = args[i+1];
        final PropertyKey<?> key = DefaultWorkingMemoryActivationParams.getProperty(wma.getParams().getProperties(), name);
        if(key == null)
        {
            throw new SoarException("Unknown parameter '" + name + "'");
        }
        return wma.getParams().getProperties().get(key).toString();
    }

    private String doSet(int i, String[] args) throws SoarException
    {
        if(i + 2 >= args.length)
        {
            throw new SoarException("Invalid arguments for " + args[i] + " option");
        }
        final String name = args[i+1];
        final String value = args[i+2];
        final PropertyManager props = wma.getParams().getProperties();
        if(name.equals("activation"))
        {
            props.set(DefaultWorkingMemoryActivationParams.ACTIVATION, "on".equals(value));
        }
        else if(props.get(DefaultWorkingMemoryActivationParams.ACTIVATION))
        {
            // TODO: This check should be done in the property system
            throw new SoarException("This parameter is protected while WMA is on.");
        }
        else if(name.equals("decay-rate"))
        {
            props.set(DefaultWorkingMemoryActivationParams.DECAY_RATE, Double.valueOf(value));
        }
        else if(name.equals("decay-thresh"))
        {
            props.set(DefaultWorkingMemoryActivationParams.DECAY_THRESH, Double.valueOf(value));
        }
        else if(name.equals("forgetting"))
        {
            props.set(DefaultWorkingMemoryActivationParams.FORGETTING_CHOICES, "on".equals(value) ? ForgettingChoices.approx : ForgettingChoices.valueOf(value));
        }
        else if(name.equals("forget-wme"))
        {
            props.set(DefaultWorkingMemoryActivationParams.FORGET_WME_CHOICES, ForgetWmeChoices.valueOf(value));
        }
        else if(name.equals("max-pow-cache"))
        {
            props.set(DefaultWorkingMemoryActivationParams.MAX_POW_CACHE, Integer.valueOf(value));
        }
        else if(name.equals("petrov-approx"))
        {
            props.set(DefaultWorkingMemoryActivationParams.PETROV_APPROX, "on".equals(value));
        }
        else if(name.equals("timers"))
        {
            props.set(DefaultWorkingMemoryActivationParams.TIMERS, TimerLevels.valueOf(value));
        }
        else
        {
            throw new SoarException("Unknown parameter '" + name + "'");
        }
        
        return "";
    }

    private String doStats(int i, String[] args) throws SoarException
    {
        final StringWriter sw = new StringWriter();
        final PrintWriter pw = new PrintWriter(sw);
        
        final DefaultWorkingMemoryActivationStats p = wma.getStats();
        if(args.length == i + 1)
        {
            pw.printf("Forgotten WMEs: %d%n", p.forgotten_wmes.get());
        }
        else
        {
            final String name = args[i+1];
            final PropertyKey<?> key = DefaultWorkingMemoryActivationStats.getProperty(wma.getParams().getProperties(), name);
            if(key == null)
            {
                throw new SoarException("Unknown stat '" + name + "'");
            }
            pw.printf("%s%n", wma.getParams().getProperties().get(key).toString());
        }
        
        pw.flush();
        return sw.toString();
    }

    private String doTimers(int i, String[] args)
    {
        // TODO Auto-generated method stub
        return null;
    }

    private String doHistory(int i, String[] args) throws SoarException
    {
        if(i + 1 >= args.length)
        {
            throw new SoarException("Invalid arguments for " + args[i] + " option");
        }
        final String tt = args[i+1];
        
        final long timetag = Long.valueOf(tt);
        if (timetag == 0)
            return ("Invalid timetag.");
        
        Wme wme = null;
        
        for ( Wme tempwme : rete.getAllWmes() )
        {
            if ( tempwme.getTimetag() == timetag )
            {
                wme = tempwme;
                break;
            }
        }

        if ( wme != null )
        {
            
            return wma.wma_get_wme_history( wme );

        }
        
        return "WME has no decay history";
    }

    private String doWma()
    {
        final StringWriter sw = new StringWriter();
        final PrintWriter pw = new PrintWriter(sw);
        
        final DefaultWorkingMemoryActivationParams p = wma.getParams();
        pw.printf("WMA activation: %s%n", p.activation.get() ? "on" : "off");
        pw.println();
        pw.println("Activation");
        pw.println("----------");
        pw.printf("decay-rate: %f%n", p.decay_rate.get());
        pw.printf("petrov-approx: %s%n", p.petrov_approx.get() ? "on" : "off");
        pw.println();
        pw.println("Forgetting");
        pw.println("----------");
        pw.printf("decay-thresh: %f%n", p.decay_thresh.get());
        pw.printf("forgetting: %s%n", p.forgetting.get());
        pw.printf("forget-wme: %s%n", p.forget_wme.get());
        pw.printf("fake-forgetting: %s%n", p.fake_forgetting.get() ? "on" : "off");
        pw.println();
        pw.println("Performance");
        pw.println("-----------");
        pw.printf("timers: %s%n", p.timers.get());
        pw.printf("max-pow-cache: %d%n", p.max_pow_cache.get());
        
        pw.flush();
        return sw.toString();
    }

}
