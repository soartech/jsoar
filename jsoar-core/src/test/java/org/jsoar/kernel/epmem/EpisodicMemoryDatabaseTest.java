/*
 * Copyright (c) 2010 Dave Ray <daveray@gmail.com>
 *
 * Created on Jun 23, 2010
 */
package org.jsoar.kernel.epmem;


import static org.junit.Assert.*;

import java.sql.Connection;
import java.sql.ResultSet;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

import org.jsoar.util.JdbcTools;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import com.google.common.collect.Sets;

public class EpisodicMemoryDatabaseTest
{
    private Connection db;
    
    @Before
    public void setUp() throws Exception
    {
        db = JdbcTools.connect("org.sqlite.JDBC", "jdbc:sqlite::memory:");
    }

    @After
    public void tearDown() throws Exception
    {
        db.close();
    }
    
    @Test
    public void testIfStructureAlreadyExistsDontRecreate() throws Exception
    {
        final EpisodicMemoryDatabase smdb = new EpisodicMemoryDatabase("org.sqlite.JDBC", db);
        assertTrue(smdb.structure());
        
        assertFalse(smdb.structure());
    }
    
    @Test
    public void testCanCreateInitialTables() throws Exception
    {
        final EpisodicMemoryDatabase smdb = new EpisodicMemoryDatabase("org.sqlite.JDBC", db);
        smdb.structure();
        
        final Set<String> tables = new HashSet<String>();
        final ResultSet rs = db.getMetaData().getTables(null, null, null, new String[] {"TABLE"});
        while(rs.next())
        {
            tables.add(rs.getString("TABLE_NAME").toLowerCase());
        }
        
        // Here's the tables we expect
        final Set<String> expectedTables = new HashSet<String>(Arrays.asList(
            EpisodicMemoryDatabase.EPMEM_SIGNATURE,
            EpisodicMemoryDatabase.EPMEM_SCHEMA + "vars", 
            EpisodicMemoryDatabase.EPMEM_SCHEMA + "rit_left_nodes",
            EpisodicMemoryDatabase.EPMEM_SCHEMA + "rit_right_nodes",
            EpisodicMemoryDatabase.EPMEM_SCHEMA + "temporal_symbol_hash",
            
            EpisodicMemoryDatabase.EPMEM_SCHEMA + "times", 
            EpisodicMemoryDatabase.EPMEM_SCHEMA + "node_now",
            EpisodicMemoryDatabase.EPMEM_SCHEMA + "edge_now",
            EpisodicMemoryDatabase.EPMEM_SCHEMA + "node_point",
            EpisodicMemoryDatabase.EPMEM_SCHEMA + "edge_point",
            EpisodicMemoryDatabase.EPMEM_SCHEMA + "node_range",
            EpisodicMemoryDatabase.EPMEM_SCHEMA + "edge_range",
            EpisodicMemoryDatabase.EPMEM_SCHEMA + "node_unique",
            EpisodicMemoryDatabase.EPMEM_SCHEMA + "edge_unique",
            EpisodicMemoryDatabase.EPMEM_SCHEMA + "lti",
            EpisodicMemoryDatabase.EPMEM_SCHEMA + "ascii",
            "sqlite_sequence" // craeted automatically for AUTOINCREMENT
        ));
        
        for(String expected : expectedTables)
        {
            assertTrue("Missing expected table '" + expected + "'", 
                       tables.contains(expected));
        }
        assertEquals(Sets.symmetricDifference(expectedTables, tables).toString(), expectedTables.size(), tables.size());
    }
    
