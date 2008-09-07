/*
 * (c) 2008  Dave Ray
 *
 * Created on Aug 14, 2008
 */
package org.jsoar.kernel.parser;

import java.io.IOException;
import java.io.Reader;

public class Lexer implements LexemeTypes
{
    private static final char EOF_AS_CHAR = 0xffff;

    private Reader input;

    private char current_char;

    private int current_column = 0;

    private int current_line = 0;

    private int column_of_start_of_last_lexeme = 0;

    private int line_of_start_of_last_lexeme = 0;

    private boolean allow_ids;

    private int parentheses_level = 0;

    private boolean fake_rparen_at_eol;

    private boolean load_errors_quit = false;

    private Lexeme lexeme = new Lexeme();

    private static final LexerRoutine lex_unknown = new LexerRoutine()
    {
        public void lex(Lexer lexer) throws IOException
        {
            // TODO
            // if(reading_from_top_level(thisAgent) && thisAgent->current_char
            // == 0) {
            // }
            // else {
            // print (thisAgent, "Error: Unknown character encountered by lexer,
            // code=%d\n",
            // thisAgent->current_char);
            // print (thisAgent, "File %s, line %lu, column %lu.\n",
            // thisAgent->current_file->filename,
            // thisAgent->current_file->current_line,
            // thisAgent->current_file->current_column);
            // if (! reading_from_top_level(thisAgent)) {
            // //respond_to_load_errors (thisAgent);
            // if (thisAgent->load_errors_quit)
            // thisAgent->current_char = EOF_AS_CHAR;
            // }
            // }
            // get_next_char(thisAgent);
            // get_lexeme(thisAgent);
        }
    };

    private LexerRoutine lex_eof = new LexerRoutine()
    {
        public void lex(Lexer lexer) throws IOException
        {
            if (fake_rparen_at_eol)
            {
                do_fake_rparen();
                return;
            }
            lexer.store_and_advance();
            lexer.finish();
            lexer.setLexemeType(EOF_LEXEME);
        }
    };

    private LexerRoutine lex_equal = new LexerRoutine()
    {
        public void lex(Lexer lexer) throws IOException
        {
            /* Lexeme might be "=", or symbol */
            /* Note: this routine relies on = being a constituent character */
            lexer.read_constituent_string();
            if (lexer.lexeme.string.length() == 1)
            {
                lexer.lexeme.type = EQUAL_LEXEME;
                return;
            }
            lexer.determine_type_of_constituent_string();
        }
    };

    private static LexerRoutine lex_ampersand = new LexerRoutine()
    {
        public void lex(Lexer lexer) throws IOException
        {
            /* Lexeme might be "&", or symbol */
            /* Note: this routine relies on & being a constituent character */
            lexer.read_constituent_string();
            if (lexer.lexeme.string.length() == 1)
            {
                lexer.lexeme.type = AMPERSAND_LEXEME;
                return;
            }
            lexer.determine_type_of_constituent_string();
        }
    };

    private static LexerRoutine lex_lparen = new BasicLexerRoutine(
            L_PAREN_LEXEME)
    {
        public void lex(Lexer lexer) throws IOException
        {
            super.lex(lexer);
            lexer.parentheses_level++;
        }
    };

    private static LexerRoutine lex_rparen = new BasicLexerRoutine(
            R_PAREN_LEXEME)
    {
        public void lex(Lexer lexer) throws IOException
        {
            super.lex(lexer);
            if (lexer.parentheses_level > 0)
            {
                lexer.parentheses_level--;
            }
        }
    };

    private static LexerRoutine lex_greater = new LexerRoutine()
    {
        public void lex(Lexer lexer) throws IOException
        {
            /* Lexeme might be ">", ">=", ">>", or symbol */
            /* Note: this routine relies on =,> being constituent characters */
            lexer.read_constituent_string();
            if (lexer.lexeme.string.length() == 1)
            {
                lexer.lexeme.type = GREATER_LEXEME;
                return;
            }
            if (lexer.lexeme.string.length() == 2)
            {
                if (lexer.lexeme.string.charAt(1) == '>')
                {
                    lexer.lexeme.type = GREATER_GREATER_LEXEME;
                    return;
                }
                if (lexer.lexeme.string.charAt(1) == '=')
                {
                    lexer.lexeme.type = GREATER_EQUAL_LEXEME;
                    return;
                }
            }
            lexer.determine_type_of_constituent_string();
        }
    };

