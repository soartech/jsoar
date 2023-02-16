/*
 * Copyright (c) 2010 Dave Ray <daveray@gmail.com>
 *
 * Created on Jul 4, 2010
 */
package org.jsoar.kernel.smem;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.File;
import java.io.StringWriter;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;

import org.jsoar.kernel.FunctionalTestHarness;
import org.jsoar.kernel.Phase;
import org.jsoar.kernel.RunType;
import org.jsoar.kernel.SoarProperties;
import org.jsoar.kernel.rhs.functions.AbstractRhsFunctionHandler;
import org.jsoar.kernel.rhs.functions.RhsFunctionContext;
import org.jsoar.kernel.rhs.functions.RhsFunctionException;
import org.jsoar.kernel.rhs.functions.RhsFunctionHandler;
import org.jsoar.kernel.smem.DefaultSemanticMemory.BasicWeightedCue;
import org.jsoar.kernel.symbols.Symbol;
import org.jsoar.runtime.ThreadedAgent;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;

/**
 * @author ray
 */
public class SMemFunctionalTests extends FunctionalTestHarness
{
    @Test
    public void testSimpleCueBasedRetrieval() throws Exception
    {
        agent.setStopPhase(Phase.OUTPUT);
        runTest("testSimpleCueBasedRetrieval", 1);
    }
    
    @Test
    public void testSimpleNonCueBasedRetrieval() throws Exception
    {
        agent.setStopPhase(Phase.OUTPUT);
        runTest("testSimpleNonCueBasedRetrieval", 2);
    }
    
    @Test
    public void testSimpleStore() throws Exception
    {
        agent.setStopPhase(Phase.OUTPUT);
        runTest("testSimpleStore", 2);
    }
    
    @Test
    public void testTrivialMathQuery() throws Exception
    {
        agent.setStopPhase(Phase.OUTPUT);
        runTest("testTrivialMathQuery", 2);
    }
    
    @Test
    public void testBadMathQuery() throws Exception
    {
        agent.setStopPhase(Phase.OUTPUT);
        runTest("testBadMathQuery", 2);
    }
    
    @Test
    public void testMaxQuery() throws Exception
    {
        agent.setStopPhase(Phase.OUTPUT);
        runTest("testMax", 1);
    }
    
    @Test
    public void testMaxMixedTypes() throws Exception
    {
        agent.setStopPhase(Phase.OUTPUT);
        runTest("testMaxMixedTypes", 1);
    }
    
    @Test
    public void testMaxMultivalued() throws Exception
    {
        agent.setStopPhase(Phase.OUTPUT);
        runTest("testMaxMultivalued", 1);
    }
    
    @Test
    public void testMin() throws Exception
    {
        agent.setStopPhase(Phase.OUTPUT);
        runTest("testMin", 1);
    }
    
    @Test
    public void testMaxNegQuery() throws Exception
    {
        agent.setStopPhase(Phase.OUTPUT);
        runTest("testMaxNegation", 1);
    }
    
    @Test
    public void testGreater() throws Exception
    {
        agent.setStopPhase(Phase.OUTPUT);
        runTest("testGreater", 1);
    }
    
    @Test
    public void testLess() throws Exception
    {
        agent.setStopPhase(Phase.OUTPUT);
        runTest("testLess", 1);
    }
    
    @Test
    public void testGreaterOrEqual() throws Exception
    {
        agent.setStopPhase(Phase.OUTPUT);
        runTest("testGreaterOrEqual", 1);
    }
    
    @Test
    public void testLessOrEqual() throws Exception
    {
        agent.setStopPhase(Phase.OUTPUT);
        runTest("testLessOrEqual", 1);
    }
    
    @Test
    public void testLessWithNeg() throws Exception
    {
        agent.setStopPhase(Phase.OUTPUT);
        runTest("testLessWithNeg", 1);
    }
    
    @Test
    public void testLessNoSolution() throws Exception
    {
        agent.setStopPhase(Phase.OUTPUT);
        runTest("testLessNoSolution", 1);
    }
    
    @Test
    public void testMirroring() throws Exception
    {
        agent.setStopPhase(Phase.OUTPUT);
        runTest("testMirroring", 4);
    }
    
