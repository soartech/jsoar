package org.jsoar.debugger;

import java.lang.reflect.InvocationTargetException;

import javax.swing.JTextPane;
import javax.swing.SwingUtilities;
import javax.swing.text.BadLocationException;
import javax.swing.text.DefaultStyledDocument;

public class BatchStyledDocument extends DefaultStyledDocument{
	private static final long serialVersionUID = 1L;

	public void takeBatchUpdate(ElementSpec[] elements) {
		takeBatchUpdate(elements, null);
	}
	
	//If you pass in the output window, it will be scrolled to the end
	public void takeBatchUpdate(ElementSpec[] elements, JTextPane outputWindow) {
		try {
			SwingUtilities.invokeAndWait(new Runnable() {
				@Override
				public void run() {

					try {
						//This function is protected
						insert(getEndPosition().getOffset(), elements);
					} catch (BadLocationException e) {
						e.printStackTrace();
					}
					if (outputWindow != null) {
                        // Scroll to the end
                        outputWindow.setCaretPosition(outputWindow.getDocument().getLength());
                    }
				}
			});
		} catch (InvocationTargetException | InterruptedException e) {
			e.printStackTrace();
		}
	} 
}
