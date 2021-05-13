/*
 * (c) 2008  Dave Ray
 *
 * Created on Aug 19, 2008
 */
package org.jsoar.util;

/** @author ray */
public final class Arguments {

  private Arguments() {}
  /**
   * If the given condition is not true, throw an {@link IllegalArgumentException} with the given
   * description.
   *
   * @param condition the desired condition
   * @param description exception message
   */
  public static void check(boolean condition, String description) {
    if (!condition) {
      throw new IllegalArgumentException(description);
    }
  }
}
