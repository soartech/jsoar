/*
 * Copyright (c) 2009 Dave Ray <daveray@gmail.com>
 *
 * Created on Jun 3, 2009
 */
package org.jsoar.demos.robot.events;

import org.jsoar.demos.robot.World;
import org.jsoar.util.events.SoarEvent;

/** @author ray */
public abstract class AbstractWorldEvent implements SoarEvent {
  public final World world;

  public AbstractWorldEvent(World world) {
    this.world = world;
  }
}