    private static LexerRoutine lex_less = new LexerRoutine()
    {
        public void lex(Lexer lexer) throws IOException
        {
            /* Lexeme might be "<", "<=", "<=>", "<>", "<<", or variable */
            /* Note: this routine relies on =,<,> being constituent characters */
            lexer.read_constituent_string();
            if (lexer.lexeme.string.length() == 1)
            {
                lexer.lexeme.type = LESS_LEXEME;
                return;
            }
            if (lexer.lexeme.string.length() == 2)
            {
                if (lexer.lexeme.string.charAt(1) == '>')
                {
                    lexer.lexeme.type = NOT_EQUAL_LEXEME;
                    return;
                }
                if (lexer.lexeme.string.charAt(1) == '=')
                {
                    lexer.lexeme.type = LESS_EQUAL_LEXEME;
                    return;
                }
                if (lexer.lexeme.string.charAt(1) == '<')
                {
                    lexer.lexeme.type = LESS_LESS_LEXEME;
                    return;
                }
            }
            if (lexer.lexeme.string.length() == 3)
            {
                if (lexer.lexeme.string.charAt(1) == '='
                        && lexer.lexeme.string.charAt(2) == '>')
                {
                    lexer.lexeme.type = LESS_EQUAL_GREATER_LEXEME;
                    return;
                }
            }
            lexer.determine_type_of_constituent_string();
        }
    };

    private static LexerRoutine lex_period = new LexerRoutine()
    {
        public void lex(Lexer lexer) throws IOException
        {
            lexer.store_and_advance();
            lexer.finish();
            /*
             * --- if we stopped at '.', it might be a floating-point number, so
             * be careful to check for this case ---
             */
            if (Character.isDigit(lexer.current_char))
            {
                lexer.read_rest_of_floating_point_number();
            }
            if (lexer.lexeme.string.length() == 1)
            {
                lexer.lexeme.type = PERIOD_LEXEME;
                return;
            }
            lexer.determine_type_of_constituent_string();
        }
    };

    private static LexerRoutine lex_plus = new LexerRoutine()
    {
        public void lex(Lexer lexer) throws IOException
        {
            /* Lexeme might be +, number, or symbol */
            /*
             * Note: this routine relies on various things being constituent
             * chars
             */
            boolean could_be_floating_point;

            lexer.read_constituent_string();
            /*
             * --- if we stopped at '.', it might be a floating-point number, so
             * be careful to check for this case ---
             */
            if (lexer.current_char == '.')
            {
                could_be_floating_point = true;
                for (int i = 1; i < lexer.lexeme.string.length(); i++)
                {
                    if (!Character.isDigit(lexer.lexeme.string.charAt(i)))
                    {
                        could_be_floating_point = false;
                    }
                }
                if (could_be_floating_point)
                {
                    lexer.read_rest_of_floating_point_number();
                }
            }
            if (lexer.lexeme.string.length() == 1)
            {
                lexer.lexeme.type = PLUS_LEXEME;
                return;
            }
            lexer.determine_type_of_constituent_string();
        }
    };

    private static LexerRoutine lex_minus = new LexerRoutine()
    {
        public void lex(Lexer lexer) throws IOException
        {
            /* Lexeme might be -, -->, number, or symbol */
            /*
             * Note: this routine relies on various things being constituent
             * chars
             */
            boolean could_be_floating_point;

            lexer.read_constituent_string();
            /*
             * --- if we stopped at '.', it might be a floating-point number, so
             * be careful to check for this case ---
             */
            if (lexer.current_char == '.')
            {
                could_be_floating_point = true;
                for (int i = 1; i < lexer.lexeme.string.length(); i++)
                {
                    if (!Character.isDigit(lexer.lexeme.string.charAt(i)))
                    {
                        could_be_floating_point = false;
                    }
                }
                if (could_be_floating_point)
                {
                    lexer.read_rest_of_floating_point_number();
                }
            }
            if (lexer.lexeme.string.length() == 1)
            {
                lexer.lexeme.type = MINUS_LEXEME;
                return;
            }
            if (lexer.lexeme.string.length() == 3)
            {
                if ((lexer.lexeme.string.charAt(1) == '-')
                        && (lexer.lexeme.string.charAt(2) == '>'))
                {
                    lexer.lexeme.type = RIGHT_ARROW_LEXEME;
                    return;
                }
            }
            lexer.determine_type_of_constituent_string();
        }
    };

    private static LexerRoutine lex_digit = new LexerRoutine()
    {
        public void lex(Lexer lexer) throws IOException
        {
            boolean could_be_floating_point;

            lexer.read_constituent_string();
            /*
             * --- if we stopped at '.', it might be a floating-point number, so
             * be careful to check for this case ---
             */
            if (lexer.current_char == '.')
            {
                could_be_floating_point = true;
                for (int i = 1; i < lexer.lexeme.string.length(); i++)
                {
                    if (!Character.isDigit(lexer.lexeme.string.charAt(i)))
                    {
                        could_be_floating_point = false;
                    }
                }
                if (could_be_floating_point)
                {
                    lexer.read_rest_of_floating_point_number();
                }
            }
            lexer.determine_type_of_constituent_string();
        }
    };

