/*
 * Copyright (c) 2009 Dave Ray <daveray@gmail.com>
 *
 * Created on Jul 17, 2009
 */
package org.jsoar.debugger.util;

import java.awt.Color;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.ArrayList;

import javax.swing.JEditorPane;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import javax.swing.text.BadLocationException;
import javax.swing.text.DefaultHighlighter;
import javax.swing.text.Document;
import javax.swing.text.Highlighter;
import javax.swing.text.JTextComponent;
import javax.swing.text.LayeredHighlighter;

import com.google.re2j.Matcher;
import com.google.re2j.Pattern;

public class IncrementalSearch implements DocumentListener, ActionListener
{
    protected JEditorPane content;
    
    protected Matcher matcher;
    
    private boolean useRegex = false;
    private boolean matchCase = false;
    private ArrayList<Match> matches = new ArrayList<>();
    private int currentMatch = -1;
    private Color highlightColor = Color.DARK_GRAY;
    
    public IncrementalSearch(JTextComponent comp)
    {
        this.content = (JEditorPane) comp;
    }
    
    /* DocumentListener implementation */
    public void insertUpdate(DocumentEvent evt)
    {
        Document query_doc = evt.getDocument();
        try
        {
            runNewSearch(query_doc.getText(0, query_doc.getLength()));
        }
        catch(BadLocationException e)
        {
            e.printStackTrace();
        }
    }
    
    public void removeUpdate(DocumentEvent evt)
    {
        Document query_doc = evt.getDocument();
        try
        {
            runNewSearch(query_doc.getText(0, query_doc.getLength()));
        }
        catch(BadLocationException e)
        {
            e.printStackTrace();
        }
    }
    
    public void changedUpdate(DocumentEvent evt)
    {
        Document query_doc = evt.getDocument();
        try
        {
            runNewSearch(query_doc.getText(0, query_doc.getLength()));
        }
        catch(BadLocationException e)
        {
            e.printStackTrace();
        }
    }
    
    /* ActionListener implementation */
    public void actionPerformed(ActionEvent evt)
    {
        continueSearch();
    }
    
    public void runNewSearch(String query)
    {
        try
        {
            
            if(query.length() > 0)
            {
                matches.clear();
                currentMatch = -1;
                
                final Document content_doc = content.getDocument();
                String body = content_doc.getText(0, content_doc.getLength());
                if(useRegex)
                {
                    final Pattern pattern = Pattern.compile(query);
                    matcher = pattern.matcher(body);
                    findAllMatches();
                }
                else
                {
                    matcher = null;
                    if(!matchCase)
                    {
                        body = body.toLowerCase();
                        query = query.toLowerCase();
                    }
                    findAllStringMatches(query, body);
                }
                continueSearch();
            }
            else
            {
                onNoMatch();
                matcher = null;
            }
        }
        catch(BadLocationException e)
        {
            onError();
        }
    }
    
    protected void removeTextHighlights()
    {
        content.getHighlighter().removeAllHighlights();
    }
    
    private void findAllStringMatches(String query, String body)
    {
        Highlighter h = content.getHighlighter();
        
        LayeredHighlighter.LayerPainter painter = new DefaultHighlighter.DefaultHighlightPainter(highlightColor.brighter());
        h.removeAllHighlights();
        
        int i = 0;
        while(i < body.length() + query.length())
        {
            int start = body.indexOf(query, i);
            int end = start + query.length();
            if(start < 0 || end > body.length())
            {
                break;
            }
            try
            {
                Object highlight = h.addHighlight(start, end, painter);
                matches.add(new Match(start, end, highlight));
                i = end;
            }
            catch(BadLocationException e)
            {
                e.printStackTrace();
                break;
            }
        }
    }
    
    private void findAllMatches()
    {
        
        if(matcher != null)
        {
            
            Highlighter h = content.getHighlighter();
            LayeredHighlighter.LayerPainter painter = new DefaultHighlighter.DefaultHighlightPainter(highlightColor.brighter());
            h.removeAllHighlights();
            while(matcher.find())
            {
                try
                {
                    Object highlight = h.addHighlight(matcher.start(), matcher.end(), painter);
                    matches.add(new Match(matcher.start(), matcher.end(), highlight));
                }
                catch(BadLocationException e)
                {
                    e.printStackTrace();
                }
            }
        }
        else
        {
            onError();
        }
    }
    
    protected void onError()
    {
    }
    
    protected void onNoMatch()
    {
    }
    
    protected void onMatch(int match, int total)
    {
    }
    
    protected void continueSearch()
    {
        if(!matches.isEmpty())
        {
            resetHighlight();
            
            currentMatch++;
            if(currentMatch >= matches.size())
            {
                currentMatch = 0;
            }
            handleMatch();
        }
        else
        {
            onNoMatch();
        }
    }
    
    public void findPrev()
    {
        if(!matches.isEmpty())
        {
            resetHighlight();
            currentMatch--;
            if(currentMatch < 0)
            {
                currentMatch = matches.size() - 1;
            }
            handleMatch();
        }
        else
        {
            onNoMatch();
        }
    }
    
    private void handleMatch()
    {
        Match match = matches.get(currentMatch);
        content.getCaret().setDot(match.start);
        content.getCaret().moveDot(match.end);
        content.getCaret().setSelectionVisible(true);
        Highlighter h = content.getHighlighter();
        LayeredHighlighter.LayerPainter painter = new DefaultHighlighter.DefaultHighlightPainter(highlightColor);
        try
        {
            h.removeHighlight(match.highlight);
            match.highlight = h.addHighlight(match.start, match.end, painter);
        }
        catch(BadLocationException e)
        {
            e.printStackTrace();
        }
        
        onMatch(currentMatch, matches.size());
    }
    
    private void resetHighlight()
    {
        if(currentMatch >= 0 && currentMatch < matches.size())
        {
            Match match = matches.get(currentMatch);
            Highlighter h = content.getHighlighter();
            LayeredHighlighter.LayerPainter painter = new DefaultHighlighter.DefaultHighlightPainter(highlightColor.brighter());
            try
            {
                h.removeHighlight(match.highlight);
                match.highlight = h.addHighlight(match.start, match.end, painter);
            }
            catch(BadLocationException e)
            {
                e.printStackTrace();
            }
        }
    }
    
    public void setMatchCase(boolean selected)
    {
        matchCase = selected;
    }
    
    public void setUseRegex(boolean selected)
    {
        useRegex = selected;
    }
    
    public void setHighlightColor(Color color)
    {
        highlightColor = color;
    }
    
    private class Match
    {
        public Object highlight;
        int start;
        int end;
        
        public Match(int start, int end, Object highlight)
        {
            this.start = start;
            this.end = end;
            this.highlight = highlight;
        }
    }
}
