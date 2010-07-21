package org.jsoar.util.commands;

import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import java.util.ArrayList;
import java.util.List;

import org.jsoar.kernel.SoarException;
import org.jsoar.util.commands.OptionProcessor;
import org.jsoar.util.commands.OptionProcessor.OptionBuilder;
import org.junit.Before;
import org.junit.Test;

import com.google.common.collect.Lists;

/**
 * TODO: new chaining
 * 
 * @author voigtjr
 * 
 */
public class OptionProcessorTest
{
    private OptionProcessor<String> op;

    private final String alpha = "alpha";

    private final String bravo = "bravo";

    private final String charlie = "charlie";

    private final String delta = "delta";

    @Before
    public void setUp() throws Exception
    {
        op = OptionProcessor.create();
    }

    @Test(expected = NullPointerException.class)
    public void testRegisterNullLong()
    {
        op.newOption(null).done();
    }

    @Test(expected = IllegalArgumentException.class)
    public void testRegisterTwiceLong()
    {
        op.newOption(alpha).done();
        op.newOption(alpha).done();
    }

    @Test(expected = IllegalArgumentException.class)
    public void testRegisterTwiceLongDiffShort()
    {
        op.newOption(alpha).shortOption('a').done();
        op.newOption(alpha).shortOption('A').done();
    }

    @Test(expected = IllegalArgumentException.class)
    public void testRegisterTwiceDiffCaseLongDiffShort()
    {
        op.newOption(alpha).shortOption('a').done();
        op.newOption("Alpha").shortOption('A').done();
    }

    @Test(expected = IllegalArgumentException.class)
    public void testRegisterTwiceShort()
    {
        op.newOption(alpha).shortOption('a').done();
        op.newOption(bravo).shortOption('a').done();
    }

    @SuppressWarnings("unchecked")
    @Test(expected = IllegalStateException.class)
    public void testRegisterPostmod1()
    {
        OptionBuilder b = op.newOption(alpha).shortOption('a');
        b.done();
        b.shortOption('a');
    }

    @SuppressWarnings("unchecked")
    @Test(expected = IllegalStateException.class)
    public void testRegisterPostmod2()
    {
        OptionBuilder b = op.newOption(alpha).shortOption('a');
        b.done();
        b.noArg();
    }

    @SuppressWarnings("unchecked")
    @Test(expected = IllegalStateException.class)
    public void testRegisterPostmod3()
    {
        OptionBuilder b = op.newOption(alpha).shortOption('a');
        b.done();
        b.optionalArg();
    }

    @SuppressWarnings("unchecked")
    @Test(expected = IllegalStateException.class)
    public void testRegisterPostmod4()
    {
        OptionBuilder b = op.newOption(alpha).shortOption('a');
        b.done();
        b.requiredArg();
    }

    @SuppressWarnings("unchecked")
    @Test(expected = IllegalStateException.class)
    public void testRegisterTwice()
    {
        OptionBuilder b = op.newOption(alpha).shortOption('a');
        b.done();
        b.done();
    }

    @Test(expected = IllegalArgumentException.class)
    public void testRegisterTwiceLongDifferentType()
    {
        op.newOption(bravo).noArg().done();
        op.newOption(bravo).optionalArg().done();
    }

    @Test(expected = IllegalArgumentException.class)
    public void testRegisterDash()
    {
        op.newOption("-").done();
    }

    @Test(expected = IllegalArgumentException.class)
    public void testRegisterEmptyLong()
    {
        op.newOption("").done();
    }

    @Test
    public void testRegisterIllegalCharacters()
    {
        for (char i = Character.MIN_VALUE; i < Character.MAX_VALUE; ++i)
        {
            if (!Character.isLetter(i))
            {
                try
                {
                    op.newOption(String.valueOf(i)).done();
                    throw new AssertionError(
                            "Should have rejected long option " + i);
                }
                catch (IllegalArgumentException e)
                {
                }
                try
                {
                    op.newOption(alpha).shortOption(Character.valueOf(i))
                            .done();
                    throw new AssertionError(
                            "Should have rejected short option " + i);
                }
                catch (IllegalArgumentException e)
                {
                }
            }
        }
    }

    @Test(expected = SoarException.class)
    public void testUnknownLongOption() throws SoarException
    {
        op.process(Lists.newArrayList("command", "--alpha"));
    }