    @Test
    public void testMergeAdd() throws Exception
    {
        agent.setStopPhase(Phase.OUTPUT);
        runTest("testMergeAdd", 4);
    }
    
    @Test
    public void testMergeNone() throws Exception
    {
        agent.setStopPhase(Phase.OUTPUT);
        runTest("testMergeNone", 4);
    }
    
    @Test
    public void testSimpleStoreMultivaluedAttribute() throws Exception
    {
        agent.setStopPhase(Phase.OUTPUT);
        runTest("testSimpleStoreMultivaluedAttribute", 2);
    }
    
    @Test
    public void testSimpleFloat() throws Exception
    {
        runTest("testSimpleFloat", 5);
    }
    
    @Test
    public void testMaxDoublePrecision_Irrational() throws Exception
    {
        runTest("testMaxDoublePrecision-Irrational", 5);
    }
    
    @Test
    public void testMaxDoublePrecision() throws Exception
    {
        runTest("testMaxDoublePrecision", 5);
    }
    
    @Test
    public void testSimpleNonCueBasedRetrievalOfNonExistingLTI() throws Exception
    {
        runTest("testSimpleNonCueBasedRetrievalOfNonExistingLTI", 1);
    }
    
    @Test
    public void testNegQuery() throws Exception
    {
        runTest("testNegQuery", 248);
    }
    
    @Test
    public void testNegStringFloat() throws Exception
    {
        runTest("testNegStringFloat", 5);
    }
    
    @Test
    public void testNegQueryNoHash() throws Exception
    {
        runTest("testNegQueryNoHash", 1);
    }
    
    @Test
    public void testCueSelection() throws Exception
    {
        runTestSetup("testCueSelection");
        agent.runFor(2, RunType.DECISIONS);
        DefaultSemanticMemory smem = (DefaultSemanticMemory) agent.getAdapter(DefaultSemanticMemory.class);
        BasicWeightedCue bwc = smem.getLastCue();
        assertTrue(
                bwc.cue.attr.asString().getValue().equals("name") &&
                        bwc.cue.value.asString().getValue().equals("val") &&
                        bwc.weight == 4,
                "Incorrect cue selected");
    }
    
    private boolean halted = false;
    
    @Test
    public void testSimpleNonCueBasedRetrieval_ActivationRecency() throws Exception
    {
        runTestSetup("testSimpleNonCueBasedRetrieval_ActivationRecency");
        
        final RhsFunctionHandler oldHalt = agent.getRhsFunctions().getHandler("halt");
        assertNotNull(oldHalt);
        
        agent.getRhsFunctions().registerHandler(new AbstractRhsFunctionHandler("halt")
        {
            
            @Override
            public Symbol execute(RhsFunctionContext rhsContext, List<Symbol> arguments) throws RhsFunctionException
            {
                halted = true;
                return oldHalt.execute(rhsContext, arguments);
            }
        });
        
        agent.runFor(3, RunType.DECISIONS);
        
        assertTrue(halted, "testSimpleNonCueBasedRetrieval_ActivationRecency functional test did not halt");
        
        String expected = "\n" +
                "(@L1 ^x 1 ^y 2 ^z 3 [+2.0])\n" +
                "(@L2 ^x 2 ^y 3 ^z 1 [+6.0])\n" +
                "(@X1 ^name |foo| ^location @L1 [+1.0])\n" +
                "(@X2 ^name |foo| ^location @L2 [+5.0])\n";
        
        StringWriter sw = new StringWriter();
        agent.getPrinter().pushWriter(sw);
        agent.getInterpreter().eval("print @");
        agent.getPrinter().popWriter();
        String result = sw.toString();
        
        assertEquals(expected, result, "testSimpleNonCueBasedRetrieval_ActivationRecency: Invalid Activation Values");
        
        halted = false;
    }
    