    @Test
    public void testCanCreateInitialIndexes() throws Exception
    {
        final EpisodicMemoryDatabase smdb = new EpisodicMemoryDatabase("org.sqlite.JDBC", db);
        smdb.structure();
        
        final Set<String> tables = new HashSet<String>();
        final ResultSet rs = db.getMetaData().getTables(null, null, null, new String[] {"INDEX"});
        while(rs.next())
        {
            tables.add(rs.getString("TABLE_NAME").toLowerCase());
        }
        
        // Here's the tables we expect
        final String[] expectedTables = new String[] {
            EpisodicMemoryDatabase.EPMEM_SCHEMA + "temporal_symbol_hash_const_type", 
            EpisodicMemoryDatabase.EPMEM_SCHEMA + "node_now_start", 
            EpisodicMemoryDatabase.EPMEM_SCHEMA + "node_now_id_start", 
            EpisodicMemoryDatabase.EPMEM_SCHEMA + "edge_now_start", 
            EpisodicMemoryDatabase.EPMEM_SCHEMA + "edge_now_id_start",
            EpisodicMemoryDatabase.EPMEM_SCHEMA + "node_point_id_start",
            EpisodicMemoryDatabase.EPMEM_SCHEMA + "node_point_start",
            EpisodicMemoryDatabase.EPMEM_SCHEMA + "edge_point_id_start",
            EpisodicMemoryDatabase.EPMEM_SCHEMA + "edge_point_start",
            EpisodicMemoryDatabase.EPMEM_SCHEMA + "node_range_lower",
            EpisodicMemoryDatabase.EPMEM_SCHEMA + "node_range_upper",
            EpisodicMemoryDatabase.EPMEM_SCHEMA + "node_range_id_start",
            EpisodicMemoryDatabase.EPMEM_SCHEMA + "node_range_id_end",
            EpisodicMemoryDatabase.EPMEM_SCHEMA + "edge_range_lower",
            EpisodicMemoryDatabase.EPMEM_SCHEMA + "edge_range_upper",
            EpisodicMemoryDatabase.EPMEM_SCHEMA + "edge_range_id_start",
            EpisodicMemoryDatabase.EPMEM_SCHEMA + "edge_range_id_end",
            EpisodicMemoryDatabase.EPMEM_SCHEMA + "node_unique_parent_attrib_value",
            EpisodicMemoryDatabase.EPMEM_SCHEMA + "edge_unique_q0_w_q1",
            EpisodicMemoryDatabase.EPMEM_SCHEMA + "lti_letter_num",
        };
        
        for(String expected : expectedTables)
        {
            assertTrue("Missing expected index '" + expected + "'", tables.contains(expected));
        }
        assertEquals(expectedTables.length, tables.size());
    }

    @Test
    public void testPreparesStatements() throws Exception
    {
        final EpisodicMemoryDatabase smdb = new EpisodicMemoryDatabase("org.sqlite.JDBC", db);
        smdb.structure();
        smdb.prepare();
        
        assertNotNull(smdb.begin);
        assertNotNull(smdb.commit);
        assertNotNull(smdb.rollback);
        assertNotNull(smdb.var_get);
        assertNotNull(smdb.var_set);
        assertNotNull(smdb.rit_add_left);
        assertNotNull(smdb.rit_truncate_left);
        assertNotNull(smdb.rit_add_right);
        assertNotNull(smdb.rit_truncate_right);
        assertNotNull(smdb.hash_get);
        assertNotNull(smdb.hash_add);
        
        // epmem_graph_statement_container
        assertNotNull(smdb.add_time);
        assertNotNull(smdb.add_node_now);
        assertNotNull(smdb.delete_node_now);
        assertNotNull(smdb.add_node_point);
        assertNotNull(smdb.add_node_range);
        assertNotNull(smdb.add_node_unique);
        assertNotNull(smdb.find_node_unique);
        assertNotNull(smdb.add_edge_now);
        assertNotNull(smdb.delete_edge_now);
        assertNotNull(smdb.add_edge_point);
        assertNotNull(smdb.add_edge_range);
        assertNotNull(smdb.add_edge_unique);
        assertNotNull(smdb.find_edge_unique);
        assertNotNull(smdb.find_edge_unique_shared);
        assertNotNull(smdb.valid_episode);
        assertNotNull(smdb.next_episode);
        assertNotNull(smdb.prev_episode);
        assertNotNull(smdb.get_nodes);
        assertNotNull(smdb.get_edges);
        assertNotNull(smdb.promote_id);
        assertNotNull(smdb.find_lti);

    }
}
