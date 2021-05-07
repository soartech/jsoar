package org.jsoar.kernel.smem.math;

public class MathQueryMax extends MathQuery {
  private double doubleValue = Double.NEGATIVE_INFINITY;
  private double stagedDoubleValue = Double.NEGATIVE_INFINITY;
  private long longValue = Long.MIN_VALUE;
  private long stagedLongValue = Long.MIN_VALUE;

  private void stageDouble(double d) {
    if (d > stagedDoubleValue) {
      stagedDoubleValue = d;
    }
  }

  private void stageLong(long l) {
    if (l > stagedLongValue) {
      stagedLongValue = l;
    }
  }

  @Override
  public boolean valueIsAcceptable(double value) {
    if (value > doubleValue && value > longValue) {
      stageDouble(value);
      return true;
    }
    return false;
  }

  @Override
  public boolean valueIsAcceptable(long value) {
    if (value > doubleValue && value > longValue) {
      stageLong(value);
      return true;
    }
    return false;
  }

  @Override
  public void commit() {
    doubleValue = stagedDoubleValue;
    longValue = stagedLongValue;
  }

  @Override
  public void rollback() {
    stagedDoubleValue = doubleValue;
    stagedLongValue = longValue;
  }
}
