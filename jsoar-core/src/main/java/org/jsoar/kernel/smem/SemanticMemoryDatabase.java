/*
 * Copyright (c) 2010 Dave Ray <daveray@gmail.com>
 *
 * Created on Jun 23, 2010
 */
package org.jsoar.kernel.smem;

import java.sql.Connection;
import java.sql.PreparedStatement;

import org.jsoar.util.db.AbstractSoarDatabase;

/**
 * Database helper class for semantic memory.
 * 
 * @author ray
 */
final class SemanticMemoryDatabase extends AbstractSoarDatabase
{
    // empty table used to verify proper structure
    static final String SMEM_SCHEMA = "smem2_";
    static final String SMEM_SIGNATURE = SMEM_SCHEMA + "smem_signature";
    public static final String SMEM_SCHEMA_VERSION = "2.0";

    // These are all the prepared statements for SMEM. They're filled in via reflection
    // from statements.properties.
    PreparedStatement begin;
    PreparedStatement commit;
    PreparedStatement rollback;

    PreparedStatement var_create;
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
    
    public SemanticMemoryDatabase(String driver, Connection db)
    {
        super(driver, db, SMEM_SIGNATURE);
        getFilterMap().put("@PREFIX@", SMEM_SCHEMA);
    }
}
