/*
 * Copyright (c) 2010 Dave Ray <daveray@gmail.com>
 *
 * Created on Jun 23, 2010
 */
package org.jsoar.kernel.smem;


import static org.junit.Assert.*;

import java.sql.Connection;
import java.sql.ResultSet;
import java.util.HashSet;
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
    
    @Test
    public void testIfStructureAlreadyExistsDontRecreate() throws Exception
    {
        final SemanticMemoryDatabase smdb = new SemanticMemoryDatabase("org.sqlite.JDBC", db);
        smdb.structure();
        
        smdb.structure();
    }
    
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
            SemanticMemoryDatabase.SMEM_SIGNATURE,
            SemanticMemoryDatabase.SMEM_SCHEMA + "vars", 
            SemanticMemoryDatabase.SMEM_SCHEMA + "symbols_type",
            SemanticMemoryDatabase.SMEM_SCHEMA + "symbols_int",
            SemanticMemoryDatabase.SMEM_SCHEMA + "symbols_float",
            SemanticMemoryDatabase.SMEM_SCHEMA + "symbols_str",
            SemanticMemoryDatabase.SMEM_SCHEMA + "lti",
            SemanticMemoryDatabase.SMEM_SCHEMA + "web",
            SemanticMemoryDatabase.SMEM_SCHEMA + "ct_attr",
            SemanticMemoryDatabase.SMEM_SCHEMA + "ct_const",
            SemanticMemoryDatabase.SMEM_SCHEMA + "ct_lti",
            SemanticMemoryDatabase.SMEM_SCHEMA + "ascii"
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
        
        final Set<String> tables = new HashSet<String>();
        final ResultSet rs = db.getMetaData().getTables(null, null, null, new String[] {"INDEX"});
        while(rs.next())
        {
            tables.add(rs.getString("TABLE_NAME").toLowerCase());
        }
        
        // Here's the tables we expect
        final String[] expectedTables = new String[] {
            SemanticMemoryDatabase.SMEM_SCHEMA + "symbols_int_const", 
            SemanticMemoryDatabase.SMEM_SCHEMA + "symbols_float_const", 
            SemanticMemoryDatabase.SMEM_SCHEMA + "symbols_str_const", 
            SemanticMemoryDatabase.SMEM_SCHEMA + "lti_letter_num",
            SemanticMemoryDatabase.SMEM_SCHEMA + "web_parent_attr_val_lti",
            SemanticMemoryDatabase.SMEM_SCHEMA + "web_attr_val_lti_cycle",
            SemanticMemoryDatabase.SMEM_SCHEMA + "web_attr_cycle",
            SemanticMemoryDatabase.SMEM_SCHEMA + "ct_const_attr_val",
            SemanticMemoryDatabase.SMEM_SCHEMA + "ct_lti_attr_val",
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

        assertNotNull(smdb.web_attr_ct);
        assertNotNull(smdb.web_const_ct);
        assertNotNull(smdb.web_lti_ct);

        assertNotNull(smdb.web_attr_all);
        assertNotNull(smdb.web_const_all);
        assertNotNull(smdb.web_lti_all);

        assertNotNull(smdb.web_attr_child);
        assertNotNull(smdb.web_const_child);
        assertNotNull(smdb.web_lti_child);

        assertNotNull(smdb.ct_attr_add);
        assertNotNull(smdb.ct_const_add);
        assertNotNull(smdb.ct_lti_add);

        assertNotNull(smdb.ct_attr_update);
        assertNotNull(smdb.ct_const_update);
        assertNotNull(smdb.ct_lti_update);

        assertNotNull(smdb.ct_attr_get);
        assertNotNull(smdb.ct_const_get);
        assertNotNull(smdb.ct_lti_get);

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
