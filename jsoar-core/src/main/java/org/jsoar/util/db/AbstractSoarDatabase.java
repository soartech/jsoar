/*
 * Copyright (c) 2010 Dave Ray <daveray@gmail.com>
 *
 * Created on Sep 11, 2010
 */
package org.jsoar.util.db;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.lang.reflect.Field;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.HashMap;
import java.util.Map;
import java.util.Properties;

import org.jsoar.kernel.SoarException;
import org.jsoar.util.JdbcTools;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.io.ByteStreams;

/**
 * Base class for Soar databases like SMEM and EPMEM.
 * 
 * @author ray
 */
public abstract class AbstractSoarDatabase
{
    private final String driver;
    private final Connection db;
    private final Properties statements = new Properties();
    private final Map<String, String> filterMap = new HashMap<String, String>();
    
    private static final Logger LOG = LoggerFactory.getLogger(AbstractSoarDatabase.class);
    
    // These are all the prepared statements shared by Soar databases. They're filled in via reflection
    // from the statements.properties for the specific database
    
    protected PreparedStatement begin;
    protected PreparedStatement commit;
    protected PreparedStatement rollback;
    
    protected SoarPreparedStatement backup;
    protected SoarPreparedStatement restore;
    
    /**
     * Construct a new database instance.
     * 
     * @param driver the driver name
     * @param db the database connection
     */
    public AbstractSoarDatabase(String driver, Connection db)
    {
        this.driver = driver;
        this.db = db;
    }
    
    /**
     * @return the driver name
     */
    public String getDriver()
    {
        return driver;
    }
    
    /**
     * @return the database connection
     */
    public Connection getConnection()
    {
        return db;
    }
    
    /**
     * @return the filter map used to filter resources. Additional replacement
     * entries can be added to this map.
     */
    public Map<String, String> getFilterMap()
    {
        return filterMap;
    }
    
    /**
     * Load and prepare statements.
     * 
     * @throws SoarException
     * @throws IOException
     */
    public void prepare() throws SoarException, IOException
    {
        loadStatementsFromResource("statements.properties", true);
        loadStatementsFromResource(driver + ".statements.properties", false);
        assignStatements();
    }
    
    /**
     * Sets up initial database structures if not already present.
     * 
     * @return true if the database was initialize, false if it already existed
     * @throws SoarException
     * @throws IOException
     */
    public boolean structure() throws SoarException, IOException
    {
        // Load the database structure by executing structures.sql
        final InputStream is = filter(getClass().getResourceAsStream("structures.sql"), getFilterMap());
        if(is == null)
        {
            throw new FileNotFoundException("Failed to open '" + getResourcePath("structures.sql") + "' resource");
        }
        try
        {
            JdbcTools.executeSqlBatch(getConnection(), is, getDriver());
        }
        catch(Exception e)
        {
            LOG.error("Failed to created database", e);
        }
        finally
        {
            is.close();
        }
        
        return true;
    }
    
    private String getResourcePath(String name)
    {
        return "/" + getClass().getPackage().getName().replace('.', '/') + "/" + name;
    }
    
    private void loadStatementsFromResource(String resource, boolean required) throws IOException
    {
        InputStream is = filter(getClass().getResourceAsStream(resource), filterMap);
        if(is == null)
        {
            if(required)
            {
                throw new FileNotFoundException("Failed to open '" + getResourcePath(resource) + "' resource");
            }
            return;
        }
        try
        {
            // Overwrite here rather than useing Properties delegation because
            // we want to be able to iterate over the property keys. :(
            statements.load(is);
        }
        finally
        {
            is.close();
        }
    }
    
    private void assignStatements() throws SoarException
    {
        for(Object name : statements.keySet())
        {
            assignStatement(name.toString());
        }
    }
    
