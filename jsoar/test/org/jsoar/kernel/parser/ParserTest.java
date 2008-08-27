/*
 * (c) 2008  Dave Ray
 *
 * Created on Aug 17, 2008
 */
package org.jsoar.kernel.parser;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertSame;

import java.io.IOException;
import java.io.StringReader;
import java.util.List;

import org.jsoar.JSoarTest;
import org.jsoar.kernel.PreferenceType;
import org.jsoar.kernel.Production;
import org.jsoar.kernel.VariableGenerator;
import org.jsoar.kernel.lhs.Condition;
import org.jsoar.kernel.lhs.ConjunctiveTest;
import org.jsoar.kernel.lhs.DisjunctionTest;
import org.jsoar.kernel.lhs.EqualityTest;
import org.jsoar.kernel.lhs.PositiveCondition;
import org.jsoar.kernel.lhs.RelationalTest;
import org.jsoar.kernel.rhs.Action;
import org.jsoar.kernel.rhs.MakeAction;
import org.jsoar.kernel.rhs.RhsFunctionCall;
import org.jsoar.kernel.rhs.RhsValue;
import org.jsoar.kernel.symbols.FloatConstant;
import org.jsoar.kernel.symbols.Identifier;
import org.jsoar.kernel.symbols.IntConstant;
import org.jsoar.kernel.symbols.SymConstant;
import org.jsoar.kernel.symbols.Symbol;
import org.jsoar.kernel.symbols.SymbolFactory;
import org.jsoar.kernel.symbols.Variable;
import org.junit.Before;
import org.junit.Test;

/**
 * @author ray
 */
public class ParserTest extends JSoarTest
{
    private SymbolFactory syms;
    
    @Before
    public void setUp()
    {
        this.syms = new SymbolFactory();
    }

    private Parser createParser(String input) throws IOException
    {
        Lexer lexer = new Lexer(new StringReader(input));
        
        Parser parser = new Parser(new VariableGenerator(syms), lexer);
        lexer.getNextLexeme();
        return parser;
    }

    private void verifyVariableSymbol(Symbol sym, String name)
    {
        Variable sc = sym.asVariable();
        assertNotNull(sc);
        assertEquals(name, sc.name);
    }
    private void verifyStringSymbol(Symbol sym, String name)
    {
        SymConstant sc = sym.asSymConstant();
        assertNotNull(sc);
        assertEquals(name, sc.name);
    }
    
    private void verifyIntSymbol(Symbol sym, int value)
    {
        IntConstant ic = sym.asIntConstant();
        assertNotNull(ic);
        assertEquals(value, ic.value);
    }
    
    private void verifyFloatSymbol(Symbol sym, double value)
    {
        FloatConstant fc = sym.asFloatConstant();
        assertNotNull(fc);
        assertEquals(value, fc.value, 0.001);
    }
    
    @Test
    public void testParseDisjunctionTest() throws Exception
    {
        // Trailing paren is because lexer doesn't like EOF
        Parser parser = createParser("   << a b 1 2 3 4.5 >> ");
        
        DisjunctionTest dt = parser.parse_disjunction_test();
        assertNotNull(dt);
        assertEquals(6, dt.disjunction_list.size());
        verifyStringSymbol(dt.disjunction_list.get(0), "a");
        verifyStringSymbol(dt.disjunction_list.get(1), "b");
        verifyIntSymbol(dt.disjunction_list.get(2), 1);
        verifyIntSymbol(dt.disjunction_list.get(3), 2);
        verifyIntSymbol(dt.disjunction_list.get(4), 3);
        verifyFloatSymbol(dt.disjunction_list.get(5), 4.5);
    }
    
    @Test
    public void testParseEqualityTest() throws Exception
    {
        Parser parser = createParser("  = |hello there| ");
        
        EqualityTest rt = parser.parse_relational_test().asEqualityTest();
        assertNotNull(rt);
        verifyStringSymbol(rt.sym, "hello there");
    }

    @Test
    public void testParseLessTest() throws Exception
    {
        Parser parser = createParser("  < 99 ");
        
        RelationalTest rt = parser.parse_relational_test().asRelationalTest();
        assertNotNull(rt);
        assertEquals(RelationalTest.LESS_TEST, rt.type);
        verifyIntSymbol(rt.referent, 99);
    }
    
    @Test
    public void testParseLessOrEqualTest() throws Exception
    {
        Parser parser = createParser("  <= 123.4 ");
        
        RelationalTest rt = parser.parse_relational_test().asRelationalTest();
        assertNotNull(rt);
        assertEquals(RelationalTest.LESS_OR_EQUAL_TEST, rt.type);
        verifyFloatSymbol(rt.referent, 123.4);
    }
    
    @Test
    public void testParseGreaterTest() throws Exception
    {
        Parser parser = createParser("  > <x> ");
        
        RelationalTest rt = parser.parse_relational_test().asRelationalTest();
        assertNotNull(rt);
        assertEquals(RelationalTest.GREATER_TEST, rt.type);
        verifyVariableSymbol(rt.referent, "<x>");
    }
    