    private static LexerRoutine lex_constituent_string = new LexerRoutine()
    {
        public void lex(Lexer lexer) throws IOException
        {
            lexer.read_constituent_string();
            lexer.determine_type_of_constituent_string();
        }
    };

    private static LexerRoutine lex_vbar = new LexerRoutine()
    {
        public void lex(Lexer lexer) throws IOException
        {
            lexer.lexeme.type = SYM_CONSTANT_LEXEME;
            lexer.get_next_char();
            do
            {
                if ((lexer.current_char == EOF_AS_CHAR))
                {

                    // TODO
                    // print (thisAgent, "Error: opening '|' without closing
                    // '|'\n");
                    // print_location_of_most_recent_lexeme(thisAgent);
                    // /* BUGBUG if reading from top level, don't want to signal
                    // EOF */
                    // lexer.lexeme.type = EOF_LEXEME;
                    // thisAgent->lexeme.string[0]=EOF_AS_CHAR;
                    // thisAgent->lexeme.string[1]=0;
                    // thisAgent->lexeme.length = 1;
                    // return;
                }
                if (lexer.current_char == '\\')
                {
                    lexer.get_next_char();
                    lexer.lexeme.string += lexer.current_char;
                    lexer.get_next_char();
                }
                else if (lexer.current_char == '|')
                {
                    lexer.get_next_char();
                    break;
                }
                else
                {
                    lexer.lexeme.string += lexer.current_char;
                    lexer.get_next_char();
                }
            } while (true);
        }
    };

    private static LexerRoutine lex_quote = new LexerRoutine()
    {
        public void lex(Lexer lexer) throws IOException
        {
            lexer.lexeme.type = QUOTED_STRING_LEXEME;
            lexer.get_next_char();
            do
            {
                if ((lexer.current_char == EOF_AS_CHAR))
                {

                    // TODO
                    // print (thisAgent, "Error: opening '\"' without closing
                    // '\"'\n");
                    // print_location_of_most_recent_lexeme(thisAgent);
                    // /* BUGBUG if reading from top level, don't want to signal
                    // EOF */
                    // thisAgent->lexeme.type = EOF_LEXEME;
                    // thisAgent->lexeme.string[0]=EOF_AS_CHAR;
                    // thisAgent->lexeme.string[1]=0;
                    // thisAgent->lexeme.length = 1;
                    return;
                }
                if (lexer.current_char == '\\')
                {
                    lexer.get_next_char();
                    lexer.lexeme.string += lexer.current_char;
                    lexer.get_next_char();
                }
                else if (lexer.current_char == '"')
                {
                    lexer.get_next_char();
                    break;
                }
                else
                {
                    lexer.lexeme.string += lexer.current_char;
                    lexer.get_next_char();
                }
            } while (true);
        }
    };

    /*
     * There are 2 functions here, for 2 different schemes for handling the
     * shell escape. Scheme 1: A '$' signals that all the rest of the text up to
     * the '\n' is to be passed to the system() command verbatim. The whole
     * string, including the '$' as its first character, is stored in a single
     * lexeme which has the type DOLLAR_STRING_LEXEME. Scheme 2: A '$' is a
     * single lexeme, much like a '(' or '&'. All the subsequent lexemes are
     * gotten individually with calls to get_lexeme(). This makes it easier to
     * parse the shell command, so that commands like cd, pushd, popd, etc. can
     * be trapped and the equivalent Soar commands executed instead. The problem
     * with this scheme is that pulling the string apart into lexemes eliminates
     * any special spacing the user may have done in specifying the shell
     * command. For that reason, my current plan is to follow scheme 1. AGR
     * 3-Jun-94
     */
    private static LexerRoutine lex_dollar = new LexerRoutine()
    {
        public void lex(Lexer lexer) throws IOException
        {
            lexer.lexeme.type = DOLLAR_STRING_LEXEME;
            lexer.lexeme.string = "$";
            lexer.get_next_char(); /* consume the '$' */
            while ((lexer.current_char != '\n')
                    && (lexer.current_char != EOF_AS_CHAR))
            {
                lexer.lexeme.string += lexer.current_char;
                lexer.get_next_char();
            }
        }
    };

