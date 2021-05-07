/*
 * Copyright (c) 2009  Dave Ray <daveray@gmail.com>
 *
 * Created on Apr 8, 2009
 */
package org.jsoar.kernel;

import java.util.List;
import org.jsoar.kernel.memory.Wme;

/**
 * Interface representing a MatchSet. Objects implementing this interface are immutable.
 *
 * @author ray
 */
public interface MatchSetEntry {
  public static enum EntryType {
    O_ASSERTION,
    I_ASSERTION,
    RETRACTION
  }

  /** @return type of match */
  EntryType getType();

  /** @return the production associated with the match set */
  Production getProduction();

  /**
   * Returns an immutable list of WMEs associated with this match set. It is the list of WMEs that
   * caused the rule to match.
   *
   * @return immutable list of WMEs
   */
  List<Wme> getWmes();
}
