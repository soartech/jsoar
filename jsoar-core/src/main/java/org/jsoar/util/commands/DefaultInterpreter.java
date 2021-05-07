/*
 * Copyright (c) 2010 Dave Ray <daveray@gmail.com>
 *
 * Created on May 12, 2010
 */
package org.jsoar.util.commands;

import com.google.common.base.Joiner;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PushbackReader;
import java.io.Reader;
import java.io.StringReader;
import java.net.URISyntaxException;
import java.net.URL;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.jsoar.kernel.Agent;
import org.jsoar.kernel.SoarException;
import org.jsoar.kernel.commands.LoadCommand;
import org.jsoar.kernel.commands.PopdCommand;
import org.jsoar.kernel.commands.PushdCommand;
import org.jsoar.kernel.commands.PwdCommand;
import org.jsoar.kernel.commands.SaveCommand;
import org.jsoar.kernel.commands.SourceCommand;
import org.jsoar.kernel.commands.SourceCommandAdapter;
import org.jsoar.kernel.commands.SpCommand;
import org.jsoar.kernel.commands.StandardCommands;
import org.jsoar.kernel.exceptions.SoarInterpreterException;
import org.jsoar.util.ByRef;
import org.jsoar.util.SourceLocation;
import org.jsoar.util.StringTools;
import org.jsoar.util.UrlTools;
import picocli.CommandLine.Command;
import picocli.CommandLine.HelpCommand;
import picocli.CommandLine.Parameters;

/**
 * Default implementation of {@link SoarCommandInterpreter}.
 *
 * @author ray
 */
public class DefaultInterpreter implements SoarCommandInterpreter {
  private final Map<String, SoarCommand> commands = new HashMap<String, SoarCommand>();
  private final Map<String, List<String>> aliases = new LinkedHashMap<String, List<String>>();

  private final SourceCommand sourceCommand;
  private final LoadCommand loadCommand;
  private final SaveCommand saveCommand;
  private SoarExceptionsManager exceptionsManager;

  public DefaultInterpreter(Agent agent) {
    this.exceptionsManager = new SoarExceptionsManager();
    // Interpreter-specific handlers
    addCommand("alias", new AliasCommand(agent, this.aliases));
    this.sourceCommand = new SourceCommand(new MySourceCommandAdapter(), agent.getEvents());
    addCommand("pushd", new PushdCommand(sourceCommand, agent));
    addCommand("popd", new PopdCommand(sourceCommand, agent));
    addCommand("pwd", new PwdCommand(sourceCommand));
    addCommand("save", this.saveCommand = new SaveCommand(sourceCommand, agent));

    // Load general handlers
    StandardCommands.addToInterpreter(agent, this);

    // this interpreter-specific handler depends on SpCommand, which is created as part of the
    // standard commands
    addCommand(
        "load",
        this.loadCommand =
            new LoadCommand(sourceCommand, (SpCommand) this.commands.get("sp"), agent));
  }

  /* (non-Javadoc)
   * @see org.jsoar.util.commands.SoarCommandInterpreter#addCommand(java.lang.String, org.jsoar.util.commands.SoarCommand)
   */
  @Override
  public void addCommand(String name, SoarCommand handler) {
    commands.put(name, handler);
  }

  /*
   * (non-Javadoc)
   * @see org.jsoar.util.commands.SoarCommandInterpreter#getCommand(java.lang.String, org.jsoar.util.SourceLocation)
   */
  public SoarCommand getCommand(String name, SourceLocation srcLoc) throws SoarException {
    final List<String> command = new ArrayList<String>();
    command.add(name);
    return getSoarCommand(ByRef.create(new ParsedCommand(srcLoc, command)));
  }

  public ParsedCommand getParsedCommand(String name, SourceLocation srcLoc) {
    final List<String> command = new ArrayList<String>();
    command.add(name);
    return resolveAliases(new ParsedCommand(srcLoc, command));
  }

  /* (non-Javadoc)
   * @see org.jsoar.util.commands.SoarCommandInterpreter#getName()
   */
  @Override
  public String getName() {
    return "default";
  }
  /* (non-Javadoc)
   * @see org.jsoar.util.commands.SoarCommandInterpreter#dispose()
   */
  @Override
  public void dispose() {}

  /* (non-Javadoc)
   * @see org.jsoar.util.commands.SoarCommandInterpreter#eval(java.lang.String)
   */
  @Override
  public String eval(String code) throws SoarException {
    return evalAndClose(
        new StringReader(code), code.length() > 100 ? code.substring(0, 100) : code);
  }

  /* (non-Javadoc)
   * @see org.jsoar.util.commands.SoarCommandInterpreter#source(java.io.File)
   */
  @Override
  public void source(File file) throws SoarException {
    sourceCommand.source(file.getAbsolutePath());
  }

  /* (non-Javadoc)
   * @see org.jsoar.util.commands.SoarCommandInterpreter#source(java.net.URL)
   */
  @Override
  public void source(URL url) throws SoarException {
    sourceCommand.source(url.toExternalForm());
  }

