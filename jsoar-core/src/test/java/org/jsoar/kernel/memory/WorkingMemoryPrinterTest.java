package org.jsoar.kernel.memory;

import static org.mockito.Mockito.mock;

import org.jsoar.kernel.Agent;
import org.jsoar.kernel.symbols.Symbol;
import org.jsoar.kernel.tracing.Printer;
import org.junit.Test;

public class WorkingMemoryPrinterTest {

  @Test(expected = IllegalArgumentException.class)
  public void testPrintThrowsExceptionIfAgentIsNull() {
    WorkingMemoryPrinter printer = new WorkingMemoryPrinter();
    printer.print(null,mock(Printer.class), mock(Symbol.class),"TEST");
  }

  @Test(expected = IllegalArgumentException.class)
  public void testPrintThrowsExceptionIfPrinterIsNull() {
    WorkingMemoryPrinter printer = new WorkingMemoryPrinter();
    printer.print(mock(Agent.class),null, mock(Symbol.class),"TEST");
  }
}