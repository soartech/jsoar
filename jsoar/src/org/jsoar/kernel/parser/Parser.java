/*
 * (c) 2008  Dave Ray
 *
 * Created on Aug 15, 2008
 */
package org.jsoar.kernel.parser;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.EnumSet;
import java.util.List;
import java.util.ListIterator;

import org.jsoar.kernel.Production;
import org.jsoar.kernel.ProductionSupport;
import org.jsoar.kernel.ProductionType;
import org.jsoar.kernel.VariableGenerator;
import org.jsoar.kernel.lhs.Condition;
import org.jsoar.kernel.lhs.ConjunctiveNegationCondition;
import org.jsoar.kernel.lhs.ConjunctiveTest;
import org.jsoar.kernel.lhs.DisjunctionTest;
import org.jsoar.kernel.lhs.EqualityTest;
import org.jsoar.kernel.lhs.GoalIdTest;
import org.jsoar.kernel.lhs.ImpasseIdTest;
import org.jsoar.kernel.lhs.NegativeCondition;
import org.jsoar.kernel.lhs.PositiveCondition;
import org.jsoar.kernel.lhs.RelationalTest;
import org.jsoar.kernel.lhs.Test;
import org.jsoar.kernel.lhs.Tests;
import org.jsoar.kernel.lhs.ThreeFieldCondition;
import org.jsoar.kernel.memory.PreferenceType;
import org.jsoar.kernel.rhs.Action;
import org.jsoar.kernel.rhs.FunctionAction;
import org.jsoar.kernel.rhs.MakeAction;
import org.jsoar.kernel.rhs.RhsFunctionCall;
import org.jsoar.kernel.rhs.RhsSymbolValue;
import org.jsoar.kernel.rhs.RhsValue;
import org.jsoar.kernel.symbols.StringSymbolImpl;
import org.jsoar.kernel.symbols.SymbolImpl;
import org.jsoar.kernel.symbols.SymbolFactoryImpl;
import org.jsoar.kernel.symbols.Variable;
import org.jsoar.kernel.tracing.Printer;
import org.jsoar.util.Arguments;
import org.jsoar.util.ByRef;

/**
   <h3>Grammar for left hand sides of productions
   
    <p><pre>{@code
    <lhs> ::= <cond>+
    <cond> ::= <positive_cond> | - <positive_cond>
    <positive_cond> ::= <conds_for_one_id> | { <cond>+ }
    <conds_for_one_id> ::= ( [state|impasse] [<id_test>] <attr_value_tests>* )
    <id_test> ::= <test>
    <attr_value_tests> ::= [-] ^ <attr_test> [.<attr_test>]* <value_test>*
    <attr_test> ::= <test>
    <value_test> ::= <test> [+] | <conds_for_one_id> [+]
    
    <test> ::= <conjunctive_test> | <simple_test>
    <conjunctive_test> ::= { <simple_test>+ }
    <simple_test> ::= <disjunction_test> | <relational_test>
    <disjunction_test> ::= << <constant>* >>
    <relational_test> ::= [<relation>] <single_test>
    <relation> ::= <> | < | > | <= | >= | = | <=>
    <single_test> ::= variable | <constant>
    <constant> ::= sym_constant | int_constant | float_constant
    }</pre>
    
  <h3>Grammar for right hand sides of productions
   <p><pre>{@code
   <rhs> ::= <rhs_action>*
   <rhs_action> ::= ( variable <attr_value_make>+ ) | <function_call>
   <function_call> ::= ( <function_name> <rhs_value>* )
   <function_name> ::= sym_constant | + | -
     (WARNING: might need others besides +, - here if the lexer changes)
   <rhs_value> ::= <constant> | <function_call> | variable
   <constant> ::= sym_constant | int_constant | float_constant
   <attr_value_make> ::= ^ <rhs_value> <value_make>+
   <value_make> ::= <rhs_value> <preferences>

   <preferences> ::= [,] | <preference_specifier>+   
   <preference-specifier> ::= <naturally-unary-preference> [,]
                            | <forced-unary-preference>
                            | <binary-preference> <rhs_value> [,]
   <naturally-unary-preference> ::= + | - | ! | ~ | @
   <binary-preference> ::= > | = | < | &
   <any-preference> ::= <naturally-unary-preference> | <binary-preference>
   <forced-unary-preference> ::= <binary-preference> 
                                 {<any-preference> | , | ) | ^}  
     ;but the parser shouldn't consume the <any-preference>, ")" or "^" 
      lexeme here
    }</pre>
    
 * @author ray
 */
public class Parser
{
    private final Lexer lexer;
    private final Printer printer;
    private final SymbolFactoryImpl syms;
    private final VariableGenerator varGen;
    private final boolean operand2_mode;
    private int[] placeholder_counter = new int[26];
    private StringSymbolImpl currentProduction = null;
    
    public Parser(VariableGenerator varGen, Lexer lexer, boolean operand2_mode)
    {
        Arguments.checkNotNull(varGen, "varGen");
        Arguments.checkNotNull(lexer, "lexer");
        
        this.printer = lexer.getPrinter();
        this.varGen = varGen;
        this.syms = varGen.getSyms();
        this.lexer = lexer;
        this.operand2_mode = operand2_mode;
    }
    
    public Lexer getLexer()
    {
        return lexer;
    }
    
    private void error(String message) throws ParserException
    {
        if(currentProduction != null)
        {
            message += String.format("\n(Ignoring production %s)\n", currentProduction);
        }
        throw new ParserException(message);
    }
    
    /**
     * If the current lexeme has the given type, just consume it and ignore it.
     * 
     * @param type type of lexeme to ignore
     * @throws IOException
     */
    private void consume(LexemeType type) throws IOException
    {
        if (currentType()==type)
        { 
            lexer.getNextLexeme(); 
        }
    }
    
    private void consumeComma() throws IOException
    {
        consume(LexemeType.COMMA);
    }
    
    private void expect(LexemeType type, String context) throws ParserException, IOException
    {
        if (currentType() != type)
        {
            error("Expected " + type.repr() + " " + context + "\n");
            throw new IllegalStateException("Unreachable code");
        }
        lexer.getNextLexeme();
    }
    
    private Lexeme current()
    {
        return lexer.getCurrentLexeme();
    }
    
    private LexemeType currentType()
    {
        return current().type;
    }
    
    private SymbolImpl make_symbol_for_current_lexeme () 
    {
        switch (currentType()) 
        {
        case SYM_CONSTANT:  return syms.createString (current().string);
        case VARIABLE:  return syms.make_variable (current().string);
        case INTEGER:  return syms.createInteger (current().int_val);
        case FLOAT:  return syms.createDouble (current().float_val);
        
        case IDENTIFIER: throw new IllegalStateException("Internal error:  ID found in make_symbol_for_current_lexeme");
        default: throw new IllegalStateException("bad lexeme type in make_symbol_for_current_lexeme: " + current());
        }
    }

    /**
     * In attribute paths (and some other places) we need to create dummy
     * variables. But we need to make sure these dummy variables don't
     * accidently have the same names as variables that occur later in the
     * user's production. So, we create "placeholder" variables, whose names
     * have funky characters in them so they couldn't possibly occur in
     * user-written code. When we're all done parsing the production, we go back
     * and replace the placeholder variables with "real" variables (names
     * without funky characters), making sure the real variables don't occur
     * anywhere else in the production.
     * 
     */
    private void reset_placeholder_variable_generator()
    {
        for (int i = 0; i < placeholder_counter.length; i++)
        {
            placeholder_counter[i] = 1;
        }
    }

