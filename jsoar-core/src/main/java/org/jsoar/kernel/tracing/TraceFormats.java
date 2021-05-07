/*
 * Copyright (c) 2008  Dave Ray <daveray@gmail.com>
 *
 * Created on Sep 22, 2008
 */
package org.jsoar.kernel.tracing;

import java.io.IOException;
import java.io.Writer;
import java.util.ArrayList;
import java.util.EnumMap;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import org.jsoar.kernel.Agent;
import org.jsoar.kernel.DecisionCycle;
import org.jsoar.kernel.PredefinedSymbols;
import org.jsoar.kernel.SoarProperties;
import org.jsoar.kernel.memory.Slot;
import org.jsoar.kernel.memory.WmeImpl;
import org.jsoar.kernel.symbols.IdentifierImpl;
import org.jsoar.kernel.symbols.StringSymbolImpl;
import org.jsoar.kernel.symbols.Symbol;
import org.jsoar.kernel.symbols.SymbolImpl;
import org.jsoar.util.adaptables.Adaptables;
import org.jsoar.util.markers.DefaultMarker;
import org.jsoar.util.markers.Marker;

/**
 * Object and stack trace formats are managed by this module.
 *
 * <p>Init_tracing() initializes the tables; at this point, there are no trace formats for anything.
 * This routine should be called at startup time.
 *
 * <p>Trace formats are changed by calls to add_trace_format() and remove_trace_format().
 * Add_trace_format() returns TRUE if the format was successfully added, or FALSE if the format
 * string didn't parse right. Remove_trace_format() returns TRUE if a trace format was actually
 * removed, or FALSE if there was no such trace format for the given type/name restrictions. These
 * routines take a "stack_trace" argument, which should be TRUE if the stack trace format is
 * intended, or FALSE if the object trace format is intended. Their "type_restriction" argument
 * should be one of FOR_ANYTHING_TF, ..., FOR_OPERATORS_TF. The "name_restriction" argument should
 * be either a pointer to a symbol, if the trace format is restricted to apply to objects with that
 * name, or NIL if the format can apply to any object.
 *
 * <p>Print_all_trace_formats() prints out either all existing stack trace or object trace formats.
 *
 * <p>Print_object_trace() takes an object (any symbol). It prints the trace for that object.
 * Print_stack_trace() takes a (context) object (the state or op), the current state, the
 * "slot_type" (one of FOR_OPERATORS_TF, etc.), and a flag indicating whether to allow %dc and %ec
 * escapes (this flag should normally be TRUE for watch 0 traces but FALSE during a "print -stack"
 * command). It prints the stack trace for that context object.
 *
 * <p>trace.cpp
 *
 * <p>The following functioms from trace.cpp weren't implemented: print_tracing_rule:unimplemented
 * print_tracing_rule_tcl:unimplemented print_trace_callback_fn:unimplemented
 * print_all_trace_formats:unimplemented print_all_trace_formats_tcl:unimplemented
 * set_print_trace_formats:unimplemented set_tagged_trace_formats:unimplemented
 * print_stack_trace_xml:unimplemented print_object_trace_using_provided_format_string:unimplemented
 * print_trace_format_list:not used (removed in jsoar trunk revision 207)
 *
 * @author ray
 */
public class TraceFormats {
  private final Agent context;
  private PredefinedSymbols predefinedSyms;
  private DecisionCycle decisionCycle;

  private String format;
  private int offset;
  private String format_string_error_message;

  private static Map<TraceFormatRestriction, Map<SymbolImpl, TraceFormat>> createMap() {
    return new EnumMap<TraceFormatRestriction, Map<SymbolImpl, TraceFormat>>(
        TraceFormatRestriction.class);
  }

  // Converted to Java maps from Soar hashtables
  private Map<TraceFormatRestriction, Map<SymbolImpl, TraceFormat>> stack_tr_ht = createMap();
  private Map<TraceFormatRestriction, Map<SymbolImpl, TraceFormat>> object_tr_ht = createMap();
  private Map<TraceFormatRestriction, TraceFormat> stack_tf_for_anything =
      new EnumMap<TraceFormatRestriction, TraceFormat>(TraceFormatRestriction.class);
  private Map<TraceFormatRestriction, TraceFormat> object_tf_for_anything =
      new EnumMap<TraceFormatRestriction, TraceFormat>(TraceFormatRestriction.class);

