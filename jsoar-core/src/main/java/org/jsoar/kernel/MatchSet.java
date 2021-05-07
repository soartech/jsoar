/*
 * Copyright (c) 2009  Dave Ray <daveray@gmail.com>
 *
 * Created on Apr 8, 2009
 */
package org.jsoar.kernel;

import java.util.List;

/**
 * Interface representing a MatchSet. Objects implementing this interface are immutable.
 *
 * @author ray
 */
public interface MatchSet {
  List<MatchSetEntry> getEntries();
}
