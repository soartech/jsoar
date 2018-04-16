/*
 * Copyright (c) 2010 Dave Ray <daveray@gmail.com>
 *
 * Created on Jul 4, 2010
 */
package org.jsoar.kernel.smem;

import android.support.test.InstrumentationRegistry;

import junit.framework.Assert;

import org.jsoar.kernel.FunctionalTestHarness;
import org.jsoar.kernel.Phase;
import org.jsoar.kernel.RunType;
import org.jsoar.kernel.SoarException;
import org.jsoar.kernel.SoarProperties;
import org.jsoar.kernel.epmem.EpMemFunctionalTests;
import org.jsoar.kernel.rhs.functions.AbstractRhsFunctionHandler;
import org.jsoar.kernel.rhs.functions.RhsFunctionContext;
import org.jsoar.kernel.rhs.functions.RhsFunctionException;
import org.jsoar.kernel.rhs.functions.RhsFunctionHandler;
import org.jsoar.kernel.smem.DefaultSemanticMemory.BasicWeightedCue;
import org.jsoar.kernel.symbols.Symbol;
import org.jsoar.runtime.ThreadedAgent;
import org.junit.Ignore;
import org.junit.Test;

import java.io.File;
import java.io.StringWriter;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;

/**
 * @author ray
 */
public class SMemFunctionalTests extends FunctionalTestHarness
{
    public void testSimpleCueBasedRetrieval() throws Exception
    {
        agent.setStopPhase(Phase.OUTPUT);
        runTest("testSimpleCueBasedRetrieval", 1);
    }
    
    public void testSimpleNonCueBasedRetrieval() throws Exception
    {
        agent.setStopPhase(Phase.OUTPUT);
        runTest("testSimpleNonCueBasedRetrieval", 2);
    }
    
    public void testSimpleStore() throws Exception
    {
        agent.setStopPhase(Phase.OUTPUT);
        runTest("testSimpleStore", 2);
    }
    
    public void testTrivialMathQuery() throws Exception
    {
        agent.setStopPhase(Phase.OUTPUT);
        runTest("testTrivialMathQuery", 2);
    }
    
    public void testBadMathQuery() throws Exception
    {
        agent.setStopPhase(Phase.OUTPUT);
        runTest("testBadMathQuery", 2);
    }
    
    public void testMaxQuery() throws Exception
    {
        agent.setStopPhase(Phase.OUTPUT);
        runTest("testMax", 1);
    }
    
    public void testMaxMixedTypes() throws Exception
    {
        agent.setStopPhase(Phase.OUTPUT);
        runTest("testMaxMixedTypes", 1);
    }
    
    public void testMaxMultivalued() throws Exception
    {
        agent.setStopPhase(Phase.OUTPUT);
        runTest("testMaxMultivalued", 1);
    }
    
    public void testMin() throws Exception
    {
        agent.setStopPhase(Phase.OUTPUT);
        runTest("testMin", 1);
    }
    
    public void testMaxNegQuery() throws Exception
    {
        agent.setStopPhase(Phase.OUTPUT);
        runTest("testMaxNegation", 1);
    }
    
    public void testGreater() throws Exception
    {
        agent.setStopPhase(Phase.OUTPUT);
        runTest("testGreater", 1);
    }
    
    public void testLess() throws Exception
    {
        agent.setStopPhase(Phase.OUTPUT);
        runTest("testLess", 1);
    }
    
    public void testGreaterOrEqual() throws Exception
    {
        agent.setStopPhase(Phase.OUTPUT);
        runTest("testGreaterOrEqual", 1);
    }
    
    public void testLessOrEqual() throws Exception
    {
        agent.setStopPhase(Phase.OUTPUT);
        runTest("testLessOrEqual", 1);
    }
    
    public void testLessWithNeg() throws Exception
    {
        agent.setStopPhase(Phase.OUTPUT);
        runTest("testLessWithNeg", 1);
    }
    
    public void testLessNoSolution() throws Exception
    {
        agent.setStopPhase(Phase.OUTPUT);
        runTest("testLessNoSolution", 1);
    }
    
    public void testMirroring() throws Exception
    {
        agent.setStopPhase(Phase.OUTPUT);
        runTest("testMirroring", 4);
    }
    
    public void testMergeAdd() throws Exception
    {
        agent.setStopPhase(Phase.OUTPUT);
        runTest("testMergeAdd", 4);
    }
    