    @Test(expected = SoarException.class)
    public void testUnknownShortOption() throws SoarException
    {
        op.process(Lists.newArrayList("command", "-a"));
    }

    @Test(expected = SoarException.class)
    public void testBadCharacterLongOption() throws SoarException
    {
        op.process(Lists.newArrayList("command", "--_"));
    }

    @Test(expected = SoarException.class)
    public void testBadCharacterShortOption() throws SoarException
    {
        op.process(Lists.newArrayList("command", "-_"));
    }

    @Test
    public void testRegisterLegalOptions01() throws SoarException
    {
        verifySingleOption(alpha, null, alpha, 'a');
    }

    @Test
    public void testRegisterLegalOptions02() throws SoarException
    {
        verifySingleOption("Accent", null, "accent", 'A');
    }

    @Test
    public void testRegisterLegalOptions03() throws SoarException
    {
        verifySingleOption("beta", 'b', "bEtA", 'b');

    }

    @Test
    public void testRegisterLegalOptions04() throws SoarException
    {
        verifySingleOption(bravo, 'B', bravo, 'B');
    }

    @Test
    public void testRegisterLegalOptions05() throws SoarException
    {
        verifySingleOption("echo-", null, "echo-", 'e');
    }

    @Test
    public void testRegisterLegalOptions06() throws SoarException
    {
        verifySingleOption("fox-trot", null, "FOX-TROT", 'f');
    }

    @Test
    public void testRegisterLegalOptions07() throws SoarException
    {
        verifySingleOption("LONG", null, "long", 'L');
    }

    @Test
    public void testRegisterLegalOptions08() throws SoarException
    {
        verifySingleOption("L-ONG", null, "l-ong", 'L');
    }

    @Test
    public void testRegisterLegalOptions09() throws SoarException
    {
        verifySingleOption("L--ONG", 'l', "l--ong", 'l');
    }

    @Test
    public void testRegisterLegalOptions10() throws SoarException
    {
        verifySingleOption("L0NG", 'l', "l0ng", 'l');
    }

    @Test
    public void testRegisterLegalOptions11() throws SoarException
    {
        verifySingleOption("e1337", null, "e1337", 'e');
    }

    @Test
    public void testRegisterLegalOptions12() throws SoarException
    {
        verifySingleOption("l0", 'L', "l0", 'L');
    }

    public void verifySingleOption(String lReg, Character sReg, String lExpect,
            char sExpect) throws SoarException
    {
        if (sReg == null)
            op.newOption(lReg).done();
        else
            op.newOption(lReg).shortOption(sReg).done();

        op.process(Lists.newArrayList("command", "asdf"));
        assertFalse(op.has(lReg));

        op.process(Lists.newArrayList("command", "--" + lExpect));
        assertTrue(op.has(lReg));
        op.process(Lists.newArrayList("command", "-" + sExpect));
        assertTrue(op.has(lReg));
    }

    @Test(expected = IllegalArgumentException.class)
    public void testRegisterIllegalOptionNumber1() throws SoarException
    {
        op.newOption("640").done();
    }

    @Test(expected = IllegalArgumentException.class)
    public void testRegisterIllegalOptionNumber2() throws SoarException
    {
        op.newOption("1eet").done();
    }

    @Test
    public void testProcessNoOptions1() throws SoarException
    {
        List<String> nonOptions = op.process(new ArrayList<String>());
        assertTrue(nonOptions.isEmpty());
    }

    @Test
    public void testProcessNoOptions2() throws SoarException
    {
        List<String> nonOptions = op.process(Lists.newArrayList("command"));
        assertTrue(nonOptions.isEmpty());
    }

    @Test
    public void testProcessNoOptions3() throws SoarException
    {
        List<String> nonOptions = op.process(Lists.newArrayList("command",
                "arg"));
        assertEquals(nonOptions.size(), 1);
    }

    @Test
    public void testProcessNoOptions4() throws SoarException
    {
        List<String> nonOptions = op.process(Lists.newArrayList("command",
                "arg", "arg"));
        assertEquals(nonOptions.size(), 2);
    }

    @Test(expected = IllegalStateException.class)
    public void testCheckOutOfOrder()
    {
        op.newOption(alpha).done();
        // no process() call
        op.has(alpha);
    }

