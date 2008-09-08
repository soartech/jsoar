/*
 * (c) 2008  Dave Ray
 *
 * Created on Aug 15, 2008
 */
package org.jsoar.kernel.parser;

import java.io.IOException;
import java.util.Collections;

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
import org.jsoar.kernel.lhs.TestTools;
import org.jsoar.kernel.lhs.ThreeFieldCondition;
import org.jsoar.kernel.memory.PreferenceType;
import org.jsoar.kernel.rhs.Action;
import org.jsoar.kernel.rhs.FunctionAction;
import org.jsoar.kernel.rhs.MakeAction;
import org.jsoar.kernel.rhs.RhsFunctionCall;
import org.jsoar.kernel.rhs.RhsSymbolValue;
import org.jsoar.kernel.rhs.RhsValue;
import org.jsoar.kernel.symbols.SymConstant;
import org.jsoar.kernel.symbols.Symbol;
import org.jsoar.kernel.symbols.SymbolFactory;
import org.jsoar.kernel.symbols.Variable;
import org.jsoar.util.Arguments;
import org.jsoar.util.ByRef;

/* =================================================================

Grammar for left hand sides of productions

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

================================================================= */

/**
 * @author ray
 */
public class Parser implements LexemeTypes
{
    private Lexer lexer;
    private SymbolFactory syms;
    private VariableGenerator varGen;
    private boolean operand2_mode = false;
    private int[] placeholder_counter = new int[26];
    
    public Parser(VariableGenerator varGen, Lexer lexer)
    {
        Arguments.checkNotNull(varGen, "varGen");
        Arguments.checkNotNull(lexer, "lexer");
        
        this.varGen = varGen;
        this.syms = varGen.getSyms();
        this.lexer = lexer;
    }
    
    public Lexer getLexer()
    {
        return lexer;
    }
    
    private Symbol make_symbol_for_current_lexeme () 
    {
        switch (lexer.getCurrentLexeme().type) 
        {
        case SYM_CONSTANT_LEXEME:  return syms.make_sym_constant (lexer.getCurrentLexeme().string);
        case VARIABLE_LEXEME:  return syms.make_variable (lexer.getCurrentLexeme().string);
        case INT_CONSTANT_LEXEME:  return syms.make_int_constant (lexer.getCurrentLexeme().int_val);
        case FLOAT_CONSTANT_LEXEME:  return syms.make_float_constant (lexer.getCurrentLexeme().float_val);
        
        case IDENTIFIER_LEXEME: throw new IllegalStateException("Internal error:  ID found in make_symbol_for_current_lexeme");
        default: throw new IllegalStateException("bad lexeme type in make_symbol_for_current_lexeme: " + lexer.getCurrentLexeme());
        }
        //return null; /* unreachable, but without it, gcc -Wall warns here */
    }

/* =================================================================
                   Placeholder (Dummy) Variables
   
   In attribute paths (and some other places) we need to create dummy
   variables.  But we need to make sure these dummy variables don't
   accidently have the same names as variables that occur later in
   the user's production.  So, we create "placeholder" variables, whose
   names have funky characters in them so they couldn't possibly occur
   in user-written code.  When we're all done parsing the production, we
   go back and replace the placeholder variables with "real" variables 
   (names without funky characters), making sure the real variables
   don't occur anywhere else in the production.
================================================================= */