    /**
     * Creates and returns a test for equality with a newly generated
     * placeholder variable.
     * 
     * @param first_letter
     * @return
     */
    private Test make_placeholder_test(char first_letter)
    {
        first_letter = Character.isLetter(first_letter) ? Character.toLowerCase(first_letter) : 'v';

        // create variable with "#" in its name:  this couldn't possibly be a
        // variable in the user's code, since the lexer doesn't handle "#"
        final String namebuf = ("<#" + first_letter) + (placeholder_counter[first_letter - 'a']++ + ">");

        final Variable new_var = syms.make_variable(namebuf);
        // indicate that there is no corresponding "real" variable yet
        new_var.current_binding_value = null;

        // return an equality test for that variable
        return SymbolImpl.makeEqualityTest(new_var);
    }

    /**
     * When done parsing the production, we go back and substitute "real"
     * variables for all the placeholders. This is done by walking all the LHS
     * conditions and destructively modifying any tests involving placeholders.
     * The placeholder-->real mapping is maintained on each placeholder symbol:
     * placeholder->var.current_binding_value is the corresponding "real"
     * variable, or null if no such "real" variable has been created yet.
     * 
     * <p>To use this, first call reset_variable_generator (lhs, rhs) with the lhs
     * and rhs of the production just parsed; then call
     * substitute_for_placeholders_in_condition_list (lhs).
     * 
     * <p>parser.cpp::substitute_for_placeholders_in_symbol
     * 
     * @param sym
     * @return
     */
    private SymbolImpl substitute_for_placeholders_in_symbol(SymbolImpl sym)
    {
        // if not a variable, do nothing
        final Variable asVar = sym.asVariable();
        if (asVar == null)
        {
            return sym;
        }
        // if not a placeholder variable, do nothing
        if (asVar.name.charAt(1) != '#')
        {
            return sym;
        }

        if (asVar.current_binding_value == null)
        {
            final String prefix = asVar.name.charAt(2) + "*";
            asVar.current_binding_value = varGen.generate_new_variable(prefix);
        }

        return asVar.current_binding_value;
    }

    /**
     * parser.cpp::substitute_for_placeholders_in_test
     * 
     * @param t Test to do substitutions on
     * @return The resulting test
     */
    private Test substitute_for_placeholders_in_test(Test t)
    {
        if (Tests.isBlank(t))
            return t;
        
        EqualityTest eqTest = t.asEqualityTest();
        if (eqTest != null)
        {
            return SymbolImpl.makeEqualityTest(substitute_for_placeholders_in_symbol(eqTest.getReferent()));
        }

        if (t.asGoalIdTest() != null || t.asImpasseIdTest() != null || t.asDisjunctionTest() != null)
        {
            return t;
        }
        
        final ConjunctiveTest conjunctive = t.asConjunctiveTest();
        if (conjunctive != null)
        {
            for (ListIterator<Test> it = conjunctive.conjunct_list.listIterator(); it.hasNext();)
            {
                final Test child = it.next();
                it.set(substitute_for_placeholders_in_test(child));
            }

            return conjunctive;
        }
        
        // relational tests other than equality
        final RelationalTest relational = t.asRelationalTest();
        if (relational != null)
        {
            return relational.withNewReferent(substitute_for_placeholders_in_symbol(relational.referent));
        }
        
        throw new IllegalStateException("Unexpected complex test: " + t);
    }

    /**
     * parser.cpp::substitute_for_placeholders_in_condition_list
     * 
     * @param cond
     */
    private void substitute_for_placeholders_in_condition_list(Condition cond)
    {
        for (; cond != null; cond = cond.next)
        {
            final ThreeFieldCondition tfc = cond.asThreeFieldCondition();
            if (tfc != null)
            {
                tfc.id_test = substitute_for_placeholders_in_test(tfc.id_test);
                tfc.attr_test = substitute_for_placeholders_in_test(tfc.attr_test);
                tfc.value_test = substitute_for_placeholders_in_test(tfc.value_test);
            }
            final ConjunctiveNegationCondition ncc = cond.asConjunctiveNegationCondition();
            if (ncc != null)
            {
                substitute_for_placeholders_in_condition_list(ncc.top);
            }
        }
    }

    /**
     * parser.cpp::substitute_for_placeholders_in_action_list
     * 
     * @param a
     */
    private void substitute_for_placeholders_in_action_list(Action a)
    {
        for (; a != null; a = a.next)
        {
            final MakeAction ma = a.asMakeAction();
            if (ma != null)
            {
                final RhsSymbolValue id = ma.id.asSymbolValue();
                if (id != null)
                {
                    ma.id = id.setSymbol(substitute_for_placeholders_in_symbol(id.sym));
                }
                
                final RhsSymbolValue attr = ma.attr.asSymbolValue();
                if (attr != null)
                {
                    ma.attr = attr.setSymbol(substitute_for_placeholders_in_symbol(attr.sym));
                }
                
                final RhsSymbolValue value = ma.value.asSymbolValue();
                if (value != null)
                {
                    ma.value = value.setSymbol(substitute_for_placeholders_in_symbol(value.sym));
                }
            }
        }
    }

    /**
     * Parse Relational Test
     * 
     * <pre>
     * {@code              
     * <relational_test> ::= [<relation>] <single_test>
     * <relation> ::= <> | < | > | <= | >= | = | <=>
     * <single_test> ::= variable | <constant>
     * <constant> ::= sym_constant | int_constant | float_constant
     * }
     * </pre>
     * 
     * @return A relational test
     * @throws IOException
     * @throws ParserException
     */
    Test parse_relational_test() throws IOException, ParserException
    {
        boolean use_equality_test = false;
        int test_type = RelationalTest.NOT_EQUAL_TEST;

        // read optional relation symbol
        final LexemeType lexemeType = currentType();
        switch (lexemeType)
        {
        case EQUAL:
            use_equality_test = true;
            lexer.getNextLexeme();
            break;

        case NOT_EQUAL:
        case LESS:
        case GREATER:
        case LESS_EQUAL:
        case GREATER_EQUAL:
        case LESS_EQUAL_GREATER:
            test_type = lexemeType.getRelationalTestType();
            lexer.getNextLexeme();
            break;

        default:
            use_equality_test = true;
            break;
        }

        // read variable or constant
        switch (currentType())
        {
        case SYM_CONSTANT:
        case INTEGER:
        case FLOAT:
        case VARIABLE:
        {
            SymbolImpl referent = make_symbol_for_current_lexeme();
            lexer.getNextLexeme();
            return use_equality_test ? SymbolImpl.makeEqualityTest(referent) : new RelationalTest(test_type, referent);
        }
        default:
            error("Expected variable or constant for test");
        }
        throw new IllegalStateException("Unreachable code");
    }

    /**
     * Parse Disjunction Test
     * 
     * <pre>
     * {@code
     * <disjunction_test> ::= << <constant>* >>
     * <constant> ::= sym_constant | int_constant | float_constant
     * }
     * </pre>
     * 
     * <p>parser.cpp::parse_disjunction_test
     * 
     * @return a new disjunction test
     * @throws IOException
     * @throws ParserException
     */
    DisjunctionTest parse_disjunction_test() throws IOException, ParserException
    {
        expect(LexemeType.LESS_LESS, "to begin disjunction test");
        
        List<SymbolImpl> disjuncts = new ArrayList<SymbolImpl>();
        while (currentType() != LexemeType.GREATER_GREATER)
        {
            switch (currentType())
            {
            case SYM_CONSTANT:
            case INTEGER:
            case FLOAT:
                disjuncts.add(make_symbol_for_current_lexeme());
                lexer.getNextLexeme();
                break;
            default:
                error("Expected constant or >> while reading disjunction test");
            }
        }
        lexer.getNextLexeme(); // consume the >>
        
        return new DisjunctionTest(Collections.unmodifiableList(disjuncts));
    }

