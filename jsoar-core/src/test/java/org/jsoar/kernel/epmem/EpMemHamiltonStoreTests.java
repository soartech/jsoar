package org.jsoar.kernel.epmem;

import static org.junit.Assert.*;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.jsoar.kernel.FunctionalTestHarness;
import org.jsoar.util.adaptables.Adaptables;
import org.junit.Test;

public class EpMemHamiltonStoreTests extends FunctionalTestHarness
{   
    protected Connection getConnection()
    {
        final DefaultEpisodicMemory epmem = Adaptables.adapt(agent, DefaultEpisodicMemory.class);
        final EpisodicMemoryDatabase db = epmem.db;
        
        return db.getConnection();
    }
    
    @Test
    public void testWMEsIdentifierNowTable() throws Exception
    {
        runTest("testHamilton_store", 2);
        
        /*
         * id      start
         * 1-26    1
         */
        final List<Long> expectedRowIds = new ArrayList<Long>();
        for(long i = 1; i <= 26; i++){
            expectedRowIds.add(i);
        }
        
        final PreparedStatement p = getConnection().prepareStatement(
                "select * from " + EpisodicMemoryDatabase.EPMEM_SCHEMA + "epmem_wmes_identifier_now");
        
        final ResultSet results = p.executeQuery();
        
        while(results.next()){
            final long id = results.getLong("wi_id");
            final long start = results.getLong("start_episode_id");
            assertTrue(
                    "edge_now contained unexpected " + id + ", " + start, 
                    expectedRowIds.contains(id) && start == 1
                );
            expectedRowIds.remove(id);
        }
        assertTrue(expectedRowIds.isEmpty());
    }
    
    @Test
    public void testWMEsConstantNowTable() throws Exception
    {
        runTest("testHamilton_store", 2);
        
        /*
         * id      start
         * 1-11    1
         */
        final List<Long> expectedRowIds = new ArrayList<Long>();
        for(long i = 1; i <= 11; i++){
            expectedRowIds.add(i);
        }
        
        final PreparedStatement p = getConnection().prepareStatement(
                "select * from " + EpisodicMemoryDatabase.EPMEM_SCHEMA + "epmem_wmes_constant_now");
        
        final ResultSet results = p.executeQuery();
        
        while(results.next()){
            final long id = results.getLong("wc_id");
            final long start = results.getLong("start_episode_id");
            assertTrue(
                    "node_now contained unexpected " + id + ", " + start, 
                    expectedRowIds.contains(id) && start == 1
                );
            expectedRowIds.remove(id);
        }
        assertTrue(
                "node_now did not contain expected id " + 
                (expectedRowIds.isEmpty()?"":expectedRowIds.get(0)) + 
                ", " + 1,
                expectedRowIds.isEmpty()
            );
    }
    
    private static class NodeUniqueRow{
        public final long wc_id;
        public final long parent_n_id;
        public final long attribute_s_id;
        public final long value_s_id;
        
        public NodeUniqueRow(
            long wc_id,
            long parent_n_id,
            long attribute_s_id,
            long value_s_id
        ){
            this.wc_id = wc_id;
            this.parent_n_id = parent_n_id;
            this.attribute_s_id = attribute_s_id;
            this.value_s_id = value_s_id;
        }
        
        public String toString(){
            return 
                "ChildID: " + wc_id + 
                " ParentID: " + parent_n_id + 
                " Attrib: " + attribute_s_id + 
                " Value: " + value_s_id;
        }
        
        @Override
        public int hashCode()
        {
            final int prime = 31;
            int result = 1;
            result = prime * result + (int) (attribute_s_id ^ (attribute_s_id >>> 32));
            result = prime * result + (int) (wc_id ^ (wc_id >>> 32));
            result = prime * result + (int) (parent_n_id ^ (parent_n_id >>> 32));
            result = prime * result + (int) (value_s_id ^ (value_s_id >>> 32));
            return result;
        }