  /*
   * (non-Javadoc)
   * @see org.jsoar.util.commands.SoarCommandInterpreter#loadRete(java.io.File)
   */
  @Override
  public void loadRete(File file) throws SoarException {
    loadCommand.execute(
        DefaultSoarCommandContext.empty(),
        new String[] {"load", "rete-net", "--load", file.getAbsolutePath().replace('\\', '/')});
  }

  /*
   * (non-Javadoc)
   * @see org.jsoar.util.commands.SoarCommandInterpreter#loadRete(java.net.URL)
   */
  @Override
  public void loadRete(URL url) throws SoarException {
    loadCommand.execute(
        DefaultSoarCommandContext.empty(),
        new String[] {"load", "rete-net", "--load", url.toExternalForm()});
  }

  /*
   * (non-Javadoc)
   * @see org.jsoar.util.commands.SoarCommandInterpreter#saveRete(java.io.File)
   */
  @Override
  public void saveRete(File file) throws SoarException {
    saveCommand.execute(
        DefaultSoarCommandContext.empty(),
        new String[] {"save", "rete-net", "--save", file.getAbsolutePath().replace('\\', '/')});
  }

  @Override
  public String getWorkingDirectory() {
    return sourceCommand.getWorkingDirectory();
  }

  @Override
  public String[] getCompletionList(String command, int cursorPosition) {
    String[] commands = null;
    List<String> commandsList = new ArrayList<>();
    for (String s : getCommandStrings()) {
      if (s.startsWith(command)) {
        commandsList.add(s);
      }
    }
    for (String s : getAliasStrings()) {
      if (s.startsWith(command)) {
        commandsList.add(s);
      }
    }
    commands = new String[commandsList.size()];
    commands = commandsList.toArray(commands);

    return commands;
  }

  @Override
  public Collection<String> getSourcedFiles() {
    return sourceCommand.getSourcedFiles();
  }

  public Set<String> getAliasStrings() {
    return aliases.keySet();
  }

  private String evalAndClose(Reader reader, String context) throws SoarException {
    try {
      return eval(reader);
    } catch (IOException e) {
      throw new SoarException("Error while evaluating '" + context + "': " + e.getMessage(), e);
    } finally {
      try {
        reader.close();
      } catch (IOException e) {
        throw new SoarException("Error while closing '" + context + "': " + e.getMessage(), e);
      }
    }
  }

  private String eval(Reader reader) throws IOException, SoarException {
    final DefaultInterpreterParser parser = new DefaultInterpreterParser();
    final ParserBuffer pbReader = new ParserBuffer(new PushbackReader(reader));
    pbReader.setFile(sourceCommand.getCurrentFile());

    ParsedCommand parsedCommand = parser.parseCommand(pbReader);
    String lastResult = "";
    while (!parsedCommand.isEof()) {
      lastResult = executeParsedCommand(parsedCommand);

      parsedCommand = parser.parseCommand(pbReader);
    }
    return lastResult;
  }

  private String executeParsedCommand(ParsedCommand parsedCommand) throws SoarException {
    final ByRef<ParsedCommand> parsedCommandRef = new ByRef<ParsedCommand>(parsedCommand);
    final SoarCommand command = getSoarCommand(parsedCommandRef);
    final SoarCommandContext commandContext =
        new DefaultSoarCommandContext(parsedCommandRef.value.getLocation());
    return command.execute(
        commandContext, parsedCommandRef.value.getArgs().toArray(new String[] {}));
  }

  private SoarCommand getSoarCommand(ByRef<ParsedCommand> parsedCommand) throws SoarException {
    parsedCommand.value = resolveAliases(parsedCommand.value);
    final SoarCommand command = resolveCommand(parsedCommand.value.getArgs().get(0));
    if (command != null) {
      return command;
    } else {
      throw new SoarInterpreterException(
          parsedCommand.value.getLocation()
              + ": Unknown command '"
              + parsedCommand.value.getArgs().get(0)
              + "'",
          parsedCommand);
    }
  }

  private List<Map.Entry<String, SoarCommand>> resolvePossibleCommands(String prefix) {
    final List<Map.Entry<String, SoarCommand>> result =
        new ArrayList<Map.Entry<String, SoarCommand>>();
    for (Map.Entry<String, SoarCommand> entry : commands.entrySet()) {
      if (entry.getKey().startsWith(prefix)) {
        result.add(entry);
      }
    }
    return result;
  }

  private List<String> getNames(List<Map.Entry<String, SoarCommand>> possible) {
    final List<String> result = new ArrayList<String>(possible.size());
    for (Map.Entry<String, SoarCommand> e : possible) {
      result.add(e.getKey());
    }
    return result;
  }