    @Test
    public void testSimpleNonCueBasedRetrieval_ActivationRecency_WithoutActivateOnQuery() throws Exception
    {
        runTestSetup("testSimpleNonCueBasedRetrieval_ActivationRecency_WithoutActivateOnQuery");
        
        final RhsFunctionHandler oldHalt = agent.getRhsFunctions().getHandler("halt");
        assertNotNull(oldHalt);
        
        agent.getRhsFunctions().registerHandler(new AbstractRhsFunctionHandler("halt")
        {
            
            @Override
            public Symbol execute(RhsFunctionContext rhsContext, List<Symbol> arguments) throws RhsFunctionException
            {
                halted = true;
                return oldHalt.execute(rhsContext, arguments);
            }
        });
        
        agent.runFor(3, RunType.DECISIONS);
        
        assertTrue(halted, "testSimpleNonCueBasedRetrieval_ActivationRecency_WithoutActivateOnQuery functional test did not halt");
        
        String expected = "\n" +
                "(@L1 ^x 1 ^y 2 ^z 3 [+2.0])\n" +
                "(@L2 ^x 2 ^y 3 ^z 1 [+5.0])\n" +
                "(@X1 ^name |foo| ^location @L1 [+1.0])\n" +
                "(@X2 ^name |foo| ^location @L2 [+3.0])\n";
        
        StringWriter sw = new StringWriter();
        agent.getPrinter().pushWriter(sw);
        agent.getInterpreter().eval("print @");
        agent.getPrinter().popWriter();
        String result = sw.toString();
        
        assertEquals(expected, result, "testSimpleNonCueBasedRetrieval_ActivationRecency_WithoutActivateOnQuery: Invalid Activation Values");
        
        halted = false;
    }
    
    @Test
    public void testSimpleNonCueBasedRetrieval_ActivationFrequency() throws Exception
    {
        runTestSetup("testSimpleNonCueBasedRetrieval_ActivationFrequency");
        
        final RhsFunctionHandler oldHalt = agent.getRhsFunctions().getHandler("halt");
        assertNotNull(oldHalt);
        
        agent.getRhsFunctions().registerHandler(new AbstractRhsFunctionHandler("halt")
        {
            
            @Override
            public Symbol execute(RhsFunctionContext rhsContext, List<Symbol> arguments) throws RhsFunctionException
            {
                halted = true;
                return oldHalt.execute(rhsContext, arguments);
            }
        });
        
        agent.runFor(3, RunType.DECISIONS);
        
        assertTrue(halted, "testSimpleNonCueBasedRetrieval_ActivationFrequency functional test did not halt");
        
        String expected = "\n" +
                "(@L1 ^x 1 ^y 2 ^z 3 [+1.0])\n" +
                "(@L2 ^x 2 ^y 3 ^z 1 [+2.0])\n" +
                "(@X1 ^name |foo| ^location @L1 [+1.0])\n" +
                "(@X2 ^name |foo| ^location @L2 [+2.0])\n";
        
        StringWriter sw = new StringWriter();
        agent.getPrinter().pushWriter(sw);
        agent.getInterpreter().eval("print @");
        agent.getPrinter().popWriter();
        String result = sw.toString();
        
        assertEquals(expected, result, "testSimpleNonCueBasedRetrieval_ActivationFrequency: Invalid Activation Values");
        
        halted = false;
    }
    
    private boolean checkActivationValues(String activationString, List<Double> lowEndExpectations, List<Double> highEndExpectations)
    {
        List<String> activationLevels = new ArrayList<>();
        String activation = "";
        boolean inActivationParse = false;
        
        for(Character c : activationString.toCharArray())
        {
            if(c.equals('['))
            {
                inActivationParse = true;
                continue;
            }
            else if(c.equals(']') && inActivationParse)
            {
                inActivationParse = false;
                activationLevels.add(activation);
                
                activation = "";
                
                continue;
            }
            
            if(inActivationParse && (Character.isDigit(c) || c.equals('.') || c.equals('+') || c.equals('-')))
            {
                if(activation.length() != 0 &&
                        (c.equals('+') || c.equals('-')))
                {
                    throw new AssertionError("Found a +/- where there shouldn't be in Activation Levels!");
                }
                
                activation += c;
            }
            else if(inActivationParse)
            {
                throw new AssertionError("Non-Digit Character in Activation Level");
            }
        }
        
        if(activationLevels.size() != lowEndExpectations.size())
        {
            throw new AssertionError("Low End Expectations is not the same size as parsed Activation Levels!");
        }
        else if(activationLevels.size() != highEndExpectations.size())
        {
            throw new AssertionError("High End Expectations is not the same size as parsed Activation Levels!");
        }
        
        List<Double> activationLevelsAsDoubles = new ArrayList<>();
        
        for(String a : activationLevels)
        {
            activationLevelsAsDoubles.add(Double.parseDouble(a));
        }
        
        for(int i = 0; i < activationLevelsAsDoubles.size(); i++)
        {
            Double a = activationLevelsAsDoubles.get(i);
            
            if(!(a >= lowEndExpectations.get(i) && a <= highEndExpectations.get(i)))
            {
                throw new AssertionError("Parsed Activation " + i + 1 + " (" + a + ") is not within [" + lowEndExpectations.get(i) + ", " + highEndExpectations.get(i) + "]");
            }
        }
        
        return true;
    }
    