        @Override
        public boolean equals(Object obj)
        {
            if (this == obj)
                return true;
            if (obj == null)
                return false;
            if (getClass() != obj.getClass())
                return false;
            NodeUniqueRow other = (NodeUniqueRow) obj;
            if (attribute_s_id != other.attribute_s_id)
                return false;
            if (wc_id != other.wc_id)
                return false;
            if (parent_n_id != other.parent_n_id)
                return false;
            if (value_s_id != other.value_s_id)
                return false;
            return true;
        }
    }
    
    @Test
    public void testWMEsConstantTable() throws Exception
    {
        runTest("testHamilton_store", 2);
        
        /*
         * id      start
         * 1-11    1
         */
        
        final Set<NodeUniqueRow> expectedRows = new HashSet<NodeUniqueRow>();
        expectedRows.add(new NodeUniqueRow(1,0,2,3));
        expectedRows.add(new NodeUniqueRow(2,0,4,5));
        expectedRows.add(new NodeUniqueRow(3,0,7,8));
        
        // CK: the second column is epmem id
        // - in JSoar R1 has epmem_id 3 and O3 has epmem_id 4
        // - in Csoar R1 has epmem_id 4 and O3 has epmem_id 3
        // so in JSoar we except a 4 instead of a 3
        expectedRows.add(new NodeUniqueRow(4,4,7,14));
        
        expectedRows.add(new NodeUniqueRow(5,7,7,16));
        expectedRows.add(new NodeUniqueRow(6,8,7,17));
        expectedRows.add(new NodeUniqueRow(7,9,7,18));
        expectedRows.add(new NodeUniqueRow(8,10,7,19));
        expectedRows.add(new NodeUniqueRow(9,11,7,20));
        expectedRows.add(new NodeUniqueRow(10,12,7,21));
        expectedRows.add(new NodeUniqueRow(11,13,7,22));
        
        final PreparedStatement p = getConnection().prepareStatement(
                "select * from " + EpisodicMemoryDatabase.EPMEM_SCHEMA + "epmem_wmes_constant");
        
        final ResultSet results = p.executeQuery();
        
        while(results.next()){
            final NodeUniqueRow row = 
                new NodeUniqueRow(
                        results.getLong("wc_id"),
                        results.getLong("parent_n_id"),
                        results.getLong("attribute_s_id"),
                        results.getLong("value_s_id")
                    );
            assertTrue(
                    "node_unique contained unexpected " + row, 
                    expectedRows.contains(row)
                );
            expectedRows.remove(row);
        }
        assertTrue(
                expectedRows.size() > 0 ? "node_unique did not contain expected row " + expectedRows.toArray()[0] : "",
                expectedRows.isEmpty()
            );
    }
    
    private static class SymbolsStringRow{
        public final long s_id;
        public final String symbol_value;
        
        public SymbolsStringRow(
            long s_id,
            String symbol_value
        ){
            this.s_id = s_id;
            this.symbol_value = symbol_value;
        }
        
        public String toString(){
            return 
                "S_ID: " + s_id + 
                " Symbol Value: " + symbol_value;
        }

        @Override
        public int hashCode()
        {
            final int prime = 31;
            int result = 1;
            result = prime * result + (int) (s_id ^ (s_id >>> 32));
            result = prime * result
                    + ((symbol_value == null) ? 0 : symbol_value.hashCode());
            return result;
        }

        @Override
        public boolean equals(Object obj)
        {
            if (this == obj)
                return true;
            if (obj == null)
                return false;
            if (getClass() != obj.getClass())
                return false;
            SymbolsStringRow other = (SymbolsStringRow) obj;
            if (s_id != other.s_id)
                return false;
            if (symbol_value == null)
            {
                if (other.symbol_value != null)
                    return false;
            }
            else if (!symbol_value.equals(other.symbol_value))
                return false;
            return true;
        }
    }
    
