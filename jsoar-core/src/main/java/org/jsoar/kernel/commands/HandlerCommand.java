package org.jsoar.kernel.commands;

import java.util.List;

import org.jsoar.kernel.Agent;
import org.jsoar.kernel.SoarException;
import org.jsoar.kernel.rhs.functions.RhsFunctionHandler;
import org.jsoar.kernel.rhs.functions.RhsFunctionManager;
import org.jsoar.util.commands.OptionProcessor;
import org.jsoar.util.commands.SoarCommand;
import org.jsoar.util.commands.SoarCommandContext;

import com.google.common.collect.Lists;

/**
 * Implementation of the "enable" command.
 * @author adam.sypniewski
 */
public class HandlerCommand implements SoarCommand {
	
	private final Agent agent;
    private final OptionProcessor<Options> options = OptionProcessor.create();
    
    private enum Options
    {
        enable,
        disable
    }
    
    public HandlerCommand(Agent agent)
    {
    	this.agent = agent;
        
        options
        	.newOption(Options.enable)
        	.newOption(Options.disable)
        	.done();
    }

	@Override
	public String execute(SoarCommandContext context, String[] args) throws SoarException
	{
		List<String> nonOpts = options.process(Lists.newArrayList(args));
		
		RhsFunctionManager rhsFunctionManager = agent.getRhsFunctions();
		
		if (options.has(Options.enable))
		{
			if (nonOpts.size() == 1)
			{
				rhsFunctionManager.enableHandler(nonOpts.get(0));
				return "RHS function enabled: " + nonOpts.get(0);
			}
		}
		else if (options.has(Options.disable))
		{
			if (nonOpts.size() == 1)
			{
				rhsFunctionManager.disableHandler(nonOpts.get(0));
				return "RHS function disabled: " + nonOpts.get(0);
			}
		}
		else
		{
			if (nonOpts.isEmpty())
			{
				String result = "===== Disabled RHS Functions =====\n";
				for (RhsFunctionHandler handler : rhsFunctionManager.getDisabledHandlers())
				{
					result += handler.getName() + "\n";
				}
				return result;
			}
		}
		
		throw new SoarException("Invalid arguments. Expected: handler [{--enable | --disable} RHS-FUNCTION-NAME]");
	}
}
