package org.jsoar.soarunit.junit;

import com.google.common.base.Function;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.google.common.collect.Queues;
import com.google.common.util.concurrent.Futures;
import com.google.common.util.concurrent.ListenableFuture;
import com.google.common.util.concurrent.ListeningExecutorService;
import com.google.common.util.concurrent.MoreExecutors;
import java.io.IOException;
import java.io.PrintWriter;
import java.lang.annotation.Annotation;
import java.net.URL;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Queue;
import java.util.concurrent.Callable;
import java.util.concurrent.Executors;
import org.apache.commons.io.FilenameUtils;
import org.jsoar.kernel.SoarException;
import org.jsoar.soarunit.SoarUnit.PrintWriterProxy;
import org.jsoar.soarunit.Test;
import org.jsoar.soarunit.TestAgent;
import org.jsoar.soarunit.TestCase;
import org.jsoar.soarunit.TestResult;
import org.jsoar.soarunit.TestRunner;
import org.jsoar.soarunit.jsoar.JSoarTestAgent;
import org.jsoar.soarunit.jsoar.JSoarTestAgentFactory;
import org.jsoar.util.commands.SoarCommands;
import org.junit.runner.Description;
import org.junit.runner.Runner;
import org.junit.runner.notification.Failure;
import org.junit.runner.notification.RunNotifier;
import org.junit.runners.model.FrameworkMethod;
import org.junit.runners.model.TestClass;
import org.springframework.core.io.Resource;
import org.springframework.core.io.support.PathMatchingResourcePatternResolver;

public class SoarUnitRunner extends Runner {
  private final TestClass testClass;
  private final JSoarTestAgentFactory agentFactory = new AgentFactory();
  private final PrintWriterProxy out;
  private final PathMatchingResourcePatternResolver resolverSoarUnit =
      new PathMatchingResourcePatternResolver();
  private final int POOL_SIZE;
  private final ListeningExecutorService exec =
      MoreExecutors.listeningDecorator(Executors.newFixedThreadPool(8));
  private final ListeningExecutorService runNotifierExec =
      MoreExecutors.listeningDecorator(Executors.newFixedThreadPool(1));
  private final Description rootDescription;
  private final Queue<Function<TestContext, Void>> testRunners = Queues.newConcurrentLinkedQueue();
  private final Collection<ListenableFuture<Void>> runNotifications =
      Collections.synchronizedCollection(Lists.<ListenableFuture<Void>>newArrayList());
  private final List<String> sourceIncludes = Lists.newArrayList();

  public SoarUnitRunner(Class<?> clazz) throws IOException, SoarException {
    PrintWriter pw = new PrintWriter(System.out);
    out = () -> pw;

    this.testClass = new TestClass(clazz);
    if (testClass.getAnnotation(SoarInterpreter.class) != null) {
      String newInterpreter = testClass.getAnnotation(SoarInterpreter.class).interpreter();
      System.setProperty("jsoar.agent.interpreter", newInterpreter);
    }
    if (testClass.getAnnotation(SoarSourceInclude.class) != null) {
      // TODO: When jsoar upgrades to Java 8, turn this into a repeatable.
      sourceIncludes.add(testClass.getAnnotation(SoarSourceInclude.class).url());
    }
    if (testClass.getAnnotation(SoarUnitSettings.class) != null) {
      POOL_SIZE = testClass.getAnnotation(SoarUnitSettings.class).threads();
    } else {
      POOL_SIZE = 1;
    }

    List<FrameworkMethod> annotatedMethods = testClass.getAnnotatedMethods(SoarUnitTestFile.class);
    this.rootDescription = Description.createSuiteDescription(clazz);
    final TestRunner testRunner = new TestRunner(agentFactory, out, exec);
    for (FrameworkMethod method : annotatedMethods) {
      Description testCaseDescription =
          Description.createSuiteDescription(method.getName(), (Annotation[]) null);
      rootDescription.addChild(testCaseDescription);
      SoarUnitTestFile testFile = method.getAnnotation(SoarUnitTestFile.class);
      for (URL url : getResources(testFile.url())) {
        final TestCase testCase = TestCase.fromURL(url, 0);
        String testCaseName = FilenameUtils.getBaseName(url.toString());
        final Map<String, Description> testDescriptions = Maps.newHashMap();
        for (final org.jsoar.soarunit.Test test : testCase.getTests()) {
          String testName = test.getName();
          final Description testDescription =
              Description.createTestDescription(testCaseName, testName);
          testCaseDescription.addChild(testDescription);
          testDescriptions.put(test.getName(), testDescription);
          testRunners.add(
              new Function<TestContext, Void>() {
                @Override
                public Void apply(final TestContext testContext) {
                  final TestResult testResult;
                  try {
                    testContext.agent.reinitialize(test);
                    // XXX: JUnit isn't threadsafe with run notifications - so these all get sent in
                    // their own dedicated thread.
                    runNotifications.add(
                        runNotifierExec.submit(
                            new Callable<Void>() {
                              @Override
                              public Void call() throws Exception {
                                testContext.runNotifier.fireTestStarted(testDescription);
                                return null;
                              }
                            }));
                    testResult = testRunner.runTest(test, testContext.agent);
                    if (!testResult.isPassed()) {
                      runNotifications.add(
                          runNotifierExec.submit(
                              new Callable<Void>() {
                                @SuppressWarnings("serial")
                                @Override
                                public Void call() throws Exception {
                                  testContext.runNotifier.fireTestFailure(
                                      new Failure(
                                          testDescription,
                                          new Exception() {
                                            public String toString() {
                                              return testResult.getMessage()
                                                  + "\n\n"
                                                  + testResult.getOutput();
                                            }
                                          }));
                                  return null;
                                }
                              }));
                    }
                  } catch (final SoarException e) {
                    runNotifications.add(
                        runNotifierExec.submit(
                            new Callable<Void>() {
                              @Override
                              public Void call() throws Exception {
                                testContext.runNotifier.fireTestFailure(
                                    new Failure(testDescription, e));
                                return null;
                              }
                            }));
                  }
                  runNotifications.add(
                      runNotifierExec.submit(
                          new Callable<Void>() {
                            @Override
                            public Void call() throws Exception {
                              testContext.runNotifier.fireTestFinished(testDescription);
                              return null;
                            }
                          }));
                  testContext.runNotifier.fireTestFinished(testDescription);
                  return null;
                }
              });
        }
      }
    }
  }

