package org.jsoar.debugger.stopcommand;

import java.awt.BorderLayout;
import java.awt.EventQueue;
import java.io.StringWriter;
import java.lang.reflect.InvocationTargetException;

import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTextPane;
import javax.swing.SwingUtilities;
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
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import bibliothek.gui.dock.common.DefaultMultipleCDockable;
import bibliothek.gui.dock.common.MultipleCDockableFactory;

public class StopCommandView extends DefaultMultipleCDockable implements SelectionListener, Refreshable, Disposable, SoarEventListener
{
    private static final Logger LOG = LoggerFactory.getLogger(StopCommandView.class);
    
    private final JSoarDebugger debugger;
    private final Highlighter highlighter;
    private JXTextField txtCommand = new JXTextField("");
    private JTextPane txtResult = new JTextPane();
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
        setTitleText("On Stop: "+txtCommand.getText());
        debugger.getAgent().execute(() -> { runStopCommand(); return null; }, null);
    }

    @Override
    public void dispose()
    {

    }

    /**
     * This method will be called from the UI thread
     */
    @Override
    public void refresh(boolean afterInitSoar)
    {
        // runStopCommand() expects to run on the Soar agent thread
        debugger.getAgent().execute(() -> { runStopCommand(); return null; }, null);
    }

    @Override
    public void selectionChanged(SelectionManager manager)
    {

    }

    public String getCurrentCommand()
    {
        return txtCommand.getText();
    }

    /**
     * This method will be called on the Soar agent thread
     */
    @Override
    public void onEvent(SoarEvent event)
    {
        if (event instanceof StopEvent){
            runStopCommand();
        }
    }

    /**
     * This method should be called on the Soar agent thread
     */
    public void runStopCommand()
    {
        String command = getCommandToExecute();

        if (command != null && !command.trim().isEmpty()) 
        {
            
            LOG.info("stopcommand " + command + " running...");

            //most commands don't actually return a string, but print straight to the writer. We need to add our own writer to intercept the output
            StringWriter writer = new StringWriter();

            debugger.getAgent().getPrinter().pushWriter(writer); //redirect output to us
            
            String commandReturnResult;
            try
            {
                commandReturnResult = debugger.getAgent().getInterpreter().eval(command);
            } catch (SoarException e) {
                LOG.info("stopcommand " + command + " error!");
                commandReturnResult = e.getMessage(); //print the error if there was one
            }
            
            debugger.getAgent().getPrinter().popWriter();//stop redirecting output to us

            String commandPrintResult = writer.getBuffer().toString();
            final String result = commandReturnResult + commandPrintResult;

            SwingUtilities.invokeLater(() -> txtResult.setText(result.trim()));

            LOG.info("stopcommand " + command + " done!");
            
        }
    }
    
    private String getCommandToExecute()
    {
        final String[] textholder = { null };
        try
        {
            EventQueue.invokeAndWait(() -> textholder[0] = txtCommand.getText());
        }
        catch (InvocationTargetException e)
        {
            LOG.error("Exception while getting stop command", e);
            return "";
        }
        catch (InterruptedException e)
        {
            Thread.currentThread().interrupt();
        }
        return textholder[0];
    }

    /**
     * This method is called on the UI thread
     */
    public void setCommand(String command)
    {
        txtCommand.setText(command);
        setTitleText("On Stop: "+txtCommand.getText());
        // runStopCommand() expects to run on the Soar agent thread
        debugger.getAgent().execute(() -> { runStopCommand(); return null; }, null);
    }


}