  /**
   * set to TRUE whenever an escape sequence result is undefined--for use with %ifdef
   *
   * <p>trace.cpp:921:found_undefined
   */
  private boolean found_undefined = false;

  /** trace.cpp:924:tracing_parameters */
  private static class TracingParameters {
    IdentifierImpl current_s = null; // current state, etc. -- for use in %cs, etc.
    IdentifierImpl current_o = null;
    boolean allow_cycle_counts = false; // TRUE means allow %dc and %ec

    public TracingParameters() {}

    public TracingParameters(TracingParameters other) {
      this.current_s = other.current_s;
      this.current_o = other.current_o;
      this.allow_cycle_counts = other.allow_cycle_counts;
    }
  }

  /** trace.cpp:928:tparams */
  private TracingParameters tparams = new TracingParameters();

  private Marker tf_printing_tc;

  /** @param context */
  public TraceFormats(Agent context) {
    this.context = context;

    // trace.cpp:654:init_tracing
    for (TraceFormatRestriction r : TraceFormatRestriction.values()) {
      stack_tr_ht.put(r, new HashMap<SymbolImpl, TraceFormat>());
      object_tr_ht.put(r, new HashMap<SymbolImpl, TraceFormat>());
    }
  }

  public void initalize() {
    this.predefinedSyms = Adaptables.adapt(this.context, PredefinedSymbols.class);
    this.decisionCycle = Adaptables.adapt(this.context, DecisionCycle.class);
  }

  /**
   * parses a format string and returns a trace_format structure for it, or NIL if any error
   * occurred. This is the top-level routine here.
   *
   * <p>trace.cpp:152:parse_format_string
   *
   * @param string
   */
  private TraceFormat parse_format_string(String string) {
    format = string;

    TraceFormat prev = null;
    TraceFormat first = null;
    offset = 0;
    while (offset < string.length()) {
      TraceFormat New = parse_item_from_format_string();
      if (New == null) {
        context.getPrinter().error("Error:  bad trace format string: %s\n", string);
        if (format_string_error_message != null) {
          context
              .getPrinter()
              .print(" %s\n Error found at: %s\n", format_string_error_message, format);
        }
        return null;
      }
      if (prev != null) prev.next = New;
      else first = New;
      prev = New;
    }
    if (prev != null) prev.next = null;
    else first = null;

    return first;
  }

  /** trace.cpp:180:parse_attribute_path_in_brackets */
  private List<Symbol> parse_attribute_path_in_brackets() {
    /* --- look for opening bracket --- */
    if (format.charAt(offset) != '[') {
      format_string_error_message = "Expected '[' followed by attribute (path)";
      return null;
    }
    offset++;

    List<Symbol> path = null;

    /* --- check for '*' (null path) --- */
    if (format.charAt(offset) == '*') {
      path = null;
      offset++;
    } else {
      /* --- normal case: read the attribute path --- */
      path = new ArrayList<Symbol>();
      while (true) {
        String name = "";
        while ((offset != format.length())
            && (format.charAt(offset) != ']')
            && (format.charAt(offset) != '.')) {
          name += format.charAt(offset);
          offset++;
        }
        if (offset == format.length()) {
          format_string_error_message = "'[' without closing ']'";
          return null;
        }
        if (name.length() == 0) {
          format_string_error_message = "null attribute found in attribute path";
          return null;
        }
        path.add(context.getSymbols().createString(name));
        if (format.charAt(offset) == ']') break;
        offset++; /* skip past '.' */
      }
    }

    /* --- look for closing bracket --- */
    if (format.charAt(offset) != ']') {
      format_string_error_message = "'[' without closing ']'";
      return null;
    }
    offset++;

    return path;
  }

  /**
   * trace.cpp:232:parse_pattern_in_brackets
   *
   * @param read_opening_bracket
   */
  private TraceFormat parse_pattern_in_brackets(boolean read_opening_bracket) {
    /* --- look for opening bracket --- */
    if (read_opening_bracket) {
      if (format.charAt(offset) != '[') {
        format_string_error_message = "Expected '[' followed by attribute path";
        return null;
      }
      offset++;
    }

    /* --- read pattern --- */
    TraceFormat prev = null;
    TraceFormat first = null;
    while ((offset < format.length()) && (format.charAt(offset) != ']')) {
      TraceFormat New = parse_item_from_format_string();
      if (New == null) {
        return null;
      }
      if (prev != null) prev.next = New;
      else first = New;
      prev = New;
    }
    if (prev != null) prev.next = null;
    else first = null;

    /* --- look for closing bracket --- */
    if (format.charAt(offset) != ']') {
      format_string_error_message = "'[' without closing ']'";
      return null;
    }
    offset++;

    return first;
  }

