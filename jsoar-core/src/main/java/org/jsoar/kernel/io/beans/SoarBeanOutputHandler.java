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
public abstract class SoarBeanOutputHandler<T>
{
    protected SoarBeanExceptionHandler exceptionHandler = null;
    
    /**
     * Called when an output command is executed by the agent.
     * 
     * <p>This method is called from the agent thread!
     * 
     * @param context output command execution context info
     * @param bean the bean object constructed from the output command's working memory
     */
    public abstract void handleOutputCommand(SoarBeanOutputContext context, T bean);
    
    /**
     * Set a handler to be called when a WME fails to be converted to a SoarBean.
     * 
     * <p> If no handler is null (default value), the error will be logged by JSoar, otherwise,
     * no error will be logged by JSoar.
     */
    public void setExceptionHandler(SoarBeanExceptionHandler handler)
    {
        exceptionHandler = handler;
    }
}