    /**
     * Parse Simple Test
     * 
     * <p>{@code <simple_test> ::= <disjunction_test> | <relational_test>}
     * 
     * <p>parser.cpp::parse_simple_test
     * 
     * @return a new simple test
     * @throws IOException
     * @throws ParserException
     */
    Test parse_simple_test() throws IOException, ParserException
    {
        if (currentType() == LexemeType.LESS_LESS)
        {
            return parse_disjunction_test();
        }
        return parse_relational_test();
    }

    /**
     * Parse Test
     * 
     * <pre>{@code
     * <test> ::= <conjunctive_test> | <simple_test>
     * <conjunctive_test> ::= { <simple_test>+ }
     * }</pre>
     * 
     * <p>parser.cpp::parse_test
     * 
     * @return a new test
     * @throws IOException
     * @throws ParserException
     */
    Test parse_test() throws IOException, ParserException
    {
        if (currentType() != LexemeType.L_BRACE)
        {
            return parse_simple_test();
        }
        
        // parse and return conjunctive test
        lexer.getNextLexeme();
        Test t = null; // blank test
        do
        {
            t = Tests.add_new_test_to_test(t, parse_simple_test());
        } while (currentType() != LexemeType.R_BRACE);
        
        lexer.getNextLexeme(); // consume the "}"

        // add_new_test_to_test() pushes tests onto the front of the
        // conjunt_list so we have to reverse them here.
        final ConjunctiveTest cjt = t.asConjunctiveTest();
        if (cjt != null)
        {
            Collections.reverse(cjt.conjunct_list);
        }

        return t;
    }

    /**
     * As low-level routines (e.g., parse_value_test_star) parse, they leave the
     * id (and sometimes attribute) test fields blank (null) in the condition
     * structures they return. The calling routine must fill in the id tests
     * and/or attribute tests. These routines fill in any still-blank {id, attr}
     * tests with copies of a given test. They try to add non-equality portions
     * of the test only once, if possible.
     * 
     * <p>parser.cpp::fill_in_id_tests
     * 
     * @param conds
     * @param t
     */
    private void fill_in_id_tests(Condition conds, Test t)
    {
        PositiveCondition positive_c = null;

        // see if there's at least one positive condition
        for (Condition c = conds; c != null; c = c.next)
        {
            positive_c = c.asPositiveCondition();
            if (positive_c != null && (positive_c.id_test == null))
            {
                break;
            }
        }

        if (positive_c != null)
        { 
            // there is at least one positive condition
            // add just the equality test to most of the conditions
            final EqualityTest equality_test_from_t = Tests.copy_of_equality_test_found_in_test(t);
            for (Condition c = conds; c != null; c = c.next)
            {
                ConjunctiveNegationCondition ncc = c.asConjunctiveNegationCondition();
                ThreeFieldCondition tfc = c.asThreeFieldCondition();
                if (ncc != null)
                {
                    fill_in_id_tests(ncc.top, equality_test_from_t);
                }
                else if (tfc.id_test == null)
                {
                    tfc.id_test = equality_test_from_t.copy();
                }
            }

            // add the whole test to one positive condition
            positive_c.id_test = t.copy();
            return;
        }

        // all conditions are negative
        for (Condition c = conds; c != null; c = c.next)
        {
            final ConjunctiveNegationCondition ncc = c.asConjunctiveNegationCondition();
            final ThreeFieldCondition tfc = c.asThreeFieldCondition();
            if (ncc != null)
            {
                fill_in_id_tests(ncc.top, t);
            }
            else
            {
                if (tfc.id_test == null)
                {
                    tfc.id_test = t.copy();
                }
            }
        }
    }

    /**
     * <p>parser.cpp::fill_in_attr_tests
     * 
     * @param conds
     * @param t
     */
    private void fill_in_attr_tests(Condition conds, Test t)
    {
        PositiveCondition positive_c = null;

        // see if there's at least one positive condition
        for (Condition c = conds; c != null; c = c.next)
        {
            positive_c = c.asPositiveCondition();
            if (positive_c != null && (positive_c.attr_test == null))
            {
                break;
            }
        }

        if (positive_c != null)
        { 
            // there is at least one positive condition
            // add just the equality test to most of the conditions
            EqualityTest equality_test_from_t = Tests.copy_of_equality_test_found_in_test(t);
            for (Condition c = conds; c != null; c = c.next)
            {
                final ConjunctiveNegationCondition ncc = c.asConjunctiveNegationCondition();
                final ThreeFieldCondition tfc = c.asThreeFieldCondition();
                if (ncc != null)
                {
                    fill_in_attr_tests(ncc.top, equality_test_from_t);
                }
                else if (tfc.attr_test == null)
                {
                    tfc.attr_test = equality_test_from_t.copy();
                }
            }
            // add the whole test to one positive condition
            positive_c.attr_test = t.copy();
            return;
        }

        // all conditions are negative
        for (Condition c = conds; c != null; c = c.next)
        {
            final ConjunctiveNegationCondition ncc = c.asConjunctiveNegationCondition();
            final ThreeFieldCondition tfc = c.asThreeFieldCondition();
            if (ncc != null)
            {
                fill_in_attr_tests(ncc.top, t);
            }
            else
            {
                if (tfc.attr_test == null)
                {
                    tfc.attr_test = t.copy();
                }
            }
        }
    }


    /**
     * Returns the negation of the given condition list. If the given list is a
     * single positive or negative condition, it just toggles the type. If the
     * given list is a single ncc, it strips off the ncc part and returns the
     * subconditions. Otherwise it makes a new ncc using the given conditions.
     * 
     * <p>parser.cpp:568:negate_condition_list
     * 
     * @param conds
     * @return new negated condition list
     */
    private Condition negate_condition_list(Condition conds)
    {
        if (conds.next == null)
        {
            // only one condition to negate, so toggle the type
            final PositiveCondition pc = conds.asPositiveCondition();
            if (pc != null)
            {
                return pc.negate();
            }
            final NegativeCondition nc = conds.asNegativeCondition();
            if (nc != null)
            {
                return nc.negate();
            }
            final ConjunctiveNegationCondition ncc = conds.asConjunctiveNegationCondition();
            if (ncc != null)
            {
                return ncc.top;
            }
            throw new IllegalStateException("Unknown condition type: " + conds);
        }
        
        // more than one condition; so build a conjunctive negation
        final ConjunctiveNegationCondition tempNcc = new ConjunctiveNegationCondition();
        tempNcc.top = conds;
        Condition last = conds;
        for (; last.next != null; last = last.next)
        {
        }
        tempNcc.bottom = last;
        return tempNcc;
    }

