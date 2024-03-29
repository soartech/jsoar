/*
 * Copyright (c) 2010 Dave Ray <daveray@gmail.com>
 *
 * Created on Jun 23, 2010
 */
package org.jsoar.util;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.Statement;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

public class JdbcToolsTest
{
    
    @BeforeEach
    void setUp() throws Exception
    {
    }
    
    @AfterEach
    void tearDown() throws Exception
    {
    }
    
    @Test
    void testCanDetectIfATableExists() throws Exception
    {
        try(Connection conn = JdbcTools.connect("org.sqlite.JDBC", "jdbc:sqlite::memory:"))
        {
            assertFalse(JdbcTools.tableExists(conn, "people"));
            Statement stat = conn.createStatement();
            stat.executeUpdate("create table people (name, occupation);");
            assertTrue(JdbcTools.tableExists(conn, "people"));
        }
    }
    
    @Test
    void testCanCreateAndConnectToInMemorySqlLiteDatabase() throws Exception
    {
        try(Connection conn = JdbcTools.connect("org.sqlite.JDBC", "jdbc:sqlite::memory:"))
        {
            Statement stat = conn.createStatement();
            stat.executeUpdate("drop table if exists people;");
            stat.executeUpdate("create table people (name, occupation);");
            
            PreparedStatement prep = conn.prepareStatement("insert into people values (?, ?);");
            
            final String[][] entries = {
                    new String[] { "Gandhi", "politics" },
                    new String[] { "Turing", "computers" },
                    new String[] { "Wittgenstein", "smartypants" }
            };
            
            for(String[] entry : entries)
            {
                prep.setString(1, entry[0]);
                prep.setString(2, entry[1]);
                prep.addBatch();
            }
            
            conn.setAutoCommit(false);
            prep.executeBatch();
            conn.setAutoCommit(true);
            
            try(ResultSet rs = stat.executeQuery("select * from people;"))
            {
                int index = 0;
                while(rs.next())
                {
                    assertEquals(entries[index][0], rs.getString("name"));
                    assertEquals(entries[index][1], rs.getString("occupation"));
                    index++;
                }
            }
        }
    }
    
    @Test
    void testCanGetLastInsertedRowId() throws Exception
    {
        try(Connection conn = JdbcTools.connect("org.sqlite.JDBC", "jdbc:sqlite::memory:"))
        {
            Statement stat = conn.createStatement();
            stat.executeUpdate("drop table if exists people;");
            stat.executeUpdate("create table people (name, occupation);");
            
            final PreparedStatement prep = conn.prepareStatement("insert into people values (?, ?);");
            prep.setString(1, "foo");
            prep.setString(2, "bar");
            assertEquals(1, JdbcTools.insertAndGetRowId(prep));
            
            prep.setString(1, "yum");
            prep.setString(2, "bar");
            assertEquals(2, JdbcTools.insertAndGetRowId(prep));
            
            prep.setString(1, "baz");
            prep.setString(2, "bar");
            assertEquals(3, JdbcTools.insertAndGetRowId(prep));
        }
    }
    
}