  /** trace.cpp:270:parse_item_from_format_string */
  private TraceFormat parse_item_from_format_string() {
    if (offset >= format.length()) return null;
    if (format.charAt(offset) == ']') return null;
    if (format.charAt(offset) == '[') {
      format_string_error_message = "unexpected '[' character";
      return null;
    }

    if (format.charAt(offset) != '%') {

      String buf = "";
      while ((offset < format.length())
          && (format.charAt(offset) != '%')
          && (format.charAt(offset) != '[')
          && (format.charAt(offset) != ']')) {
        buf += format.charAt(offset);
        offset++;
      }
      TraceFormat tf = new TraceFormat();
      tf.type = TraceFormatType.STRING_TFT;
      tf.data_string = buf;
      return tf;
    }

    /* --- otherwise *format is '%', so parse the escape sequence --- */

    if (format.startsWith("%v", offset)) {
      offset += 2;
      List<Symbol> attribute_path = parse_attribute_path_in_brackets();
      if (format_string_error_message != null) return null;
      TraceFormat tf = new TraceFormat();
      tf.type = TraceFormatType.VALUES_TFT;
      tf.data_attribute_path = attribute_path;
      return tf;
    }

    if (format.startsWith("%o", offset)) {
      offset += 2;
      List<Symbol> attribute_path = parse_attribute_path_in_brackets();
      if (format_string_error_message != null) return null;
      TraceFormat tf = new TraceFormat();
      tf.type = TraceFormatType.VALUES_RECURSIVELY_TFT;
      tf.data_attribute_path = attribute_path;
      return tf;
    }

    if (format.startsWith("%av", offset)) {
      offset += 3;
      List<Symbol> attribute_path = parse_attribute_path_in_brackets();
      if (format_string_error_message != null) return null;
      TraceFormat tf = new TraceFormat();
      tf.type = TraceFormatType.ATTS_AND_VALUES_TFT;
      tf.data_attribute_path = attribute_path;
      return tf;
    }

    if (format.startsWith("%ao", offset)) {
      offset += 3;
      List<Symbol> attribute_path = parse_attribute_path_in_brackets();
      if (format_string_error_message != null) return null;
      TraceFormat tf = new TraceFormat();
      tf.type = TraceFormatType.ATTS_AND_VALUES_RECURSIVELY_TFT;
      tf.data_attribute_path = attribute_path;
      return tf;
    }

    if (format.startsWith("%cs", offset)) {
      offset += 3;
      TraceFormat tf = new TraceFormat();
      tf.type = TraceFormatType.CURRENT_STATE_TFT;
      return tf;
    }

    if (format.startsWith("%co", offset)) {
      offset += 3;
      TraceFormat tf = new TraceFormat();
      tf.type = TraceFormatType.CURRENT_OPERATOR_TFT;
      return tf;
    }

    if (format.startsWith("%dc", offset)) {
      offset += 3;
      TraceFormat tf = new TraceFormat();
      tf.type = TraceFormatType.DECISION_CYCLE_COUNT_TFT;
      return tf;
    }

    if (format.startsWith("%ec", offset)) {
      offset += 3;
      TraceFormat tf = new TraceFormat();
      tf.type = TraceFormatType.ELABORATION_CYCLE_COUNT_TFT;
      return tf;
    }

    if (format.startsWith("%%", offset)) {
      offset += 2;
      TraceFormat tf = new TraceFormat();
      tf.type = TraceFormatType.PERCENT_TFT;
      return tf;
    }

    if (format.startsWith("%[", offset)) {
      offset += 2;
      TraceFormat tf = new TraceFormat();
      tf.type = TraceFormatType.L_BRACKET_TFT;
      return tf;
    }

    if (format.startsWith("%]", offset)) {
      offset += 2;
      TraceFormat tf = new TraceFormat();
      tf.type = TraceFormatType.R_BRACKET_TFT;
      return tf;
    }

    if (format.startsWith("%sd", offset)) {
      offset += 3;
      TraceFormat tf = new TraceFormat();
      tf.type = TraceFormatType.SUBGOAL_DEPTH_TFT;
      return tf;
    }

    if (format.startsWith("%id", offset)) {
      offset += 3;
      TraceFormat tf = new TraceFormat();
      tf.type = TraceFormatType.IDENTIFIER_TFT;
      return tf;
    }

    if (format.startsWith("%ifdef", offset)) {
      offset += 6;
      TraceFormat pattern = parse_pattern_in_brackets(true);
      if (format_string_error_message != null) return null;
      TraceFormat tf = new TraceFormat();
      tf.type = TraceFormatType.IF_ALL_DEFINED_TFT;
      tf.data_subformat = pattern;
      return tf;
    }

    if (format.startsWith("%left", offset)) {
      offset += 5;
      if (format.charAt(offset) != '[') {
        format_string_error_message = "Expected '[' after %left";
        return null;
      }
      offset++;
      if (!Character.isDigit(format.charAt(offset))) {
        format_string_error_message = "Expected number with %left";
        return null;
      }
      int n = 0;
      while (Character.isDigit(format.charAt(offset))) n = 10 * n + (format.charAt(offset++) - '0');
      if (format.charAt(offset) != ',') {
        format_string_error_message = "Expected ',' after number in %left";
        return null;
      }
      offset++;
      TraceFormat pattern = parse_pattern_in_brackets(false);
      if (format_string_error_message != null) return null;
      TraceFormat tf = new TraceFormat();
      tf.type = TraceFormatType.LEFT_JUSTIFY_TFT;
      tf.num = n;
      tf.data_subformat = pattern;
      return tf;
    }

    if (format.startsWith("%right", offset)) {
      offset += 6;
      if (format.charAt(offset) != '[') {
        format_string_error_message = "Expected '[' after %right";
        return null;
      }
      offset++;
      if (!Character.isDigit(format.charAt(offset))) {
        format_string_error_message = "Expected number with %right";
        return null;
      }
      int n = 0;
      while (Character.isDigit(format.charAt(offset))) n = 10 * n + (format.charAt(offset++) - '0');
      if (format.charAt(offset) != ',') {
        format_string_error_message = "Expected ',' after number in %right";
        return null;
      }
      offset++;
      TraceFormat pattern = parse_pattern_in_brackets(false);
      if (format_string_error_message != null) return null;
      TraceFormat tf = new TraceFormat();
      tf.type = TraceFormatType.RIGHT_JUSTIFY_TFT;
      tf.num = n;
      tf.data_subformat = pattern;
      return tf;
    }

    if (format.startsWith("%rsd", offset)) {
      offset += 4;
      TraceFormat pattern = parse_pattern_in_brackets(true);
      if (format_string_error_message != null) return null;
      TraceFormat tf = new TraceFormat();
      tf.type = TraceFormatType.REPEAT_SUBGOAL_DEPTH_TFT;
      tf.data_subformat = pattern;
      return tf;
    }

    if (format.startsWith("%nl", offset)) {
      offset += 3;
      TraceFormat tf = new TraceFormat();
      tf.type = TraceFormatType.NEWLINE_TFT;
      return tf;
    }

    /* --- if we haven't recognized it yet, we don't understand it --- */
    format_string_error_message = "Unrecognized escape sequence";
    return null;
  }

