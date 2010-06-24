/*
 * Copyright (c) 2010 Dave Ray <daveray@gmail.com>
 *
 * Created on Jun 23, 2010
 */
package org.jsoar.util;

import static org.junit.Assert.*;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.Statement;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

public class JdbcToolsTest
{

    @Before
    public void setUp() throws Exception
    {
    }

    @After
    public void tearDown() throws Exception
    {
    }

    @Test
    public void testCanCreateAndConnectToInMemorySqlLiteDatabase() throws Exception
    {
        final Connection conn = JdbcTools.connect("org.sqlite.JDBC", "jdbc:sqlite::memory:");
        try
        {
            Statement stat = conn.createStatement();
            stat.executeUpdate("drop table if exists people;");
            stat.executeUpdate("create table people (name, occupation);");
            
            PreparedStatement prep = conn.prepareStatement("insert into people values (?, ?);");

            final String[][] entries = new String[][] {
                new String[] {"Gandhi", "politics" },
                new String[] {"Turing", "computers" },
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

            final ResultSet rs = stat.executeQuery("select * from people;");
            int index = 0;
            while (rs.next()) 
            {
                assertEquals(entries[index][0], rs.getString("name"));
                assertEquals(entries[index][1], rs.getString("occupation"));
                index++;
            }
            rs.close();
        }
        finally
        {
            conn.close();
        }
    }

}
