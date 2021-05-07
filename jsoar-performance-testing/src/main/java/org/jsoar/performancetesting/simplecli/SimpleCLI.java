package org.jsoar.performancetesting.simplecli;

import java.io.IOException;
import java.io.Writer;
import java.util.Arrays;
import java.util.Scanner;
import org.jsoar.kernel.SoarException;
import org.jsoar.runtime.ThreadedAgent;

public class SimpleCLI {

  ThreadedAgent agent = ThreadedAgent.create("CLI Agent");
  Scanner scanner = new Scanner(System.in);

  public static void main(String[] args) throws SoarException {
    SimpleCLI simpleCLI = new SimpleCLI();
    simpleCLI.run();
  }

  private void run() throws SoarException {
    boolean continu = true;
    agent.getTrace().getPrinter().addPersistentWriter(new ConsoleWriter());
    while (continu) {
      String command = scanner.nextLine();

      switch (command) {
        case "exit":
          continu = false;
          break;
        case "gc":
          System.gc();
          System.gc();
          break;
        default:
          String result = "CLI failed to execute command";
          try {
            result = agent.getAgent().getInterpreter().eval(command);
          } catch (SoarException e) {
          }
          System.out.println(result);
          break;
      }
    }

    scanner.close();
    agent.dispose();
  }

  private static class ConsoleWriter extends Writer {

    @Override
    public void close() throws IOException {}

    @Override
    public void flush() throws IOException {}

    @Override
    public void write(char[] cbuf, int off, int len) throws IOException {
      System.out.print(Arrays.copyOfRange(cbuf, off, off + len));
    }
  }
}
