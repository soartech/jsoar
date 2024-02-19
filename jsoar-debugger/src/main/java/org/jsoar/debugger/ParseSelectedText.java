/********************************************************************************************
*
* ParseText.java
* 
* Description:
* 
* Created on     Mar 18, 2005
* @author        Douglas Pearson
* 
* Developed by ThreePenny Software <a href="http://www.threepenny.net">www.threepenny.net</a>
********************************************************************************************/
package org.jsoar.debugger;

import java.util.Iterator;

import javax.swing.JMenu;

import org.jsoar.debugger.actions.ExecuteCommandAction;
import org.jsoar.kernel.memory.Wme;
import org.jsoar.kernel.symbols.Identifier;
import org.jsoar.util.adaptables.Adaptables;

/************************************************************************
 * 
 * This class can be used to parse a user's text selection into a more
 * logical construct. It only parses the text immediately around a
 * given selection point. (We use this for the context menu)
 * 
 ************************************************************************/
public class ParseSelectedText
{
    public abstract static class SelectedObject
    {
        public abstract Object retrieveSelection(JSoarDebugger debugger);
        
        public abstract void fillMenu(JSoarDebugger debugger, JMenu parent);
    }
    
    public static class SelectedProduction extends SelectedObject
    {
        private final String m_Name;
        
        public SelectedProduction(String name)
        {
            m_Name = name;
        }
        
        @Override
        public String toString()
        {
            return "Production " + m_Name;
        }
        
        @Override
        public Object retrieveSelection(JSoarDebugger debugger)
        {
            return debugger.getAgent().getProductions().getProduction(m_Name);
        }
        
        /** Fills in menu items that are appropriate for this type of object */
        @Override
        public void fillMenu(JSoarDebugger debugger, JMenu parent)
        {
            parent.add(new ExecuteCommandAction(debugger, String.format("print %s", m_Name)));
            parent.add(new ExecuteCommandAction(debugger, String.format("edit %s", m_Name)));
            parent.addSeparator();
            parent.add(new ExecuteCommandAction(debugger, String.format("production matches %s", m_Name)));
            parent.add(new ExecuteCommandAction(debugger, String.format("production matches --wmes %s", m_Name)));
            parent.addSeparator();
            parent.add(new ExecuteCommandAction(debugger, String.format("production excise %s", m_Name)));
            parent.addSeparator();
            parent.add(new ExecuteCommandAction(debugger, String.format("production break --set %s", m_Name)));
            parent.add(new ExecuteCommandAction(debugger, String.format("production break --clear %s", m_Name)));
        }
    }
    
    public static class SelectedID extends SelectedObject
    {
        private final String m_Name;
        
        public SelectedID(String id)
        {
            m_Name = id;
        }
        
        @Override
        public String toString()
        {
            return "ID " + m_Name;
        }
        
        @Override
        public Object retrieveSelection(JSoarDebugger debugger)
        {
            return debugger.getAgent().getSymbols().findIdentifier(m_Name.charAt(0), Integer.parseInt(m_Name.substring(1)));
        }
        
        /** Fills in menu items that are appropriate for this type of object */
        @Override
        public void fillMenu(JSoarDebugger debugger, JMenu parent)
        {
            parent.add(new ExecuteCommandAction(debugger, String.format("print %s", m_Name)));
            parent.add(new ExecuteCommandAction(debugger, String.format("print --depth 2 %s", m_Name)));
            parent.add(new ExecuteCommandAction(debugger, String.format("print --internal %s", m_Name)));
            parent.add(new ExecuteCommandAction(debugger, String.format("print --exact (* * %s)", m_Name)));
            parent.add(new ExecuteCommandAction(debugger, String.format("preferences %s", m_Name)));
            parent.add(new ExecuteCommandAction(debugger, String.format("preferences --object %s", m_Name)));
        }
    }
    
    public static class SelectedWme extends SelectedObject
    {
        // Some may be null
        private final String m_ID;
        private final String m_Att;
        private final String m_Value;
        
