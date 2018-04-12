package org.jsoar.soarunit.junit;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;

@Retention(RetentionPolicy.RUNTIME)
public @interface SoarInterpreter
{
    /**
     * Change Soar agent interpreter (e.g., "default" or "tcl")
     */
    String interpreter();
}
