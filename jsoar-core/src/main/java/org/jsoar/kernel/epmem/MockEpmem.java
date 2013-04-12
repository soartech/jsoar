package org.jsoar.kernel.epmem;

import org.jsoar.kernel.SoarException;
import org.jsoar.kernel.memory.WmeImpl;
import org.jsoar.kernel.memory.WorkingMemory;
import org.jsoar.kernel.symbols.IdentifierImpl;

/**
 * This implementation is intended to be used for testing. The only useful thing
 * it does is return false for epmem_enabled. All of the other calls will throw
 * a runtime exception.
 * 
 * @author ACNickels
 * 
 */
public class MockEpmem implements EpisodicMemory
{

    @Override
    public boolean epmem_enabled()
    {
        return false;
    }

    @Override
    public void epmem_close() throws SoarException
    {
        throw new UnsupportedOperationException();
    }

    @Override
    public void initializeNewContext(WorkingMemory wm, IdentifierImpl id)
    {
        throw new UnsupportedOperationException();
    }

    @Override
    public void epmem_reset(IdentifierImpl state)
    {
        throw new UnsupportedOperationException();
    }

    @Override
    public void epmem_go()
    {
        throw new UnsupportedOperationException();
    }

    @Override
    public void epmem_go(boolean allow_store)
    {
        throw new UnsupportedOperationException();
    }

    @Override
    public boolean encodeInOutputPhase()
    {
        throw new UnsupportedOperationException();
    }

    @Override
    public boolean encodeInSelectionPhase()
    {
        throw new UnsupportedOperationException();
    }

    @Override
    public long epmem_validation()
    {
        throw new UnsupportedOperationException();
    }

    @Override
    public boolean addIdRefCount(long id, WmeImpl w)
    {
        throw new UnsupportedOperationException();
    }

    @Override
    public void addWme(IdentifierImpl id)
    {
        throw new UnsupportedOperationException();
    }

    @Override
    public void removeWme(WmeImpl w)
    {
        throw new UnsupportedOperationException();
    }

    @Override
    public void processIds()
    {
        throw new UnsupportedOperationException();
    }

}
