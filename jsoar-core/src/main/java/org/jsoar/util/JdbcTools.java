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
import java.io.PrintWriter;
import java.io.Writer;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.sql.SQLException;
import java.sql.Statement;

import org.jsoar.kernel.SoarException;

/**
 * JDBC helpers.
 * 
 * @author ray
 */
public class JdbcTools
{
    /**
     * Open a JDBC connection.
     * 
     * @param klass the driver class, e.g. org.sqlite.JDBC
     * @param jdbcUrl the JDBC url
     * @return an open JDBC connection
     * @throws SoarException
     */
    public static Connection connect(String klass, String jdbcUrl) throws SoarException
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
            return DriverManager.getConnection(jdbcUrl);
        }
        catch (SQLException e)
        {
            throw new SoarException("Failed to connect to database '" + jdbcUrl + "': " + e.getMessage(), e);
        }
    }
    
    private static String filterLine(String driverFilter, String line)
    {
        if(driverFilter == null || !line.startsWith("["))
        {
            return line;
        }
        final String prefix = "[" + driverFilter + "]";
        if(line.startsWith(prefix))
        {
            // Strip off prefix and return rest of line
            return line.substring(prefix.length());
        }
        // Filter failed, return nothing
        return null;
    }
    
    /**
     * Execute SQL statements from an input stream.
     * 
     * <p>One statement per line is expected. Blank lines and lines that start 
     * with {@code #} are ignored.
     *  
     * @param db the database connection
     * @param is the input stream
     * @throws SoarException
     * @throws IOException
     */
    public static void executeSql(Connection db, InputStream is, String driverFilter) throws SoarException, IOException
    {
        try
        {
            final BufferedReader reader = new BufferedReader(new InputStreamReader(is));
            String line = reader.readLine();
            while(line != null)
            {
                line = line.trim();
                if(!line.isEmpty() && !line.startsWith("#"))
                {
                    line = filterLine(driverFilter, line);
                    if(line != null)
                    {
                        final Statement s = db.createStatement();
                        try
                        {
                            s.execute(line);
                        }
                        finally
                        {
                            s.close();
                        }
                    }
                }
                
                line = reader.readLine();
            }
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
    /**
     * Execute SQL statements from an input stream.
     * 
     * <p>One statement per line is expected. Blank lines and lines that start 
     * with {@code #} are ignored.
     *  
     * @param db the database connection
     * @param is the input stream
     * @throws SoarException
     * @throws IOException
     */
    public static void executeSqlBatch(Connection db, InputStream is, String driverFilter) throws SoarException, IOException
    {
        try
        {
            final Statement s = db.createStatement();
            try
            {
                final BufferedReader reader = new BufferedReader(new InputStreamReader(is));
                String line = reader.readLine();
                while(line != null)
                {
                    line = line.trim();
                    if(!line.isEmpty() && !line.startsWith("#"))
                    {
                        line = filterLine(driverFilter, line);
                        if(line != null)
                        {
                            s.addBatch(line);
                        }
                    }
                    
                    line = reader.readLine();
                }
                db.setAutoCommit(false);
                s.executeBatch();
                db.setAutoCommit(true);
            }
            finally
            {
                s.close();
            }
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
    
    /**
     * Execute a prepared statement and get the row id generated by it.
     * 
     * @param s the statement to execute
     * @return the row id
     * @throws SQLException
     */
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
                throw new IllegalStateException("getGeneratedKeys() returned empty result set for '" + s + "'");
            }
        }
        finally
        {
            keySet.close();
        }
    }
    
    /**
     * Return true if a table exists in the database
     * 
     * @param db the database connection
     * @param table the name of the table
     * @return true if the table exists
     * @throws SQLException
     */
    public static boolean tableExists(Connection db, String table) throws SQLException
    {
        final ResultSet rs = db.getMetaData().getTables(null, null, null, new String[] {"TABLE"});
        try
        {
            while(rs.next())
            {
                if(table.equals(rs.getString("TABLE_NAME")))
                {
                    return true;
                }
            }
            return false;
        }
        finally
        {
            rs.close();
        }
    }
    
    /**
     * Print a result set to a writer. No real attempt is made to format the table nicely.
     * 
     * @param rs the result set
     * @param out the writer
     * @throws SQLException
     */
    public static void printResultSet(ResultSet rs, Writer out) throws SQLException
    {
        final PrintWriter p = new PrintWriter(out);
        final ResultSetMetaData md = rs.getMetaData();
        for(int c = 1; c <= md.getColumnCount(); c++)
        {
            p.printf("| %10s ", md.getColumnLabel(c));
        }
        p.println("|");
        
        // ResultSet.getRows() isn't implemented in xerial sqlite :(
        int rows = 0;
        while(rs.next())
        {
            for(int c = 1; c <= md.getColumnCount(); c++)
            {
                p.printf("| %10s ", rs.getObject(c));
            }
            p.println("|");
            rows++;
        }
        p.printf("%d row%s", rows, rows != 1 ? "s" : "");
        p.println();
        p.flush();
        
    }
}