        public SelectedWme(String id, String att, String value)
        {
            m_ID = id;
            m_Att = att;
            m_Value = value;
        }
        
        @Override
        public Object retrieveSelection(JSoarDebugger debugger)
        {
            if(m_ID == null || m_Att == null || m_Value == null)
            {
                return null;
            }
            final Identifier id = debugger.getAgent().getSymbols().findIdentifier(m_ID.charAt(0), Integer.parseInt(m_ID.substring(1)));
            if(id == null)
            {
                return null;
            }
            final String strippedAttr = m_Att.substring(1); // strip off leading caret
            final Iterator<Wme> it = id.getWmes();
            while(it.hasNext())
            {
                final Wme w = it.next();
                // These have to be formatted so that they're escaped properly to match what would show
                // up in the trace window.
                final String attr = String.format("%s", w.getAttribute());
                final String value = String.format("%s", w.getValue());
                if(strippedAttr.equals(attr) && m_Value.equals(value))
                {
                    return w;
                }
            }
            return null;
        }
        
        /** Fills in menu items that are appropriate for this type of object */
        @Override
        public void fillMenu(JSoarDebugger debugger, JMenu parent)
        {
            parent.add(new ExecuteCommandAction(debugger, String.format("preferences %s %s", m_ID, m_Att)));
            parent.add(new ExecuteCommandAction(debugger, String.format("preferences --names %s %s", m_ID, m_Att)));
            parent.add(new ExecuteCommandAction(debugger, String.format("print %s", m_ID)));
            parent.add(new ExecuteCommandAction(debugger, String.format("print %s", m_Value)));
            parent.add(new ExecuteCommandAction(debugger, String.format("print --exact (* %s %s)", m_Att, m_Value)));
            parent.add(new ExecuteCommandAction(debugger, String.format("print --exact (* %s *)", m_Att)));
            final JMenu pfMenu = new JMenu("production-find");
            pfMenu.add(new ExecuteCommandAction(debugger, String.format("pf (<v> %s *)", m_Att)));
            pfMenu.add(new ExecuteCommandAction(debugger, String.format("pf (<v> %s %s)", m_Att, m_Value)));
            pfMenu.add(new ExecuteCommandAction(debugger, String.format("pf (<v> ^* %s)", m_Value)));
            pfMenu.add(new ExecuteCommandAction(debugger, String.format("pf --rhs (<v> %s *)", m_Att)));
            pfMenu.add(new ExecuteCommandAction(debugger, String.format("pf --rhs (<v> %s %s)", m_Att, m_Value)));
            pfMenu.add(new ExecuteCommandAction(debugger, String.format("pf --rhs (<v> ^* %s)", m_Value)));
            parent.add(pfMenu);
        }
        
        @Override
        public String toString()
        {
            return "WME " + m_ID + " " + m_Att + " " + m_Value;
        }
    }
    
    public static class SelectedUnknown extends SelectedObject
    {
        private String m_Name;
        
        public SelectedUnknown(String token)
        {
            m_Name = token;
        }
        
        /*
         * (non-Javadoc)
         * 
         * @see org.jsoar.debugger.ParseSelectedText.SelectedObject#retrieveSelection(org.jsoar.debugger.JSoarDebugger)
         */
        @Override
        public Object retrieveSelection(JSoarDebugger debugger)
        {
            return null;
        }
        
        /** Fills in menu items that are appropriate for this type of object */
        @Override
        public void fillMenu(JSoarDebugger debugger, JMenu parent)
        {
        }
        
        @Override
        public String toString()
        {
            return "Unknown " + m_Name;
        }
    }
    
    // Soar functions on triplets (ID ^att value) so we often need to parse
    // the tokens before and after the current selection position
    protected static final int K_PREV_TOKEN = 0;
    protected static final int K_CURR_TOKEN = 1;
    protected static final int K_NEXT_TOKEN = 2;
    protected String[] m_Tokens = new String[3];
    protected static final char[] kWhiteSpaceChars = { ' ', '\n', '\r', ')', '(', '{', '}' };
    