  /**
   * returns the trace format matching a given type restriction and/or name restriction, or NIL if
   * no such format has been specified.
   *
   * <p>trace.cpp:665:lookup_trace_format
   *
   * @param stack_trace
   * @param type_restriction
   * @param name_restriction
   */
  private TraceFormat lookup_trace_format(
      boolean stack_trace, TraceFormatRestriction type_restriction, SymbolImpl name_restriction) {

    if (name_restriction != null) {
      if (stack_trace) return stack_tr_ht.get(type_restriction).get(name_restriction);
      else return object_tr_ht.get(type_restriction).get(name_restriction);
    }
    /* --- no name restriction --- */
    if (stack_trace) return stack_tf_for_anything.get(type_restriction);
    else return object_tf_for_anything.get(type_restriction);
  }

  /**
   * returns TRUE if a trace format was actually removed, or FALSE if there was no such trace format
   * for the given type/name restrictions.
   *
   * <p>trace.cpp:691:remove_trace_format
   *
   * @param stack_trace
   * @param type_restriction
   * @param name_restriction
   * @return true on success
   */
  public boolean remove_trace_format(
      boolean stack_trace, TraceFormatRestriction type_restriction, SymbolImpl name_restriction) {
    if (name_restriction != null) {
      if (stack_trace) return null != stack_tr_ht.get(type_restriction).remove(name_restriction);
      else return null != object_tr_ht.get(type_restriction).remove(name_restriction);
    }
    /* --- no name restriction --- */
    if (stack_trace) return null != stack_tf_for_anything.remove(type_restriction);
    else return null != object_tf_for_anything.remove(type_restriction);
  }

