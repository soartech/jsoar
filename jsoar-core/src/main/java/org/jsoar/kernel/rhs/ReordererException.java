/*
 * Copyright (c) 2008  Dave Ray <daveray@gmail.com>
 *
 * Created on Sep 26, 2008
 */
package org.jsoar.kernel.rhs;

/**
 * Exception thrown by reorderers on error
 *
 * @author ray
 */
public class ReordererException extends Exception {
  private static final long serialVersionUID = 2012285532948797485L;

  public ReordererException(String message) {
    super(message);
  }
}
