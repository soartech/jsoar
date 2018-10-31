/*
 * Copyright (c) 2010 Dave Ray <daveray@gmail.com>
 *
 * Created on Jun 23, 2010
 */
package org.jsoar.kernel.epmem;


import android.test.AndroidTestCase;

import com.google.common.collect.Sets;

import org.jsoar.util.JdbcTools;

import java.sql.Connection;
import java.sql.DatabaseMetaData;
import java.sql.ResultSet;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;

public class EpisodicMemoryDatabaseTest extends AndroidTestCase
{
    private Connection db;
    
    @Override
    public void setUp() throws Exception
    {
        db = JdbcTools.connect("org.sqlite.JDBC", "jdbc:sqlite::memory:");
    }

    @Override
    public void tearDown() throws Exception
    {
        db.close();
    }
    
    // With the change to make JSoar behave like CSoar, this test is no longer valid
    // Because it will recreate the tables (or at least try to).
    // - ALT
//    @Test
//    public void testIfStructureAlreadyExistsDontRecreate() throws Exception
//    {
//        final EpisodicMemoryDatabase emdb = new EpisodicMemoryDatabase("org.sqlite.JDBC", db);
//        assertTrue(emdb.structure());
//        
//        assertFalse(emdb.structure());
//    }
    
    public void testCanCreateInitialTables() throws Exception
    {
        final EpisodicMemoryDatabase emdb = new EpisodicMemoryDatabase("org.sqlite.JDBC", db);
        emdb.structure();
        
        final Set<String> tables = new HashSet<String>();
        final ResultSet rs = db.getMetaData().getTables(null, null, null, new String[] {"TABLE"});
        while(rs.next())
        {
            tables.add(rs.getString("TABLE_NAME").toLowerCase());
        }
        
        // Here's the tables we expect
        final Set<String> expectedTables = new HashSet<String>(Arrays.asList(            
            EpisodicMemoryDatabase.EPMEM_SCHEMA + "ascii",
            EpisodicMemoryDatabase.EPMEM_SCHEMA + "episodes",
            EpisodicMemoryDatabase.EPMEM_SCHEMA + "lti",
            EpisodicMemoryDatabase.EPMEM_SCHEMA + "nodes",
            
            EpisodicMemoryDatabase.EPMEM_SCHEMA + "persistent_variables", 
            EpisodicMemoryDatabase.EPMEM_SCHEMA + "rit_left_nodes",
            EpisodicMemoryDatabase.EPMEM_SCHEMA + "rit_right_nodes",
            
            EpisodicMemoryDatabase.EPMEM_SCHEMA + "symbols_float",
            EpisodicMemoryDatabase.EPMEM_SCHEMA + "symbols_integer",
            EpisodicMemoryDatabase.EPMEM_SCHEMA + "symbols_string",
            EpisodicMemoryDatabase.EPMEM_SCHEMA + "symbols_type",
            
            EpisodicMemoryDatabase.EPMEM_SCHEMA + "wmes_constant",
            EpisodicMemoryDatabase.EPMEM_SCHEMA + "wmes_constant_now",
            EpisodicMemoryDatabase.EPMEM_SCHEMA + "wmes_constant_point",
            EpisodicMemoryDatabase.EPMEM_SCHEMA + "wmes_constant_range",
            
            EpisodicMemoryDatabase.EPMEM_SCHEMA + "wmes_identifier",
            EpisodicMemoryDatabase.EPMEM_SCHEMA + "wmes_identifier_now",
            EpisodicMemoryDatabase.EPMEM_SCHEMA + "wmes_identifier_point",
            EpisodicMemoryDatabase.EPMEM_SCHEMA + "wmes_identifier_range",
            "versions"
        ));
        
        for(String expected : expectedTables)
        {
            assertTrue("Missing expected table '" + expected + "'", 
                       tables.contains(expected));
        }
        assertEquals(Sets.symmetricDifference(expectedTables, tables).toString(), expectedTables.size(), tables.size());
    }
    
