/*
 * Copyright (c) 2010 Dave Ray <daveray@gmail.com>
 *
 * Created on Jul 22, 2010
 */
package org.jsoar.soarunit;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PushbackReader;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;
import org.jsoar.kernel.SoarException;
import org.jsoar.util.commands.DefaultInterpreterParser;
import org.jsoar.util.commands.ParsedCommand;
import org.jsoar.util.commands.ParserBuffer;

/** @author ray */
public class TestCase {
  private final URL url;
  private final String name;
  private final int prefixIndex;
  private String setup = "";
  private final List<Test> tests = new ArrayList<Test>();

  private static String getNameFromFile(URL url, int prefixIndex) {
    final String name = url.getFile().substring(prefixIndex);
    final int dot = name.lastIndexOf('.');

    return dot > 0 ? name.substring(0, dot) : name;
  }

  public static TestCase fromFile(File file, int prefixIndex) throws SoarException, IOException {
    return fromURL(file.toURI().toURL(), prefixIndex);
  }

  public static TestCase fromURL(URL url, int prefixIndex) throws SoarException, IOException {
    final ParserBuffer reader =
        new ParserBuffer(
            new PushbackReader(new BufferedReader(new InputStreamReader(url.openStream()))));
    reader.setFile(url.getPath());
    try {
      final TestCase testCase = new TestCase(url, getNameFromFile(url, prefixIndex), prefixIndex);
      final DefaultInterpreterParser parser = new DefaultInterpreterParser();
      ParsedCommand parsedCommand = parser.parseCommand(reader);
      while (!parsedCommand.isEof()) {
        final String name = parsedCommand.getArgs().get(0);
        if ("setup".equals(name)) {
          testCase.setup += "\n";
          testCase.setup += parsedCommand.getArgs().get(1);
        } else if ("test".equals(name)) {
          final Test test =
              new Test(testCase, parsedCommand.getArgs().get(1), parsedCommand.getArgs().get(2));
          testCase.addTest(test);
        } else {
          throw new SoarException(url.toString() + ": Unsupported SoarUnit command '" + name + "'");
        }

        parsedCommand = parser.parseCommand(reader);
      }
      return testCase;
    } finally {
      reader.close();
    }
  }

  public static int getTotalTests(List<TestCase> allCases) {
    int result = 0;
    for (TestCase testCase : allCases) {
      result += testCase.getTests().size();
    }
    return result;
  }

  public TestCase(URL url, String name, int prefixIndex) {
    this.url = url;
    this.name = name;
    this.prefixIndex = prefixIndex;
  }

  /** @return the setup */
  public String getSetup() {
    return setup;
  }

  /** @param setup the setup to set */
  public void setSetup(String setup) {
    this.setup = setup;
  }

  /** @return the name */
  public String getName() {
    return name;
  }

  /** @return the url */
  public URL getUrl() {
    return url;
  }

  public void addTest(Test test) {
    tests.add(test);
  }

  /** @return the tests */
  public List<Test> getTests() {
    return tests;
  }

  public Test getTest(String name) {
    for (Test test : tests) {
      if (name.equals(test.getName())) {
        return test;
      }
    }
    return null;
  }

  /* (non-Javadoc)
   * @see java.lang.Object#toString()
   */
  public String toString() {
    return name;
  }

  public TestCase reload() throws SoarException, IOException {
    return fromURL(getUrl(), this.prefixIndex);
  }
}