    @Test
    public void testSymbolsStringTable() throws Exception
    {
        runTest("testHamilton_store", 2);
        
        /*
         * id      start
         * 1-11    1
         */
        
        final Set<SymbolsStringRow> expectedRows = new HashSet<SymbolsStringRow>();
        expectedRows.add(new SymbolsStringRow(20,"Albany"));
        expectedRows.add(new SymbolsStringRow(21,"Atlanta"));
        expectedRows.add(new SymbolsStringRow(22,"Boston"));
        expectedRows.add(new SymbolsStringRow(19,"Dallas"));
        expectedRows.add(new SymbolsStringRow(16,"Fresno"));
        expectedRows.add(new SymbolsStringRow(18,"Omaha"));
        expectedRows.add(new SymbolsStringRow(17,"Seattle"));
        expectedRows.add(new SymbolsStringRow(13,"city"));
        expectedRows.add(new SymbolsStringRow(8,"graph-match-unit"));
        expectedRows.add(new SymbolsStringRow(14,"halt"));
        expectedRows.add(new SymbolsStringRow(9,"hamilton"));
        expectedRows.add(new SymbolsStringRow(12,"input-link"));
        expectedRows.add(new SymbolsStringRow(6,"io"));
        expectedRows.add(new SymbolsStringRow(7,"name"));
        expectedRows.add(new SymbolsStringRow(3,"nil"));
        expectedRows.add(new SymbolsStringRow(1,"operator*"));
        expectedRows.add(new SymbolsStringRow(11,"output-link"));
        expectedRows.add(new SymbolsStringRow(10,"reward-link"));
        expectedRows.add(new SymbolsStringRow(0, "root"));
        expectedRows.add(new SymbolsStringRow(5,"state"));
        expectedRows.add(new SymbolsStringRow(2,"superstate"));
        expectedRows.add(new SymbolsStringRow(15,"to"));
        expectedRows.add(new SymbolsStringRow(4,"type"));
        
        final PreparedStatement p = getConnection().prepareStatement(
                "select * from " + EpisodicMemoryDatabase.EPMEM_SCHEMA + "epmem_symbols_string");
        
        final ResultSet results = p.executeQuery();
        
        while(results.next()){
            final SymbolsStringRow row = 
                new SymbolsStringRow(
                        results.getLong("s_id"),
                        results.getString("symbol_value")
                    );
            assertTrue(
                    "epmem_symbols_string contained unexpected " + row, 
                    expectedRows.contains(row)
                );
            expectedRows.remove(row);
        }
        assertTrue(
                "epmem_symbols_string did not contain expected row " + 
                (expectedRows.isEmpty()?"":expectedRows.toArray()[0]),
                expectedRows.isEmpty()
            );
    }
    
    private static class WMEsIdentifierRow{
        public final long wi_id;
        public final long parent_n_id;
        public final long attribute_s_id;
        public final long child_n_id;
        public final long last_episode_id;
        
        public WMEsIdentifierRow(
            long wi_id,
            long parent_n_id,
            long attribute_s_id,
            long child_n_id,
            long last_episode_id
        ){
            this.wi_id = wi_id;
            this.parent_n_id = parent_n_id;
            this.attribute_s_id = attribute_s_id;
            this.child_n_id = child_n_id;
            this.last_episode_id = last_episode_id;
        }
        
        public String toString(){
            return 
                "wi_id: " + wi_id + 
                " parent_n_id: " + parent_n_id + 
                " attribute_s_id: " + attribute_s_id + 
                " child_n_id: " + child_n_id + 
                " last_episode_id: " + last_episode_id;
        }

