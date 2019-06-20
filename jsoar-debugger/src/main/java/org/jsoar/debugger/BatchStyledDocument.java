package org.jsoar.debugger;

import java.lang.reflect.InvocationTargetException;
import java.util.TreeSet;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.atomic.AtomicBoolean;

import javax.swing.JTextPane;
import javax.swing.SwingUtilities;
import javax.swing.event.DocumentEvent;
import javax.swing.event.UndoableEditEvent;
import javax.swing.text.AttributeSet;
import javax.swing.text.BadLocationException;
import javax.swing.text.DefaultStyledDocument;
import javax.swing.text.Element;
import javax.swing.text.MutableAttributeSet;

import org.jsoar.debugger.syntax.Highlighter;
import org.jsoar.debugger.syntax.StyleOffset;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class BatchStyledDocument extends DefaultStyledDocument {
    private static final long serialVersionUID = 1L;
    private static final Logger logger = LoggerFactory.getLogger(BatchStyledDocument.class);

    private final Highlighter highlighter;
    private final JSoarDebugger debugger;
    private final AtomicBoolean colorImmediately;
    private final Object colorLock;

    public BatchStyledDocument(Highlighter highlighter, JSoarDebugger debugger, AtomicBoolean colorImmediately, Object colorLock) {
        this.highlighter = highlighter;
        this.debugger = debugger;
        highlightingThread.setDaemon(true);
        highlightingThread.start();
        this.colorImmediately = colorImmediately;
        this.colorLock = colorLock;
    }

    private final class NewText {
        public final int offset;
        public final String text;

        public NewText(int offset, String text) {
            this.offset = offset;
            this.text = text;
        }
    }

    private final BlockingQueue<NewText> newTexts = new LinkedBlockingQueue<>();

    public void takeDelayedBatchUpdate(String text, JTextPane outputWindow, boolean scrollToBottom) {
        Runnable runnable = new Runnable() {
            @Override
            public void run() {
//                int offset = getEndPosition().getOffset();
//                offset--;
//                //BatchStyledDocument.logger.info("Offset = {} {} {}", offset, getEndPosition(), getLength());
//                try {
//                    //This function is protected
//                    insertString(offset, text, highlighter.getDefaultAttributes());
//                    //This needs to be on the EDT where we still know the offset is correct -ACN
//                    boolean result = newTexts.offer(new NewText(offset, text));
//                    if ( !result ) {
//                        logger.error("This should never block so you should never see this message!");
//                    }
//                } catch (BadLocationException e) {
//                    BatchStyledDocument.logger.info("Offset = {} {} {}", offset, getEndPosition(), getLength());
//                    e.printStackTrace();
//                }
                if (scrollToBottom) {
                    // Scroll to the end
                    BatchStyledDocument.this.writeLock();
                    outputWindow.setCaretPosition(outputWindow.getDocument().getLength());
                    BatchStyledDocument.this.writeUnlock();
                }
            }
        };
        
        writeLock();
        int offset = getEndPosition().getOffset();
        offset--;
        //BatchStyledDocument.logger.info("Offset = {} {} {}", offset, getEndPosition(), getLength());
        try {
            // This function is protected and gets it's own writelocks but they are cheap if you already have them
            insertString(offset, text, highlighter.getDefaultAttributes());
            // This needs to be between writelocks to keep the offset correct
            boolean result = newTexts.offer(new NewText(offset, text));
            if ( !result ) {
                logger.error("This should never block so you should never see this message!");
            }
        } catch (BadLocationException e) {
            BatchStyledDocument.logger.info("Offset = {} {} {}", offset, getEndPosition(), getLength());
            e.printStackTrace();
        } finally {
            writeUnlock();
        }
        
        //On close, this gets purged from the EDT which will cause a deadlock. -ACN
        if (SwingUtilities.isEventDispatchThread()) {
            runnable.run();
        } else {
            try {
                SwingUtilities.invokeAndWait(runnable);
            } catch (InvocationTargetException e) {
                e.printStackTrace();
            } catch (InterruptedException e) {
                //Probably means we're shutting down
                logger.info("The following text was not printed due to thread interrupt.  If you see this message at shutdown, it is safely ignored. " + text);
            }
        }
    }

    private final BatchStyledDocument self = this;
    private final Thread highlightingThread = new Thread("Document Highlight Thread"){
        public void run() {
            while(!Thread.interrupted()) {
                synchronized (colorLock) {
                    NewText newText = null;
                    try {
                        newText = newTexts.take();
                    } catch (InterruptedException e) {
                        logger.error("Syntax highlighting thread interrupted!", e);
                        continue;
                    }
                    
                    // This shouldn't happen but just in case
                    if (newText == null) {
                        logger.error("Retrieved null from text queue!");
                        continue;
                    }
                    
                    //logger.info("DHT received new text |{}|", newText.text);
                    final TreeSet<StyleOffset> styles = highlighter.getPatterns().getForAll(newText.text, debugger);
                    if ( !styles.isEmpty( )) {
                        int index = 0;
                        // FIXME - this is SLOW!!!!
                        for (StyleOffset offset : styles) {
                            int start = offset.start;
                            int end = offset.end;
                            if (start >= end || start < index) {
                                // At the moment I'm not sure if this is an error or something else...
//                                BatchStyledDocument.logger.info("hit strange style offset values start={}, end={}, index={}"
//                                        ,start
//                                        ,end,
//                                        index);
                                continue;
                            }

                            //writeLock();
                            // the stuff between the match
                            //self.setCharacterAttributesUnsafe(newText.offset + index, start - index,
                            //        highlighter.getDefaultAttributes(), true);

                            // the matched stuff
                            self.setCharacterAttributes(newText.offset + index + start - index, end - start,
                                    offset.style, true);

                            index = end;
                            //writeUnlock();
                        }
//                        if (index < newText.text.length()) {
//                            // trailing text after all matches
//                            self.setCharacterAttributes(newText.offset + index, newText.text.length() - index,
//                                    highlighter.getDefaultAttributes(), true);
//                        }
                    }
                }
            }
            throw new RuntimeException("Highlighting thread has died");
        };
    };

    private final void setCharacterAttributesUnsafe(int offset, int length, AttributeSet s, boolean replace) {
        if (length == 0) {
            return;
        }
        try {
            //            writeLock();
            DefaultDocumentEvent changes =
                    new DefaultDocumentEvent(offset, length, DocumentEvent.EventType.CHANGE);

            // split elements that need it
            buffer.change(offset, length, changes);

            AttributeSet sCopy = s.copyAttributes();

            // PENDING(prinz) - this isn't a very efficient way to iterate
            int lastEnd;
            for (int pos = offset; pos < (offset + length); pos = lastEnd) {
                Element run = getCharacterElement(pos);
                lastEnd = run.getEndOffset();
                if (pos == lastEnd) {
                    // offset + length beyond length of document, bail.
                    break;
                }
                MutableAttributeSet attr = (MutableAttributeSet) run.getAttributes();
                changes.addEdit(new AttributeUndoableEdit(run, sCopy, replace));
                if (replace) {
                    attr.removeAttributes(attr);
                }
                attr.addAttributes(s);
            }
            changes.end();
            fireChangedUpdate(changes);
            fireUndoableEditUpdate(new UndoableEditEvent(this, changes));
        } finally {
            //            writeUnlock();
        }
    }
}
