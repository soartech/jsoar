package org.jsoar.debugger;

import java.lang.reflect.InvocationTargetException;
import java.util.TreeSet;
import java.util.concurrent.ConcurrentLinkedQueue;
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
	public void takeBatchUpdate(ElementSpec[] elements, JTextPane outputWindow) {
		takeBatchUpdate(elements, outputWindow, false);
	}

	private static final DefaultStyledDocument emptyDocument = new DefaultStyledDocument();

	// If you pass in the output window, it will be scrolled to the end
	public void takeBatchUpdate(ElementSpec[] elements, JTextPane outputWindow, boolean scrollToBottom) {
		final DefaultStyledDocument self = this;
		try {
			SwingUtilities.invokeAndWait(new Runnable() {
				@Override
				public void run() {
					outputWindow.setDocument(emptyDocument);
					try {
						// This function is protected
						insert(getEndPosition().getOffset(), elements);
					} catch (BadLocationException e) {
						e.printStackTrace();
					}
					outputWindow.setDocument(self);
					if (scrollToBottom) {
						// Scroll to the end
						outputWindow.setCaretPosition(outputWindow.getDocument().getLength());
					}
				}
			});
		} catch (InvocationTargetException | InterruptedException e) {
			e.printStackTrace();
		}
	}

	private final class NewText {
		public final int offset;
		public final String text;

		public NewText(int offset, String text) {
			this.offset = offset;
			this.text = text;
		}
	}

	private final ConcurrentLinkedQueue<NewText> newTexts = new ConcurrentLinkedQueue<>();

	public void takeDelayedBatchUpdate(String text, JTextPane outputWindow, boolean scrollToBottom) {
		Runnable runnable = new Runnable() {
			@Override
			public void run() {
//			outputWindow.setDocument(emptyDocument);
				int offset = getEndPosition().getOffset();
				try {
					//This function is protected
					insertString(offset, text, highlighter.getDefaultAttributes());
					//This needs to be on the EDT where we still know the offset is correct -ACN
					newTexts.offer(new NewText(offset, text));
				} catch (BadLocationException e) {
					e.printStackTrace();
				}
//			outputWindow.setDocument(self);
				if (scrollToBottom) {
		            // Scroll to the end
		            outputWindow.setCaretPosition(outputWindow.getDocument().getLength());
		        }
			}
		};
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
				//If this isn't the active thread, don't act or block 
				if(!colorImmediately.get()) {try {
					Thread.sleep(0);
					continue;
				} catch (InterruptedException e) {}}
				synchronized (colorLock) {
					NewText newText = newTexts.poll();
					if (newText == null) {
						Thread.yield();
						continue;
					}
					final TreeSet<StyleOffset> styles = highlighter.getPatterns().getForAll(newText.text, debugger);
					if (styles.isEmpty()) {
						// no matches, just print the text
						// elements.add(new ElementSpec(highlighter.getDefaultAttributes(),
						// ElementSpec.ContentType, string.toCharArray(), 0, string.length()));
					} else {
						int index = 0;
						// FIXME - this is SLOW!!!!
						for (StyleOffset offset : styles) {
							int start = offset.start;
							int end = offset.end;
							if (start >= end || start < index) {
								System.out.println("hit");
								continue;
							}

							writeLock();
							// the stuff between the match
							self.setCharacterAttributesUnsafe(newText.offset + index, start - index,
									highlighter.getDefaultAttributes(), true);

							// elements.add(
							// new ElementSpec(
							// highlighter.getDefaultAttributes(),
							// ElementSpec.ContentType,
							// string.substring(index, start).toCharArray(),
							// 0,
							// start - index));
							// the matched stuff
							self.setCharacterAttributesUnsafe(newText.offset + index + start - index, end - start,
									offset.style, true);
							// elements.add(
							// new ElementSpec(
							// offset.style,
							// ElementSpec.ContentType,
							// string.substring(start, end).toCharArray(),
							// 0,
							// end - start));
							index = end;
							writeUnlock();
						}
						if (index < newText.text.length()) {
							// trailing text after all matches
							self.setCharacterAttributes(newText.offset + index, newText.text.length() - index,
									highlighter.getDefaultAttributes(), true);
							// elements.add(
							// new ElementSpec(
							// highlighter.getDefaultAttributes(),
							// ElementSpec.ContentType,
							// string.substring(index).toCharArray(),
							// 0,
							// string.length() - index));
						}
					}
				}
//		        elements.add(new ElementSpec(null, ElementSpec.EndTagType));
//		        elements.add(new ElementSpec(highlighter.getDefaultAttributes(), ElementSpec.StartTagType));
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
