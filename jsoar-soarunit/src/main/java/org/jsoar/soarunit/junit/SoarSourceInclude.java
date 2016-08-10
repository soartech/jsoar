package org.jsoar.soarunit.junit;

import java.lang.annotation.Repeatable;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;

@Retention(RetentionPolicy.RUNTIME)
@Repeatable(SoarSourceIncludes.class)
public @interface SoarSourceInclude
{
    String url();
}
