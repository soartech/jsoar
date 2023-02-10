package org.jsoar.debugger;

import java.util.TreeSet;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;

import javax.swing.text.BadLocationException;
import javax.swing.text.DefaultStyledDocument;
import javax.swing.text.Position;

import org.jsoar.debugger.syntax.Highlighter;
import org.jsoar.debugger.syntax.StyleOffset;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class BatchStyledDocument extends DefaultStyledDocument
{
    private static final long serialVersionUID = 1L;
    private static final Logger LOG = LoggerFactory.getLogger(BatchStyledDocument.class);
    
    private final Highlighter highlighter;
    private final JSoarDebugger debugger;
    
    public BatchStyledDocument(Highlighter highlighter, JSoarDebugger debugger)
    {
        this.highlighter = highlighter;
        this.debugger = debugger;
        highlightingThread.setDaemon(true);
        highlightingThread.start();
    }
    
    private final class NewText
    {
        public final Position offset;
        public final String text;
        
        public NewText(Position offset, String text)
        {
            this.offset = offset;
            this.text = text;
        }
    }
    
    private final BlockingQueue<NewText> newTexts = new LinkedBlockingQueue<>();
    
    private final BatchStyledDocument self = this;
    private final Thread highlightingThread = new Thread("Document Highlight Thread")
    {
        public void run()
        {
            while(!Thread.interrupted())
            {
                NewText newText = null;
                try
                {
                    newText = newTexts.take();
                }
                catch(InterruptedException e)
                {
                    LOG.error("Syntax highlighting thread interrupted!", e);
                    Thread.currentThread().interrupt();
                    break;
                }
                
                // This shouldn't happen but just in case
                if(newText == null)
                {
                    LOG.error("Retrieved null from text queue!");
                    continue;
                }
                
                highlightNewText(newText);
            }
            throw new RuntimeException("Highlighting thread has died");
        }
    };
    
    public void reformatText()
    {
        // TODO Auto-generated method stub
        
    }
    
    public void takeDelayedBatchUpdate(String text, boolean highlightText)
    {
        NewText newText = null;
        try
        {
            writeLock();
            Position endPos = getEndPosition();
            int offset = endPos.getOffset() - 1;
            Position offsetPos = createPosition(offset - 1);
            // This function is protected and gets it's own writelocks but they are cheap if you already have them
            insertString(offset, text, highlighter.getDefaultAttributes());
            // This needs to be between writelocks to keep the offset correct
            if(highlightText)
            {
                newText = new NewText(offsetPos, text);
            }
        }
        catch(BadLocationException e)
        {
            BatchStyledDocument.LOG.info("Bad Location offset = {} {} {}", getEndPosition(), getLength(), e);
        }
        finally
        {
            writeUnlock();
        }
        
        if(newText != null)
        {
            boolean result = newTexts.offer(newText);
            if(!result)
            {
                LOG.error("This should never block so you should never see this message!");
            }
        }
    }
    
    public void takeBatchUpdate(String text, boolean highlightText)
    {
        writeLock();
        int offset = getEndPosition().getOffset();
        offset--;
        // BatchStyledDocument.logger.info("Offset = {} {} {}", offset, getEndPosition(), getLength());
        try
        {
            // This function is protected and gets it's own writelocks but they are cheap if you already have them
            insertString(offset, text, highlighter.getDefaultAttributes());
            // This needs to be between writelocks to keep the offset correct
            if(highlightText)
            {
                highlightNewText(offset, text);
            }
        }
        catch(BadLocationException e)
        {
            BatchStyledDocument.LOG.info("Bad Location offset = {} {} {}", offset, getEndPosition(), getLength(), e);
        }
        finally
        {
            writeUnlock();
        }
    }
    
    private void highlightNewText(NewText newText)
    {
        // logger.info("DHT received new text |{}|", newText.text);
        final TreeSet<StyleOffset> styles = highlighter.getPatterns().getForAll(newText.text, debugger);
        if(!styles.isEmpty())
        {
            int index = 0;
            // FIXME - this is SLOW!!!!
            for(StyleOffset styleOffset : styles)
            {
                int start = styleOffset.start;
                int end = styleOffset.end;
                if(start >= end || start < index)
                {
                    // At the moment I'm not sure if this is an error or something else...
                    continue;
                }
                
                writeLock();
                int offset = newText.offset.getOffset() + 1;
                self.setCharacterAttributes(offset + index + start - index, end - start,
                        styleOffset.style, true);
                writeUnlock();
                
                index = end;
            }
        }
    }
    
    private void highlightNewText(int offset, String text)
    {
        // logger.info("DHT received new text |{}|", newText.text);
        final TreeSet<StyleOffset> styles = highlighter.getPatterns().getForAll(text, debugger);
        if(!styles.isEmpty())
        {
            int index = 0;
            // FIXME - this is SLOW!!!!
            for(StyleOffset styleOffset : styles)
            {
                int start = styleOffset.start;
                int end = styleOffset.end;
                if(start >= end || start < index)
                {
                    // At the moment I'm not sure if this is an error or something else...
                    continue;
                }
                
                self.setCharacterAttributes(offset + index + start - index, end - start,
                        styleOffset.style, true);
                
                index = end;
            }
        }
    }
    
    public void trim(int limit, int limitTolerance)
    {
        if(limit > 0)
        {
            // Locking here so that the int returned by getLength is still valid in the remove call
            writeLock();
            final int length = getLength();
            if(length > limit + limitTolerance)
            {
                try
                {
                    // Trim the trace back down to limit
                    remove(0, length - limit);
                }
                catch(BadLocationException e)
                {
                }
            }
            writeUnlock();
        }
    }
    
    // Exposing write lock functionality (seems to improve the performance of the caret position setting code
    public void documentWriteLock()
    {
        this.writeLock();
    }
    
    public void documentWriteUnlock()
    {
        this.writeUnlock();
    }
}
