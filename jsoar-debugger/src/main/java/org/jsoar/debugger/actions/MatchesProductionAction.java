package org.jsoar.debugger.actions;

import java.awt.event.ActionEvent;
import java.util.List;

import org.jsoar.kernel.Production;
import org.jsoar.runtime.ThreadedAgent;
import org.jsoar.util.adaptables.Adaptables;

public class MatchesProductionAction extends AbstractDebuggerAction
{
    private final boolean showWmes;
    
    private static final long serialVersionUID = -7767296421795513742L;
    
    /**
     * @param manager the owning action manager
     * @param showWmes whether to add --wmes to the production matches command
     */
    public MatchesProductionAction(ActionManager manager, boolean showWmes)
    {
        super(manager, getBaseCommand(showWmes), null, Production.class, true);
        
        this.showWmes = showWmes;
        
        setToolTip(getBaseCommand(showWmes));
    }
    
    @Override
    public void actionPerformed(ActionEvent e)
    {
        final List<Production> prods = Adaptables.adaptCollection(getSelectionManager().getSelection(), Production.class);
        if(prods.isEmpty())
        {
            return;
        }
        
        final String command = getBaseCommand(this.showWmes) + prods.get(0).getName();
        final ThreadedAgent agent = getApplication().getAgent();
        
        agent.execute(() ->
        {
            agent.getPrinter().startNewLine();
            agent.getInterpreter().eval("echo " + command);
            agent.getInterpreter().eval(command);
            return null;
        }, null);
    }
    
    private static String getBaseCommand(boolean showWmes)
    {
        return "production matches " + (showWmes ? "--wmes " : "");
    }
    
    @Override
    public void update()
    {
    }
    
}
