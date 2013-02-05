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
    public void testVarsTable() throws Exception
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
        
        final PreparedStatement ps = getConnection().prepareStatement("SELECT * FROM "+EpisodicMemoryDatabase.EPMEM_SCHEMA+"vars WHERE id=?");
        
        ResultSet rs;
        long value;

        for (int id = 0; id < expectedVals.size(); id++)
        {
            ps.setLong(1, id);
            rs = ps.executeQuery();
            rs.next();
            value = rs.getLong("value");
            assertTrue("id "+id+" is "+value+", expected "+expectedVals.get(id), value == (long)expectedVals.get(id));
        }
    }
    
    protected class TemporalSymbol
    {
        public TemporalSymbol(String cont, int type)
        {
            sym_const = cont;
            sym_type = type;
        }
        public String sym_const;
        public int sym_type;
    }
    
    @Test
    public void testTemporalSymbolHashTable() throws Exception
    {
        runTest("store", 2);
        /*
         * this data is expected in temporal_symbol_hash:
         * id  sym_const  sym_type
         * 0              1
         * 1   operator*  2
         */
        
        List<TemporalSymbol> temporalSymbols = new ArrayList<TemporalSymbol>();
        temporalSymbols.add(new TemporalSymbol(null, 1));
        temporalSymbols.add(new TemporalSymbol("operator*", 2));
        
        
        final PreparedStatement ps = getConnection()
                .prepareStatement("SELECT * FROM "+EpisodicMemoryDatabase.EPMEM_SCHEMA+"temporal_symbol_hash WHERE id=?");
        
        ResultSet rs;
        String sym_const;
        int sym_type;
        
        for (int id = 0; id < temporalSymbols.size(); id++)
        {
            ps.setLong(1, id);
            rs = ps.executeQuery();
            rs.next();
            sym_const = rs.getString("sym_const");
            sym_type = rs.getInt("sym_type");
            if(sym_const == null)
            {
                assertTrue("id " + id + " is " + sym_const + ", expected " + temporalSymbols.get(id).sym_const,
                        temporalSymbols.get(id).sym_const == null);
            }
            else
            {
                assertTrue("id " + id + " is " + sym_const + ", expected " + temporalSymbols.get(id).sym_const,
                        temporalSymbols.get(id).sym_const.equals(sym_const));
            }
            assertTrue("id "+id+" is "+sym_type+", expected "+temporalSymbols.get(id).sym_type, 
                    temporalSymbols.get(id).sym_type == sym_type);
        }
    }
}