    public void testMergeNone() throws Exception
    {
        agent.setStopPhase(Phase.OUTPUT);
        runTest("testMergeNone", 4);
    }
    
    public void testSimpleStoreMultivaluedAttribute() throws Exception
    {
        agent.setStopPhase(Phase.OUTPUT);
        runTest("testSimpleStoreMultivaluedAttribute", 2);
    }
    
    public void testSimpleFloat() throws Exception
    {
        runTest("testSimpleFloat", 5);
    }
    
    public void testMaxDoublePrecision_Irrational() throws Exception
    {
        runTest("testMaxDoublePrecision-Irrational", 5);
    }
    
    public void testMaxDoublePrecision() throws Exception
    {
        runTest("testMaxDoublePrecision", 5);
    }
    
    public void testSimpleNonCueBasedRetrievalOfNonExistingLTI() throws Exception
    {
        runTest("testSimpleNonCueBasedRetrievalOfNonExistingLTI", 1);
    }
    
    public void testNegQuery() throws Exception
    {
        runTest("testNegQuery", 248);
    }
    
    public void testNegStringFloat() throws Exception
    {
        runTest("testNegStringFloat", 5);
    }
    
    public void testNegQueryNoHash() throws Exception
    {
        runTest("testNegQueryNoHash", 1);
    }
    
    public void testCueSelection() throws Exception
    {
        runTestSetup("testCueSelection");
        agent.runFor(2, RunType.DECISIONS);
        DefaultSemanticMemory smem = (DefaultSemanticMemory) agent.getAdapter(DefaultSemanticMemory.class);
        BasicWeightedCue bwc = smem.getLastCue();
        Assert.assertTrue("Incorrect cue selected",
                bwc.cue.attr.asString().getValue().equals("name") && 
                bwc.cue.value.asString().getValue().equals("val") &&
                bwc.weight == 4);
    }
    
    private boolean halted = false;
    
    public void testSimpleNonCueBasedRetrieval_ActivationRecency() throws Exception
    {
        runTestSetup("testSimpleNonCueBasedRetrieval_ActivationRecency");
        
        final RhsFunctionHandler oldHalt = agent.getRhsFunctions().getHandler("halt");
        Assert.assertNotNull(oldHalt);
        
        agent.getRhsFunctions().registerHandler(new AbstractRhsFunctionHandler("halt") {

            @Override
            public Symbol execute(RhsFunctionContext rhsContext, List<Symbol> arguments) throws RhsFunctionException
            {
                halted = true;
                return oldHalt.execute(rhsContext, arguments);
            }
        });
        
        agent.runFor(3, RunType.DECISIONS);

        Assert.assertTrue("testSimpleNonCueBasedRetrieval_ActivationRecency functional test did not halt", halted);
        
        String expected = "========================================\n" +
                          "            Semantic Memory             \n" +         
                          "========================================\n" +
                          "(@L1 ^x 1 ^y 2 ^z 3 [+2.0])\n" +
                          "(@L2 ^x 2 ^y 3 ^z 1 [+6.0])\n" +
                          "(@X1 ^name |foo| ^location @L1 [+1.0])\n" +
                          "(@X2 ^name |foo| ^location @L2 [+5.0])\n";
        
        String result = agent.getInterpreter().eval("smem --print");

        Assert.assertTrue("testSimpleNonCueBasedRetrieval_ActivationRecency: Invalid Activation Values", result.equals(expected));
    
        halted = false;
    }
    
    public void testSimpleNonCueBasedRetrieval_ActivationRecency_WithoutActivateOnQuery() throws Exception
    {
        runTestSetup("testSimpleNonCueBasedRetrieval_ActivationRecency_WithoutActivateOnQuery");
        
        final RhsFunctionHandler oldHalt = agent.getRhsFunctions().getHandler("halt");
        Assert.assertNotNull(oldHalt);
        
        agent.getRhsFunctions().registerHandler(new AbstractRhsFunctionHandler("halt") {

            @Override
            public Symbol execute(RhsFunctionContext rhsContext, List<Symbol> arguments) throws RhsFunctionException
            {
                halted = true;
                return oldHalt.execute(rhsContext, arguments);
            }
        });
        
        agent.runFor(3, RunType.DECISIONS);

        Assert.assertTrue("testSimpleNonCueBasedRetrieval_ActivationRecency_WithoutActivateOnQuery functional test did not halt", halted);

        String expected = "========================================\n" +
                          "            Semantic Memory             \n" +         
                          "========================================\n" +
                          "(@L1 ^x 1 ^y 2 ^z 3 [+2.0])\n" +
                          "(@L2 ^x 2 ^y 3 ^z 1 [+5.0])\n" +
                          "(@X1 ^name |foo| ^location @L1 [+1.0])\n" +
                          "(@X2 ^name |foo| ^location @L2 [+3.0])\n";
        
        String result = agent.getInterpreter().eval("smem --print");

        Assert.assertTrue("testSimpleNonCueBasedRetrieval_ActivationRecency_WithoutActivateOnQuery: Invalid Activation Values", result.equals(expected));
    
        halted = false;
    }
    