    private void assignStatement(String name) throws SoarException
    {
        try
        {
            assignStatementInternal(name);
        }
        catch(SecurityException | IllegalArgumentException | IllegalAccessException | NoSuchFieldException e)
        {
            throw new SoarException("While assigning statement field '" + name + "': " + e.getMessage(), e);
        }
    }
    
    private void assignStatementInternal(String name) throws IllegalArgumentException, IllegalAccessException, SoarException, NoSuchFieldException, SecurityException
    {
        try
        {
            // Reflectively find the field and assign the prepared statement
            // to it.
            final Field field = getClass().getDeclaredField(name);
            setField(field);
        }
        catch(NoSuchFieldException e)
        {
            // check the superclass (i.e., this class)
            final Field field = getClass().getSuperclass().getDeclaredField(name);
            setField(field);
        }
    }
    
    private void setField(Field field) throws SoarException, IllegalArgumentException, IllegalAccessException
    {
        // This is necessary since we're trying to set a possibly non-public
        // field in a sub-class. Another option is to add a protected
        // abstract method, implemented by the sub-class that sets the field.
        // This works for now.
        field.setAccessible(true);
        PreparedStatement ps = prepareNamedStatement(field.getName());
        field.set(this, ps);
    }
    
    private PreparedStatement prepareNamedStatement(String name) throws SoarException
    {
        final String sql = statements.getProperty(name);
        if(sql == null)
        {
            throw new SoarException("Could not find statement '" + name + "'");
        }
        try
        {
            // See sqlite-jdbc notes
            final String trimmed = sql.trim();
            if(trimmed.startsWith("INSERT"))
            {
                return new SoarPreparedStatement(db.prepareStatement(trimmed, Statement.RETURN_GENERATED_KEYS), trimmed);
            }
            else if(trimmed.startsWith("backup") || trimmed.startsWith("restore"))
            {
                return new SoarPreparedStatement(trimmed);
            }
            else
            {
                return new SoarPreparedStatement(db.prepareStatement(trimmed), trimmed);
            }
        }
        catch(SQLException e)
        {
            throw new SoarException("Failed to prepare statement '" + sql + "': " + e.getMessage(), e);
        }
    }
    
    private static InputStream filter(InputStream in, Map<String, String> replacements) throws IOException
    {
        if(in == null)
        {
            return null;
        }
        
        final ByteArrayOutputStream temp = new ByteArrayOutputStream();
        try
        {
            ByteStreams.copy(in, temp);
        }
        finally
        {
            in.close();
        }
        
        String tempString = temp.toString("UTF-8");
        for(Map.Entry<String, String> entry : replacements.entrySet())
        {
            tempString = tempString.replace(entry.getKey(), entry.getValue());
        }
        
        return new ByteArrayInputStream(tempString.getBytes("UTF-8"));
    }
    
    public boolean backupDb(String fileName) throws SQLException
    {
        boolean returnValue = false;
        
        if(this.getConnection().getAutoCommit())
        {
            commit.execute();
            begin.execute();
        }
        
        // See sqlite-jdbc notes
        File file = new File(fileName);
        String query = backup.getQuery() + " \"" + file.getAbsolutePath() + "\"";
        try(Statement s = this.getConnection().createStatement())
        {
            s.executeUpdate(query);
        }
        
        returnValue = true;
        
        if(this.getConnection().getAutoCommit())
        {
            commit.execute();
            begin.execute();
        }
        
        return returnValue;
    }
    
    public int beginExecuteUpdate() throws SQLException
    {
        return this.begin.executeUpdate();
    }
    
    public int commitExecuteUpdate() throws SQLException
    {
        return this.commit.executeUpdate();
    }
    
    public int rollbackExecuteUpdate() throws SQLException
    {
        return this.rollback.executeUpdate();
    }
    
    public int backupExecuteUpdate() throws SQLException
    {
        return this.backup.executeUpdate();
    }
    
    public int restoreExecuteUpdate() throws SQLException
    {
        return this.restore.executeUpdate();
    }
    
}
