package org.jsoar.kernel.smem;

import org.jsoar.kernel.SoarException;
import org.jsoar.kernel.memory.WorkingMemory;
import org.jsoar.kernel.symbols.IdentifierImpl;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Set;

/**
 * This implementation is intended to be used for testing. The only useful thing
 * it does is return false for epmem_enabled. All of the other calls will throw
 * a runtime exception.
 * 
 * @author ALT
 * 
 */
public class MockSmem implements SemanticMemory
{
    private static final Logger logger = LoggerFactory.getLogger(MockSmem.class);

    public MockSmem(){
        logger.warn("Instantiated MockSmem.  This will not perform any smem operations.");
    }

    @Override
    public boolean smem_enabled()
    {
        return false;
    }

    @Override
    public void smem_attach() throws SoarException{}

    @Override
    public long smem_lti_get_id(char name_letter, long name_number) throws SoarException
    {
        throw new UnsupportedOperationException();
    }

    @Override
    public IdentifierImpl smem_lti_soar_make(long lti, char name_letter, long name_number, int level)
    {
        throw new UnsupportedOperationException();
    }

    @Override
    public void smem_reset(IdentifierImpl state){}

    @Override
    public void smem_reset_id_counters() throws SoarException
    {
        throw new UnsupportedOperationException();
    }

    @Override
    public void smem_close() throws SoarException
    {
        throw new UnsupportedOperationException();
    }

    @Override
    public void smem_go(boolean store_only)
    {
        throw new UnsupportedOperationException();
    }

    @Override
    public void resetStatistics()
    {
        throw new UnsupportedOperationException();
    }

    @Override
    public SemanticMemoryStatistics getStatistics()
    {
        throw new UnsupportedOperationException();
    }

    @Override
    public boolean isMirroringEnabled()
    {
        throw new UnsupportedOperationException();
    }

    @Override
    public Set<IdentifierImpl> smem_changed_ids()
    {
        throw new UnsupportedOperationException();
    }

    @Override
    public boolean smem_ignore_changes()
    {
        throw new UnsupportedOperationException();
    }

    @Override
    public void initializeNewContext(WorkingMemory wm, IdentifierImpl id){}
}