    /**
     * This routine parses {@code <value_test>* }, given as input the id_test and
     * attr_test already read.
     * 
     * <p>{@code <value_test> ::= <test> [+] | <conds_for_one_id> [+]}
     * 
     * <p>parser.cpp::parse_value_test_star
     * 
     * @param first_letter
     * @return
     * @throws IOException
     * @throws ParserException
     */
    Condition parse_value_test_star(char first_letter) throws IOException, ParserException
    {
        final EnumSet<LexemeType> endOfTest = EnumSet.of(LexemeType.MINUS, LexemeType.UP_ARROW, LexemeType.R_PAREN);
        if (endOfTest.contains(currentType()))
        {
            // value omitted, so create dummy value test
            PositiveCondition c = new PositiveCondition();
            c.value_test = make_placeholder_test(first_letter);
            return c;
        }

        Condition last_c = null, first_c = null;
        ByRef<Test> value_test = ByRef.create(null);
        do
        {
            Condition new_conds;
            if (currentType() == LexemeType.L_PAREN)
            {
                // read <conds_for_one_id>, take the id_test from it
                new_conds = parse_conds_for_one_id(first_letter, value_test);
            }
            else
            {
                // read <value_test>
                new_conds = null;
                value_test.value = parse_test();
                if (!Tests.test_includes_equality_test_for_symbol(value_test.value, null))
                {
                    value_test.value = Tests.add_new_test_to_test(value_test.value,
                            make_placeholder_test(first_letter));
                }
            }
            
            // check for acceptable preference indicator
            boolean acceptable = false;
            if (currentType() == LexemeType.PLUS)
            {
                acceptable = true;
                lexer.getNextLexeme();
            }
            
            // build condition using the new value test
            PositiveCondition pc = new PositiveCondition();
            pc.value_test = value_test.value;
            pc.test_for_acceptable_preference = acceptable;
            new_conds = Condition.insertAtHead(new_conds, pc);

            // add new conditions to the end of the list
            if (last_c != null)
            {
                last_c.next = new_conds;
            }
            else
            {
                first_c = new_conds;
            }
            
            new_conds.prev = last_c;
            for (last_c = new_conds; last_c.next != null; last_c = last_c.next)
            {
                // nothing
            }
        } while (!endOfTest.contains(currentType()));
        
        return first_c;
    }

    /**
     * This routine parses <attr_value_tests>, given as input the id_test already read.
     * 
     * <pre>{@code
     * <attr_value_tests> ::= [-] ^ <attr_test> [.<attr_test>]* <value_test>*
     * <attr_test> ::= <test>
     * }</pre>
     *  
     * <p>parser.cpp::parse_attr_value_tests
     * 
     * @return new condition
     * @throws IOException
     * @throws ParserException
     */
    Condition parse_attr_value_tests() throws IOException, ParserException
    {
        boolean negate_it;

        // read optional minus sign
        negate_it = false;
        if (currentType() == LexemeType.MINUS)
        {
            negate_it = true;
            lexer.getNextLexeme();
        }

        // read up arrow
        expect(LexemeType.UP_ARROW, "followed by attribute");

        // read first <attr_test>
        Test attr_test = parse_test();
        if (!Tests.test_includes_equality_test_for_symbol(attr_test, null))
        {
            attr_test = Tests.add_new_test_to_test(attr_test, make_placeholder_test('a'));
        }

        // read optional attribute path
        Condition first_c = null;
        Condition last_c = null;
        Test id_test_to_use = null;
        while (currentType() == LexemeType.PERIOD)
        {
            lexer.getNextLexeme(); // consume the "."
            
            // setup for next attribute in path:  make a dummy variable,
            //   create a new condition in the path
            PositiveCondition c = new PositiveCondition();
            if (last_c != null)
            {
                last_c.next = c;
            }
            else
            {
                first_c = c;
            }
            c.next = null;
            c.prev = last_c;
            last_c = c;
            if (id_test_to_use != null)
            {
                c.id_test = id_test_to_use.copy();
            }
            else
            {
                c.id_test = null;
            }
            c.attr_test = attr_test;

            id_test_to_use = make_placeholder_test(Tests.first_letter_from_test(attr_test));
            c.value_test = id_test_to_use;
            c.test_for_acceptable_preference = false;

            // update id and attr tests for the next path element
            attr_test = parse_test();
            if (!Tests.test_includes_equality_test_for_symbol(attr_test, null))
            {
                attr_test = Tests.add_new_test_to_test(attr_test, make_placeholder_test('a'));
            }
        }

        // finally, do the <value_test>* part
        Condition new_conds = parse_value_test_star(Tests.first_letter_from_test(attr_test));
        fill_in_attr_tests(new_conds, attr_test);
        if (id_test_to_use != null)
        {
            fill_in_id_tests(new_conds, id_test_to_use);
        }

        if (last_c != null)
        {
            last_c.next = new_conds;
        }
        else
        {
            first_c = new_conds;
        }
        new_conds.prev = last_c;
        // should update last_c here, but it's not needed anymore

        // negate everything if necessary
        if (negate_it)
        {
            first_c = negate_condition_list(first_c);
        }

        return first_c;
    }

    /**
     * This routine parses the {@code "( [state|impasse] [<id_test>]" } part of
     *  {@code <conds_for_one_id> } and returns the resulting id_test.
     * 
     * <pre>{@code
     * <conds_for_one_id> ::= ( [state|impasse] [<id_test>] <attr_value_tests>* )
     * <id_test> ::= <test>
     * }</pre>
     * 
     * <p>parser.cpp::parse_head_of_conds_for_one_id
     * 
     * @param first_letter_if_no_id_given
     * @return a new test
     * @throws IOException
     * @throws ParserException
     */
    private Test parse_head_of_conds_for_one_id(char first_letter_if_no_id_given) throws IOException, ParserException
    {
        expect(LexemeType.L_PAREN, "to begin condition element");

        Test id_goal_impasse_test = null;
        // look for goal/impasse indicator
        if (currentType() == LexemeType.SYM_CONSTANT)
        {
            if (current().string.equals("state"))
            {
                id_goal_impasse_test = GoalIdTest.INSTANCE;
                lexer.getNextLexeme();
                first_letter_if_no_id_given = 's';
            }
            else if (current().string.equals("impasse"))
            {
                id_goal_impasse_test = ImpasseIdTest.INSTANCE;
                lexer.getNextLexeme();
                first_letter_if_no_id_given = 'i';
            }
            else
            {
                id_goal_impasse_test = null; // make_blank_test();
            }
        }
        else
        {
            id_goal_impasse_test = null; // make_blank_test();
        }

        Test id_test = null; // blank test
        Test check_for_symconstant;
        SymbolImpl sym;
        // read optional id test; create dummy one if none given
        if (currentType() != LexemeType.MINUS && 
            currentType() != LexemeType.UP_ARROW && 
            currentType() != LexemeType.R_PAREN)
        {
            id_test = parse_test();
            if (!Tests.test_includes_equality_test_for_symbol(id_test, null))
            {
                id_test = Tests.add_new_test_to_test(id_test,
                        make_placeholder_test(first_letter_if_no_id_given));
            }
            else
            {
                check_for_symconstant = Tests.copy_of_equality_test_found_in_test(id_test);
                sym = check_for_symconstant.asEqualityTest().getReferent();
                if (sym.asVariable() == null)
                {
                    error(String.format("Warning: Constant %s in id field test.\n    This will never match.", sym));
                }
            }
        }
        else
        {
            id_test = make_placeholder_test(first_letter_if_no_id_given);
        }

        // add the goal/impasse test to the id test
        id_test = Tests.add_new_test_to_test(id_test, id_goal_impasse_test);

        // return the resulting id test
        return id_test;
    }

