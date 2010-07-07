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

public class OptionProcessorTest
{
    enum Options
    {
        alpha, accent, beta, bravo, charlie, ceasar, delta, echo, fox_trot
    }

    private OptionProcessor<Options> op;

    @Before
    public void setUp() throws Exception
    {
        op = OptionProcessor.create();
    }

    @Test(expected = NullPointerException.class)
    public void testRegisterNullClient()
    {
        op.newOption(null, "alpha").register();
    }

    @Test(expected = NullPointerException.class)
    public void testRegisterNullLong()
    {
        op.newOption(Options.alpha, null).register();
    }

    @Test(expected = NullPointerException.class)
    public void testRegisterNullShort()
    {
        op.newOption(Options.alpha, "alpha").setShortOption(null).register();
    }

    @Test(expected = IllegalArgumentException.class)
    public void testRegisterTwiceLong()
    {
        op.newOption(Options.alpha, "alpha").register();
        op.newOption(Options.alpha, "alpha").register();
    }

    @Test(expected = IllegalArgumentException.class)
    public void testRegisterTwiceLongDiffShort()
    {
        op.newOption(Options.alpha, "alpha").setShortOption("a").register();
        op.newOption(Options.alpha, "alpha").setShortOption("A").register();
    }

    @Test(expected = IllegalArgumentException.class)
    public void testRegisterTwiceShort()
    {
        op.newOption(Options.alpha, "alpha").setShortOption("a").register();
        op.newOption(Options.beta, "beta").setShortOption("a").register();
    }

    @Test
    public void testRegisterSameOption()
    {
        op.newOption(Options.alpha, "alpha").setShortOption("a").register();
        op.newOption(Options.alpha, "beta").setShortOption("b").register();
    }

    @SuppressWarnings("unchecked")
    @Test(expected=IllegalStateException.class)
    public void testRegisterPostmod1()
    {
        OptionBuilder b = op.newOption(Options.alpha, "alpha").setShortOption("a");
        b.register();
        b.setShortOption("a");
    }

    @SuppressWarnings("unchecked")
    @Test(expected=IllegalStateException.class)
    public void testRegisterPostmod2()
    {
        OptionBuilder b = op.newOption(Options.alpha, "alpha").setShortOption("a");
        b.register();
        b.setNoArg();
    }

    @SuppressWarnings("unchecked")
    @Test(expected=IllegalStateException.class)
    public void testRegisterPostmod3()
    {
        OptionBuilder b = op.newOption(Options.alpha, "alpha").setShortOption("a");
        b.register();
        b.setOptionalArg();
    }

    @SuppressWarnings("unchecked")
    @Test(expected=IllegalStateException.class)
    public void testRegisterPostmod4()
    {
        OptionBuilder b = op.newOption(Options.alpha, "alpha").setShortOption("a");
        b.register();
        b.setRequiredArg();
    }

    @SuppressWarnings("unchecked")
    @Test(expected=IllegalStateException.class)
    public void testRegisterTwice()
    {
        OptionBuilder b = op.newOption(Options.alpha, "alpha").setShortOption("a");
        b.register();
        b.register();
    }

    @Test(expected = IllegalArgumentException.class)
    public void testRegisterTwiceLongDifferentType()
    {
        op.newOption(Options.beta, "beta").setNoArg().register();
        op.newOption(Options.beta, "beta").setOptionalArg().register();
    }

    @Test(expected = IllegalArgumentException.class)
    public void testRegisterDash()
    {
        op.newOption(Options.alpha, "-").register();
    }

    @Test(expected = IllegalArgumentException.class)
    public void testRegisterEmptyLong()
    {
        op.newOption(Options.alpha, "").register();
    }