    public void testCanCreateInitialIndexes() throws Exception
    {
        final EpisodicMemoryDatabase emdb = new EpisodicMemoryDatabase("org.sqlite.JDBC", db);
        emdb.structure();
        
        final Set<String> indicies = new HashSet<String>();
        
        // Here's the tables we expect
        //Android fails the query for tables that don't have indexes, so they are commented out
        final Set<String> table_names = new HashSet<String>(Arrays.asList(            
//            EpisodicMemoryDatabase.EPMEM_SCHEMA + "ascii",
//            EpisodicMemoryDatabase.EPMEM_SCHEMA + "episodes",
            EpisodicMemoryDatabase.EPMEM_SCHEMA + "lti",
//            EpisodicMemoryDatabase.EPMEM_SCHEMA + "nodes",
            
//            EpisodicMemoryDatabase.EPMEM_SCHEMA + "persistent_variables",
//            EpisodicMemoryDatabase.EPMEM_SCHEMA + "rit_left_nodes",
//            EpisodicMemoryDatabase.EPMEM_SCHEMA + "rit_right_nodes",
            
            EpisodicMemoryDatabase.EPMEM_SCHEMA + "symbols_float",
            EpisodicMemoryDatabase.EPMEM_SCHEMA + "symbols_integer",
            EpisodicMemoryDatabase.EPMEM_SCHEMA + "symbols_string",
//            EpisodicMemoryDatabase.EPMEM_SCHEMA + "symbols_type",
            
            EpisodicMemoryDatabase.EPMEM_SCHEMA + "wmes_constant",
            EpisodicMemoryDatabase.EPMEM_SCHEMA + "wmes_constant_now",
            EpisodicMemoryDatabase.EPMEM_SCHEMA + "wmes_constant_point",
            EpisodicMemoryDatabase.EPMEM_SCHEMA + "wmes_constant_range",
            
            EpisodicMemoryDatabase.EPMEM_SCHEMA + "wmes_identifier",
            EpisodicMemoryDatabase.EPMEM_SCHEMA + "wmes_identifier_now",
            EpisodicMemoryDatabase.EPMEM_SCHEMA + "wmes_identifier_point",
            EpisodicMemoryDatabase.EPMEM_SCHEMA + "wmes_identifier_range",
            "versions"
        ));

        DatabaseMetaData md = db.getMetaData();
        ResultSet rs;
        for(String tbl : table_names)
        {
            rs = md.getIndexInfo(null, null, tbl, true, false);
        while(rs.next())
        {
                String idx = rs.getString("INDEX_NAME").toLowerCase();
                indicies.add(idx);
                System.err.println("Got index with name: " + idx);
        }
        }
        /*
         * "sqlite_autoindex_" + EpisodicMemoryDatabase.EPMEM_SCHEMA + "versions_1", is an unnamed
         * index autogenerated by sqlite. In CSoar it is unnamed and if it changes these tests will
         * need to be redone anyways
         * - ALT
         */
        final List<String> expectedTables = new ArrayList<String>(Arrays.asList(new String[] {
            EpisodicMemoryDatabase.EPMEM_SCHEMA + "lti_letter_num",
            EpisodicMemoryDatabase.EPMEM_SCHEMA + "wmes_constant_now_id_start",
            EpisodicMemoryDatabase.EPMEM_SCHEMA + "wmes_constant_now_start",
            EpisodicMemoryDatabase.EPMEM_SCHEMA + "wmes_constant_parent_attribute_value",
            EpisodicMemoryDatabase.EPMEM_SCHEMA + "wmes_constant_point_id_start",
            EpisodicMemoryDatabase.EPMEM_SCHEMA + "wmes_constant_point_start",
            EpisodicMemoryDatabase.EPMEM_SCHEMA + "wmes_constant_range_id_end_start",
            EpisodicMemoryDatabase.EPMEM_SCHEMA + "wmes_constant_range_id_start",
            EpisodicMemoryDatabase.EPMEM_SCHEMA + "wmes_constant_range_lower",
            EpisodicMemoryDatabase.EPMEM_SCHEMA + "wmes_constant_range_upper",
            EpisodicMemoryDatabase.EPMEM_SCHEMA + "wmes_identifier_now_id_start",
            EpisodicMemoryDatabase.EPMEM_SCHEMA + "wmes_identifier_now_start",
            EpisodicMemoryDatabase.EPMEM_SCHEMA + "wmes_identifier_parent_attribute_child",
            EpisodicMemoryDatabase.EPMEM_SCHEMA + "wmes_identifier_parent_attribute_last",
            EpisodicMemoryDatabase.EPMEM_SCHEMA + "wmes_identifier_point_id_start",
            EpisodicMemoryDatabase.EPMEM_SCHEMA + "wmes_identifier_point_start",
            EpisodicMemoryDatabase.EPMEM_SCHEMA + "wmes_identifier_range_id_end_start",
            EpisodicMemoryDatabase.EPMEM_SCHEMA + "wmes_identifier_range_id_start",
            EpisodicMemoryDatabase.EPMEM_SCHEMA + "wmes_identifier_range_lower",
            EpisodicMemoryDatabase.EPMEM_SCHEMA + "wmes_identifier_range_upper",
            "sqlite_autoindex_versions_1",
            EpisodicMemoryDatabase.EPMEM_SCHEMA + "symbols_float_const",
            EpisodicMemoryDatabase.EPMEM_SCHEMA + "symbols_int_const",
            EpisodicMemoryDatabase.EPMEM_SCHEMA + "symbols_str_const",
        }));
        
        for(Iterator<String> it = expectedTables.iterator();it.hasNext();)
        {
            String expected = it.next();
            
            assertTrue("Missing expected index '" + expected + "'", indicies.contains(expected));
            
            indicies.remove(expected);
            it.remove();
        }
        
        assertTrue("Unexpected indices: '" + ((indicies.isEmpty())?"":indicies.iterator().next()) + "'", indicies.isEmpty());
        assertTrue("Not Found indices: '" + ((expectedTables.isEmpty())?"":expectedTables.get(0)) + "'", expectedTables.isEmpty());
    }

