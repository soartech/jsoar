/*
 * Copyright (c) 2008  Dave Ray <daveray@gmail.com>
 *
 * Created on Oct 25, 2008
 */
package org.jsoar.debugger.actions;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.jsoar.debugger.JSoarDebugger;
import org.jsoar.debugger.selection.SelectionListener;
import org.jsoar.debugger.selection.SelectionManager;
import org.jsoar.util.adaptables.Adaptables;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * @author ray
 */
public class ActionManager
{
    private static final Logger logger = LoggerFactory.getLogger(ActionManager.class);
    
    private JSoarDebugger app;
    private List<AbstractDebuggerAction> actions = new ArrayList<AbstractDebuggerAction>();
    private Map<String, AbstractDebuggerAction> actionCache = new HashMap<String, AbstractDebuggerAction>();
    
    private static class ObjectActionPair
    {
        AbstractDebuggerAction action;
        Class<?> klass;
        public boolean adapt;
    };
    
    private List<ObjectActionPair> objectActions = new ArrayList<ObjectActionPair>();
    
    /**
     * @param app The owning application
     */
    public ActionManager(JSoarDebugger app)
    {
        this.app = app;
        
        SelectionManager selectionManager = this.app.getSelectionManager();
        selectionManager.addListener(new SelectionListener()
        {
            
            public void selectionChanged(SelectionManager manager)
            {
                updateActions();
            }
        });
    }
    
    /**
     * @return The owning application
     */
    public JSoarDebugger getApplication()
    {
        return app;
    }
    
    /**
     * @return the selection manager
     */
    public SelectionManager getSelectionManager()
    {
        return app.getSelectionManager();
    }
    
    public AbstractDebuggerAction getAction(String id)
    {
        AbstractDebuggerAction r = actionCache.get(id);
        if(r != null)
        {
            return r;
        }
        
        for(AbstractDebuggerAction action : actions)
        {
            if(id.equals(action.getId()))
            {
                r = action;
                break;
            }
        }
        
        if(r != null)
        {
            actionCache.put(r.getId(), r);
        }
        
        return r;
    }
    
    public <T extends AbstractDebuggerAction> T getAction(Class<T> klass)
    {
        return klass.cast(getAction(klass.getCanonicalName()));
    }
    
    /**
     * Add an action that is managed by the application
     * 
     * @param action The action to add
     */
    public void addAction(AbstractDebuggerAction action)
    {
        if(!actionCache.containsKey(action.getId()))
        {
            actionCache.put(action.getId(), action);
        }
        actions.add(action);
    }
    
    public void updateActions()
    {
        for(AbstractDebuggerAction action : actions)
        {
            action.update();
        }
    }
    
    public void executeAction(String id)
    {
        AbstractDebuggerAction action = getAction(id);
        if(action != null)
        {
            action.actionPerformed(null);
        }
        else
        {
            logger.error("No action found with id '" + id + "'");
        }
    }
    
    /**
     * Register an action associated with a particular object class.
     * 
     * @param action The action
     * @param klass The class of object this action is associated with.
     * @param adapt If true, the class is located through adapters in addition to the usual
     *     instanceof test.
     */
    public void addObjectAction(AbstractDebuggerAction action, Class<?> klass, boolean adapt)
    {
        addAction(action);
        
        ObjectActionPair pair = new ObjectActionPair();
        pair.action = action;
        pair.klass = klass;
        pair.adapt = adapt;
        
        objectActions.add(pair);
    }
    
    /**
     * Return a list of actions applicable to the given object. These are
     * actions previously installed with a call to {@link #addObjectAction(AbstractDebuggerAction, Class, boolean)}.
     * 
     * @param o The object
     * @return The list of applicable actions.
     */
    public List<AbstractDebuggerAction> getActionsForObject(Object o)
    {
        List<AbstractDebuggerAction> result = new ArrayList<AbstractDebuggerAction>();
        for(ObjectActionPair pair : objectActions)
        {
            if(pair.adapt)
            {
                if(Adaptables.adapt(o, pair.klass) != null)
                {
                    result.add(pair.action);
                }
            }
            else if(pair.klass.isInstance(o))
            {
                result.add(pair.action);
            }
        }
        return result;
    }
}
