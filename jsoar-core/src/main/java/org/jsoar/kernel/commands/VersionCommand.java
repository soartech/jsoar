/*
 * Copyright (c) 2010 Dave Ray <daveray@gmail.com>
 *
 * Created on May 28, 2010
 */
package org.jsoar.kernel.commands;

import org.jsoar.kernel.JSoarVersion;
import org.jsoar.kernel.SoarException;
import org.jsoar.util.commands.SoarCommand;

/**
 * @author ray
 */
public class VersionCommand implements SoarCommand
{
    /* (non-Javadoc)
     * @see org.jsoar.util.commands.SoarCommand#execute(java.lang.String[])
     */
    @Override
    public String execute(String[] args) throws SoarException
    {
        final JSoarVersion v = JSoarVersion.getInstance();
        return String.format("%s%nBuilt on: %s%nBuilt by: %s", v.getVersion(), v.getBuildDate(), v.getBuiltBy());
    }

}
