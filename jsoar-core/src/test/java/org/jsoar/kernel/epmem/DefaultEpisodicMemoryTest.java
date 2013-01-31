/*
 * Copyright (c) 2012 Dave Ray <daveray@gmail.com>
 *
 * Created on Mar 20, 2012
 */
package org.jsoar.kernel.epmem;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;

import java.sql.Connection;

import org.jsoar.kernel.Agent;
import org.jsoar.kernel.symbols.SymbolFactoryImpl;
import org.jsoar.util.JdbcTools;
import org.jsoar.util.adaptables.AdaptableContainer;
import org.jsoar.util.properties.PropertyManager;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

public class DefaultEpisodicMemoryTest
{
    private AdaptableContainer context;
    private Connection conn;
    private DefaultEpisodicMemory epmem;
    
    @Before
    public void setUp() throws Exception
    {
        context = AdaptableContainer.from(new SymbolFactoryImpl(), new PropertyManager(), new Agent());
        conn = JdbcTools.connect("org.sqlite.JDBC", "jdbc:sqlite::memory:");
        final EpisodicMemoryDatabase db = new EpisodicMemoryDatabase("org.sqlite.JDBC", conn);
        db.structure();
        db.prepare();
        epmem = new DefaultEpisodicMemory(context, db);
        epmem.initialize();
    }
    
    @After
    public void tearDown() throws Exception
    {
        conn.close();
    }

    @Test
    public void testCanInitializeTheDatabase() throws Exception
    {
        final DefaultEpisodicMemory epmem = new DefaultEpisodicMemory(context);
        epmem.initialize();
        // TODO database is being initialized here, should it be somewhere else?
//        assertNull(epmem.getDatabase());
        epmem.epmem_init_db();
        assertNotNull(epmem.getDatabase());
        assertFalse(epmem.getDatabase().getConnection().isClosed());
    }

}
