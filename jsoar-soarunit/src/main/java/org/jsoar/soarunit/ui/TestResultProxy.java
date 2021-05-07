/** Copyright (c) 2010 Dave Ray <daveray@gmail.com> */
package org.jsoar.soarunit.ui;

import org.jsoar.soarunit.Test;
import org.jsoar.soarunit.TestResult;

/** @author dave */
public class TestResultProxy {
  private final Test test;
  private TestResult result;

  public TestResultProxy(Test test) {
    this.test = test;
  }

  public TestResult getResult() {
    return result;
  }

  public void setResult(TestResult result) {
    this.result = result;
  }

  public Test getTest() {
    return test;
  }

  @Override
  public String toString() {
    return result != null ? result.toString() : test.toString();
  }
}
