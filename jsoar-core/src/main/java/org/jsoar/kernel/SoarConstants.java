/*
 * Copyright (c) 2008  Dave Ray <daveray@gmail.com>
 *
 * Created on Sep 10, 2008
 */
package org.jsoar.kernel;

/**
 * General Soar constants that don't have an obvious home
 * 
 * @author ray
 */
public final class SoarConstants
{
    /**
     * kernel.h:206:TOP_GOAL_LEVEL
     */
    public static final int TOP_GOAL_LEVEL = 1;
    
    /**
     * kernel.h:207:ATTRIBUTE_IMPASSE_LEVEL
     */
    public static final int ATTRIBUTE_IMPASSE_LEVEL = Integer.MAX_VALUE;
    
    /**
     * UNcomment the following line to have Soar maintain reference counts on
     * wmes and prefs at the top level. This can result in larger memory growth
     * due to top-level objects that never get deallocated because the ref
     * counts never drop to 0. The default for Soar v6 - v8.6.1 was to maintain
     * the ref cts. It's possible that in your particular application, weird
     * things could happen if you don't do these ref cts, but if you are trying
     * to improve performance and reduce memory, it's worth testing your system
     * with the top-level-ref-cts turned off. Soar will be much more efficient.
     * See comments in recmem.cpp
     * 
     * <p>This value is controlled by the system property 'jsoar.do_top_level_ref_cts'. For example:
     * <pre>
     * $ java -Djsoar.do_top_level_ref_cts=false ...
     * </pre>
     * <p>kernel.h:129:DO_TOP_LEVEL_REF_CTS (pre-processor macro defaults to <b>not defined</b> in csoar)
     * <p>Defaults to <code>false</code>
     */
    public static final boolean DO_TOP_LEVEL_REF_CTS = Boolean.parseBoolean(System.getProperty("jsoar.do_top_level_ref_cts", "false"));
}
