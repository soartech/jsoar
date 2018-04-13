/*
 * Copyright (c) 2010 Dave Ray <daveray@gmail.com>
 *
 * Created on Jun 11, 2010
 */
package org.jsoar.kernel.commands;


import android.test.AndroidTestCase;

import junit.framework.Assert;

import org.jsoar.kernel.SoarException;
import org.jsoar.kernel.commands.ExplainBacktracesCommand.Options;

public class ExplainBacktracesCommandTest extends AndroidTestCase
{

    public void testFullOption() throws Exception
    {
        verifyOptions(new Options(false, -1, null), "eb");
        verifyOptions(new Options(true, -1, null), "eb", "-f");
        verifyOptions(new Options(true, -1, null), "eb", "--full");
    }

    public void testConditionOption() throws Exception
    {
        verifyOptions(new Options(false, -1, null), "eb");
        verifyOptions(new Options(false, 99, null), "eb", "-c", "99");
        verifyOptions(new Options(false, 101, null), "eb", "--condition", "101");
    }

    public void testThrowsExceptionForNonNumericCondition()
    {
        try {
            ExplainBacktracesCommand.processArgs(new String[] {"eb", "-c", "b"});
            Assert.fail("Should have thrown exception");
        } catch (SoarException e) {
            e.printStackTrace();
        }
    }

    public void testThrowsExceptionForMissingNumericCondition()
    {
        try {
            ExplainBacktracesCommand.processArgs(new String[] {"eb","-c" });
            Assert.fail("Should have thrown exception");
        } catch (SoarException e) {
            //success
        }
    }

    public void testProductionArgument() throws Exception
    {
        verifyOptions(new Options(false, -1, "p"), "eb", "p");
    }

    public void testMixedOptions() throws Exception
    {
        verifyOptions(new Options(false, 99, "rule"), "eb", "-c", "99", "rule");
        verifyOptions(new Options(true, 101, "hello"), "eb", "--condition", "101", "--full", "hello");
        verifyOptions(new Options(true, -1, "hello"), "eb", "-f", "hello");
    }

    private void verifyOptions(Options expected, String ... args) throws SoarException
    {
        final Options result = ExplainBacktracesCommand.processArgs(args);
        Assert.assertEquals(expected, result);
    }
}
