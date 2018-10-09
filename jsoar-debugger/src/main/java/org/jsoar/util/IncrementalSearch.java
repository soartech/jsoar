/*
 * Copyright (c) 2009 Dave Ray <daveray@gmail.com>
 *
 * Created on Jul 17, 2009
 */
package org.jsoar.util;

import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.regex.PatternSyntaxException;

import javax.swing.*;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import javax.swing.text.*;

public class IncrementalSearch implements DocumentListener, ActionListener
{
    protected JEditorPane content;

    protected Matcher matcher;

    public IncrementalSearch(JTextComponent comp)
    {
        this.content = (JEditorPane) comp;
    }

    /* DocumentListener implementation */
    public void insertUpdate(DocumentEvent evt)
    {
        runNewSearch(evt.getDocument());
    }

    public void removeUpdate(DocumentEvent evt)
    {
        runNewSearch(evt.getDocument());
    }

    public void changedUpdate(DocumentEvent evt)
    {
        runNewSearch(evt.getDocument());
    }

    /* ActionListener implementation */
    public void actionPerformed(ActionEvent evt)
    {
        continueSearch();
    }

    private void runNewSearch(Document query_doc)
    {
        try
        {
            final String query = query_doc.getText(0, query_doc.getLength());
            if(query.length() > 0)
            {
                final Pattern pattern = Pattern.compile(query);
                final Document content_doc = content.getDocument();
                String body = content_doc.getText(0, content_doc.getLength());
                matcher = pattern.matcher(body);
            }
            else
            {
                matcher = null;
            }

            highlightAll();


            continueSearch();
        }
        catch (PatternSyntaxException ex)
        {
            matcher = null;
            onError();
        }
        catch (BadLocationException e)
        {
            onError();
        }
    }

    private void highlightAll() {
        if (matcher != null) {

            Highlighter h = content.getHighlighter();
            LayeredHighlighter.LayerPainter painter = new DefaultHighlighter.DefaultHighlightPainter(Color.red);
            h.removeAllHighlights();
//            h.install(content);
            while (matcher.find()) {
                try {

                    h.addHighlight(matcher.start(),matcher.end(), painter);
                } catch (BadLocationException e) {
                    e.printStackTrace();
                }
            }

        }
    }

    protected void onError() {}
    
    protected void onNoMatch() {}
    
    protected void onMatch() {}

    private void continueSearch()
    {
        if (matcher != null)
        {
            if (matcher.find())
            {
                handleMatch();
            }
            else
            {
                // wrap search
                matcher.reset();
                if (matcher.find())
                {
                    handleMatch();
                }
                else
                {
                    onNoMatch();
                }
            }
        }
        else
        {
            onNoMatch();
        }
    }
    
    private void handleMatch()
    {
        content.getCaret().setDot(matcher.start());
        content.getCaret().moveDot(matcher.end());
        content.getCaret().setSelectionVisible(true);


        onMatch();
    }
}
