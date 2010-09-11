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
    static final String SMEM_SCHEMA = "smem3_";
    static final String SMEM_SIGNATURE = SMEM_SCHEMA + "signature";

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
    
    PreparedStatement ct_attr_check;
    PreparedStatement ct_const_check;
    PreparedStatement ct_lti_check;

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
    
    public SemanticMemoryDatabase(String driver, Connection db)
    {
        super(driver, db, SMEM_SIGNATURE);
        getFilterMap().put("@PREFIX@", SMEM_SCHEMA);
    }
}
