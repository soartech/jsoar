/*
 * Copyright (c) 2010 Dave Ray <daveray@gmail.com>
 *
 * Created on May 15, 2010
 */
package org.jsoar.util.commands;

import android.content.res.AssetManager;

import org.jsoar.kernel.Agent;

/**
 * @author ray
 */
public interface SoarCommandInterpreterFactory
{
    String getName();
    SoarCommandInterpreter create(Agent agent, AssetManager assetManager);
}
