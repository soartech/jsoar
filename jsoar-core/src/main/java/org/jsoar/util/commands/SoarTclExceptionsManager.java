package org.jsoar.util.commands;

import org.jsoar.kernel.exceptions.SoftTclInterpreterException;

import java.util.ArrayList;
import java.util.List;

public class SoarTclExceptionsManager {

    private List<SoftTclInterpreterException> exceptions;

    public SoarTclExceptionsManager() {
        exceptions = new ArrayList<>();
    }

    public void addException(Exception e, SoarCommandContext context, String production) {
        addException(new SoftTclInterpreterException(e.getMessage(), context, production));
    }

    public void addException(String msg, SoarCommandContext context, String production) {
        addException(new SoftTclInterpreterException(msg, context, production));
    }

    public void addException(SoftTclInterpreterException ex) {
        exceptions.add(ex);
    }

    public List<SoftTclInterpreterException> getExceptions() {
        return exceptions;
    }

    public void clearExceptions() {
        exceptions.clear();
    }
}
