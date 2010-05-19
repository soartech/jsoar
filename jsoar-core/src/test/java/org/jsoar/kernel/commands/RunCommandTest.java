/*
 * Copyright (c) 2010 Dave Ray <daveray@gmail.com>
 *
 * Created on May 19, 2010
 */
package org.jsoar.kernel.commands;

import static org.junit.Assert.*;

import org.jsoar.kernel.AgentRunController;
import org.jsoar.kernel.RunType;
import org.jsoar.kernel.SoarException;
import org.junit.Before;
import org.junit.Test;


public class RunCommandTest
{
    private MockRunControl mock;
    private RunCommand command;
    
    private static class MockRunControl implements AgentRunController
    {
        long count = -1;
        RunType runType;
        
        /* (non-Javadoc)
         * @see org.jsoar.kernel.AgentRunController#runFor(long, org.jsoar.kernel.RunType)
         */
        @Override
        public void runFor(long n, RunType runType)
        {
            this.count = n;
            this.runType = runType;
        }
    }
    
    @Before
    public void setUp()
    {
        this.mock = new MockRunControl();
        this.command = new RunCommand(mock);
    }
    
    @Test(expected=SoarException.class)
    public void testThrowsExceptionOnNonNumericCount() throws Exception
    {
        execute("run", "-d", "xyz");
    }
    
    @Test(expected=SoarException.class)
    public void testThrowsExceptionOnZeroCount() throws Exception
    {
        execute("run", "-e", "0");
    }
    
    @Test(expected=SoarException.class)
    public void testThrowsExceptionNegativeCount() throws Exception
    {
        execute("run", "-e", "-10");
    }
    
    @Test
    public void testCountDefaultsToOneDecisionIfNoIntegerArgumentIsGiven() throws Exception
    {
        execute("run", "-d");
        verify(1, RunType.DECISIONS);
        execute("run", "--decision");
        verify(1, RunType.DECISIONS);
    }
    @Test
    public void testCountDefaultsToOneElaborationIfNoIntegerArgumentIsGiven() throws Exception
    {
        execute("run", "-e");
        verify(1, RunType.ELABORATIONS);
        execute("run", "--elaboration");
        verify(1, RunType.ELABORATIONS);
    }
    @Test
    public void testCountDefaultsToOnePhaseIfNoIntegerArgumentIsGiven() throws Exception
    {
        execute("run", "-p");
        verify(1, RunType.PHASES);
        execute("run", "--phase");
        verify(1, RunType.PHASES);
    }
    
    @Test
    public void testCountDecisions() throws Exception
    {
        execute("run", "-d", "99");
        verify(99, RunType.DECISIONS);
        execute("run", "--decision", "99");
        verify(99, RunType.DECISIONS);
    }
    @Test
    public void testCountElaborations() throws Exception
    {
        execute("run", "-e", "100");
        verify(100, RunType.ELABORATIONS);
        execute("run", "--elaboration", "100");
        verify(100, RunType.ELABORATIONS);
    }
    @Test
    public void testCountPhases() throws Exception
    {
        execute("run", "-p", "7654321");
        verify(7654321, RunType.PHASES);
        execute("run", "--phase", "7654321");
        verify(7654321, RunType.PHASES);
    }
   
    
    ////////////////////////////////////////////////////////////////////////
    
    private void execute(String... args) throws SoarException
    {
        mock.count = -1;
        mock.runType = null;
        command.execute(args);
    }
    private void verify(long count, RunType runType)
    {
        assertEquals(count, mock.count);
        assertEquals(runType, mock.runType);
    }
}
