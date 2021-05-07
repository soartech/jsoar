/** */
package org.jsoar.performancetesting.csoar;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;

/**
 * This is the implementation of a loaded sml.Kernel class using the CSoarKernelWrapper class. It
 * implements all functions that are used by the tests.
 *
 * @author ALT
 */
public class ImplCSoarKernelWrapper implements CSoarKernelWrapper {
  private Object kernelImpl;

  private Class<?> kernel;

  private Class<?> agent;

  private Method createAgent;

  private Method destroyAgent;

  private Method stopEventThread;

  private Method shutdown;

  /**
   * Initializes the kernel class and retrieves all the used methods via reflection.
   *
   * @param kernelImpl
   * @param kernel
   * @param agent
   */
  public ImplCSoarKernelWrapper(Object kernelImpl, Class<?> kernel, Class<?> agent) {
    this.kernelImpl = kernelImpl;
    this.kernel = kernel;
    this.agent = agent;

    try {
      createAgent = this.kernel.getDeclaredMethod("CreateAgent", String.class);
      destroyAgent = this.kernel.getDeclaredMethod("DestroyAgent", agent);
      stopEventThread = this.kernel.getDeclaredMethod("StopEventThread");
      shutdown = this.kernel.getDeclaredMethod("Shutdown");
    } catch (NoSuchMethodException | SecurityException e) {
      System.out.println("Failed to load methods for kernel class! Everything will fail horribly!");
      System.out.println(e.getMessage());
    }
  }

  /*
   * (non-Javadoc)
   *
   * @see
   * org.jsoar.performancetesting.csoar.CSoarKernelWrapper#CreateAgent(java
   * .lang.String)
   */
  @Override
  public CSoarAgentWrapper CreateAgent(String name) {
    try {
      Object agentImpl = createAgent.invoke(kernelImpl, name);
      return new ImplCSoarAgentWrapper(agentImpl, agent);
    } catch (IllegalAccessException | IllegalArgumentException | InvocationTargetException e) {
      System.out.println("Failed to create agent! Everything will fail horribly!");
      return new DefaultCSoarAgentWrapper();
    }
  }

  /*
   * (non-Javadoc)
   *
   * @see
   * org.jsoar.performancetesting.csoar.CSoarKernelWrapper#DestroyAgent(org
   * .jsoar.performancetesting.csoar.CSoarAgentWrapper)
   */
  @Override
  public void DestroyAgent(CSoarAgentWrapper agent) {
    try {
      destroyAgent.invoke(kernelImpl, agent.getAgentImpl());
    } catch (IllegalAccessException | IllegalArgumentException | InvocationTargetException e) {
      System.out.println("Failed to destroy agent! Everything will fail horribly!");
    }
  }

  @Override
  public void StopEventThread() {
    try {
      stopEventThread.invoke(kernelImpl);
    } catch (IllegalAccessException | IllegalArgumentException | InvocationTargetException e) {
      System.out.println(
          "Failed to stop event thread! This may affect some older versions of Soar!");
    }
  }

  @Override
  public void Shutdown() {
    try {
      shutdown.invoke(kernelImpl);
    } catch (IllegalAccessException | IllegalArgumentException | InvocationTargetException e) {
      System.out.println("Failed to shutdown kernel! This may affect some older versions of Soar!");
    }
  }
}
