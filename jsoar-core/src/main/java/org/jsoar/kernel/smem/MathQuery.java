package org.jsoar.kernel.smem;

public abstract class MathQuery
{
    //Use these to indicate if the current value is a potential new best match
    public abstract boolean valueIsAcceptable(double value);
    public abstract boolean valueIsAcceptable(long value);
    //Use this to record things like the new max values
    public abstract void commit();
    
    public static class MathQueryMax extends MathQuery
    {
        private double doubleValue = Double.NEGATIVE_INFINITY;
        private double stagedDoubleValue;
        private long longValue = Long.MIN_VALUE;
        private long stagedLongValue;
        
        private void stageDouble(double d){
            if(d > stagedDoubleValue){
                stagedDoubleValue = d;
            }
        }
        private void stageLong(long l){
            if(l > stagedLongValue){
                stagedLongValue = l;
            }
        }
        
        @Override
        public boolean valueIsAcceptable(double value)
        {
            if(value > doubleValue && value > longValue){
                stageDouble(value);
                return true;
            }
            return false;
        }

        @Override
        public boolean valueIsAcceptable(long value)
        {
            if(value > doubleValue && value > longValue){
                stageLong(value);
                return true;
            }
            return false;
        }

        @Override
        public void commit()
        {
            doubleValue = stagedDoubleValue;
            longValue = stagedLongValue;
        }
        
    }
}