    @Test
    public void testParseGreaterOrEqualTest() throws Exception
    {
        Parser parser = createParser("  >= <y> ");
        
        RelationalTest rt = parser.parse_relational_test().asRelationalTest();
        assertNotNull(rt);
        assertEquals(RelationalTest.GREATER_OR_EQUAL_TEST, rt.type);
        verifyVariableSymbol(rt.referent, "<y>");
    }
    
    @Test
    public void testNotEqualTest() throws Exception
    {
        Parser parser = createParser("  <> <z> ");
        
        RelationalTest rt = parser.parse_relational_test().asRelationalTest();
        assertNotNull(rt);
        assertEquals(RelationalTest.NOT_EQUAL_TEST, rt.type);
        verifyVariableSymbol(rt.referent, "<z>");
    }
    
    @Test
    public void testSameTypeTest() throws Exception
    {
        Parser parser = createParser("  <=> <another> ");
        
        RelationalTest rt = parser.parse_relational_test().asRelationalTest();
        assertNotNull(rt);
        assertEquals(RelationalTest.SAME_TYPE_TEST, rt.type);
        verifyVariableSymbol(rt.referent, "<another>");
    }
    
    @Test
    public void testParseConjunctiveTest() throws Exception
    {
        Parser parser = createParser(" { < 99 <> <x> << a b >> } ");
        
        ConjunctiveTest ct = parser.parse_test().asConjunctiveTest();
        assertNotNull(ct);
        assertEquals(3, ct.conjunct_list.size());
        
        RelationalTest t0 = ct.conjunct_list.get(0).asRelationalTest();
        assertNotNull(t0);
        assertEquals(RelationalTest.LESS_TEST, t0.type);
        verifyIntSymbol(t0.referent, 99);

        RelationalTest t1 = ct.conjunct_list.get(1).asRelationalTest();
        assertNotNull(t1);
        assertEquals(RelationalTest.NOT_EQUAL_TEST, t1.type);
        verifyVariableSymbol(t1.referent, "<x>");

        DisjunctionTest t2 = ct.conjunct_list.get(2).asDisjunctionTest();
        assertNotNull(t2);
        assertEquals(2, t2.disjunction_list.size());
        verifyStringSymbol(t2.disjunction_list.get(0), "a");
        verifyStringSymbol(t2.disjunction_list.get(1), "b");
    }
    
    @Test
    public void testParseCondsForOneId() throws Exception
    {
        Parser parser = createParser("(state <s> ^superstate nil) ");
        
        Condition c = parser.parse_conds_for_one_id('s', null);
        assertNotNull(c);
        assertNull(c.prev);
        assertNull(c.next);
        PositiveCondition pc = c.asPositiveCondition();
        assertNotNull(pc);
        assertNotNull(pc.id_test);
        assertNotNull(pc.attr_test);
        assertNotNull(pc.value_test);
        
        // Testing actual values of the three tests is tricky :(
    }
    
    @Test
    public void testParseCondsPlus() throws Exception
    {
        Parser parser = createParser("(state <s> ^superstate nil)\n" +
        		"(<s> ^io <io> ^superstate <ss>)\n" +
        		" -(<s> ^value 3)");
        
        Condition c0 = parser.parse_cond_plus(); // ^superstate nil
        assertNotNull(c0);
        assertNull(c0.prev);
        assertNotNull(c0.asPositiveCondition());
        
        Condition c1 = c0.next; // ^io <io>
        assertNotNull(c1);
        assertSame(c0, c1.prev);
        assertNotNull(c1.asPositiveCondition());
        
        Condition c2 = c1.next; // ^superstate <ss>
        assertNotNull(c2);
        assertSame(c1, c2.prev);
        assertNotNull(c2.next);
        assertNotNull(c2.asPositiveCondition());
        
        Condition c3 = c2.next; // ^value 3
        assertNull(c3.next);
        assertSame(c2, c3.prev);
        assertNotNull(c3.asNegativeCondition());
    }
    
    @Test
    public void testFunctionCallAfterLParen() throws Exception
    {
        Parser parser = createParser("   + 1 2 (- 3 5) <x> <y>)");
        
        RhsFunctionCall rfc = parser.parse_function_call_after_lparen(false);
        assertNotNull(rfc);
        verifyStringSymbol(rfc.getName(), "+");
        List<RhsValue> args = rfc.getArguments();
        assertEquals(5, args.size());
        verifyIntSymbol(args.get(0).asSymbolValue().getSym(), 1);
        verifyIntSymbol(args.get(1).asSymbolValue().getSym(), 2);
        assertNotNull(args.get(2).asFunctionCall());
        verifyVariableSymbol(args.get(3).asSymbolValue().getSym(), "<x>");
        verifyVariableSymbol(args.get(4).asSymbolValue().getSym(), "<y>");
    }
    
