/*
 * Copyright (c) 2010 Dave Ray <daveray@gmail.com>
 *
 * Created on Jun 23, 2010
 */
package org.jsoar.kernel.smem;


import static org.junit.Assert.*;

import java.sql.Connection;
import java.sql.ResultSet;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;

import org.jsoar.util.JdbcTools;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

public class SemanticMemoryDatabaseTest
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
    
    // With the change to make JSoar behave like CSoar, this test is no longer valid
    // Because it will recreate the tables (or at least try to).
    // - ALT
//    @Test
//    public void testIfStructureAlreadyExistsDontRecreate() throws Exception
//    {
//        final SemanticMemoryDatabase smdb = new SemanticMemoryDatabase("org.sqlite.JDBC", db);
//        assertTrue(smdb.structure());
//        
//        assertFalse(smdb.structure());
//    }
    
    @Test
    public void testCanCreateInitialTables() throws Exception
    {
        final SemanticMemoryDatabase smdb = new SemanticMemoryDatabase("org.sqlite.JDBC", db);
        smdb.structure();
        
        final Set<String> tables = new HashSet<String>();
        final ResultSet rs = db.getMetaData().getTables(null, null, null, new String[] {"TABLE"});
        while(rs.next())
        {
            tables.add(rs.getString("TABLE_NAME").toLowerCase());
        }
        
        // Here's the tables we expect
        final String[] expectedTables = new String[] {
            SemanticMemoryDatabase.SMEM_SCHEMA + "persistent_variables", 
            SemanticMemoryDatabase.SMEM_SCHEMA + "symbols_type",
            SemanticMemoryDatabase.SMEM_SCHEMA + "symbols_integer",
            SemanticMemoryDatabase.SMEM_SCHEMA + "symbols_float",
            SemanticMemoryDatabase.SMEM_SCHEMA + "symbols_string",
            SemanticMemoryDatabase.SMEM_SCHEMA + "lti",
            SemanticMemoryDatabase.SMEM_SCHEMA + "activation_history",
            SemanticMemoryDatabase.SMEM_SCHEMA + "augmentations",
            SemanticMemoryDatabase.SMEM_SCHEMA + "attribute_frequency",
            SemanticMemoryDatabase.SMEM_SCHEMA + "wmes_constant_frequency",
            SemanticMemoryDatabase.SMEM_SCHEMA + "wmes_lti_frequency",
            SemanticMemoryDatabase.SMEM_SCHEMA + "ascii",
            
            "versions"
        };
        
        for(String expected : expectedTables)
        {
            assertTrue("Missing expected table '" + expected + "'", 
                       tables.contains(expected));
        }
        assertEquals(expectedTables.length, tables.size());
    }
    
    @Test
    public void testCanCreateInitialIndexes() throws Exception
    {
        final SemanticMemoryDatabase smdb = new SemanticMemoryDatabase("org.sqlite.JDBC", db);
        smdb.structure();
        
final Set<String> indexes = new HashSet<String>();
        
        final ResultSet rs = db.getMetaData().getTables(null, null, null, new String[] {"TABLE"});
        while(rs.next())
        {
        	String tableName = rs.getString("TABLE_NAME");
        	System.out.println(tableName);
            final ResultSet rsIndexes = db.getMetaData().getIndexInfo(null, null, tableName, false, false);
            while(rsIndexes.next())
            {
            	indexes.add(rsIndexes.getString("INDEX_NAME").toLowerCase());
            }
        }
        
        // Here's the tables we expect
        final List<String> expectedTables = new ArrayList<String>(Arrays.asList(new String[] {
            SemanticMemoryDatabase.SMEM_SCHEMA + "symbols_int_const",
            SemanticMemoryDatabase.SMEM_SCHEMA + "symbols_float_const",
            SemanticMemoryDatabase.SMEM_SCHEMA + "symbols_str_const",
            SemanticMemoryDatabase.SMEM_SCHEMA + "lti_letter_num",
            SemanticMemoryDatabase.SMEM_SCHEMA + "lti_t",
            SemanticMemoryDatabase.SMEM_SCHEMA + "augmentations_parent_attr_val_lti",
            SemanticMemoryDatabase.SMEM_SCHEMA + "augmentations_attr_val_lti_cycle",
            SemanticMemoryDatabase.SMEM_SCHEMA + "augmentations_attr_cycle",
            SemanticMemoryDatabase.SMEM_SCHEMA + "wmes_constant_frequency_attr_val",
            SemanticMemoryDatabase.SMEM_SCHEMA + "ct_lti_attr_val",
            "sqlite_autoindex_versions_1",
        }));
        
        for(Iterator<String> it = expectedTables.iterator();it.hasNext();)
        {
            String expected = it.next();
            
            assertTrue("Missing expected index '" + expected + "'", indexes.contains(expected));
            
            indexes.remove(expected);
            it.remove();
        }
        
        assertTrue("Unexpected indices: '" + ((indexes.isEmpty())?"":indexes.iterator().next()) + "'", indexes.isEmpty());
        assertTrue("Not Found indices: '" + ((expectedTables.isEmpty())?"":expectedTables.get(0)) + "'", expectedTables.isEmpty());
    }

    @Test
    public void testPreparesStatements() throws Exception
    {
        final SemanticMemoryDatabase smdb = new SemanticMemoryDatabase("org.sqlite.JDBC", db);
        smdb.structure();
        smdb.prepare();
        
        assertNotNull(smdb.begin);
        assertNotNull(smdb.commit);
        assertNotNull(smdb.rollback);

        assertNotNull(smdb.var_get);
        assertNotNull(smdb.var_set);

        assertNotNull(smdb.hash_rev_int);
        assertNotNull(smdb.hash_rev_float);
        assertNotNull(smdb.hash_rev_str);
        assertNotNull(smdb.hash_get_int);
        assertNotNull(smdb.hash_get_float);
        assertNotNull(smdb.hash_get_str);
        assertNotNull(smdb.hash_add_type);
        assertNotNull(smdb.hash_add_int);
        assertNotNull(smdb.hash_add_float);
        assertNotNull(smdb.hash_add_str);

        assertNotNull(smdb.lti_add);
        assertNotNull(smdb.lti_get);
        assertNotNull(smdb.lti_letter_num);
        assertNotNull(smdb.lti_max);

        assertNotNull(smdb.web_add);
        assertNotNull(smdb.web_truncate);
        assertNotNull(smdb.web_expand);

        assertNotNull(smdb.web_all);

        assertNotNull(smdb.web_attr_all);
        assertNotNull(smdb.web_const_all);
        assertNotNull(smdb.web_lti_all);

        assertNotNull(smdb.web_attr_child);
        assertNotNull(smdb.web_const_child);
        assertNotNull(smdb.web_lti_child);

        assertNotNull(smdb.attribute_frequency_check);
        assertNotNull(smdb.wmes_constant_frequency_check);
        assertNotNull(smdb.wmes_lti_frequency_check);

        assertNotNull(smdb.attribute_frequency_add);
        assertNotNull(smdb.wmes_constant_frequency_add);
        assertNotNull(smdb.wmes_lti_frequency_add);

        assertNotNull(smdb.attribute_frequency_update);
        assertNotNull(smdb.wmes_constant_frequency_update);
        assertNotNull(smdb.wmes_lti_frequency_update);
        
        assertNotNull(smdb.attribute_frequency_get);
        assertNotNull(smdb.wmes_constant_frequency_get);
        assertNotNull(smdb.wmes_lti_frequency_get);

        assertNotNull(smdb.act_set);
        assertNotNull(smdb.act_lti_child_ct_set);
        assertNotNull(smdb.act_lti_child_ct_get);
        assertNotNull(smdb.act_lti_set);
        assertNotNull(smdb.act_lti_get);

        assertNotNull(smdb.vis_lti);
        assertNotNull(smdb.vis_value_const);
        assertNotNull(smdb.vis_value_lti);
    }
}