    private final LexerRoutine[] lexer_routines = new LexerRoutine[/* 256 */] {
            lex_unknown, lex_unknown, lex_unknown, lex_unknown, lex_unknown,
            lex_unknown, lex_unknown, lex_unknown, lex_unknown, lex_unknown,
            lex_unknown, lex_unknown, lex_unknown, lex_unknown, lex_unknown,
            lex_unknown, lex_unknown, lex_unknown, lex_unknown, lex_unknown,
            lex_unknown, lex_unknown, lex_unknown, lex_unknown, lex_unknown,
            lex_unknown, lex_unknown, lex_unknown, lex_unknown, lex_unknown,
            lex_unknown, lex_unknown, lex_unknown, lex_unknown, lex_unknown,
            lex_unknown, lex_unknown, lex_unknown, lex_unknown, lex_unknown,
            lex_unknown, lex_unknown, lex_unknown, lex_unknown, lex_unknown,
            lex_unknown, lex_unknown, lex_unknown, lex_unknown, lex_unknown,
            lex_unknown, lex_unknown, lex_unknown, lex_unknown, lex_unknown,
            lex_unknown, lex_unknown, lex_unknown, lex_unknown, lex_unknown,
            lex_unknown, lex_unknown, lex_unknown, lex_unknown, lex_unknown,
            lex_unknown, lex_unknown, lex_unknown, lex_unknown, lex_unknown,
            lex_unknown, lex_unknown, lex_unknown, lex_unknown, lex_unknown,
            lex_unknown, lex_unknown, lex_unknown, lex_unknown, lex_unknown,
            lex_unknown, lex_unknown, lex_unknown, lex_unknown, lex_unknown,
            lex_unknown, lex_unknown, lex_unknown, lex_unknown, lex_unknown,
            lex_unknown, lex_unknown, lex_unknown, lex_unknown, lex_unknown,
            lex_unknown, lex_unknown, lex_unknown, lex_unknown, lex_unknown,
            lex_unknown, lex_unknown, lex_unknown, lex_unknown, lex_unknown,
            lex_unknown, lex_unknown, lex_unknown, lex_unknown, lex_unknown,
            lex_unknown, lex_unknown, lex_unknown, lex_unknown, lex_unknown,
            lex_unknown, lex_unknown, lex_unknown, lex_unknown, lex_unknown,
            lex_unknown, lex_unknown, lex_unknown, lex_unknown, lex_unknown,
            lex_unknown, lex_unknown, lex_unknown, lex_unknown, lex_unknown,
            lex_unknown, lex_unknown, lex_unknown, lex_unknown, lex_unknown,
            lex_unknown, lex_unknown, lex_unknown, lex_unknown, lex_unknown,
            lex_unknown, lex_unknown, lex_unknown, lex_unknown, lex_unknown,
            lex_unknown, lex_unknown, lex_unknown, lex_unknown, lex_unknown,
            lex_unknown, lex_unknown, lex_unknown, lex_unknown, lex_unknown,
            lex_unknown, lex_unknown, lex_unknown, lex_unknown, lex_unknown,
            lex_unknown, lex_unknown, lex_unknown, lex_unknown, lex_unknown,
            lex_unknown, lex_unknown, lex_unknown, lex_unknown, lex_unknown,
            lex_unknown, lex_unknown, lex_unknown, lex_unknown, lex_unknown,
            lex_unknown, lex_unknown, lex_unknown, lex_unknown, lex_unknown,
            lex_unknown, lex_unknown, lex_unknown, lex_unknown, lex_unknown,
            lex_unknown, lex_unknown, lex_unknown, lex_unknown, lex_unknown,
            lex_unknown, lex_unknown, lex_unknown, lex_unknown, lex_unknown,
            lex_unknown, lex_unknown, lex_unknown, lex_unknown, lex_unknown,
            lex_unknown, lex_unknown, lex_unknown, lex_unknown, lex_unknown,
            lex_unknown, lex_unknown, lex_unknown, lex_unknown, lex_unknown,
            lex_unknown, lex_unknown, lex_unknown, lex_unknown, lex_unknown,
            lex_unknown, lex_unknown, lex_unknown, lex_unknown, lex_unknown,
            lex_unknown, lex_unknown, lex_unknown, lex_unknown, lex_unknown,
            lex_unknown, lex_unknown, lex_unknown, lex_unknown, lex_unknown,
            lex_unknown, lex_unknown, lex_unknown, lex_unknown, lex_unknown,
            lex_unknown, lex_unknown, lex_unknown, lex_unknown, lex_unknown,
            lex_unknown, lex_unknown, lex_unknown, lex_unknown, lex_unknown,
            lex_unknown, lex_unknown, lex_unknown, lex_unknown, lex_unknown,
            lex_unknown, lex_unknown, lex_unknown, lex_unknown, lex_unknown,
            lex_unknown };

    private static final boolean constituent_char[] = new boolean[256];

    private static final boolean number_starters[] = new boolean[256];

    public Lexer(Reader reader) throws IOException
    {
        this.input = reader;
        init_lexer();
        get_next_char();
    }
    
    public Lexeme getCurrentLexeme()
    {
        return lexeme;
    }

    public boolean isEof()
    {
        return lexeme.type == LexemeTypes.EOF_LEXEME;
    }
    
