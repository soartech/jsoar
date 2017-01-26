/*
 * Copyright (c) 2009 Dave Ray <daveray@gmail.com>
 *
 * Created on May 24, 2009
 */
package org.jsoar.kernel.io.beans;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.jsoar.kernel.events.OutputEvent;
import org.jsoar.kernel.io.InputOutput;
import org.jsoar.kernel.io.InputWmes;
import org.jsoar.kernel.memory.Wme;
import org.jsoar.kernel.symbols.Identifier;
import org.jsoar.util.events.SoarEvent;
import org.jsoar.util.events.SoarEventListener;
import org.jsoar.util.events.SoarEventManager;

/**
 * An output command manager that uses {@link SoarBeanReader} to automatically
 * convert an output command from working memory to a Java object and passes
 * the converted object to a client-registered handler.
 * 
 * @author ray
 */
public class SoarBeanOutputManager
{
    private static final Logger logger = LoggerFactory.getLogger(SoarBeanOutputManager.class);

    private final SoarEventManager eventManager;
    private final SoarEventListener listener;
    private final SoarBeanReader reader = new SoarBeanReader();
    private final Map<String, HandlerInfo> handlers = new ConcurrentHashMap<String, HandlerInfo>();
    
    /**
     * Construct a new output manager and register it with the given
     * event manager.
     * 
     * @param eventManager the event manager to receive {@link OutputEvent}s from
     */
    public SoarBeanOutputManager(SoarEventManager eventManager)
    {
        this.eventManager = eventManager;
        this.listener = new SoarEventListener() {

            @Override
            public void onEvent(SoarEvent event)
            {
                handleCommands((OutputEvent) event);
            }};
        this.eventManager.addListener(OutputEvent.class, listener);
    }
    
    /**
     * Dispose this object, removing it from the event manager
     */
    public void dispose()
    {
        this.eventManager.removeListener(null, listener);
    }
    
    /**
     * Register a handler for an output command. Only a single handler may
     * be registered for a given name.
     * 
     * <p>This method may be called from any thread.
     * 
     * @param <T> type of Java object that receives output command structure
     * @param name the name of the output command
     * @param handler the handler object to call when new output commands are
     *      detected.
     * @param beanClass Java bean class
     */
    public <T> void registerHandler(String name, SoarBeanOutputHandler<T> handler, Class<T> beanClass)
    {
        handlers.put(name, new HandlerInfo(handler, beanClass));
    }
        
    /**
     * Unregister a handler previously registered with {@link #registerHandler(String, SoarBeanOutputHandler, Class)}.
     * 
     * @param name hane of handler to unregister
     */
    public void unregisterHandler(String name)
    {
        handlers.remove(name);
    }
    
    private void handleCommands(OutputEvent outputEvent)
    {
        for(Wme command : outputEvent.getInputOutput().getPendingCommands())
        {
            handleCommand(outputEvent, command);
        }
    }
    
    @SuppressWarnings("unchecked")
    private void handleCommand(final OutputEvent outputEvent, final Wme command)
    {
        final String name = command.getAttribute().toString();
        final HandlerInfo info = handlers.get(name);
        if(info == null)
        {
            return;
        }
        
        final Identifier id = command.getValue().asIdentifier();
        if(id == null)
        {
            return;
        }
        
        try
        {
            final Object bean = reader.read(id, info.beanClass);
            final SoarBeanOutputContext context = new SoarBeanOutputContext() {

                @Override
                public Wme getCommand()
                {
                    return command;
                }

                @Override
                public InputOutput getInputOutput()
                {
                    return outputEvent.getInputOutput();
                }

                @Override
                public void setStatus(Object status)
                {
                    InputWmes.add(getInputOutput(), id, "status", status);
                }

                @Override
                public void setStatusComplete()
                {
                    setStatus("complete");
                }
            };
            info.handler.handleOutputCommand(context, bean);
        }
        catch (SoarBeanException e)
        {
            if(info.handler.exceptionHandler != null)
            {
                info.handler.exceptionHandler.handleSoarBeanException(e);
            }
            else
            {
                logger.error("While handling output command '" + name + "'", e);
            }
        }
    }
    
    private static class HandlerInfo
    {
        final Class<?> beanClass;
        @SuppressWarnings("rawtypes")
        final SoarBeanOutputHandler handler;
        
        public HandlerInfo(SoarBeanOutputHandler<?> handler, Class<?> beanClass)
        {
            this.beanClass = beanClass;
            this.handler = handler;
        }
    }
}
