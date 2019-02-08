package org.jsoar.debugger;

import java.lang.reflect.InvocationTargetException;

import javax.swing.JTextPane;
import javax.swing.SwingUtilities;
import javax.swing.text.BadLocationException;
import javax.swing.text.DefaultStyledDocument;

public class BatchStyledDocument extends DefaultStyledDocument{
	private static final long serialVersionUID = 1L;

	public void takeBatchUpdate(ElementSpec[] elements, JTextPane outputWindow) {
		takeBatchUpdate(elements, outputWindow, false);
	}
	
	
	private static final DefaultStyledDocument emptyDocument = new DefaultStyledDocument();
	//If you pass in the output window, it will be scrolled to the end
	public void takeBatchUpdate(ElementSpec[] elements, JTextPane outputWindow, boolean scrollToBottom) {
		final DefaultStyledDocument self = this;
		try {
			SwingUtilities.invokeAndWait(new Runnable() {
				@Override
				public void run() {
					outputWindow.setDocument(emptyDocument);
					try {
						//This function is protected
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
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}
}