    /**
     * This routine parses the {@code "<attr_value_tests>* )" } part of {@code <conds_for_one_id>}
     * and returns the resulting conditions.
     * It does not fill in the id tests of the conditions.
     * 
     * <pre>{@code
     * <conds_for_one_id> ::= ( [state|impasse] [<id_test>] <attr_value_tests>* )
     * <id_test> ::= <test>
     * }</pre>
     * 
     * <p>parser.cpp::parse_tail_of_conds_for_one_id
     * 
     * @return new condition
     * @throws IOException
     * @throws ParserException
     */
    private Condition parse_tail_of_conds_for_one_id() throws IOException, ParserException
    {
        // if no <attr_value_tests> are given, create a dummy one
        if (currentType() == LexemeType.R_PAREN)
        {
            lexer.getNextLexeme(); // consume the right parenthesis
            PositiveCondition c = new PositiveCondition();
            c.attr_test = make_placeholder_test('a');
            c.value_test = make_placeholder_test('v');
            c.test_for_acceptable_preference = false;
            return c;
        }

        // read <attr_value_tests>*
        Condition first_c = null;
        Condition last_c = null;
        Condition new_conds = null;
        while (currentType() != LexemeType.R_PAREN)
        {
            new_conds = parse_attr_value_tests();
            if (last_c != null)
            {
                last_c.next = new_conds;
            }
            else
            {
                first_c = new_conds;
            }
            new_conds.prev = last_c;
            for (last_c = new_conds; last_c.next != null; last_c = last_c.next)
            { 
                // nothing
            }
        }

        // reached the end of the condition
        lexer.getNextLexeme(); // consume the right parenthesis

        return first_c;
    }

    /**
     * This routine parses {@code <conds_for_one_id>} and returns the conditions (of
     * null if any error occurs).
     * 
     * <pre>{@code
     * <conds_for_one_id> ::= ( [state|impasse] [<id_test>] <attr_value_tests>* )
     * <id_test> ::= <test>
     * }</pre>
     * 
     * <p>If the argument dest_id_test is non-NULL, then *dest_id_test is set
     * to the resulting complete id test (which includes any goal/impasse test),
     * and in all the conditions the id field is filled in with just the
     * equality portion of id_test.
     * 
     * <p>If the argument dest_id_test is NULL, then the complete id_test is
     * included in the conditions.
     * 
     * <p>parser.cpp::parse_conds_for_one_id
     * 
     * @param first_letter_if_no_id_given
     * @param dest_id_test
     * @return list of conditions
     * @throws IOException
     * @throws ParserException
     */
    Condition parse_conds_for_one_id(char first_letter_if_no_id_given, ByRef<Test> dest_id_test)
            throws IOException, ParserException
    {
        // parse the head
        Test id_test = parse_head_of_conds_for_one_id(first_letter_if_no_id_given);

        // parse the tail
        Condition conds = parse_tail_of_conds_for_one_id();

        // fill in the id test in all the conditions just read
        if (dest_id_test != null)
        {
            dest_id_test.value = id_test;
            Test equality_test_from_id_test = Tests.copy_of_equality_test_found_in_test(id_test);
            fill_in_id_tests(conds, equality_test_from_id_test);
        }
        else
        {
            fill_in_id_tests(conds, id_test);
        }

        return conds;
    }

    /**
     * Parse a condition
     * 
     * <pre>{@code
     * <cond> ::= <positive_cond> | - <positive_cond>
     * <positive_cond> ::= <conds_for_one_id> | { <cond>+ }
     * }</pre>
     * 
     * <p>parser.cpp::parse_cond
     * 
     * @return
     * @throws IOException
     * @throws ParserException
     */
    Condition parse_cond() throws IOException, ParserException
    {
        // look for leading "-" sign
        boolean negate_it = false;
        if (currentType() == LexemeType.MINUS)
        {
            negate_it = true;
            lexer.getNextLexeme();
        }

        Condition c = null;
        // parse <positive_cond>
        if (currentType() == LexemeType.L_BRACE)
        {
            // read conjunctive condition
            lexer.getNextLexeme();
            c = parse_cond_plus();
            
            expect(LexemeType.R_BRACE, "to end conjunctive condition"); // consume the R_BRACE
        }
        else
        {
            // read conds for one id
            c = parse_conds_for_one_id('s', null);
        }

        // if necessary, handle the negation
        if (negate_it)
        {
            c = negate_condition_list(c);
        }

        return c;
    }

    /**
     * Parses {@code <cond>+} and builds a condition list.
     * 
     * <p>parser.cpp::parse_cond_plus
     * 
     * @return list of conditions
     * @throws IOException
     * @throws ParserException
     */
    Condition parse_cond_plus() throws IOException, ParserException
    {
        Condition first_c = null;
        Condition last_c = null;
        do
        {
            /* --- get individual <cond> --- */
            Condition new_conds = parse_cond();
            if (last_c != null)
            {
                last_c.next = new_conds;
            }
            else
            {
                first_c = new_conds;
            }
            new_conds.prev = last_c;
            for (last_c = new_conds; last_c.next != null; last_c = last_c.next)
            { 
                // nothing
            }
        } while ((currentType() == LexemeType.MINUS) || (currentType() == LexemeType.L_PAREN)
                || (currentType() == LexemeType.L_BRACE));
        return first_c;
    }

    /**
     * Parses {@code <lhs>} and builds a condition list.
     * 
     * <p>{@code <lhs> ::= <cond>+}
     * 
     * <p>parser.cpp::parse_lhs
     * 
     * @return condition list
     * @throws IOException
     * @throws ParserException
     */
    Condition parse_lhs() throws IOException, ParserException
    {
        return parse_cond_plus();
    }

    /**
     * Parses a {@code <function_call> } after the "(" has already been consumed.
     * At entry, the current lexeme should be the function name.
     *
     * <pre>{@code
     * <function_call> ::= ( <function_name> <rhs_value>* )
     * <function_name> ::= sym_constant | + | -
     * }</pre>
     * 
     * <p>(Warning: might need others besides +, - here if the lexer changes)
     * 
     * <p>parser.cpp::parse_function_call_after_lparen
     * 
     * @param is_stand_alone_action
     * @return a rhs value
     * @throws IOException
     * @throws ParserException
     */
    RhsFunctionCall parse_function_call_after_lparen(boolean is_stand_alone_action) throws IOException,
            ParserException
    {
        StringSymbolImpl fun_name;

        // read function name, find the rhs_function structure
        if (currentType() == LexemeType.PLUS)
        {
            fun_name = syms.createString("+");
        }
        else if (currentType() == LexemeType.MINUS)
        {
            fun_name = syms.createString("-");
        }
        else
        {
            fun_name = syms.createString(current().string);
        }
        // if (fun_name == null) {
        // // TODO
        // throw new IllegalStateException("No RHS function named " +
        // current().string);
        // // print (thisAgent, "No RHS function named %s\n",current().string);
        // // print_location_of_most_recent_lexeme(thisAgent);
        // // return null;
        // }
        // rf = lookup_rhs_function (thisAgent, fun_name);
        // if (!rf) {
        // print (thisAgent, "No RHS function named %s\n",current().string);
        // print_location_of_most_recent_lexeme(thisAgent);
        // return null;
        // }

        // make sure stand-alone/rhs_value is appropriate
        // if (is_stand_alone_action && (! rf->can_be_stand_alone_action)) {
        // print (thisAgent, "Function %s cannot be used as a stand-alone
        // action\n",
        // current().string);
        // print_location_of_most_recent_lexeme(thisAgent);
        // return null;
        // }
        // if ((! is_stand_alone_action) && (! rf->can_be_rhs_value)) {
        // print (thisAgent, "Function %s can only be used as a stand-alone
        // action\n",
        // current().string);
        // print_location_of_most_recent_lexeme(thisAgent);
        // return null;
        // }
        
        // build list of rhs_function and arguments
        RhsFunctionCall rfc = new RhsFunctionCall(fun_name, is_stand_alone_action);
        lexer.getNextLexeme(); // consume function name, advance to argument list
        while (currentType() != LexemeType.R_PAREN)
        {
            RhsValue arg_rv = parse_rhs_value();
            rfc.addArgument(arg_rv);
        }

        // check number of arguments
        // if ((rf->num_args_expected != -1) && (rf->num_args_expected !=
        // num_args)) {
        //    print (thisAgent, "Wrong number of arguments to function %s (expected %d)\n",
        //           rf->name->sc.name, rf->num_args_expected);
        //    print_location_of_most_recent_lexeme(thisAgent);
        //    deallocate_rhs_value (thisAgent, funcall_list_to_rhs_value(fl));
        //    return null;
        //  }
        lexer.getNextLexeme(); // consume the right parenthesis
        return rfc;
    }

