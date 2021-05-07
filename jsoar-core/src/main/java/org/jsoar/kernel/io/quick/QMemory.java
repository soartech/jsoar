/*
 * Copyright (c) 2008  Dave Ray <daveray@gmail.com>
 *
 * Created on Nov 21, 2008
 */
package org.jsoar.kernel.io.quick;

import java.util.Set;

/**
 * Interface for a "quick memory" object. Essentially stores key/value pairs but keys are
 * dot-delimited paths corresponding, eventually, to Soar working memory paths.
 *
 * <p>Instances of QMemory may be instantiated with the create() methods of {@link DefaultQMemory}.
 *
 * <p>Unlike in Soar, paths are unique, so setting more than one value at a particular path will
 * overwrite the value rather than creating multi-attributes. Multi-attributes are supported through
 * and array-style syntax in the paths (note that the array index may be any string, not just
 * integers):
 *
 * <pre>{@code
 * QMemory q = DefualtQMemory.create();
 *
 * q.setDouble("a.b.c", 9.0);
 * q.setDouble("a.b.c", 10.0);
 * double v = q.getDouble("a.b.c"); // returns 10.0
 *
 * q.setInteger("a.c[0]", 1);
 * q.setInteger("a.c[1]", 2);
 *
 * int i = q.getInteger("a.c[0]"); // returns 1
 * i = q.getInteger("a.c[1]"); // returns 2
 * }</pre>
 *
 * <p>QMemory objects are thread-safe. They may be manipulated from any thread. They synchronize on
 * themselves (this). Client code may synchronize on a QMemory object to safely perform larger
 * transactions on the object. For example, without the synchronization below, an agent may see x
 * and y in an inconsistent state:
 *
 * <pre>{@code
 * QMemory q = DefualtQMemory.create();
 *
 * synchronized(q)
 * {
 *     q.setDouble("x", 9.0);
 *     q.setDouble("y", 10.0);
 * }
 *
 * }</pre>
 *
 * <p>Credit for this concept, including all the classes in this package goes to Mike Quist.
 *
 * @author ray
 */
public interface QMemory {
  /**
   * @param path path to check for
   * @return true if the specified path is set
   */
  boolean hasPath(String path);

  /**
   * @param path path to search
   * @return double value on the given path, or 0.0 if not found. Conversions from int and string
   *     are performed if possible.
   */
  double getDouble(String path);

  /**
   * @param path path to search
   * @return int value on the given path, or 0 if not found. Conversions from double and string are
   *     performed if possible.
   */
  long getInteger(String path);

  /**
   * @param path path to search
   * @return string value on the given path, or empty string if not found. Conversions from numeric
   *     values are performed
   */
  String getString(String path);

  /** @return Copy of set of all paths in this memory. */
  Set<String> getPaths();

  /**
   * Returns a new QMemory that views the contents of this memory at a particular path prefix.
   *
   * <p>For example,
   *
   * <pre>{@code
   * QMemory base = DefaultQMemory.create();
   * base.setString("a.b.c.d.e", "hi");
   * QMemory sub = base.subMemory("a.b.c.d");
   * sub.getString("e"); // returns "hi"
   * }</pre>
   *
   * @param prefix The prefix to start at. May end with a dot.
   * @return New QMemory view of this memory starting at the given prefix
   */
  QMemory subMemory(String prefix);

  /**
   * Set a path to a double value. Any previous value will be removed
   *
   * @param path path to set
   * @param doubleVal Double value
   */
  void setDouble(String path, double doubleVal);

  /**
   * Set a path to an int value. Any previous value will be removed.
   *
   * @param path path to set
   * @param intVal Integer value
   */
  void setInteger(String path, int intVal);

  /**
   * Set a path to a long value. Any previous value will be removed.
   *
   * @param path path to set
   * @param longVal Long value
   */
  void setInteger(String path, long longVal);

  /**
   * Set a path to a string value. Any previous value will be removed.
   *
   * @param path path to set
   * @param strVal String value
   */
  void setString(String path, String strVal);

  /**
   * Clear the value at the given path, i.e change to a non-leaf. The path remains, but it no longer
   * has a value.
   *
   * @param path The path to clear
   */
  void clear(String path);

  /**
   * Permanently remove the given path and all of its children from this memory
   *
   * @param path The path to remove
   */
  void remove(String path);

  void addListener(QMemoryListener listener);

  void removeListener(QMemoryListener listener);
}
