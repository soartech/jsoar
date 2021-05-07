/*
 * Copyright (c) 2008  Dave Ray <daveray@gmail.com>
 *
 * Created on Dec 22, 2008
 */
package org.jsoar.util.properties;

import java.util.concurrent.atomic.AtomicBoolean;

/**
 * A fast, thread-safe property provider for a boolean value. Uses an {@link AtomicBoolean} to
 * provide safe access to the value. This provider is intended for use with flags that must be
 * accessed frequently.
 *
 * @author ray
 */
public class BooleanPropertyProvider implements PropertyProvider<Boolean> {
  /**
   * The current value. This value may be freely modified by owning code as long as change events
   * are not required.
   */
  public final AtomicBoolean value;

  public BooleanPropertyProvider(PropertyKey<Boolean> key) {
    value = new AtomicBoolean(key.getDefaultValue());
  }

  /* (non-Javadoc)
   * @see org.jsoar.util.properties.PropertyProvider#get()
   */
  @Override
  public Boolean get() {
    return value.get();
  }

  /* (non-Javadoc)
   * @see org.jsoar.util.properties.PropertyProvider#set(java.lang.Object)
   */
  @Override
  public Boolean set(Boolean value) {
    return this.value.getAndSet(value.booleanValue());
  }

  /* (non-Javadoc)
   * @see java.lang.Object#toString()
   */
  @Override
  public String toString() {
    return value.toString();
  }
}
