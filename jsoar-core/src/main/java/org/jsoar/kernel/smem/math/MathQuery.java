package org.jsoar.kernel.smem.math;

public abstract class MathQuery
{
    // Use these to indicate if the current value is a potential new best match
    public abstract boolean valueIsAcceptable(double value);
    
    public abstract boolean valueIsAcceptable(long value);
    
    // Use this to record things like the new max values
    public abstract void commit();
    
    public abstract void rollback();
}
