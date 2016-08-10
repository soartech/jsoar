package org.jsoar.soarunit.junit;

import com.google.common.base.Function;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.google.common.util.concurrent.Futures;
import com.google.common.util.concurrent.ListenableFuture;
import com.google.common.util.concurrent.ListeningExecutorService;
import com.google.common.util.concurrent.MoreExecutors;
import org.apache.commons.io.FilenameUtils;
import org.jsoar.kernel.SoarException;
import org.jsoar.soarunit.*;
import org.jsoar.soarunit.jsoar.JSoarTestAgent;
import org.jsoar.soarunit.jsoar.JSoarTestAgentFactory;
import org.jsoar.util.commands.SoarCommands;
import org.junit.runner.Description;
import org.junit.runner.Result;
import org.junit.runner.Runner;
import org.junit.runner.notification.Failure;
import org.junit.runner.notification.RunNotifier;
import org.junit.runners.model.FrameworkMethod;
import org.junit.runners.model.TestClass;
import org.springframework.core.io.Resource;
import org.springframework.core.io.support.PathMatchingResourcePatternResolver;

import java.io.IOException;
import java.io.PrintWriter;
import java.lang.annotation.Annotation;
import java.lang.reflect.Method;
import java.net.URL;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicInteger;

public class SoarUnitRunner extends Runner
{
    private final TestClass testClass;
    private final JSoarTestAgentFactory agentFactory = new AgentFactory();
    private final PrintWriter out = new PrintWriter(System.out);
    private final PathMatchingResourcePatternResolver resolverSoarUnit = new PathMatchingResourcePatternResolver();
    // TODO: Make parallel executor choice configurable via attribute.
    private final ListeningExecutorService exec = MoreExecutors.listeningDecorator(Executors.newFixedThreadPool(1));
    private final Description rootDescription;
    private final List<Function<RunNotifier, ListenableFuture<TestCaseResult>>> testRunners = Lists.newArrayList();
    private final List<String> sourceIncludes = Lists.newArrayList();

    public SoarUnitRunner(Class clazz) throws IOException, SoarException
    {
        this.testClass = new TestClass(clazz);
        if (testClass.getAnnotation(SoarInterpreter.class) != null)
        {
            String newInterpreter = testClass.getAnnotation(SoarInterpreter.class).interpreter();
            System.setProperty("jsoar.agent.interpreter", newInterpreter);
        }
        if (testClass.getAnnotation(SoarSourceInclude.class) != null || testClass.getAnnotation(SoarSourceIncludes.class) != null)
        {
            List<Annotation> annotations = Arrays.asList(testClass.getAnnotations());
            for(Annotation a : annotations)
            {
                if (a instanceof SoarSourceInclude)
                {
                    sourceIncludes.add(((SoarSourceInclude) a).url());
                }
                else if (a instanceof SoarSourceIncludes)
                {
                    List<SoarSourceInclude> soarSourceIncludes = Arrays.asList(((SoarSourceIncludes) a).value());
                    for(SoarSourceInclude include : soarSourceIncludes)
                    {
                        sourceIncludes.add(include.url());
                    }
                }
            }
        }

        List<Method> methods = Arrays.asList(clazz.getDeclaredMethods());
        List<FrameworkMethod> annotatedMethods = testClass.getAnnotatedMethods(SoarUnitTestFile.class);
        final AtomicInteger index = new AtomicInteger(0);
        this.rootDescription = Description.createSuiteDescription(clazz);
        for (FrameworkMethod method : annotatedMethods)
        {
            Description testCaseDescription = Description.createSuiteDescription(method.getName(), null);
            rootDescription.addChild(testCaseDescription);
            SoarUnitTestFile testFile = method.getAnnotation(SoarUnitTestFile.class);
            for(URL url : getResources(testFile.url()))
            {
                final TestCase testCase = TestCase.fromURL(url);
                String testCaseName = FilenameUtils.getBaseName(url.toString());
                final Map<String, Description> testDescriptions = Maps.newHashMap();
                for(org.jsoar.soarunit.Test test : testCase.getTests())
                {
                    String testName = test.getName();
                    Description testDescription = Description.createTestDescription(testCaseName, testName);
                    testCaseDescription.addChild(testDescription);
                    testDescriptions.put(test.getName(), testDescription);
                }

                testRunners.add(new Function<RunNotifier, ListenableFuture<TestCaseResult>>()
                {
                    @Override
                    public ListenableFuture<TestCaseResult> apply(final RunNotifier runNotifier)
                    {
                        TestRunner testRunner = new TestRunner(agentFactory, out, exec);
                        return exec.submit(testRunner.createTestCaseRunner(testCase, new TestCaseResultHandler()
                        {
                            @Override
                            public void handleTestCaseResult(TestCaseResult result)
                            {
                                for(final TestResult r : result.getTestResults())
                                {
                                    Description description = testDescriptions.get(r.getTest().getName());
                                    if (description == null)
                                    {
                                        continue;
                                    }
                                    runNotifier.fireTestStarted(description);
                                    if (r.isPassed())
                                    {
                                    }
                                    else
                                    {
                                        runNotifier.fireTestFailure(new Failure(description, new Exception() {
                                            public String toString()
                                            {
                                                return r.getMessage() + "\n\n" + r.getOutput();
                                            }
                                        }));
                                    }
                                    runNotifier.fireTestFinished(description);
                                }
                            }
                        }, index.getAndIncrement()));
                    }
                });
            }
        }
    }

    @Override
    public Description getDescription()
    {
        return rootDescription;
    }

    @Override
    public void run(RunNotifier runNotifier)
    {
        List<ListenableFuture<?>> wait = Lists.newArrayList();
        for (Function<RunNotifier, ListenableFuture<TestCaseResult>> f : testRunners)
        {
            wait.add(f.apply(runNotifier));
        }
        // Block until completion.
        try {
            Futures.successfulAsList(wait).get();
        } catch (Exception e) {
        }
    }

    private List<URL> getResources(String path) throws IOException
    {
        if (path.startsWith("classpath:"))
        {
            List<Resource> resources = Arrays.asList(resolverSoarUnit.getResources(path));
            List<URL> urls = Lists.newArrayList();
            for(Resource resource : resources)
            {
                urls.add(resource.getURL());
            }
            return urls;
        }
        else
        {
            return Lists.newArrayList(new URL(path));
        }
    }

    private class AgentFactory extends JSoarTestAgentFactory
    {
        private final JSoarTestAgent testAgent = new JSoarTestAgent() {
            @Override
            public void initialize(Test test) throws SoarException
            {
                super.initialize(test);
                attachSource();
            }
        };

        @Override
        public TestAgent createTestAgent()
        {
            return testAgent;
        }

        private void attachSource()
        {
            try
            {
                for(String resource : sourceIncludes)
                {
                    for(URL url : getResources(resource))
                    {
                        SoarCommands.source(testAgent.getInterpreter(), url);
                    }
                }
            }
            catch (Exception e) {
                throw new RuntimeException(e);
            }
        }

        /* (non-Javadoc)
         * @see org.jsoar.soarunit.TestAgentFactory#debugTest(org.jsoar.soarunit.Test)
         */
        @Override
        public void debugTest(Test test, boolean exitOnClose) throws SoarException, InterruptedException
        {
            testAgent.debug(test, exitOnClose);
        }
    }
}
