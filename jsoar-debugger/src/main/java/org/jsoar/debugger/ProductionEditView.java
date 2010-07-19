/*
 * Copyright (c) 2008  Dave Ray <daveray@gmail.com>
 *
 * Created on Oct 23, 2008
 */
package org.jsoar.debugger;

import java.awt.BorderLayout;
import java.awt.Font;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.KeyEvent;
import java.io.StringWriter;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Callable;

import javax.swing.AbstractAction;
import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTextArea;
import javax.swing.JTextField;
import javax.swing.KeyStroke;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.jdesktop.swingx.JXLabel;
import org.jdesktop.swingx.autocomplete.AutoCompleteDecorator;
import org.jdesktop.swingx.autocomplete.ObjectToStringConverter;
import org.jsoar.kernel.Production;
import org.jsoar.kernel.SoarException;
import org.jsoar.kernel.tracing.Printer;
import org.jsoar.runtime.CompletionHandler;
import org.jsoar.runtime.SwingCompletionHandler;
import org.jsoar.runtime.ThreadedAgent;
import org.jsoar.util.SwingTools;
import org.jsoar.util.adaptables.Adaptable;
import org.jsoar.util.adaptables.Adaptables;

import com.google.common.collect.ForwardingList;

/**
 * @author ray
 */
public class ProductionEditView extends AbstractAdaptableView implements Disposable
{
    private static final long serialVersionUID = -5150761314645770374L;
    private static final Log logger = LogFactory.getLog(ProductionEditView.class);

    private static final String DEFAULT_CONTENTS = "Double-click a production (or right-click) to edit, or just start typing.";
    private static final String LAST_CONTENT_KEY = "lastContent";
    
    private final Adaptable debugger;
    private final ThreadedAgent agent;
    private final JTextArea textArea = new JTextArea(DEFAULT_CONTENTS);
    private final JXLabel status = new JXLabel("Ready");
    private final AbstractAction loadAction = new AbstractAction("Load [Ctrl-Return]") {

        private static final long serialVersionUID = -199488933120052983L;

        @Override
        public void actionPerformed(ActionEvent arg0)
        {
            load();
        }};

    /**
     * Wrapper list that forwards to the production model in the list view.
     * This is the list we use for auto-complete.
     * 
     * TODO: There are probably some synchronization issues here.
     */
    private final ForwardingList<Production> productions = new ForwardingList<Production>() {

        @Override
        protected List<Production> delegate()
        {
            ProductionTableModel model = Adaptables.adapt(debugger, ProductionTableModel.class);
            if(model == null)
            {
                return new ArrayList<Production>();
            }
            return model.getProductions();
        }};
    
    public ProductionEditView(Adaptable debugger)
    {
        super("productionEditor", "Production Editor");
        
        this.debugger = debugger;
        this.agent = Adaptables.adapt(debugger, ThreadedAgent.class);
        
        JPanel p = new JPanel(new BorderLayout());
        SwingTools.addUndoSupport(textArea);
        textArea.setFont(new Font("Monospaced", Font.PLAIN, 12));
        p.add(new JScrollPane(textArea), BorderLayout.CENTER);
        
        JPanel north = new JPanel(new BorderLayout());
        north.setBorder(BorderFactory.createEmptyBorder(5, 5, 5, 5));
        final JTextField productionField = new JTextField("Enter production name here and hit enter");
        // Edit the production when they hit enter
        productionField.addActionListener(new ActionListener() {

            @Override
            public void actionPerformed(ActionEvent e)
            {
                editProduction(productionField.getText().trim());
            }});
        north.add(productionField, BorderLayout.CENTER);
        
        // Set up auto completion...
        // TODO: get new swingx with fix for exception on double-click:
        // https://swingx.dev.java.net/issues/show_bug.cgi?id=943
        AutoCompleteDecorator.decorate(productionField, productions, true, new ObjectToStringConverter() {

            @Override
            public String getPreferredStringForItem(Object o)
            {
                return o != null ? ((Production) o).getName() : null;
            }});
        
        p.add(north, BorderLayout.NORTH);
        
        JPanel south = new JPanel(new BorderLayout());
        south.setBorder(BorderFactory.createEmptyBorder(5, 5, 5, 5));
        
        status.setLineWrap(true);
        
        south.add(status, BorderLayout.CENTER);
        final JButton loadButton = new JButton(loadAction);
        // map ctrl+return to load the production back into the agent.
        textArea.getKeymap().addActionForKeyStroke(KeyStroke.getKeyStroke(KeyEvent.VK_ENTER, KeyEvent.CTRL_MASK), loadAction);
        textArea.selectAll();
        
        JPanel t = new JPanel();
        t.add(loadButton);
        south.add(t, BorderLayout.EAST);
        
        p.add(south, BorderLayout.SOUTH);
        
        getContentPane().add(p);
        
        final String oldContents = getPreferences().get(LAST_CONTENT_KEY, null);
        if(oldContents != null)
        {
            textArea.setText(oldContents);
        }
    }
    
    /**
     * Tell the view to start editing the named production. The production's
     * code is loaded into the editor and the view is brought to the front.
     * 
     * @param name The name of the production to edit.
     */
    public void editProduction(final String name)
    {
        final Callable<String> call = new Callable<String>() {

            public String call() throws Exception
            {
                final Production p = agent.getProductions().getProduction(name);
                if(p != null)
                {
                    StringWriter s = new StringWriter();
                    s.append("# " + p.getLocation() + "\n");
                    p.print(new Printer(s), false);
                    return s.toString();
                }
                return "";
            }};
        final CompletionHandler<String> finish = new CompletionHandler<String>() {
            @Override
            public void finish(String result)
            {
                textArea.setText(result);
                status.setText(result.length() != 0 ? "Editing production '" + name + "'" : "No production '" + name + "'");
                toFront();
            }
        };
        agent.execute(call, SwingCompletionHandler.newInstance(finish));
    }
    
    private void load()
    {
        final String contents = textArea.getText().trim();
        if(contents.length() == 0)
        {
            return;
        }
        
        final Callable<String> call = new Callable<String>() {

            @Override
            public String call()
            {
                try
                {
                    agent.getInterpreter().eval(contents);
                    agent.getPrinter().flush();
                    return "Loaded";
                }
                catch (SoarException e)
                {
                    logger.error(e.getMessage(), e);
                    return "ERROR: " + e.getMessage();
                }
            }};
        final CompletionHandler<String> finish = new CompletionHandler<String>() {

            @Override
            public void finish(String result)
            {
                status.setText(result);
            }
        };
        agent.execute(call, SwingCompletionHandler.newInstance(finish));
    }

    /* (non-Javadoc)
     * @see org.jsoar.debugger.Disposable#dispose()
     */
    @Override
    public void dispose()
    {
        final String contents = textArea.getText();
        if(!contents.equals(DEFAULT_CONTENTS))
        {
            getPreferences().put(LAST_CONTENT_KEY, contents);
        }
    }
    
    
}