    private void reset_placeholder_variable_generator () 
    {
      for (int i=0; i<placeholder_counter.length; i++)
      {
          placeholder_counter[i] = 1;
      }
    }

/* -----------------------------------------------------------------
               Make Placeholder (Dummy) Equality Test
   
   Creates and returns a test for equality with a newly generated
   placeholder variable.
----------------------------------------------------------------- */

private Test make_placeholder_test (char first_letter) {

  first_letter = Character.isLetter(first_letter) ? Character.toLowerCase(first_letter) : 'v';
  
  /* --- create variable with "#" in its name:  this couldn't possibly be a
  variable in the user's code, since the lexer doesn't handle "#" --- */
  String namebuf = ("<#" + first_letter) + (placeholder_counter[first_letter - 'a']++ + ">");

  Variable new_var = syms.make_variable (namebuf);
  /* --- indicate that there is no corresponding "real" variable yet --- */
  new_var.current_binding_value = null; 
  
  /* --- return an equality test for that variable --- */
  return new EqualityTest(new_var);
  //return make_equality_test_without_adding_reference (new_var);
}

/* -----------------------------------------------------------------
            Substituting Real Variables for Placeholders
   
   When done parsing the production, we go back and substitute "real"
   variables for all the placeholders.  This is done by walking all the
   LHS conditions and destructively modifying any tests involving
   placeholders.  The placeholder-->real mapping is maintained on each
   placeholder symbol: placeholder->var.current_binding_value is the
   corresponding "real" variable, or null if no such "real" variable has
   been created yet.

   To use this, first call reset_variable_generator (lhs, rhs) with the
   lhs and rhs of the production just parsed; then call
   substitute_for_placeholders_in_condition_list (lhs).
----------------------------------------------------------------- */

private void substitute_for_placeholders_in_symbol (Symbol[] sym) {

  /* --- if not a variable, do nothing --- */
  Variable asVar = sym[0].asVariable();
  if (asVar == null) { return; }
  /* --- if not a placeholder variable, do nothing --- */
  if (asVar.name.charAt(1) != '#'){ return; }

  //boolean just_created = false;
  
  if (asVar.current_binding_value == null) {
      String prefix = "";
      prefix += asVar.name.charAt(2);
      prefix += '*';
    //prefix[0] = *((*sym)->var.name + 2);
    //prefix[1] = '*';
    //prefix[2] = 0;
    asVar.current_binding_value = varGen.generate_new_variable (prefix);
    //just_created = true;
  }

  Symbol var = asVar.current_binding_value;
  //symbol_remove_ref (thisAgent, sym[0]);
  sym[0] = var;
  //if (!just_created) symbol_add_ref (var);
}

private void substitute_for_placeholders_in_test (Test t) {

  if (t.isBlank()) return;
  EqualityTest eqTest = t.asEqualityTest();
  if (eqTest != null) {
     Symbol sym[] = new Symbol[] { eqTest.sym }; // pointer to pointer trick
    substitute_for_placeholders_in_symbol (sym /*(Symbol **) t*/);
    eqTest.sym = sym[0];
    /* Warning: this relies on the representation of tests */
    return;
  }

  Test ct = t.asComplexTest();
  if(ct.asGoalIdTest() != null || ct.asImpasseIdTest() != null || ct.asDisjunctionTest() != null)
  {
      return;
  }
  ConjunctiveTest conjunctive = ct.asConjunctiveTest();
  if(conjunctive != null)
  {
      for(Test child : conjunctive.conjunct_list)
      {
          substitute_for_placeholders_in_test(child);
      }
//      for (c=ct->data.conjunct_list; c!=null; c=c->rest)
//          substitute_for_placeholders_in_test (thisAgent, (test *)(&(c->first)));
      
      return;
  }
  /* relational tests other than equality */
  RelationalTest relational = ct.asRelationalTest();
  if(relational != null)
  {
      Symbol[] sym = new Symbol[] { relational.referent };
      substitute_for_placeholders_in_symbol (sym);
      relational.referent = sym[0];
      return;
  }
  throw new IllegalStateException("Unexpected complex test: " + ct);
}

private void substitute_for_placeholders_in_condition_list (Condition cond) {
  for ( ; cond!=null; cond=cond.next) {
    ThreeFieldCondition tfc = cond.asThreeFieldCondition();
    if(tfc != null)
    {
//    case POSITIVE_CONDITION:
//    case NEGATIVE_CONDITION:
      substitute_for_placeholders_in_test (tfc.id_test);
      substitute_for_placeholders_in_test (tfc.attr_test);
      substitute_for_placeholders_in_test (tfc.value_test);
    }
    ConjunctiveNegationCondition ncc = cond.asConjunctiveNegationCondition();
    if(ncc != null)
    {
    //case CONJUNCTIVE_NEGATION_CONDITION:
      substitute_for_placeholders_in_condition_list (ncc.top);
    }
  }
}

/* begin KJC 10/19/98 */
private void substitute_for_placeholders_in_action_list (Action a) {
  for ( ; a!=null; a=a.next) {
      MakeAction ma = a.asMakeAction();
      if(ma != null)
      {
          RhsSymbolValue id = ma.id.asSymbolValue();
          if(id != null)
          {
              Symbol[] sym = new Symbol[] { id.sym };
              substitute_for_placeholders_in_symbol(sym);
              id.sym = sym[0];
          }
          RhsSymbolValue attr = ma.attr.asSymbolValue();
          if(attr != null)
          {
              Symbol[] sym = new Symbol[] { attr.sym };
              substitute_for_placeholders_in_symbol(sym);
              attr.sym = sym[0];
          }
          RhsSymbolValue value = ma.value.asSymbolValue();
          if(value != null)
          {
              Symbol[] sym = new Symbol[] { value.sym };
              substitute_for_placeholders_in_symbol(sym);
              value.sym = sym[0];
          }
      }
  }
}
/* end KJC 10/19/98 */





/* =================================================================
                          Routines for Tests

   The following routines are used to parse and build <test>'s.
   At entry, they expect the current lexeme to be the start of a
   test.  At exit, they leave the current lexeme at the first lexeme
   following the test.  They return the test read, or null if any
   error occurred.  (They never return a blank_test.)
================================================================= */

/* -----------------------------------------------------------------
                      Parse Relational Test
                      
   <relational_test> ::= [<relation>] <single_test>
   <relation> ::= <> | < | > | <= | >= | = | <=>
   <single_test> ::= variable | <constant>
   <constant> ::= sym_constant | int_constant | float_constant
----------------------------------------------------------------- */

/*package*/ Test parse_relational_test () throws IOException {
  boolean use_equality_test = false;
  Test t = null;
  
  int test_type = RelationalTest.NOT_EQUAL_TEST; /* unnecessary, but gcc -Wall warns without it */

  /* --- read optional relation symbol --- */
  switch(lexer.getCurrentLexeme().type) {
  case EQUAL_LEXEME:
    use_equality_test = true;
    lexer.getNextLexeme();
    break;

  case NOT_EQUAL_LEXEME:
    test_type = RelationalTest.NOT_EQUAL_TEST;
    lexer.getNextLexeme();
    break;
    
  case LESS_LEXEME:
    test_type = RelationalTest.LESS_TEST;
    lexer.getNextLexeme();
    break;

  case GREATER_LEXEME:
    test_type = RelationalTest.GREATER_TEST;
    lexer.getNextLexeme();
    break;

  case LESS_EQUAL_LEXEME:
    test_type = RelationalTest.LESS_OR_EQUAL_TEST;
    lexer.getNextLexeme();
    break;

  case GREATER_EQUAL_LEXEME:
    test_type = RelationalTest.GREATER_OR_EQUAL_TEST;
    lexer.getNextLexeme();
    break;

  case LESS_EQUAL_GREATER_LEXEME:
    test_type = RelationalTest.SAME_TYPE_TEST;
    lexer.getNextLexeme();
    break;

  default:
    use_equality_test = true;
    break;
  }

  /* --- read variable or constant --- */
  switch (lexer.getCurrentLexeme().type) {
  case SYM_CONSTANT_LEXEME:
  case INT_CONSTANT_LEXEME:
  case FLOAT_CONSTANT_LEXEME:
  case VARIABLE_LEXEME: {
    Symbol referent = make_symbol_for_current_lexeme();
    lexer.getNextLexeme();
    if (use_equality_test) {
      t = new EqualityTest(referent);
    } else {
      t = new RelationalTest(test_type, referent);
    }
    return t;
  }
  default:
      // TODO:
      throw new IllegalStateException("Expected variable or constant for test");
    //print (thisAgent, "Expected variable or constant for test\n");
    //print_location_of_most_recent_lexeme(thisAgent);
    // return null;
  }
}

/* -----------------------------------------------------------------
                      Parse Disjunction Test
                      
   <disjunction_test> ::= << <constant>* >>
   <constant> ::= sym_constant | int_constant | float_constant
----------------------------------------------------------------- */

/*package*/ DisjunctionTest parse_disjunction_test () throws IOException {

  if (lexer.getCurrentLexeme().type!=LESS_LESS_LEXEME) {
      throw new IllegalStateException("Expected << to begin disjunction test");
      // TODO
//    print (thisAgent, "Expected << to begin disjunction test\n");
//    print_location_of_most_recent_lexeme(thisAgent);
//    return null;
  }
  lexer.getNextLexeme();

  DisjunctionTest ct = new DisjunctionTest();

  while (lexer.getCurrentLexeme().type!=GREATER_GREATER_LEXEME) {
    switch (lexer.getCurrentLexeme().type) {
    case SYM_CONSTANT_LEXEME:
    case INT_CONSTANT_LEXEME:
    case FLOAT_CONSTANT_LEXEME:
      ct.disjunction_list.add(make_symbol_for_current_lexeme());
      lexer.getNextLexeme();
      break;
    default:
      throw new IllegalStateException("Expected constant or >> while reading disjunction test");
    // TODO
//      print (thisAgent, "Expected constant or >> while reading disjunction test\n");
//      print_location_of_most_recent_lexeme(thisAgent);
//      deallocate_test (thisAgent, t);
//      return null;
    }
  }
  lexer.getNextLexeme();  /* consume the >> */
  //ct->data.disjunction_list = destructively_reverse_list (ct->data.disjunction_list);
  return ct;
}

/* -----------------------------------------------------------------
                        Parse Simple Test
                      
   <simple_test> ::= <disjunction_test> | <relational_test>
----------------------------------------------------------------- */

/*package*/ Test parse_simple_test () throws IOException {
  if (lexer.getCurrentLexeme().type==LESS_LESS_LEXEME)
    return parse_disjunction_test();
  return parse_relational_test();
}

/* -----------------------------------------------------------------
                            Parse Test
                      
    <test> ::= <conjunctive_test> | <simple_test>
    <conjunctive_test> ::= { <simple_test>+ }
----------------------------------------------------------------- */

/*package*/ Test parse_test () throws IOException {

  if (lexer.getCurrentLexeme().type!=L_BRACE_LEXEME)
    return parse_simple_test();
  /* --- parse and return conjunctive test --- */
  lexer.getNextLexeme();
  ByRef<Test> t = ByRef.create(new EqualityTest(null)); // make_blank_test();
  do {
    Test temp = parse_simple_test();
    if (temp == null) {
      //deallocate_test(thisAgent, t);
      return null;
    }
    TestTools.add_new_test_to_test(t, temp);
  } while (lexer.getCurrentLexeme().type!=R_BRACE_LEXEME);
  lexer.getNextLexeme(); /* consume the "}" */

  Test ct = t.value.asComplexTest();
  if (ct != null) {
    ConjunctiveTest cjt = ct.asConjunctiveTest();
    if (cjt != null)
    {
        Collections.reverse(cjt.conjunct_list);
        //ct->data.conjunct_list = destructively_reverse_list (ct->data.conjunct_list);
    }
  }

  return t.value;
}

/* =================================================================
                        Routines for Conditions

   Various routines here are used to parse and build conditions, etc.
   At entry, each expects the current lexeme to be at the start of whatever
   thing they're supposed to parse.  At exit, each leaves the current
   lexeme at the first lexeme following the parsed object.  Each returns
   a list of the conditions they parsed, or null if any error occurred.
================================================================= */

/* -----------------------------------------------------------------
                          Fill In Id Tests
                         Fill In Attr Tests

   As low-level routines (e.g., parse_value_test_star) parse, they
   leave the id (and sometimes attribute) test fields blank (null) in the
   condition structures they return.  The calling routine must fill in
   the id tests and/or attribute tests.  These routines fill in any
   still-blank {id, attr} tests with copies of a given test.  They try
   to add non-equality portions of the test only once, if possible.
----------------------------------------------------------------- */

private void fill_in_id_tests (Condition conds, Test t) {
  PositiveCondition positive_c = null;

  /* --- see if there's at least one positive condition --- */
  for (Condition c=conds; c!=null; c=c.next)
  {
      positive_c = c.asPositiveCondition(); 
    if (positive_c != null &&
        (positive_c.id_test==null)) 
    {  
        break; 
    }
  }

  
  if (positive_c != null) {  /* --- there is at least one positive condition --- */
    /* --- add just the equality test to most of the conditions --- */
    EqualityTest equality_test_from_t = TestTools.copy_of_equality_test_found_in_test (t);
    for (Condition c=conds; c!=null; c=c.next) {
      ConjunctiveNegationCondition ncc = c.asConjunctiveNegationCondition();
      ThreeFieldCondition tfc = c.asThreeFieldCondition();
      if (ncc != null) {
        fill_in_id_tests (ncc.top, equality_test_from_t);
      }
      else if (tfc.id_test==null) {
        tfc.id_test = equality_test_from_t.copy();
      }
    }
    //deallocate_test (thisAgent, equality_test_from_t);
    
    /* --- add the whole test to one positive condition --- */
    //deallocate_test (thisAgent, positive_c->data.tests.id_test);
    positive_c.id_test = t.copy();
    return;
  }

  /* --- all conditions are negative --- */      
  for (Condition c=conds; c!=null; c=c.next) {
      ConjunctiveNegationCondition ncc = c.asConjunctiveNegationCondition();
      ThreeFieldCondition tfc = c.asThreeFieldCondition();
    if (ncc != null) {
      fill_in_id_tests (ncc.top, t);
    } else {
      if (tfc.id_test==null) {
        tfc.id_test = t.copy(); 
      }
    }
  }
}

private void fill_in_attr_tests (Condition conds, Test t) {

  PositiveCondition positive_c = null;

  /* --- see if there's at least one positive condition --- */
  for (Condition c=conds; c!=null; c=c.next)
  {
      positive_c = c.asPositiveCondition(); 
    if (positive_c != null &&
        (positive_c.attr_test==null)) 
    {  
        break; 
    }
  }

  if (positive_c != null) {  /* --- there is at least one positive condition --- */
    /* --- add just the equality test to most of the conditions --- */
    EqualityTest equality_test_from_t = TestTools.copy_of_equality_test_found_in_test (t);
    for (Condition c=conds; c!=null; c=c.next) {
      ConjunctiveNegationCondition ncc = c.asConjunctiveNegationCondition();
      ThreeFieldCondition tfc = c.asThreeFieldCondition();
      if (ncc != null) {
        fill_in_attr_tests (ncc.top, equality_test_from_t);
      }
      else if (tfc.attr_test==null) {
        tfc.attr_test = equality_test_from_t.copy();
      }
    }
    //deallocate_test (thisAgent, equality_test_from_t);
    /* --- add the whole test to one positive condition --- */
    //deallocate_test (thisAgent, positive_c->data.tests.attr_test);
    positive_c.attr_test = t.copy();
    return;
  }

  /* --- all conditions are negative --- */      
  for (Condition c=conds; c!=null; c=c.next) {
      ConjunctiveNegationCondition ncc = c.asConjunctiveNegationCondition();
      ThreeFieldCondition tfc = c.asThreeFieldCondition();
    if (ncc != null) {
      fill_in_attr_tests (ncc.top, t);
    } else {
      if (tfc.attr_test==null)
      {
        tfc.attr_test = t.copy();
      }
    }
  }
}

/* -----------------------------------------------------------------
                     Negate Condition List
   
   Returns the negation of the given condition list.  If the given
   list is a single positive or negative condition, it just toggles
   the type.  If the given list is a single ncc, it strips off the ncc
   part and returns the subconditions.  Otherwise it makes a new ncc
   using the given conditions.
----------------------------------------------------------------- */

private Condition negate_condition_list (Condition conds) {

  if (conds.next==null) {
    /* --- only one condition to negate, so toggle the type --- */
    PositiveCondition pc = conds.asPositiveCondition();
    if(pc != null)
    {
        return pc.negate();
    }
    NegativeCondition nc = conds.asNegativeCondition();
    if(nc != null)
    {
        return nc.negate();
    }
    ConjunctiveNegationCondition ncc = conds.asConjunctiveNegationCondition();
    if(ncc != null)
    {
        return ncc.top;
    }
    throw new IllegalStateException("Unknown condition type: " + conds);
  }
  /* --- more than one condition; so build a conjunctive negation --- */
  ConjunctiveNegationCondition tempNcc = new ConjunctiveNegationCondition();
  tempNcc.top = conds;
  Condition last = conds;
  for (; last.next!=null; last=last.next) {}
  tempNcc.bottom = last;
  return tempNcc;
}

/* -----------------------------------------------------------------
                        Parse Value Test Star
                      
   <value_test> ::= <test> [+] | <conds_for_one_id> [+]

   (This routine parses <value_test>*, given as input the id_test and
   attr_test already read.)
----------------------------------------------------------------- */

/*package*/ Condition parse_value_test_star (char first_letter) throws IOException {

  if ((lexer.getCurrentLexeme().type==MINUS_LEXEME) ||
      (lexer.getCurrentLexeme().type==UP_ARROW_LEXEME) ||
      (lexer.getCurrentLexeme().type==R_PAREN_LEXEME)) 
  {
    /* --- value omitted, so create dummy value test --- */
      PositiveCondition c = new PositiveCondition();
      c.value_test = make_placeholder_test(first_letter);
      return c;
  }

  Condition last_c = null, first_c = null, new_conds = null;
  ByRef<Test> value_test = ByRef.create(null);
  boolean acceptable = false;
  do {
    if (lexer.getCurrentLexeme().type==L_PAREN_LEXEME) {
      /* --- read <conds_for_one_id>, take the id_test from it --- */
      new_conds = parse_conds_for_one_id (first_letter, value_test);
      if (new_conds == null) {
          // TODO
        //deallocate_condition_list (thisAgent, first_c);
        return null;
      }
    } else {
      /* --- read <value_test> --- */
      new_conds = null;
      value_test.value = parse_test();
      if (value_test.value == null) {
          // TODO
        //deallocate_condition_list (thisAgent, first_c);
        return null;
      }
      if (! TestTools.test_includes_equality_test_for_symbol (value_test.value, null)) {
        TestTools.add_new_test_to_test (value_test,make_placeholder_test(first_letter));
      }
    }
    /* --- check for acceptable preference indicator --- */
    acceptable = false;
    if (lexer.getCurrentLexeme().type==PLUS_LEXEME) 
    { 
        acceptable = true; lexer.getNextLexeme(); 
    }
    /* --- build condition using the new value test --- */
    PositiveCondition pc = new PositiveCondition();
    pc.value_test = value_test.value;
    pc.test_for_acceptable_preference = acceptable;
    new_conds = Condition.insertAtHead(new_conds, pc);
    
    /* --- add new conditions to the end of the list --- */
    if (last_c != null){ last_c.next = new_conds; } else { first_c = new_conds; }
    new_conds.prev = last_c;
    for (last_c=new_conds; last_c.next!=null; last_c=last_c.next) {}
  } while ((lexer.getCurrentLexeme().type!=MINUS_LEXEME) &&
           (lexer.getCurrentLexeme().type!=UP_ARROW_LEXEME) &&
           (lexer.getCurrentLexeme().type!=R_PAREN_LEXEME));
  return first_c;
}

/* -----------------------------------------------------------------
                      Parse Attr Value Tests
                      
   <attr_value_tests> ::= [-] ^ <attr_test> [.<attr_test>]* <value_test>*
   <attr_test> ::= <test>

   (This routine parses <attr_value_tests>, given as input the id_test 
   already read.)
----------------------------------------------------------------- */

/*package*/ Condition parse_attr_value_tests () throws IOException {
  boolean negate_it;
  
  /* --- read optional minus sign --- */
  negate_it = false;
  if (lexer.getCurrentLexeme().type==MINUS_LEXEME) { negate_it = true; lexer.getNextLexeme(); }
  
  /* --- read up arrow --- */
  if (lexer.getCurrentLexeme().type!=UP_ARROW_LEXEME) {
      // TODO
      throw new IllegalStateException("Expected ^ followed by attribute");
//    print (thisAgent, "Expected ^ followed by attribute\n");
//    print_location_of_most_recent_lexeme(thisAgent);
//    return null;
  }
  lexer.getNextLexeme();

  
  /* --- read first <attr_test> --- */
  ByRef<Test> attr_test = ByRef.create(parse_test());
  if (attr_test == null) 
  {
      return null;
  }
  if (! TestTools.test_includes_equality_test_for_symbol (attr_test.value, null)) {
    TestTools.add_new_test_to_test (attr_test, make_placeholder_test ('a'));
  }

  /* --- read optional attribute path --- */
  Condition first_c = null;
  Condition last_c = null;
  Test id_test_to_use = null;
  while (lexer.getCurrentLexeme().type==PERIOD_LEXEME) {
    lexer.getNextLexeme();  /* consume the "." */
    /* --- setup for next attribute in path:  make a dummy variable,
       create a new condition in the path --- */
    PositiveCondition c = new PositiveCondition();
    if (last_c != null) { last_c.next = c; } else { first_c = c; }
    c.next = null;
    c.prev = last_c;
    last_c = c;
    if (id_test_to_use != null) {
      c.id_test = id_test_to_use.copy();
    }
    else {
      c.id_test = null;
    }
    c.attr_test = attr_test.value;
    
    id_test_to_use = make_placeholder_test (TestTools.first_letter_from_test(attr_test.value));
    c.value_test = id_test_to_use;
    c.test_for_acceptable_preference = false;
    
    /* --- update id and attr tests for the next path element --- */
    attr_test.value = parse_test ();
    if (attr_test.value == null) {
      //deallocate_condition_list (thisAgent, first_c);
      return null;
    }
/* AGR 544 begin */
    if (! TestTools.test_includes_equality_test_for_symbol (attr_test.value, null)) {
      TestTools.add_new_test_to_test (attr_test,make_placeholder_test('a'));
    }
/* AGR 544 end */
  } /* end of while (lexer.getCurrentLexeme().type==PERIOD_LEXEME) */

  /* --- finally, do the <value_test>* part --- */  
  Condition new_conds = parse_value_test_star (TestTools.first_letter_from_test (attr_test.value));
  if (new_conds == null) {
    //deallocate_condition_list (thisAgent, first_c);
    //deallocate_test (thisAgent, attr_test);
    return null;
  }
  fill_in_attr_tests (new_conds, attr_test.value);
  if (id_test_to_use != null) { fill_in_id_tests (new_conds, id_test_to_use); }
  //deallocate_test (thisAgent, attr_test);
  if (last_c != null) { last_c.next = new_conds; } else { first_c = new_conds; }
  new_conds.prev = last_c;
  /* should update last_c here, but it's not needed anymore */
  
  /* --- negate everything if necessary --- */
  if (negate_it){  first_c = negate_condition_list (first_c); }

  return first_c;
}

/* -----------------------------------------------------------------
                    Parse Head Of Conds For One Id
                      
   <conds_for_one_id> ::= ( [state|impasse] [<id_test>] <attr_value_tests>* )
   <id_test> ::= <test>

   This routine parses the "( [state|impasse] [<id_test>]" part of
   <conds_for_one_id> and returns the resulting id_test (or null if
   any error occurs).
----------------------------------------------------------------- */

private Test parse_head_of_conds_for_one_id (char first_letter_if_no_id_given) throws IOException {

  if (lexer.getCurrentLexeme().type!=L_PAREN_LEXEME) {
      // TODO
      throw new IllegalStateException("Expected ( to begin condition element");
//    print (thisAgent, "Expected ( to begin condition element\n");
//    print_location_of_most_recent_lexeme(thisAgent);
//    return null;
  }
  lexer.getNextLexeme();

  Test id_goal_impasse_test = null;
  /* --- look for goal/impasse indicator --- */
  if (lexer.getCurrentLexeme().type==SYM_CONSTANT_LEXEME) {
    if (lexer.getCurrentLexeme().string.equals("state")) {
      id_goal_impasse_test = new GoalIdTest();
      lexer.getNextLexeme();
      first_letter_if_no_id_given = 's';
    } else if (lexer.getCurrentLexeme().string.equals("impasse")) {
      id_goal_impasse_test = new ImpasseIdTest();
      lexer.getNextLexeme();
      first_letter_if_no_id_given = 'i';
    } else {
      id_goal_impasse_test = new EqualityTest(null); // make_blank_test();
    }
  } else {
    id_goal_impasse_test =  new EqualityTest(null); // make_blank_test();
  }

  ByRef<Test> id_test = ByRef.create(null);
  Test check_for_symconstant;
  Symbol sym;
  /* --- read optional id test; create dummy one if none given --- */
  if ((lexer.getCurrentLexeme().type!=MINUS_LEXEME) &&
      (lexer.getCurrentLexeme().type!=UP_ARROW_LEXEME) &&
      (lexer.getCurrentLexeme().type!=R_PAREN_LEXEME)) {
    id_test.value = parse_test();
    if (id_test.value == null) {
      //deallocate_test (thisAgent, id_goal_impasse_test);
      return null;
    }
    if (! TestTools.test_includes_equality_test_for_symbol (id_test.value, null)) {
      TestTools.add_new_test_to_test(id_test, make_placeholder_test(first_letter_if_no_id_given));
    } else {
      check_for_symconstant = TestTools.copy_of_equality_test_found_in_test(id_test.value);
      sym = check_for_symconstant.asEqualityTest().sym; // referent_of_equality_test(check_for_symconstant);
      //deallocate_test (thisAgent, check_for_symconstant); /* RBD added 3/28/95 */
      if(sym.asVariable() == null) {
          // TODO
        //print_with_symbols(thisAgent, "Warning: Constant %y in id field test.\n", sym);
        //print(thisAgent, "         This will never match.\n");

//		growable_string gs = make_blank_growable_string(thisAgent);
//		add_to_growable_string(thisAgent, &gs, "Warning: Constant ");
//		add_to_growable_string(thisAgent, &gs, symbol_to_string(thisAgent, sym, true, 0, 0));
//		add_to_growable_string(thisAgent, &gs, " in id field test.\n         This will never match.");
//		xml_generate_warning(thisAgent, text_of_growable_string(gs));
//		free_growable_string(thisAgent, gs);
//		//TODO: should we append this to the previous XML message or create a new message for it?
//        print_location_of_most_recent_lexeme(thisAgent);
//	deallocate_test (thisAgent, id_test);   /* AGR 527c */
	return null;                  /* AGR 527c */
      } 
    }
  } else {
    id_test.value = make_placeholder_test (first_letter_if_no_id_given);
  }
  
  /* --- add the goal/impasse test to the id test --- */
  TestTools.add_new_test_to_test (id_test, id_goal_impasse_test);

  /* --- return the resulting id test --- */
  return id_test.value;
}

/* -----------------------------------------------------------------
                    Parse Tail Of Conds For One Id
                      
   <conds_for_one_id> ::= ( [state|impasse] [<id_test>] <attr_value_tests>* )
   <id_test> ::= <test>

   This routine parses the "<attr_value_tests>* )" part of <conds_for_one_id>
   and returns the resulting conditions (or null if any error occurs).
   It does not fill in the id tests of the conditions.
----------------------------------------------------------------- */

private Condition parse_tail_of_conds_for_one_id () throws IOException {

  /* --- if no <attr_value_tests> are given, create a dummy one --- */
  if (lexer.getCurrentLexeme().type==R_PAREN_LEXEME) {
    lexer.getNextLexeme();       /* consume the right parenthesis */
    PositiveCondition c = new PositiveCondition();
    c.attr_test = make_placeholder_test ('a');
    c.value_test = make_placeholder_test ('v');
    c.test_for_acceptable_preference = false;
    return c;
  }

  /* --- read <attr_value_tests>* --- */
  Condition first_c = null;
  Condition last_c = null;
  Condition new_conds = null;
  while (lexer.getCurrentLexeme().type!=R_PAREN_LEXEME) {
    new_conds = parse_attr_value_tests ();
    if (new_conds == null) {
      //deallocate_condition_list (thisAgent, first_c);
      return null;
    }
    if (last_c != null) { last_c.next = new_conds; } else { first_c = new_conds;}
    new_conds.prev = last_c;
    for (last_c=new_conds; last_c.next!=null; last_c=last_c.next) { /*nothing*/ }
  }

  /* --- reached the end of the condition --- */
  lexer.getNextLexeme();       /* consume the right parenthesis */

  return first_c;
}

/* -----------------------------------------------------------------
                      Parse Conds For One Id
                      
   <conds_for_one_id> ::= ( [state|impasse] [<id_test>] <attr_value_tests>* )
   <id_test> ::= <test>

   This routine parses <conds_for_one_id> and returns the conditions (of
   null if any error occurs).

   If the argument dest_id_test is non-NULL, then *dest_id_test is set
   to the resulting complete id test (which includes any goal/impasse test),
   and in all the conditions the id field is filled in with just the
   equality portion of id_test.

   If the argument dest_id_test is NULL, then the complete id_test is
   included in the conditions.
----------------------------------------------------------------- */

/*package*/ Condition parse_conds_for_one_id (char first_letter_if_no_id_given,
        ByRef<Test> dest_id_test) throws IOException {
  /* --- parse the head --- */
  Test id_test = parse_head_of_conds_for_one_id (first_letter_if_no_id_given);
  if (id_test == null) { return null; }

  /* --- parse the tail --- */
  Condition conds = parse_tail_of_conds_for_one_id ();
  if (conds == null) {
    //deallocate_test (thisAgent, id_test);
    return null;
  }

  /* --- fill in the id test in all the conditions just read --- */
  if (dest_id_test != null) {
    dest_id_test.value = id_test;
    Test equality_test_from_id_test = TestTools.copy_of_equality_test_found_in_test (id_test);
    fill_in_id_tests (conds, equality_test_from_id_test);
    //deallocate_test (thisAgent, equality_test_from_id_test);
  } else {
    fill_in_id_tests (conds, id_test);
    //deallocate_test (thisAgent, id_test);
  }

  return conds;
}

/* -----------------------------------------------------------------
                            Parse Cond
                      
   <cond> ::= <positive_cond> | - <positive_cond>
   <positive_cond> ::= <conds_for_one_id> | { <cond>+ }
----------------------------------------------------------------- */

/*package*/ Condition parse_cond () throws IOException {

  /* --- look for leading "-" sign --- */
  boolean negate_it = false;
  if (lexer.getCurrentLexeme().type==MINUS_LEXEME) { negate_it = true; lexer.getNextLexeme(); }

  Condition c = null;
  /* --- parse <positive_cond> --- */
  if (lexer.getCurrentLexeme().type==L_BRACE_LEXEME) {
    /* --- read conjunctive condition --- */
    lexer.getNextLexeme();
    c = parse_cond_plus();
    if (c == null) {
        return null;
    }
    if (lexer.getCurrentLexeme().type!=R_BRACE_LEXEME) {
        // TODO
        throw new IllegalStateException("Expected } to end conjunctive condition\n");
//      print (thisAgent, "Expected } to end conjunctive condition\n");
//      print_location_of_most_recent_lexeme(thisAgent);
//      deallocate_condition_list (thisAgent, c);
//      return null;
    }
    lexer.getNextLexeme();  /* consume the R_BRACE */
  } else {
    /* --- read conds for one id --- */
    c = parse_conds_for_one_id ('s', null);
    if (c == null) { return null; }
  } 

  /* --- if necessary, handle the negation --- */
  if (negate_it) { c = negate_condition_list (c); }
     
  return c;
}

/* -----------------------------------------------------------------
                            Parse Cond Plus
                      
   (Parses <cond>+ and builds a condition list.)
----------------------------------------------------------------- */

/*package*/ Condition parse_cond_plus () throws IOException {

  Condition first_c = null;
  Condition last_c = null;
  do {
    /* --- get individual <cond> --- */
    Condition new_conds = parse_cond ();
    if (new_conds == null) {
      //deallocate_condition_list (thisAgent, first_c);
      return null;
    }
    if (last_c != null) { last_c.next = new_conds; } else { first_c = new_conds; }
    new_conds.prev = last_c;
    for (last_c=new_conds; last_c.next!=null; last_c=last_c.next) { /* nothing */ }
  } while ((lexer.getCurrentLexeme().type==MINUS_LEXEME) ||
           (lexer.getCurrentLexeme().type==L_PAREN_LEXEME) ||
           (lexer.getCurrentLexeme().type==L_BRACE_LEXEME));
  return first_c;
}

/* -----------------------------------------------------------------
                            Parse LHS
                      
   (Parses <lhs> and builds a condition list.)

   <lhs> ::= <cond>+
----------------------------------------------------------------- */

/*package*/ Condition parse_lhs () throws IOException {
  return parse_cond_plus ();
}



/* =================================================================

                        Routines for Actions

   The following routines are used to parse and build actions, etc.
   Except as otherwise noted, at entry each routine expects the
   current lexeme to be at the start of whatever thing it's supposed
   to parse.  At exit, each leaves the current lexeme at the first
   lexeme following the parsed object.
================================================================= */

/* =====================================================================

   Grammar for right hand sides of productions

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
===================================================================== */

public static final String help_on_rhs_grammar[] = new String[] {
"Grammar for right hand sides of productions:",
"",
"   <rhs> ::= <rhs_action>*",
"   <rhs_action> ::= ( variable <attr_value_make>+ ) | <function_call>",
"   <function_call> ::= ( <function_name> <rhs_value>* )",
"   <function_name> ::= sym_constant | + | -",
"   <rhs_value> ::= <constant> | <function_call> | variable",
"   <constant> ::= sym_constant | int_constant | float_constant",
"   <attr_value_make> ::= ^ <rhs_value> <value_make>+",
"   <value_make> ::= <rhs_value> <preferences>",
"",
"   <preferences> ::= [,] | <preference_specifier>+",
"   <preference-specifier> ::= <naturally-unary-preference> [,]",
"                            | <forced-unary-preference>",
"                            | <binary-preference> <rhs_value> [,]",
"   <naturally-unary-preference> ::= + | - | ! | ~ | @",
"   <binary-preference> ::= > | = | < | &",
"   <any-preference> ::= <naturally-unary-preference> | <binary-preference>",
"   <forced-unary-preference> ::= <binary-preference> ",
"                                 {<any-preference> | , | ) | ^}",
"     ;but the parser shouldn't consume the <any-preference>, \")\" or \"^\"",
"      lexeme here",
"",
"See also:  lhs-grammar, sp" };

/* -----------------------------------------------------------------
                 Parse Function Call After Lparen

   Parses a <function_call> after the "(" has already been consumed.
   At entry, the current lexeme should be the function name.  Returns
   an rhs_value, or null if any error occurred.

   <function_call> ::= ( <function_name> <rhs_value>* )
   <function_name> ::= sym_constant | + | -
     (Warning: might need others besides +, - here if the lexer changes)
----------------------------------------------------------------- */

/*package*/ RhsFunctionCall parse_function_call_after_lparen (boolean is_stand_alone_action) throws IOException {
  SymConstant fun_name;

  /* --- read function name, find the rhs_function structure --- */
  if (lexer.getCurrentLexeme().type==PLUS_LEXEME) { fun_name = syms.make_sym_constant ("+"); }
  else if (lexer.getCurrentLexeme().type==MINUS_LEXEME) { fun_name = syms.make_sym_constant ("-"); }
  else { fun_name = syms.make_sym_constant (lexer.getCurrentLexeme().string); }
//  if (fun_name == null) {
//      // TODO
//      throw new IllegalStateException("No RHS function named " + lexer.getCurrentLexeme().string);
////    print (thisAgent, "No RHS function named %s\n",lexer.getCurrentLexeme().string);
////    print_location_of_most_recent_lexeme(thisAgent);
////    return null;
//  }
//  rf = lookup_rhs_function (thisAgent, fun_name);
//  if (!rf) {
//    print (thisAgent, "No RHS function named %s\n",lexer.getCurrentLexeme().string);
//    print_location_of_most_recent_lexeme(thisAgent);
//    return null;
//  }

  /* --- make sure stand-alone/rhs_value is appropriate --- */
//  if (is_stand_alone_action && (! rf->can_be_stand_alone_action)) {
//    print (thisAgent, "Function %s cannot be used as a stand-alone action\n",
//           lexer.getCurrentLexeme().string);
//    print_location_of_most_recent_lexeme(thisAgent);
//    return null;
//  }
//  if ((! is_stand_alone_action) && (! rf->can_be_rhs_value)) {
//    print (thisAgent, "Function %s can only be used as a stand-alone action\n",
//           lexer.getCurrentLexeme().string);
//    print_location_of_most_recent_lexeme(thisAgent);
//    return null;
//  }

  /* --- build list of rhs_function and arguments --- */
  RhsFunctionCall rfc = new RhsFunctionCall(fun_name, is_stand_alone_action);
  lexer.getNextLexeme(); /* consume function name, advance to argument list */
  while (lexer.getCurrentLexeme().type!=R_PAREN_LEXEME) {
    RhsValue arg_rv = parse_rhs_value ();
    if (arg_rv == null) {
//      deallocate_rhs_value (thisAgent, funcall_list_to_rhs_value(fl));
      return null;
    }
    rfc.addArgument(arg_rv);
  }

  /* --- check number of arguments --- */
//  if ((rf->num_args_expected != -1) && (rf->num_args_expected != num_args)) {
//    print (thisAgent, "Wrong number of arguments to function %s (expected %d)\n",
//           rf->name->sc.name, rf->num_args_expected);
//    print_location_of_most_recent_lexeme(thisAgent);
//    deallocate_rhs_value (thisAgent, funcall_list_to_rhs_value(fl));
//    return null;
//  }
  
  lexer.getNextLexeme();  /* consume the right parenthesis */
  return rfc;
}

/* -----------------------------------------------------------------
                          Parse RHS Value

   Parses an <rhs_value>.  Returns an rhs_value, or null if any error
   occurred.

   <rhs_value> ::= <constant> | <function_call> | variable
   <constant> ::= sym_constant | int_constant | float_constant
----------------------------------------------------------------- */

RhsValue parse_rhs_value () throws IOException {
  
  if (lexer.getCurrentLexeme().type==L_PAREN_LEXEME) {
    lexer.getNextLexeme();
    return parse_function_call_after_lparen (false);
  }
  if ((lexer.getCurrentLexeme().type==SYM_CONSTANT_LEXEME) ||
      (lexer.getCurrentLexeme().type==INT_CONSTANT_LEXEME) ||
      (lexer.getCurrentLexeme().type==FLOAT_CONSTANT_LEXEME) ||
      (lexer.getCurrentLexeme().type==VARIABLE_LEXEME)) {
      RhsValue rv = new RhsSymbolValue(make_symbol_for_current_lexeme ());
    lexer.getNextLexeme();
    return rv;
  }
  // TODO
  throw new IllegalStateException("Illegal value for RHS value");
//  print (thisAgent, "Illegal value for RHS value\n");
//  print_location_of_most_recent_lexeme(thisAgent);
//  return false;
}


/* -----------------------------------------------------------------
                         Is Preference Lexeme

   Given a token type, returns true if the token is a preference
   lexeme.

----------------------------------------------------------------- */

boolean is_preference_lexeme( int test_lexeme )
{
  switch (test_lexeme) {
    
  case LexemeTypes.PLUS_LEXEME:
    return true;
  case LexemeTypes.MINUS_LEXEME:
    return true;
  case LexemeTypes.EXCLAMATION_POINT_LEXEME:
    return true;
  case LexemeTypes.TILDE_LEXEME:
    return true;
  case LexemeTypes.AT_LEXEME:
    return true;
  case LexemeTypes.GREATER_LEXEME:
    return true;
  case LexemeTypes.EQUAL_LEXEME:
    return true;
  case LexemeTypes.LESS_LEXEME:
    return true;
  case LexemeTypes.AMPERSAND_LEXEME:
    return true;
  default:
    return false;
  }
}

/* -----------------------------------------------------------------
               Parse Preference Specifier Without Referent

   Parses a <preference-specifier>.  Returns the appropriate 
   xxx_PREFERENCE_TYPE (see soarkernel.h).

   Note:  in addition to the grammar below, if there is no preference
   specifier given, then this routine returns ACCEPTABLE_PREFERENCE_TYPE.
   Also, for <binary-preference>'s, this routine *does not* read the
   <rhs_value> referent.  This must be done by the caller routine.

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
----------------------------------------------------------------- */

PreferenceType parse_preference_specifier_without_referent () throws IOException {
  switch (lexer.getCurrentLexeme().type) {
    
  case PLUS_LEXEME:
    lexer.getNextLexeme();
    if (lexer.getCurrentLexeme().type==COMMA_LEXEME) lexer.getNextLexeme();
    return PreferenceType.ACCEPTABLE_PREFERENCE_TYPE;
    
  case MINUS_LEXEME:
    lexer.getNextLexeme();
    if (lexer.getCurrentLexeme().type==COMMA_LEXEME) lexer.getNextLexeme();
    return PreferenceType.REJECT_PREFERENCE_TYPE;
    
  case EXCLAMATION_POINT_LEXEME:
    lexer.getNextLexeme();
    if (lexer.getCurrentLexeme().type==COMMA_LEXEME) lexer.getNextLexeme();
    return PreferenceType.REQUIRE_PREFERENCE_TYPE;
    
  case TILDE_LEXEME:
    lexer.getNextLexeme();
    if (lexer.getCurrentLexeme().type==COMMA_LEXEME) lexer.getNextLexeme();
    return PreferenceType.PROHIBIT_PREFERENCE_TYPE;
    
  case AT_LEXEME:
    lexer.getNextLexeme();
    if (lexer.getCurrentLexeme().type==COMMA_LEXEME) lexer.getNextLexeme();
    return PreferenceType.RECONSIDER_PREFERENCE_TYPE;
    
/****************************************************************************
 * [Soar-Bugs #55] <forced-unary-preference> ::= <binary-preference> 
 *                                             {<any-preference> | , | ) | ^} 
 *
 *   Forced unary preferences can now occur when a binary preference is
 *   followed by a ",", ")", "^" or any preference specifier
 ****************************************************************************/

  case GREATER_LEXEME:
    lexer.getNextLexeme();
    if (!lexer.isEof() &&
        (lexer.getCurrentLexeme().type!=COMMA_LEXEME) &&
        (lexer.getCurrentLexeme().type!=R_PAREN_LEXEME) &&
        (lexer.getCurrentLexeme().type!=UP_ARROW_LEXEME) &&
        (!is_preference_lexeme(lexer.getCurrentLexeme().type)))
      return PreferenceType.BETTER_PREFERENCE_TYPE;
    /* --- forced unary preference --- */
    if (lexer.getCurrentLexeme().type==COMMA_LEXEME) lexer.getNextLexeme();
    return PreferenceType.BEST_PREFERENCE_TYPE;
    
  case EQUAL_LEXEME:
    lexer.getNextLexeme();
    if (!lexer.isEof() &&
        (lexer.getCurrentLexeme().type!=COMMA_LEXEME) &&
        (lexer.getCurrentLexeme().type!=R_PAREN_LEXEME) &&
        (lexer.getCurrentLexeme().type!=UP_ARROW_LEXEME) &&
        (!is_preference_lexeme(lexer.getCurrentLexeme().type)))
    {
    	
		if ((lexer.getCurrentLexeme().type == INT_CONSTANT_LEXEME) ||
			  (lexer.getCurrentLexeme().type == FLOAT_CONSTANT_LEXEME))
			return PreferenceType.NUMERIC_INDIFFERENT_PREFERENCE_TYPE;
		      else
		    return PreferenceType.BINARY_INDIFFERENT_PREFERENCE_TYPE;
    }
    
    /* --- forced unary preference --- */
    if (lexer.getCurrentLexeme().type==COMMA_LEXEME) lexer.getNextLexeme();
    return PreferenceType.UNARY_INDIFFERENT_PREFERENCE_TYPE;
    
  case LESS_LEXEME:
    lexer.getNextLexeme();
    if (!lexer.isEof() &&
        (lexer.getCurrentLexeme().type!=COMMA_LEXEME) &&
        (lexer.getCurrentLexeme().type!=R_PAREN_LEXEME) &&
        (lexer.getCurrentLexeme().type!=UP_ARROW_LEXEME) &&
        (!is_preference_lexeme(lexer.getCurrentLexeme().type)))
      return PreferenceType.WORSE_PREFERENCE_TYPE;
    /* --- forced unary preference --- */
    if (lexer.getCurrentLexeme().type==COMMA_LEXEME) lexer.getNextLexeme();
    return PreferenceType.WORST_PREFERENCE_TYPE;
    
  case AMPERSAND_LEXEME:
    lexer.getNextLexeme();
    if (!lexer.isEof() &&
        (lexer.getCurrentLexeme().type!=COMMA_LEXEME) &&
        (lexer.getCurrentLexeme().type!=R_PAREN_LEXEME) &&
        (lexer.getCurrentLexeme().type!=UP_ARROW_LEXEME) &&
        (!is_preference_lexeme(lexer.getCurrentLexeme().type)))
      return PreferenceType.BINARY_PARALLEL_PREFERENCE_TYPE;
    /* --- forced unary preference --- */
    if (lexer.getCurrentLexeme().type==COMMA_LEXEME) lexer.getNextLexeme();
    return PreferenceType.UNARY_PARALLEL_PREFERENCE_TYPE;
    
  default:
    /* --- if no preference given, make it an acceptable preference --- */
    return PreferenceType.ACCEPTABLE_PREFERENCE_TYPE;
  } /* end of switch statement */
}

/* -----------------------------------------------------------------
                         Parse Preferences

   Given the id, attribute, and value already read, this routine
   parses zero or more <preference-specifier>'s.  It builds and
   returns an action list for these RHS make's.  It returns null if
   any error occurred.

   <value_make> ::= <rhs_value> <preferences>
   <preferences> ::= [,] | <preference_specifier>+   
   <preference-specifier> ::= <naturally-unary-preference> [,]
                            | <forced-unary-preference>
                            | <binary-preference> <rhs_value> [,]
----------------------------------------------------------------- */

Action parse_preferences (Symbol id, RhsValue attr, RhsValue value) throws IOException {
  RhsValue referent;
  PreferenceType preference_type;
  boolean saw_plus_sign = false;
  
  /* --- Note: this routine is set up so if there's not preference type
     indicator at all, we return a single acceptable preference make --- */
  Action prev_a = null;
  
  saw_plus_sign = (lexer.getCurrentLexeme().type==PLUS_LEXEME);
  preference_type = parse_preference_specifier_without_referent();
  if ((preference_type==PreferenceType.ACCEPTABLE_PREFERENCE_TYPE) && (! saw_plus_sign)) {
    /* If the routine gave us a + pref without seeing a + sign, then it's
       just giving us the default acceptable preference.  Look for optional
       comma. */
    if (lexer.getCurrentLexeme().type==COMMA_LEXEME) { lexer.getNextLexeme(); }
  }
  
  while (true) {
    /* --- read referent --- */
    if (preference_type.isBinary()) {
      referent = parse_rhs_value();
      if (referent == null) {
        //deallocate_action_list (thisAgent, prev_a);
        return null;
      }
      if (lexer.getCurrentLexeme().type==COMMA_LEXEME) lexer.getNextLexeme();
    } else {
      referent = null; /* unnecessary, but gcc -Wall warns without it */
    }

    /* --- create the appropriate action --- */
    MakeAction a = new MakeAction();
    a.preference_type = preference_type;
    a.next = prev_a;
    prev_a = a;
    a.id = new RhsSymbolValue(id);
    //symbol_add_ref (id);
    a.attr = attr.copy();
    a.value = value.copy();
    if (preference_type.isBinary()) { a.referent = referent; }

    /* --- look for another preference type specifier --- */
    saw_plus_sign = (lexer.getCurrentLexeme().type==PLUS_LEXEME);
    preference_type = parse_preference_specifier_without_referent ();
    
    /* --- exit loop when done reading preferences --- */
    if ((preference_type==PreferenceType.ACCEPTABLE_PREFERENCE_TYPE) && (! saw_plus_sign))
      /* If the routine gave us a + pref without seeing a + sign, then it's
         just giving us the default acceptable preference, it didn't see any
         more preferences specified. */
      return prev_a;
  }
}

/* KJC begin:  10.09.98 */
/* modified 3.99 to take out parallels and only create acceptables */
/* -----------------------------------------------------------------
              Parse Preferences for Soar8 Non-Operators

   Given the id, attribute, and value already read, this routine
   parses zero or more <preference-specifier>'s.  If preferences
   other than reject and acceptable are specified, it prints
   a warning message that they are being ignored.  It builds an
   action list for creating an ACCEPTABLE preference.  If binary 
   preferences are encountered, a warning message is printed and 
   the production is ignored (returns null).  It returns null if any 
   other error occurred.  This works in conjunction with the code
   that supports attribute_preferences_mode == 2.  Anywhere that
   attribute_preferences_mode == 2 is tested, the code now tests
   for operand2_mode == true.


   <value_make> ::= <rhs_value> <preferences>
   <preferences> ::= [,] | <preference_specifier>+   
   <preference-specifier> ::= <naturally-unary-preference> [,]
                            | <forced-unary-preference>
                            | <binary-preference> <rhs_value> [,]
----------------------------------------------------------------- */

Action parse_preferences_soar8_non_operator (Symbol id, RhsValue attr, RhsValue value) throws IOException 
{
  /* --- Note: this routine is set up so if there's not preference type
     indicator at all, we return an acceptable preference make
     and a parallel preference make.  For non-operators, allow
     only REJECT_PREFERENCE_TYPE, (and UNARY_PARALLEL and ACCEPTABLE).
     If any other preference type indicator is found, a warning or
     error msg (error only on binary prefs) is printed. --- */
  
  boolean saw_plus_sign = (lexer.getCurrentLexeme().type==PLUS_LEXEME);
  PreferenceType preference_type = parse_preference_specifier_without_referent ();
  if ((preference_type==PreferenceType.ACCEPTABLE_PREFERENCE_TYPE) && (! saw_plus_sign)) {
    /* If the routine gave us a + pref without seeing a + sign, then it's
       just giving us the default acceptable preference.  Look for optional
       comma. */
    if (lexer.getCurrentLexeme().type==COMMA_LEXEME) { lexer.getNextLexeme(); }
  }
  
  Action prev_a = null;

  while (true) {
    /* step through the pref list, print warning messages when necessary. */

    /* --- read referent --- */
    if (preference_type.isBinary()) 
    {
        // TODO
        throw new IllegalStateException("in Soar8, binary preference illegal for non-operator.");
//      print (thisAgent, "\nERROR: in Soar8, binary preference illegal for non-operator.");
//      
//      /* JC BUG FIX: Have to check to make sure that the rhs_values are converted to strings
//               correctly before we print */
//      rhs_value_to_string(thisAgent, attr, szPrintAttr, 256);
//      rhs_value_to_string(thisAgent, value, szPrintValue, 256);
//      symbol_to_string(thisAgent, id, true, szPrintId, 256);
//      print(thisAgent, "id = %s\t attr = %s\t value = %s\n", szPrintId, szPrintAttr, szPrintValue);
//      
//      deallocate_action_list (thisAgent, prev_a);
//      return null;
    
    }

    if ( (preference_type != PreferenceType.ACCEPTABLE_PREFERENCE_TYPE) &&
			(preference_type != PreferenceType.REJECT_PREFERENCE_TYPE) ) {
        // TODO
//      print (thisAgent, "\nWARNING: in Soar8, the only allowable non-operator preference \nis REJECT - .\nIgnoring specified preferences.\n");
//	  xml_generate_warning(thisAgent, "WARNING: in Soar8, the only allowable non-operator preference \nis REJECT - .\nIgnoring specified preferences.");
//
//      /* JC BUG FIX: Have to check to make sure that the rhs_values are converted to strings
//               correctly before we print */
//      rhs_value_to_string(thisAgent, attr, szPrintAttr, 256);
//      rhs_value_to_string(thisAgent, value, szPrintValue, 256);
//      symbol_to_string(thisAgent, id, true, szPrintId, 256);
//      print(thisAgent, "id = %s\t attr = %s\t value = %s\n", szPrintId, szPrintAttr, szPrintValue);
//      
//      print_location_of_most_recent_lexeme(thisAgent);
    }

    if (preference_type == PreferenceType.REJECT_PREFERENCE_TYPE) {
      /* --- create the appropriate action --- */
      MakeAction a = new MakeAction();
      a.next = prev_a;
      prev_a = a;
      a.preference_type = preference_type;
      a.id = new RhsSymbolValue(id);
      //symbol_add_ref (id);
      a.attr = attr.copy();
      a.value = value.copy();
    }

    /* --- look for another preference type specifier --- */
    saw_plus_sign = (lexer.getCurrentLexeme().type==PLUS_LEXEME);
    preference_type = parse_preference_specifier_without_referent ();
    
    /* --- exit loop when done reading preferences --- */
    if ((preference_type==PreferenceType.ACCEPTABLE_PREFERENCE_TYPE) && (! saw_plus_sign)) {
      /* If the routine gave us a + pref without seeing a + sign, then it's
         just giving us the default acceptable preference, it didn't see any
         more preferences specified. */

      /* for soar8, if this wasn't a REJECT preference, then
			create acceptable preference makes.  */
      if (prev_a == null) {
	
		  MakeAction a = new MakeAction();
		  a.next = prev_a;
		  prev_a = a;
		  a.preference_type = PreferenceType.ACCEPTABLE_PREFERENCE_TYPE;
		  a.id = new RhsSymbolValue(id);
		  //symbol_add_ref (id);
		  a.attr = attr.copy(); 
		  a.value = value.copy();
      }
      return prev_a;
    }
  }
}
/* KJC end:  10.09.98 */

/* -----------------------------------------------------------------
                      Parse Attr Value Make

   Given the id already read, this routine parses an <attr_value_make>.
   It builds and returns an action list for these RHS make's.  It
   returns null if any error occurred.

   <attr_value_make> ::= ^ <rhs_value> <value_make>+
   <value_make> ::= <rhs_value> <preferences>
----------------------------------------------------------------- */

Action parse_attr_value_make (Symbol id) throws IOException 
{
  RhsValue value;
  Action new_actions, last;

  if (lexer.getCurrentLexeme().type!=UP_ARROW_LEXEME) {
      // TODO
      throw new IllegalStateException("Expected ^ in RHS make action");
//    print (thisAgent, "Expected ^ in RHS make action\n");
//    print_location_of_most_recent_lexeme(thisAgent);
//    return null;
  }

  lexer.getNextLexeme(); /* consume up-arrow, advance to attribute */
  RhsValue attr = parse_rhs_value();  
  if (attr == null) { 
     return null; 
  }
  
  /* JC Added, we will need the attribute as a string, so we get it here */
  String szAttribute = attr.toString(); // TODO (rhs_value_to_string)
  
  Action all_actions = null;
  
  /*  allow dot notation "." in RHS attribute path  10/15/98 KJC */
  while (lexer.getCurrentLexeme().type == LexemeTypes.PERIOD_LEXEME) 
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
    if(operand2_mode && !"operator".equals(szAttribute))
    {
      new_actions = parse_preferences_soar8_non_operator (id, attr, new RhsSymbolValue(new_var));
    } 
    else 
    {
      new_actions = parse_preferences (id, attr, new RhsSymbolValue(new_var));
    }
    
    for (last=new_actions; last.next!=null; last=last.next)
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

    /* JC Added. We need to get the new attribute's name */
    szAttribute = attr.toString();
  } 
  /* end of while (lexer.getCurrentLexeme().type == PERIOD_LEXEME */
  /* end KJC 10/15/98 */

