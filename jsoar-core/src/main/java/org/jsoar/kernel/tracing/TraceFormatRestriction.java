/*
 * Copyright (c) 2008  Dave Ray <daveray@gmail.com>
 *
 * Created on Sep 22, 2008
 */
package org.jsoar.kernel.tracing;

/**
 * Trace format type restrictions
 *
 * <p>trace.h:48:FOR_XXX_TF
 *
 * @author ray
 */
public enum TraceFormatRestriction {
  FOR_ANYTHING_TF, /* format applies to any object */
  FOR_STATES_TF, /* format applies only to states */
  FOR_OPERATORS_TF, /* format applies only to operators */
}
