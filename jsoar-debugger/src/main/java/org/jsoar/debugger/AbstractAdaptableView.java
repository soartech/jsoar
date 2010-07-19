/*
 * Copyright (c) 2008  Dave Ray <daveray@gmail.com>
 *
 * Created on Oct 23, 2008
 */
package org.jsoar.debugger;

import java.util.prefs.Preferences;

import org.jsoar.util.adaptables.Adaptable;
import org.jsoar.util.adaptables.Adaptables;

import bibliothek.gui.dock.common.DefaultSingleCDockable;

/**
 * @author ray
 */
public abstract class AbstractAdaptableView extends DefaultSingleCDockable implements Adaptable
{
    private static final long serialVersionUID = 8049528094231200441L;

    public AbstractAdaptableView(String persistentId, String title)
    {
        super(persistentId, title);
        
        setCloseable(true);
    }
    
    public String getShortcutKey()
    {
        return null;
    }
    
    /**
     * Called when the view becomes active 
     */
    public void activate()
    {
    }
    
//    /* (non-Javadoc)
//     * @see org.flexdock.view.View#setContentPane(java.awt.Container)
//     */
//    @Override
//    public void setContentPane(Container c) throws IllegalArgumentException
//    {
//        // Give every view a default border ...
//        final JPanel wrapper = new JPanel(new BorderLayout());
//        wrapper.add(c, BorderLayout.CENTER);
//        wrapper.setBorder(BorderFactory.createLineBorder(Color.GRAY, 1));
//        super.setContentPane(wrapper);
//    }


    /* (non-Javadoc)
     * @see org.jsoar.util.adaptables.AbstractAdaptable#getAdapter(java.lang.Class)
     */
    @Override
    public Object getAdapter(Class<?> klass)
    {
        return Adaptables.adapt(this, klass, false);
    }
    
    public Preferences getPreferences()
    {
        return JSoarDebugger.PREFERENCES.node("views/" + this.getUniqueId());
    }
}
