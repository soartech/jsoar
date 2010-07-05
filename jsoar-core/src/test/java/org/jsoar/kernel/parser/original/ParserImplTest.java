/*
 * (c) 2008  Dave Ray
 *
 * Created on Aug 17, 2008
 */
package org.jsoar.kernel.parser.original;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertSame;

import java.io.IOException;
import java.io.StringReader;
import java.util.List;

import org.jsoar.JSoarTest;
import org.jsoar.kernel.Production;
import org.jsoar.kernel.SoarException;
import org.jsoar.kernel.lhs.Condition;
import org.jsoar.kernel.lhs.ConjunctiveTest;
import org.jsoar.kernel.lhs.DisjunctionTest;
import org.jsoar.kernel.lhs.EqualityTest;
import org.jsoar.kernel.lhs.PositiveCondition;
import org.jsoar.kernel.lhs.RelationalTest;
import org.jsoar.kernel.memory.PreferenceType;
import org.jsoar.kernel.parser.original.OriginalParserImpl;
import org.jsoar.kernel.rhs.Action;
import org.jsoar.kernel.rhs.MakeAction;
import org.jsoar.kernel.rhs.RhsFunctionCall;
import org.jsoar.kernel.rhs.RhsValue;
import org.jsoar.kernel.symbols.DoubleSymbolImpl;
import org.jsoar.kernel.symbols.IdentifierImpl;
import org.jsoar.kernel.symbols.IntegerSymbolImpl;
import org.jsoar.kernel.symbols.LongTermIdentifierSource;
import org.jsoar.kernel.symbols.StringSymbolImpl;
import org.jsoar.kernel.symbols.SymbolImpl;
import org.jsoar.kernel.symbols.Variable;
import org.jsoar.kernel.tracing.Printer;
import org.junit.Test;

/**
 * @author ray
 */
public class ParserImplTest extends JSoarTest
{
    private void verifyVariableSymbol(SymbolImpl sym, String name)
    {
        Variable sc = sym.asVariable();
        assertNotNull(sc);
        assertEquals(name, sc.name);
    }
    private void verifyStringSymbol(SymbolImpl sym, String name)
    {
        StringSymbolImpl sc = sym.asString();
        assertNotNull(sc);
        assertEquals(name, sc.getValue());
    }
    
    private void verifyIntSymbol(SymbolImpl sym, int value)
    {
        IntegerSymbolImpl ic = sym.asInteger();
        assertNotNull(ic);
        assertEquals(value, ic.getValue());
    }
    
    private void verifyFloatSymbol(SymbolImpl sym, double value)
    {
        DoubleSymbolImpl fc = sym.asDouble();
        assertNotNull(fc);
        assertEquals(value, fc.getValue(), 0.001);
    }
    
    private OriginalParserImpl createParser(String input) throws IOException
    {
        Lexer lexer = new Lexer(Printer.createStdOutPrinter(), new StringReader(input));
        
        OriginalParserImpl parser = new OriginalParserImpl(syms.getVariableGenerator(), lexer);
        lexer.getNextLexeme();
        return parser;
    }

    
    @Test
    public void testParseDisjunctionTest() throws Exception
    {
        // Trailing paren is because lexer doesn't like EOF
        OriginalParserImpl parser = createParser("   << a b 1 2 3 4.5 >> ");
        
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
        OriginalParserImpl parser = createParser("  = |hello there| ");
        
        EqualityTest rt = parser.parse_relational_test().asEqualityTest();
        assertNotNull(rt);
        verifyStringSymbol(rt.getReferent(), "hello there");
    }

    @Test
    public void testParseLessTest() throws Exception
    {
        OriginalParserImpl parser = createParser("  < 99 ");
        
        RelationalTest rt = parser.parse_relational_test().asRelationalTest();
        assertNotNull(rt);
        assertEquals(RelationalTest.LESS_TEST, rt.type);
        verifyIntSymbol(rt.referent, 99);
    }
    
    @Test
    public void testParseLessOrEqualTest() throws Exception
    {
        OriginalParserImpl parser = createParser("  <= 123.4 ");
        
        RelationalTest rt = parser.parse_relational_test().asRelationalTest();
        assertNotNull(rt);
        assertEquals(RelationalTest.LESS_OR_EQUAL_TEST, rt.type);
        verifyFloatSymbol(rt.referent, 123.4);
    }
    
