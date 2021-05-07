/*
 * Copyright (c) 2010 Dave Ray <daveray@gmail.com>
 *
 * Created on Aug 23, 2010
 */
package org.jsoar.util.commands;

import java.util.List;
import org.jsoar.util.SourceLocation;

/** @author ray */
public class ParsedCommand {
  private final SourceLocation location;
  private final List<String> args;

  public ParsedCommand(SourceLocation location, List<String> args) {
    this.location = location;
    this.args = args;
  }

  /** @return the location */
  public SourceLocation getLocation() {
    return location;
  }

  public boolean isEof() {
    return args.isEmpty();
  }

  /** @return the args */
  public List<String> getArgs() {
    return args;
  }

  public String toString() {
    return location + ": " + args;
  }
}
