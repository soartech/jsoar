package org.jsoar.performancetesting;

import com.opencsv.bean.CsvBindAndSplitByPosition;
import java.util.ArrayList;
import java.util.List;

public class RawResults {
  @CsvBindAndSplitByPosition(elementType = Double.class, position = 0)
  public List<Double> cpuTimes = new ArrayList<>();

  @CsvBindAndSplitByPosition(elementType = Double.class, position = 1)
  public List<Double> kernelTimes = new ArrayList<>();

  @CsvBindAndSplitByPosition(elementType = Integer.class, position = 2)
  public List<Integer> decisionCycles = new ArrayList<>();

  @CsvBindAndSplitByPosition(elementType = Long.class, position = 3)
  public List<Long> memoryLoads = new ArrayList<>();

  public static String[] header = {"cpuTimes", "kernelTimes", "decisionCycles", "memoryLoads"};

  public RawResults accumulate(RawResults other) {
    this.cpuTimes.addAll(other.cpuTimes);
    this.kernelTimes.addAll(other.kernelTimes);
    this.decisionCycles.addAll(other.decisionCycles);
    this.memoryLoads.addAll(other.memoryLoads);
    return this;
  }
}
