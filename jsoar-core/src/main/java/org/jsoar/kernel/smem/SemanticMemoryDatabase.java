/*
 * Copyright (c) 2010 Dave Ray <daveray@gmail.com>
 *
 * Created on Jun 23, 2010
 */
package org.jsoar.kernel.smem;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.util.Properties;

import org.jsoar.kernel.SoarException;
import org.jsoar.util.JdbcTools;

/**
 * @author ray
 */
class SemanticMemoryDatabase
{
    // empty table used to verify proper structure
    static final String SMEM_SCHEMA = "smem2_";
    static final String SMEM_SIGNATURE = SMEM_SCHEMA + "signature";
    
    private final String driver;
    private final Connection db;
    private Properties statements = new Properties();

    PreparedStatement begin;
    PreparedStatement commit;
    PreparedStatement rollback;

    PreparedStatement var_get;
    PreparedStatement var_set;

    PreparedStatement hash_rev_int;
    PreparedStatement hash_rev_float;
    PreparedStatement hash_rev_str;
    PreparedStatement hash_get_int;
    PreparedStatement hash_get_float;
    PreparedStatement hash_get_str;
    PreparedStatement hash_add_type;
    PreparedStatement hash_add_int;
    PreparedStatement hash_add_float;
    PreparedStatement hash_add_str;

    PreparedStatement lti_add;
    PreparedStatement lti_get;
    PreparedStatement lti_letter_num;
    PreparedStatement lti_max;

    PreparedStatement web_add;
    PreparedStatement web_truncate;
    PreparedStatement web_expand;

    PreparedStatement web_attr_ct;
    PreparedStatement web_const_ct;
    PreparedStatement web_lti_ct;

    PreparedStatement web_attr_all;
    PreparedStatement web_const_all;
    PreparedStatement web_lti_all;

    PreparedStatement web_attr_child;
    PreparedStatement web_const_child;
    PreparedStatement web_lti_child;

    PreparedStatement ct_attr_add;
    PreparedStatement ct_const_add;
    PreparedStatement ct_lti_add;

    PreparedStatement ct_attr_update;
    PreparedStatement ct_const_update;
    PreparedStatement ct_lti_update;

    PreparedStatement ct_attr_get;
    PreparedStatement ct_const_get;
    PreparedStatement ct_lti_get;

    PreparedStatement act_set;
    PreparedStatement act_lti_child_ct_set;
    PreparedStatement act_lti_child_ct_get;
    PreparedStatement act_lti_set;
    PreparedStatement act_lti_get;

    PreparedStatement vis_lti;
    PreparedStatement vis_value_const;
    PreparedStatement vis_value_lti;
    
    public SemanticMemoryDatabase(String driver, Connection db) throws SoarException
    {
        this.driver = driver;
        this.db = db;
    }

    Connection getConnection()
    {
        return this.db;
    }
    
    void structure() throws SoarException, IOException
    {
        // First check if the signature table is already present. If it is, the
        // db is initialized.
        try
        {
            if(JdbcTools.tableExists(db, SMEM_SIGNATURE))
            {
                // If we're here, the table already exists, so don't set up the rest of the 
                // db structure.
                return;
            }
        }
        catch (SQLException e)
        {
            throw new SoarException("While detecting signature table: " + e.getMessage(), e);
        }
        
        // Load the database structure by executing structures.sql
        final InputStream is = SemanticMemoryDatabase.class.getResourceAsStream("structures.sql");
        if(is == null)
        {
            throw new FileNotFoundException("Failed to open structure.sql resource");
        }
        try
        {
            JdbcTools.executeSqlBatch(db, is);
        }
        finally
        {
            is.close();
        }
        
        // The signature table (tested above) is created at the end of structures.sql
    }
    
    void prepare() throws SoarException, IOException
    {
        loadStatements();
        loadDriverSpecificStatements();
        
        //
        begin = prepare( "begin" );
        commit = prepare( "commit" );
        rollback = prepare( "rollback" );

        //
        var_get = prepare( "var_get" );
        var_set = prepare( "var_set" );

        //
        hash_rev_int = prepare("hash_rev_int");
        hash_rev_float = prepare("hash_rev_float");
        hash_rev_str = prepare("hash_rev_str");
        hash_get_int = prepare("hash_get_int");
        hash_get_float = prepare("hash_get_float");
        hash_get_str = prepare("hash_get_str");
        hash_add_type = prepare("hash_add_type");
        hash_add_int = prepare("hash_add_int");
        hash_add_float = prepare("hash_add_float");
        hash_add_str = prepare("hash_add_str");

        //
        lti_add = prepare( "lti_add" );
        lti_get = prepare( "lti_get" );
        lti_letter_num = prepare( "lti_letter_num" );
        lti_max = prepare( "lti_max" );

        //
        web_add = prepare( "web_add" );
        web_truncate = prepare( "web_truncate" );
        web_expand = prepare( "web_expand" );

        //
        web_attr_ct = prepare( "web_attr_ct" );
        web_const_ct = prepare( "web_const_ct" );
        web_lti_ct = prepare( "web_lti_ct" );

        //
        web_attr_all = prepare( "web_attr_all" );
        web_const_all = prepare( "web_const_all" );
        web_lti_all = prepare( "web_lti_all" );

        //
        web_attr_child = prepare( "web_attr_child" );
        web_const_child = prepare( "web_const_child" );
        web_lti_child = prepare( "web_lti_child" );

        //
        ct_attr_add = prepare( "ct_attr_add" );
        ct_const_add = prepare( "ct_const_add" );
        ct_lti_add = prepare( "ct_lti_add" );

        //
        ct_attr_update = prepare( "ct_attr_update" );
        ct_const_update = prepare( "ct_const_update" );
        ct_lti_update = prepare( "ct_lti_update" );

        //
        ct_attr_get = prepare( "ct_attr_get" );
        ct_const_get = prepare( "ct_const_get" );
        ct_lti_get = prepare( "ct_lti_get" );

        //
        act_set = prepare( "act_set" );
        act_lti_child_ct_get = prepare( "act_lti_child_ct_get" );
        act_lti_child_ct_set = prepare( "act_lti_child_ct_set" );
        act_lti_set = prepare( "act_lti_set" );
        act_lti_get = prepare( "act_lti_get" );

        //
        vis_lti = prepare( "vis_lti" );
        vis_value_const = prepare( "vis_value_const" );
        vis_value_lti = prepare( "vis_value_lti" );
    }

    private void loadStatements() throws IOException
    {
        final InputStream is = SemanticMemoryDatabase.class.getResourceAsStream("statements.properties");
        if(is == null)
        {
            throw new FileNotFoundException("Failed to open statements.properties resource");
        }
        try
        {
            statements.load(is);
        }
        finally
        {
            is.close();
        }
    }
    
    private void loadDriverSpecificStatements() throws IOException
    {
        final InputStream is = SemanticMemoryDatabase.class.getResourceAsStream(driver + ".statements.properties");
        if(is == null)
        {
            return;
        }
        try
        {
            final Properties newStatements = new Properties(statements);
            newStatements.load(is);
            statements = newStatements;
        }
        finally
        {
            is.close();
        }
    }
    
    private PreparedStatement prepare(String name) throws SoarException
    {
        final String sql = statements.getProperty(name);
        if(sql == null)
        {
            throw new SoarException("Could not find statement '" + name + "'");
        }
        try
        {
            return db.prepareStatement(sql.trim());
        }
        catch (SQLException e)
        {
            throw new SoarException("Failed to prepare statement '" + sql + "': " + e.getMessage(), e);
        }
    }
}