    public void testPreparesStatements() throws Exception
    {
        final EpisodicMemoryDatabase emdb = new EpisodicMemoryDatabase("org.sqlite.JDBC", db);
        emdb.structure();
        emdb.prepare();
        
        assertNotNull(emdb.begin);
        assertNotNull(emdb.commit);
        assertNotNull(emdb.rollback);
        
        assertNotNull(emdb.var_get);
        assertNotNull(emdb.var_set);
        
        assertNotNull(emdb.rit_add_left);
        assertNotNull(emdb.rit_truncate_left);
        assertNotNull(emdb.rit_add_right);
        assertNotNull(emdb.rit_truncate_right);
        
        assertNotNull(emdb.hash_rev_int);
        assertNotNull(emdb.hash_rev_float);
        assertNotNull(emdb.hash_rev_str);
        assertNotNull(emdb.hash_get_int);
        assertNotNull(emdb.hash_get_float);
        assertNotNull(emdb.hash_get_str);
        assertNotNull(emdb.hash_get_type);
        assertNotNull(emdb.hash_add_type);
        assertNotNull(emdb.hash_add_int);
        assertNotNull(emdb.hash_add_float);
        assertNotNull(emdb.hash_add_str);
        
        // graph_statement_container
        assertNotNull(emdb.add_node);
        assertNotNull(emdb.add_time);
        
        //
        
        assertNotNull(emdb.add_epmem_wmes_constant_now);
        assertNotNull(emdb.delete_epmem_wmes_constant_now);
        assertNotNull(emdb.add_epmem_wmes_constant_point);
        assertNotNull(emdb.add_epmem_wmes_constant_range);
        
        assertNotNull(emdb.add_epmem_wmes_constant);
        assertNotNull(emdb.find_epmem_wmes_constant);
        
        //
        
        assertNotNull(emdb.add_epmem_wmes_identifier_now);
        assertNotNull(emdb.delete_epmem_wmes_identifier_now);
        assertNotNull(emdb.add_epmem_wmes_identifier_point);
        assertNotNull(emdb.add_epmem_wmes_identifier_range);
        
        assertNotNull(emdb.add_epmem_wmes_identifier);
        assertNotNull(emdb.find_epmem_wmes_identifier);
        assertNotNull(emdb.find_epmem_wmes_identifier_shared);
        
        //
        
        assertNotNull(emdb.valid_episode);
        assertNotNull(emdb.next_episode);
        assertNotNull(emdb.prev_episode);
        
        assertNotNull(emdb.get_wmes_with_constant_values);
        assertNotNull(emdb.get_wmes_with_constant_values);
        
        //
        
        assertNotNull(emdb.promote_id);
        assertNotNull(emdb.find_lti);
        assertNotNull(emdb.find_lti_promotion_time);
        
        //
        
        assertNotNull(emdb.update_epmem_wmes_identifier_last_episode_id);
    }
}
