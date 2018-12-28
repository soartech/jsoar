package org.jsoar.debugger.stopcommand;

import org.jsoar.debugger.JSoarDebugger;

import bibliothek.gui.dock.common.CLocation;
import bibliothek.gui.dock.common.MultipleCDockableFactory;

public class StopCommandViewFactory implements MultipleCDockableFactory<StopCommandView, StopCommandViewLayout>
{
    private final JSoarDebugger debugger;

    public StopCommandViewFactory(JSoarDebugger debuggerIn)
    {
        this.debugger = debuggerIn;
    }

    @SuppressWarnings("unused")
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
        String command = dockable.getCurrentCommand();
        return command.equals(layout.getCommand());
    }

    @Override
    public StopCommandView read(StopCommandViewLayout layout)
    {
        String command = layout.getCommand();

        StopCommandView frame = new StopCommandView(this, debugger);
        frame.setCommand(command);
//        frame.setLocation(defaultLocation);

        return frame;
    }


    @Override
    public StopCommandViewLayout write(StopCommandView dockable)
    {
        StopCommandViewLayout layout = new StopCommandViewLayout();
        layout.setCommand(dockable.getCurrentCommand());
        return layout;
    }
}

/**
 * Describes the layout of one {@link PvdDockable}
 */

