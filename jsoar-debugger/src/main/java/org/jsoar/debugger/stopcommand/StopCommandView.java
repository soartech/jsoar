package org.jsoar.debugger.stopcommand;

import bibliothek.gui.dock.common.*;
import org.jsoar.debugger.Disposable;
import org.jsoar.debugger.JSoarDebugger;
import org.jsoar.debugger.Refreshable;
import org.jsoar.debugger.TableFilterPanel;
import org.jsoar.debugger.selection.SelectionListener;
import org.jsoar.debugger.selection.SelectionManager;
import org.jsoar.kernel.SoarException;
import org.jsoar.kernel.events.StopEvent;
import org.jsoar.util.events.SoarEvent;
import org.jsoar.util.events.SoarEventListener;

import javax.swing.*;
import java.awt.*;
import java.io.StringWriter;

public class StopCommandView extends DefaultMultipleCDockable implements SelectionListener, Refreshable, Disposable, SoarEventListener
{

    private final JSoarDebugger debugger;
    private JTextField txtCommand = new JTextField("");
    private JTextArea txtResult = new JTextArea("");
    public StopCommandView(MultipleCDockableFactory factory, JSoarDebugger debuggerIn)
    {
        super(factory, "Stop Command View");
        this.debugger = debuggerIn;

        debugger.getAgent().getAgent().getEvents().addListener(StopEvent.class,this);
        JPanel p = new JPanel(new BorderLayout());
        txtResult.setLineWrap(true);
        txtResult.setEditable(false);


        p.add(txtCommand, BorderLayout.NORTH);
        p.add(txtResult, BorderLayout.CENTER);


        this.getContentPane().add(p);
    }

    @Override
    public void dispose()
    {

    }

    @Override
    public void refresh(boolean afterInitSoar)
    {

    }

    @Override
    public void selectionChanged(SelectionManager manager)
    {

    }

    public String getCurrentCommand()
    {
        return txtCommand.getText();
    }

    @Override
    public void onEvent(SoarEvent event)
    {
        System.out.println("event!");
        if (event instanceof StopEvent){
            try {
                String command = txtCommand.getText();
                StringWriter writer = new StringWriter();

                debugger.getAgent().getPrinter().pushWriter(writer); //redirect output
                String result = debugger.getAgent().getAgent().getInterpreter().eval(command);

                debugger.getAgent().getPrinter().popWriter();//pop our writer so output goes back to the window

                txtResult.setText(result);

                txtResult.append(writer.getBuffer().toString());
            } catch (SoarException e) {
                txtResult.setText(e.getMessage()); //print the error if there was one
            }
        }
    }
}
