/*
 * Copyright (c) 2012 Dave Ray <daveray@gmail.com>
 *
 * Created on Mar 20, 2012
 */
package org.jsoar.kernel.epmem;

import android.test.AndroidTestCase;

import org.jsoar.kernel.Agent;
import org.jsoar.kernel.symbols.SymbolFactoryImpl;
import org.jsoar.util.JdbcTools;
import org.jsoar.util.adaptables.AdaptableContainer;
import org.jsoar.util.properties.PropertyManager;

import java.sql.Connection;

public class DefaultEpisodicMemoryTest extends AndroidTestCase
{
    private AdaptableContainer context;
    private Connection conn;
    private DefaultEpisodicMemory epmem;
    
    @Override
    public void setUp() throws Exception
    {
        Agent temp = new Agent(getContext());
        context = AdaptableContainer.from(new SymbolFactoryImpl(), new PropertyManager(), temp);
        conn = JdbcTools.connect("org.sqldroid.SQLDroidDriver", "jdbc:sqlite::memory:");
//        final EpisodicMemoryDatabase db = new EpisodicMemoryDatabase("org.sqlite.JDBC", conn);
//        db.structure();
//        db.prepare();
        epmem = new DefaultEpisodicMemory(context, getContext());
        epmem.initialize();
    }
    
    @Override
    public void tearDown() throws Exception
    {
        conn.close();
    }

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
