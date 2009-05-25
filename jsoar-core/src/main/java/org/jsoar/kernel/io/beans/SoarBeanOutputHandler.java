/*
 * Copyright (c) 2009 Dave Ray <daveray@gmail.com>
 *
 * Created on May 24, 2009
 */
package org.jsoar.kernel.io.beans;


/**
 * Handler interface registered with an instance of {@link SoarBeanOutputManager}.
 * 
 * @author ray
 */
public interface SoarBeanOutputHandler<T>
{
    /**
     * Called when an output command is executed by the agent.
     * 
     * <p>This method is called from the agent thread!
     * 
     * @param context output command execution context info
     * @param bean the bean object constructed from the output command's working memory
     */
    void handleOutputCommand(SoarBeanOutputContext context, T bean);
}