    @Test
    public void testParseRhsValue() throws Exception
    {
        Parser parser = createParser(" |rhs value|");
        RhsValue v = parser.parse_rhs_value();
        assertNotNull(v);
        verifyStringSymbol(v.asSymbolValue().getSym(), "rhs value");
        
        parser = createParser(" (write |hello|) }");
        v = parser.parse_rhs_value();
        assertNotNull(v);
        assertNotNull(v.asFunctionCall());
    }
    
    private void verifyParsePreference(String input, PreferenceType expected) throws Exception
    {
        Parser parser = createParser(input);
        assertEquals(expected, parser.parse_preference_specifier_without_referent());
    }
    
    @Test
    public void testParsePreferenceSpecifierWithoutReferent() throws Exception
    {
        verifyParsePreference(" + " , PreferenceType.ACCEPTABLE_PREFERENCE_TYPE);
        verifyParsePreference(" - " , PreferenceType.REJECT_PREFERENCE_TYPE);
        verifyParsePreference(" ! " , PreferenceType.REQUIRE_PREFERENCE_TYPE);
        verifyParsePreference(" ~ " , PreferenceType.PROHIBIT_PREFERENCE_TYPE);
        verifyParsePreference(" @ " , PreferenceType.RECONSIDER_PREFERENCE_TYPE);
        verifyParsePreference(" > 5 " , PreferenceType.BETTER_PREFERENCE_TYPE);
        verifyParsePreference(" > " , PreferenceType.BEST_PREFERENCE_TYPE);
        verifyParsePreference(" = 5 " , PreferenceType.NUMERIC_INDIFFERENT_PREFERENCE_TYPE);
        verifyParsePreference(" = <x> " , PreferenceType.BINARY_INDIFFERENT_PREFERENCE_TYPE);
        verifyParsePreference(" =  " , PreferenceType.UNARY_INDIFFERENT_PREFERENCE_TYPE);
        verifyParsePreference(" < 5 " , PreferenceType.WORSE_PREFERENCE_TYPE);
        verifyParsePreference(" < " , PreferenceType.WORST_PREFERENCE_TYPE);
        verifyParsePreference(" & 5 " , PreferenceType.BINARY_PARALLEL_PREFERENCE_TYPE);
        verifyParsePreference(" & " , PreferenceType.UNARY_PARALLEL_PREFERENCE_TYPE);
    }
    
    @Test
    public void testParseAttributeValueMake() throws Exception
    {
        Parser parser = createParser(" ^test 99 - )");
        Identifier id = syms.make_new_identifier('s', (short) 0);
        Action a = parser.parse_attr_value_make(id);
        assertNotNull(a);
        MakeAction ma = a.asMakeAction();
        assertNotNull(ma);
        assertNull(ma.next);
        assertEquals(PreferenceType.REJECT_PREFERENCE_TYPE, ma.preference_type);
        assertSame(id, ma.id.asSymbolValue().getSym());
        assertEquals("test", ma.attr.asSymbolValue().getSym().asSymConstant().name);
        assertEquals(99, ma.value.asSymbolValue().getSym().asIntConstant().value);
    }
    
    @Test
    public void testParseProduction() throws Exception
    {
        Parser parser = createParser("testParseProduction (state <s> ^superstate nil) --> (<s> ^value 99) ");
        Production p = parser.parse_production();
        assertNotNull(p);
    }
    
    @Test
    public void testParseProduction2() throws Exception
    {
        // Just testing a larger production to exercise the parser more
        // blocks-opsub.soar:58
        Parser parser = createParser(
        "    top-ps*propose*operator*build-tower\n" +
        "    (state <s> ^name blocks-world\n" +
        "               ^object <blockA> <blockB> <blockC> <table>)\n" +
        "    (<blockA> ^name A ^type block)\n" +
        "    (<blockB> ^name B ^type block)\n" +
        "    (<blockC> ^name C ^type block)\n" +
        "    (<table> ^name table ^type table)\n" +
        "  -{(<s> ^ontop <ontopa1> <ontopa2> <ontopa3>)\n" +
        "    (<ontopa1> ^top-block <blockA> \n" +
        "               ^bottom-block <blockB>)\n" +
        "    (<ontopa2> ^top-block <blockB> \n" +
        "               ^bottom-block <blockC>)\n" +
        "    (<ontopa3> ^top-block <blockC> \n" +
        "               ^bottom-block <table>)}\n" +
        "    -->\n" +
        "    (<s> ^operator <o>)\n" +
        "    (<o> ^name build-tower \n" +
        "         ^desired <ds>)\n" +
        "    (<ds> ^ontop <ontop1> <ontop2> <ontop3>)\n" +
        "    (<ontop1> ^top-block <blockA> \n" +
        "              ^bottom-block <blockB>)\n" +
        "    (<ontop2> ^top-block <blockB> \n" +
        "              ^bottom-block <blockC>)\n" +
        "    (<ontop3> ^top-block <blockC> \n" +
        "              ^bottom-block <table>)\n" +
        "    (write (crlf) |The goal is to get A on B on C on the table.|)\n");
        Production p = parser.parse_production();
        assertNotNull(p);
    }
}
