/*
 * Copyright (c) 2008  Dave Ray <daveray@gmail.com>
 *
 * Created on Oct 13, 2008
 */
package org.jsoar.kernel.parser;

import org.jsoar.kernel.parser.original.Lexer;

/**
 * @author ray
 * @see Lexer#determine_possible_symbol_types_for_string(String)
 */
public class PossibleSymbolTypes {
  public boolean possible_id;
  public boolean possible_var;
  public boolean possible_sc;
  public boolean possible_ic;
  public boolean possible_fc;
  public boolean rereadable;
}