    /**
     * Parses an {@code <rhs_value>}. Returns an rhs_value, or null if any error
     * occurred.
     * 
     * <pre>{@code
     * <rhs_value> ::= <constant> | <function_call> | variable 
     * <constant> ::= sym_constant | int_constant | float_constant
     * }</pre>
     * 
     * <p>parser.cpp::parse_rhs_value
     * 
     * @return
     * @throws IOException
     * @throws ParserException
     */
    RhsValue parse_rhs_value() throws IOException, ParserException
    {
        if (currentType() == LexemeType.L_PAREN)
        {
            lexer.getNextLexeme();
            return parse_function_call_after_lparen(false);
        }
        if ((currentType() == LexemeType.SYM_CONSTANT)
                || (currentType() == LexemeType.INTEGER)
                || (currentType() == LexemeType.FLOAT)
                || (currentType() == LexemeType.VARIABLE))
        {
            RhsValue rv = new RhsSymbolValue(make_symbol_for_current_lexeme());
            lexer.getNextLexeme();
            return rv;
        }
        error("Illegal value for RHS value");
        throw new IllegalStateException("Unreachable code");
    }

    /**
     * Parses a {@code <preference-specifier> }.  Returns the appropriate 
     * xxx_PREFERENCE_TYPE (see soarkernel.h).
     * 
     * <p>Note:  in addition to the grammar below, if there is no preference
     * specifier given, then this routine returns ACCEPTABLE_PREFERENCE_TYPE.
     * Also, for {@code <binary-preference>'s}, this routine *does not* read the
     * {@code <rhs_value> } referent.  This must be done by the caller routine.
     * 
     * <pre>{@code
     *    <preference-specifier> ::= <naturally-unary-preference> [,]
     *                             | <forced-unary-preference>
     *                             | <binary-preference> <rhs_value> [,]
     *    <naturally-unary-preference> ::= + | - | ! | ~ | @
     *    <binary-preference> ::= > | = | < | &
     *    <any-preference> ::= <naturally-unary-preference> | <binary-preference>
     *    <forced-unary-preference> ::= <binary-preference> 
     *                                  {<any-preference> | , | ) | ^}  
     *      ;but the parser shouldn't consume the <any-preference>, ")" or "^" 
     *       lexeme here
     * }</pre>
     * 
     * <p>parser.cpp::parse_preference_specifier_without_referent
     * 
     * @return preferenct type
     * @throws IOException
     */
    PreferenceType parse_preference_specifier_without_referent() throws IOException
    {
        switch (currentType())
        {
        case PLUS:
        case MINUS:
        case EXCLAMATION_POINT:
        case TILDE:
        case AT:
            final PreferenceType type = currentType().getPreferenceType();
            lexer.getNextLexeme();
            consumeComma();
            return type;

        /*******************************************************************
         * [Soar-Bugs #55] <forced-unary-preference> ::= <binary-preference> {<any-preference> | , | ) | ^}
         * 
         * Forced unary preferences can now occur when a binary preference
         * is followed by a ",", ")", "^" or any preference specifier
         ******************************************************************/

        case GREATER:   return parseBetterBest();
        case EQUAL:     return parseIndifferent();
        case LESS:      return parseWorseWorst();
        case AMPERSAND: return parseParallel();

        default:
            // if no preference given, make it an acceptable preference
            return PreferenceType.ACCEPTABLE;
        }
    }
    
    private PreferenceType parseBinaryOrUnaryPreference(PreferenceType bin, PreferenceType unary) throws IOException
    {
        lexer.getNextLexeme();
        if (!lexer.isEof() && 
            (currentType() != LexemeType.COMMA) && 
            (currentType() != LexemeType.R_PAREN) && 
            (currentType() != LexemeType.UP_ARROW) && 
            (!currentType().isPreference()))
        {
            return bin;
        }
        // forced unary preference
        consumeComma();
        return unary;
    }

    private PreferenceType parseParallel() throws IOException
    {
        return parseBinaryOrUnaryPreference(PreferenceType.BINARY_PARALLEL, PreferenceType.UNARY_PARALLEL);
    }

    private PreferenceType parseWorseWorst() throws IOException
    {
        return parseBinaryOrUnaryPreference(PreferenceType.WORSE, PreferenceType.WORST);
    }
    
    private PreferenceType parseBetterBest() throws IOException
    {
        return parseBinaryOrUnaryPreference(PreferenceType.BETTER, PreferenceType.BEST);
    }

    private PreferenceType parseIndifferent() throws IOException
    {
        lexer.getNextLexeme();
        if (!lexer.isEof() && 
            (currentType() != LexemeType.COMMA) && 
            (currentType() != LexemeType.R_PAREN) && 
            (currentType() != LexemeType.UP_ARROW) && 
            (!currentType().isPreference()))
        {

            if ((currentType() == LexemeType.INTEGER) || (currentType() == LexemeType.FLOAT))
                return PreferenceType.NUMERIC_INDIFFERENT;
            else
                return PreferenceType.BINARY_INDIFFERENT;
        }

        // forced unary preference
        consumeComma();
        return PreferenceType.UNARY_INDIFFERENT;
    }

    /**
     * Given the id, attribute, and value already read, this routine
     * parses zero or more {@code <preference-specifier>'s }.  It builds and
     * returns an action list for these RHS make's.  It returns null if
     * any error occurred.
     * 
     * <pre>{@code
     * <value_make> ::= <rhs_value> <preferences>
     * <preferences> ::= [,] | <preference_specifier>+   
     * <preference-specifier> ::= <naturally-unary-preference> [,]
     *                          | <forced-unary-preference>
     *                          | <binary-preference> <rhs_value> [,]
     * }</pre>
     *     
     * <p>parser.cpp::parse_preferences
     * 
     * @param id
     * @param attr
     * @param value
     * @return an action list
     * @throws IOException
     * @throws ParserException
     */
    Action parse_preferences(SymbolImpl id, RhsValue attr, RhsValue value) throws IOException, ParserException
    {
        // Note: this routine is set up so if there's not preference type
        // indicator at all, we return a single acceptable preference make

        boolean saw_plus_sign = (currentType() == LexemeType.PLUS);
        PreferenceType preference_type = parse_preference_specifier_without_referent();
        if ((preference_type == PreferenceType.ACCEPTABLE) && (!saw_plus_sign))
        {
            // If the routine gave us a + pref without seeing a + sign, then it's
            // just giving us the default acceptable preference.  Look for optional
            // comma.
            consumeComma();
        }

        Action prev_a = null;
        while (true)
        {
            // read referent
            final RhsValue referent;
            if (preference_type.isBinary())
            {
                referent = parse_rhs_value();
                consumeComma();
            }
            else
            {
                referent = null;
            }

            // create the appropriate action
            final MakeAction a = new MakeAction();
            a.preference_type = preference_type;
            a.next = prev_a;
            prev_a = a;
            a.id = new RhsSymbolValue(id);
            a.attr = attr.copy();
            a.value = value.copy();
            if (preference_type.isBinary())
            {
                a.referent = referent;
            }

            // look for another preference type specifier
            saw_plus_sign = (currentType() == LexemeType.PLUS);
            preference_type = parse_preference_specifier_without_referent();

            // exit loop when done reading preferences
            if ((preference_type == PreferenceType.ACCEPTABLE) && (!saw_plus_sign))
            {
                // If the routine gave us a + pref without seeing a + sign, then it's
                // just giving us the default acceptable preference, it didn't see any
                // more preferences specified.
                return prev_a;
            }
        }
    }

