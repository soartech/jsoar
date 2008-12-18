package org.jsoar.tcl;

import org.jsoar.kernel.learning.rl.ReinforcementLearning;

import tcl.lang.Command;
import tcl.lang.Interp;
import tcl.lang.TclException;
import tcl.lang.TclNumArgsException;
import tcl.lang.TclObject;

/**
 * @author ray
 */
final class ReinforcementLearningCommand implements Command
{
    /**
     * 
     */
    private final SoarTclInterface ifc;

    /**
     * @param soarTclInterface
     */
    ReinforcementLearningCommand(SoarTclInterface soarTclInterface)
    {
        ifc = soarTclInterface;
    }

    @Override
    public void cmdProc(Interp interp, TclObject[] args) throws TclException
    {
        if(args.length != 4)
        {
            throw new TclNumArgsException(interp, 0, args, "--set learning [on|off]");
        }
        
        // TODO reinforcement learning: Obviously, this implementation is insufficient
        ifc.agent.rl.rl_set_parameter(ReinforcementLearning.RL_PARAM_LEARNING, 
                "on".equals(args[3].toString()) ? ReinforcementLearning.RL_LEARNING_ON :
                    ReinforcementLearning.RL_LEARNING_ON );
    }
}