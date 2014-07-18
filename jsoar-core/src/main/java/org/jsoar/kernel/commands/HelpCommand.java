/*
 * Copyright (c) 2009 Dave Ray <daveray@gmail.com>
 */
package org.jsoar.kernel.commands;

import java.awt.Desktop;
import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.ResourceBundle;

import org.jsoar.kernel.SoarException;
import org.jsoar.util.commands.SoarCommand;
import org.jsoar.util.commands.SoarCommandContext;

/**
 * Implementation of the "help" command.
 * 
 * @author ray
 */
public final class HelpCommand implements SoarCommand
{
    private final ResourceBundle resources = ResourceBundle.getBundle("jsoar");
    
    public HelpCommand()
    {
    }

    @Override
    public String execute(SoarCommandContext commandContext, String[] args) throws SoarException
    {
        if(args.length > 2)
        {
            // TODO illegal args
            throw new SoarException(String.format("%s [command name]", args[0]));
        }
        
        final String url;
        if(args.length == 1)
        {
            url = resources.getString("help.url.all");
        }
        else 
        {
            String command = args[1];
            url = resources.getString("help.url.base") + command.replace('-', '_');
        }
        
        try
        {
            Desktop.getDesktop().browse(new URI(url));
            return url;
        }
        catch (IOException e1)
        {
            throw new SoarException(e1.getMessage());
        }
        catch (URISyntaxException e1)
        {
            throw new SoarException(e1.getMessage());
        }
    }
}