  /**
   * trace.cpp:727:add_trace_format
   *
   * @param stack_trace
   * @param type_restriction
   * @param name_restriction
   * @param format_string
   * @return true on success
   */
  public boolean add_trace_format(
      boolean stack_trace,
      TraceFormatRestriction type_restriction,
      SymbolImpl name_restriction,
      String format_string) {

    // parse the format string into a trace_format
    TraceFormat new_tf = parse_format_string(format_string);
    if (new_tf == null) return false;

    // first remove any existing trace format with same conditions
    remove_trace_format(stack_trace, type_restriction, name_restriction);

    /* --- now add the new one --- */
    if (name_restriction != null) {
      if (stack_trace) stack_tr_ht.get(type_restriction).put(name_restriction, new_tf);
      else object_tr_ht.get(type_restriction).put(name_restriction, new_tf);
      return true;
    }
    /* --- no name restriction --- */
    if (stack_trace) stack_tf_for_anything.put(type_restriction, new_tf);
    else object_tf_for_anything.put(type_restriction, new_tf);

    return true;
  }

  /**
   * Adds all values of the given attribute path off the given object to the "*result"
   * growable_string. If "recursive" is TRUE, the values are printed recursively as objects, rather
   * than as simple atomic values. "*count" is incremented each time a value is printed. (To get a
   * count of how many values were printed, the caller should initialize this to 0, then call this
   * routine.)
   *
   * <p>trace.cpp:939:add_values_of_attribute_path
   *
   * @param object
   * @param path
   * @param pathIndex
   * @param result
   * @param recursive
   * @param count the current count
   * @return the new count
   */
  private int add_values_of_attribute_path(
      SymbolImpl object,
      List<Symbol> path,
      int pathIndex,
      StringBuilder result,
      boolean recursive,
      int count) {
    if (pathIndex >= path.size()) {
      /* path is NIL, so we've reached the end of the path */
      result.append(" ");
      if (recursive) {
        result.append(object_to_trace_string(object));
      } else {
        result.append(String.format("%s", object));
      }
      count++;
      return count;
    }

    /* --- not at end of path yet --- */
    /* --- can't follow any more path segments off of a non-identifier --- */
    IdentifierImpl id = object.asIdentifier();
    if (id == null) return count;

    // call this routine recursively on any wme matching the first segment
    //   of the attribute path
    for (WmeImpl w = id.goalInfo != null ? id.goalInfo.getImpasseWmes() : null;
        w != null;
        w = w.next)
      if (w.attr == path.get(pathIndex))
        count =
            add_values_of_attribute_path(w.value, path, pathIndex + 1, result, recursive, count);
    for (WmeImpl w = id.getInputWmes(); w != null; w = w.next)
      if (w.attr == path.get(pathIndex))
        count =
            add_values_of_attribute_path(w.value, path, pathIndex + 1, result, recursive, count);
    Slot s = Slot.find_slot(id, path.get(pathIndex));
    if (s != null) {
      for (WmeImpl w = s.getWmes(); w != null; w = w.next)
        count =
            add_values_of_attribute_path(w.value, path, pathIndex + 1, result, recursive, count);
    }
    return count;
  }

  /**
   * Adds info for a wme to the given "*result" growable_string. If "print_attribute" is TRUE, then
   * "^att-name" is included. If "recursive" is TRUE, the value is printed recursively as an object,
   * rather than as a simple atomic value.
   *
   * <p>trace.cpp:993:add_trace_for_wme
   *
   * @param result
   * @param w
   * @param print_attribute
   * @param recursive
   */
  void add_trace_for_wme(
      StringBuilder result, WmeImpl w, boolean print_attribute, boolean recursive) {
    result.append(" ");
    if (print_attribute) {
      result.append("^");
      result.append(String.format("%s", w.attr));
      result.append(" ");
    }
    if (recursive) {
      result.append(object_to_trace_string(w.value));
    } else {
      result.append(String.format("%s", w.value));
    }
  }

