package org.jsoar.kernel.epmem;

import org.jsoar.kernel.SoarException;
import org.jsoar.kernel.memory.WmeImpl;
import org.jsoar.kernel.memory.WorkingMemory;
import org.jsoar.kernel.symbols.IdentifierImpl;

public class MockEpmem implements EpisodicMemory
{

    @Override
    public boolean epmem_enabled()
    {
        // TODO Auto-generated method stub
        return false;
    }

    @Override
    public void epmem_close() throws SoarException
    {
        // TODO Auto-generated method stub
        
    }

    @Override
    public void initializeNewContext(WorkingMemory wm, IdentifierImpl id)
    {
        // TODO Auto-generated method stub
        
    }

    @Override
    public void epmem_reset(IdentifierImpl state)
    {
        // TODO Auto-generated method stub
        
    }

    @Override
    public void epmem_go()
    {
        // TODO Auto-generated method stub
        
    }

    @Override
    public void epmem_go(boolean allow_store)
    {
        // TODO Auto-generated method stub
        
    }

    @Override
    public boolean encodeInOutputPhase()
    {
        // TODO Auto-generated method stub
        return false;
    }

    @Override
    public boolean encodeInSelectionPhase()
    {
        // TODO Auto-generated method stub
        return false;
    }

    @Override
    public long epmem_validation()
    {
        // TODO Auto-generated method stub
        return 0;
    }

    @Override
    public boolean addIdRefCount(long id, WmeImpl w)
    {
        // TODO Auto-generated method stub
        return false;
    }

    @Override
    public void addWme(IdentifierImpl id)
    {
        // TODO Auto-generated method stub
        
    }

    @Override
    public void removeWme(WmeImpl w)
    {
        // TODO Auto-generated method stub
        
    }

    @Override
    public void processIds()
    {
        // TODO Auto-generated method stub
        
    }

}