    /*
     * ======================================================================
     * Get next char
     * 
     * Get_next_char() gets the next character from the current input file and
     * puts it into the agent variable current_char.
     * ======================================================================
     */

    private void get_next_char() throws IOException
    {
        if (current_char == EOF_AS_CHAR)
        {
            return;
        }

        int next_char = input.read();
        current_char = next_char >= 0 ? (char) next_char : EOF_AS_CHAR;
    }

    /*
     * ======================================================================
     * 
     * Lexer Utility Routines
     * 
     * ======================================================================
     */

    private void record_position_of_start_of_lexeme()
    {
        column_of_start_of_last_lexeme = current_column - 1;
        line_of_start_of_last_lexeme = current_line;
    }

    public void setLexemeType(int type)
    {
        lexeme.type = type;
    }

    public void store_and_advance() throws IOException
    {
        lexeme.append(current_char);
        get_next_char();
    }

    public void finish()
    {
    }

    void read_constituent_string() throws IOException
    {

        while ((current_char != EOF_AS_CHAR) && constituent_char[current_char])
        {
            store_and_advance();
        }
        finish();
    }

    void read_rest_of_floating_point_number() throws IOException
    {
        /*
         * --- at entry, current_char=="."; we read the "." and rest of number
         * ---
         */
        store_and_advance();

        while (Character.isDigit(current_char))
        {
            store_and_advance(); /* string of digits */
        }
        if ((current_char == 'e') || (current_char == 'E'))
        {
            store_and_advance(); /* E */
            if ((current_char == '+') || (current_char == '-'))
            {
                store_and_advance(); /* optional leading + or - */
            }
            while (Character.isDigit(current_char))
            {
                store_and_advance(); /* string of digits */
            }
        }
        finish();
    }

    private static class PossibleLexemeTypes
    {
        boolean possible_id, possible_var, possible_sc, possible_ic,
                possible_fc;
    }

    private boolean determine_type_of_constituent_string()
    {
        PossibleLexemeTypes possibleType = determine_possible_symbol_types_for_string(lexeme.string);

        /* --- check whether it's a variable --- */
        if (possibleType.possible_var)
        {
            lexeme.type = VARIABLE_LEXEME;
            return true;
        }

        /* --- check whether it's an integer --- */
        if (possibleType.possible_ic)
        {
            try
            {
                lexeme.type = INT_CONSTANT_LEXEME;
                lexeme.int_val = Integer.valueOf(lexeme.string);
            }
            catch (NumberFormatException e)
            {

                // TODO
                // print (thisAgent, "Error: bad integer (probably too
                // large)\n");
                // print_location_of_most_recent_lexeme(thisAgent);
                // lexeme.int_val = 0;
                return false;
            }
            return true;
        }

        /* --- check whether it's a floating point number --- */
        if (possibleType.possible_fc)
        {
            try
            {
                lexeme.type = FLOAT_CONSTANT_LEXEME;
                lexeme.float_val = Double.valueOf(lexeme.string);
            }
            catch (NumberFormatException e)
            {
                // TODO
                // print (thisAgent, "Error: bad floating point number\n");
                // print_location_of_most_recent_lexeme(thisAgent);
                lexeme.float_val = 0.0f;
                return false;
            }
            return true;
        }

        /* --- check if it's an identifier --- */
        if (allow_ids && possibleType.possible_id)
        {
            try
            {
                lexeme.id_letter = Character.toUpperCase(lexeme.string
                        .charAt(0));
                lexeme.type = IDENTIFIER_LEXEME;
                lexeme.id_number = Integer.valueOf(lexeme.string.substring(1));
            }
            catch (NumberFormatException e)
            {
                // TODO
                // print (thisAgent, "Error: bad number for identifier (probably
                // too large)\n");
                // print_location_of_most_recent_lexeme(thisAgent);
                lexeme.id_number = 0;
                return false;
            }
            return true;
        }

        /* --- otherwise it must be a symbolic constant --- */
        if (possibleType.possible_sc)
        {
            lexeme.type = SYM_CONSTANT_LEXEME;
            if (true /* TODO printWarnings */)
            {
                if (lexeme.string.charAt(0) == '<')
                {
                    if (lexeme.string.charAt(1) == '<')
                    {
                        // print (thisAgent, "Warning: Possible disjunctive
                        // encountered in reading symbolic constant\n");
                        // print (thisAgent, " If a disjunctive was intended,
                        // add a space after <<\n");
                        // print (thisAgent, " If a constant was intended,
                        // surround constant with vertical bars\n");
                        //
                        // xml_generate_warning(thisAgent, "Warning: Possible
                        // disjunctive encountered in reading symbolic
                        // constant.\n If a disjunctive was intended, add a
                        // space after &lt;&lt;\n If a constant was intended,
                        // surround constant with vertical bars.");
                        // TODO: should this be appended to previous XML
                        // message, or should it be a separate message?
                        print_location_of_most_recent_lexeme();
                    }
                    else
                    {
                        // print (thisAgent, "Warning: Possible variable
                        // encountered in reading symbolic constant\n");
                        // print (thisAgent, " If a constant was intended,
                        // surround constant with vertical bars\n");
                        //
                        // xml_generate_warning(thisAgent, "Warning: Possible
                        // variable encountered in reading symbolic constant.\n
                        // If a constant was intended, surround constant with
                        // vertical bars.");
                        // TODO: should this be appended to previous XML
                        // message, or should it be a separate message?
                        print_location_of_most_recent_lexeme();
                    }
                }
                else
                {
                    if (lexeme.string.charAt(lexeme.string.length() - 1) == '>')
                    {
                        if (lexeme.string.charAt(lexeme.string.length() - 2) == '>')
                        {
                            // print (thisAgent, "Warning: Possible disjunctive
                            // encountered in reading symbolic constant\n");
                            // print (thisAgent, " If a disjunctive was
                            // intended, add a space before >>\n");
                            // print (thisAgent, " If a constant was intended,
                            // surround constant with vertical bars\n");
                            //
                            // xml_generate_warning(thisAgent, "Warning:
                            // Possible disjunctive encountered in reading
                            // symbolic constant.\n If a disjunctive was
                            // intended, add a space before &gt;&gt;\n If a
                            // constant was intended, surround constant with
                            // vertical bars.");
                            // TODO: should this be appended to previous XML
                            // message, or should it be a separate message?
                            print_location_of_most_recent_lexeme();

                        }
                        else
                        {
                            // print (thisAgent, "Warning: Possible variable
                            // encountered in reading symbolic constant\n");
                            // print (thisAgent, " If a constant was intended,
                            // surround constant with vertical bars\n");
                            //
                            // xml_generate_warning(thisAgent, "Warning:
                            // Possible variable encountered in reading symbolic
                            // constant.\n If a constant was intended, surround
                            // constant with vertical bars.");
                            // //TODO: should this be appended to previous XML
                            // message, or should it be a separate message?
                            // print_location_of_most_recent_lexeme(thisAgent);

                            // TODO: generate tagged output in
                            // print_location_of_most_recent_lexeme
                        }
                    }
                }
            }
            return true;
        }

        lexeme.type = QUOTED_STRING_LEXEME;
        return true;
    }