    @Test
    public void testParseGreaterTest() throws Exception
    {
        OriginalParserImpl parser = createParser("  > <x> ");
        
        RelationalTest rt = parser.parse_relational_test().asRelationalTest();
        assertNotNull(rt);
        assertEquals(RelationalTest.GREATER_TEST, rt.type);
        verifyVariableSymbol(rt.referent, "<x>");
    }
    
    @Test
    public void testParseGreaterOrEqualTest() throws Exception
    {
        OriginalParserImpl parser = createParser("  >= <y> ");
        
        RelationalTest rt = parser.parse_relational_test().asRelationalTest();
        assertNotNull(rt);
        assertEquals(RelationalTest.GREATER_OR_EQUAL_TEST, rt.type);
        verifyVariableSymbol(rt.referent, "<y>");
    }
    
    @Test
    public void testNotEqualTest() throws Exception
    {
        OriginalParserImpl parser = createParser("  <> <z> ");
        
        RelationalTest rt = parser.parse_relational_test().asRelationalTest();
        assertNotNull(rt);
        assertEquals(RelationalTest.NOT_EQUAL_TEST, rt.type);
        verifyVariableSymbol(rt.referent, "<z>");
    }
    
    @Test
    public void testSameTypeTest() throws Exception
    {
        OriginalParserImpl parser = createParser("  <=> <another> ");
        
        RelationalTest rt = parser.parse_relational_test().asRelationalTest();
        assertNotNull(rt);
        assertEquals(RelationalTest.SAME_TYPE_TEST, rt.type);
        verifyVariableSymbol(rt.referent, "<another>");
    }
    
