/*
 * Copyright (c) 2008  Dave Ray <daveray@gmail.com>
 *
 * Created on Oct 21, 2008
 */
package org.jsoar.kernel;

/**
 * The various ways of running an agent.
 * 
 * @author ray
 */
public enum RunType
{
    DECISIONS,
    ELABORATIONS,
    FOREVER,
    PHASES,
    MODIFICATIONS_OF_OUTPUT,
}