    private void do_fake_rparen()
    {
        record_position_of_start_of_lexeme();
        lexeme.type = R_PAREN_LEXEME;
        lexeme.string = ")";
        if (parentheses_level > 0)
        {
            parentheses_level--;
        }
        fake_rparen_at_eol = false;
    }

    /*
     * ======================================================================
     * Lex such-and-such Routines
     * 
     * These routines are called from get_lexeme(). Which routine gets called
     * depends on the first character of the new lexeme being read. Each
     * routine's job is to finish reading the lexeme and store the necessary
     * items in the agent variable "lexeme".
     * ======================================================================
     */

    /*
     * ======================================================================
     * Get lexeme
     * 
     * This is the main routine called from outside the lexer. It reads past any
     * whitespace, then calls some lex_xxx routine (using the lexer_routines[]
     * table) based on the first character of the lexeme.
     * ======================================================================
     */

    public void getNextLexeme() throws IOException
    {

        lexeme.string = "";

        load_errors_quit = false; /* AGR 527c */

        while (load_errors_quit == false)
        { /* AGR 527c */
            if (current_char == EOF_AS_CHAR)
                break;
            if (Character.isWhitespace(current_char))
            {
                if (current_char == '\n')
                {
                    if (fake_rparen_at_eol)
                    {
                        do_fake_rparen();
                        return;
                    }

                }
                get_next_char();
                continue;
            }

            // #ifdef USE_TCL
            if (current_char == ';')
            {
                /* --- skip the semi-colon, forces newline in TCL --- */
                get_next_char(); /* consume it */
                continue;
            }
            if (current_char == '#')
            {
                /* --- read from hash to end-of-line --- */
                while ((current_char != '\n') && (current_char != EOF_AS_CHAR))
                    get_next_char();
                if (fake_rparen_at_eol)
                {
                    do_fake_rparen();
                    return;
                }
                if (current_char != EOF_AS_CHAR)
                {
                    get_next_char();
                }
                continue;
            }
            break; /* if no whitespace or comments found, break out of the loop */
        }
        /* --- no more whitespace, so go get the actual lexeme --- */
        record_position_of_start_of_lexeme();
        if (current_char != EOF_AS_CHAR)
        {
            lexer_routines[current_char].lex(this);
        }
        else
        {
            lex_eof.lex(this);
        }
    }