    @Test(expected = IllegalStateException.class)
    public void testCheckArgOutOfOrder()
    {
        op.newOption(alpha).done();
        // no process() call
        op.get(alpha);
    }

    @Test
    public void testMultipleOptions() throws SoarException
    {
        op.newOption(alpha).done();
        op.newOption(bravo).done();

        List<String> empty = new ArrayList<String>();
        List<String> arg = Lists.newArrayList("arg");

        verify(Lists.newArrayList("command", "--alpha", "--bravo"), empty,
                alpha, bravo);
        verify(Lists.newArrayList("command", "arg", "--alpha", "--bravo"), arg,
                alpha, bravo);
        verify(Lists.newArrayList("command", "--alpha", "arg", "--bravo"), arg,
                alpha, bravo);
        verify(Lists.newArrayList("command", "--alpha", "--bravo", "arg"), arg,
                alpha, bravo);

        verify(Lists.newArrayList("command", "-a", "--bravo"), empty, alpha,
                bravo);
        verify(Lists.newArrayList("command", "arg", "-a", "--bravo"), arg,
                alpha, bravo);
        verify(Lists.newArrayList("command", "-a", "arg", "--bravo"), arg,
                alpha, bravo);
        verify(Lists.newArrayList("command", "-a", "--bravo", "arg"), arg,
                alpha, bravo);

        verify(Lists.newArrayList("command", "--alpha", "-b"), empty, alpha,
                bravo);
        verify(Lists.newArrayList("command", "arg", "--alpha", "-b"), arg,
                alpha, bravo);
        verify(Lists.newArrayList("command", "--alpha", "arg", "-b"), arg,
                alpha, bravo);
        verify(Lists.newArrayList("command", "--alpha", "-b", "arg"), arg,
                alpha, bravo);

        verify(Lists.newArrayList("command", "-a", "-b"), empty, alpha, bravo);
        verify(Lists.newArrayList("command", "arg", "-a", "-b"), arg, alpha,
                bravo);
        verify(Lists.newArrayList("command", "-a", "arg", "-b"), arg, alpha,
                bravo);
        verify(Lists.newArrayList("command", "-a", "-b", "arg"), arg, alpha,
                bravo);

        verify(Lists.newArrayList("command", "-b", "-a"), empty, alpha, bravo);
        verify(Lists.newArrayList("command", "arg", "-b", "-a"), arg, alpha,
                bravo);
        verify(Lists.newArrayList("command", "-b", "arg", "-a"), arg, alpha,
                bravo);
        verify(Lists.newArrayList("command", "-b", "-a", "arg"), arg, alpha,
                bravo);

        verify(Lists.newArrayList("command", "--bravo", "-a"), empty, alpha,
                bravo);
        verify(Lists.newArrayList("command", "arg", "--bravo", "-a"), arg,
                alpha, bravo);
        verify(Lists.newArrayList("command", "--bravo", "arg", "-a"), arg,
                alpha, bravo);
        verify(Lists.newArrayList("command", "--bravo", "-a", "arg"), arg,
                alpha, bravo);

        verify(Lists.newArrayList("command", "-b", "--alpha"), empty, alpha,
                bravo);
        verify(Lists.newArrayList("command", "arg", "-b", "--alpha"), arg,
                alpha, bravo);
        verify(Lists.newArrayList("command", "-b", "arg", "--alpha"), arg,
                alpha, bravo);
        verify(Lists.newArrayList("command", "-b", "--alpha", "arg"), arg,
                alpha, bravo);

        verify(Lists.newArrayList("command", "--bravo", "--alpha"), empty,
                alpha, bravo);
        verify(Lists.newArrayList("command", "arg", "--bravo", "--alpha"), arg,
                alpha, bravo);
        verify(Lists.newArrayList("command", "--bravo", "arg", "--alpha"), arg,
                alpha, bravo);
        verify(Lists.newArrayList("command", "--bravo", "--alpha", "arg"), arg,
                alpha, bravo);

        verify(Lists.newArrayList("command", "-ab"), empty, alpha, bravo);
        verify(Lists.newArrayList("command", "arg", "-ab"), arg, alpha, bravo);
        verify(Lists.newArrayList("command", "-ab", "arg"), arg, alpha, bravo);

        verify(Lists.newArrayList("command", "-ba"), empty, alpha, bravo);
        verify(Lists.newArrayList("command", "arg", "-ba"), arg, alpha, bravo);
        verify(Lists.newArrayList("command", "-ba", "arg"), arg, alpha, bravo);
    }

