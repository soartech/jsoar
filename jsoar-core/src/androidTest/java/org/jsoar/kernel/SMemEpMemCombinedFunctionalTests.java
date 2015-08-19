package org.jsoar.kernel;

public class SMemEpMemCombinedFunctionalTests extends FunctionalTestHarness
{
    public void smemEpMemFactorizationCombinationTest() throws Exception
    {
        runTestSetup("testSMemEpMemFactorization");
        
        agent.runFor(100, RunType.DECISIONS);
        
        String actualResultSMem = agent.getInterpreter().eval("smem --print");
        
        String expectedResultSMem = "========================================\n" +
                                    "            Semantic Memory             \n" +
                                    "========================================\n" +
                                    "(@F4 ^complete |true| ^number 3 ^factor @F5 [+5.0])\n" +
                                    "(@F5 ^value 3 ^multiplicity 1 [+6.0])\n" +
                                    "(@F12 ^complete |true| ^number 5 ^factor @F13 [+3.0])\n" +
                                    "(@F13 ^value 5 ^multiplicity 1 [+4.0])\n" +
                                    "(@F17 ^complete |true| ^number 7 ^factor @F18 [+7.0])\n" +
                                    "(@F18 ^value 7 ^multiplicity 1 [+8.0])\n";
                
        assertTrue("Unexpected output from SMem!", actualResultSMem.equals(expectedResultSMem));
        
        String actualResultEpMem = agent.getInterpreter().eval("epmem --print 97");
        
        String expectedResultEpMem = "(<id0> ^counter 7 ^factorization-object @F17 ^has-factorization-object true ^has-factorization-object-complete true ^io <id1> ^name Factorization ^needs-factorization true ^number-to-factor 7 ^number-to-factor-int 7 ^operator* <id3> <id7> ^reward-link <id2> ^superstate nil ^type state)\n" +
                                     "(<id1> ^input-link <id5> ^output-link <id4>)\n" +
                                     "(<id3> ^name factor-number ^number-to-factor 7)\n" +
                                     "(<id7> ^factorization-object @F17 ^name check)\n" +
                                     "(@F17 ^complete true ^factor @F18 ^number 7)\n" +
                                     "(@F18 ^multiplicity 1 ^value 7)\n";
        
        assertTrue("Unexpected output from EpMem!", actualResultEpMem.equals(expectedResultEpMem));
    }
}
