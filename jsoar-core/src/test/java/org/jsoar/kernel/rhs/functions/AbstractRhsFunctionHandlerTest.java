package org.jsoar.kernel.rhs.functions;

import java.util.List;
import org.jsoar.kernel.symbols.Symbol;
import org.junit.Test;

public class AbstractRhsFunctionHandlerTest {

  private class DummyRhsFunctionHandler extends AbstractRhsFunctionHandler {

    private DummyRhsFunctionHandler(String name) {
      super(name);
    }

    /**
     * Execute the function and return a result.
     *
     * @param context Context info for the function including symbol factory
     * @param arguments List of arguments
     * @return Result symbol
     * @throws RhsFunctionException if an error occurs
     */
    @Override
    public Symbol execute(RhsFunctionContext context, List<Symbol> arguments)
        throws RhsFunctionException {
      return null;
    }
  }

  @Test(expected = IllegalArgumentException.class)
  public void testConstructorThrowsExceptionIfNameIsNull() {
    new DummyRhsFunctionHandler(null);
  }
}