    private void verify(List<String> line, List<String> nonOptExpected,
            String... options) throws SoarException
    {
        List<String> nonOpt = op.process(line);
        for (String o : options)
            assertTrue(op.has(o));
        assertArrayEquals(nonOptExpected.toArray(), nonOpt.toArray());
    }

    @Test
    public void testNonOptArgOrder() throws SoarException
    {
        op.newOption(alpha).newOption(bravo).newOption(charlie).done();

        verifyabc(Lists.newArrayList("command", "--alpha", "--bravo",
                "--charlie", "1", "2", "3"));
        verifyabc(Lists.newArrayList("command", "--alpha", "--bravo", "1",
                "--charlie", "2", "3"));
        verifyabc(Lists.newArrayList("command", "--alpha", "1", "--bravo",
                "--charlie", "2", "3"));
        verifyabc(Lists.newArrayList("command", "--alpha", "1", "--bravo", "2",
                "--charlie", "3"));
        verifyabc(Lists.newArrayList("command", "1", "--alpha", "--bravo", "2",
                "--charlie", "3"));
        verifyabc(Lists.newArrayList("command", "1", "--alpha", "2", "--bravo",
                "--charlie", "3"));
        verifyabc(Lists.newArrayList("command", "1", "--alpha", "2", "--bravo",
                "3", "--charlie"));
        verifyabc(Lists.newArrayList("command", "1", "2", "3", "--alpha",
                "--bravo", "--charlie"));
    }

    private void verifyabc(List<String> line) throws SoarException
    {
        String[] expected = new String[] { "1", "2", "3" };

        List<String> nonOpt = op.process(line);

        assertTrue(op.has(alpha));
        assertTrue(op.has(bravo));
        assertTrue(op.has(charlie));

        assertArrayEquals(expected, nonOpt.toArray());
    }

    @Test
    public void testOptionalArg() throws SoarException
    {
        op.newOption(alpha).optionalArg().done();

        op.process(Lists.newArrayList("command", "-a"));
        assertTrue(op.has(alpha));
        assertNull(op.get(alpha));

        op.process(Lists.newArrayList("command", "-a", "arg"));
        assertTrue(op.has(alpha));
        assertEquals("arg", op.get(alpha));
    }

    @Test
    public void testRequiredArg() throws SoarException
    {
        op.newOption(alpha).requiredArg().done();

        op.process(Lists.newArrayList("command", "-a", "arg"));
        assertTrue(op.has(alpha));
        assertEquals("arg", op.get(alpha));
    }

    @Test(expected = SoarException.class)
    public void testMissingRequired() throws SoarException
    {
        op.newOption(alpha).requiredArg().done();
        op.process(Lists.newArrayList("command", "-a"));
    }

    @Test(expected = SoarException.class)
    public void testMissingRequiredTwo() throws SoarException
    {
        op.newOption(alpha).done();
        op.newOption(bravo).requiredArg().done();
        op.process(Lists.newArrayList("command", "-ab"));
    }

    @Test
    public void testArgConsumesOption() throws SoarException
    {
        op.newOption(alpha).optionalArg().done();
        op.newOption(bravo).requiredArg().done();

        op.process(Lists.newArrayList("command", "-ab"));
        assertTrue(op.has(alpha));
        assertEquals("b", op.get(alpha));
        assertFalse(op.has(bravo));

        op.process(Lists.newArrayList("command", "-a", "-b"));
        assertTrue(op.has(alpha));
        assertEquals("-b", op.get(alpha));
        assertFalse(op.has(bravo));

        op.process(Lists.newArrayList("command", "-b", "-a"));
        assertTrue(op.has(bravo));
        assertEquals("-a", op.get(bravo));
        assertFalse(op.has(alpha));
    }