    public void testSimpleNonCueBasedRetrieval_ActivationFrequency() throws Exception
    {
        runTestSetup("testSimpleNonCueBasedRetrieval_ActivationFrequency");
        
        final RhsFunctionHandler oldHalt = agent.getRhsFunctions().getHandler("halt");
        Assert.assertNotNull(oldHalt);
        
        agent.getRhsFunctions().registerHandler(new AbstractRhsFunctionHandler("halt") {

            @Override
            public Symbol execute(RhsFunctionContext rhsContext, List<Symbol> arguments) throws RhsFunctionException
            {
                halted = true;
                return oldHalt.execute(rhsContext, arguments);
            }
        });
        
        agent.runFor(3, RunType.DECISIONS);

        Assert.assertTrue("testSimpleNonCueBasedRetrieval_ActivationFrequency functional test did not halt", halted);
        
        String expected = "========================================\n" +
                          "            Semantic Memory             \n" +         
                          "========================================\n" +
                          "(@L1 ^x 1 ^y 2 ^z 3 [+1.0])\n" +
                          "(@L2 ^x 2 ^y 3 ^z 1 [+2.0])\n" +
                          "(@X1 ^name |foo| ^location @L1 [+1.0])\n" +
                          "(@X2 ^name |foo| ^location @L2 [+2.0])\n";
        
        String result = agent.getInterpreter().eval("smem --print");

        Assert.assertTrue("testSimpleNonCueBasedRetrieval_ActivationFrequency: Invalid Activation Values", result.equals(expected));
    
        halted = false;
    }
    
    private boolean checkActivationValues(String activationString, List<Double> lowEndExpectations, List<Double> highEndExpectations)
    {
        List<String> activationLevels = new ArrayList<String>();
        String activation = "";
        boolean inActivationParse = false;
        
        for (Character c : activationString.toCharArray())
        {
            if (c.equals('['))
            {
                inActivationParse = true;
                continue;
            }
            else if (c.equals(']') && inActivationParse)
            {
                inActivationParse = false;
                activationLevels.add(activation);
                
                activation = "";
                
                continue;
            }
            
            if (inActivationParse && (Character.isDigit(c) || c.equals('.') || c.equals('+') || c.equals('-')))
            {
                if (activation.length() != 0 &&
                    (c.equals('+') || c.equals('-')))
                {
                    throw new AssertionError("Found a +/- where there shouldn't be in Activation Levels!");
                }
                
                activation += c;
            }
            else if (inActivationParse)
            {
                throw new AssertionError("Non-Digit Character in Activation Level");
            }
        }
        
        if (activationLevels.size() != lowEndExpectations.size())
        {
            throw new AssertionError("Low End Expectations is not the same size as parsed Activation Levels!");
        }
        else if (activationLevels.size() != highEndExpectations.size())
        {
            throw new AssertionError("High End Expectations is not the same size as parsed Activation Levels!");
        }
        
        List<Double> activationLevelsAsDoubles = new ArrayList<Double>();
        
        for (String a : activationLevels)
        {
            activationLevelsAsDoubles.add(Double.parseDouble(a));
        }
        
        for (int i = 0;i < activationLevelsAsDoubles.size();i++)
        {
            Double a = activationLevelsAsDoubles.get(i);
            
            if (!(a >= lowEndExpectations.get(i) && a <= highEndExpectations.get(i)))
            {
                throw new AssertionError("Parsed Activation " + i+1 + " (" + a + ") is not within [" + lowEndExpectations.get(i) + ", " + highEndExpectations.get(i) + "]");
            }
        }
        
        return true;
    }
    
