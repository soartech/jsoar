package org.jsoar.soarunit.junit;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;

@Retention(RetentionPolicy.RUNTIME)
public @interface SoarSourceInclude {
  String url();
}
