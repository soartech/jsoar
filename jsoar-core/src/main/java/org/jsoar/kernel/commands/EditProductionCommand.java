/*
 * Copyright (c) 2009 Dave Ray <daveray@gmail.com>
 */
package org.jsoar.kernel.commands;

import java.awt.Desktop;
import java.io.File;
import java.io.IOException;

import org.jsoar.kernel.Agent;
import org.jsoar.kernel.Production;
import org.jsoar.kernel.SoarException;
import org.jsoar.util.FileTools;
import org.jsoar.util.SourceLocation;
import org.jsoar.util.commands.SoarCommand;
import org.jsoar.util.commands.SoarCommandContext;

/**
 * Implementation of the "edit-production" command.
 * 
 * @author ray
 */
public final class EditProductionCommand implements SoarCommand
{
    private final Agent agent;

    public EditProductionCommand(Agent agent)
    {
        this.agent = agent;
    }

    @Override
    public String execute(SoarCommandContext commandContext, String[] args) throws SoarException
    {
        if(args.length != 2)
        {
            throw new SoarException("Expected single production name argument");
        }
        final String name = args[1];
        final Production p = agent.getProductions().getProduction(name);
        if(p == null)
        {
            throw new SoarException("No production named '" + name + "'");
        }
        final SourceLocation location = p.getLocation();
        final String file = location.getFile();
        if(file == null || file.length() == 0)
        {
            throw new SoarException("Don't know source location of production '" + name + "'");
        }
        if(FileTools.asUrl(file) != null)
        {
            throw new SoarException("Don't know how to edit productions loaded from URLs: " + file);
        }
        
        try
        {
            Desktop.getDesktop().edit(new File(file));
        }
        catch (IOException e)
        {
            throw new SoarException("Failed to edit '" + file + "': " + e);
        }
        return file;
    }
}