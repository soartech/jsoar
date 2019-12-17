package org.jsoar.kernel;

import static org.junit.Assert.assertTrue;

import java.io.StringWriter;

import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


public class SMemEpMemCombinedFunctionalTests extends FunctionalTestHarness
{
	private static final Logger logger = LoggerFactory.getLogger(SMemEpMemCombinedFunctionalTests.class);
	
    @Test
    public void smemEpMemFactorizationCombinationTest() throws Exception
    {
        runTestSetup("testSMemEpMemFactorization");
        
        agent.runFor(100, RunType.DECISIONS);
        
        StringWriter sw = new StringWriter();
        agent.getPrinter().pushWriter(sw);
        agent.getInterpreter().eval("smem --print");
        agent.getPrinter().popWriter();
        String actualResultSMem = sw.toString();
        
        String expectedResultSMem = "========================================\n" +
                                    "            Semantic Memory             \n" +
                                    "========================================\n" +
                                    "(@F4 ^complete |true| ^number 3 ^factor @F5 [+5.0])\n" +
                                    "(@F5 ^value 3 ^multiplicity 1 [+6.0])\n" +
                                    "(@F12 ^complete |true| ^number 5 ^factor @F13 [+3.0])\n" +
                                    "(@F13 ^value 5 ^multiplicity 1 [+4.0])\n" +
                                    "(@F17 ^complete |true| ^number 7 ^factor @F18 [+7.0])\n" +
                                    "(@F18 ^value 7 ^multiplicity 1 [+8.0])\n";
                
        assertTrue("Unexpected output from SMem!\n" + actualResultSMem, actualResultSMem.equals(expectedResultSMem));
        
        StringWriter sw2 = new StringWriter();
        agent.getPrinter().pushWriter(sw2);
        agent.getInterpreter().eval("epmem --print 97");
        agent.getPrinter().popWriter();
        String actualResultEpMem = sw2.toString();
        
        String expectedResultEpMem = "(<id0> ^counter 7 ^factorization-object @F17 ^has-factorization-object true ^has-factorization-object-complete true ^io <id1> ^name Factorization ^needs-factorization true ^number-to-factor 7 ^number-to-factor-int 7 ^operator* <id3> <id7> ^reward-link <id2> ^superstate nil ^type state)\n" +
                                     "(<id1> ^input-link <id5> ^output-link <id4>)\n" +
                                     "(<id3> ^name factor-number ^number-to-factor 7)\n" +
                                     "(<id7> ^factorization-object @F17 ^name check)\n" +
                                     "(@F17 ^complete true ^factor @F18 ^number 7)\n" +
                                     "(@F18 ^multiplicity 1 ^value 7)\n";
        
        logger.info("Epmem test actual result: " + actualResultEpMem);
        assertTrue("Unexpected output from EpMem!\n" + actualResultEpMem, actualResultEpMem.equals(expectedResultEpMem));
    }
}