  /**
   * Adds the trace for values of a given attribute path off a given object, to the given "*result"
   * growable_string. If "print_attributes" is TRUE, then "^att-name" is included. If "recursive" is
   * TRUE, the values are printed recursively as objects, rather than as a simple atomic value. If
   * the given path is NIL, then all values of all attributes of the given object are printed.
   *
   * <p>trace.cpp:1028:add_trace_for_attribute_path
   *
   * @param object
   * @param path
   * @param result
   * @param print_attributes
   * @param recursive
   */
  private void add_trace_for_attribute_path(
      SymbolImpl object,
      List<Symbol> path,
      StringBuilder result,
      boolean print_attributes,
      boolean recursive) {
    StringBuilder values = new StringBuilder();

    if (path.isEmpty()) {
      IdentifierImpl id = object.asIdentifier();
      if (id == null) return;
      for (Slot s = id.slots; s != null; s = s.next)
        for (WmeImpl w = s.getWmes(); w != null; w = w.next)
          add_trace_for_wme(values, w, print_attributes, recursive);
      for (WmeImpl w = id.goalInfo != null ? id.goalInfo.getImpasseWmes() : null;
          w != null;
          w = w.next) add_trace_for_wme(values, w, print_attributes, recursive);
      for (WmeImpl w = id.getInputWmes(); w != null; w = w.next)
        add_trace_for_wme(values, w, print_attributes, recursive);
      if (values.length() > 0) result.append(values.substring(1));
      return;
    }

    int count = 0;
    count = add_values_of_attribute_path(object, path, 0, values, recursive, count);
    if (count == 0) {
      this.found_undefined = true;
      return;
    }

    if (print_attributes) {
      result.append("^");
      for (Iterator<Symbol> it = path.iterator(); it.hasNext(); ) {
        final Symbol c = it.next();
        result.append(String.format("%s", c));
        if (it.hasNext()) result.append(".");
      }
      result.append(" ");
    }
    if (values.length() > 0) result.append(values.substring(1));
  }

  /**
   * This is the main routine here. It returns a growable_string, given a trace format list (the
   * format to use) and an object (the object being printed).
   *
   * <p>trace.cpp:1086:trace_format_list_to_string
   *
   * @param tf
   * @param object
   * @return object formatted as a string
   */
  public String trace_format_list_to_string(TraceFormat tf, SymbolImpl object) {
    StringBuilder result = new StringBuilder();

    for (; tf != null; tf = tf.next) {
      switch (tf.type) {
        case STRING_TFT:
          result.append(tf.data_string);
          break;
        case PERCENT_TFT:
          result.append("%");
          break;
        case L_BRACKET_TFT:
          result.append("[");
          break;
        case R_BRACKET_TFT:
          result.append("]");
          break;

        case VALUES_TFT:
          add_trace_for_attribute_path(object, tf.data_attribute_path, result, false, false);
          break;
        case VALUES_RECURSIVELY_TFT:
          add_trace_for_attribute_path(object, tf.data_attribute_path, result, false, true);
          break;
        case ATTS_AND_VALUES_TFT:
          add_trace_for_attribute_path(object, tf.data_attribute_path, result, true, false);
          break;
        case ATTS_AND_VALUES_RECURSIVELY_TFT:
          add_trace_for_attribute_path(object, tf.data_attribute_path, result, true, true);
          break;

        case CURRENT_STATE_TFT:
          if (tparams.current_s == null) {
            found_undefined = true;
          } else {
            String temp_gs = object_to_trace_string(tparams.current_s);
            result.append(temp_gs);
          }
          break;
        case CURRENT_OPERATOR_TFT:
          if (tparams.current_o == null) {
            found_undefined = true;
          } else {
            String temp_gs = object_to_trace_string(tparams.current_o);
            result.append(temp_gs);
          }
          break;

        case DECISION_CYCLE_COUNT_TFT:
          if (tparams.allow_cycle_counts) {
            result.append(this.decisionCycle.d_cycle_count);
          } else {
            found_undefined = true;
          }
          break;
        case ELABORATION_CYCLE_COUNT_TFT:
          if (tparams.allow_cycle_counts) {
            result.append(this.context.getProperties().get(SoarProperties.E_CYCLE_COUNT));
          } else {
            found_undefined = true;
          }
          break;

        case IDENTIFIER_TFT:
          result.append(String.format("%s", object));
          break;

        case IF_ALL_DEFINED_TFT:
          {
            boolean saved_found_undefined = found_undefined;
            found_undefined = false;
            String temp_gs = trace_format_list_to_string(tf.data_subformat, object);
            if (!found_undefined) result.append(temp_gs);
            found_undefined = saved_found_undefined;
          }
          break;

        case LEFT_JUSTIFY_TFT:
          {
            String temp_gs = trace_format_list_to_string(tf.data_subformat, object);
            result.append(temp_gs);
            for (int i = tf.num - temp_gs.length(); i > 0; i--) result.append(" ");
          }
          break;

        case RIGHT_JUSTIFY_TFT:
          {
            String temp_gs = trace_format_list_to_string(tf.data_subformat, object);
            for (int i = tf.num - temp_gs.length(); i > 0; i--) result.append(" ");
            result.append(temp_gs);
          }
          break;

        case SUBGOAL_DEPTH_TFT:
          if (tparams.current_s != null) {
            result.append(tparams.current_s.level - 1);
          } else {
            found_undefined = true;
          }
          break;

        case REPEAT_SUBGOAL_DEPTH_TFT:
          if (tparams.current_s != null) {
            String temp_gs = trace_format_list_to_string(tf.data_subformat, object);
            for (int i = tparams.current_s.level - 1; i > 0; i--) result.append(temp_gs);
          } else {
            found_undefined = true;
          }
          break;

        case NEWLINE_TFT:
          result.append("\n");
          break;

        default:
          throw new IllegalStateException("Internal error: bad trace format type: " + tf.type);
      }
    }
    return result.toString();
  }

