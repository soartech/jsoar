/*
 * Copyright (c) 2009  Dave Ray <daveray@gmail.com>
 *
 * Created on Jan 17, 2009
 */
package org.jsoar.kernel.parser;

import org.jsoar.util.adaptables.Adaptable;

/**
 * An interface for an object that provides context information to a Soar
 * parser. In particular it must provide access to services requested by the
 * parser through calls to {@link Adaptable#getAdapter(Class)}.
 * 
 * <p>The default context for a parser will typically be a Soar agent.
 * 
 * @author ray
 */
public interface ParserContext extends Adaptable
{

}