    @Test(expected = IllegalArgumentException.class)
    public void testRegisterEmptyShort()
    {
        op.newOption(Options.alpha, "alpha").setShortOption("").register();
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
                    op.newOption(Options.alpha, String.valueOf(i)).register();
                    throw new AssertionError(
                            "Should have rejected long option " + i);
                }
                catch (IllegalArgumentException e)
                {
                }
                try
                {
                    op.newOption(Options.alpha, "alpha").setShortOption(
                            String.valueOf(i)).register();
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
    public void testRegisterLegalOptions() throws SoarException
    {
        verifySingleOption(Options.alpha, "alpha", null, "alpha", "a");
        verifySingleOption(Options.accent, "Accent", null, "accent", "A");
        verifySingleOption(Options.beta, "beta", "b", "beta", "b");
        verifySingleOption(Options.bravo, "bravo", "B", "bravo", "B");
        verifySingleOption(Options.echo, "echo-", null, "echo-", "e");
        verifySingleOption(Options.fox_trot, "fox-trot", null, "fox-trot", "f");
    }

    public void verifySingleOption(Options option, String lReg, String sReg,
            String lExpect, String sExpect) throws SoarException
    {
        if (sReg == null)
            op.newOption(option, lReg).register();
        else
            op.newOption(option, lReg).setShortOption(sReg).register();

        op.process(Lists.newArrayList("command", "asdf"));
        assertFalse(op.has(option));

        op.process(Lists.newArrayList("command", "--" + lExpect));
        assertTrue(op.has(option));
        op.process(Lists.newArrayList("command", "-" + sExpect));
        assertTrue(op.has(option));
    }

    @Test
    public void testProcessNoOptions() throws SoarException
    {
        op.process(new ArrayList<String>());
        op.process(Lists.newArrayList("command"));
        op.process(Lists.newArrayList("command", "arg"));
        op.process(Lists.newArrayList("command", "arg", "arg"));
    }

    @Test(expected = IllegalStateException.class)
    public void testCheckOutOfOrder()
    {
        op.newOption(Options.alpha, "alpha").register();
        op.has(Options.alpha);
    }

    @Test(expected = IllegalStateException.class)
    public void testCheckArgOutOfOrder()
    {
        op.newOption(Options.alpha, "alpha").register();
        op.getArgument(Options.alpha);
    }

    @Test
    public void testMultipleOptions() throws SoarException
    {
        op.newOption(Options.alpha, "alpha").register();
        op.newOption(Options.bravo, "bravo").register();

        List<String> empty = new ArrayList<String>();
        List<String> arg = Lists.newArrayList("arg");

        verify(Lists.newArrayList("command", "--alpha", "--bravo"), empty,
                Options.alpha, Options.bravo);
        verify(Lists.newArrayList("command", "arg", "--alpha", "--bravo"), arg,
                Options.alpha, Options.bravo);
        verify(Lists.newArrayList("command", "--alpha", "arg", "--bravo"), arg,
                Options.alpha, Options.bravo);
        verify(Lists.newArrayList("command", "--alpha", "--bravo", "arg"), arg,
                Options.alpha, Options.bravo);

        verify(Lists.newArrayList("command", "-a", "--bravo"), empty,
                Options.alpha, Options.bravo);
        verify(Lists.newArrayList("command", "arg", "-a", "--bravo"), arg,
                Options.alpha, Options.bravo);
        verify(Lists.newArrayList("command", "-a", "arg", "--bravo"), arg,
                Options.alpha, Options.bravo);
        verify(Lists.newArrayList("command", "-a", "--bravo", "arg"), arg,
                Options.alpha, Options.bravo);

        verify(Lists.newArrayList("command", "--alpha", "-b"), empty,
                Options.alpha, Options.bravo);
        verify(Lists.newArrayList("command", "arg", "--alpha", "-b"), arg,
                Options.alpha, Options.bravo);
        verify(Lists.newArrayList("command", "--alpha", "arg", "-b"), arg,
                Options.alpha, Options.bravo);
        verify(Lists.newArrayList("command", "--alpha", "-b", "arg"), arg,
                Options.alpha, Options.bravo);

        verify(Lists.newArrayList("command", "-a", "-b"), empty, Options.alpha,
                Options.bravo);
        verify(Lists.newArrayList("command", "arg", "-a", "-b"), arg,
                Options.alpha, Options.bravo);
        verify(Lists.newArrayList("command", "-a", "arg", "-b"), arg,
                Options.alpha, Options.bravo);
        verify(Lists.newArrayList("command", "-a", "-b", "arg"), arg,
                Options.alpha, Options.bravo);

        verify(Lists.newArrayList("command", "-b", "-a"), empty, Options.alpha,
                Options.bravo);
        verify(Lists.newArrayList("command", "arg", "-b", "-a"), arg,
                Options.alpha, Options.bravo);
        verify(Lists.newArrayList("command", "-b", "arg", "-a"), arg,
                Options.alpha, Options.bravo);
        verify(Lists.newArrayList("command", "-b", "-a", "arg"), arg,
                Options.alpha, Options.bravo);

        verify(Lists.newArrayList("command", "--bravo", "-a"), empty,
                Options.alpha, Options.bravo);
        verify(Lists.newArrayList("command", "arg", "--bravo", "-a"), arg,
                Options.alpha, Options.bravo);
        verify(Lists.newArrayList("command", "--bravo", "arg", "-a"), arg,
                Options.alpha, Options.bravo);
        verify(Lists.newArrayList("command", "--bravo", "-a", "arg"), arg,
                Options.alpha, Options.bravo);

        verify(Lists.newArrayList("command", "-b", "--alpha"), empty,
                Options.alpha, Options.bravo);
        verify(Lists.newArrayList("command", "arg", "-b", "--alpha"), arg,
                Options.alpha, Options.bravo);
        verify(Lists.newArrayList("command", "-b", "arg", "--alpha"), arg,
                Options.alpha, Options.bravo);
        verify(Lists.newArrayList("command", "-b", "--alpha", "arg"), arg,
                Options.alpha, Options.bravo);

        verify(Lists.newArrayList("command", "--bravo", "--alpha"), empty,
                Options.alpha, Options.bravo);
        verify(Lists.newArrayList("command", "arg", "--bravo", "--alpha"), arg,
                Options.alpha, Options.bravo);
        verify(Lists.newArrayList("command", "--bravo", "arg", "--alpha"), arg,
                Options.alpha, Options.bravo);
        verify(Lists.newArrayList("command", "--bravo", "--alpha", "arg"), arg,
                Options.alpha, Options.bravo);

        verify(Lists.newArrayList("command", "-ab"), empty, Options.alpha,
                Options.bravo);
        verify(Lists.newArrayList("command", "arg", "-ab"), arg, Options.alpha,
                Options.bravo);
        verify(Lists.newArrayList("command", "-ab", "arg"), arg, Options.alpha,
                Options.bravo);

        verify(Lists.newArrayList("command", "-ba"), empty, Options.alpha,
                Options.bravo);
        verify(Lists.newArrayList("command", "arg", "-ba"), arg, Options.alpha,
                Options.bravo);
        verify(Lists.newArrayList("command", "-ba", "arg"), arg, Options.alpha,
                Options.bravo);
    }

    private void verify(List<String> line, List<String> nonOptExpected,
            Options... options) throws SoarException
    {
        List<String> nonOpt = op.process(line);
        for (Options o : options)
            assertTrue(op.has(o));
        assertArrayEquals(nonOptExpected.toArray(), nonOpt.toArray());
    }

    @Test
    public void testNonOptArgOrder() throws SoarException
    {
        op.newOption(Options.alpha, "alpha").register();
        op.newOption(Options.bravo, "bravo").register();
        op.newOption(Options.charlie, "charlie").register();

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

        assertTrue(op.has(Options.alpha));
        assertTrue(op.has(Options.bravo));
        assertTrue(op.has(Options.charlie));

        assertArrayEquals(expected, nonOpt.toArray());
    }

    @Test
    public void testOptionalArg() throws SoarException
    {
        op.newOption(Options.alpha, "alpha").setOptionalArg().register();

        op.process(Lists.newArrayList("command", "-a"));
        assertTrue(op.has(Options.alpha));
        assertNull(op.getArgument(Options.alpha));

        op.process(Lists.newArrayList("command", "-a", "arg"));
        assertTrue(op.has(Options.alpha));
        assertEquals("arg", op.getArgument(Options.alpha));
    }

    @Test
    public void testRequiredArg() throws SoarException
    {
        op.newOption(Options.alpha, "alpha").setRequiredArg().register();

        op.process(Lists.newArrayList("command", "-a", "arg"));
        assertTrue(op.has(Options.alpha));
        assertEquals("arg", op.getArgument(Options.alpha));
    }

    @Test(expected = SoarException.class)
    public void testMissingRequired() throws SoarException
    {
        op.newOption(Options.alpha, "alpha").setRequiredArg().register();
        op.process(Lists.newArrayList("command", "-a"));
    }

    @Test(expected = SoarException.class)
    public void testMissingRequiredTwo() throws SoarException
    {
        op.newOption(Options.alpha, "alpha").register();
        op.newOption(Options.bravo, "bravo").setRequiredArg().register();
        op.process(Lists.newArrayList("command", "-ab"));
    }

    @Test
    public void testArgConsumesOption() throws SoarException
    {
        op.newOption(Options.alpha, "alpha").setOptionalArg().register();
        op.newOption(Options.bravo, "bravo").setRequiredArg().register();

        op.process(Lists.newArrayList("command", "-ab"));
        assertTrue(op.has(Options.alpha));
        assertEquals("b", op.getArgument(Options.alpha));
        assertFalse(op.has(Options.bravo));

        op.process(Lists.newArrayList("command", "-a", "-b"));
        assertTrue(op.has(Options.alpha));
        assertEquals("-b", op.getArgument(Options.alpha));
        assertFalse(op.has(Options.bravo));

        op.process(Lists.newArrayList("command", "-b", "-a"));
        assertTrue(op.has(Options.bravo));
        assertEquals("-a", op.getArgument(Options.bravo));
        assertFalse(op.has(Options.alpha));
    }

    @Test
    public void testMixed() throws SoarException
    {
        op.newOption(Options.alpha, "alpha").register();
        op.newOption(Options.bravo, "bravo").setOptionalArg().register();
        op.newOption(Options.charlie, "charlie").setRequiredArg().register();
        op.newOption(Options.delta, "delta").setRequiredArg().register();

        List<String> nonOpt = op.process(Lists.newArrayList("command", "0",
                "-a", "1", "-b", "2", "-c", "3", "4", "-d", "5", "6"));
        String[] expected = new String[] { "0", "1", "4", "6" };

        assertTrue(op.has(Options.alpha));
        assertTrue(op.has(Options.bravo));
        assertTrue(op.has(Options.charlie));
        assertTrue(op.has(Options.delta));

        assertNull(op.getArgument(Options.alpha));
        assertEquals("2", op.getArgument(Options.bravo));
        assertEquals("3", op.getArgument(Options.charlie));
        assertEquals("5", op.getArgument(Options.delta));

        assertArrayEquals(expected, nonOpt.toArray());
    }

    @Test
    public void testRepeated() throws SoarException
    {
        op.newOption(Options.alpha, "alpha").register();
        op.newOption(Options.bravo, "bravo").setOptionalArg().register();
        op.newOption(Options.charlie, "charlie").setRequiredArg().register();

        List<String> nonOpt = op.process(Lists.newArrayList("command", "-a",
                "-a", "-b", "-b", "-b", "1", "-c", "-c", "-c", "2", "3"));
        String[] expected = new String[] { "3" };

        assertTrue(op.has(Options.alpha));
        assertTrue(op.has(Options.bravo));
        assertTrue(op.has(Options.charlie));

        assertNull(op.getArgument(Options.alpha));
        assertEquals("1", op.getArgument(Options.bravo));
        assertEquals("2", op.getArgument(Options.charlie));

        assertArrayEquals(expected, nonOpt.toArray());
    }

    @Test
    public void testStopOption() throws SoarException
    {
        op.newOption(Options.alpha, "alpha").register();
        op.newOption(Options.bravo, "bravo").register();

        List<String> nonOpt = op.process(Lists.newArrayList("command", "-a",
                "--", "-b", "3"));
        String[] expected = new String[] { "-b", "3" };

        assertTrue(op.has(Options.alpha));
        assertFalse(op.has(Options.bravo));

        assertArrayEquals(expected, nonOpt.toArray());
    }

    @Test
    public void testOptionArgConsumesStopOption() throws SoarException
    {
        op.newOption(Options.alpha, "alpha").setOptionalArg().register();
        op.newOption(Options.bravo, "bravo").register();

        List<String> nonOpt = op.process(Lists.newArrayList("command", "-a",
                "--", "-b", "3"));
        String[] expected = new String[] { "3" };

        assertTrue(op.has(Options.alpha));
        assertEquals("--", op.getArgument(Options.alpha));
        assertTrue(op.has(Options.bravo));

        assertArrayEquals(expected, nonOpt.toArray());
    }

    @Test
    public void testIgnoreNegativeInteger() throws SoarException
    {
        op.newOption(Options.alpha, "alpha").setOptionalArg().register();
        op.newOption(Options.bravo, "bravo").register();

        List<String> nonOpt = op.process(Lists.newArrayList("command", "-a",
                "-1", "-2", "3"));
        String[] expected = new String[] { "-2", "3" };

        assertTrue(op.has(Options.alpha));
        assertEquals("-1", op.getArgument(Options.alpha));
        assertFalse(op.has(Options.bravo));

        assertArrayEquals(expected, nonOpt.toArray());

    }

    @Test
    public void testIgnoreNegativeDouble() throws SoarException
    {
        op.newOption(Options.alpha, "alpha").setOptionalArg().register();
        op.newOption(Options.bravo, "bravo").register();

        List<String> nonOpt = op.process(Lists.newArrayList("command", "-a",
                "-1.43", "-2.676", "3"));
        String[] expected = new String[] { "-2.676", "3" };

        assertTrue(op.has(Options.alpha));
        assertEquals("-1.43", op.getArgument(Options.alpha));
        assertFalse(op.has(Options.bravo));

        assertArrayEquals(expected, nonOpt.toArray());

    }
    
    @Test
    public void testManualSetUnset() throws SoarException
    {
        op.newOption(Options.alpha, "alpha").setOptionalArg().register();
        op.newOption(Options.bravo, "bravo").register();
        op.process(Lists.newArrayList("command"));

        assertFalse(op.has(Options.alpha));
        assertFalse(op.has(Options.bravo));

        op.set(Options.alpha);

        assertTrue(op.has(Options.alpha));
        assertFalse(op.has(Options.bravo));

        op.unset(Options.alpha);
        op.set(Options.bravo);

        assertFalse(op.has(Options.alpha));
        assertTrue(op.has(Options.bravo));
        assertNull(op.getArgument(Options.bravo));
        
        op.set(Options.bravo, "arg"); // ignores fact this option takes no arg

        assertFalse(op.has(Options.alpha));
        assertTrue(op.has(Options.bravo));
        assertEquals("arg", op.getArgument(Options.bravo));
        
        op.unset(Options.bravo);
        op.set(Options.bravo, null);

        assertFalse(op.has(Options.alpha));
        assertTrue(op.has(Options.bravo));
        assertNull(op.getArgument(Options.bravo));
    }
    
    @Test(expected=IllegalStateException.class)
    public void testManualOrderException1()
    {
        op.newOption(Options.alpha, "alpha").register();
        op.set(Options.alpha);
    }
    
    @Test(expected=IllegalStateException.class)
    public void testManualOrderException2()
    {
        op.newOption(Options.alpha, "alpha").register();
        op.unset(Options.alpha);
    }
    
    @Test(expected=IllegalStateException.class)
    public void testManualOrderException3()
    {
        op.newOption(Options.alpha, "alpha").register();
        op.set(Options.alpha, null);
    }
    
    @Test(expected=NullPointerException.class)
    public void testHasNull() throws SoarException
    {
        op.newOption(Options.alpha, "alpha").register();
        op.process(Lists.newArrayList("command"));
        op.has(null);
    }
    
    @Test(expected=NullPointerException.class)
    public void testGetNull() throws SoarException
    {
        op.newOption(Options.alpha, "alpha").register();
        op.process(Lists.newArrayList("command"));
        op.getArgument(null);
    }
    
    @Test(expected=NullPointerException.class)
    public void testSetNull() throws SoarException
    {
        op.newOption(Options.alpha, "alpha").register();
        op.process(Lists.newArrayList("command"));
        op.set(null);
    }
    
    @Test(expected=NullPointerException.class)
    public void testSetArgNull() throws SoarException
    {
        op.newOption(Options.alpha, "alpha").register();
        op.process(Lists.newArrayList("command"));
        op.set(null, null); // the second null here is legal
    }
    
    @Test(expected=NullPointerException.class)
    public void testUnsetNull() throws SoarException
    {
        op.newOption(Options.alpha, "alpha").register();
        op.process(Lists.newArrayList("command"));
        op.unset(null);
    }
}
