package org.jsoar.debugger.stopcommand;

import java.awt.BorderLayout;
import java.io.StringWriter;

import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTextPane;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import javax.swing.text.DefaultStyledDocument;

import org.jdesktop.swingx.JXTextField;
import org.jsoar.debugger.Disposable;
import org.jsoar.debugger.JSoarDebugger;
import org.jsoar.debugger.Refreshable;
import org.jsoar.debugger.selection.SelectionListener;
import org.jsoar.debugger.selection.SelectionManager;
import org.jsoar.debugger.syntax.Highlighter;
import org.jsoar.kernel.SoarException;
import org.jsoar.kernel.events.StopEvent;
import org.jsoar.util.events.SoarEvent;
import org.jsoar.util.events.SoarEventListener;

import bibliothek.gui.dock.common.DefaultMultipleCDockable;
import bibliothek.gui.dock.common.MultipleCDockableFactory;

public class StopCommandView extends DefaultMultipleCDockable implements SelectionListener, Refreshable, Disposable, SoarEventListener
{

    private static final long COMMAND_DELAY_MILLIS = 800;
    private final JSoarDebugger debugger;
    private final Highlighter highlighter;
    private JXTextField txtCommand = new JXTextField("");
    private JTextPane txtResult = new JTextPane();
    private long lastInputTimestamp = System.currentTimeMillis();
    private static final Object lock = new Object();
    private DefaultStyledDocument styledDocument = new DefaultStyledDocument();

    public StopCommandView(MultipleCDockableFactory<?,?> factory, JSoarDebugger debuggerIn)
    {
        super(factory, "Stop Command View");
        this.debugger = debuggerIn;

        highlighter = Highlighter.getInstance(debugger);


        setResizeLocked(false);
        setCloseable(true);
        setExternalizable(true);


        debugger.getAgent().getAgent().getEvents().addListener(StopEvent.class,this);
        JPanel p = new JPanel();
        p.setLayout(new BorderLayout(2,2));
//        txtResult.setLineWrap(true);
        txtResult.setEditable(false);
        txtResult.setStyledDocument(styledDocument);
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
        highlighter.setDefaultTextStyle(txtResult);



        p.add(txtCommand, BorderLayout.NORTH);
        p.add(txtResult, BorderLayout.CENTER);


        getContentPane().add(new JScrollPane(p));
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

                    result += writer.getBuffer().toString();
                    txtResult.setText(result.trim());
                    //highlighter.formatText(txtResult);

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