    @Test
    public void testSimpleNonCueBasedRetrieval_ActivationBaseLevel_Stable() throws Exception
    {
        runTestSetup("testSimpleNonCueBasedRetrieval_ActivationBaseLevel_Stable");
        
        final RhsFunctionHandler oldHalt = agent.getRhsFunctions().getHandler("halt");
        assertNotNull(oldHalt);
        
        agent.getRhsFunctions().registerHandler(new AbstractRhsFunctionHandler("halt")
        {
            
            @Override
            public Symbol execute(RhsFunctionContext rhsContext, List<Symbol> arguments) throws RhsFunctionException
            {
                halted = true;
                return oldHalt.execute(rhsContext, arguments);
            }
        });
        
        agent.runFor(3, RunType.DECISIONS);
        
        assertTrue(halted, "testSimpleNonCueBasedRetrieval_ActivationBaseLevel_Stable functional test did not halt");
        
        List<Double> lowEndExpectations = new ArrayList<>();
        List<Double> highEndExpectations = new ArrayList<>();
        
        lowEndExpectations.add(0.0);
        highEndExpectations.add(0.0);
        
        lowEndExpectations.add(0.455);
        highEndExpectations.add(0.456);
        
        lowEndExpectations.add(0.0);
        highEndExpectations.add(0.0);
        
        lowEndExpectations.add(0.455);
        highEndExpectations.add(0.456);
        
        // This is the expected output from print @ modified from CSoar to look like JSoar outputs it (reverse string attributes)
        @SuppressWarnings("unused")
        String expected = "========================================\n" +
                "            Semantic Memory             \n" +
                "========================================\n" +
                "(@L1 ^x 1 ^y 2 ^z 3 [+0.0])\n" +
                "(@L2 ^x 2 ^y 3 ^z 1 [+0.456])\n" +
                "(@X1 ^name |foo| ^location @L1 [+0.0])\n" +
                "(@X2 ^name |foo| ^location @L2 [+0.456])\n";
        
        StringWriter sw = new StringWriter();
        agent.getPrinter().pushWriter(sw);
        agent.getInterpreter().eval("print @");
        agent.getPrinter().popWriter();
        String result = sw.toString();
        
        assertTrue(checkActivationValues(result, lowEndExpectations, highEndExpectations), "testSimpleNonCueBasedRetrieval_ActivationBaseLevel_Stable: Invalid Activation Values");
        
        halted = false;
    }
    
