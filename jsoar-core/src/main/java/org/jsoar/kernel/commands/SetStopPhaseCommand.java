/*
 * Copyright (c) 2010 Dave Ray <daveray@gmail.com>
 *
 * Created on May 29, 2010
 */
package org.jsoar.kernel.commands;

import org.jsoar.kernel.Phase;
import org.jsoar.kernel.SoarException;
import org.jsoar.kernel.SoarProperties;
import org.jsoar.util.commands.SoarCommand;
import org.jsoar.util.commands.SoarCommandContext;
import org.jsoar.util.properties.PropertyManager;

/**
 * @author ray
 */
public class SetStopPhaseCommand implements SoarCommand
{
    private final PropertyManager props;
    
    
    public SetStopPhaseCommand(PropertyManager props)
    {
        this.props = props;
    }

    /* (non-Javadoc)
     * @see org.jsoar.util.commands.SoarCommand#execute(java.lang.String[])
     */
    @Override
    public String execute(SoarCommandContext commandContext, String[] args) throws SoarException
    {
        if(args.length == 1)
        {
            return getMessage(props.get(SoarProperties.STOP_PHASE));
        }
        boolean before = true;
        Phase phase = null;
        
        for(int i = 1; i < args.length; ++i)
        {
            final String arg = args[i];
            
            if("-A".equals(arg) || "--after".equals(arg))
            {
                before = false;
            }
            else if("-B".equals(arg) || "--before".equals(arg))
            {
                before = true;
            }
        }
        for(int i = 1; i < args.length; ++i)
        {
            final String arg = args[i];
            
            // input -> propose -> decision -> apply -> output
            if("-a".equals(arg) || "--apply".equals(arg))
            {
                phase = before ? Phase.APPLY : Phase.OUTPUT;
            }
            else if("-d".equals(arg) || "--decision".equals(arg))
            {
                phase = before ? Phase.DECISION : Phase.APPLY;
            }
            else if("-i".equals(arg) || "--input".equals(arg))
            {
                phase = before ? Phase.INPUT : Phase.PROPOSE;
            }
            else if("-o".equals(arg) || "--output".equals(arg))
            {
                phase = before ? Phase.OUTPUT : Phase.INPUT;
            }
            else if("-p".equals(arg) || "--proposal".equals(arg))
            {
                phase = before ? Phase.PROPOSE : Phase.DECISION;
            }
            else if(!"-A".equals(arg) && !"--after".equals(arg) && !"-B".equals(arg) && !"--before".equals(arg))
            {
                throw new SoarException("Unknown option '" + arg + "'");
            }
        }
        if(phase == null)
        {
            throw new SoarException("No phase specified.");
        }
        
        props.set(SoarProperties.STOP_PHASE, phase);
        
        return getMessage(phase);
    }

    private String getMessage(Phase phase)
    {
        return "Stop before " + phase.toString().toLowerCase() + " phase";
    }

}
