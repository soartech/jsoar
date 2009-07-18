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
