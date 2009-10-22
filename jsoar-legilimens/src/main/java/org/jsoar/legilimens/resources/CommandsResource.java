/*
 * Copyright (c) 2009 Dave Ray <daveray@gmail.com>
 *
 * Created on Oct 14, 2009
 */
package org.jsoar.legilimens.resources;

import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import org.jsoar.kernel.SoarException;
import org.jsoar.kernel.tracing.Printer;
import org.restlet.data.Form;
import org.restlet.data.Status;
import org.restlet.representation.Representation;
import org.restlet.representation.StringRepresentation;
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
    public Representation postTextCommand(Representation entity)
    {
        final Form form = new Form(entity);
        final String command = form.getFirstValue("command");
        
        final Callable<String> callable = new Callable<String>()
        {
            @Override
            public String call() throws Exception
            {
                final Printer printer = agent.getPrinter();
                printer.startNewLine().print(agent.getName() + "> " + command + "\n");
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
                    printer.error(e.getMessage() + "\n").flush();
                    throw e;
                }
                printer.flush();
                return result;
            }
        };
        
        String result = "";
        try
        {
            result = agent.executeAndWait(callable, 10, TimeUnit.SECONDS);
        }
        catch (InterruptedException e)
        {
            setStatus(Status.SERVER_ERROR_INTERNAL, e, "Interrupted while executing '" + command + "'");
            Thread.currentThread().interrupt();
        }
        catch (ExecutionException e)
        {
            setStatus(Status.SERVER_ERROR_INTERNAL, e, "Error while executing '" + command + "': " + e.getMessage());
        }
        catch (TimeoutException e)
        {
            setStatus(Status.SERVER_ERROR_INTERNAL, e, "Timeout while executing '" + command + "'");
        }
        getResponse().redirectSeeOther(getReferrerRef());
        return new StringRepresentation(result);
    }
}
