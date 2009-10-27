/*
 * Copyright (c) 2009 Dave Ray <daveray@gmail.com>
 *
 * Created on Oct 14, 2009
 */
package org.jsoar.legilimens.resources;

import java.util.concurrent.Callable;

import org.jsoar.kernel.SoarException;
import org.jsoar.kernel.tracing.Printer;
import org.restlet.data.Form;
import org.restlet.representation.Representation;
import org.restlet.resource.Post;

/**
 * @author ray
 */
public class CommandsResource extends BaseAgentResource
{
    /**
     * 
     */
    public CommandsResource()
    {
    }

    @Post()
    public void postTextCommand(Representation entity)
    {
        final Form form = new Form(entity);
        final String command = form.getFirstValue("command");
        
        final Callable<String> callable = new Callable<String>()
        {
            @Override
            public String call() throws Exception
            {
                final Printer printer = agent.getPrinter();
                printer.startNewLine().print(agent.getName() + "> " + command);
                String result;
                try
                {
                    result = agent.getInterpreter().eval(command);
                    if(result != null && result.length() != 0)
                    {
                        printer.startNewLine().print(result).flush();
                    }
                }
                catch (SoarException e)
                {
                    printer.startNewLine().error(e.getMessage()).flush();
                    throw e;
                }
                printer.flush();
                return result;
            }
        };
        
        executeCallable(callable);
        //getResponse().redirectSeeOther(getReferrerRef());
    }
}