    // The raw values
    protected String m_FullText;
    protected int m_SelectionStart;
    
    public ParseSelectedText(String content, int selectionStart)
    {
        m_FullText = content;
        m_SelectionStart = selectionStart;
        
        if(m_FullText != null && m_FullText.length() > 0)
        {
            parseTokens();
        }
    }
    
    @Override
    public String toString()
    {
        StringBuilder buffer = new StringBuilder();
        for(int i = 0; i < 3; i++)
        {
            buffer.append("Token " + i + " is |" + m_Tokens[i] + "| ");
        }
        
        return buffer.toString();
    }
    
    /**
     * Finds the first char from the set of chars to occur in string starting from startPos.
     * SLOWSLOW: This implementation is based on indexOf so it handles end cases in exactly the
     * same way as the original but it's slow. If we ever plan to call this repeatedly it should
     * be rewritten.
     */
    protected int indexOfSet(String string, char[] chars, int startPos)
    {
        int min = -1;
        for(char element : chars)
        {
            int index = string.indexOf(element, startPos);
            if(index != -1 && (min == -1 || index < min))
            {
                min = index;
            }
        }
        
        return min;
    }
    
    protected int lastIndexOfSet(String string, char[] chars, int startPos)
    {
        int max = -1;
        for(char element : chars)
        {
            int index = string.lastIndexOf(element, startPos);
            if(index > max)
            {
                max = index;
            }
        }
        
        return max;
    }
    
    protected boolean isProductionNameQuickTest(String token)
    {
        if(token == null || token.length() == 0)
        {
            return false;
        }
        
        // Productions almost always start with a lower case character
        if(token.charAt(0) < 'a' || token.charAt(0) > 'z')
        {
            return false;
        }
        
        // Production names almost always include multiple *'s
        if(token.indexOf('*') == -1)
        {
            return false;
        }
        
        return true;
    }
    
    protected boolean isProductionNameAskKernel(JSoarDebugger debugger, String token)
    {
        if(token == null || token.length() == 0)
        {
            return false;
        }
        
        // Go through prod table model so we can call it safely from any thread.
        final ProductionTableModel prods = Adaptables.adapt(debugger, ProductionTableModel.class);
        return prods != null ? prods.getProduction(token) != null : false;
    }
    
    protected boolean isAttribute(String token)
    {
        if(token == null || token.length() == 0)
        {
            return false;
        }
        
        // Attributes start with "^"
        return token.startsWith("^");
    }
    
    protected String isID(String token)
    {
        if(token == null || token.length() == 0)
        {
            return null;
        }
        
        // There can be leading preference symbols in some cases
        int startId = 0;
        if(token.charAt(0) == '@' ||
                token.charAt(0) == '<' || token.charAt(0) == '>' ||
                token.charAt(0) == '=' || token.charAt(0) == '+' || token.charAt(0) == '-')
        {
            startId = 1;
        }
        
        if(token.length() <= startId)
        {
            return null;
        }
        
        // ID's start with capital letters
        if(token.charAt(startId) < 'A' || token.charAt(startId) > 'Z')
        {
            return null;
        }
        
        // ID's end with numbers
        if(token.charAt(token.length() - 1) < '0' || token.charAt(token.length() - 1) > '9')
        {
            return null;
        }
        
        return token.substring(startId);
    }
    