  @Override
  public Description getDescription() {
    return rootDescription;
  }

  @Override
  public void run(final RunNotifier runNotifier) {
    final JSoarTestAgentFactory agentFactory = new JSoarTestAgentFactory();
    List<ListenableFuture<?>> wait = Lists.newArrayList();
    for (int i = 0; i < POOL_SIZE; i++) {
      wait.add(
          exec.submit(
              new Callable<Void>() {
                @Override
                public Void call() throws Exception {
                  TestAgent agent = agentFactory.createTestAgent();
                  Function<TestContext, Void> item = null;
                  while ((item = testRunners.poll()) != null) {
                    item.apply(new TestContext(runNotifier, agent));
                  }
                  return null;
                }
              }));
    }

    // Block until completion.
    try {
      Futures.successfulAsList(wait).get();
      Futures.successfulAsList(runNotifications).get();
    } catch (Exception e) {
    } // note this catches an InterruptedException, but it might be safe in this case since the next
    // step is to shutdown
    exec.shutdown();
    runNotifierExec.shutdown();
  }

  private List<URL> getResources(String path) throws IOException {
    if (path.startsWith("classpath:")) {
      List<Resource> resources = Arrays.asList(resolverSoarUnit.getResources(path));
      List<URL> urls = Lists.newArrayList();
      for (Resource resource : resources) {
        urls.add(resource.getURL());
      }
      return urls;
    } else {
      return Lists.newArrayList(new URL(path));
    }
  }

  private class AgentFactory extends JSoarTestAgentFactory {
    private final JSoarTestAgent testAgent =
        new JSoarTestAgent() {
          @Override
          public void initialize(Test test) throws SoarException {
            super.initialize(test);
            attachSource();
          }
        };

    @Override
    public TestAgent createTestAgent() {
      return testAgent;
    }

    private void attachSource() {
      try {
        for (String resource : sourceIncludes) {
          for (URL url : getResources(resource)) {
            SoarCommands.source(testAgent.getInterpreter(), url);
          }
        }
      } catch (IOException | SoarException e) {
        throw new RuntimeException(e);
      }
    }

    /* (non-Javadoc)
     * @see org.jsoar.soarunit.TestAgentFactory#debugTest(org.jsoar.soarunit.Test)
     */
    @Override
    public void debugTest(Test test, boolean exitOnClose)
        throws SoarException, InterruptedException {
      testAgent.debug(test, exitOnClose);
    }
  }

  private static class TestContext {
    public final RunNotifier runNotifier;
    public final TestAgent agent;

    private TestContext(RunNotifier runNotifier, TestAgent agent) {
      this.runNotifier = runNotifier;
      this.agent = agent;
    }
  }
}
