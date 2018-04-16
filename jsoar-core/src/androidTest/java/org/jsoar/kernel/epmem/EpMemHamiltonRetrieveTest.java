package org.jsoar.kernel.epmem;

import junit.framework.Assert;

import org.jsoar.kernel.FunctionalTestHarness;
import org.jsoar.kernel.Phase;
import org.jsoar.kernel.RunType;
import org.jsoar.kernel.SoarProperties;
import org.jsoar.util.adaptables.Adaptables;

import java.sql.Connection;
import java.sql.SQLException;

public class EpMemHamiltonRetrieveTest extends FunctionalTestHarness
{
    protected Connection getConnection()
    {
        final DefaultEpisodicMemory epmem = Adaptables.adapt(agent, DefaultEpisodicMemory.class);
        final EpisodicMemoryDatabase db = epmem.db;
        
        return db.getConnection();
    }
    
    private void populateDataBase(Connection db) throws SQLException{
        db.prepareStatement("DELETE FROM " + EpisodicMemoryDatabase.EPMEM_SCHEMA + "ascii");
        db.prepareStatement(
                "insert into " + EpisodicMemoryDatabase.EPMEM_SCHEMA + "ascii " +
                		"select 65 as ascii_num, 'A' as ascii_chr " +
                		"union select 66, 'B' " +
                		"union select 67, 'C' " +
                		"union select 68, 'D' " +
                		"union select 69, 'E' " +
                		"union select 70, 'F' " +
                		"union select 71, 'G' " +
                		"union select 72, 'H' " +
                		"union select 73, 'I' " +
                		"union select 74, 'J' " +
                		"union select 75, 'K' " +
                		"union select 76, 'L' " +
                		"union select 77, 'M' " +
                		"union select 78, 'N' " +
                		"union select 79, 'O' " +
                		"union select 80, 'P' " +
                		"union select 81, 'Q' " +
                		"union select 82, 'R' " +
                		"union select 83, 'S' " +
                		"union select 84, 'T' " +
                		"union select 85, 'U' " +
                		"union select 86, 'V' " +
                		"union select 87, 'W' " +
                		"union select 88, 'X' " +
                		"union select 89, 'Y' " +
                		"union select 90, 'Z'"
            );
        db.prepareStatement("DELETE FROM " + EpisodicMemoryDatabase.EPMEM_SCHEMA + "wmes_identifier_now");
        db.prepareStatement(
                "insert into " + EpisodicMemoryDatabase.EPMEM_SCHEMA + "wmes_identifier_now " +
                		"select 1 as wi_id, 3 as start_episode_id " +
                        "union select 2,3 " +
                        "union select 3,3 " +
                        "union select 4,3 " +
                        "union select 5,3 " +
                        "union select 6,3 " +
                        "union select 13,3 " +
                        "union select 12,3 " +
                        "union select 11,3 " +
                        "union select 10,3 " +
                        "union select 9,3 " +
                        "union select 8,3 " +
                        "union select 7,3 " +
                        "union select 27,3 " +
                        "union select 28,3 " +
                        "union select 29,3 " +
                        "union select 30,3 " +
                        "union select 25,3 " +
                        "union select 31,3 " +
                        "union select 23,3 " +
                        "union select 32,3 " +
                        "union select 33,3 " +
                        "union select 20,3 " +
                        "union select 34,3 " +
                        "union select 18,3 " +
                        "union select 35,3"
            );
        db.prepareStatement("DELETE FROM " + EpisodicMemoryDatabase.EPMEM_SCHEMA + "wmes_identifier_point");
        db.prepareStatement(
                "insert into " + EpisodicMemoryDatabase.EPMEM_SCHEMA + "wmes_identifier_point " +
                		"select 1 as wi_id, 1 as episode_id " +
                		"union select 2,1 " +
                		"union select 3,1 " +
                		"union select 4,1 " +
                		"union select 5,1 " +
                		"union select 6,1 " +
                		"union select 7,1 " +
                		"union select 8,1 " +
                		"union select 9,1 " +
                		"union select 10,1 " +
                		"union select 11,1 " +
                		"union select 12,1 " +
                		"union select 13,1 " +
                		"union select 14,1 " +
                		"union select 15,1 " +
                		"union select 16,1 " +
                		"union select 17,1 " +
                		"union select 18,1 " +
                		"union select 19,1 " +
                		"union select 20,1 " +
                		"union select 21,1 " +
                		"union select 22,1 " +
                		"union select 23,1 " +
                		"union select 24,1 " +
                		"union select 25,1 " +
                		"union select 26,1 " +
                		"union select 1,2 " +
                		"union select 2,2 " +
                        "union select 3,2 " +
                        "union select 4,2 " +
                        "union select 5,2 " +
                        "union select 6,2 " +
                        "union select 13,2 " +
                        "union select 12,2 " +
                        "union select 11,2 " +
                        "union select 10,2 " +
                        "union select 9,2 " +
                        "union select 8,2 " +
                        "union select 7,2 " + 
                        "union select 27,2 " +
                        "union select 28,2 " +
                        "union select 29,2 " +
                        "union select 30,2 " +
                        "union select 25,2 " +
                        "union select 31,2 " +
                        "union select 23,2 " +
                        "union select 32,2 " +
                        "union select 33,2 " +
                        "union select 20,2 " +
                        "union select 34,2 " +
                        "union select 18,2 " +
                        "union select 35,2 " +
                        "union select 1,3 " +
                        "union select 2,3 " +
                        "union select 3,3 " +
                        "union select 4,3 " +
                        "union select 5,3 " +
                        "union select 6,3 " +
                        "union select 13,3 " +
                        "union select 12,3 " +
                        "union select 11,3 " +
                        "union select 10,3 " +
                        "union select 9,3 " +
                        "union select 8,3 " +
                        "union select 7,3 " +
                        "union select 27,3 " +
                        "union select 28,3 " +
                        "union select 29,3 " +
                        "union select 30,3 " +
                        "union select 25,3 " +
                        "union select 31,3 " +
                        "union select 23,3 " +
                        "union select 32,3 " +
                        "union select 33,3 " +
                        "union select 20,3 " +
                        "union select 34,3 " +
                        "union select 18,3 " +
                        "union select 35,3 "
            );
        db.prepareStatement("DELETE FROM " + EpisodicMemoryDatabase.EPMEM_SCHEMA + "wmes_identifier");
        db.prepareStatement(
                "insert into " + EpisodicMemoryDatabase.EPMEM_SCHEMA + "wmes_identifier " +
                		"select 1 as wi_id, 0 as parent_n_id, 6 as attribute_s_id, 1 as child_n_id, 9223372036854776000 as last_episode_id " +
                        "union select 2,0,9,2,9223372036854776000 " +
                        "union select 3,0,1,3,9223372036854776000 " +
                        "union select 4,0,10,4,9223372036854776000 " +
                        "union select 5,1,11,5,9223372036854776000 " +
                        "union select 6,1,12,6,9223372036854776000 " +
                        "union select 7,2,13,7,9223372036854776000 " +
                        "union select 8,2,13,8,9223372036854776000 " +
                        "union select 9,2,13,9,9223372036854776000 " +
                        "union select 10,2,13,10,9223372036854776000 " +
                        "union select 11,2,13,11,9223372036854776000 " +
                        "union select 12,2,13,12,9223372036854776000 " +
                        "union select 13,2,13,13,9223372036854776000 " +
                        "union select 14,7,15,8,1 " +
                        "union select 15,7,15,11,1 " +
                        "union select 16,7,15,13,1 " +
                        "union select 17,8,15,9,1 " +
                        "union select 18,8,15,10,9223372036854776000 " +
                        "union select 19,9,15,11,1 " +
                        "union select 20,9,15,12,9223372036854776000 " +
                        "union select 21,10,15,8,1 " +
                        "union select 22,10,15,11,1 " +
                        "union select 23,11,15,8,9223372036854776000 " +
                        "union select 24,11,15,10,1 " +
                        "union select 25,12,15,10,9223372036854776000 " +
                        "union select 26,12,15,13,1 " +
                        "union select 27,13,15,12,9223372036854776000 " +
                        "union select 28,13,15,9,9223372036854776000 " +
                        "union select 29,13,15,7,9223372036854776000 " +
                        "union select 30,12,15,11,9223372036854776000 " +
                        "union select 31,11,15,9,9223372036854776000 " +
                        "union select 32,10,15,12,9223372036854776000 " +
                        "union select 33,10,15,9,9223372036854776000 " +
                        "union select 34,9,15,10,9223372036854776000 " +
                        "union select 35,8,15,7,9223372036854776000"
                );
        db.prepareStatement("DELETE FROM " + EpisodicMemoryDatabase.EPMEM_SCHEMA + "wmes_constant_now");
        db.prepareStatement(
                "insert into " + EpisodicMemoryDatabase.EPMEM_SCHEMA + "wmes_constant_now " +
                		"select 1 as wc_id, 4 as start_episode_id " +
                        "union select 2,4 " +
                        "union select 3,4 " +
                        "union select 4,4 " +
                        "union select 12,4 " +
                        "union select 13,4 " +
                        "union select 14,4 " +
                        "union select 8,4 " +
                        "union select 15,4 " +
                        "union select 16,4 " +
                        "union select 17,4"
                    );
        db.prepareStatement("DELETE FROM " + EpisodicMemoryDatabase.EPMEM_SCHEMA + "wmes_constant_point");
        db.prepareStatement(
                "insert into " + EpisodicMemoryDatabase.EPMEM_SCHEMA + "wmes_constant_point " +
                		"select 1 as wc_id, 1 as episode_id " +
                		"union select 2,1 " +
                		"union select 3,1 " +
                		"union select 4,1 " +
                		"union select 5,1 " +
                		"union select 6,1 " +
                		"union select 7,1 " +
                		"union select 8,1 " +
                		"union select 9,1 " +
                		"union select 10,1 " +
                		"union select 11,1 " +
                		"union select 1,2 " +
                        "union select 2,2 " +
                        "union select 3,2 " +
                        "union select 4,2 " +
                        "union select 12,2 " +
                        "union select 13,2 " +
                        "union select 14,2 " +
                        "union select 8,2 " +
                        "union select 15,2 " +
                        "union select 16,2 " +
                        "union select 17,2 " +
                        "union select 1,3 " +
                        "union select 2,3 " +
                        "union select 3,3 " +
                        "union select 4,3 " +
                        "union select 12,3 " +
                        "union select 13,3 " +
                        "union select 14,3 " +
                        "union select 8,3 " +
                        "union select 15,3 " +
                        "union select 16,3 " +
                        "union select 17,3"
                );
        db.prepareStatement("DELETE FROM " + EpisodicMemoryDatabase.EPMEM_SCHEMA + "wmes_constant");
        db.prepareStatement(
                "insert into " + EpisodicMemoryDatabase.EPMEM_SCHEMA + "wmes_constant " +
                		"select 1 as wc_id, 0 as parent_n_id, 2 as attribute_s_id, 3 as value_s_id " +
                        "union select 2,0,4,5 " +
                        "union select 3,0,7,8 " +
                        "union select 4,3,7,14 " +
                        "union select 5,7,7,16 " +
                        "union select 17,7,7,22 " +
                        "union select 6,8,7,17 " +
                        "union select 16,8,7,21 " +
                        "union select 7,9,7,18 " +
                        "union select 15,9,7,20 " +
                        "union select 8,10,7,19 " +
                        "union select 14,11,7,18 " +
                        "union select 9,11,7,20 " +
                        "union select 13,12,7,17 " +
                        "union select 10,12,7,21 " +
                        "union select 12,13,7,16 " +
                        "union select 11,13,7,22"
                    );
        db.prepareStatement("DELETE FROM " + EpisodicMemoryDatabase.EPMEM_SCHEMA + "symbols_string");
        db.prepareStatement(
                "insert into " + EpisodicMemoryDatabase.EPMEM_SCHEMA + "symbols_string " +
                		"select 0 as s_id, \"root\" as symbol_value " +
                        "union select 20,\"Albany\" " +
                        "union select 21,\"Atlanta\" " +
                        "union select 22,\"Boston\" " +
                        "union select 19,\"Dallas\" " +
                        "union select 16,\"Fresno\" " +
                        "union select 18,\"Omaha\" " +
                        "union select 17,\"Seattle\" " +
                        "union select 13,\"city\" " +
                        "union select 8,\"graph-match-unit\" " +
                        "union select 14,\"halt\" " +
                        "union select 9,\"hamilton\" " +
                        "union select 12,\"input-link\" " +
                        "union select 6,\"io\" " +
                        "union select 7,\"name\" " +
                        "union select 3,\"nil\" " +
                        "union select 1,\"operator*\" " +
                        "union select 11,\"output-link\" " +
                        "union select 10,\"reward-link\" " +
                        "union select 5,\"state\" " +
                        "union select 2,\"superstate\" " +
                        "union select 15,\"to\" " +
                        "union select 4,\"type\""
                    );
        db.prepareStatement("DELETE FROM " + EpisodicMemoryDatabase.EPMEM_SCHEMA + "episodes");
        db.prepareStatement(
                "insert into " + EpisodicMemoryDatabase.EPMEM_SCHEMA + "episodes " +
                		"select 1 as episode_id " +
                        "union select 2 " +
                        "union select 3 " +
                        "union select 4"
                    );
        db.prepareStatement("DELETE FROM " + EpisodicMemoryDatabase.EPMEM_SCHEMA + "persistent_variables");
        db.prepareStatement(
                "insert into " + EpisodicMemoryDatabase.EPMEM_SCHEMA + "persistent_variables " +
                		"select 0 as variable_id, -1 as variable_value " +
                        "union select 1,0 " +
                        "union select 2,1 " +
                        "union select 3,2147483647 " +
                        "union select 4,-1 " +
                        "union select 5,0 " +
                        "union select 6,1 " +
                        "union select 7,2147483647 " +
                        "union select 8,14"
                    );
        /*
         * We shouldn't need this table unless we are storing.
        db.prepareStatement("DELETE FROM " + EpisodicMemoryDatabase.EPMEM_SCHEMA + "sqlite_sequence");
        db.prepareStatement(
                "insert into " + EpisodicMemoryDatabase.EPMEM_SCHEMA + "sqlite_sequence " +
                        "select 1 as rowid, \"node_unique\" as name, 17 as seq) " +
                        "union select 2, \"edge_unique\", 35"
                );
        */
    }
    
    public void testRetrieval() throws Exception{
        // Since this is the retrieve tests, these tests have to stop after output
        // (ie. before INPUT). I changed this to Phase.APPLY so this broke all the tests.
        // - ALT
        agent.setStopPhase(Phase.INPUT);
        
        runTestSetup("testHamilton_retrieve");
        agent.runFor(1, RunType.DECISIONS);
        populateDataBase(getConnection());
        agent.runFor(2, RunType.DECISIONS);

		Assert.assertTrue("Retrieval test did not halt", halted);
		Assert.assertFalse("Retrieval test failed", failed);
		Assert.assertEquals(3, agent.getProperties().get(SoarProperties.D_CYCLE_COUNT).intValue()); // deterministic!
    }
}
