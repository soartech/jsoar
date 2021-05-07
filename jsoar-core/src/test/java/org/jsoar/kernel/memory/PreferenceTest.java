/*
 * Copyright (c) 2008  Dave Ray <daveray@gmail.com>
 *
 * Created on Sep 20, 2008
 */
package org.jsoar.kernel.memory;

import org.jsoar.JSoarTest;
import org.junit.Test;

/** @author ray */
public class PreferenceTest extends JSoarTest {

  /**
   * Test method for {@link org.jsoar.kernel.memory.Preference#formatTo(java.util.Formatter, int,
   * int, int)}.
   */
  @Test
  public void testFormatTo() {

    // RPM 5/2010 commented this out since it tests a defunct preference type (which I'm removing)
    // RPM 1/2013 leaving this here as a template for testing other preference types
    //
    //        Preference p = new Preference(PreferenceType.BINARY_PARALLEL,
    //                                      syms.make_new_identifier('S', 0),
    //                                      syms.createString("superstate"),
    //                                      syms.createString("nil"),
    //                                      syms.createDouble(3.14));
    //
    //        assertEquals("(S1 ^superstate nil & 3.14)\n", String.format("%s", p));
    //
    //        p.o_supported = true;
    //        assertEquals("(S1 ^superstate nil & 3.14  :O )\n", String.format("%s", p));

    // TODO test all other preference types :(
  }
}