        @Override
        public int hashCode()
        {
            final int prime = 31;
            int result = 1;
            result = prime * result + (int) (last_episode_id ^ (last_episode_id >>> 32));
            result = prime * result + (int) (wi_id ^ (wi_id >>> 32));
            result = prime * result + (int) (parent_n_id ^ (parent_n_id >>> 32));
            result = prime * result + (int) (child_n_id ^ (child_n_id >>> 32));
            result = prime * result + (int) (attribute_s_id ^ (attribute_s_id >>> 32));
            return result;
        }

        @Override
        public boolean equals(Object obj)
        {
            if (this == obj)
                return true;
            if (obj == null)
                return false;
            if (getClass() != obj.getClass())
                return false;
            WMEsIdentifierRow other = (WMEsIdentifierRow) obj;
            if (last_episode_id != other.last_episode_id)
                return false;
            if (wi_id != other.wi_id)
                return false;
            if (parent_n_id != other.parent_n_id)
                return false;
            if (child_n_id != other.child_n_id)
                return false;
            if (attribute_s_id != other.attribute_s_id)
                return false;
            return true;
        }
    }
    
    @Test
    public void testWMEsIdentifierTable() throws Exception
    {
        runTest("testHamilton_store", 2);
        
        /*
         * id      start
         * 1-11    1
         */
        
        final Set<WMEsIdentifierRow> expectedRows = new HashSet<WMEsIdentifierRow>();
        expectedRows.add(new WMEsIdentifierRow(1,0,6,1,Long.MAX_VALUE));
        expectedRows.add(new WMEsIdentifierRow(2,0,9,2,Long.MAX_VALUE));
        
        // CK: rows 3 and 4 are reversed because the order of S1's slots is different in JSoar
        // - in JSoar R1 has epmem_id 3 and O3 has epmem_id 4
        // - in Csoar R1 has epmem_id 4 and O3 has epmem_id 3
        expectedRows.add(new WMEsIdentifierRow(3,0,10,3,Long.MAX_VALUE));
        expectedRows.add(new WMEsIdentifierRow(4,0,1,4,Long.MAX_VALUE));
        
        expectedRows.add(new WMEsIdentifierRow(5,1,11,5,Long.MAX_VALUE));
        expectedRows.add(new WMEsIdentifierRow(6,1,12,6,Long.MAX_VALUE));
        expectedRows.add(new WMEsIdentifierRow(7,2,13,7,Long.MAX_VALUE));
        expectedRows.add(new WMEsIdentifierRow(8,2,13,8,Long.MAX_VALUE));
        expectedRows.add(new WMEsIdentifierRow(9,2,13,9,Long.MAX_VALUE));
        expectedRows.add(new WMEsIdentifierRow(10,2,13,10,Long.MAX_VALUE));
        expectedRows.add(new WMEsIdentifierRow(11,2,13,11,Long.MAX_VALUE));
        expectedRows.add(new WMEsIdentifierRow(12,2,13,12,Long.MAX_VALUE));
        expectedRows.add(new WMEsIdentifierRow(13,2,13,13,Long.MAX_VALUE));
        expectedRows.add(new WMEsIdentifierRow(14,7,15,8,Long.MAX_VALUE));
        expectedRows.add(new WMEsIdentifierRow(15,7,15,11,Long.MAX_VALUE));
        expectedRows.add(new WMEsIdentifierRow(16,7,15,13,Long.MAX_VALUE));
        expectedRows.add(new WMEsIdentifierRow(17,8,15,9,Long.MAX_VALUE));
        expectedRows.add(new WMEsIdentifierRow(18,8,15,10,Long.MAX_VALUE));
        expectedRows.add(new WMEsIdentifierRow(19,9,15,11,Long.MAX_VALUE));
        expectedRows.add(new WMEsIdentifierRow(20,9,15,12,Long.MAX_VALUE));
        expectedRows.add(new WMEsIdentifierRow(21,10,15,8,Long.MAX_VALUE));
        expectedRows.add(new WMEsIdentifierRow(22,10,15,11,Long.MAX_VALUE));
        expectedRows.add(new WMEsIdentifierRow(23,11,15,8,Long.MAX_VALUE));
        expectedRows.add(new WMEsIdentifierRow(24,11,15,10,Long.MAX_VALUE));
        expectedRows.add(new WMEsIdentifierRow(25,12,15,10,Long.MAX_VALUE));
        expectedRows.add(new WMEsIdentifierRow(26,12,15,13,Long.MAX_VALUE));
        
        final PreparedStatement p = getConnection().prepareStatement(
                "select * from " + EpisodicMemoryDatabase.EPMEM_SCHEMA + "epmem_wmes_identifier");
        
        final ResultSet results = p.executeQuery();
        
        while(results.next()){
            final WMEsIdentifierRow row = 
                new WMEsIdentifierRow(
                        results.getLong("wi_id"),
                        results.getLong("parent_n_id"),
                        results.getLong("attribute_s_id"),
                        results.getLong("child_n_id"),
                        results.getLong("last_episode_id")
                    );
            assertTrue(
                    "epmem_wmes_identifer contained unexpected " + row, 
                    expectedRows.contains(row)
                );
            expectedRows.remove(row);
        }
        assertTrue(
                "epmem_wmes_identifier did not contain expected row " + 
                (expectedRows.isEmpty()?"":expectedRows.toArray()[0]),
                expectedRows.isEmpty()
            );
    }
    
