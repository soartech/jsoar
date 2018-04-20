/*
 * Copyright (c) 2010 Dave Ray <daveray@gmail.com>
 *
 * Created on May 29, 2010
 */
package org.jsoar.kernel.commands;


import android.test.AndroidTestCase;

import junit.framework.Assert;

import org.jsoar.kernel.Phase;
import org.jsoar.kernel.SoarException;
import org.jsoar.kernel.SoarProperties;
import org.jsoar.util.commands.DefaultSoarCommandContext;
import org.jsoar.util.properties.PropertyManager;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class SetStopPhaseCommandTest extends AndroidTestCase
{
    private PropertyManager props;
    private SetStopPhaseCommand command;
    
    @Override
    public void setUp() throws Exception
    {
        props = new PropertyManager();
        command = new SetStopPhaseCommand(props);
    }
    
    public void testThrowsExceptionOnUnknownOption()
    {
        try {
            verify(null, "--unknown");
            fail("Should have thrown exception");
        } catch (SoarException e) {
            Assert.assertEquals("Unknown option '--unknown'", e.getMessage());
        }
    }
    
    // input -> propose -> decision -> apply -> output
    
    public void testSetToBeforeInput() throws Exception
    {
        props.set(SoarProperties.STOP_PHASE, Phase.OUTPUT); // since input is default
        verify(Phase.INPUT, "-i");
        verify(Phase.INPUT, "-B", "-i");
        verify(Phase.INPUT, "--before", "--input");
        verify(Phase.INPUT, "--input", "-B");
    }
    
    public void testSetToAfterInput() throws Exception
    {
        verify(Phase.PROPOSE, "-A", "-i");
        verify(Phase.PROPOSE, "--after", "--input");
        verify(Phase.PROPOSE, "--input", "-A");
    }
    
    public void testSetToBeforePropose() throws Exception
    {
        verify(Phase.PROPOSE, "-p");
        verify(Phase.PROPOSE, "-B", "-p");
        verify(Phase.PROPOSE, "--before", "--proposal");
        verify(Phase.PROPOSE, "--proposal", "-B");
    }
    
    public void testSetToAfterPropose() throws Exception
    {
        verify(Phase.DECISION, "-A", "-p");
        verify(Phase.DECISION, "--after", "--proposal");
        verify(Phase.DECISION, "--proposal", "-A");
    }
    
    public void testSetToBeforeDecision() throws Exception
    {
        verify(Phase.DECISION, "-d");
        verify(Phase.DECISION, "-B", "-d");
        verify(Phase.DECISION, "--before", "--decision");
        verify(Phase.DECISION, "--decision", "-B");
    }
    
    public void testSetToAfterDecision() throws Exception
    {
        verify(Phase.APPLY, "-A", "-d");
        verify(Phase.APPLY, "--after", "--decision");
        verify(Phase.APPLY, "--decision", "-A");
    }
    
    public void testSetToBeforeApply() throws Exception
    {
        verify(Phase.APPLY, "-a");
        verify(Phase.APPLY, "-B", "-a");
        verify(Phase.APPLY, "--before", "--apply");
        verify(Phase.APPLY, "--apply", "-B");
    }
    
    public void testSetToAfterApply() throws Exception
    {
        verify(Phase.OUTPUT, "-A", "-a");
        verify(Phase.OUTPUT, "--after", "--apply");
        verify(Phase.OUTPUT, "--apply", "-A");
    }

    public void testSetToBeforeOutput() throws Exception
    {
        verify(Phase.OUTPUT, "-o");
        verify(Phase.OUTPUT, "-B","-o");
        verify(Phase.OUTPUT, "--before", "--output");
        verify(Phase.OUTPUT, "--output", "-B");
    }
    
    public void testSetToAfterOutput() throws Exception
    {
        props.set(SoarProperties.STOP_PHASE, Phase.OUTPUT); // since input is default
        verify(Phase.INPUT, "-A", "-o");
        verify(Phase.INPUT, "--after", "--output");
        verify(Phase.INPUT, "--output", "-A");
    }
    
    private void verify(Phase expectedPhase, String ... args) throws SoarException
    {
        final List<String> argsList = new ArrayList<String>(Arrays.asList(args));
        argsList.add(0, "set-stop-phase");
        command.execute(DefaultSoarCommandContext.empty(), argsList.toArray(new String[] {}));
        Assert.assertSame(expectedPhase, props.get(SoarProperties.STOP_PHASE));
    }

}
