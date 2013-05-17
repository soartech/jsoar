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
    public void testEdgeNowTable() throws Exception
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
                "select * from " + EpisodicMemoryDatabase.EPMEM_SCHEMA + "edge_now");
        
        final ResultSet results = p.executeQuery();
        
        while(results.next()){
            final long id = results.getLong("id");
            final long start = results.getLong("start");
            assertTrue(
                    "edge_now contained unexpected " + id + ", " + start, 
                    expectedRowIds.contains(id) && start == 1
                );
            expectedRowIds.remove(id);
        }
        assertTrue(
                "edge_now did not contain expected id " + expectedRowIds.get(0) + ", " + 1,
                expectedRowIds.isEmpty()
            );
    }
    
    @Test
    public void testNodeNowTable() throws Exception
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
                "select * from " + EpisodicMemoryDatabase.EPMEM_SCHEMA + "node_now");
        
        final ResultSet results = p.executeQuery();
        
        while(results.next()){
            final long id = results.getLong("id");
            final long start = results.getLong("start");
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
        public final long childID;
        public final long parentID;
        public final long attrib;
        public final long value;
        
        public NodeUniqueRow(
            long childID,
            long parentID,
            long attrib,
            long value
        ){
            this.childID = childID;
            this.parentID = parentID;
            this.attrib = attrib;
            this.value = value;
        }
        
        public String toString(){
            return 
                "ChildID: " + childID + 
                " ParentID: " + parentID + 
                " Attrib: " + attrib + 
                " Value: " + value;
        }
        
        @Override
        public int hashCode()
        {
            final int prime = 31;
            int result = 1;
            result = prime * result + (int) (attrib ^ (attrib >>> 32));
            result = prime * result + (int) (childID ^ (childID >>> 32));
            result = prime * result + (int) (parentID ^ (parentID >>> 32));
            result = prime * result + (int) (value ^ (value >>> 32));
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
            if (attrib != other.attrib)
                return false;
            if (childID != other.childID)
                return false;
            if (parentID != other.parentID)
                return false;
            if (value != other.value)
                return false;
            return true;
        }
    }
    
    @Test
    public void testNodeUniqueTable() throws Exception
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
        expectedRows.add(new NodeUniqueRow(4,3,7,14));
        expectedRows.add(new NodeUniqueRow(5,7,7,16));
        expectedRows.add(new NodeUniqueRow(6,8,7,17));
        expectedRows.add(new NodeUniqueRow(7,9,7,18));
        expectedRows.add(new NodeUniqueRow(8,10,7,19));
        expectedRows.add(new NodeUniqueRow(9,11,7,20));
        expectedRows.add(new NodeUniqueRow(10,12,7,21));
        expectedRows.add(new NodeUniqueRow(11,13,7,22));
        
        final PreparedStatement p = getConnection().prepareStatement(
                "select * from " + EpisodicMemoryDatabase.EPMEM_SCHEMA + "node_unique");
        
        final ResultSet results = p.executeQuery();
        
        while(results.next()){
            final NodeUniqueRow row = 
                new NodeUniqueRow(
                        results.getLong("child_id"),
                        results.getLong("parent_id"),
                        results.getLong("attrib"),
                        results.getLong("value")
                    );
            assertTrue(
                    "node_unique contained unexpected " + row, 
                    expectedRows.contains(row)
                );
            expectedRows.remove(row);
        }
        assertTrue(
                "node_unique did not contain expected row " + expectedRows.toArray()[0],
                expectedRows.isEmpty()
            );
    }
    
    private static class TemporalSymbolHashRow{
        public final long id;
        public final String symConstant;
        public final long symType;
        
        public TemporalSymbolHashRow(
            long id,
            String symConstant,
            long symType
        ){
            this.id = id;
            this.symConstant = symConstant;
            this.symType = symType;
        }
        
        public String toString(){
            return 
                "ID: " + id + 
                " SymConstant: " + symConstant + 
                " SymType: " + symType;
        }

        @Override
        public int hashCode()
        {
            final int prime = 31;
            int result = 1;
            result = prime * result + (int) (id ^ (id >>> 32));
            result = prime * result
                    + ((symConstant == null) ? 0 : symConstant.hashCode());
            result = prime * result + (int) (symType ^ (symType >>> 32));
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
            TemporalSymbolHashRow other = (TemporalSymbolHashRow) obj;
            if (id != other.id)
                return false;
            if (symConstant == null)
            {
                if (other.symConstant != null)
                    return false;
            }
            else if (!symConstant.equals(other.symConstant))
                return false;
            if (symType != other.symType)
                return false;
            return true;
        }
    }
    
    @Test
    public void testTemporalSymbolHashTable() throws Exception
    {
        runTest("testHamilton_store", 2);
        
        /*
         * id      start
         * 1-11    1
         */
        
        final Set<TemporalSymbolHashRow> expectedRows = new HashSet<TemporalSymbolHashRow>();
        expectedRows.add(new TemporalSymbolHashRow(0, null,1));
        expectedRows.add(new TemporalSymbolHashRow(20,"Albany",2));
        expectedRows.add(new TemporalSymbolHashRow(21,"Atlanta",2));
        expectedRows.add(new TemporalSymbolHashRow(22,"Boston",2));
        expectedRows.add(new TemporalSymbolHashRow(19,"Dallas",2));
        expectedRows.add(new TemporalSymbolHashRow(16,"Fresno",2));
        expectedRows.add(new TemporalSymbolHashRow(18,"Omaha",2));
        expectedRows.add(new TemporalSymbolHashRow(17,"Seattle",2));
        expectedRows.add(new TemporalSymbolHashRow(13,"city",2));
        expectedRows.add(new TemporalSymbolHashRow(8,"graph-match-unit",2));
        expectedRows.add(new TemporalSymbolHashRow(14,"halt",2));
        expectedRows.add(new TemporalSymbolHashRow(9,"hamilton",2));
        expectedRows.add(new TemporalSymbolHashRow(12,"input-link",2));
        expectedRows.add(new TemporalSymbolHashRow(6,"io",2));
        expectedRows.add(new TemporalSymbolHashRow(7,"name",2));
        expectedRows.add(new TemporalSymbolHashRow(3,"nil",2));
        expectedRows.add(new TemporalSymbolHashRow(1,"operator*",2));
        expectedRows.add(new TemporalSymbolHashRow(11,"output-link",2));
        expectedRows.add(new TemporalSymbolHashRow(10,"reward-link",2));
        expectedRows.add(new TemporalSymbolHashRow(5,"state",2));
        expectedRows.add(new TemporalSymbolHashRow(2,"superstate",2));
        expectedRows.add(new TemporalSymbolHashRow(15,"to",2));
        expectedRows.add(new TemporalSymbolHashRow(4,"type",2));
        
        final PreparedStatement p = getConnection().prepareStatement(
                "select * from " + EpisodicMemoryDatabase.EPMEM_SCHEMA + "temporal_symbol_hash");
        
        final ResultSet results = p.executeQuery();
        
        while(results.next()){
            final TemporalSymbolHashRow row = 
                new TemporalSymbolHashRow(
                        results.getLong("id"),
                        results.getString("sym_const"),
                        results.getLong("sym_type")
                    );
            assertTrue(
                    "temporal_symbol_hash contained unexpected " + row, 
                    expectedRows.contains(row)
                );
            expectedRows.remove(row);
        }
        assertTrue(
                "temporal_symbol_hash did not contain expected row " + 
                (expectedRows.isEmpty()?"":expectedRows.toArray()[0]),
                expectedRows.isEmpty()
            );
    }
    
    private static class EdgeUniqueRow{
        public final long parentID;
        public final long q0;
        public final long w;
        public final long q1;
        public final long last;
        
        public EdgeUniqueRow(
            long parentID,
            long q0,
            long w,
            long q1,
            long last
        ){
            this.parentID = parentID;
            this.q0 = q0;
            this.w = w;
            this.q1 = q1;
            this.last = last;
        }
        
        public String toString(){
            return 
                "ParentID: " + parentID + 
                " q0: " + q0 + 
                " w: " + w + 
                " q1: " + q1 + 
                " last: " + last;
        }

        @Override
        public int hashCode()
        {
            final int prime = 31;
            int result = 1;
            result = prime * result + (int) (last ^ (last >>> 32));
            result = prime * result + (int) (parentID ^ (parentID >>> 32));
            result = prime * result + (int) (q0 ^ (q0 >>> 32));
            result = prime * result + (int) (q1 ^ (q1 >>> 32));
            result = prime * result + (int) (w ^ (w >>> 32));
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
            EdgeUniqueRow other = (EdgeUniqueRow) obj;
            if (last != other.last)
                return false;
            if (parentID != other.parentID)
                return false;
            if (q0 != other.q0)
                return false;
            if (q1 != other.q1)
                return false;
            if (w != other.w)
                return false;
            return true;
        }
    }
    
    @Test
    public void testEdgeUniqueTable() throws Exception
    {
        runTest("testHamilton_store", 2);
        
        /*
         * id      start
         * 1-11    1
         */
        
        final Set<EdgeUniqueRow> expectedRows = new HashSet<EdgeUniqueRow>();
        expectedRows.add(new EdgeUniqueRow(1,0,6,1,Long.MAX_VALUE));
        expectedRows.add(new EdgeUniqueRow(2,0,9,2,Long.MAX_VALUE));
        expectedRows.add(new EdgeUniqueRow(3,0,1,3,Long.MAX_VALUE));
        expectedRows.add(new EdgeUniqueRow(4,0,10,4,Long.MAX_VALUE));
        expectedRows.add(new EdgeUniqueRow(5,1,11,5,Long.MAX_VALUE));
        expectedRows.add(new EdgeUniqueRow(6,1,12,6,Long.MAX_VALUE));
        expectedRows.add(new EdgeUniqueRow(7,2,13,7,Long.MAX_VALUE));
        expectedRows.add(new EdgeUniqueRow(8,2,13,8,Long.MAX_VALUE));
        expectedRows.add(new EdgeUniqueRow(9,2,13,9,Long.MAX_VALUE));
        expectedRows.add(new EdgeUniqueRow(10,2,13,10,Long.MAX_VALUE));
        expectedRows.add(new EdgeUniqueRow(11,2,13,11,Long.MAX_VALUE));
        expectedRows.add(new EdgeUniqueRow(12,2,13,12,Long.MAX_VALUE));
        expectedRows.add(new EdgeUniqueRow(13,2,13,13,Long.MAX_VALUE));
        expectedRows.add(new EdgeUniqueRow(14,7,15,8,Long.MAX_VALUE));
        expectedRows.add(new EdgeUniqueRow(15,7,15,11,Long.MAX_VALUE));
        expectedRows.add(new EdgeUniqueRow(16,7,15,13,Long.MAX_VALUE));
        expectedRows.add(new EdgeUniqueRow(17,8,15,9,Long.MAX_VALUE));
        expectedRows.add(new EdgeUniqueRow(18,8,15,10,Long.MAX_VALUE));
        expectedRows.add(new EdgeUniqueRow(19,9,15,11,Long.MAX_VALUE));
        expectedRows.add(new EdgeUniqueRow(20,9,15,12,Long.MAX_VALUE));
        expectedRows.add(new EdgeUniqueRow(21,10,15,8,Long.MAX_VALUE));
        expectedRows.add(new EdgeUniqueRow(22,10,15,11,Long.MAX_VALUE));
        expectedRows.add(new EdgeUniqueRow(23,11,15,8,Long.MAX_VALUE));
        expectedRows.add(new EdgeUniqueRow(24,11,15,10,Long.MAX_VALUE));
        expectedRows.add(new EdgeUniqueRow(25,12,15,10,Long.MAX_VALUE));
        expectedRows.add(new EdgeUniqueRow(26,12,15,13,Long.MAX_VALUE));
        
        final PreparedStatement p = getConnection().prepareStatement(
                "select * from " + EpisodicMemoryDatabase.EPMEM_SCHEMA + "edge_unique");
        
        final ResultSet results = p.executeQuery();
        
        while(results.next()){
            final EdgeUniqueRow row = 
                new EdgeUniqueRow(
                        results.getLong("parent_id"),
                        results.getLong("q0"),
                        results.getLong("w"),
                        results.getLong("q1"),
                        results.getLong("last")
                    );
            assertTrue(
                    "edge_unique contained unexpected " + row, 
                    expectedRows.contains(row)
                );
            expectedRows.remove(row);
        }
        assertTrue(
                "edge_unique did not contain expected row " + 
                (expectedRows.isEmpty()?"":expectedRows.toArray()[0]),
                expectedRows.isEmpty()
            );
    }
    
    @Test
    public void testTimesTable() throws Exception
    {
        runTest("testHamilton_store", 2);
        
        /*
         * id      start
         * 1-11    1
         */
        
        final List<Long> expectedRows = new ArrayList<Long>();
        expectedRows.add(1L);
        
        final PreparedStatement p = getConnection().prepareStatement(
                "select * from " + EpisodicMemoryDatabase.EPMEM_SCHEMA + "times");
        
        final ResultSet results = p.executeQuery();
        
        while(results.next()){
            assertTrue(
                    "times contained unexpected " + results.getLong("id"),
                    expectedRows.contains(results.getLong("id"))
                );
            expectedRows.remove(results.getLong("id"));
        }
        assertTrue(
                "times did not contain expected row " + 
                (expectedRows.isEmpty()?"":expectedRows.get(0)),
                expectedRows.isEmpty()
            );
    }
}