    @Test
    public void testSimpleNonCueBasedRetrieval_ActivationBaseLevel_Naive() throws Exception
    {
        runTestSetup("testSimpleNonCueBasedRetrieval_ActivationBaseLevel_Naive");
        
        final RhsFunctionHandler oldHalt = agent.getRhsFunctions().getHandler("halt");
        assertNotNull(oldHalt);
        
        agent.getRhsFunctions().registerHandler(new AbstractRhsFunctionHandler("halt")
        {
            
            @Override
            public Symbol execute(RhsFunctionContext rhsContext, List<Symbol> arguments) throws RhsFunctionException
            {
                halted = true;
                return oldHalt.execute(rhsContext, arguments);
            }
        });
        
        agent.runFor(3, RunType.DECISIONS);
        
        assertTrue(halted, "testSimpleNonCueBasedRetrieval_ActivationBaseLevel_Naive functional test did not halt");
        
        List<Double> lowEndExpectations = new ArrayList<>();
        List<Double> highEndExpectations = new ArrayList<>();
        
        lowEndExpectations.add(0.0);
        highEndExpectations.add(0.0);
        
        lowEndExpectations.add(0.455);
        highEndExpectations.add(0.456);
        
        lowEndExpectations.add(-0.694);
        highEndExpectations.add(-0.693);
        
        lowEndExpectations.add(0.455);
        highEndExpectations.add(0.456);
        
        // This is the expected output from print @ modified from CSoar to look like JSoar outputs it (reverse string attributes)
        @SuppressWarnings("unused")
        String expected = "========================================\n" +
                "            Semantic Memory             \n" +
                "========================================\n" +
                "(@L1 ^x 1 ^y 2 ^z 3 [+0.0])\n" +
                "(@L2 ^x 2 ^y 3 ^z 1 [+0.456])\n" +
                "(@X1 ^name |foo| ^location @L1 [-0.693])\n" +
                "(@X2 ^name |foo| ^location @L2 [+0.456])\n";
        
        StringWriter sw = new StringWriter();
        agent.getPrinter().pushWriter(sw);
        agent.getInterpreter().eval("print @");
        agent.getPrinter().popWriter();
        String result = sw.toString();
        
        assertTrue(checkActivationValues(result, lowEndExpectations, highEndExpectations), "testSimpleNonCueBasedRetrieval_ActivationBaseLevel_Naive: Invalid Activation Values");
        
        halted = false;
    }
    
    @Test
    public void testSimpleNonCueBasedRetrieval_ActivationBaseLevel_Incremental() throws Exception
    {
        runTestSetup("testSimpleNonCueBasedRetrieval_ActivationBaseLevel_Incremental");
        
        final RhsFunctionHandler oldHalt = agent.getRhsFunctions().getHandler("halt");
        assertNotNull(oldHalt);
        
        agent.getRhsFunctions().registerHandler(new AbstractRhsFunctionHandler("halt")
        {
            
            @Override
            public Symbol execute(RhsFunctionContext rhsContext, List<Symbol> arguments) throws RhsFunctionException
            {
                halted = true;
                return oldHalt.execute(rhsContext, arguments);
            }
        });
        
        agent.runFor(4, RunType.DECISIONS);
        
        assertTrue(halted, "testSimpleNonCueBasedRetrieval_ActivationBaseLevel_Incremental functional test did not halt");
        
        List<Double> lowEndExpectations = new ArrayList<>();
        List<Double> highEndExpectations = new ArrayList<>();
        
        lowEndExpectations.add(-0.347);
        highEndExpectations.add(-0.346);
        
        lowEndExpectations.add(0.405);
        highEndExpectations.add(0.406);
        
        lowEndExpectations.add(0.109);
        highEndExpectations.add(0.110);
        
        lowEndExpectations.add(0.143);
        highEndExpectations.add(0.144);
        
        // This is the expected output from print @ modified from CSoar to look like JSoar outputs it (reverse string attributes)
        @SuppressWarnings("unused")
        String expected = "========================================\n" +
                "            Semantic Memory             \n" +
                "========================================\n" +
                "(@L1 ^x 1 ^y 2 ^z 3 [-0.347])\n" +
                "(@L2 ^x 2 ^y 3 ^z 1 [+0.405])\n" +
                "(@X1 ^name |foo| ^location @L1 [+0.109])\n" +
                "(@X2 ^name |foo| ^location @L2 [+0.144])\n";
        
        StringWriter sw = new StringWriter();
        agent.getPrinter().pushWriter(sw);
        agent.getInterpreter().eval("print @");
        agent.getPrinter().popWriter();
        String result = sw.toString();
        
        assertTrue(checkActivationValues(result, lowEndExpectations, highEndExpectations), "testSimpleNonCueBasedRetrieval_ActivationBaseLevel_Incremental: Invalid Activation Values");
        
        halted = false;
    }
    