  private SoarCommand resolveCommand(String name) throws SoarException {
    // First a quick check for an exact match
    final SoarCommand quick = commands.get(name);
    if (quick != null) {
      return quick;
    }

    // Otherwise check for partial matches
    final List<Map.Entry<String, SoarCommand>> possible = resolvePossibleCommands(name);
    if (possible.isEmpty()) {
      return null;
    } else if (possible.size() == 1) {
      return possible.get(0).getValue();
    } else {
      throw new SoarException(
          "Ambiguous command '"
              + name
              + "'. Could be one of '"
              + Joiner.on(", ").join(getNames(possible)));
    }
  }

  private ParsedCommand resolveAliases(ParsedCommand parsedCommand) {
    final String first = parsedCommand.getArgs().get(0);
    final List<String> alias = aliases.get(first);
    if (alias == null) {
      return parsedCommand;
    } else {
      final List<String> result = new ArrayList<String>(alias);
      result.addAll(parsedCommand.getArgs().subList(1, parsedCommand.getArgs().size()));
      return new ParsedCommand(parsedCommand.getLocation(), result);
    }
  }

  public SoarCommand getCommand(String s) {
    return commands.get(s);
  }

  private static class AliasCommand extends PicocliSoarCommand {
    public AliasCommand(Agent agent, Map<String, List<String>> aliases) {
      super(agent, new Alias(agent, aliases));
    }

    @Command(
        name = "alias",
        description = "Create or print command aliases",
        subcommands = {HelpCommand.class})
    private static class Alias implements Runnable {

      private final Map<String, List<String>> aliases;
      private final Agent agent;

      @Parameters(
          description =
              "If no args, prints the list of aliases. If 1 arg, prints the command for that alias. If 2 or more args, defines an alias. The first arg is the alias name, and all following args are the command and options/parameters that it maps to.")
      private String[] args = new String[0];

      public Alias(Agent agent, Map<String, List<String>> aliases) {
        this.agent = agent;
        this.aliases = aliases;
      }

      @Override
      public void run() {
        if (args.length == 0) {
          final StringBuilder b = new StringBuilder();
          for (Map.Entry<String, List<String>> e : aliases.entrySet()) {
            b.append(aliasToString(e.getKey(), e.getValue()));
            b.append('\n');
          }
          agent.getPrinter().print(b.toString());
        } else if (args.length == 1) {
          final List<String> aliasArgs = aliases.get(args[0]);
          if (aliasArgs == null) {
            agent.getPrinter().print("Unknown alias '" + args[0] + "'");
            return;
          }
          agent.getPrinter().print(aliasToString(args[0], aliasArgs));
        } else {
          final List<String> aliasArgs = new ArrayList<String>(args.length - 1);
          for (int i = 1; i < args.length; ++i) {
            aliasArgs.add(args[i]);
          }
          aliases.put(args[0], aliasArgs);
          agent.getPrinter().print(aliasToString(args[0], aliasArgs));
        }
      }

      private String aliasToString(String name, List<String> args) {
        return name + "=" + StringTools.join(args, " ");
      }
    }
  }

  private class MySourceCommandAdapter implements SourceCommandAdapter {
    @Override
    public void eval(File file) throws SoarException {
      try {
        String code = getReaderContents(new BufferedReader(new FileReader(file)));
        code = fixLineEndings(code);
        evalAndClose(new StringReader(code), file.getAbsolutePath());
        //                evalAndClose(new BufferedReader(new FileReader(file)),
        // file.getAbsolutePath());
      } catch (IOException e) {
        throw new SoarException(e.getMessage(), e);
      }
    }

    @Override
    public void eval(URL url) throws SoarException {
      try {
        url = UrlTools.normalize(url);
        String code =
            getReaderContents(new BufferedReader(new InputStreamReader(url.openStream())));
        code = fixLineEndings(code);
        evalAndClose(new StringReader(code), url.toExternalForm());
        //                evalAndClose(new BufferedReader(new InputStreamReader(url.openStream())),
        // url.toExternalForm());
      } catch (IOException | URISyntaxException e) {
        throw new SoarException("Failed to open '" + url + "': " + e.getStackTrace(), e);
      }
    }

    @Override
    public String eval(String code) throws SoarException {
      code = fixLineEndings(code);
      return DefaultInterpreter.this.eval(code);
    }

    // returns string of all contents in reader
    private String getReaderContents(Reader reader) throws IOException {
      StringBuilder builder = new StringBuilder();
      int ch;

      while ((ch = reader.read()) != -1) {
        builder.append((char) ch);
      }

      return builder.toString();
    }

    // Replaces windows "\r\n" and old mac line "\r" endings with unix "\n"
    private String fixLineEndings(String code) {
      code = code.replaceAll("\r\n", "\n");
      code = code.replaceAll("\r", "\n");
      return code;
    }
  }

  @Override
  public List<String> getCommandStrings() {
    List<String> commandList = new ArrayList<>(this.commands.keySet());
    Collections.sort(commandList);
    return commandList;
  }

  @Override
  public SoarExceptionsManager getExceptionsManager() {
    return exceptionsManager;
  }
}