    /*
     * ======================================================================
     * Init lexer
     * 
     * This should be called before anything else in this file. It does all the
     * necessary init stuff for the lexer, and starts the lexer reading from
     * standard input.
     * ======================================================================
     */

    //
    // This file badly need to be locked. Probably not the whole thing, but
    // certainly the last
    // call to start_lext_from_file. It does a memory allocation and other
    // things that should
    // never happen more than once.
    //
    private void init_lexer()
    {

        /* --- setup constituent_char array --- */
        String extra_constituents = "$%&*+-/:<=>?_";
        for (int i = 0; i < 256; i++)
        {
            //
            // When i == 1, strchr returns true based on the terminating
            // character. This is not the intent, so we exclude that case
            // here.
            //
            if (i != 0 && extra_constituents.indexOf((char) i) != -1)
            {
                constituent_char[i] = true;
            }
            else
            {
                constituent_char[i] = Character.isLetterOrDigit((char) i);
            }
        }

        // for (i=0; i<strlen(extra_constituents); i++)
        // {
        // constituent_char[(int)extra_constituents[i]]=TRUE;
        // }

        /* --- setup number_starters array --- */
        for (int i = 0; i < 256; i++)
        {
            switch (i)
            {
            case '+':
                number_starters[(int) '+'] = true;
                break;
            case '-':
                number_starters[(int) '-'] = true;
                break;
            case '.':
                number_starters[(int) '.'] = true;
                break;
            default:
                number_starters[i] = Character.isDigit((char) i);
            }
        }

        /* --- setup lexer_routines array --- */
        //
        // I go to some effort here to insure that values do not
        // get overwritten. That could cause problems in a multi-
        // threaded sense because values could get switched to one
        // value and then another. If a value is only ever set to
        // one thing, resetting it to the same thing should be
        // perfectly safe.
        //
        for (int i = 0; i < 256; i++)
        {
            switch (i)
            {
            case '@':
                lexer_routines[(int) '@'] = new BasicLexerRoutine(AT_LEXEME);
                break;
            case '(':
                lexer_routines[(int) '('] = lex_lparen;
                break;
            case ')':
                lexer_routines[(int) ')'] = lex_rparen;
                break;
            case '+':
                lexer_routines[(int) '+'] = lex_plus;
                break;
            case '-':
                lexer_routines[(int) '-'] = lex_minus;
                break;
            case '~':
                lexer_routines[(int) '~'] = new BasicLexerRoutine(TILDE_LEXEME);
                break;
            case '^':
                lexer_routines[(int) '^'] = new BasicLexerRoutine(
                        UP_ARROW_LEXEME);
                break;
            case '{':
                lexer_routines[(int) '{'] = new BasicLexerRoutine(
                        L_BRACE_LEXEME);
                break;
            case '}':
                lexer_routines[(int) '}'] = new BasicLexerRoutine(
                        R_BRACE_LEXEME);
                break;
            case '!':
                lexer_routines[(int) '!'] = new BasicLexerRoutine(
                        EXCLAMATION_POINT_LEXEME);
                break;
            case '>':
                lexer_routines[(int) '>'] = lex_greater;
                break;
            case '<':
                lexer_routines[(int) '<'] = lex_less;
                break;
            case '=':
                lexer_routines[(int) '='] = lex_equal;
                break;
            case '&':
                lexer_routines[(int) '&'] = lex_ampersand;
                break;
            case '|':
                lexer_routines[(int) '|'] = lex_vbar;
                break;
            case ',':
                lexer_routines[(int) ','] = new BasicLexerRoutine(COMMA_LEXEME);
                break;
            case '.':
                lexer_routines[(int) '.'] = lex_period;
                break;
            case '"':
                lexer_routines[(int) '"'] = lex_quote;
                break;
            case '$':
                lexer_routines[(int) '$'] = lex_dollar; /* AGR 562 */
                break;
            default:
                if (Character.isDigit((char) i))
                {
                    lexer_routines[i] = lex_digit;
                    continue;
                }

                if (constituent_char[i])
                {
                    lexer_routines[i] = lex_constituent_string;
                    continue;
                }
            }
        }

        /* --- initially we're reading from the standard input --- */
        // TODO start_lex_from_file (thisAgent, "[standard input]", stdin);
    }

    /*
     * ======================================================================
     * Print location of most recent lexeme
     * 
     * This routine is used to print an indication of where a parser or
     * interface command error occurred. It tries to print out the current
     * source line with a pointer to where the error was detected. If the
     * current source line is no longer available, it just prints out the line
     * number instead.
     * 
     * BUGBUG: if the input line contains any tabs, the pointer comes out in the
     * wrong place.
     * ======================================================================
     */