    @Test
    public void dbBackupAndLoadTests() throws Exception
    {
        StringWriter outputWriter = new StringWriter();
        agent.getPrinter().addPersistentWriter(outputWriter);
        
        runTestSetup("testFactorization");
        agent.runFor(1178, RunType.DECISIONS);
        
        outputWriter.getBuffer().setLength(0);
        agent.getInterpreter().eval("p s1");
        
        String resultOfPS1 = outputWriter.toString();
        
        outputWriter.getBuffer().setLength(0);
        
        String expectedResultOfPS1 = "(S1 ^counter 50 ^epmem E1 ^io I1 ^name Factorization ^operator O1385 ^operator O1385 + ^reward-link R1 ^smem S2 ^superstate nil ^type state ^using-smem true)\n";
        
        assertEquals(expectedResultOfPS1, resultOfPS1, "Didn't stop where expected!");
        
        agent.getInterpreter().eval("smem --backup backup.sqlite");
        agent.getInterpreter().eval("smem --init");
        outputWriter.getBuffer().setLength(0);
        
        agent.getInterpreter().eval("p @");
        
        assertEquals("SMem| Semantic memory is empty.", outputWriter.toString(), "smem --init didn't init smem!");
        
        outputWriter.getBuffer().setLength(0);
        
        agent.getInterpreter().eval("p");
        
        String resultOfP = outputWriter.toString();
        outputWriter.getBuffer().setLength(0);
        
        assertEquals(0, resultOfP.length(), "smem --init didn't excise all productions!");
        
        agent.getInterpreter().eval("p s1");
        
        resultOfPS1 = outputWriter.toString();
        outputWriter.getBuffer().setLength(0);
        
        expectedResultOfPS1 = "(S1 ^epmem E1 ^io I1 ^reward-link R1 ^smem S2 ^superstate nil ^type state)\n";
        
        assertEquals(expectedResultOfPS1, resultOfPS1, "smem --init didn't reinit WM!");
        
        agent.getInterpreter().eval("smem --set path backup.sqlite");
        agent.getInterpreter().eval("smem --set append-database on");
        agent.getInterpreter().eval("smem --init");
        
        runTestSetup("testFactorization");
        
        final RhsFunctionHandler oldHalt = agent.getRhsFunctions().getHandler("halt");
        assertNotNull(oldHalt);
        
        agent.getRhsFunctions().registerHandler(new AbstractRhsFunctionHandler("halt")
        {
            
            @Override
            public Symbol execute(RhsFunctionContext rhsContext, List<Symbol> arguments) throws RhsFunctionException
            {
                halted = true;
                return oldHalt.execute(rhsContext, arguments);
            }
        });
        
        agent.runFor(2811 + 1, RunType.DECISIONS);
        
        assertTrue(halted, "testFactorization: Test did not halt.");
        
        outputWriter.getBuffer().setLength(0);
        
        agent.getInterpreter().eval("p -d 2 @F197");
        
        String expectedResultOfPD2F197 = "\n" +
                "(@F197 ^complete |true| ^number 100 ^factor @F48 @F198 [+368.0])\n" +
                " (@F48 ^value 5 ^multiplicity 2 [+370.0])\n" +
                " (@F198 ^value 2 ^multiplicity 2 [+369.0])\n";
        
        String resultOfPD2F197 = outputWriter.toString();
        
        assertEquals(expectedResultOfPD2F197, resultOfPD2F197, "testFactorization: Test did not get the correct result!");
        
        agent.dispose();
        
        String pwd = agent.getInterpreter().eval("pwd");
        pwd = pwd.replaceAll("\\s+", "/");
        File backupDB = new File(pwd + "/backup.sqlite");
        backupDB.delete();
    }
    
