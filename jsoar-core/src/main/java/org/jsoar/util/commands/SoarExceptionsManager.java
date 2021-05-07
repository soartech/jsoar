package org.jsoar.util.commands;

import java.util.ArrayList;
import java.util.List;
import org.jsoar.kernel.exceptions.SoftInterpreterException;

public class SoarExceptionsManager {

  private List<SoftInterpreterException> exceptions;

  public SoarExceptionsManager() {
    exceptions = new ArrayList<>();
  }

  public void addException(Exception e, SoarCommandContext context, String production) {
    addException(new SoftInterpreterException(e.getMessage(), context, production));
  }

  public void addException(String msg, SoarCommandContext context, String production) {
    addException(new SoftInterpreterException(msg, context, production));
  }

  public void addException(SoftInterpreterException ex) {
    exceptions.add(ex);
  }

  public List<SoftInterpreterException> getExceptions() {
    return exceptions;
  }

  public void clearExceptions() {
    exceptions.clear();
  }
}