    /********************************************************************************************
     * 
     * Returns an object representing the parsed text. This object has some Soar structure
     * and the process of deciding which Soar object was clicked on is heuristic in nature.
     * We make an educated guess based on detailed knowledge of how the output trace is displayed.
     * Hopefully, this is one of only a few places where this will happen in the debugger with
     * the new XML representations.
     * 
     **********************************************************************************************/
    public SelectedObject getParsedObject(JSoarDebugger debugger)
    {
        String curr = m_Tokens[K_CURR_TOKEN];
        
        if(curr == null)
        {
            return null;
        }
        
        // Strip bars if they are present (e.g. |print-NAME-production| => print-NAME-production)
        // One could argue that the kernel should handle this but currently it fails to do so, so we'll handle it here.
        if(curr.length() > 2 && curr.charAt(0) == '|' && curr.charAt(curr.length() - 1) == '|')
        {
            curr = curr.substring(1, curr.length() - 1);
        }
        
        if(isProductionNameQuickTest(curr))
        {
            return new SelectedProduction(curr);
        }
        
        if(isAttribute(curr))
        {
            // Search backwards looking for the ID for this attribute.
            // We do this by looking for (X .... ^att name) or
            // (nnnn: X ... ^att name)
            int startPos = m_SelectionStart;
            int openParens = m_FullText.lastIndexOf('(', startPos);
            int colon = m_FullText.lastIndexOf(':', startPos);
            
            // Move to the start of the ID
            if(openParens != -1)
            {
                openParens++;
            }
            if(colon != -1)
            {
                colon += 2;
            }
            
            int idPos = Math.max(openParens, colon);
            
            int endSpace = m_FullText.indexOf(' ', idPos);
            
            if(idPos != -1 && endSpace != -1)
            {
                String id = m_FullText.substring(idPos, endSpace);
                
                String foundId = isID(id);
                
                if(foundId != null)
                {
                    return new SelectedWme(foundId, curr, m_Tokens[K_NEXT_TOKEN]);
                }
            }
            
            // Couldn't find an ID to connect to this wme.
            return new SelectedWme(null, curr, m_Tokens[K_NEXT_TOKEN]);
        }
        
        String foundId = isID(curr);
        if(foundId != null)
        {
            return new SelectedID(foundId);
        }
        
        // As a final test check the string against the real list of production names in the kernel
        // We do this last so that most RHS clicks this doesn't come up.
        if(isProductionNameAskKernel(debugger, curr))
        {
            return new SelectedProduction(curr);
        }
        
        return new SelectedUnknown(curr);
    }
    
    protected boolean isWhiteSpace(char ch)
    {
        for(char kWhiteSpaceChar : kWhiteSpaceChars)
        {
            if(kWhiteSpaceChar == ch)
            {
                return true;
            }
        }
        
        return false;
    }
    
    /** Extract prev, current and next tokens */
    protected void parseTokens()
    {
        int len = m_FullText.length();
        
        // Start by skipping over any white space to get to real content
        while(m_SelectionStart < len && isWhiteSpace(m_FullText.charAt(m_SelectionStart)))
        {
            m_SelectionStart++;
        }
        
        if(m_SelectionStart == len)
        {
            return;
        }
        
        int startPos = m_SelectionStart;
        
        // Move back to the space at the start of the current token
        int back1 = lastIndexOfSet(m_FullText, kWhiteSpaceChars, startPos);
        
        // Now move back to the space at the start of the previous token
        int back2 = lastIndexOfSet(m_FullText, kWhiteSpaceChars, back1 - 1);
        
        // Move to the space at the end of the current token
        int fore1 = indexOfSet(m_FullText, kWhiteSpaceChars, startPos);
        
        // Move to the space at the end of the next token
        int fore2 = indexOfSet(m_FullText, kWhiteSpaceChars, fore1 + 1);
        
        // Handle the end correctly
        if(fore1 == -1)
        {
            fore1 = m_FullText.length();
        }
        if(fore2 == -1)
        {
            fore2 = m_FullText.length();
        }
        
        // Extract the three tokens
        if(back1 != -1)
        {
            m_Tokens[K_PREV_TOKEN] = m_FullText.substring(back2 + 1, back1);
        }
        if(fore2 != -1 && fore1 < fore2)
        {
            m_Tokens[K_NEXT_TOKEN] = m_FullText.substring(fore1 + 1, fore2);
        }
        if(fore1 != -1)
        {
            m_Tokens[K_CURR_TOKEN] = m_FullText.substring(back1 + 1, fore1);
        }
        
        // System.out.println(toString()) ;
    }
}