    @Test
    public void testParseConjunctiveTest() throws Exception
    {
        OriginalParserImpl parser = createParser(" { < 99 <> <x> << a b >> } ");
        
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
        OriginalParserImpl parser = createParser("(state <s> ^superstate nil) ");
        
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
        OriginalParserImpl parser = createParser("(state <s> ^superstate nil)\n" +
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
        OriginalParserImpl parser = createParser("   + 1 2 (- 3 5) <x> <y>)");
        
        RhsFunctionCall rfc = parser.parse_function_call_after_lparen(false);
        assertNotNull(rfc);
        verifyStringSymbol((SymbolImpl) rfc.getName(), "+");
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
        OriginalParserImpl parser = createParser(" |rhs value|");
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
        OriginalParserImpl parser = createParser(input);
        assertEquals(expected, parser.parse_preference_specifier_without_referent());
    }
    
    @Test
    public void testParsePreferenceSpecifierWithoutReferent() throws Exception
    {
        verifyParsePreference(" + " , PreferenceType.ACCEPTABLE);
        verifyParsePreference(" - " , PreferenceType.REJECT);
        verifyParsePreference(" ! " , PreferenceType.REQUIRE);
        verifyParsePreference(" ~ " , PreferenceType.PROHIBIT);
        verifyParsePreference(" @ " , PreferenceType.RECONSIDER);
        verifyParsePreference(" > 5 " , PreferenceType.BETTER);
        verifyParsePreference(" > " , PreferenceType.BEST);
        verifyParsePreference(" = 5 " , PreferenceType.NUMERIC_INDIFFERENT);
        verifyParsePreference(" = <x> " , PreferenceType.BINARY_INDIFFERENT);
        verifyParsePreference(" =  " , PreferenceType.UNARY_INDIFFERENT);
        verifyParsePreference(" < 5 " , PreferenceType.WORSE);
        verifyParsePreference(" < " , PreferenceType.WORST);
    }
    
    @Test
    public void testParseAttributeValueMake() throws Exception
    {
        OriginalParserImpl parser = createParser(" ^test 99 - )");
        IdentifierImpl id = syms.make_new_identifier('s', (short) 0);
        Action a = parser.parse_attr_value_make(id);
        assertNotNull(a);
        MakeAction ma = a.asMakeAction();
        assertNotNull(ma);
        assertNull(ma.next);
        assertEquals(PreferenceType.REJECT, ma.preference_type);
        assertSame(id, ma.id.asSymbolValue().getSym());
        assertEquals("test", ma.attr.asSymbolValue().getSym().asString().getValue());
        assertEquals(99, ma.value.asSymbolValue().getSym().asInteger().getValue());
    }
    
    @Test
    public void testParseProduction() throws Exception
    {
        OriginalParserImpl parser = createParser("testParseProduction (state <s> ^superstate nil) --> (<s> ^value 99) ");
        Production p = parser.parseProduction();
        assertNotNull(p);
    }
    
    @Test
    public void testParseProduction2() throws Exception
    {
        // Just testing a larger production to exercise the parser more
        // blocks-opsub.soar:58
        OriginalParserImpl parser = createParser(
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
        Production p = parser.parseProduction();
        assertNotNull(p);
    }
    
    @Test
    public void testParseProductionWithNoActions() throws Exception
    {
        OriginalParserImpl parser = createParser("test (state <s> ^superstate nil) -->");
        Production p = parser.parseProduction();
        assertNotNull(p);
    }
    
    @Test
    public void testParseProduction3() throws Exception
    {
        // Testing a problematic production from towers-of-hanoi
        OriginalParserImpl parser = createParser("towers-of-hanoi*propose*initialize\n" +
"   (state <s> ^superstate nil\n" +
"             -^name)\n" +
"-->\n" +
"   (<s> ^operator <o> +)\n" +
"   (<o> ^name initialize-toh)");
        Production p = parser.parseProduction();
        assertNotNull(p);
    }
    
    @Test(expected=IllegalStateException.class)
    public void testThrowsIllegalStateExceptionWithLtiAndNoLtiSource() throws Exception
    {
        OriginalParserImpl parser = createParser("testParseLongTermIdentifier\n" +
                "(state <s> ^value @L1)" +
                "-->" +
                "(write hello)");
        parser.parseProduction();
    }

    @Test
    public void testCanParseLongTermIdentifierInTest() throws Exception
    {
        OriginalParserImpl parser = createParser("testCanParseLongTermIdentifierInTest\n" +
                "(state <s> ^value @L1)" +
                "-->" +
                "(write hello)");
        
        final long expectedLti = 1000;
        final LongTermIdentifierSource ltis = new LongTermIdentifierSource()
        {
            
            @Override
            public IdentifierImpl smem_lti_soar_make(long lti, char nameLetter,
                    long nameNumber, int level)
            {
                assertEquals(expectedLti, lti);
                assertEquals('L', nameLetter);
                assertEquals(1, nameNumber);
                final IdentifierImpl result = syms.make_new_identifier(nameLetter, level);
                result.smem_lti = lti;
                return result;
            }
            
            @Override
            public long smem_lti_get_id(char nameLetter, long nameNumber)
                    throws SoarException
            {
                return expectedLti;
            }
        };
        parser.setLongTermIdSource(ltis);
        
        final Production p = parser.parseProduction();
        assertNotNull(p);
        final IdentifierImpl id = p.condition_list.asThreeFieldCondition().value_test.asEqualityTest().getReferent().asIdentifier();
        assertEquals('L', id.getNameLetter());
        assertEquals(1, id.getNameNumber());
        assertEquals(expectedLti, id.smem_lti);
    }
    
    @Test
    public void testCanParseLongTermIdentifierInRhsAction() throws Exception
    {
        OriginalParserImpl parser = createParser("testCanParseLongTermIdentifierInRhsAction\n" +
                "(state <s> ^value)" +
                "-->" +
                "(<s> ^foo @L2)");
        
        final long expectedLti = 1001;
        final LongTermIdentifierSource ltis = new LongTermIdentifierSource()
        {
            
            @Override
            public IdentifierImpl smem_lti_soar_make(long lti, char nameLetter,
                    long nameNumber, int level)
            {
                assertEquals(expectedLti, lti);
                assertEquals('L', nameLetter);
                assertEquals(2, nameNumber);
                final IdentifierImpl result = syms.make_new_identifier(nameLetter, level);
                result.smem_lti = lti;
                return result;
            }
            
            @Override
            public long smem_lti_get_id(char nameLetter, long nameNumber)
                    throws SoarException
            {
                return expectedLti;
            }
        };
        parser.setLongTermIdSource(ltis);
        
        final Production p = parser.parseProduction();
        assertNotNull(p);
        final IdentifierImpl id = p.action_list.asMakeAction().value.asSymbolValue().getSym().asIdentifier();
        assertEquals('L', id.getNameLetter());
        assertEquals(1, id.getNameNumber());
        assertEquals(expectedLti, id.smem_lti);
    }
}
