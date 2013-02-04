package org.jsoar.kernel.epmem;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import java.net.URL;
import java.sql.Connection;
import java.sql.DatabaseMetaData;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.List;

import org.jsoar.kernel.Agent;
import org.jsoar.kernel.Decider;
import org.jsoar.kernel.RunType;
import org.jsoar.kernel.SoarException;
import org.jsoar.kernel.SoarProperties;
import org.jsoar.kernel.rhs.functions.AbstractRhsFunctionHandler;
import org.jsoar.kernel.rhs.functions.RhsFunctionContext;
import org.jsoar.kernel.rhs.functions.RhsFunctionException;
import org.jsoar.kernel.rhs.functions.RhsFunctionHandler;
import org.jsoar.kernel.symbols.Symbol;
import org.jsoar.kernel.symbols.SymbolFactory;
import org.jsoar.util.JdbcTools;
import org.jsoar.util.adaptables.Adaptables;
import org.jsoar.util.properties.PropertyManager;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

public class EpMemSaveToDbTests
{
    protected Agent agent;
    
    private boolean halted = false;
    private boolean failed = false;
        
    // sources rules
    protected void runTestSetup(String testName) throws SoarException
    {
        String sourceName = getClass().getSimpleName() + "_" + testName + ".soar";
        URL sourceUrl = getClass().getResource(sourceName);
        assertNotNull("Could not find test file " + sourceName, sourceUrl);
        agent.getInterpreter().source(sourceUrl);
    }
    
    // this function assumes some other function has set up the agent (like runTestSetup)
    protected void runTestExecute(String testName, int expectedDecisions) throws Exception
    {
        if(expectedDecisions >= 0)
        {
            agent.runFor(expectedDecisions + 1, RunType.DECISIONS);
        }
        else
        {
            agent.runForever();
        }
        
        assertTrue(testName + " functional test did not halt", halted);
        assertFalse(testName + " functional test failed", failed);
        if(expectedDecisions >= 0)
        {
            assertEquals(expectedDecisions, agent.getProperties().get(SoarProperties.D_CYCLE_COUNT).intValue()); // deterministic!
        }
        
        agent.getInterpreter().eval("stats");
        
    }
    protected void runTest(String testName, int expectedDecisions) throws Exception
    {
        runTestSetup(testName);  
        runTestExecute(testName, expectedDecisions);
    }
    
    /**
     * @throws java.lang.Exception
     */
    @Before
    public void setUp() throws Exception
    {
        halted = false;
        failed = false;
        agent = new Agent();
        agent.getTrace().enableAll();
        agent.initialize();
        
        agent.getTrace().disableAll();
        //agent.trace.setEnabled(Category.TRACE_CONTEXT_DECISIONS_SYSPARAM, true);
        //agent.trace.setEnabled(false);
        
        // set up the agent with common RHS functions
        final RhsFunctionHandler oldHalt = agent.getRhsFunctions().getHandler("halt");
        assertNotNull(oldHalt);     
        
        agent.getRhsFunctions().registerHandler(new AbstractRhsFunctionHandler("halt") {

            @Override
            public Symbol execute(RhsFunctionContext rhsContext, List<Symbol> arguments) throws RhsFunctionException
            {
                halted = true;
                return oldHalt.execute(rhsContext, arguments);
            }});
        agent.getRhsFunctions().registerHandler(new AbstractRhsFunctionHandler("failed") {

            @Override
            public Symbol execute(RhsFunctionContext rhsContext, List<Symbol> arguments) throws RhsFunctionException
            {
                halted = true;
                failed = true;
                return oldHalt.execute(rhsContext, arguments);
            }});
        agent.getRhsFunctions().registerHandler(new AbstractRhsFunctionHandler("succeeded") {

            @Override
            public Symbol execute(RhsFunctionContext rhsContext, List<Symbol> arguments) throws RhsFunctionException
            {
                halted = true;
                failed = false;
                return oldHalt.execute(rhsContext, arguments);
            }});
    }

    /**
     * @throws java.lang.Exception
     */
    @After
    public void tearDown() throws Exception
    {
        agent.getPrinter().flush();
        agent.dispose();
    }
    
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
        final PropertyManager properties = Adaptables.adapt(agent, PropertyManager.class);
        final SymbolFactory symbols = Adaptables.adapt(agent, SymbolFactory.class);
        final DefaultEpisodicMemoryParams params = new DefaultEpisodicMemoryParams(properties, symbols);
        final String jdbcUrl = params.protocol.get() + ":" + params.path.get();
        final Connection connection = JdbcTools.connect(params.driver.get(), jdbcUrl);
        final EpisodicMemoryDatabase db = new EpisodicMemoryDatabase(params.driver.get(), connection);
        
        final Connection conn = db.getConnection();
        final PreparedStatement ps = conn.prepareStatement("SELECT * FROM "+EpisodicMemoryDatabase.EPMEM_SCHEMA+"vars WHERE id=?");
        ps.setLong(1, 0);
        ResultSet rs = ps.executeQuery();
        rs.next();
        long value = rs.getLong("value");
        assertTrue(value == -1);
        
        ps.setLong(1, 1);
        rs = ps.executeQuery();
        rs.next();
        value = rs.getLong("value");
        assertTrue(value == 0);
        
    }
}