  do {
    value = parse_rhs_value();
    if (value == null) {
//      deallocate_rhs_value (thisAgent, attr);
//      deallocate_action_list (thisAgent, all_actions);
      return null;
    }
    if(operand2_mode && !"operator".equals(szAttribute))
	 {
      new_actions = parse_preferences_soar8_non_operator (id, attr, value);
    } 
    else 
    {
      new_actions = parse_preferences (id, attr, value);
    }
    //deallocate_rhs_value (thisAgent, value);
    if (new_actions == null) {
      //deallocate_rhs_value (thisAgent, attr);
      return null;
    }
    for (last=new_actions; last.next!=null; last=last.next)
    { 
        /* nothing */
    }
    last.next = all_actions;
    all_actions = new_actions;
  } while ((lexer.getCurrentLexeme().type!=R_PAREN_LEXEME) &&
           (lexer.getCurrentLexeme().type!=UP_ARROW_LEXEME));

  //deallocate_rhs_value (thisAgent, attr);
  return all_actions;
}

/* -----------------------------------------------------------------
                        Parse RHS Action

   Parses an <rhs_action> and returns an action list.  If any error
   occurrs, null is returned.

   <rhs_action> ::= ( variable <attr_value_make>+ ) | <function_call>
----------------------------------------------------------------- */

