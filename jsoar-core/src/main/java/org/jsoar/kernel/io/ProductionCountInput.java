/*
 * Copyright (c) 2011  Dave Ray <daveray@gmail.com>
 *
 * Created on Feb 22, 2011
 * Derived from CycleCountInput.java
 */
package org.jsoar.kernel.io;

import java.util.Map;
import lombok.NonNull;
import org.jsoar.kernel.Agent;
import org.jsoar.kernel.ProductionType;
import org.jsoar.kernel.events.InputEvent;
import org.jsoar.kernel.symbols.Symbols;
import org.jsoar.util.events.SoarEvent;
import org.jsoar.util.events.SoarEventListener;

/**
 * Adds augmentations to the input-link with the current production counts. The root augmentation is
 * "production-counts" which has a child for each production type with the count for that type, and
 * a total count.
 *
 * @author marinier
 */
public class ProductionCountInput {
  private final Agent agent;
  private final InputOutput io;
  private final InputListener listener;

  private InputWme rootIdWme;
  private InputWme totalCountWme;
  private InputWme userCountWme;
  private InputWme chunkCountWme;
  private InputWme defaultCountWme;
  private InputWme justificationCountWme;
  private InputWme templateCountWme;

  /**
   * Construct a new production count input object. This object will automatically register for
   * input events and update the input-link.
   *
   * @param agent The agent
   */
  public ProductionCountInput(@NonNull Agent agent) {
    this.agent = agent;
    this.io = agent.getInputOutput();
    this.listener = new InputListener();
    this.io.getEvents().addListener(InputEvent.class, listener);
  }

  /**
   * Dispose this object, removing the production counts from the input link and unregistering from
   * the event manager if necessary
   */
  public void dispose() {
    this.io.getEvents().removeListener(null, listener);

    // Schedule removal of wme at next input cycle.
    if (rootIdWme != null) {
      this.rootIdWme.remove();
      this.rootIdWme = null;
      this.chunkCountWme = null;
      this.defaultCountWme = null;
      this.justificationCountWme = null;
      this.templateCountWme = null;
      this.totalCountWme = null;
      this.userCountWme = null;
    }
  }

  /** Updates the input-link. This should only be called during the input cycle. */
  private void update() {
    Map<ProductionType, Integer> counts = agent.getProductions().getProductionCounts();
    int totalCount = agent.getProductions().getProductionCount();
    int chunkCount = counts.get(ProductionType.CHUNK);
    int defaultCount = counts.get(ProductionType.DEFAULT);
    int justificationCount = counts.get(ProductionType.JUSTIFICATION);
    int templateCount = counts.get(ProductionType.TEMPLATE);
    int userCount = counts.get(ProductionType.USER);

    if (rootIdWme == null) {
      rootIdWme = InputWmes.add(io, "production-counts", Symbols.NEW_ID);
      chunkCountWme = InputWmes.add(rootIdWme, "chunk", chunkCount);
      defaultCountWme = InputWmes.add(rootIdWme, "default", defaultCount);
      justificationCountWme = InputWmes.add(rootIdWme, "justification", justificationCount);
      templateCountWme = InputWmes.add(rootIdWme, "template", templateCount);
      totalCountWme = InputWmes.add(rootIdWme, "total", totalCount);
      userCountWme = InputWmes.add(rootIdWme, "user", userCount);
    } else {
      InputWmes.update(chunkCountWme, chunkCount);
      InputWmes.update(defaultCountWme, defaultCount);
      InputWmes.update(justificationCountWme, justificationCount);
      InputWmes.update(templateCountWme, templateCount);
      InputWmes.update(totalCountWme, totalCount);
      InputWmes.update(userCountWme, userCount);
    }
  }

  private class InputListener implements SoarEventListener {
    /* (non-Javadoc)
     * @see org.jsoar.kernel.events.SoarEventListener#onEvent(org.jsoar.kernel.events.SoarEvent)
     */
    @Override
    public void onEvent(SoarEvent event) {
      update();
    }
  }
}
