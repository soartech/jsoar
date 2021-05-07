/*
 * Copyright (c) 2008  Dave Ray <daveray@gmail.com>
 *
 * Created on Nov 21, 2008
 */
package org.jsoar.kernel.io.quick;

import java.util.Set;

/**
 * Implementation of QMemory that accesses only the values of a QMemory on a particular prefix.
 * Facade pattern.
 *
 * <p>Note that because members are final and all methods delegate to synchronized {@link
 * DefaultQMemory} methods, this class is also thread-safe.
 *
 * @author ray
 */
class SubQMemory implements QMemory {
  private final DefaultQMemory source;
  private final String prefix;

  /**
   * @param source
   * @param prefix
   */
  SubQMemory(DefaultQMemory source, String prefix) {
    this.source = source;
    this.prefix = prefix.endsWith(".") ? prefix : prefix + '.';
  }

  private String getPath(String path) {
    return prefix + path;
  }

  /* (non-Javadoc)
   * @see org.jsoar.kernel.io.quick.QMemory#getDouble(java.lang.String)
   */
  @Override
  public double getDouble(String path) {
    return source.getDouble(getPath(path));
  }

  /* (non-Javadoc)
   * @see org.jsoar.kernel.io.quick.QMemory#getInteger(java.lang.String)
   */
  @Override
  public long getInteger(String path) {
    return source.getInteger(getPath(path));
  }

  /* (non-Javadoc)
   * @see org.jsoar.kernel.io.quick.QMemory#getPaths()
   */
  @Override
  public Set<String> getPaths() {
    return source.getPaths(prefix, true);
  }

  /* (non-Javadoc)
   * @see org.jsoar.kernel.io.quick.QMemory#getString(java.lang.String)
   */
  @Override
  public String getString(String path) {
    return source.getString(getPath(path));
  }

  /* (non-Javadoc)
   * @see org.jsoar.kernel.io.quick.QMemory#hasPath(java.lang.String)
   */
  @Override
  public boolean hasPath(String path) {
    return source.hasPath(getPath(path));
  }

  /* (non-Javadoc)
   * @see org.jsoar.kernel.io.quick.QMemory#subMemory(java.lang.String)
   */
  @Override
  public QMemory subMemory(String prefix) {
    return new SubQMemory(source, this.prefix + prefix);
  }

  /* (non-Javadoc)
   * @see org.jsoar.kernel.io.quick.QMemory#clear(java.lang.String)
   */
  @Override
  public void clear(String path) {
    source.clear(getPath(path));
  }

  /* (non-Javadoc)
   * @see org.jsoar.kernel.io.quick.QMemory#remove(java.lang.String)
   */
  @Override
  public void remove(String path) {
    source.remove(getPath(path));
  }

  /* (non-Javadoc)
   * @see org.jsoar.kernel.io.quick.QMemory#setDouble(java.lang.String, double)
   */
  @Override
  public void setDouble(String path, double doubleVal) {
    source.setDouble(getPath(path), doubleVal);
  }

  /* (non-Javadoc)
   * @see org.jsoar.kernel.io.quick.QMemory#setInteger(java.lang.String, int)
   */
  @Override
  public void setInteger(String path, int intVal) {
    source.setInteger(getPath(path), intVal);
  }

  /* (non-Javadoc)
   * @see org.jsoar.kernel.io.quick.QMemory#setInteger(java.lang.String, int)
   */
  @Override
  public void setInteger(String path, long longVal) {
    source.setInteger(getPath(path), longVal);
  }

  /* (non-Javadoc)
   * @see org.jsoar.kernel.io.quick.QMemory#setString(java.lang.String, java.lang.String)
   */
  @Override
  public void setString(String path, String strVal) {
    source.setString(getPath(path), strVal);
  }

  /* (non-Javadoc)
   * @see org.jsoar.kernel.io.quick.QMemory#addListener(org.jsoar.kernel.io.quick.QMemoryListener)
   */
  @Override
  public void addListener(QMemoryListener listener) {
    source.addListener(listener);
  }

  /* (non-Javadoc)
   * @see org.jsoar.kernel.io.quick.QMemory#removeListener(org.jsoar.kernel.io.quick.QMemoryListener)
   */
  @Override
  public void removeListener(QMemoryListener listener) {
    source.removeListener(listener);
  }
}