Action parse_rhs_action () throws IOException {
  
  if (lexer.getCurrentLexeme().type!=L_PAREN_LEXEME) {
      // TODO
      throw new IllegalStateException("Expected ( to begin RHS action\n");
//    print (thisAgent, "Expected ( to begin RHS action\n");
//    print_location_of_most_recent_lexeme(thisAgent);
//    return null;
  }
  lexer.getNextLexeme();
  if (lexer.getCurrentLexeme().type!=VARIABLE_LEXEME) {
    /* --- the action is a function call --- */
    RhsFunctionCall funcall_value = parse_function_call_after_lparen (true);
    if (funcall_value == null ) { return null; }
    return new FunctionAction(funcall_value);
  }
  /* --- the action is a regular make action --- */
  Variable var = syms.make_variable(lexer.getCurrentLexeme().string);
  lexer.getNextLexeme();
  Action all_actions = null;
  Action last = null;
  while (lexer.getCurrentLexeme().type!=R_PAREN_LEXEME) {
    Action new_actions = parse_attr_value_make (var);
    if (new_actions != null) {
      for (last=new_actions; last.next!=null; last=last.next)
      {
          // nothing
      }
      last.next = all_actions;
      all_actions = new_actions;
    } else {
      //symbol_remove_ref (thisAgent, var);
      //deallocate_action_list (thisAgent, all_actions);
      return null;
    }
  }
  lexer.getNextLexeme();  /* consume the right parenthesis */
  //symbol_remove_ref (thisAgent, var);
  return all_actions;
}