  /**
   * trace.cpp:1257:find_appropriate_trace_format
   *
   * @param stack_trace
   * @param type
   * @param name
   */
  private TraceFormat find_appropriate_trace_format(
      boolean stack_trace, TraceFormatRestriction type, SymbolImpl name) {
    // first try to find the exact one
    TraceFormat tf = lookup_trace_format(stack_trace, type, name);
    if (tf != null) return tf;

    // failing that, try ignoring the type but retaining the name
    if (type != TraceFormatRestriction.FOR_ANYTHING_TF) {
      tf = lookup_trace_format(stack_trace, TraceFormatRestriction.FOR_ANYTHING_TF, name);
      if (tf != null) return tf;
    }

    // failing that, try ignoring the name but retaining the type
    if (name != null) {
      tf = lookup_trace_format(stack_trace, type, null);
      if (tf != null) return tf;
    }

    // last resort: find a format that applies to anything at all
    return lookup_trace_format(stack_trace, TraceFormatRestriction.FOR_ANYTHING_TF, null);
  }

  /**
   * trace.cpp:1283:object_to_trace_string
   *
   * @param object
   * @return trace string
   */
  private String object_to_trace_string(SymbolImpl object) {
    // If it's not an identifier, just print it as an atom. Also, if it's
    // already being printed, print it as an atom to avoid getting into an
    // infinite loop.
    IdentifierImpl id = object.asIdentifier();
    if (id == null || id.tc_number == tf_printing_tc) {
      return String.format("%s", object);
    }

    // mark it as being printed
    id.tc_number = tf_printing_tc;

    // determine the type and name of the object
    TraceFormatRestriction type_of_object;
    if (id.isGoal()) type_of_object = TraceFormatRestriction.FOR_STATES_TF;
    else if (id.isa_operator != 0) type_of_object = TraceFormatRestriction.FOR_OPERATORS_TF;
    else type_of_object = TraceFormatRestriction.FOR_ANYTHING_TF;

    SymbolImpl name = find_name_of_object(object, predefinedSyms.name_symbol);

    // find the trace format to use
    TraceFormat tf = find_appropriate_trace_format(false, type_of_object, name);

    // now call trace_format_list_to_string()
    String gs = null;
    if (tf != null) {
      TracingParameters saved_tparams = new TracingParameters(tparams);
      tparams.current_s = tparams.current_o = null;
      tparams.allow_cycle_counts = false;
      gs = trace_format_list_to_string(tf, object);
      tparams = saved_tparams;
    } else {
      // no applicable trace format, so just print the object itself
      gs = String.format("%s", object);
    }

    id.tc_number = null; // unmark it now that we're done
    return gs;
  }

