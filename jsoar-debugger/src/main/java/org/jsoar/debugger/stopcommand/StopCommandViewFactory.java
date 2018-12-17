package org.jsoar.debugger.stopcommand;

import bibliothek.gui.dock.common.*;
import bibliothek.gui.dock.common.intern.CControlAccess;
import bibliothek.gui.dock.common.intern.CommonMultipleDockableFactory;
import org.jsoar.debugger.JSoarDebugger;

import java.util.Map;

public class StopCommandViewFactory implements MultipleCDockableFactory<StopCommandView, StopCommandViewLayout>
{
    private final JSoarDebugger debugger;

    public StopCommandViewFactory(JSoarDebugger debuggerIn)
    {
        this.debugger = debuggerIn;
    }

    private CLocation defaultLocation;

    @Override
    public StopCommandViewLayout create()
    {
        return new StopCommandViewLayout();
    }

    public void setDefaultLocation(CLocation loc){
        this.defaultLocation = loc;
    }

    @Override
    public boolean match(StopCommandView dockable, StopCommandViewLayout layout)
    {
        String name = dockable.getCurrentCommand();
        return name.equals(layout.getName());
    }

    @Override
    public StopCommandView read(StopCommandViewLayout layout)
    {
        String name = layout.getName();

        StopCommandView frame = new StopCommandView(this, debugger);
        frame.setLocation(defaultLocation);

        return frame;
    }


    @Override
    public StopCommandViewLayout write(StopCommandView dockable)
    {
        StopCommandViewLayout layout = new StopCommandViewLayout();
        layout.setName(dockable.getTitleText());
        return layout;
    }
}

/**
 * Describes the layout of one {@link PvdDockable}
 */