    public void testSimpleNonCueBasedRetrieval_ActivationBaseLevel_Stable() throws Exception
    {
        runTestSetup("testSimpleNonCueBasedRetrieval_ActivationBaseLevel_Stable");
        
        final RhsFunctionHandler oldHalt = agent.getRhsFunctions().getHandler("halt");
        Assert.assertNotNull(oldHalt);
        
        agent.getRhsFunctions().registerHandler(new AbstractRhsFunctionHandler("halt") {

            @Override
            public Symbol execute(RhsFunctionContext rhsContext, List<Symbol> arguments) throws RhsFunctionException
            {
                halted = true;
                return oldHalt.execute(rhsContext, arguments);
            }
        });
        
        agent.runFor(3, RunType.DECISIONS);

        Assert.assertTrue("testSimpleNonCueBasedRetrieval_ActivationBaseLevel_Stable functional test did not halt", halted);
        
        List<Double> lowEndExpectations = new ArrayList<Double>();
        List<Double> highEndExpectations = new ArrayList<Double>();
        
        lowEndExpectations.add(0.0);
        highEndExpectations.add(0.0);
        
        lowEndExpectations.add(0.455);
        highEndExpectations.add(0.456);
        
        lowEndExpectations.add(0.0);
        highEndExpectations.add(0.0);
        
        lowEndExpectations.add(0.455);
        highEndExpectations.add(0.456);
        
        // This is the expected output from smem --print modified from CSoar to look like JSoar outputs it (reverse string attributes)
        @SuppressWarnings("unused")
        String expected = "========================================\n" +
                          "            Semantic Memory             \n" +         
                          "========================================\n" +
                          "(@L1 ^x 1 ^y 2 ^z 3 [+0.0])\n" +
                          "(@L2 ^x 2 ^y 3 ^z 1 [+0.456])\n" +
                          "(@X1 ^name |foo| ^location @L1 [+0.0])\n" +
                          "(@X2 ^name |foo| ^location @L2 [+0.456])\n";
        
        String result = agent.getInterpreter().eval("smem --print");

        Assert.assertTrue("testSimpleNonCueBasedRetrieval_ActivationBaseLevel_Stable: Invalid Activation Values", checkActivationValues(result, lowEndExpectations, highEndExpectations));
    
        halted = false;
    }
    
    public void testSimpleNonCueBasedRetrieval_ActivationBaseLevel_Naive() throws Exception
    {
        runTestSetup("testSimpleNonCueBasedRetrieval_ActivationBaseLevel_Naive");
        
        final RhsFunctionHandler oldHalt = agent.getRhsFunctions().getHandler("halt");
        Assert.assertNotNull(oldHalt);
        
        agent.getRhsFunctions().registerHandler(new AbstractRhsFunctionHandler("halt") {

            @Override
            public Symbol execute(RhsFunctionContext rhsContext, List<Symbol> arguments) throws RhsFunctionException
            {
                halted = true;
                return oldHalt.execute(rhsContext, arguments);
            }
        });
        
        agent.runFor(3, RunType.DECISIONS);

        Assert.assertTrue("testSimpleNonCueBasedRetrieval_ActivationBaseLevel_Naive functional test did not halt", halted);
        
        List<Double> lowEndExpectations = new ArrayList<Double>();
        List<Double> highEndExpectations = new ArrayList<Double>();
        
        lowEndExpectations.add(0.0);
        highEndExpectations.add(0.0);
        
        lowEndExpectations.add(0.455);
        highEndExpectations.add(0.456);
        
        lowEndExpectations.add(-0.694);
        highEndExpectations.add(-0.693);
        
        lowEndExpectations.add(0.455);
        highEndExpectations.add(0.456);
        
        // This is the expected output from smem --print modified from CSoar to look like JSoar outputs it (reverse string attributes)
        @SuppressWarnings("unused")
        String expected = "========================================\n" +
                          "            Semantic Memory             \n" +         
                          "========================================\n" +
                          "(@L1 ^x 1 ^y 2 ^z 3 [+0.0])\n" +
                          "(@L2 ^x 2 ^y 3 ^z 1 [+0.456])\n" +
                          "(@X1 ^name |foo| ^location @L1 [-0.693])\n" +
                          "(@X2 ^name |foo| ^location @L2 [+0.456])\n";
        
        String result = agent.getInterpreter().eval("smem --print");

        Assert.assertTrue("testSimpleNonCueBasedRetrieval_ActivationBaseLevel_Naive: Invalid Activation Values", checkActivationValues(result, lowEndExpectations, highEndExpectations));
    
        halted = false;
    }
    
