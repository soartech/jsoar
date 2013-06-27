package org.jsoar.kernel.epmem;

import static org.junit.Assert.assertTrue;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.ArrayList;
import java.util.List;

import org.jsoar.kernel.FunctionalTestHarness;
import org.jsoar.util.adaptables.Adaptables;
import org.junit.Test;

public class EpMemSaveToDbTests extends FunctionalTestHarness
{   
    protected Connection getConnection()
    {
        final DefaultEpisodicMemory epmem = Adaptables.adapt(agent, DefaultEpisodicMemory.class);
        final EpisodicMemoryDatabase db = epmem.db;
        
        return db.getConnection();
    }
    
    @Test
    public void testPersistentVariablesTable() throws Exception
    {
        runTest("store", 2);
        /* this data is expected in vars:
         * id  value
         * 0    -1
         * 1   0
         * 2   1
         * 3   2147483647
         * 4   -1
         * 5   0
         * 6   1
         * 7   2147483647
         * 8   1
         */
        
        /*
         * List index by id containing values
         */
        final List<Long> expectedVals = new ArrayList<Long>();
        expectedVals.add(-1L);
        expectedVals.add(0L);
        expectedVals.add(1L);
        expectedVals.add(Long.MAX_VALUE);
        expectedVals.add(-1L);
        expectedVals.add(0L);
        expectedVals.add(1L);
        expectedVals.add(Long.MAX_VALUE);
        expectedVals.add(1L);
        
        final PreparedStatement ps = getConnection().prepareStatement("SELECT * FROM "+EpisodicMemoryDatabase.EPMEM_SCHEMA+"epmem_persistent_variables WHERE variable_id=?");
        
        ResultSet rs;
        long value;

        for (int id = 0; id < expectedVals.size(); id++)
        {
            ps.setLong(1, id);
            rs = ps.executeQuery();
            rs.next();
            value = rs.getLong("variable_value");
            assertTrue("variable_id "+id+" is "+value+", expected "+expectedVals.get(id), value == (long)expectedVals.get(id));
            expectedVals.remove((Integer)id);
        }
    }
    
    @Test
    public void testSymbolsStringTable() throws Exception
    {
        runTest("store", 2);
        /*
         * this data is expected in epmem_symbols_string:
         * s_id  symbol_value
         * 0     root      
         * 1     operator*
         */
        
        List<String> symbolsString = new ArrayList<String>();
        symbolsString.add("root");
        symbolsString.add("operator*");
        
        
        final PreparedStatement ps = getConnection()
                .prepareStatement("SELECT * FROM "+EpisodicMemoryDatabase.EPMEM_SCHEMA+"epmem_symbols_string WHERE s_id=?");
        
        ResultSet rs;
        String sym_const;
        
        for (int id = 0; id < symbolsString.size(); id++)
        {
            ps.setLong(1, id);
            rs = ps.executeQuery();
            rs.next();
            sym_const = rs.getString("symbol_value");
            if(sym_const == null)
            {
                assertTrue("id " + id + " is " + sym_const + ", expected " + symbolsString.get(id),
                        symbolsString.get(id) == null);
            }
            else
            {
                assertTrue("id " + id + " is " + sym_const + ", expected " + symbolsString.get(id),
                        symbolsString.get(id).equals(sym_const));
            }
            symbolsString.remove(id);
        }
    }
}
