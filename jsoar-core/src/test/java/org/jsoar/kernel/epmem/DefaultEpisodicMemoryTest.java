/*
 * Copyright (c) 2012 Dave Ray <daveray@gmail.com>
 *
 * Created on Mar 20, 2012
 */
package org.jsoar.kernel.epmem;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;

import java.sql.Connection;

import org.jsoar.kernel.Agent;
import org.jsoar.kernel.symbols.SymbolFactoryImpl;
import org.jsoar.util.JdbcTools;
import org.jsoar.util.adaptables.AdaptableContainer;
import org.jsoar.util.properties.PropertyManager;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

public class DefaultEpisodicMemoryTest
{
    private AdaptableContainer context;
    private Connection conn;
    private DefaultEpisodicMemory epmem;
    
    @BeforeEach
    public void setUp() throws Exception
    {
        Agent temp = new Agent();
        context = AdaptableContainer.from(new SymbolFactoryImpl(), new PropertyManager(), temp);
        conn = JdbcTools.connect("org.sqlite.JDBC", "jdbc:sqlite::memory:");
//        final EpisodicMemoryDatabase db = new EpisodicMemoryDatabase("org.sqlite.JDBC", conn);
//        db.structure();
//        db.prepare();
        epmem = new DefaultEpisodicMemory(context);
        epmem.initialize();
    }
    
    @AfterEach
    public void tearDown() throws Exception
    {
        conn.close();
    }

    @Test
    public void testCanInitializeTheDatabase() throws Exception
    {
//        final DefaultEpisodicMemory epmem = new DefaultEpisodicMemory(context);
//        epmem.initialize();
        // TODO database is being initialized here, should it be somewhere else?
//        assertNull(epmem.getDatabase());
        epmem.epmem_init_db();
        assertNotNull(epmem.getDatabase());
        assertFalse(epmem.getDatabase().getConnection().isClosed());
    }

}
