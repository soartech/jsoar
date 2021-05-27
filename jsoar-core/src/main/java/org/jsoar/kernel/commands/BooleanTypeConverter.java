package org.jsoar.kernel.commands;

import lombok.NonNull;
import picocli.CommandLine.ITypeConverter;

public class BooleanTypeConverter implements ITypeConverter<Boolean> {

  @Override
  public Boolean convert(@NonNull String value) {
    switch (value.toLowerCase()) {
      case "true", "yes", "enable", "on":
        return Boolean.TRUE;
      case "false", "no", "disable", "off":
        return Boolean.FALSE;
      default:
        throw new IllegalArgumentException("Expected one argument: on | off");
    }
  }
}
