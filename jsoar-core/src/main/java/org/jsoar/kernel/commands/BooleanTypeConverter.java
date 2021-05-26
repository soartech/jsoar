package org.jsoar.kernel.commands;

import picocli.CommandLine;
import picocli.CommandLine.ITypeConverter;
import picocli.CommandLine.ParameterException;
import picocli.CommandLine.PicocliException;

public class BooleanTypeConverter implements ITypeConverter<Boolean> {

  @Override
  public Boolean convert(String value) throws Exception {
    if (value.equalsIgnoreCase("true")
        || value.equalsIgnoreCase("yes")
        || value.equalsIgnoreCase("enable")
        || value.equalsIgnoreCase("on")) {
      return Boolean.TRUE;
    } else if (
        value.equalsIgnoreCase("false")
        || value.equalsIgnoreCase("no")
        || value.equalsIgnoreCase("disable")
        || value.equalsIgnoreCase("off")) {
      return Boolean.FALSE;
    } else
      throw new PicocliException("Expected one argument: on | off");
    }
}
