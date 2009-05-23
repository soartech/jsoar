package org.jsoar.kernel.memory;

import java.io.IOException;
import java.io.StringReader;

import org.jsoar.kernel.Agent;
import org.jsoar.kernel.parser.original.Lexeme;
import org.jsoar.kernel.parser.original.LexemeType;
import org.jsoar.kernel.parser.original.Lexer;
import org.jsoar.kernel.symbols.Identifier;
import org.jsoar.kernel.symbols.SymbolFactory;
import org.jsoar.kernel.tracing.Printer;

import com.google.common.base.Predicate;

public class WorkingMemoryPatternReader
{

    public static Predicate<Wme> GetPredicate(Agent context, String pattern) throws Exception {
		Printer printer = context.getPrinter();
		StringReader reader = new StringReader(pattern);
		SymbolFactory syms = context.getSymbols();
		
		Lexer lex = null;
		// assume null (any value), and override below as necessary
		Identifier id = null;
        Object attr = null;
        Object value = null;
        
		try
        {
			lex = new Lexer(printer, reader);
			lex.setAllowIds(true);
			lex.getNextLexeme();
	        Lexeme idlexeme = lex.getCurrentLexeme();
	        if(idlexeme.type == LexemeType.IDENTIFIER)
	        {
	            id = syms.findIdentifier(idlexeme.id_letter, idlexeme.id_letter);
	            if(id == null)
	            {
	                // TODO: This should be a new exception type
	                throw new Exception("No such id");
	            }
	        }
	        else if(!idlexeme.toString().equals("*"))
	        {
	            // TODO: This should be a new exception type
	            throw new Exception("First entry must be identifier or '*'");
	        }
	        
	        lex.getNextLexeme();
	        Lexeme attrlexeme = lex.getCurrentLexeme();
	        attr = GetPatternValue(syms, attrlexeme);
	        
	        lex.getNextLexeme();
	        Lexeme valuelexeme = lex.getCurrentLexeme();
	        value = GetPatternValue(syms, valuelexeme);
        }
		catch(IOException e)
		{
		    e.printStackTrace();
		}
		
        return Wmes.newMatcher(syms, id, attr, value, -1);
	}
    
    private static Object GetPatternValue(SymbolFactory syms, Lexeme l) throws Exception
    {
        Object value = null;
        switch(l.type)
        {
        case IDENTIFIER:
            value = syms.findIdentifier(l.id_letter, l.id_letter);
            if(value == null)
            {
                // TODO: This should be a new exception type
                throw new Exception("No such id");
            }
            break;
        case SYM_CONSTANT:
            if(!l.toString().equals("*"))
            {
                value = l.toString();
            }
            break;
        case FLOAT:
            value = new Double(l.float_val);
            break;
        case INTEGER:
            value = new Integer(l.int_val);
            break;
        default:
            break;
        }
        
        return value;
    }
}