    public void testSimpleNonCueBasedRetrieval_ActivationBaseLevel_Incremental() throws Exception
    {
        runTestSetup("testSimpleNonCueBasedRetrieval_ActivationBaseLevel_Incremental");
        
        final RhsFunctionHandler oldHalt = agent.getRhsFunctions().getHandler("halt");
        Assert.assertNotNull(oldHalt);
        
        agent.getRhsFunctions().registerHandler(new AbstractRhsFunctionHandler("halt") {

            @Override
            public Symbol execute(RhsFunctionContext rhsContext, List<Symbol> arguments) throws RhsFunctionException
            {
                halted = true;
                return oldHalt.execute(rhsContext, arguments);
            }
        });
        
        agent.runFor(4, RunType.DECISIONS);

        Assert.assertTrue("testSimpleNonCueBasedRetrieval_ActivationBaseLevel_Incremental functional test did not halt", halted);
        
        List<Double> lowEndExpectations = new ArrayList<Double>();
        List<Double> highEndExpectations = new ArrayList<Double>();
        
        lowEndExpectations.add(-0.347);
        highEndExpectations.add(-0.346);
        
        lowEndExpectations.add(0.405);
        highEndExpectations.add(0.406);
        
        lowEndExpectations.add(0.109);
        highEndExpectations.add(0.110);
        
        lowEndExpectations.add(0.143);
        highEndExpectations.add(0.144);
        
        // This is the expected output from smem --print modified from CSoar to look like JSoar outputs it (reverse string attributes)
        @SuppressWarnings("unused")
        String expected = "========================================\n" +
                          "            Semantic Memory             \n" +         
                          "========================================\n" +
                          "(@L1 ^x 1 ^y 2 ^z 3 [-0.347])\n" +
                          "(@L2 ^x 2 ^y 3 ^z 1 [+0.405])\n" +
                          "(@X1 ^name |foo| ^location @L1 [+0.109])\n" +
                          "(@X2 ^name |foo| ^location @L2 [+0.144])\n";
        
        String result = agent.getInterpreter().eval("smem --print");

        Assert.assertTrue("testSimpleNonCueBasedRetrieval_ActivationBaseLevel_Incremental: Invalid Activation Values", checkActivationValues(result, lowEndExpectations, highEndExpectations));
    
        halted = false;
    }
    
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

        Assert.assertTrue("Didn't stop where expected!", resultOfPS1.equals(expectedResultOfPS1));
        
        agent.getInterpreter().eval("smem --backup backup.sqlite");
        agent.getInterpreter().eval("smem --init");
        outputWriter.getBuffer().setLength(0);
        try
        {
            agent.getInterpreter().eval("smem --print");
        }
        catch (SoarException e)
        {
            Assert.assertTrue("smem --init didn't init smem!", e.getMessage().equals("SMem| Semantic memory is empty."));
        }
        
        agent.getInterpreter().eval("p");
        
        String resultOfP = outputWriter.toString();
        outputWriter.getBuffer().setLength(0);

        Assert.assertTrue("smem --init didn't excise all productions!", resultOfP.length() == 0);
        
        agent.getInterpreter().eval("p s1");
        
        resultOfPS1 = outputWriter.toString();
        outputWriter.getBuffer().setLength(0);
        
        expectedResultOfPS1 = "(S1 ^epmem E1 ^io I1 ^reward-link R1 ^smem S2 ^superstate nil ^type state)\n";

        Assert.assertTrue("smem --init didn't reinit WM!", resultOfPS1.equals(expectedResultOfPS1));
        
        agent.getInterpreter().eval("smem --set path backup.sqlite");
        agent.getInterpreter().eval("smem --set append-database on");
        agent.getInterpreter().eval("smem --init");
        
        runTestSetup("testFactorization");
        
        final RhsFunctionHandler oldHalt = agent.getRhsFunctions().getHandler("halt");
        Assert.assertNotNull(oldHalt);
        
        agent.getRhsFunctions().registerHandler(new AbstractRhsFunctionHandler("halt") {

            @Override
            public Symbol execute(RhsFunctionContext rhsContext, List<Symbol> arguments) throws RhsFunctionException
            {
                halted = true;
                return oldHalt.execute(rhsContext, arguments);
            }
        });
        
        agent.runFor(2811 + 1, RunType.DECISIONS);

        Assert.assertTrue("testFactorization: Test did not halt.", halted);
        
        outputWriter.getBuffer().setLength(0);
        
        agent.getInterpreter().eval("p -d 2 @F197");
        
