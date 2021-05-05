/*
 * Copyright (c) 2010 Dave Ray <daveray@gmail.com>
 *
 * Created on Jun 23, 2010
 */
package org.jsoar.kernel.smem;

import org.jsoar.util.db.AbstractSoarDatabase;
import org.jsoar.util.db.SoarPreparedStatement;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.sql.Statement;

/**
 * Database helper class for semantic memory.
 *
 * @author ray
 */
final class SemanticMemoryDatabase extends AbstractSoarDatabase {
    // empty table used to verify proper structure
    static final String SMEM_SCHEMA = "smem_";

    static final String SMEM_SCHEMA_VERSION = "2.0";

    static final String IN_MEMORY_PATH = ":memory:";

    // These are all the prepared statements for SMEM. They're filled in via reflection
    // from statements.properties.
    PreparedStatement begin;
    PreparedStatement commit;
    PreparedStatement rollback;

    SoarPreparedStatement backup;
    SoarPreparedStatement restore;

    PreparedStatement var_create;
    PreparedStatement var_get;
    PreparedStatement var_set;

    PreparedStatement hash_rev_int;
    PreparedStatement hash_rev_float;
    PreparedStatement hash_rev_str;
    PreparedStatement hash_rev_type;
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
    PreparedStatement lti_access_get;
    PreparedStatement lti_access_set;
    PreparedStatement lti_get_t;

    PreparedStatement web_add;
    PreparedStatement web_truncate;
    PreparedStatement web_expand;

    PreparedStatement web_all;

    PreparedStatement web_attr_all;
    PreparedStatement web_const_all;
    PreparedStatement web_lti_all;

    PreparedStatement web_attr_child;
    PreparedStatement web_const_child;
    PreparedStatement web_lti_child;

    PreparedStatement attribute_frequency_check;
    PreparedStatement wmes_constant_frequency_check;
    PreparedStatement wmes_lti_frequency_check;

    PreparedStatement attribute_frequency_add;
    PreparedStatement wmes_constant_frequency_add;
    PreparedStatement wmes_lti_frequency_add;

    PreparedStatement attribute_frequency_update;
    PreparedStatement wmes_constant_frequency_update;
    PreparedStatement wmes_lti_frequency_update;

    PreparedStatement attribute_frequency_get;
    PreparedStatement wmes_constant_frequency_get;
    PreparedStatement wmes_lti_frequency_get;

    PreparedStatement act_set;
    PreparedStatement act_lti_child_ct_set;
    PreparedStatement act_lti_child_ct_get;
    PreparedStatement act_lti_set;
    PreparedStatement act_lti_get;

    PreparedStatement history_get;
    PreparedStatement history_push;
    PreparedStatement history_add;

    PreparedStatement vis_lti;
    PreparedStatement vis_lti_act;
    PreparedStatement vis_value_const;
    PreparedStatement vis_value_lti;

    PreparedStatement set_schema_version;
    PreparedStatement get_schema_version;

    PreparedStatement drop_smem_persistent_variables;
    PreparedStatement drop_smem_symbols_type;
    PreparedStatement drop_smem_symbols_integer;
    PreparedStatement drop_smem_symbols_float;
    PreparedStatement drop_smem_symbols_string;
    PreparedStatement drop_smem_lti;
    PreparedStatement drop_smem_activation_history;
    PreparedStatement drop_smem_augmentations;
    PreparedStatement drop_smem_attribute_frequency;
    PreparedStatement drop_smem_wmes_constant_frequency;
    PreparedStatement drop_smem_wmes_lti_frequency;
    PreparedStatement drop_smem_ascii;

    public SemanticMemoryDatabase(String driver, Connection db) {
        super(driver, db);
        getFilterMap().put("@PREFIX@", SMEM_SCHEMA);
    }

    public void dropSmemTables() throws SQLException {
        drop_smem_persistent_variables.execute();
        drop_smem_symbols_type.execute();
        drop_smem_symbols_integer.execute();
        drop_smem_symbols_float.execute();
        drop_smem_symbols_string.execute();
        drop_smem_lti.execute();
        drop_smem_activation_history.execute();
        drop_smem_augmentations.execute();
        drop_smem_attribute_frequency.execute();
        drop_smem_wmes_constant_frequency.execute();
        drop_smem_wmes_lti_frequency.execute();
        drop_smem_ascii.execute();
    }

    public boolean backupDb(String fileName) throws SQLException {
        Connection connection = getConnection();

        if (connection.getAutoCommit()) {
            commit.execute();
            begin.execute();
        }

        // See sqlite-jdbc notes
        String query = backup.getQuery() + " \"" + fileName + "\"";
        try (Statement statement = connection.createStatement()) {
            statement.executeUpdate(query);
        }

        if (connection.getAutoCommit()) {
            commit.execute();
            begin.execute();
        }

        return true;
    }
}
