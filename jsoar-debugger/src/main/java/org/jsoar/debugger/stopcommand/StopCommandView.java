package org.jsoar.debugger.stopcommand;

import bibliothek.gui.dock.common.*;
import org.jdesktop.swingx.JXTextArea;
import org.jdesktop.swingx.JXTextField;
import org.jsoar.debugger.Disposable;
import org.jsoar.debugger.JSoarDebugger;
import org.jsoar.debugger.Refreshable;
import org.jsoar.debugger.selection.SelectionListener;
import org.jsoar.debugger.selection.SelectionManager;
import org.jsoar.kernel.SoarException;
import org.jsoar.kernel.events.StopEvent;
import org.jsoar.util.events.SoarEvent;
import org.jsoar.util.events.SoarEventListener;

import javax.swing.*;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import java.awt.*;
import java.io.StringWriter;

public class StopCommandView extends DefaultMultipleCDockable implements SelectionListener, Refreshable, Disposable, SoarEventListener
{

    private static final long COMMAND_DELAY_MILLIS = 800;
    private final JSoarDebugger debugger;
    private JXTextField txtCommand = new JXTextField("");
    private JXTextArea txtResult = new JXTextArea("");
    private long lastInputTimestamp = System.currentTimeMillis();
    private static final Object lock = new Object();

    public StopCommandView(MultipleCDockableFactory factory, JSoarDebugger debuggerIn)
    {
        super(factory, "Stop Command View");
        this.debugger = debuggerIn;

        setResizeLocked(false);
        setCloseable(true);
        setExternalizable(true);


        debugger.getAgent().getAgent().getEvents().addListener(StopEvent.class,this);
        JPanel p = new JPanel(new BorderLayout());
        txtResult.setLineWrap(true);
        txtResult.setEditable(false);
        txtCommand.getDocument().addDocumentListener(new DocumentListener()
        {
            @Override
            public void insertUpdate(DocumentEvent e)
            {
                updateCommand(e);
            }

            @Override
            public void removeUpdate(DocumentEvent e)
            {
                updateCommand(e);
            }

            @Override
            public void changedUpdate(DocumentEvent e)
            {
                updateCommand(e);
            }
        });


        p.add(txtCommand, BorderLayout.NORTH);
        p.add(txtResult, BorderLayout.CENTER);


        this.getContentPane().add(p);
    }

    private void updateCommand(DocumentEvent e)
    {
        new Thread(() ->
        {
            lastInputTimestamp = System.currentTimeMillis();
            try {
                Thread.sleep(COMMAND_DELAY_MILLIS);
            } catch (InterruptedException ignored) {
            }
            long time = System.currentTimeMillis();
            if (lastInputTimestamp + COMMAND_DELAY_MILLIS <= time) {
                setTitleText("On Stop: "+txtCommand.getText());
                runStopCommand();
            }
        }).start();
    }

    @Override
    public void dispose()
    {

    }

    @Override
    public void refresh(boolean afterInitSoar)
    {
        runStopCommand();
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
        if (event instanceof StopEvent){
            runStopCommand();
        }
    }

    public void runStopCommand()
    {
        String command = txtCommand.getText();
        if (!command.trim().isEmpty()) {
            try {
                synchronized (lock) {
                    System.out.println("stopcommand " + command + " running...");

                    //most commands don't actually return a string, but print straight to the writer. We need to add our own writer to intercept the output
                    StringWriter writer = new StringWriter();

                    debugger.getAgent().getPrinter().pushWriter(writer); //redirect output to us

                    String result = debugger.getAgent().getAgent().getInterpreter().eval(command);

                    Thread.sleep(30); //need to pause because otherwise we get some fun race conditions for slower commands and multiple windows
                    //may need to increase pause duration based on how long some commands take to run

                    debugger.getAgent().getPrinter().popWriter();//stop redirecting output to us

                    txtResult.setText(result);

                    txtResult.append(writer.getBuffer().toString());
                    System.out.println("stopcommand " + command + " done!");
                }
            } catch (SoarException | InterruptedException e) {
                System.out.println("stopcommand " + command + " error!");
                txtResult.setText(e.getMessage()); //print the error if there was one
            }
        }
    }

    public void setCommand(String command)
    {
        txtCommand.setText(command);
        setTitleText("On Stop: "+txtCommand.getText());
        runStopCommand();
    }
}
