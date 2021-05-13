package org.jsoar.kernel.rete;

import static org.mockito.Mockito.mock;

import org.jsoar.kernel.epmem.EpisodicMemory;
import org.jsoar.kernel.smem.SemanticMemory;
import org.jsoar.kernel.symbols.SymbolFactoryImpl;
import org.jsoar.kernel.tracing.Trace;
import org.junit.Test;

public class ReteNetReaderTest {

  @Test(expected = IllegalArgumentException.class)
  public void testConstructorThrowsExceptionIfContextIsNull() {
    new ReteNetReader(null);
  }

}
