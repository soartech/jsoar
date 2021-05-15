/*
 * Copyright (c) 2009  Dave Ray <daveray@gmail.com>
 *
 * Created on April 28, 2009
 */
package org.jsoar.kernel.rhs.functions;

import com.google.common.io.ByteStreams;
import java.io.BufferedInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.MalformedURLException;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.List;
import lombok.NonNull;
import org.jsoar.kernel.symbols.Symbol;

/**
 * A Right Hand Side function that opens a connection to a URL, reads it and returns the contents as
 * a string.
 *
 * @author ray
 */
public class GetUrl extends AbstractRhsFunctionHandler {

  /**
   *
   */
  public GetUrl() {
    super("get-url", 1, 1);
  }

  /**
   * Returns the URL from the passed arguments
   */
  private URL getUrl(List<Symbol> arguments) throws RhsFunctionException {
    try {
      return new URL(arguments.get(0).toString());
    } catch (MalformedURLException e) {
      throw new RhsFunctionException("In '" + getName() + "' RHS function: " + e.getMessage());
    }
  }

  /* (non-Javadoc)
   * @see org.jsoar.kernel.rhs.functions.RhsFunctionHandler#execute(org.jsoar.kernel.rhs.functions.RhsFunctionContext, java.util.List)
   */
  @Override
  public Symbol execute(@NonNull final RhsFunctionContext context, @NonNull List<Symbol> arguments)
      throws RhsFunctionException {
    RhsFunctions.checkArgumentCount(this, arguments);

    final var url = getUrl(arguments);

    // Open an input stream
    try (InputStream is = new BufferedInputStream(url.openStream())) {
      // Read the input stream into a buffer
      final var out = new ByteArrayOutputStream();
      ByteStreams.copy(is, out);
      return context.getSymbols().createString(out.toString(StandardCharsets.UTF_8));
    } catch (IOException e) {
      throw new RhsFunctionException("In '" + getName() + "' RHS function: " + e.getMessage());
    }
  }
}