    /**
     * Given the id, attribute, and value already read, this routine
     * parses zero or more {@code <preference-specifier>'s}.  If preferences
     * other than reject and acceptable are specified, it prints
     * a warning message that they are being ignored.  It builds an
     * action list for creating an ACCEPTABLE preference.  If binary 
     * preferences are encountered, a warning message is printed and 
     * the production is ignored (returns null).  It returns null if any 
     * other error occurred.  This works in conjunction with the code
     * that supports attribute_preferences_mode == 2.  Anywhere that
     * attribute_preferences_mode == 2 is tested, the code now tests
     * for operand2_mode == true.
     * 
     * <pre>{@code
     * <value_make> ::= <rhs_value> <preferences>
     * <preferences> ::= [,] | <preference_specifier>+   
     * <preference-specifier> ::= <naturally-unary-preference> [,]
     *                          | <forced-unary-preference>
     *                          | <binary-preference> <rhs_value> [,]
     * }</pre>
     *                 
     * <p>parser.cpp::parse_preferences_soar8_non_operator
     * 
     * @param id
     * @param attr
     * @param value
     * @return action
     * @throws IOException
     * @throws ParserException
     */
    Action parse_preferences_soar8_non_operator (SymbolImpl id, RhsValue attr, RhsValue value) throws IOException, ParserException 
    {
        /* --- Note: this routine is set up so if there's not preference type
           indicator at all, we return an acceptable preference make
           and a parallel preference make.  For non-operators, allow
           only REJECT_PREFERENCE_TYPE, (and UNARY_PARALLEL and ACCEPTABLE).
           If any other preference type indicator is found, a warning or
           error msg (error only on binary prefs) is printed. --- */

        boolean saw_plus_sign = (currentType() == LexemeType.PLUS);
        PreferenceType preference_type = parse_preference_specifier_without_referent();
        if ((preference_type == PreferenceType.ACCEPTABLE) && (!saw_plus_sign))
        {
            /* If the routine gave us a + pref without seeing a + sign, then it's
               just giving us the default acceptable preference.  Look for optional
               comma. */
            consumeComma();
        }

        Action prev_a = null;

        while (true)
        {
            /* step through the pref list, print warning messages when necessary. */

            /* --- read referent --- */
            if (preference_type.isBinary())
            {
                error(String.format("In Soar8, binary preference illegal for non-operator. "
                        + "(id = %s\t attr = %s\t value = %s)\n", id, attr, value));
            }

            if ((preference_type != PreferenceType.ACCEPTABLE) && (preference_type != PreferenceType.REJECT))
            {
                printer.warn("\nWARNING: in Soar8, the only allowable non-operator preference \n"
                        + "is REJECT - .\nIgnoring specified preferences.\n" + "id = %s\t attr = %s\t value = %s\n",
                        id, attr, value);
                // print_location_of_most_recent_lexeme(thisAgent);
            }

            if (preference_type == PreferenceType.REJECT)
            {
                // create the appropriate action
                MakeAction a = new MakeAction();
                a.next = prev_a;
                prev_a = a;
                a.preference_type = preference_type;
                a.id = new RhsSymbolValue(id);
                a.attr = attr.copy();
                a.value = value.copy();
            }

            // look for another preference type specifier
            saw_plus_sign = (currentType() == LexemeType.PLUS);
            preference_type = parse_preference_specifier_without_referent();

            // exit loop when done reading preferences
            if ((preference_type == PreferenceType.ACCEPTABLE) && (!saw_plus_sign))
            {
                /* If the routine gave us a + pref without seeing a + sign, then it's
                   just giving us the default acceptable preference, it didn't see any
                   more preferences specified. */

                /* for soar8, if this wasn't a REJECT preference, then
                	create acceptable preference makes.  */
                if (prev_a == null)
                {

                    MakeAction a = new MakeAction();
                    a.next = prev_a;
                    prev_a = a;
                    a.preference_type = PreferenceType.ACCEPTABLE;
                    a.id = new RhsSymbolValue(id);
                    a.attr = attr.copy();
                    a.value = value.copy();
                }
                return prev_a;
            }
        }
    }

    /**
     * Given the id already read, this routine parses an <attr_value_make>.
     * It builds and returns an action list for these RHS make's.  It
     * returns null if any error occurred.
     * 
     * <pre>{@code
     * <attr_value_make> ::= ^ <rhs_value> <value_make>+
     * <value_make> ::= <rhs_value> <preferences>
     * }</pre>
     * 
     * <p>parser.cpp::parse_attr_value_make
     * 
     * @param id
     * @return make action
     * @throws IOException
     * @throws ParserException
     */
    Action parse_attr_value_make (SymbolImpl id) throws IOException, ParserException 
    {
        RhsValue value;
        Action new_actions, last;

        expect(LexemeType.UP_ARROW, "in RHS make action"); // consume up-arrow, advance to attribute

        RhsValue attr = parse_rhs_value();
        if (attr == null)
        {
            return null;
        }

        String szAttribute = String.format("%s", attr); // (rhs_value_to_string)

        Action all_actions = null;

        /*  allow dot notation "." in RHS attribute path  10/15/98 KJC */
        while (currentType() == LexemeType.PERIOD)
        {
            lexer.getNextLexeme(); /* consume the "."  */
            /* set up for next attribute in path: make dummy variable,
               and create new action in the path */

            /* --- create variable with "#" in its name:  this couldn't possibly be a
               variable in the user's code, since the lexer doesn't handle "#" --- */
            /* KJC used same format so could steal code... */
            char first_letter = attr.getFirstLetter();
            String namebuf = "<#" + first_letter + '*' + placeholder_counter[first_letter - 'a']++;
            Variable new_var = syms.make_variable(namebuf);
            /* --- indicate that there is no corresponding "real" variable yet --- */
            new_var.current_binding_value = null;

            /* parse_preferences actually creates the action.  eventhough
             there aren't really any preferences to read, we need the default
             acceptable and parallel prefs created for all attributes in path */
            if (operand2_mode && !"operator".equals(szAttribute))
            {
                new_actions = parse_preferences_soar8_non_operator(id, attr, new RhsSymbolValue(new_var));
            }
            else
            {
                new_actions = parse_preferences(id, attr, new RhsSymbolValue(new_var));
            }

            for (last = new_actions; last.next != null; last = last.next)
            {
                // Do nothing
            }

            last.next = all_actions;
            all_actions = new_actions;

            /* if there was a "." then there must be another attribute
               set id for next action and get the next attribute */
            id = new_var;
            attr = parse_rhs_value();
            if (attr == null)
            {
                return null;
            }

            szAttribute = String.format("%s", attr); // rhs_value_to_string
        }
        /* end of while (currentType() == PERIOD_LEXEME */

        do
        {
            value = parse_rhs_value();
            if (operand2_mode && !"operator".equals(szAttribute))
            {
                new_actions = parse_preferences_soar8_non_operator(id, attr, value);
            }
            else
            {
                new_actions = parse_preferences(id, attr, value);
            }
            for (last = new_actions; last.next != null; last = last.next)
            {
                // nothing
            }
            last.next = all_actions;
            all_actions = new_actions;
        } while ((currentType() != LexemeType.R_PAREN) && (currentType() != LexemeType.UP_ARROW));

        return all_actions;
    }

