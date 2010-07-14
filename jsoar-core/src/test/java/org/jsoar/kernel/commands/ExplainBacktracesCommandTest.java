/*
 * Copyright (c) 2010 Dave Ray <daveray@gmail.com>
 *
 * Created on Jun 11, 2010
 */
package org.jsoar.kernel.commands;


import static org.junit.Assert.*;

import org.jsoar.kernel.SoarException;
import org.jsoar.kernel.commands.ExplainBacktracesCommand.Options;
import org.junit.Test;

public class ExplainBacktracesCommandTest
{

    @Test
    public void testFullOption() throws Exception
    {
        verifyOptions(new Options(false, -1, null), "eb");
        verifyOptions(new Options(true, -1, null), "eb", "-f");
        verifyOptions(new Options(true, -1, null), "eb", "--full");
    }
    
    @Test
    public void testConditionOption() throws Exception
    {
        verifyOptions(new Options(false, -1, null), "eb");
        verifyOptions(new Options(false, 99, null), "eb", "-c", "99");
        verifyOptions(new Options(false, 101, null), "eb", "--condition", "101");
    }
    
    @Test(expected=SoarException.class)
    public void testThrowsExceptionForNonNumericCondition() throws Exception
    {
        ExplainBacktracesCommand.processArgs(new String[] {"eb", "-c", "b"});
    }
    
    @Test(expected=SoarException.class)
    public void testThrowsExceptionForMissingNumericCondition() throws Exception
    {
        ExplainBacktracesCommand.processArgs(new String[] {"eb","-c" });
    }
    
    @Test
    public void testProductionArgument() throws Exception
    {
        verifyOptions(new Options(false, -1, "p"), "eb", "p");
    }
    
    @Test
    public void testMixedOptions() throws Exception
    {
        verifyOptions(new Options(false, 99, "rule"), "eb", "-c", "99", "rule");
        verifyOptions(new Options(true, 101, "hello"), "eb", "--condition", "101", "--full", "hello");
        verifyOptions(new Options(true, -1, "hello"), "eb", "-f", "hello");
    }

    private void verifyOptions(Options expected, String ... args) throws SoarException
    {
        final Options result = ExplainBacktracesCommand.processArgs(args);
        assertEquals(expected, result);
    }
}