    private void print_location_of_most_recent_lexeme()
    {
        // TODO
        // int i;
        //
        // if (thisAgent->current_file->line_of_start_of_last_lexeme ==
        // thisAgent->current_file->current_line) {
        // /* --- error occurred on current line, so print out the line --- */
        // if (! reading_from_top_level(thisAgent)) {
        // print (thisAgent, "File %s, line %lu:\n",
        // thisAgent->current_file->filename,
        // thisAgent->current_file->current_line);
        // /* respond_to_load_errors (); AGR 527a */
        // }
        // if
        // (thisAgent->current_file->buffer[strlen(thisAgent->current_file->buffer)-1]=='\n')
        // print_string (thisAgent, thisAgent->current_file->buffer);
        // else
        // print (thisAgent, "%s\n",thisAgent->current_file->buffer);
        // for (i=0; i<thisAgent->current_file->column_of_start_of_last_lexeme;
        // i++)
        // print_string (thisAgent, "-");
        // print_string (thisAgent, "^\n");
        //
        // if (! reading_from_top_level(thisAgent)) {
        // //respond_to_load_errors (thisAgent); /* AGR 527a */
        // if (thisAgent->load_errors_quit)
        // thisAgent->current_char = EOF_AS_CHAR;
        // }
        //
        // /* AGR 527a The respond_to_load_errors call came too early (above),
        // and the "continue" prompt appeared before the offending line was
        // printed
        // out, so the respond_to_load_errors call was moved here.
        // AGR 26-Apr-94 */
        //
        // } else {
        // /* --- error occurred on a previous line, so just give the position
        // --- */
        // print (thisAgent, "File %s, line %lu, column %lu.\n",
        // thisAgent->current_file->filename,
        // thisAgent->current_file->line_of_start_of_last_lexeme,
        // thisAgent->current_file->column_of_start_of_last_lexeme + 1);
        // if (! reading_from_top_level(thisAgent)) {
        // //respond_to_load_errors (thisAgent);
        // if (thisAgent->load_errors_quit)
        // thisAgent->current_char = EOF_AS_CHAR;
        // }
        // }
    }

    /*
     * ======================================================================
     * Parentheses Utilities
     * 
     * Skip_ahead_to_balanced_parentheses() eats lexemes until the appropriate
     * closing paren is found (0 means eat until back at the top level).
     * 
     * Fake_rparen_at_next_end_of_line() tells the lexer to insert a fake
     * R_PAREN_LEXEME token the next time it reaches the end of a line.
     * ======================================================================
     */

    /*
     * ======================================================================
     * Set lexer allow ids
     * 
     * This routine should be called to tell the lexer whether to allow
     * identifiers to be read. If FALSE, things that look like identifiers will
     * be returned as SYM_CONSTANT_LEXEME's instead.
     * ======================================================================
     */

    public void setAllowIds(boolean allow_identifiers)
    {
        this.allow_ids = allow_identifiers;
    }

    /*
     * ======================================================================
     * Determine possible symbol types for string
     * 
     * This is a utility routine which figures out what kind(s) of symbol a
     * given string could represent. At entry: s, length_of_s represent the
     * string. At exit: possible_xxx is set to TRUE/FALSE to indicate whether
     * the given string could represent that kind of symbol; rereadable is set
     * to TRUE indicating whether the lexer would read the given string as a
     * symbol with exactly the same name (as opposed to treating it as a special
     * lexeme like "+", changing upper to lower case, etc.
     * ======================================================================
     */

    private PossibleLexemeTypes determine_possible_symbol_types_for_string(
            String s)
    {

        PossibleLexemeTypes p = new PossibleLexemeTypes();

        if (s.length() == 0)
        {
            return p;
        }

        /* --- check if it's an integer or floating point number --- */
        if (number_starters[s.charAt(0)])
        {
            try
            {
                Integer.parseInt(s);
                p.possible_ic = true;
            }
            catch (NumberFormatException e)
            {
            }
            try
            {
                Float.parseFloat(s);
                p.possible_fc = s.indexOf('.') != -1;
            }
            catch (NumberFormatException e)
            {
            }
        }

        /* --- make sure it's entirely constituent characters --- */
        for (int i = 0; i < s.length(); ++i)
        {
            if (!constituent_char[s.charAt(i)])
            {
                return p;
            }
        }

        /* --- any string of constituents could be a sym constant --- */
        p.possible_sc = true;

        /* --- check whether it's a variable --- */
        if ((s.charAt(0) == '<') && (s.charAt(s.length() - 1) == '>'))
        {
            p.possible_var = true;
        }

        /* --- check if it's an identifier --- */
        if (Character.isLetter(s.charAt(0)))
        {
            /* --- is the rest of the string an integer? --- */
            int i = 1;
            while (i < s.length() && Character.isDigit(s.charAt(i)))
            {
                ++i;
            }
            p.possible_id = i == s.length();
        }
        return p;
    }

}
