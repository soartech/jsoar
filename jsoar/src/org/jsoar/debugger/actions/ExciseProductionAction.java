/*
 * Copyright (c) 2008  Dave Ray <daveray@gmail.com>
 *
 * Created on Oct 25, 2008
 */
package org.jsoar.debugger.actions;

import java.awt.event.ActionEvent;
import java.util.List;
import java.util.concurrent.Callable;

import org.jsoar.debugger.Images;
import org.jsoar.kernel.Production;
import org.jsoar.runtime.ThreadedAgentProxy;
import org.jsoar.util.adaptables.Adaptables;

/**
 * @author ray
 */
public class ExciseProductionAction extends AbstractDebuggerAction
{
    private static final long serialVersionUID = -1460902354871319429L;

    /**
     * @param manager the owning action manager
     */
    public ExciseProductionAction(ActionManager manager)
    {
        super(manager, "Excise", Images.DELETE, Production.class, true);
        
        setToolTip("Excise production");
    }

    /* (non-Javadoc)
     * @see org.jsoar.debugger.actions.AbstractDebuggerAction#update()
     */
    @Override
    public void update()
    {
        final List<Production> prods = Adaptables.adaptCollection(getSelectionManager().getSelection(), Production.class);
        setEnabled(!prods.isEmpty());
    }

    /* (non-Javadoc)
     * @see java.awt.event.ActionListener#actionPerformed(java.awt.event.ActionEvent)
     */
    @Override
    public void actionPerformed(ActionEvent arg0)
    {
        final List<Production> prods = Adaptables.adaptCollection(getSelectionManager().getSelection(), Production.class);
        if(prods.isEmpty())
        {
            return;
        }
        final ThreadedAgentProxy proxy = getApplication().getAgentProxy();
        proxy.execute(new Callable<Void>() {

            @Override
            public Void call() throws Exception
            {
                for(Production p : prods)
                {
                    proxy.getAgent().getProductions().exciseProduction(p, true);
                }
                proxy.getAgent().getTrace().flush();
                return null;
            }});
        getApplication().updateActionsAndStatus();
    }

}
