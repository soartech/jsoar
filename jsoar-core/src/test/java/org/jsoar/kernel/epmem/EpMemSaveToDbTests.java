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
    @Test
    public void testCountEpMem() throws Exception
    {
        runTest("store", 2);
        /* this data is expected in vars:
id  value
0    -1
1   0
2   1
3   2147483647
4   -1
5   0
6   1
7   2147483647
8   1
         */
        
        /*
         * List index by id containing values
         */
        final List<Long> expectedVals = new ArrayList<Long>();
        expectedVals.add(-1L);
        expectedVals.add(0L);
        expectedVals.add(1L);
        expectedVals.add(2147483647L);
        expectedVals.add(-1L);
        expectedVals.add(0L);
        expectedVals.add(1L);
        expectedVals.add(2147483647L);
        expectedVals.add(1L);
        
        final DefaultEpisodicMemory epmem = Adaptables.adapt(agent, DefaultEpisodicMemory.class);
        final EpisodicMemoryDatabase db = epmem.db;
        
        final Connection conn = db.getConnection();
        final PreparedStatement ps = conn.prepareStatement("SELECT * FROM "+EpisodicMemoryDatabase.EPMEM_SCHEMA+"vars WHERE id=?");
        ps.setLong(1, 0);
        ResultSet rs;
        long value;
        
        for(int id=0; id<expectedVals.size(); id++)
        {
            ps.setLong(1, id);
            rs = ps.executeQuery();
            rs.next();
            value = rs.getLong("value");
            assertTrue("id "+id+" is "+value+", expected "+expectedVals.get(id), value == (long)expectedVals.get(id));
        }
    }
}
