package org.jsoar.performancetesting.csoar;

/**
 * This is a wrapper class around the sml.Kernel. It implements all the functions that the
 * performance testing framework from sml.Kernel
 *
 * @author ALT
 */
public interface CSoarKernelWrapper {
  CSoarAgentWrapper CreateAgent(String name);

  void DestroyAgent(CSoarAgentWrapper agent);

  void StopEventThread();

  void Shutdown();
}
