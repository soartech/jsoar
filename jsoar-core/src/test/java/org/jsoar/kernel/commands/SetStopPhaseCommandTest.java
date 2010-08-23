/*
 * Copyright (c) 2010 Dave Ray <daveray@gmail.com>
 *
 * Created on May 29, 2010
 */
package org.jsoar.kernel.commands;


import static org.junit.Assert.*;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.jsoar.kernel.Phase;
import org.jsoar.kernel.SoarException;
import org.jsoar.kernel.SoarProperties;
import org.jsoar.util.commands.DefaultSoarCommandContext;
import org.jsoar.util.properties.PropertyManager;
import org.junit.Before;
import org.junit.Test;

public class SetStopPhaseCommandTest
{
    private PropertyManager props;
    private SetStopPhaseCommand command;
    
    @Before
    public void setUp() throws Exception
    {
        props = new PropertyManager();
        command = new SetStopPhaseCommand(props);
    }
    
    @Test(expected=SoarException.class)
    public void testThrowsExceptionOnUnknownOption() throws Exception
    {
        verify(null, "--unknown");
    }
    
    // input -> propose -> decision -> apply -> output
    
    @Test
    public void testSetToBeforeInput() throws Exception
    {
        props.set(SoarProperties.STOP_PHASE, Phase.OUTPUT); // since input is default
        verify(Phase.INPUT, "-i");
        verify(Phase.INPUT, "-B", "-i");
        verify(Phase.INPUT, "--before", "--input");
        verify(Phase.INPUT, "--input", "-B");
    }
    
    @Test
    public void testSetToAfterInput() throws Exception
    {
        verify(Phase.PROPOSE, "-A", "-i");
        verify(Phase.PROPOSE, "--after", "--input");
        verify(Phase.PROPOSE, "--input", "-A");
    }
    
    @Test
    public void testSetToBeforePropose() throws Exception
    {
        verify(Phase.PROPOSE, "-p");
        verify(Phase.PROPOSE, "-B", "-p");
        verify(Phase.PROPOSE, "--before", "--proposal");
        verify(Phase.PROPOSE, "--proposal", "-B");
    }
    
    @Test
    public void testSetToAfterPropose() throws Exception
    {
        verify(Phase.DECISION, "-A", "-p");
        verify(Phase.DECISION, "--after", "--proposal");
        verify(Phase.DECISION, "--proposal", "-A");
    }
    
    @Test
    public void testSetToBeforeDecision() throws Exception
    {
        verify(Phase.DECISION, "-d");
        verify(Phase.DECISION, "-B", "-d");
        verify(Phase.DECISION, "--before", "--decision");
        verify(Phase.DECISION, "--decision", "-B");
    }
    
    @Test
    public void testSetToAfterDecision() throws Exception
    {
        verify(Phase.APPLY, "-A", "-d");
        verify(Phase.APPLY, "--after", "--decision");
        verify(Phase.APPLY, "--decision", "-A");
    }
    
    @Test
    public void testSetToBeforeApply() throws Exception
    {
        verify(Phase.APPLY, "-a");
        verify(Phase.APPLY, "-B", "-a");
        verify(Phase.APPLY, "--before", "--apply");
        verify(Phase.APPLY, "--apply", "-B");
    }
    
    @Test
    public void testSetToAfterApply() throws Exception
    {
        verify(Phase.OUTPUT, "-A", "-a");
        verify(Phase.OUTPUT, "--after", "--apply");
        verify(Phase.OUTPUT, "--apply", "-A");
    }
    
    @Test
    public void testSetToBeforeOutput() throws Exception
    {
        verify(Phase.OUTPUT, "-o");
        verify(Phase.OUTPUT, "-B","-o");
        verify(Phase.OUTPUT, "--before", "--output");
        verify(Phase.OUTPUT, "--output", "-B");
    }
    
    @Test
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
        assertSame(expectedPhase, props.get(SoarProperties.STOP_PHASE));
    }

}
