/*
 * Copyright (c) 2008  Dave Ray <daveray@gmail.com>
 *
 * Created on Nov 26, 2008
 */
package org.jsoar.kernel.memory;

import java.io.IOException;
import java.io.StringWriter;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import org.jsoar.kernel.Agent;
import org.jsoar.kernel.PredefinedSymbols;
import org.jsoar.kernel.Production;
import org.jsoar.kernel.symbols.Identifier;
import org.jsoar.kernel.symbols.Symbol;
import org.jsoar.kernel.tracing.TraceFormats;
import org.jsoar.util.Arguments;
import org.jsoar.util.adaptables.AbstractAdaptable;
import org.jsoar.util.adaptables.Adaptables;

/**
 * Simple class that retrieves support information for a particular WME.
 *
 * <p>This code is based very loosely on the implementation of the preferences command in csoar.
 *
 * @author ray
 */
public class WmeSupportInfo {
  private final Wme wme;
  private final List<Support> supports;

  /**
   * Represents support for the WME from a single preference. Includes info about the preference as
   * well as the source production that created the preference and the LHS WMEs involved.
   *
   * @author ray
   */
  public static class Support extends AbstractAdaptable {
    private final Preference pref;
    private final boolean osupported;
    private final String valueTrace;
    private final List<Wme> wmes;

    private Support(Preference pref, boolean osupported, String valueTrace, List<Wme> wmes) {
      this.pref = pref;
      this.osupported = osupported;
      this.valueTrace = valueTrace;
      this.wmes = wmes;
    }

    public PreferenceType getType() {
      return pref.type;
    }

    public Identifier getIdentifier() {
      return pref.id;
    }

    public Symbol getAttribute() {
      return pref.attr;
    }

    public Symbol getValue() {
      return pref.value;
    }

    public String getValueTrace() {
      return valueTrace;
    }

    public Symbol getReferent() {
      return pref.referent;
    }

    public boolean isOSupported() {
      return osupported;
    }

    public Production getSource() {
      if (pref.inst != null) {
        return pref.inst.prod;
      }
      return null;
    }

    public List<Wme> getSourceWmes() {
      return wmes;
    }

    /* (non-Javadoc)
     * @see org.jsoar.util.adaptables.AbstractAdaptable#getAdapter(java.lang.Class)
     */
    @Override
    public Object getAdapter(Class<?> klass) {
      if (klass.equals(Production.class)) {
        return getSource();
      }
      return super.getAdapter(klass);
    }
  }

  /**
   * Get support info for the given WME.
   *
   * @param agent The agent
   * @param wme The wme
   * @return support info for the given wme, or null if it is architectural or I/O, i.e. if it has
   *     no preference
   * @throws IllegalArgumentException if agent or wme is <code>null</code>.
   */
  public static WmeSupportInfo get(Agent agent, Wme wme) {
    Arguments.checkNotNull(agent, "agent");
    Arguments.checkNotNull(wme, "wme");

    final List<Support> sources = new ArrayList<Support>();
    final Iterator<Preference> prefIt = wme.getPreferences();
    while (prefIt.hasNext()) {
      sources.add(createSupport(agent, prefIt.next()));
    }
    return new WmeSupportInfo(wme, sources);
  }

  /** @return the wme this support info refers to */
  public Wme getWme() {
    return wme;
  }

  /** @return the list of supports (one per preference) for the WME. */
  public List<Support> getSupports() {
    return supports;
  }

  private static Support createSupport(Agent agent, Preference pref) {
    final TraceFormats traceFormats = Adaptables.adapt(agent, TraceFormats.class);
    final PredefinedSymbols predefinedSyms = Adaptables.adapt(agent, PredefinedSymbols.class);
    final List<Wme> sourceWmes = pref.inst.getBacktraceWmes();

    final String valueTrace;
    if (pref.attr == predefinedSyms.operator_symbol) {
      StringWriter w = new StringWriter();
      try {
        traceFormats.print_object_trace(w, pref.value);
      } catch (IOException e) {
        e.printStackTrace();
      }
      valueTrace = w.toString();
    } else {
      valueTrace = String.format("%s", pref.value);
    }

    return new Support(pref, pref.o_supported, valueTrace, sourceWmes);
  }

  private WmeSupportInfo(Wme wme, List<Support> supports) {
    this.wme = wme;
    this.supports = Collections.unmodifiableList(supports);
  }

  /* (non-Javadoc)
   * @see java.lang.Object#toString()
   */
  @Override
  public String toString() {
    StringBuilder b = new StringBuilder();
    b.append(String.format("%s is supported by:\n", wme));
    for (Support s : supports) {
      b.append(String.format("   %s\n", s.getSource()));
      for (Wme w : s.getSourceWmes()) {
        b.append(String.format("      %s\n", w));
      }
    }
    return b.toString();
  }
}
