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

import org.jsoar.kernel.SoarException;
import org.jsoar.util.JdbcTools;

/**
 * @author ray
 */
class SemanticMemoryDatabase
{
    private final Connection db;

    PreparedStatement begin;
    PreparedStatement commit;
    PreparedStatement rollback;

    PreparedStatement var_get;
    PreparedStatement var_set;

    PreparedStatement hash_get;
    PreparedStatement hash_add;

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
    
    public SemanticMemoryDatabase(Connection db) throws SoarException
    {
        this.db = db;
    }

    Connection getConnection()
    {
        return this.db;
    }
    
    void structure() throws SoarException, IOException
    {
        final InputStream is = SemanticMemoryDatabase.class.getResourceAsStream("structures.sql");
        if(is == null)
        {
            throw new FileNotFoundException("Failed to open structure.sql resource");
        }
        JdbcTools.executeSql(db, is);
    }
    
    void prepare() throws SoarException
    {
        //
        begin = prepare( "BEGIN" );
        commit = prepare( "COMMIT" );
        rollback = prepare( "ROLLBACK" );

        //
        var_get = prepare( "SELECT value FROM vars WHERE id=?" );
        var_set = prepare( "REPLACE INTO vars (id,value) VALUES (?,?)" );

        //
        hash_get = prepare( "SELECT id FROM temporal_symbol_hash WHERE sym_type=? AND sym_const=?" );
        hash_add = prepare( "INSERT INTO temporal_symbol_hash (sym_type,sym_const) VALUES (?,?)" );

        //
        lti_add = prepare( "INSERT INTO lti (letter,num,child_ct,act_cycle) VALUES (?,?,?,?)" );
        lti_get = prepare( "SELECT id FROM lti WHERE letter=? AND num=?" );
        lti_letter_num = prepare( "SELECT letter, num FROM lti WHERE id=?" );
        lti_max = prepare( "SELECT letter, MAX(num) FROM lti GROUP BY letter" );

        //
        web_add = prepare( "INSERT INTO web (parent_id, attr, val_const, val_lti, act_cycle) VALUES (?,?,?,?,?)" );
        web_truncate = prepare( "DELETE FROM web WHERE parent_id=?" );
        web_expand = prepare( "SELECT tsh_a.sym_const AS attr_const, tsh_a.sym_type AS attr_type, vcl.sym_const AS value_const, vcl.sym_type AS value_type, vcl.letter AS value_letter, vcl.num AS value_num, vcl.val_lti AS value_lti FROM ((web w LEFT JOIN temporal_symbol_hash tsh_v ON w.val_const=tsh_v.id) vc LEFT JOIN lti ON vc.val_lti=lti.id) vcl INNER JOIN temporal_symbol_hash tsh_a ON vcl.attr=tsh_a.id WHERE parent_id=?" );

        //
        web_attr_ct = prepare( "SELECT attr, COUNT(*) AS ct FROM web WHERE parent_id=? GROUP BY attr" );
        web_const_ct = prepare( "SELECT attr, val_const, COUNT(*) AS ct FROM web WHERE parent_id=? AND val_const IS NOT NULL GROUP BY attr, val_const" );
        web_lti_ct = prepare( "SELECT attr, val_lti, COUNT(*) AS ct FROM web WHERE parent_id=? AND val_const IS NULL GROUP BY attr, val_const, val_lti" );

        //
        web_attr_all = prepare( "SELECT parent_id, act_cycle FROM web w WHERE attr=? ORDER BY act_cycle DESC" );
        web_const_all = prepare( "SELECT parent_id, act_cycle FROM web w WHERE attr=? AND val_const=? AND val_lti IS NULL ORDER BY act_cycle DESC" );
        web_lti_all = prepare( "SELECT parent_id, act_cycle FROM web w WHERE attr=? AND val_const IS NULL AND val_lti=? ORDER BY act_cycle DESC" );

        //
        web_attr_child = prepare( "SELECT parent_id FROM web WHERE parent_id=? AND attr=?" );
        web_const_child = prepare( "SELECT parent_id FROM web WHERE parent_id=? AND attr=? AND val_const=?" );
        web_lti_child = prepare( "SELECT parent_id FROM web WHERE parent_id=? AND attr=? AND val_const IS NULL AND val_lti=?" );

        //
        ct_attr_add = prepare( "INSERT OR IGNORE INTO ct_attr (attr, ct) VALUES (?,0)" );
        ct_const_add = prepare( "INSERT OR IGNORE INTO ct_const (attr, val_const, ct) VALUES (?,?,0)" );
        ct_lti_add = prepare( "INSERT OR IGNORE INTO ct_lti (attr, val_lti, ct) VALUES (?,?,0)" );

        //
        ct_attr_update = prepare( "UPDATE ct_attr SET ct = ct + ? WHERE attr=?" );
        ct_const_update = prepare( "UPDATE ct_const SET ct = ct + ? WHERE attr=? AND val_const=?" );
        ct_lti_update = prepare( "UPDATE ct_lti SET ct = ct + ? WHERE attr=? AND val_lti=?" );

        //
        ct_attr_get = prepare( "SELECT ct FROM ct_attr WHERE attr=?" );
        ct_const_get = prepare( "SELECT ct FROM ct_const WHERE attr=? AND val_const=?" );
        ct_lti_get = prepare( "SELECT ct FROM ct_lti WHERE attr=? AND val_lti=?" );

        //
        act_set = prepare( "UPDATE web SET act_cycle=? WHERE parent_id=?" );
        act_lti_child_ct_get = prepare( "SELECT child_ct FROM lti WHERE id=?" );
        act_lti_child_ct_set = prepare( "UPDATE lti SET child_ct=? WHERE id=?" );
        act_lti_set = prepare( "UPDATE lti SET act_cycle=? WHERE id=?" );
        act_lti_get = prepare( "SELECT act_cycle FROM lti WHERE id=?" );

        //
        vis_lti = prepare( "SELECT id, letter, num FROM lti" );
        vis_value_const = prepare( "SELECT parent_id, tsh1.sym_type AS attr_type, tsh1.sym_const AS attr_val, tsh2.sym_type AS val_type, tsh2.sym_const AS val_val FROM web w, temporal_symbol_hash tsh1, temporal_symbol_hash tsh2 WHERE (w.attr=tsh1.id) AND (w.val_const=tsh2.id)" );
        vis_value_lti = prepare( "SELECT parent_id, tsh.sym_type AS attr_type, tsh.sym_const AS attr_val, val_lti FROM web w, temporal_symbol_hash tsh WHERE (w.attr=tsh.id) AND (val_lti IS NOT NULL)" );
    }

    
    private PreparedStatement prepare(String sql) throws SoarException
    {
        try
        {
            return db.prepareStatement(sql);
        }
        catch (SQLException e)
        {
            throw new SoarException("Failed to prepare statement '" + sql + "': " + e.getMessage(), e);
        }
    }
}