    @Test
    @Disabled
    public void readCSoarDB() throws Exception
    {
        agent.initialize();
        
        URL db = getClass().getResource("smem-csoar-db.sqlite");
        assertNotNull(db, "No CSoar db!");
        agent.getInterpreter().eval("smem --set path " + db.getPath());
        agent.getInterpreter().eval("smem --set append-database on");
        agent.getInterpreter().eval("smem --init");
        
        StringWriter sw = new StringWriter();
        agent.getPrinter().pushWriter(sw);
        agent.getInterpreter().eval("print @");
        agent.getPrinter().popWriter();
        String actualResult = sw.toString();
        
        String expectedResult = "(@F1 ^number 2 ^complete |true| ^factor @F2 [+5.0])\n" +
                "(@F2 ^value 2 ^multiplicity 1 [+6.0])\n" +
                "(@F3 ^number 3 ^complete |true| ^factor @F4 [+3.0])\n" +
                "(@F4 ^value 3 ^multiplicity 1 [+4.0])\n" +
                "(@F5 ^number 4 ^complete |true| ^factor @F6 [+7.0])\n" +
                "(@F6 ^value 2 ^multiplicity 2 [+8.0])\n";
        
        assertEquals(expectedResult, actualResult, "Unexpected output from CSoar database!");
    }
    
    @Test
    public void testSimpleStoreGC() throws Exception
    {
        agent.setStopPhase(Phase.OUTPUT);
        runTestSetup("testSimpleStoreGC");
        
        agent.runFor(2, RunType.DECISIONS);
        
        StringWriter sw = new StringWriter();
        agent.getPrinter().pushWriter(sw);
        agent.getInterpreter().eval("print @");
        agent.getPrinter().popWriter();
        String result = sw.toString();
        
        String[] split = result.split("\\s+");
        
        List<String> ltis = new ArrayList<>();
        
        for(String lti : split)
        {
            if(lti.length() == 3 ||
                    lti.length() == 4)
            {
                if(lti.charAt(0) == '@')
                {
                    ltis.add(lti);
                }
                else if(lti.charAt(0) == '(' &&
                        lti.charAt(1) == '@')
                {
                    ltis.add(lti.substring(1));
                }
            }
        }
        
        System.gc();
        System.gc();
        System.gc();
        System.gc();
        System.gc();
        
        agent.runFor(4, RunType.DECISIONS);
        
        StringWriter outputWriter;
        agent.getPrinter().addPersistentWriter(
                outputWriter = new StringWriter());
        
        agent.getInterpreter().eval("print (* ^retrieve *)");
        
        String result_after = outputWriter.toString();
        
        String[] split_after = result_after.split("\\s+");
        
        for(String lti : split_after)
        {
            if((lti.length() == 3 || lti.length() == 4) && lti.charAt(0) == '@')
            {
                String correctLti = lti.substring(0, 3);
                assertTrue(ltis.contains(correctLti));
            }
        }
    }
    
    @Test
    public void testMultiAgent() throws Exception
    {
        List<ThreadedAgent> agents = new ArrayList<>();
        
        for(int i = 1; i <= 250; i++)
        {
            ThreadedAgent t = ThreadedAgent.create("Agent " + i);
            t.getAgent().getTrace().setEnabled(true);
            String sourceName = getClass().getSimpleName() + "_testMultiAgent.soar";
            URL sourceUrl = getClass().getResource(sourceName);
            assertNotNull(sourceUrl, "Could not find test file " + sourceName);
            t.getAgent().getInterpreter().source(sourceUrl);
            
            agents.add(t);
        }
        
        for(ThreadedAgent a : agents)
        {
            a.runFor(4 + 1, RunType.DECISIONS);
        }
        
        boolean allStopped = false;
        while(!allStopped)
        {
            allStopped = true;
            
            for(ThreadedAgent a : agents)
            {
                if(a.isRunning())
                {
                    allStopped = false;
                    break;
                }
            }
        }
        
        for(ThreadedAgent a : agents)
        {
            if(a.getAgent().getProperties().get(SoarProperties.DECISION_PHASES_COUNT).intValue() != 4)
            {
                throw new AssertionError("Agent did not stop correctly! Ran too many cycles!");
            }
            
            StringWriter sw = new StringWriter();
            a.getPrinter().pushWriter(sw);
            a.getAgent().getInterpreter().eval("smem");
            a.getPrinter().popWriter();
            String result = sw.toString();
            
            if(!result.contains("Native"))
            {
                throw new AssertionError("Non Native Driver!");
            }
        }
    }
}