        String expectedResultOfPD2F197 = "\n(@F197 ^complete true ^factor @F48 ^factor @F198 ^number 100)\n" +
                                         "  (@F48 ^multiplicity 2 ^value 5)\n" +
                                         "  (@F198 ^multiplicity 2 ^value 2)\n";
        
        String resultOfPD2F197 = outputWriter.toString();

        Assert.assertTrue("testFactorization: Test did not get the correct result!", expectedResultOfPD2F197.equals(resultOfPD2F197));
    
        agent.dispose();
        
        String pwd = agent.getInterpreter().eval("pwd");
        pwd = pwd.replaceAll("\\s+", "/");
        File backupDB = new File(pwd + "/backup.sqlite");
        backupDB.delete();
    }
    
    public void readCSoarDB() throws Exception
    {
        agent.initialize();
        
        URL db = getClass().getResource("smem-csoar-db.sqlite");
        Assert.assertNotNull("No CSoar db!", db);
        agent.getInterpreter().eval("smem --set path " + db.getPath());
        agent.getInterpreter().eval("smem --set append-database on");
        agent.getInterpreter().eval("smem --init");
        
        String actualResult = agent.getInterpreter().eval("smem --print");
        
        String expectedResult = "========================================\n" +
                                "            Semantic Memory             \n" +
                                "========================================\n" +
                                "(@F1 ^number 2 ^complete |true| ^factor @F2 [+5.0])\n" +
                                "(@F2 ^value 2 ^multiplicity 1 [+6.0])\n" +
                                "(@F3 ^number 3 ^complete |true| ^factor @F4 [+3.0])\n" +
                                "(@F4 ^value 3 ^multiplicity 1 [+4.0])\n" +
                                "(@F5 ^number 4 ^complete |true| ^factor @F6 [+7.0])\n" +
                                "(@F6 ^value 2 ^multiplicity 2 [+8.0])\n";

        Assert.assertTrue("Unexpected output from CSoar database!", actualResult.equals(expectedResult));
    }
    
    public void testSimpleStoreGC() throws Exception
    {
        agent.setStopPhase(Phase.OUTPUT);
        runTestSetup("testSimpleStoreGC");
        
        agent.runFor(2, RunType.DECISIONS);
        
        String result = agent.getInterpreter().eval("smem --print");
        
        String[] split = result.split("\\s+");
        
        List<String> ltis = new ArrayList<String>();
        
        for (String lti : split)
        {
            if (lti.length() == 3 ||
                lti.length() == 4)
            {
                if (lti.charAt(0) == '@')
                {
                    ltis.add(lti);
                }
                else if (lti.charAt(0) == '(' &&
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
        
        for (String lti : split_after)
        {
            if ((lti.length() == 3 || lti.length() == 4) && lti.charAt(0) == '@')
            {
                String correctLti = lti.substring(0, 3);
                Assert.assertTrue(ltis.contains(correctLti));
            }
        }
    }
    
    @Ignore("db driver is now always native, so no longer specifies 'native' in version number, so the test fails when it shouldn't")
    public void testMultiAgent() throws Exception
    {
        List<ThreadedAgent> agents = new ArrayList<ThreadedAgent>();
        
        for (int i = 1;i <= 250;i++)
        {
            ThreadedAgent t = ThreadedAgent.create("Agent " + i, InstrumentationRegistry.getTargetContext());
            t.getAgent().getTrace().setEnabled(true);
            String sourceName = getClass().getSimpleName() + "_testMultiAgent.soar";
            URL sourceUrl = getClass().getResource(sourceName);
            Assert.assertNotNull("Could not find test file " + sourceName, sourceUrl);
            t.getAgent().getInterpreter().source(sourceUrl);
            
            agents.add(t);
        }
        
        for (ThreadedAgent a : agents)
        {
            a.runFor(4+1, RunType.DECISIONS);
        }
        
        boolean allStopped = false;
        while (!allStopped)
        {
            allStopped = true;
            
            for (ThreadedAgent a : agents)
            {
                if (a.isRunning())
                {
                    allStopped = false;
                    break;
                }
            }
        }
        
        for (ThreadedAgent a : agents)
        {
            if (a.getAgent().getProperties().get(SoarProperties.DECISION_PHASES_COUNT).intValue() != 4)
            {
                throw new AssertionError("Agent did not stop correctly! Ran too many cycles!");
            }
            
            String result = a.getAgent().getInterpreter().eval("smem");
            
            if (!result.contains("native"))
            {
                throw new AssertionError("Non Native Driver!");
            }
        }
    }
}