    /**
     * Parses a {@code <rhs_action>} and returns an action list. If any error occurrs,
     * null is returned.
     * 
     * <p>{@code <rhs_action> ::= ( variable <attr_value_make>+ ) | <function_call> }
     * 
     * <p>parser.cpp::parse_rhs_action
     * 
     * @return a new action
     * @throws IOException
     * @throws ParserException
     */
    Action parse_rhs_action() throws IOException, ParserException
    {
        expect(LexemeType.L_PAREN, "to begin RHS action");
        
        if (currentType() != LexemeType.VARIABLE)
        {
            // the action is a function call
            RhsFunctionCall funcall_value = parse_function_call_after_lparen(true);
            if (funcall_value == null)
            {
                return null;
            }
            return new FunctionAction(funcall_value);
        }
        
        // the action is a regular make action
        Variable var = syms.make_variable(current().string);
        lexer.getNextLexeme();
        Action all_actions = null;
        Action last = null;
        while (currentType() != LexemeType.R_PAREN)
        {
            final Action new_actions = parse_attr_value_make(var);
            for (last = new_actions; last.next != null; last = last.next)
            {
                // nothing
            }
            last.next = all_actions;
            all_actions = new_actions;
        }
        lexer.getNextLexeme(); // consume the right parenthesis
        return all_actions;
    }

    /**
     * Parses the {@code <rhs>} and sets *actions to the resulting action list. Returns
     * true if successful, false if any error occurred.
     * 
     * <p>{@code <rhs> ::= <rhs_action>*}
     * 
     * @return
     * @throws IOException
     * @throws ParserException
     */
    Action parse_rhs() throws IOException, ParserException
    {
        Action all_actions = null;
        Action last = null;
        while (!lexer.isEof() && currentType() != LexemeType.R_PAREN)
        {
            final Action new_actions = parse_rhs_action();
            for (last = new_actions; last.next != null; last = last.next)
            {
                // nothing
            }
            last.next = all_actions;
            all_actions = new_actions;
        }
        return all_actions;
    }

    private StringSymbolImpl parseProductionName() throws ParserException, IOException
    {
        if (currentType()!=LexemeType.SYM_CONSTANT) {
            error("Expected symbol for production name\n");
            throw new IllegalStateException("Unreachable code");
        }
        StringSymbolImpl name = syms.createString(current().string);
        lexer.getNextLexeme();
        
        return name;
    }
    
    private String parseDocumenation() throws IOException
    {
        String documentation = null;
        // read optional documentation string
        if (currentType()==LexemeType.QUOTED_STRING) {
          documentation = current().string;
          lexer.getNextLexeme();
        }
        return documentation;
    }

    /**
     * This routine reads a production (everything inside the body of the "sp"
     * command), builds a production, and adds it to the rete.
     * 
     * <p>If successful, it returns a pointer to the new production struct.
     * 
     * <p>parser.cpp::parse_production
     * 
     * @return new production, never <code>null</code>
     * @throws IOException
     * @throws ParserException
     */
    public Production parserProduction() throws IOException, ParserException
    {
        reset_placeholder_variable_generator();

        final StringSymbolImpl name = parseProductionName();
        currentProduction = name;

        // read optional documentation string
        final String documentation = parseDocumenation();

        // read optional flags
        ProductionSupport declared_support = ProductionSupport.UNDECLARED;
        ProductionType prod_type = ProductionType.USER;
        boolean interrupt_on_match = false;
        while (currentType() == LexemeType.SYM_CONSTANT)
        {
            if (":o-support".equals(current().string))
            {
                declared_support = ProductionSupport.DECLARED_O_SUPPORT;
            }
            else if (":i-support".equals(current().string))
            {
                declared_support = ProductionSupport.DECLARED_I_SUPPORT;
            }
            else if (":chunk".equals(current().string))
            {
                prod_type = ProductionType.CHUNK;
            }
            else if (":default".equals(current().string))
            {
                prod_type = ProductionType.DEFAULT;
            }
            else if (":template".equals(current().string))
            {
                prod_type = ProductionType.TEMPLATE;
            }
            else if (":interrupt".equals(current().string))
            {
                printer.warn("WARNING :interrupt is not supported with the current build options...");
                interrupt_on_match = true;
                // xml_generate_warning(thisAgent, "WARNING :interrupt is not
                // supported with the current build options...");
            }
            else
            {
                break;
            }
            lexer.getNextLexeme();
        }

        final Condition lhs = parse_lhs();

        // read the "-->"
        expect(LexemeType.RIGHT_ARROW, "in production");

        final Action rhs = destructively_reverse_action_list(parse_rhs());

        // TODO: DR: This makes no sense to me
        // /* --- finally, make sure there's a closing right parenthesis (but
        // don't consume it) --- */
        // if (currentType()!=R_PAREN_LEXEME) {
        // // TODO
        // throw new IllegalStateException("Expected ) to end production");
        // print (thisAgent, "Expected ) to end production\n");
        // print_location_of_most_recent_lexeme(thisAgent);
        // print_with_symbols (thisAgent, "(Ignoring production %y)\n\n", name);
        // symbol_remove_ref (thisAgent, name);
        // deallocate_condition_list (thisAgent, lhs);
        // deallocate_action_list (thisAgent, rhs);
        // return null;
        // }

        // replace placeholder variables with real variables
        varGen.reset(lhs, rhs);
        substitute_for_placeholders_in_condition_list(lhs);
        substitute_for_placeholders_in_action_list(rhs);

        // everything parsed okay, so make the production structure
        final Condition lhs_top = lhs;
        Condition lhs_bottom = lhs;
        for (; lhs_bottom.next != null; lhs_bottom = lhs_bottom.next)
        {
            // Nothing
        }

        Production p = new Production(prod_type, name, lhs_top, lhs_bottom, rhs);

        p.documentation = documentation;
        p.declared_support = declared_support;
        p.interrupt = interrupt_on_match;

        return p;
}

    /**
     * As the parser builds the action list for the RHS, it adds each new action
     * onto the front of the list. This results in the order of the actions
     * getting reversed. This has certain problems--for example, if there are
     * several (write) actions on the RHS, reversing their order means the
     * output lines get printed in the wrong order. To avoid this problem, we
     * reverse the list after building it.
     * 
     * <p>This routine destructively reverses an action list.
     * 
     * @param a
     * @return
     */
    private static Action destructively_reverse_action_list(Action a)
    {
        Action prev = null;
        Action current = a;
        while (current != null)
        {
            Action next = current.next;
            current.next = prev;
            prev = current;
            current = next;
        }
        return prev;
    }
}
