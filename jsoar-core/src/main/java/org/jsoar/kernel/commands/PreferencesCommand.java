/*
 * Copyright (c) 2009 Dave Ray <daveray@gmail.com>
 *
 * Created on Jun 21, 2009
 */
package org.jsoar.kernel.commands;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import org.jsoar.kernel.Agent;
import org.jsoar.kernel.Decider;
import org.jsoar.kernel.PredefinedSymbols;
import org.jsoar.kernel.SoarException;
import org.jsoar.kernel.symbols.Identifier;
import org.jsoar.kernel.symbols.Symbol;
import org.jsoar.kernel.symbols.Symbols;
import org.jsoar.kernel.tracing.Trace.WmeTraceType;
import org.jsoar.util.adaptables.Adaptables;
import org.jsoar.util.commands.SoarCommand;

/**
 * http://winter.eecs.umich.edu/soarwiki/Preferences
 * 
 * <p>cli_preferences.cpp
 * <p>sml_KernelHelpers.cpp:soar_ecPrintPreferences
 * 
 * @author ray
 */
public class PreferencesCommand implements SoarCommand
{
    private final Agent agent;
    
    /**
     * @param agent
     */
    public PreferencesCommand(Agent agent)
    {
        this.agent = agent;
    }

    /* (non-Javadoc)
     * @see org.jsoar.util.commands.SoarCommand#execute(java.lang.String[])
     */
    @Override
    public String execute(String[] args) throws SoarException
    {
        final PrintPreferencesCommand ppc = new PrintPreferencesCommand();
        processArgs(args, ppc);
        try
        {
            ppc.print(agent, agent.getPrinter());
        }
        catch (IOException e)
        {
            throw new SoarException(e.getMessage(), e);
        }
        agent.getPrinter().flush();
        return "";
    }

    private void processArgs(String[] args, PrintPreferencesCommand ppc) throws SoarException
    {
        ppc.setWmeTraceType(WmeTraceType.NONE);
        ppc.setPrintProduction(false);
        ppc.setObject(false);
        
        final Decider decider = Adaptables.adapt(agent, Decider.class);
        ppc.setId(decider.bottom_goal);
        final PredefinedSymbols preSyms = Adaptables.adapt(agent, PredefinedSymbols.class);
        ppc.setAttr(preSyms.operator_symbol);
        
        final List<String> nonOpts = new ArrayList<String>();
        for(int i = 1; i < args.length; ++i)
        {
            final String arg = args[i];
            
            if("-0".equals(arg) || "-n".equals(arg) || "--none".equals(arg))
            {
                ppc.setWmeTraceType(WmeTraceType.NONE);
            }
            else if("-1".equals(arg) || "-N".equals(arg) || "--names".equals(arg))
            {
                ppc.setWmeTraceType(WmeTraceType.NONE);
                ppc.setPrintProduction(true);
            }
            else if("-2".equals(arg) || "-t".equals(arg) || "--timetags".equals(arg))
            {
                ppc.setWmeTraceType(WmeTraceType.TIMETAG);
                ppc.setPrintProduction(true);
            }
            else if("-3".equals(arg) || "-w".equals(arg) || "--wmes".equals(arg))
            {
                ppc.setWmeTraceType(WmeTraceType.FULL);
                ppc.setPrintProduction(true);
            }
            else if("-o".equals(arg) || "--object".equals(arg))
            {
                ppc.setObject(true);
            }
            else if(arg.startsWith("-"))
            {
                throw new SoarException("Unknown option to " + arg);
            }
            else
            {
                nonOpts.add(arg);
            }
        }
        
        if(nonOpts.size() == 1 || nonOpts.size() == 2)
        {
            final String idArg = nonOpts.get(0);
            Symbol idSym = agent.readIdentifierOrContextVariable(idArg);
            if(idSym == null)
            {
                throw new SoarException("Could not find identifier '" + idArg + "'");
            }
            final Identifier id = idSym.asIdentifier();
            if(id == null)
            {
                throw new SoarException("'" + idArg + "' is not an identifier");
            }
            ppc.setId(id);
            
            if(nonOpts.size() == 2)
            {
                final String attrArg = nonOpts.get(1);
                final Symbol attr = Symbols.readAttributeFromString(agent, attrArg);
                if(attr == null)
                {
                    throw new SoarException("'" + attrArg + "' is not a known attribute");
                }
                ppc.setAttr(attr);
            }
            else if(!id.isGoal())
            {
                ppc.setAttr(null);
            }
        }
        else if(nonOpts.size() != 0)
        {
            throw new SoarException("Too many arguments, expected [id] [[^]attribute]");
        }
    }
}
