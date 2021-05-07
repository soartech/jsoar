package org.jsoar.kernel.rete;

import org.jsoar.kernel.SoarException;

/**
 * CSoar compatible constants used in {@link ReteNetReader} and {@link ReteNetWriter}.
 *
 * @author charles.newton
 */
public class ReteNetConstants {
  // Used in {@link ReteNetReader#readAction} and {@link ReteNetWriter#writeAction}
  protected enum Action {
    MAKE_ACTION,
    FUNCALL_ACTION;

    public static Action fromOrdinal(int i) throws SoarException {
      return ReteNetConstants.fromOrdinal(Action.class, i);
    }
  }

  // Used in {@link ReteNetReader#readRHSValue} and {@link ReteNetWriter#writeRHSValue}
  protected enum RHS {
    RHS_SYMBOL,
    RHS_FUNCALL,
    RHS_RETELOC,
    RHS_UNBOUND_VAR;

    public static RHS fromOrdinal(int i) throws SoarException {
      return ReteNetConstants.fromOrdinal(RHS.class, i);
    }
  }

  // Used in {@link ReteNetReader#readVarNames} and {@link ReteNetWriter#writeVarNames}
  protected enum VarName {
    VARNAME_NULL,
    VARNAME_ONE_VAR,
    VARNAME_LIST;

    public static VarName fromOrdinal(int i) throws SoarException {
      return ReteNetConstants.fromOrdinal(VarName.class, i);
    }
  }

  /**
   * Returns the instance of the enum at a specific ordinal.
   *
   * @param enm Enum class to operate on.
   * @param index the ordinal of the enum constant to return (see {@link Enum#ordinal()}).
   */
  private static <E extends Enum<E>> E fromOrdinal(Class<E> enm, int index) throws SoarException {
    E[] enumConstants = enm.getEnumConstants();
    if (index >= enumConstants.length) {
      throw new SoarException(
          String.format(
              "Unknown %s type %d. Expected value from 0 to %d.",
              enm.getName(), index, enumConstants.length - 1));
    }
    return enumConstants[index];
  }
}
