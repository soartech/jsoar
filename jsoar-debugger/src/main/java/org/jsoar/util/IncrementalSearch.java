/*
 * Copyright (c) 2009 Dave Ray <daveray@gmail.com>
 *
 * Created on Jul 17, 2009
 */
package org.jsoar.util;

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.regex.PatternSyntaxException;

import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import javax.swing.text.BadLocationException;
import javax.swing.text.Document;
import javax.swing.text.JTextComponent;

public class IncrementalSearch implements DocumentListener, ActionListener
{
    protected JTextComponent content;

    protected Matcher matcher;

    public IncrementalSearch(JTextComponent comp)
    {
        this.content = comp;
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
            String query = query_doc.getText(0, query_doc.getLength());
            Pattern pattern = Pattern.compile(query);
            Document content_doc = content.getDocument();
            String body = content_doc.getText(0, content_doc.getLength());
            matcher = pattern.matcher(body);
        }
        catch (PatternSyntaxException ex)
        {
            matcher = null;
        }
        catch (BadLocationException e)
        {
            matcher = null;
        }
        continueSearch();
    }

    private void continueSearch()
    {
        if (matcher != null)
        {
            if (matcher.find())
            {
                content.getCaret().setDot(matcher.start());
                content.getCaret().moveDot(matcher.end());
                content.getCaret().setSelectionVisible(true);
            }
            else
            {
                // wrap search
                matcher.reset();
                if (matcher.find())
                {
                    content.getCaret().setDot(matcher.start());
                    content.getCaret().moveDot(matcher.end());
                    content.getCaret().setSelectionVisible(true);
                }
            }
        }
    }
}
