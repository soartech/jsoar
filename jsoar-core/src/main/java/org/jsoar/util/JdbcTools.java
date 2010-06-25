/*
 * Copyright (c) 2010 Dave Ray <daveray@gmail.com>
 *
 * Created on Jun 23, 2010
 */
package org.jsoar.util;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;

import org.jsoar.kernel.SoarException;

/**
 * @author ray
 */
public class JdbcTools
{
    public static Connection connect(String klass, String spec) throws SoarException
    {
        try
        {
            Class.forName(klass);
        }
        catch (ClassNotFoundException e)
        {
            throw new SoarException("Failed to load database driver class: " + e.getMessage(), e);
        }
        
        try
        {
            return DriverManager.getConnection(spec);
        }
        catch (SQLException e)
        {
            throw new SoarException("Failed to connect to database '" + spec + "': " + e.getMessage(), e);
        }
    }
    
    public static void executeSql(Connection db, InputStream is) throws SoarException, IOException
    {
        try
        {
            final Statement s = db.createStatement();
            final BufferedReader reader = new BufferedReader(new InputStreamReader(is));
            String line = reader.readLine();
            while(line != null)
            {
                line = line.trim();
                if(!line.isEmpty() && !line.startsWith("#"))
                {
                    s.addBatch(line);
                }
                
                line = reader.readLine();
            }
            db.setAutoCommit(false);
            s.executeBatch();
            db.setAutoCommit(true);
        }
        catch (SQLException e)
        {
            throw new SoarException("Sql error: " + e.getMessage(), e);
        }
        finally
        {
            is.close();
        }
    }
    
    public static long insertAndGetRowId(PreparedStatement s) throws SQLException
    {
        s.executeUpdate();
        final ResultSet keySet = s.getGeneratedKeys();
        try
        {
            if(keySet.next())
            {
                return keySet.getLong(1);
            }
            else
            {
                return 0;
            }
        }
        finally
        {
            keySet.close();
        }
    }
}
