package org.jsoar.util.commands;

import org.jsoar.kernel.exceptions.SoftTclInterpreterException;

import java.util.ArrayList;
import java.util.List;

public class SoarTclExceptionsManager {

    private List<SoftTclInterpreterException> exceptions;

    public SoarTclExceptionsManager() {
        exceptions = new ArrayList<>();
    }

    public void addException(SoftTclInterpreterException ex) {
        exceptions.add(ex);
    }

    public List<SoftTclInterpreterException> getExceptions() {
        return exceptions;
    }
}
