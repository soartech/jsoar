package org.jsoar.performancetesting;

import java.nio.file.Path;
import java.util.stream.Collectors;

import com.google.common.math.Quantiles;
import com.google.common.math.Stats;
import com.opencsv.bean.CsvBindByPosition;

public class Results
{
    @CsvBindByPosition(position = 0)
    public String testName;
    @CsvBindByPosition(position = 1)
    public Path testFile;
    @CsvBindByPosition(position = 2)
    public String soarVariant;
    @CsvBindByPosition(position = 3)
    public Path soarPath;
    
    @CsvBindByPosition(position = 4)
    public double cpuTimesTotal;
    @CsvBindByPosition(position = 5)
    public double cpuTimesAverage;
    @CsvBindByPosition(position = 6)
    public double cpuTimesMedian;
    @CsvBindByPosition(position = 7)
    public double cpuTimesStdDeviation;
    
    @CsvBindByPosition(position = 8)
    public double kernelTimesTotal;
    @CsvBindByPosition(position = 9)
    public double kernelTimesAverage;
    @CsvBindByPosition(position = 10)
    public double kernelTimesMedian;
    @CsvBindByPosition(position = 11)
    public double kernelTimesStdDeviation;
    
    @CsvBindByPosition(position = 12)
    public double decisionCyclesTotal;
    @CsvBindByPosition(position = 13)
    public double decisionCyclesAverage;
    @CsvBindByPosition(position = 14)
    public double decisionCyclesMedian;
    @CsvBindByPosition(position = 15)
    public double decisionCyclesStdDeviation;
    
    @CsvBindByPosition(position = 16)
    public double memoryLoadsTotal;
    @CsvBindByPosition(position = 17)
    public double memoryLoadsAverage;
    @CsvBindByPosition(position = 18)
    public double memoryLoadsMedian;
    @CsvBindByPosition(position = 19)
    public double memoryLoadsStdDeviation;
    
    public static String[] header = { "Test", "Test File", "Soar Variant", "Soar Path",
            "Total CPU Time (s)", "Average CPU Time Per Run (s)", "Median CPU Time Per Run (s)", "CPU Time Std Deviation (s)",
            "Total Kernel Time (s)", "Average Kernel Time Per Run (s)", "Median Kernel Time Per Run (s)", "Kernel Time Std Deviation (s)",
            "Total Decision Cycles", "Average Decision Cycles Per Run", "Median Decision Cycles Per Run", "Decision Cycles Std Deviation",
            "Total Memory Used (M)", "Average Memory Used Per Run (M)", "Median Memory Used Per Run (M)", "Memory Std Deviation (M)" };
    
    public Results(Test test)
    {
        this.testName = test.getTestName();
        this.testFile = test.getTestFile();
        this.soarVariant = test.getSoarVariant();
        this.soarPath = test.getTestSettings().isJsoarEnabled()
                ? test.getTestSettings().getJsoarCoreJars().get(0)
                : test.getTestSettings().getCsoarDirectories().get(0);
    }
    
    public Results(String testName, Path testFile, String soarVariant, Path soarPath)
    {
        this.testName = testName;
        this.testFile = testFile;
        this.soarVariant = soarVariant;
        this.soarPath = soarPath;
    }
    
    public void updateStats(RawResults rawResults)
    {
        // if(rawResults.cpuTimes.size() > 0) {
        Stats cpuTimesStats = Stats.of(rawResults.cpuTimes);
        this.cpuTimesTotal = cpuTimesStats.sum();
        this.cpuTimesAverage = cpuTimesStats.mean();
        this.cpuTimesMedian = Quantiles.median().compute(rawResults.cpuTimes);
        this.cpuTimesStdDeviation = getStdDeviation(cpuTimesStats);
        
        Stats dcStats = Stats.of(rawResults.decisionCycles);
        this.decisionCyclesTotal = dcStats.sum();
        this.decisionCyclesAverage = dcStats.mean();
        this.decisionCyclesMedian = Quantiles.median().compute(rawResults.decisionCycles);
        this.decisionCyclesStdDeviation = getStdDeviation(dcStats);
        
        Stats kernelTimesStats = Stats.of(rawResults.kernelTimes);
        this.kernelTimesTotal = kernelTimesStats.sum();
        this.kernelTimesAverage = kernelTimesStats.mean();
        this.kernelTimesMedian = Quantiles.median().compute(rawResults.kernelTimes);
        this.kernelTimesStdDeviation = getStdDeviation(kernelTimesStats);
        
        // convert list from bytes to megabytes first
        Stats memoryLoadsStats = Stats.of(rawResults.memoryLoads.stream().map(e -> (e / 1000.0 / 1000.0)).collect(Collectors.toList()));
        this.memoryLoadsTotal = memoryLoadsStats.sum();
        this.memoryLoadsAverage = memoryLoadsStats.mean();
        this.memoryLoadsMedian = Quantiles.median().compute(rawResults.memoryLoads);
        this.memoryLoadsStdDeviation = getStdDeviation(memoryLoadsStats);
        // }
    }
    
    private double getStdDeviation(Stats stats)
    {
        return stats.count() > 1 ? stats.populationStandardDeviation() : 0.0;
    }
}
