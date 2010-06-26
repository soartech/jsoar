/*
 * Copyright (c) 2010 Dave Ray <daveray@gmail.com>
 *
 * Created on Jun 24, 2010
 */
package org.jsoar.kernel.smem;

import static org.junit.Assert.*;

import java.sql.Connection;

import org.jsoar.kernel.symbols.SymbolFactoryImpl;
import org.jsoar.util.JdbcTools;
import org.jsoar.util.adaptables.AdaptableContainer;
import org.jsoar.util.adaptables.Adaptables;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

public class DefaultSemanticMemoryTest
{
    private AdaptableContainer context;
    private Connection conn;
    private DefaultSemanticMemory smem;
    
    @Before
    public void setUp() throws Exception
    {
        context = AdaptableContainer.from(new SymbolFactoryImpl());
        conn = JdbcTools.connect("org.sqlite.JDBC", "jdbc:sqlite::memory:");
        final SemanticMemoryDatabase db = new SemanticMemoryDatabase(conn);
        db.structure();
        db.prepare();
        smem = new DefaultSemanticMemory(context, db);
    }

    @After
    public void tearDown() throws Exception
    {
        conn.close();
    }

    @Test
    public void testCanAddALongTermIdentifier() throws Exception
    {
        final long lti = smem.smem_lti_add_id('Z', 99);
        assertEquals(1, lti);
    }
    
    @Test
    public void testCanRetrieveALongTermIdentifier() throws Exception
    {
        final long expected = smem.smem_lti_add_id('Z', 99);
        assertEquals(expected, smem.smem_lti_get_id('Z', 99));
        
        final long expected2 = smem.smem_lti_add_id('S', 2);
        assertEquals(expected2, smem.smem_lti_get_id('S', 2));
        assertFalse(expected == expected2);
    }

    @Test
    public void testCanResetIdCountersInSymbolFactory() throws Exception
    {
        long number = 1; 
        for(char letter = 'A'; letter <= 'Z'; letter++)
        {
            smem.smem_lti_add_id(letter, number++);
        }
        
        final SymbolFactoryImpl syms = Adaptables.adapt(context, SymbolFactoryImpl.class);
        smem.smem_reset_id_counters();
        
        long expected_number = 1;
        for(char letter = 'A'; letter <= 'Z'; letter++)
        {
            assertEquals(expected_number + 1, syms.getIdNumber(letter));
            expected_number++;
        }
    }
    
    @Test
    public void testCanInitializeTheDatabase() throws Exception
    {
        final DefaultSemanticMemory smem = new DefaultSemanticMemory(context);
        assertNull(smem.getDatabase());
        smem.smem_attach();
        assertNotNull(smem.getDatabase());
        assertFalse(smem.getDatabase().getConnection().isClosed());
    }
}