    @Test
    public void testMixed() throws SoarException
    {
        op.newOption(alpha).done();
        op.newOption(bravo).optionalArg().done();
        op.newOption(charlie).requiredArg().done();
        op.newOption(delta).requiredArg().done();

        List<String> nonOpt = op.process(Lists.newArrayList("command", "0",
                "-a", "1", "-b", "2", "-c", "3", "4", "-d", "5", "6"));
        String[] expected = new String[] { "0", "1", "4", "6" };

        assertTrue(op.has(alpha));
        assertTrue(op.has(bravo));
        assertTrue(op.has(charlie));
        assertTrue(op.has(delta));

        assertNull(op.get(alpha));
        assertEquals("2", op.get(bravo));
        assertEquals("3", op.get(charlie));
        assertEquals("5", op.get(delta));

        assertArrayEquals(expected, nonOpt.toArray());
    }

    @Test
    public void testRepeated() throws SoarException
    {
        op.newOption(alpha).done();
        op.newOption(bravo).optionalArg().done();
        op.newOption(charlie).requiredArg().done();

        List<String> nonOpt = op.process(Lists.newArrayList("command", "-a",
                "-a", "-b", "-b", "-b", "1", "-c", "-c", "-c", "2", "3"));
        String[] expected = new String[] { "3" };

        assertTrue(op.has(alpha));
        assertTrue(op.has(bravo));
        assertTrue(op.has(charlie));

        assertNull(op.get(alpha));
        assertEquals("1", op.get(bravo));
        assertEquals("2", op.get(charlie));

        assertArrayEquals(expected, nonOpt.toArray());
    }

    @Test
    public void testStopOption() throws SoarException
    {
        op.newOption(alpha).done();
        op.newOption(bravo).done();

        List<String> nonOpt = op.process(Lists.newArrayList("command", "-a",
                "--", "-b", "3"));
        String[] expected = new String[] { "-b", "3" };

        assertTrue(op.has(alpha));
        assertFalse(op.has(bravo));

        assertArrayEquals(expected, nonOpt.toArray());
    }

    @Test
    public void testOptionArgConsumesStopOption() throws SoarException
    {
        op.newOption(alpha).optionalArg().done();
        op.newOption(bravo).done();

        List<String> nonOpt = op.process(Lists.newArrayList("command", "-a",
                "--", "-b", "3"));
        String[] expected = new String[] { "3" };

        assertTrue(op.has(alpha));
        assertEquals("--", op.get(alpha));
        assertTrue(op.has(bravo));

        assertArrayEquals(expected, nonOpt.toArray());
    }

    @Test
    public void testIgnoreNegativeInteger() throws SoarException
    {
        op.newOption(alpha).optionalArg().done();
        op.newOption(bravo).done();

        List<String> nonOpt = op.process(Lists.newArrayList("command", "-a",
                "-1", "-2", "3"));
        String[] expected = new String[] { "-2", "3" };

        assertTrue(op.has(alpha));
        assertEquals("-1", op.get(alpha));
        assertFalse(op.has(bravo));

        assertArrayEquals(expected, nonOpt.toArray());

    }

    @Test
    public void testIgnoreNegativeDouble() throws SoarException
    {
        op.newOption(alpha).optionalArg().done();
        op.newOption(bravo).done();

        List<String> nonOpt = op.process(Lists.newArrayList("command", "-a",
                "-1.43", "-2.676", "3"));
        String[] expected = new String[] { "-2.676", "3" };

        assertTrue(op.has(alpha));
        assertEquals("-1.43", op.get(alpha));
        assertFalse(op.has(bravo));

        assertArrayEquals(expected, nonOpt.toArray());

    }

    @Test
    public void testManualSetUnset() throws SoarException
    {
        op.newOption(alpha).optionalArg().done();
        op.newOption(bravo).done();
        op.process(Lists.newArrayList("command"));

        assertFalse(op.has(alpha));
        assertFalse(op.has(bravo));

        op.set(alpha);

        assertTrue(op.has(alpha));
        assertFalse(op.has(bravo));

        op.unset(alpha);
        op.set(bravo);

        assertFalse(op.has(alpha));
        assertTrue(op.has(bravo));
        assertNull(op.get(bravo));

        op.set(bravo, "arg"); // ignores fact this option takes no arg

        assertFalse(op.has(alpha));
        assertTrue(op.has(bravo));
        assertEquals("arg", op.get(bravo));

        op.unset(bravo);
        op.set(bravo, null);

        assertFalse(op.has(alpha));
        assertTrue(op.has(bravo));
        assertNull(op.get(bravo));
    }