  /**
   * trace.cpp:1330:selection_to_trace_string
   *
   * @param object
   * @param current_state
   * @param selection_type
   * @param allow_cycle_counts
   * @return trace string
   */
  private String selection_to_trace_string(
      SymbolImpl object,
      IdentifierImpl current_state,
      TraceFormatRestriction selection_type,
      boolean allow_cycle_counts) {
    // find the problem space name
    SymbolImpl name = null;

    // find the trace format to use
    TraceFormat tf = find_appropriate_trace_format(true, selection_type, name);

    /* --- if there's no applicable trace format, print nothing --- */
    if (tf == null) return "";

    /* --- save/restore tparams, and call trace_format_list_to_string() --- */
    TracingParameters saved_tparams = new TracingParameters(tparams);
    tparams.current_s = tparams.current_o = null;
    if (current_state != null) {
      tparams.current_s = current_state;
      if (current_state.goalInfo.operator_slot.getWmes() != null) {
        // TODO Is it safe to assume this is an IdentifierImpl?
        tparams.current_o = current_state.goalInfo.operator_slot.getWmes().value.asIdentifier();
      }
    }
    tparams.allow_cycle_counts = allow_cycle_counts;
    String gs = trace_format_list_to_string(tf, object);
    tparams = saved_tparams;

    return gs;
  }

  /**
   * Print_object_trace() takes an object (any symbol). It prints the trace for that object.
   *
   * <p>trace.cpp:1377:print_object_trace
   *
   * @param writer
   * @param object
   * @throws IOException
   */
  public void print_object_trace(Writer writer, SymbolImpl object) throws IOException {
    tf_printing_tc = DefaultMarker.create();
    String gs = object_to_trace_string(object);
    writer.append(gs);
  }

  /**
   * Print_stack_trace() takes a (context) object (the state or op), the current state, the
   * "slot_type" (one of FOR_OPERATORS_TF, etc.), and a flag indicating whether to allow %dc and %ec
   * escapes (this flag should normally be TRUE for watch 0 traces but FALSE during a "pgs"
   * command). It prints the trace for that context object.
   *
   * <p>trace.cpp:1459:print_stack_trace
   *
   * @param writer
   * @param object
   * @param state
   * @param slot_type
   * @param allow_cycle_counts
   * @throws IOException
   */
  public void print_stack_trace(
      Writer writer,
      SymbolImpl object,
      IdentifierImpl state,
      TraceFormatRestriction slot_type,
      boolean allow_cycle_counts)
      throws IOException {
    tf_printing_tc = DefaultMarker.create();
    String gs = selection_to_trace_string(object, state, slot_type, allow_cycle_counts);
    writer.append(gs);

    // RPM 5/05
    // print_stack_trace_xml(thisAgent, object, state, slot_type, allow_cycle_counts);
  }

  /**
   * TODO This should probably go somewhere else
   *
   * <p>decide.cpp:2456:print_lowest_slot_in_context_stack
   *
   * @param writer
   * @throws IOException
   */
  public void print_lowest_slot_in_context_stack(Writer writer, IdentifierImpl bottom_goal)
      throws IOException {
    // Note: There was commented out code from Bob Wray in 1997 here in csoar about
    // "this doesn't work yet so for now just print the last selection".
    // Presumably, whatever it was supposed to do has been lost to the ages.

    if (bottom_goal.goalInfo.operator_slot.getWmes() != null) {
      print_stack_trace(
          writer,
          bottom_goal.goalInfo.operator_slot.getWmes().value,
          bottom_goal,
          TraceFormatRestriction.FOR_OPERATORS_TF,
          true);
    }

    /*
    this coded is needed just so that when an ONC is created in OPERAND
    (i.e. if the previous goal's operator slot is not empty), it's stack
    trace line doesn't get a number.  this is done because in OPERAND,
    ONCs are detected for "free".
    */

    else {
      print_stack_trace(
          writer, bottom_goal, bottom_goal, TraceFormatRestriction.FOR_STATES_TF, true);
    }
  }

  /**
   * a utility function for finding the value of the ^name attribute on a given object (symbol). It
   * returns the name, or NIL if the object has no name.
   *
   * <p>wmem.cpp:295:find_name_of_object
   *
   * @param object
   * @return the name of the object
   */
  private static SymbolImpl find_name_of_object(SymbolImpl object, StringSymbolImpl name_symbol) {
    IdentifierImpl id = object.asIdentifier();
    if (id == null) {
      return null;
    }
    Slot s = Slot.find_slot(id, name_symbol);
    if (s == null) {
      return null;
    }
    return s.getWmes() != null ? s.getWmes().value : null;
  }
}