/* -----------------------------------------------------------------
                            Parse RHS

   Parses the <rhs> and sets *dest_rhs to the resulting action list.
   Returns true if successful, false if any error occurred.

   <rhs> ::= <rhs_action>*
----------------------------------------------------------------- */

Action parse_rhs () throws IOException {

  Action all_actions = null;
  Action last = null;
  while (!lexer.isEof() && lexer.getCurrentLexeme().type!=R_PAREN_LEXEME) {
    Action new_actions = parse_rhs_action ();
    if (new_actions != null) {
      for (last=new_actions; last.next!=null; last=last.next)
      {
          // nothing
      }
      last.next = all_actions;
      all_actions = new_actions;
    } else {
      //deallocate_action_list (thisAgent, all_actions);
      return null;
    }
  }
  return all_actions;
}


/* =================================================================
                        Parse Production

   This routine reads a production (everything inside the body of the
   "sp" command), builds a production, and adds it to the rete.

   If successful, it returns a pointer to the new production struct.
   If any error occurred, it returns null (and may or may not read
   the rest of the body of the sp).
================================================================= */

public Production parse_production ( /*unsigned char* rete_addition_result */ ) throws IOException {

  // voigtjr: added to parameter list so that CLI can ignore the error 
  // of a duplicate production with a different name
  //byte rete_addition_result;

  reset_placeholder_variable_generator ();

  /* --- read production name --- */
  if (lexer.getCurrentLexeme().type!=SYM_CONSTANT_LEXEME) {
      // TODO
      throw new IllegalStateException("Expected symbol for production name\n");
//    print (thisAgent, "Expected symbol for production name\n");
//    print_location_of_most_recent_lexeme(thisAgent);
//    return null;
  }
  SymConstant name = syms.make_sym_constant(lexer.getCurrentLexeme().string);
  lexer.getNextLexeme();

  /* --- if there's already a prod with this name, excise it --- */
  if (name.production != null) {
    // TODO
    // excise_production (thisAgent, name->sc.production, (true && thisAgent->sysparams[TRACE_LOADING_SYSPARAM]));
  }

  String documentation = null;
  /* --- read optional documentation string --- */
  if (lexer.getCurrentLexeme().type==QUOTED_STRING_LEXEME) {
    documentation = lexer.getCurrentLexeme().string;
    lexer.getNextLexeme();
  }

  /* --- read optional flags --- */
  ProductionSupport declared_support = ProductionSupport.UNDECLARED_SUPPORT;
  ProductionType prod_type = ProductionType.USER_PRODUCTION_TYPE;
  boolean interrupt_on_match = false;
  while (true) {
    if (lexer.getCurrentLexeme().type!=SYM_CONSTANT_LEXEME) { break; }
    if (":o-support".equals(lexer.getCurrentLexeme().string)) {
      declared_support = ProductionSupport.DECLARED_O_SUPPORT;
      lexer.getNextLexeme();
      continue;
    }
    if (":i-support".equals(lexer.getCurrentLexeme().string)) {
      declared_support = ProductionSupport.DECLARED_I_SUPPORT;
      lexer.getNextLexeme();
      continue;
    }
    if (":chunk".equals(lexer.getCurrentLexeme().string)) {
      prod_type = ProductionType.CHUNK_PRODUCTION_TYPE;
      lexer.getNextLexeme();
      continue;
    }
    if (":default".equals(lexer.getCurrentLexeme().string)) {
      prod_type = ProductionType.DEFAULT_PRODUCTION_TYPE;
      lexer.getNextLexeme();
      continue;
    }
    if (":template".equals(lexer.getCurrentLexeme().string)) {
      prod_type = ProductionType.TEMPLATE_PRODUCTION_TYPE;
      lexer.getNextLexeme();
      continue;
    }
	if (":interrupt".equals(lexer.getCurrentLexeme().string)) {
	    // TODO
//	  print(thisAgent, "WARNING :interrupt is not supported with the current build options...");
//	  xml_generate_warning(thisAgent, "WARNING :interrupt is not supported with the current build options...");
	  lexer.getNextLexeme();
	  continue;
	}
    break;
  } /* end of while (true) */

  /* --- read the LHS --- */
  Condition lhs = parse_lhs();
  if (lhs == null) {
      // TODO
    //print_with_symbols (thisAgent, "(Ignoring production %y)\n\n", name);
    //symbol_remove_ref (thisAgent, name);
    return null;
  }

  /* --- read the "-->" --- */
  if (lexer.getCurrentLexeme().type!=RIGHT_ARROW_LEXEME) {
      // TODO
      throw new IllegalStateException("Expected --> in production");
//    print (thisAgent, "Expected --> in production\n");
//    print_location_of_most_recent_lexeme(thisAgent);
//    print_with_symbols (thisAgent, "(Ignoring production %y)\n\n", name);
//    symbol_remove_ref (thisAgent, name);
//    deallocate_condition_list (thisAgent, lhs);
//    return null;
  }
  lexer.getNextLexeme();

  /* --- read the RHS --- */
  Action rhs = parse_rhs ();
  if (rhs == null) {
      // TODO
//    print_with_symbols (thisAgent, "(Ignoring production %y)\n\n", name);
//    symbol_remove_ref (thisAgent, name);
//    deallocate_condition_list (thisAgent, lhs);
    return null;
  }
  rhs = destructively_reverse_action_list (rhs);

  // TODO: DR: This makes no sense to me
//  /* --- finally, make sure there's a closing right parenthesis (but
//     don't consume it) --- */
//  if (lexer.getCurrentLexeme().type!=R_PAREN_LEXEME) {
//      // TODO
//      throw new IllegalStateException("Expected ) to end production");
//    print (thisAgent, "Expected ) to end production\n");
//    print_location_of_most_recent_lexeme(thisAgent);
//    print_with_symbols (thisAgent, "(Ignoring production %y)\n\n", name);
//    symbol_remove_ref (thisAgent, name);
//    deallocate_condition_list (thisAgent, lhs);
//    deallocate_action_list (thisAgent, rhs);
//    return null;
//  }

  /* --- replace placeholder variables with real variables --- */
  varGen.reset (lhs, rhs);
  substitute_for_placeholders_in_condition_list (lhs);
  substitute_for_placeholders_in_action_list (rhs);

  /* --- everything parsed okay, so make the production structure --- */
  Condition lhs_top = lhs;
  Condition lhs_bottom = lhs;
  for (; lhs_bottom.next!=null; lhs_bottom=lhs_bottom.next)
  {
      // Nothing
  }
  Production p = new Production(varGen, prod_type, name, lhs_top, lhs_bottom, rhs, true);
  //p = make_production (thisAgent, prod_type, name, &lhs_top, &lhs_bottom, &rhs, true);

  // TODO
//  if (!p) {
//    print_with_symbols (thisAgent, "(Ignoring production %y)\n\n", name);
//    symbol_remove_ref (thisAgent, name);
//    deallocate_condition_list (thisAgent, lhs_top);
//    deallocate_action_list (thisAgent, rhs);
//    return null;
//  }

  p.documentation = documentation;
  p.declared_support = declared_support;
  p.interrupt = interrupt_on_match;
  
  // TODO
//  *rete_addition_result = add_production_to_rete (thisAgent, p, lhs_top, null, true);
//  deallocate_condition_list (thisAgent, lhs_top);
//
//  if (*rete_addition_result==DUPLICATE_PRODUCTION) {
//    excise_production (thisAgent, p, false);
//    p = null;
//  }

  return p;
}
/* =================================================================
Destructively Reverse Action List

As the parser builds the action list for the RHS, it adds each new
action onto the front of the list.  This results in the order of
the actions getting reversed.  This has certain problems--for example,
if there are several (write) actions on the RHS, reversing their order
means the output lines get printed in the wrong order.  To avoid this
problem, we reverse the list after building it.

This routine destructively reverses an action list.
================================================================= */

private Action destructively_reverse_action_list (Action a) {

Action prev = null;
Action current = a;
while (current != null) {
Action next = current.next;
current.next = prev;
prev = current;
current = next;
}
return prev;
}

}