    @Test(expected = IllegalStateException.class)
    public void testManualOrderException1()
    {
        op.newOption(alpha).done();
        op.set(alpha);
    }

    @Test(expected = IllegalStateException.class)
    public void testManualOrderException2()
    {
        op.newOption(alpha).done();
        op.unset(alpha);
    }

    @Test(expected = IllegalStateException.class)
    public void testManualOrderException3()
    {
        op.newOption(alpha).done();
        op.set(alpha, null);
    }

    @Test(expected = NullPointerException.class)
    public void testHasNull() throws SoarException
    {
        op.newOption(alpha).done();
        op.process(Lists.newArrayList("command"));
        op.has(null);
    }

    @Test(expected = NullPointerException.class)
    public void testGetNull() throws SoarException
    {
        op.newOption(alpha).done();
        op.process(Lists.newArrayList("command"));
        op.get(null);
    }

    @Test(expected = NullPointerException.class)
    public void testSetNull() throws SoarException
    {
        op.newOption(alpha).done();
        op.process(Lists.newArrayList("command"));
        op.set(null);
    }

    @Test(expected = NullPointerException.class)
    public void testSetArgNull() throws SoarException
    {
        op.newOption(alpha).done();
        op.process(Lists.newArrayList("command"));
        op.set(null, null); // the second null here is legal
    }

    @Test(expected = NullPointerException.class)
    public void testUnsetNull() throws SoarException
    {
        op.newOption(alpha).done();
        op.process(Lists.newArrayList("command"));
        op.unset(null);
    }

    @Test
    public void testClientUpperCaseGiven() throws SoarException
    {
        op.newOption(alpha).done();
        op.process(Lists.newArrayList("command", "--ALPHA"));
        assertTrue(op.has(alpha));
    }

    @Test
    public void testKeyUpperCaseGiven() throws SoarException
    {
        op.newOption(alpha).done();
        op.process(Lists.newArrayList("command", "--alpha"));
        assertTrue(op.has("ALPHA"));
    }

    @Test
    public void testGetInteger() throws SoarException
    {
        op.newOption(alpha).requiredArg().done();
        op.process(Lists.newArrayList("command", "--alpha", "1"));
        assertTrue(op.has(alpha));
        assertEquals(1, op.getInteger(alpha));
    }

    @Test
    public void testGetDouble() throws SoarException
    {
        op.newOption(alpha).requiredArg().done();
        op.process(Lists.newArrayList("command", "--alpha", "1.0"));
        assertTrue(op.has(alpha));
        assertEquals(1.0, op.getDouble(alpha), 0); // 1.0 is representable
        // exactly
    }

    @Test
    public void testGetFloat() throws SoarException
    {
        op.newOption(alpha).requiredArg().done();
        op.process(Lists.newArrayList("command", "--alpha", "1.0"));
        assertTrue(op.has(alpha));
        assertEquals(1.0, op.getFloat(alpha), 0); // 1.0 is representable
        // exactly
    }

    @Test(expected = SoarException.class)
    public void testGetBadInteger() throws SoarException
    {
        op.newOption(alpha).requiredArg().done();
        op.process(Lists.newArrayList("command", "--alpha", "1.0"));
        assertTrue(op.has(alpha));
        op.getInteger(alpha);
    }

    @Test(expected = SoarException.class)
    public void testGetBadDouble() throws SoarException
    {
        op.newOption(alpha).requiredArg().done();
        op.process(Lists.newArrayList("command", "--alpha", "a"));
        assertTrue(op.has(alpha));
        op.getDouble(alpha);
    }

    @Test(expected = SoarException.class)
    public void testGetBadFloat() throws SoarException
    {
        op.newOption(alpha).requiredArg().done();
        op.process(Lists.newArrayList("command", "--alpha", "a"));
        assertTrue(op.has(alpha));
        op.getFloat(alpha);
    }

    @Test
    public void testToString() throws SoarException
    {
        op.newOption(alpha).newOption(bravo).requiredArg().newOption(charlie)
                .optionalArg().done();

        System.out.println(op);
    }

}