    @Test
    public void testEpisodesTable() throws Exception
    {
        runTest("testHamilton_store", 2);
        
        /*
         * id      start
         * 1-11    1
         */
        
        final List<Long> expectedRows = new ArrayList<Long>();
        expectedRows.add(1L);
        
        final PreparedStatement p = getConnection().prepareStatement(
                "select * from " + EpisodicMemoryDatabase.EPMEM_SCHEMA + "epmem_episodes");
        
        final ResultSet results = p.executeQuery();
        
        while(results.next()){
            assertTrue(
                    "epmem_episodes contained unexpected " + results.getLong("episode_id"),
                    expectedRows.contains(results.getLong("episode_id"))
                );
            expectedRows.remove(results.getLong("episode_id"));
        }
        assertTrue(
                "epmem_episodes did not contain expected row " + 
                (expectedRows.isEmpty()?"":expectedRows.get(0)),
                expectedRows.isEmpty()
            );
    }
    
    @Test
    public void testPersistentVariablesTable() throws Exception
    {
        runTest("testHamilton_store", 2);
        
        /*
         * id      start
         * 1-11    1
         */
        
        final Map<Long, Long> expectedRows = new HashMap<Long, Long>();
        expectedRows.put(0L,-1L);
        expectedRows.put(1L,0L);
        expectedRows.put(2L,1L);
        expectedRows.put(3L, Long.MAX_VALUE);
        expectedRows.put(4L,-1L);
        expectedRows.put(5L,0L);
        expectedRows.put(6L,1L);
        expectedRows.put(7L, Long.MAX_VALUE);
        expectedRows.put(8L,14L);
        
        final PreparedStatement p = getConnection().prepareStatement(
                "select * from " + EpisodicMemoryDatabase.EPMEM_SCHEMA + "epmem_persistent_variables");
        
        final ResultSet results = p.executeQuery();
        
        while(results.next()){
            assertTrue(
                    "epmem_persistent_variables contained unexpected " + results.getLong("variable_id") + ", " + results.getLong("variable_value"),
                    expectedRows.get(results.getLong("variable_id")) == results.getLong("variable_value")
                );
            expectedRows.remove(results.getLong("variable_id"));
        }
        assertTrue(
                "epmem_persistent_variables did not contain expected row " + 
                        (expectedRows.isEmpty()?"":
                            expectedRows.keySet().toArray()[0]
                            + ", " + 
                            expectedRows.get(expectedRows.keySet().toArray()[0])),
                        expectedRows.isEmpty()
                );
    }
}
