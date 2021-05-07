package org.jsoar.kernel.smem.math;

public class MathQueryGreaterOrEqual extends MathQuery {
  private final double doubleValue;
  private final long longValue;
  private final boolean isDouble;

  public MathQueryGreaterOrEqual(double value) {
    doubleValue = value;
    longValue = 0;
    isDouble = true;
  }

  public MathQueryGreaterOrEqual(long value) {
    doubleValue = 0;
    longValue = value;
    isDouble = false;
  }

  @Override
  public boolean valueIsAcceptable(double value) {
    if (isDouble) {
      return value >= doubleValue;
    }
    return value >= longValue;
  }

  @Override
  public boolean valueIsAcceptable(long value) {
    if (isDouble) {
      return value >= doubleValue;
    }
    return value >= longValue;
  }

  // There is no running data in this query
  @Override
  public void commit() {}

  @Override
  public void rollback() {}
}
